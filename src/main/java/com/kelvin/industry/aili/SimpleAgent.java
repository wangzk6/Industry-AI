package com.kelvin.industry.aili;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import lombok.extern.slf4j.Slf4j;

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

    public SimpleAgent(ChatClient weatherChatClient) {
        this.chatClient = weatherChatClient;
    }

    /**
     * 一次性返回完整回答
     */
    @GetMapping("/ask")
    public String ask(@RequestParam(defaultValue = "现在天气怎么样？") String q) {
        log.info("[Agent] user -> model: {}", q);
        String answer = chatClient.prompt()
                .user(q)
                .call()
                .content();
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
