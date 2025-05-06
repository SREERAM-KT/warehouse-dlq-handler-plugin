package com.warehouse.dlq.handler.config;

import com.warehouse.dlq.handler.properties.DLQProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(DLQProperties.class)
public class DLQConfiguration {
}
