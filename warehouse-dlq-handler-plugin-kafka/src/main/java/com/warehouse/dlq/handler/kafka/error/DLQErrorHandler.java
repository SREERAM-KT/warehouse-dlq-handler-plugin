package com.warehouse.dlq.handler.kafka.error;

import com.warehouse.dlq.handler.kafka.service.DLQHandlerService;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.MessageListenerContainer;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class DLQErrorHandler implements CommonErrorHandler {
    private final DLQHandlerService dlqHandlerService;

    public DLQErrorHandler(DLQHandlerService dlqHandlerService) {
        this.dlqHandlerService = dlqHandlerService;
    }

    @Override
    public boolean handleOne(Exception thrownException, ConsumerRecord<?, ?> record, Consumer<?, ?> consumer, MessageListenerContainer container) {
        String topic = record.topic();
        String key = record.key() != null ? record.key().toString() : null;
        String value = record.value() != null ? record.value().toString() : null;
        dlqHandlerService.handleDLQMessage(topic, key, value, thrownException);
        
        return true;
    }

    @Override
    public void handleOtherException(Exception thrownException, Consumer<?, ?> consumer, MessageListenerContainer container, boolean batchListener) {
        log.error("Error in Kafka consumer: {}", thrownException.getMessage(), thrownException);
    }

} 