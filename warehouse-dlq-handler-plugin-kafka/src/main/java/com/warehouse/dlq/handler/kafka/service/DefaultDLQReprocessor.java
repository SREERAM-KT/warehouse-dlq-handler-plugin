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
        log.info("Starting reprocessing from DLQ topic: {} to original topic: {}", dlqTopic, originalTopic);
        
        KafkaConsumer<String, String> consumer = getOrCreateConsumer(dlqTopic);
        KafkaProducer<String, String> producer = getOrCreateProducer(originalTopic);
        
        try {
            log.info("Subscribing consumer to DLQ topic: {}", dlqTopic);
            consumer.subscribe(Collections.singletonList(dlqTopic));
            int reprocessedCount = 0;
            
            while (reprocessedCount < batchSize) {
                log.debug("Polling for messages from DLQ topic: {}", dlqTopic);
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(5));
                if (records.isEmpty()) {
                    log.info("No more messages found in DLQ topic: {}", dlqTopic);
                    break;
                }
                
                log.info("Found {} messages in DLQ topic: {}", records.count(), dlqTopic);
                for (ConsumerRecord<String, String> record : records) {
                    try {
                        ProducerRecord<String, String> producerRecord = new ProducerRecord<>(
                            originalTopic,
                            record.key(),
                            record.value()
                        );
                        
                        log.info("Republishing message to original topic: {}, key: {}", originalTopic, record.key());
                        producer.send(producerRecord, (metadata, exception) -> {
                            if (exception != null) {
                                log.error("Error sending message to topic: {}", originalTopic, exception);
                            } else {
                                log.info("Successfully sent message to topic: {}, partition: {}, offset: {}", 
                                    originalTopic, metadata.partition(), metadata.offset());
                            }
                        });
                        
                        reprocessedCount++;
                    } catch (Exception e) {
                        log.error("Error processing record from DLQ topic: {}", dlqTopic, e);
                    }
                }
                
                if (reprocessedCount > 0) {
                    log.info("Committing offsets for {} messages", reprocessedCount);
                    consumer.commitSync();
                }
            }
            
            log.info("Successfully reprocessed {} messages from DLQ topic: {}", reprocessedCount, dlqTopic);
            return reprocessedCount;
        } catch (Exception e) {
            log.error("Error reprocessing messages from DLQ topic: {}", dlqTopic, e);
            return 0;
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