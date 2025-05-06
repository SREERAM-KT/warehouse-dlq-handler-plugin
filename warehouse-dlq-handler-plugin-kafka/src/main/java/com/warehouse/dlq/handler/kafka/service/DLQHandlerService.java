package com.warehouse.dlq.handler.kafka.service;

import com.warehouse.dlq.handler.kafka.dto.DLQMessageDto;
import com.warehouse.dlq.handler.properties.DLQProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Optional;

@Slf4j
@Component
public class DLQHandlerService {
    private final DLQProperties dlqProperties;
    private final KafkaTemplate<String, String> kafkaTemplate;

    public DLQHandlerService(DLQProperties dlqProperties, KafkaTemplate<String, String> kafkaTemplate) {
        this.dlqProperties = dlqProperties;
        this.kafkaTemplate = kafkaTemplate;
    }

    public void handleDLQMessage(String topic, String key, String value, Exception exception, int retryCount) {
        if (dlqProperties.getTopics().isEmpty()) {
            log.error("DLQProperties is not configured");
            return;
        }

        DLQProperties.TopicConfig topicConfig = findTopicConfig(topic);
        
        if (topicConfig == null || !topicConfig.isEnabled()) {
            log.warn("DLQ handling is not enabled for topic: {}", topic);
            return;
        }

        String exceptionClassName = exception.getCause().getClass().getCanonicalName();

        if(topicConfig.getExceptionClasses().stream()
                .noneMatch(exc -> exc.equals(exceptionClassName))) {
            log.warn("DLQ handling is not enabled for topic: {} with exception {}",
                    topic, exceptionClassName);
        }

        if (retryCount >= topicConfig.getMaxRetryCount()) {
            log.error("Message exceeded max retry count for topic: {}. Max retries: {}, Current retries: {}", 
                     topic, topicConfig.getMaxRetryCount(), retryCount);
            return;
        }

        String dlqTopic = topic + Optional.ofNullable(dlqProperties.getDeadLetterSuffix()).orElse(".DLQ");
        DLQMessageDto dlqMessage = DLQMessageDto.builder()
                .topic(dlqTopic)
                .key(key)
                .value(value)
                .exception(exception.getClass().getName())
                .exceptionMessage(exception.getMessage())
                .retryCount(retryCount)
                .timestamp(Instant.now())
                .originalTopic(topic)
                .build();

        log.error("Sending message to DLQ topic: {}. Exception: {}, Message: {}", 
                 dlqTopic, exception.getMessage(), value);
        
        if (kafkaTemplate == null) {
            log.error("KafkaTemplate is not configured. Cannot send message to DLQ topic: {}", dlqTopic);
            return;
        }

        kafkaTemplate.send(dlqTopic, key, value);
    }

    private DLQProperties.TopicConfig findTopicConfig(String topic) {
        return dlqProperties.getTopics().stream()
                .filter(config -> config.getTopicName().equals(topic))
                .findFirst()
                .orElse(null);
    }
} 