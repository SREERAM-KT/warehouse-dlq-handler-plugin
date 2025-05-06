package com.warehouse.dlq.handler.kafka.error;

import com.warehouse.dlq.handler.kafka.service.DLQHandlerService;
import lombok.RequiredArgsConstructor;
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
@RequiredArgsConstructor
public class DLQErrorHandler implements CommonErrorHandler {
    private final DLQHandlerService dlqHandlerService;
    private final Map<String, Integer> retryCountMap = new ConcurrentHashMap<>();

    @Override
    public boolean handleOne(Exception thrownException, ConsumerRecord<?, ?> record, Consumer<?, ?> consumer, MessageListenerContainer container) {
        String topic = record.topic();
        String key = record.key() != null ? record.key().toString() : null;
        String value = record.value() != null ? record.value().toString() : null;

        // Get and increment retry count for this record
        String recordKey = topic + ":" + record.partition() + ":" + record.offset();
        int retryCount = retryCountMap.compute(recordKey, (k, v) -> v == null ? 1 : v + 1);

        // Handle the DLQ logic
        dlqHandlerService.handleDLQMessage(topic, key, value, thrownException, retryCount);

        // If we've exceeded max retries, remove from retry map
        if (retryCount >= 3) { // Default max retries, can be configured
            retryCountMap.remove(recordKey);
        }
        
        return true; // Return true to indicate we've handled the error
    }

    @Override
    public void handleOtherException(Exception thrownException, Consumer<?, ?> consumer, MessageListenerContainer container, boolean batchListener) {
        log.error("Error in Kafka consumer: {}", thrownException.getMessage(), thrownException);
    }
} 