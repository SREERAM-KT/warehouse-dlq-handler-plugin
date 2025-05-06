package com.warehouse.dlq.handler.kafka.model;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class DLQMessage {
    private String topic;
    private String key;
    private String value;
    private String exception;
    private String exceptionMessage;
    private int retryCount;
    private Instant timestamp;
    private String originalTopic;
} 