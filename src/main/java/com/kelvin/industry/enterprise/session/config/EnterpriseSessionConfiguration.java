package com.kelvin.industry.enterprise.session.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
@EnableConfigurationProperties(EnterpriseSessionProperties.class)
public class EnterpriseSessionConfiguration {
}
