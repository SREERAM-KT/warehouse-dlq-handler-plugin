package com.warehouse.dlq.handler.kafka.service;

import java.util.List;

/**
 * Interface for reprocessing messages from DLQ topics back to their original topics
 */
public interface DLQReprocessor {
    
    /**
     * Reprocess messages from DLQ topics in batches
     * @param batchSize The number of messages to process in each batch
     * @return The number of messages successfully reprocessed
     */
    int reprocessMessages(int batchSize);
    
    /**
     * Reprocess messages from a specific DLQ topic in batches
     * @param dlqTopic The DLQ topic to reprocess messages from
     * @param batchSize The number of messages to process in each batch
     * @return The number of messages successfully reprocessed
     */
    int reprocessMessagesFromTopic(String dlqTopic, int batchSize);
    
    /**
     * Get a list of all DLQ topics that can be reprocessed
     * @return List of DLQ topic names
     */
    List<String> getDLQTopics();
} 