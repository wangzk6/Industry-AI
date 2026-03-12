package com.kelvin.industry.enterprise.session.api;

import com.kelvin.industry.enterprise.session.model.MessageRole;

public class AppendMessageRequest {

    private MessageRole role;

    private String content;

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
}
