# Warehouse DLQ Handler Plugin

A robust Dead Letter Queue (DLQ) handler plugin for Kafka-based services built with Spring Boot. This plugin provides a comprehensive solution for managing failed messages and their reprocessing.

## Features

1. **Easy Integration**: Enable DLQ functionality by adding the plugin to your Spring Boot application.
2. **Automatic DLQ Topic Management**: DLQ topics are automatically created with the suffix `.DLQ` (configurable).
3. **Batch Processing**: Efficient batch processing of DLQ messages with configurable batch sizes.
4. **Manual Reprocessing**: Expose REST endpoints for manual triggering of DLQ message reprocessing.
5. **Configurable Topics**: Define which topics should have DLQ functionality via `application.yml` or `application.properties`.
6. **Error Handling**: Comprehensive error handling with support for custom error handlers.
7. **Non-Intrusive**: Designed to work alongside your existing Kafka configuration.

## Modules

The plugin is structured into the following modules:

* `warehouse-dlq-handler-plugin-config`: Contains core configuration classes and auto-configuration setup for Spring Boot integration.
* `warehouse-dlq-handler-plugin-kafka`: Provides Kafka-specific implementations including:
  - DLQ reprocessing logic
  - Error handlers
  - Kafka consumer/producer configurations
  - REST controllers for manual reprocessing
* `warehouse-dlq-handler-plugin-properties`: Manages configuration properties including:
  - DLQ topic configurations
  - Retry settings

## How to Use

1. **Add Dependencies**: Include the plugin dependencies in your `pom.xml`:

    ```xml
    <dependency>
        <groupId>com.warehouse</groupId>
        <artifactId>warehouse-dlq-handler-plugin-kafka</artifactId>
        <version>${dlq-plugin.version}</version>
    </dependency>
    <dependency>
        <groupId>com.warehouse</groupId>
        <artifactId>warehouse-dlq-handler-plugin-config</artifactId>
        <version>${dlq-plugin.version}</version>
    </dependency>
    ```

2. ⚙️ **Configure Properties**: Set up DLQ properties in your `application.yml`:

    ```yaml
    dlq:
      handler:
        enabled: true
        dead-letter-suffix: .DLQ
        topics:
          - topicName: test.topic-5
            enabled: true
            exceptionClasses:
              - java.lang.IllegalArgumentException
              - java.lang.IllegalStateException
          - topicName: another-topic
            enabled: true
            exceptionClasses:
              - java.lang.NullPointerException
    ```

## API Endpoints

The plugin exposes the following REST endpoints:

* `GET /api/v1/dlq/topics`: Get a list of all configured DLQ topics
* `POST /api/v1/dlq/reprocess`: Reprocess messages from all DLQ topics
  * Optional query parameter: `batchSize` (default: 50)
* `POST /api/v1/dlq/reprocess/{topic}`: Reprocess messages from a specific DLQ topic
  * Path parameter: `topic` - The DLQ topic name
  * Optional query parameter: `batchSize` (default: 50)

Each endpoint returns a JSON response with the operation status and relevant details.

## Batch Processing

The plugin supports efficient batch processing of DLQ messages:

1. Messages are collected in batches of configurable size
2. Each batch is processed atomically

## Building from Source

To build the plugin, run:

```bash
   mvn clean install
```

## Contributing

1. Fork the repository
2. Create your feature branch
3. Commit your changes
4. Push to the branch
5. Create a Pull Request

---

### Configuration Properties Reference

| Property | Description | Default         |
|----------|-------------|-----------------|
| `dlq.handler.enabled` | Enable/disable DLQ handling | `true`          |
| `dlq.handler.dead-letter-suffix` | Suffix for DLQ topics | `.DLQ`          |
| `dlq.handler.topics[].topicName` | Name of the Kafka topic | `test.topic.name` |
| `dlq.handler.topics[].enabled` | Enable/disable DLQ for this topic | `true`          |
| `dlq.handler.topics[].exceptionClasses` | List of exception classes to handle | `[]`            |