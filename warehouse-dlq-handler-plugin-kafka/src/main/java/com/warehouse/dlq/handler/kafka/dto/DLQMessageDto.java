package com.warehouse.dlq.handler.kafka.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class DLQMessageDto {
    private String topic;
    private String key;
    private String value;
    private String exception;
    private String exceptionMessage;
    private int retryCount;
    private Instant timestamp;
    private String originalTopic;
} 