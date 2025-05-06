package com.warehouse.dlq.handler.properties;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import java.util.Collections;
import java.util.List;

@Getter
@Setter
@Validated
@Component
@ConfigurationProperties(prefix = "warehouse.dlq")
public class DLQProperties {
    @NotNull(message = "Topics configuration is required")
    @Valid
    private List<TopicConfig> topics = Collections.emptyList();
    
    @NotNull(message = "Dead letter suffix is required")
    private String deadLetterSuffix;

    @Getter
    @Setter
    @Validated
    public static class TopicConfig {
        @NotEmpty(message = "Topic name is required")
        private String topicName;
        
        @NotNull(message = "Max retry count is required")
        private int maxRetryCount;
        
        @NotEmpty(message = "Exception classes are required")
        private List<String> exceptionClasses = Collections.emptyList();
        
        @NotNull(message = "Enabled flag is required")
        private boolean enabled = false;
        
        @NotNull(message = "Default retry backoff is required")
        private long defaultRetryBackoffMs;
    }
} 