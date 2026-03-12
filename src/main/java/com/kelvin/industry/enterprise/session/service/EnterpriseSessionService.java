package com.kelvin.industry.enterprise.session.service;

import com.kelvin.industry.enterprise.session.config.EnterpriseSessionProperties;
import com.kelvin.industry.enterprise.session.model.AgentSession;
import com.kelvin.industry.enterprise.session.model.MessageRole;
import com.kelvin.industry.enterprise.session.model.SessionMessage;
import com.kelvin.industry.enterprise.session.model.SessionState;
import com.kelvin.industry.enterprise.session.repository.AgentSessionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

@Service
@Slf4j
public class EnterpriseSessionService {

    private final AgentSessionRepository repository;
    private final EnterpriseSessionProperties properties;
    private final PromptContextAssembler promptContextAssembler;

    public EnterpriseSessionService(
            AgentSessionRepository repository,
            EnterpriseSessionProperties properties,
            PromptContextAssembler promptContextAssembler
    ) {
        this.repository = repository;
        this.properties = properties;
        this.promptContextAssembler = promptContextAssembler;
    }

    public AgentSession createSession(String sessionId, String tenantId, String userId, Map<String, String> metadata) {
        // 新建前先做总容量治理，避免进程内 session 无上限增长。
        evictIfNeeded();
        LocalDateTime now = LocalDateTime.now();

        AgentSession session = new AgentSession();
        if (StringUtils.hasText(sessionId)) {
            // 企业系统里自定义 sessionId 往往来自上游网关或业务单据，需要显式防重。
            if (repository.findById(sessionId).isPresent()) {
                throw new IllegalStateException("session already exists: " + sessionId);
            }
            session.setId(sessionId);
        }
        session.setTenantId(tenantId);
        session.setUserId(userId);
        session.setCreatedAt(now);
        session.setUpdatedAt(now);
        session.setLastAccessedAt(now);
        session.setExpiresAt(now.plus(properties.getTtl()));
        if (metadata != null && !metadata.isEmpty()) {
            session.getMetadata().putAll(metadata);
        }
        return repository.save(session);
    }

    public AgentSession getOrCreate(String sessionId) {
        if (!StringUtils.hasText(sessionId)) {
            // 上游未传 sessionId 时由服务端兜底创建，适合匿名访客或快速试用场景。
            return createSession(null, null, null, Map.of());
        }
        return repository.findById(sessionId)
                .map(this::activate)
                .orElseGet(() -> createSession(sessionId, null, null, Map.of()));
    }

    public AgentSession getSession(String sessionId) {
        if (!StringUtils.hasText(sessionId)) {
            throw new IllegalArgumentException("sessionId must not be blank");
        }
        return repository.findById(sessionId)
                .map(this::activate)
                .orElseThrow(() -> new NoSuchElementException("session not found: " + sessionId));
    }

    public List<AgentSession> listSessions() {
        return repository.findAll().stream()
                .sorted(Comparator.comparing(AgentSession::getUpdatedAt).reversed())
                .toList();
    }

    public SessionMessage appendUserMessage(String sessionId, String content) {
        return appendMessage(sessionId, MessageRole.USER, content);
    }

    public SessionMessage appendAssistantMessage(String sessionId, String content) {
        return appendMessage(sessionId, MessageRole.ASSISTANT, content);
    }

    public SessionMessage appendSystemMessage(String sessionId, String content) {
        return appendMessage(sessionId, MessageRole.SYSTEM, content);
    }

    public String buildPrompt(String sessionId, String latestUserInput) {
        // 这里不直接调用模型，只负责把 session 历史转换成标准上下文字符串。
        AgentSession session = getSession(sessionId);
        return promptContextAssembler.assemble(session, latestUserInput);
    }

    public boolean closeSession(String sessionId) {
        return repository.findById(sessionId).map(session -> {
            session.setState(SessionState.CLOSED);
            session.setUpdatedAt(LocalDateTime.now());
            repository.save(session);
            return true;
        }).orElse(false);
    }

    public boolean deleteSession(String sessionId) {
        if (repository.findById(sessionId).isEmpty()) {
            return false;
        }
        repository.deleteById(sessionId);
        return true;
    }

    public int cleanupExpiredSessions() {
        LocalDateTime now = LocalDateTime.now();
        int cleaned = 0;
        for (AgentSession session : repository.findAll()) {
            if (session.isExpired(now)) {
                // 过期 session 先标记状态，再删除存储记录，便于未来扩展审计或事件通知。
                session.setState(SessionState.EXPIRED);
                repository.deleteById(session.getId());
                cleaned++;
            }
        }
        if (cleaned > 0) {
            log.info("Enterprise session cleanup removed {} expired sessions", cleaned);
        }
        return cleaned;
    }

    @Scheduled(fixedDelayString = "#{T(java.time.Duration).parse('${enterprise.agent.session.cleanup-interval:PT5M}').toMillis()}")
    public void scheduledCleanup() {
        // 后台定时清理，避免依赖用户请求触发过期淘汰。
        cleanupExpiredSessions();
    }

    private SessionMessage appendMessage(String sessionId, MessageRole role, String content) {
        AgentSession session = getSession(sessionId);
        SessionMessage message = SessionMessage.of(role, content);
        session.appendMessage(message, properties.getMaxMessagesPerSession());
        // 写消息的同时刷新过期时间，保证活跃 session 不会被后台任务误删。
        session.touch(LocalDateTime.now(), LocalDateTime.now().plus(properties.getTtl()));
        repository.save(session);
        return message;
    }

    private AgentSession activate(AgentSession session) {
        LocalDateTime now = LocalDateTime.now();
        if (session.isExpired(now)) {
            repository.deleteById(session.getId());
            throw new NoSuchElementException("session expired: " + session.getId());
        }
        if (session.getState() == SessionState.CLOSED) {
            throw new IllegalStateException("session closed: " + session.getId());
        }
        // 读取 session 也算活跃访问，需要更新最近访问时间和新的续租时间。
        session.setState(SessionState.ACTIVE);
        session.touch(now, now.plus(properties.getTtl()));
        return repository.save(session);
    }

    private void evictIfNeeded() {
        if (repository.count() < properties.getMaxSessions()) {
            return;
        }
        // 达到容量上限时执行 LRU 淘汰，优先移除最久未访问的会话。
        repository.findAll().stream()
                .min(Comparator.comparing(AgentSession::getLastAccessedAt))
                .ifPresent(session -> {
                    log.warn("Enterprise session capacity reached, evicting {}", session.getId());
                    repository.deleteById(session.getId());
                });
    }
}
