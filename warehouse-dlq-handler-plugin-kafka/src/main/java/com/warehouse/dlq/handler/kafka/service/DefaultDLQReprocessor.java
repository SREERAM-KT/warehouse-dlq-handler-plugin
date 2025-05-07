package com.warehouse.dlq.handler.kafka.service;

import com.warehouse.dlq.handler.properties.DLQProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class DefaultDLQReprocessor implements DLQReprocessor {
    private final DLQProperties dlqProperties;
    private final ConsumerFactory<String, String> consumerFactory;
    private final ProducerFactory<String, String> producerFactory;
    private final Map<String, KafkaConsumer<String, String>> consumers = new HashMap<>();
    private final Map<String, KafkaProducer<String, String>> producers = new HashMap<>();

    @Override
    public List<String> getDLQTopics() {
        return dlqProperties.getTopics().stream()
                .filter(topic -> topic.isEnabled())
                .map(topic -> topic.getTopicName() + dlqProperties.getDeadLetterSuffix())
                .collect(Collectors.toList());
    }

    @Override
    public int reprocessMessages(int batchSize) {
        List<String> dlqTopics = getDLQTopics();
        log.info("Found {} DLQ topics to reprocess: {}", dlqTopics.size(), dlqTopics);
        
        AtomicInteger totalReprocessed = new AtomicInteger(0);
        
        dlqTopics.forEach(topic -> {
            try {
                int reprocessed = reprocessMessagesFromTopic(topic, batchSize);
                totalReprocessed.addAndGet(reprocessed);
            } catch (Exception e) {
                log.error("Error reprocessing messages from topic: {}", topic, e);
            }
        });
        
        return totalReprocessed.get();
    }

    @Override
    public int reprocessMessagesFromTopic(String dlqTopic, int batchSize) {
        String suffix = dlqProperties.getDeadLetterSuffix() != null ?
          dlqProperties.getDeadLetterSuffix() : ".DLQ";

        if (!dlqTopic.endsWith(suffix)) {
            throw new IllegalArgumentException("Topic must be a DLQ topic (ending with " + suffix + ")");
        }

        String originalTopic = dlqTopic.substring(0, dlqTopic.length() - suffix.length());
        log.info("Starting to reprocess messages from DLQ topic: {} to original topic: {}", dlqTopic, originalTopic);

        KafkaConsumer<String, String> consumer = getOrCreateConsumer(dlqTopic);
        KafkaProducer<String, String> producer = getOrCreateProducer(originalTopic);

        try {
            log.info("Subscribing consumer to DLQ topic: {}", dlqTopic);
            consumer.subscribe(Collections.singletonList(dlqTopic));

            int reprocessedCount = 0;
            boolean hasMoreMessages = true;

            while (hasMoreMessages) {
                log.debug("Polling for messages from DLQ topic: {}", dlqTopic);
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(5));

                if (records.isEmpty()) {
                    log.info("No more messages found in DLQ topic: {}", dlqTopic);
                    hasMoreMessages = false;
                    continue;
                }

                log.info("Found {} messages in DLQ topic: {}", records.count(), dlqTopic);

                // Process records in batches
                List<ConsumerRecord<String, String>> batch = new ArrayList<>();
                for (ConsumerRecord<String, String> record : records) {
                    batch.add(record);

                    if (batch.size() >= batchSize) {
                        processBatchAndGetCount(batch, producer, originalTopic);
                        reprocessedCount += batch.size();
                        batch.clear(); // Clear the batch after processing
                    }
                }

                // Process remaining records in the last batch
                if (!batch.isEmpty()) {
                    processBatchAndGetCount(batch, producer, originalTopic);
                    reprocessedCount += batch.size();
                }

                hasMoreMessages = false;
            }

            log.info("Successfully reprocessed {} messages from DLQ topic: {} to original topic: {}",
              reprocessedCount, dlqTopic, originalTopic);
            return reprocessedCount;
        } catch (Exception e) {
            log.error("Error reprocessing messages from DLQ topic: {}", dlqTopic, e);
            return 0;
        }
    }

    /**
     * Process a batch of records and return the number of successfully processed records.
     * 
     * @param batch The batch of records to process
     * @param producer The Kafka producer to use
     * @param originalTopic The original topic to publish to
     * @return The number of successfully processed records
     * @throws RuntimeException if batch processing fails
     */
    private int processBatchAndGetCount(List<ConsumerRecord<String, String>> batch, 
                                      KafkaProducer<String, String> producer, 
                                      String originalTopic) {
        if (batch.isEmpty()) {
            return 0;
        }

        log.info("Processing batch messages {} of size {}", batch, batch.size());
        
        // Create producer records for the batch
        List<ProducerRecord<String, String>> producerRecords = batch.stream()
            .map(record -> new ProducerRecord<>(originalTopic, record.key(), record.value()))
            .collect(Collectors.toList());
        
        // Track batch completion
        CompletableFuture<Void> batchFuture = new CompletableFuture<>();
        AtomicInteger completedCount = new AtomicInteger(0);
        AtomicInteger failedCount = new AtomicInteger(0);
        
        // Send all messages in the batch
        for (ProducerRecord<String, String> producerRecord : producerRecords) {
            producer.send(producerRecord, (metadata, exception) -> {
                if (exception != null) {
                    log.error("Error sending message to topic: {} - {}", originalTopic, exception.getMessage());
                    failedCount.incrementAndGet();
                    batchFuture.completeExceptionally(exception);
                } else {
                    log.debug("Successfully sent message to topic: {}, partition: {}, offset: {}", 
                        originalTopic, metadata.partition(), metadata.offset());
                    
                    if (completedCount.incrementAndGet() + failedCount.get() == producerRecords.size()) {
                        if (failedCount.get() == 0) {
                            batchFuture.complete(null);
                        }
                    }
                }
            });
        }
        
        try {
            // Wait for batch to complete with timeout
            batchFuture.get(30, TimeUnit.SECONDS);
            int successCount = completedCount.get();
            log.info("Successfully processed {} messages in batch", successCount);
            return successCount;
        } catch (Exception e) {
            log.error("Error processing batch of {} messages", batch.size(), e);
            throw new RuntimeException("Failed to process batch", e);
        }
    }

    private KafkaConsumer<String, String> getOrCreateConsumer(String topic) {
        return consumers.computeIfAbsent(topic, k -> {
            Properties props = new Properties();
            props.putAll(consumerFactory.getConfigurationProperties());
            props.put(ConsumerConfig.GROUP_ID_CONFIG, "dlq-reprocessor-" + topic);
            props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
            props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");
            props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 100);
            
            log.info("Creating new consumer for topic: {} with group: {}", topic, props.get(ConsumerConfig.GROUP_ID_CONFIG));
            return new KafkaConsumer<>(props);
        });
    }

    private KafkaProducer<String, String> getOrCreateProducer(String topic) {
        return producers.computeIfAbsent(topic, k -> {
            Properties props = new Properties();
            props.putAll(producerFactory.getConfigurationProperties());
            props.put(ProducerConfig.ACKS_CONFIG, "all");
            props.put(ProducerConfig.RETRIES_CONFIG, 3);
            props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
            
            log.info("Creating new producer for topic: {}", topic);
            return new KafkaProducer<>(props);
        });
    }
} 