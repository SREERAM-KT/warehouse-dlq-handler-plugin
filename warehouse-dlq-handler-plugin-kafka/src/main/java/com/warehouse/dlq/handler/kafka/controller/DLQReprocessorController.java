package com.warehouse.dlq.handler.kafka.controller;

import com.warehouse.dlq.handler.kafka.service.DLQReprocessor;
import com.warehouse.dlq.handler.properties.DLQProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "warehouse.dlq.rest-api", name = "enabled", havingValue = "true", matchIfMissing = true)
public class DLQReprocessorController {
    private final DLQReprocessor dlqReprocessor;
    private final DLQProperties dlqProperties;

    @GetMapping("${warehouse.dlq.rest-api.base-path}/topics")
    public ResponseEntity<List<String>> getDLQTopics() {
        log.info("Fetching all DLQ topics");
        return ResponseEntity.ok(dlqReprocessor.getDLQTopics());
    }

    @PostMapping("${warehouse.dlq.rest-api.base-path}/reprocess")
    public ResponseEntity<Map<String, Object>> reprocessAllTopics(
            @RequestParam(name = "batchSize", defaultValue = "50") int batchSize) {
        log.info("Triggering reprocessing for all DLQ topics with batch size: {}", batchSize);
        int reprocessedCount = dlqReprocessor.reprocessMessages(batchSize);
        return ResponseEntity.ok(Map.of(
            "status", "success",
            "reprocessedCount", reprocessedCount,
            "batchSize", batchSize
        ));
    }

    @PostMapping("${warehouse.dlq.rest-api.base-path}/reprocess/{topic}")
    public ResponseEntity<Map<String, Object>> reprocessTopic(
            @PathVariable(name = "topic") String topic,
            @RequestParam(name = "batchSize", defaultValue = "50") int batchSize) {
        log.info("Triggering reprocessing for DLQ topic: {} with batch size: {}", topic, batchSize);
        int reprocessedCount = dlqReprocessor.reprocessMessagesFromTopic(topic, batchSize);
        return ResponseEntity.ok(Map.of(
            "status", "success",
            "topic", topic,
            "reprocessedCount", reprocessedCount,
            "batchSize", batchSize
        ));
    }
} 