package com.warehouse.dlq.handler.kafka.service;

import com.warehouse.dlq.handler.kafka.model.DLQMessage;
import com.warehouse.dlq.handler.properties.DLQProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class DLQHandlerService {
    private final DLQProperties dlqProperties;
    private final KafkaTemplate<String, String> kafkaTemplate;

    public void handleDLQMessage(String topic, String key, String value, Exception exception, int retryCount) {
        if (dlqProperties == null) {
            log.error("DLQProperties is not configured");
            return;
        }

        DLQProperties.TopicConfig topicConfig = findTopicConfig(topic);
        
        if (topicConfig == null || !topicConfig.isEnabled()) {
            log.warn("DLQ handling is not enabled for topic: {}", topic);
            return;
        }

        if (retryCount >= topicConfig.getMaxRetryCount()) {
            log.error("Message exceeded max retry count for topic: {}. Max retries: {}, Current retries: {}", 
                     topic, topicConfig.getMaxRetryCount(), retryCount);
            return;
        }

        String dlqTopic = topic + dlqProperties.getDeadLetterSuffix();
        DLQMessage dlqMessage = DLQMessage.builder()
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