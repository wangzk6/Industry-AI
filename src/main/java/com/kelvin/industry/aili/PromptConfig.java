package com.kelvin.industry.aili;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Agent 配置：注册两个 ToolCallback Bean 并组装 ChatClient
 */
@Configuration
public class PromptConfig {

    static final String WEATHER_PROMPT = """
            You are a helpful weather assistant who speaks Chinese.
            
            You have access to two tools:
            - get_weather_for_location: use this to get the real-time weather for a specific city
            - get_user_location: use this to determine the user's current city
            
            When a user asks about the weather without specifying a city,
            call get_user_location first to find their city, then call get_weather_for_location.
            Always reply in Chinese and include specific temperature and weather details.
            """;

    @Bean
    public ToolCallback getUserLocationTool(UserLocationTool userLocationTool) {
        return FunctionToolCallback
                .builder("get_user_location", userLocationTool)
                .description("Get the user's current city. Input JSON: {\"query\": \"...\"}")
                .inputType(UserLocationTool.UserLocationRequest.class)
                .build();
    }

    @Bean
    public ToolCallback getWeatherForLocationTool(WeatherForLocationTool weatherForLocationTool) {
        return FunctionToolCallback
                .builder("get_weather_for_location", weatherForLocationTool)
                .description("Get real-time weather by city. Input JSON: {\"city\": \"北京\"}")
                .inputType(WeatherForLocationTool.WeatherRequest.class)
                .build();
    }

    @Bean
    public ChatClient weatherChatClient(ChatModel chatModel,
                                        ToolCallback getUserLocationTool,
                                        ToolCallback getWeatherForLocationTool) {
        return ChatClient.builder(chatModel)
                .defaultSystem(WEATHER_PROMPT)
                .defaultAdvisors(new SimpleLoggerAdvisor())
                .defaultToolCallbacks(List.of(getUserLocationTool, getWeatherForLocationTool))
                .build();
    }
}
