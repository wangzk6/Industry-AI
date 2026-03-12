package com.kelvin.industry.enterprise.session.api;

import com.kelvin.industry.enterprise.session.model.AgentSession;
import com.kelvin.industry.enterprise.session.model.MessageRole;
import com.kelvin.industry.enterprise.session.model.SessionMessage;
import com.kelvin.industry.enterprise.session.service.EnterpriseSessionService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/enterprise/sessions")
public class EnterpriseSessionController {

    private final EnterpriseSessionService sessionService;

    public EnterpriseSessionController(EnterpriseSessionService sessionService) {
        this.sessionService = sessionService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AgentSession create(@RequestBody(required = false) CreateEnterpriseSessionRequest request) {
        // 创建接口允许 request 为空，方便先快速拿到 sessionId 再逐步补充上下文。
        CreateEnterpriseSessionRequest payload = request == null ? new CreateEnterpriseSessionRequest() : request;
        return sessionService.createSession(
                payload.getSessionId(),
                payload.getTenantId(),
                payload.getUserId(),
                payload.getMetadata()
        );
    }

    @GetMapping
    public List<AgentSession> list() {
        return sessionService.listSessions();
    }

    @GetMapping("/{sessionId}")
    public AgentSession get(@PathVariable String sessionId) {
        return sessionService.getSession(sessionId);
    }

    @PostMapping("/{sessionId}/messages")
    public SessionMessage appendMessage(@PathVariable String sessionId, @RequestBody AppendMessageRequest request) {
        MessageRole role = request.getRole() == null ? MessageRole.USER : request.getRole();
        // TOOL 消息通常由运行时自动写入，这里先限制为业务侧常用的三种角色。
        return switch (role) {
            case USER -> sessionService.appendUserMessage(sessionId, request.getContent());
            case ASSISTANT -> sessionService.appendAssistantMessage(sessionId, request.getContent());
            case SYSTEM -> sessionService.appendSystemMessage(sessionId, request.getContent());
            case TOOL -> throw new IllegalArgumentException("TOOL message should be managed by tool runtime");
        };
    }

    @PostMapping("/{sessionId}/prompt")
    public Map<String, String> buildPrompt(@PathVariable String sessionId, @RequestBody BuildPromptRequest request) {
        // 这个接口专门给 Agent 编排层用，用于把 session 历史转成可直接送模型的 prompt。
        return Map.of("prompt", sessionService.buildPrompt(sessionId, request.getLatestUserInput()));
    }

    @PostMapping("/{sessionId}/close")
    public void close(@PathVariable String sessionId) {
        sessionService.closeSession(sessionId);
    }

    @DeleteMapping("/{sessionId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String sessionId) {
        sessionService.deleteSession(sessionId);
    }

    @ExceptionHandler(NoSuchElementException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ResponseBody
    public Map<String, String> handleNotFound(NoSuchElementException ex) {
        return Map.of("message", ex.getMessage());
    }

    @ExceptionHandler({IllegalStateException.class, IllegalArgumentException.class})
    @ResponseStatus(HttpStatus.CONFLICT)
    @ResponseBody
    public Map<String, String> handleConflict(RuntimeException ex) {
        return Map.of("message", ex.getMessage());
    }
}
