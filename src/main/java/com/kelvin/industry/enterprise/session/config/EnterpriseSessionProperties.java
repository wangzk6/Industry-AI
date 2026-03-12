package com.kelvin.industry.enterprise.session.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "enterprise.agent.session")
public class EnterpriseSessionProperties {

    private Duration ttl = Duration.ofMinutes(30);

    private Duration cleanupInterval = Duration.ofMinutes(5);

    private int maxSessions = 5000;

    private int maxMessagesPerSession = 50;

    private int historyWindow = 20;

    public Duration getTtl() {
        return ttl;
    }

    public void setTtl(Duration ttl) {
        this.ttl = ttl;
    }

    public Duration getCleanupInterval() {
        return cleanupInterval;
    }

    public void setCleanupInterval(Duration cleanupInterval) {
        this.cleanupInterval = cleanupInterval;
    }

    public int getMaxSessions() {
        return maxSessions;
    }

    public void setMaxSessions(int maxSessions) {
        this.maxSessions = maxSessions;
    }

    public int getMaxMessagesPerSession() {
        return maxMessagesPerSession;
    }

    public void setMaxMessagesPerSession(int maxMessagesPerSession) {
        this.maxMessagesPerSession = maxMessagesPerSession;
    }

    public int getHistoryWindow() {
        return historyWindow;
    }

    public void setHistoryWindow(int historyWindow) {
        this.historyWindow = historyWindow;
    }
}
