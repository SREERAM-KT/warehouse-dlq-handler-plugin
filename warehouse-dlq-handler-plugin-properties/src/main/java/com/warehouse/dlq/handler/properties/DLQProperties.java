package com.warehouse.dlq.handler.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Collections;
import java.util.List;

@Getter
@Setter
@ConfigurationProperties(prefix = "warehouse.dlq")
public class DLQProperties {
    private List<TopicConfig> topics = Collections.emptyList();
    private String deadLetterSuffix;

    @Getter
    @Setter
    public static class TopicConfig {
        private String topicName;
        private int maxRetryCount;
        private List<String> exceptionClasses = Collections.emptyList();
        private boolean enabled;
        private long defaultRetryBackoffMs;
    }
} 