package com.warehouse.dlq.handler.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "warehouse.dlq")
public class DLQProperties {
    private List<TopicConfig> topics;
    private String deadLetterSuffix;

    @Getter
    @Setter
    public static class TopicConfig {
        private String topicName;
        private int maxRetryCount;
        private List<String> exceptionClasses = Collections.emptyList();;
        private boolean enabled = false;
        private long defaultRetryBackoffMs;
    }
} 