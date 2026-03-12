package com.kelvin.industry.enterprise.session.model;

import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class AgentSession {

    private String id = UUID.randomUUID().toString();

    private String tenantId;

    private String userId;

    private SessionState state = SessionState.ACTIVE;

    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime updatedAt = LocalDateTime.now();

    private LocalDateTime lastAccessedAt = LocalDateTime.now();

    private LocalDateTime expiresAt;

    private long version;

    private final Map<String, String> metadata = new LinkedHashMap<>();

    // 使用双端队列维护消息窗口，便于在超过上限时从头部淘汰旧消息。
    private final Deque<SessionMessage> messages = new ArrayDeque<>();

    public synchronized void appendMessage(SessionMessage message, int maxMessagesPerSession) {
        messages.offerLast(message);
        // 企业场景里会话可能很长，超过窗口后只保留最近消息，避免上下文无限膨胀。
        while (messages.size() > maxMessagesPerSession) {
            messages.pollFirst();
        }
        LocalDateTime now = LocalDateTime.now();
        updatedAt = now;
        lastAccessedAt = now;
        version++;
    }

    public synchronized List<SessionMessage> snapshotMessages() {
        // 返回快照而不是直接暴露内部集合，避免外部线程绕过聚合根修改状态。
        return new ArrayList<>(messages);
    }

    public synchronized void touch(LocalDateTime now, LocalDateTime expireAt) {
        // 每次访问都顺带续租，session 的过期时间按最近访问时间滚动。
        updatedAt = now;
        lastAccessedAt = now;
        expiresAt = expireAt;
    }

    public boolean isExpired(LocalDateTime now) {
        return expiresAt != null && now.isAfter(expiresAt);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public SessionState getState() {
        return state;
    }

    public void setState(SessionState state) {
        this.state = state;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public LocalDateTime getLastAccessedAt() {
        return lastAccessedAt;
    }

    public void setLastAccessedAt(LocalDateTime lastAccessedAt) {
        this.lastAccessedAt = lastAccessedAt;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public long getVersion() {
        return version;
    }

    public void setVersion(long version) {
        this.version = version;
    }

    public Map<String, String> getMetadata() {
        return metadata;
    }
}
