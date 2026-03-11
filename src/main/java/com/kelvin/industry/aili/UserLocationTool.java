package com.kelvin.industry.aili;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.stereotype.Component;

import java.util.function.BiFunction;

/**
 * 用户位置工具：根据上下文中的 user_id 返回用户所在城市
 */
@Component
@Slf4j
public class UserLocationTool implements BiFunction<UserLocationTool.UserLocationRequest, ToolContext, String> {

    public static class UserLocationRequest {
        public String query;
    }

    @Override
    public String apply(UserLocationRequest input, ToolContext toolContext) {
        log.info("开始执行用户信息查询, query={}", input == null ? null : input.query);
        String userId = null;
        if (toolContext != null && toolContext.getContext() != null) {
            Object id = toolContext.getContext().get("user_id");
            if (id != null) {
                userId = id.toString();
            }
        }
        return "2".equals(userId) ? "Shanghai" : "Beijing";
    }
}
