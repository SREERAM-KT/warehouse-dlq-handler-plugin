package com.warehouse.dlq.handler.kafka.error;

import com.warehouse.dlq.handler.kafka.service.DLQHandlerService;
import com.warehouse.dlq.handler.properties.DLQProperties;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.MessageListenerContainer;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class DLQErrorHandler implements CommonErrorHandler {
    private final DLQHandlerService dlqHandlerService;
    private final DLQProperties dlqProperties;
    private final Map<String, Integer> retryCountMap = new ConcurrentHashMap<>();

    public DLQErrorHandler(DLQHandlerService dlqHandlerService, DLQProperties dlqProperties) {
        this.dlqHandlerService = dlqHandlerService;
        this.dlqProperties = dlqProperties;
    }

    @Override
    public boolean handleOne(Exception thrownException, ConsumerRecord<?, ?> record, Consumer<?, ?> consumer, MessageListenerContainer container) {
        String topic = record.topic();
        String key = record.key() != null ? record.key().toString() : null;
        String value = record.value() != null ? record.value().toString() : null;

        String recordKey = topic + ":" + record.partition() + ":" + record.offset();
        int retryCount = retryCountMap.compute(recordKey, (k, v) -> v == null ? 0 : v + 1);

        dlqHandlerService.handleDLQMessage(topic, key, value, thrownException, retryCount);

        int maxRetries = dlqProperties.getTopics().stream()
                .filter(config -> config.getTopicName().equals(topic))
                .findFirst()
                .map(DLQProperties.TopicConfig::getMaxRetryCount)
                .orElse(3);

        if (retryCount >= maxRetries) {
            retryCountMap.remove(recordKey);
            log.warn("Removed record from retry map after exceeding max retries. Topic: {}, Partition: {}, Offset: {}", 
                    topic, record.partition(), record.offset());
        }
        
        return true;
    }

    @Override
    public void handleOtherException(Exception thrownException, Consumer<?, ?> consumer, MessageListenerContainer container, boolean batchListener) {
        log.error("Error in Kafka consumer: {}", thrownException.getMessage(), thrownException);
    }

} 