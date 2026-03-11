package com.kelvin.industry.aili.weather_agent;

import com.kelvin.industry.session.bean.Message;
import com.kelvin.industry.session.bean.Role;
import com.kelvin.industry.session.bean.Session;
import com.kelvin.industry.session.service.SessionManager;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 天气 Agent HTTP 入口
 * GET /weather/ask?q=北京今天天气怎么样
 * GET /weather/stream?q=我这里天气怎么样
 */
@RestController
@RequestMapping("/weather")
@Slf4j
public class SimpleAgent {

    private final ChatClient chatClient;

    private final SessionManager sessionManager;

    public SimpleAgent(ChatClient weatherChatClient, SessionManager sessionManager) {
        this.chatClient = weatherChatClient;
        this.sessionManager = sessionManager;
    }

    /**
     * 一次性返回完整回答
     */
    @GetMapping("/ask")
    public String ask(@RequestParam(defaultValue = "现在天气怎么样？") String q, @RequestParam String sessionId) {
        log.info("[Agent] user -> model: {}", q);
        //简单会话管理
        Session session = sessionManager.getById(sessionId);
        Message message = new Message();
        message.setContent(q);
        message.setRole(Role.USER);
        message.setTimestamp(LocalDateTime.now());
        message.setId(UUID.randomUUID().toString());
        session.chat(message);

        String answer = chatClient.prompt()
                .user(q)
                .call()
                .content();
        Message messageAI = new Message();
        messageAI.setContent(answer);
        messageAI.setRole(Role.AI);
        messageAI.setTimestamp(LocalDateTime.now());
        messageAI.setId(UUID.randomUUID().toString());
        session.chat(messageAI);

        sessionManager.saveSession(session);
        log.info("[Agent] model -> user: {}", answer);

        return answer;
    }

    /**
     * 流式输出（Server-Sent Events）
     */
    @GetMapping(value = "/stream", produces = "text/event-stream;charset=UTF-8")
    public Flux<String> stream(@RequestParam(defaultValue = "现在天气怎么样？") String q) {
        log.info("[Agent] user -> model(stream): {}", q);
        return chatClient.prompt()
                .user(q)
                .stream()
                .content()
                .doOnNext(chunk -> log.info("[Agent] model stream chunk: {}", chunk));
    }
}
