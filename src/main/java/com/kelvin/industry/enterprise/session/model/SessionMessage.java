package com.kelvin.industry.enterprise.session.model;

import java.time.LocalDateTime;
import java.util.UUID;

public class SessionMessage {

    private String id = UUID.randomUUID().toString();

    private MessageRole role;

    private String content;

    private LocalDateTime createdAt = LocalDateTime.now();

    public static SessionMessage of(MessageRole role, String content) {
        SessionMessage message = new SessionMessage();
        message.setRole(role);
        message.setContent(content);
        return message;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public MessageRole getRole() {
        return role;
    }

    public void setRole(MessageRole role) {
        this.role = role;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
