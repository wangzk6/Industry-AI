package com.kelvin.industry.enterprise.session.service;

import com.kelvin.industry.enterprise.session.config.EnterpriseSessionProperties;
import com.kelvin.industry.enterprise.session.model.AgentSession;
import com.kelvin.industry.enterprise.session.model.SessionMessage;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PromptContextAssembler {

    private final EnterpriseSessionProperties properties;

    public PromptContextAssembler(EnterpriseSessionProperties properties) {
        this.properties = properties;
    }

    public String assemble(AgentSession session, String latestUserInput) {
        List<SessionMessage> messages = session.snapshotMessages();
        // 只截取最近 historyWindow 条，避免提示词过长拉高成本和延迟。
        int start = Math.max(0, messages.size() - properties.getHistoryWindow());

        StringBuilder builder = new StringBuilder();
        builder.append("你正在处理一个多轮企业级 Agent 会话，请结合历史消息继续回答。\n");
        builder.append("sessionId=").append(session.getId());
        if (session.getTenantId() != null) {
            builder.append(", tenantId=").append(session.getTenantId());
        }
        if (session.getUserId() != null) {
            builder.append(", userId=").append(session.getUserId());
        }
        builder.append("\n");

        if (!session.getMetadata().isEmpty()) {
            builder.append("metadata=").append(session.getMetadata()).append("\n");
        }

        builder.append("history:\n");
        for (int i = start; i < messages.size(); i++) {
            SessionMessage message = messages.get(i);
            builder.append(message.getRole()).append(": ").append(message.getContent()).append("\n");
        }
        // 最新用户输入始终放在最后，确保模型将其视为当前轮次的主问题。
        builder.append("USER: ").append(latestUserInput);
        return builder.toString();
    }
}
