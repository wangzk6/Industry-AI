package com.kelvin.industry.aili;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.function.BiFunction;

/**
 * 实时天气查询工具：调用高德天气 API 获取指定城市当前天气
 */
@Component
@Slf4j
public class WeatherForLocationTool implements BiFunction<WeatherForLocationTool.WeatherRequest, ToolContext, String> {

    public static class WeatherRequest {
        public String city;
    }

    private static final String BASE_URL = "https://restapi.amap.com/v3/weather/weatherInfo";

    @Value("${weather.api-key}")
    private String apiKey;

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String apply(WeatherRequest input, ToolContext toolContext) {
        String city = (input == null || input.city == null || input.city.isBlank()) ? "Beijing" : input.city;
        try {
            log.info("开始执行天气信息查询, city={}", city);
            String encodedCity = URLEncoder.encode(city, StandardCharsets.UTF_8);
            String url = String.format(
                    "%s?city=%s&key=%s&extensions=base&output=JSON",
                    BASE_URL,
                    encodedCity,
                    apiKey
            );

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();

            HttpResponse<String> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                return "无法获取 " + city + " 的天气，HTTP 状态码: " + response.statusCode();
            }

            JsonNode root = objectMapper.readTree(response.body());
            if (!"1".equals(root.path("status").asText())) {
                return "查询 " + city + " 天气失败：" + root.path("info").asText("未知错误");
            }

            JsonNode lives = root.path("lives");
            if (!lives.isArray() || lives.isEmpty()) {
                return "未查询到 " + city + " 的天气信息";
            }

            JsonNode live = lives.get(0);
            String weather = live.path("weather").asText();
            String temperature = live.path("temperature").asText();
            String humidity = live.path("humidity").asText();
            String windDirection = live.path("winddirection").asText();
            String windPower = live.path("windpower").asText();
            String reportTime = live.path("reporttime").asText();
            String realCity = live.path("city").asText(city);

            return String.format(
                    "%s 当前天气：%s，气温 %s°C，湿度 %s%%，风向 %s，风力 %s 级，发布时间 %s",
                    realCity, weather, temperature, humidity, windDirection, windPower, reportTime
            );
        } catch (Exception e) {
            return "查询 " + city + " 天气时出错：" + e.getMessage();
        }
    }
}
