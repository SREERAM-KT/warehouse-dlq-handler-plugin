package com.warehouse.dlq.handler.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Collections;
import java.util.List;

/**
 * Configuration properties for DLQ handling.
 * This is a singleton bean that is automatically created by Spring Boot
 * and shared across all components that need DLQ configuration.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "warehouse.dlq")
public class DLQProperties {
    private List<TopicConfig> topics = Collections.emptyList();
    private String deadLetterSuffix;
    private RestApi restApi = new RestApi();

    @Getter
    @Setter
    public static class TopicConfig {
        private String topicName;
        private int maxRetryCount;
        private List<String> exceptionClasses = Collections.emptyList();
        private boolean enabled;
        private long defaultRetryBackoffMs;
    }

    @Getter
    @Setter
    public static class RestApi {
        private boolean enabled = true;
        private String basePath = "/api/v1/dlq";
    }
} 