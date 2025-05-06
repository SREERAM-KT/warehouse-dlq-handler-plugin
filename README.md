# 📦 Warehouse Kafka DLQ Plugin

This plugin enables retry and Dead Letter Queue (DLQ) support for Kafka consumers within the Warehouse service.  
It ensures failed messages are retried and, upon exhaustion, redirected to a DLQ topic for further inspection or reprocessing.

---

## 👥 Contributors

- [Mahipathi Yugandhar](https://github.com/YUGANDHAR-3)
- [Sreeram KT](https://github.com/SREERAM-KT)

## ⚙️ Required Configuration Properties

Add the following properties to your `application.yml` or `application.properties` file to enable DLQ handling:

```properties
# Suffix added to the original topic name to create the DLQ topic
warehouse.dlq.deadLetterSuffix=.DLQ

# Configuration for the first Kafka topic
warehouse.dlq.topics[0].topicName=test-topic
warehouse.dlq.topics[0].enabled=true
warehouse.dlq.topics[0].maxRetryCount=3
warehouse.dlq.topics[0].defaultRetryBackoffMs=1000
warehouse.dlq.topics[0].exceptionClasses[0]=java.lang.IllegalArgumentException