package com.warehouse.dlq.handler.properties;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(DLQProperties.class)
public class DLQPropertiesConfiguration {
} 