//package com.warehouse.dlq.handler.kafka.config;
//
//import com.warehouse.dlq.handler.kafka.error.DLQErrorHandler;
//import com.warehouse.dlq.handler.kafka.service.DLQHandlerService;
//import com.warehouse.dlq.handler.properties.DLQProperties;
//import com.warehouse.dlq.handler.properties.DLQPropertiesConfiguration;
//import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
//import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
//import org.springframework.boot.ssl.SslBundles;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.context.annotation.Import;
//import org.springframework.kafka.annotation.EnableKafka;
//import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
//import org.springframework.kafka.core.ConsumerFactory;
//import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
//import org.springframework.kafka.core.DefaultKafkaProducerFactory;
//import org.springframework.kafka.core.KafkaTemplate;
//import org.springframework.kafka.core.ProducerFactory;
//
//@Configuration
//@EnableKafka
//@Import(DLQPropertiesConfiguration.class)
//@ConditionalOnProperty(prefix = "warehouse.dlq", name = "enabled", havingValue = "true", matchIfMissing = true)
//public class DLQAutoConfiguration {
//
//    @Bean
//    @ConditionalOnProperty(name = "spring.kafka.bootstrap-servers")
//    public ConsumerFactory<String, String> consumerFactory(KafkaProperties kafkaProperties, SslBundles sslBundles) {
//        return new DefaultKafkaConsumerFactory<>(kafkaProperties.getConsumer().buildProperties(sslBundles));
//    }
//
//    @Bean
//    @ConditionalOnProperty(name = "spring.kafka.bootstrap-servers")
//    public ProducerFactory<String, String> producerFactory(KafkaProperties kafkaProperties, SslBundles sslBundles) {
//        return new DefaultKafkaProducerFactory<>(kafkaProperties.getProducer().buildProperties(sslBundles));
//    }
//
//    @Bean
//    @ConditionalOnProperty(name = "spring.kafka.bootstrap-servers")
//    public KafkaTemplate<String, String> kafkaTemplate(ProducerFactory<String, String> producerFactory) {
//        return new KafkaTemplate<>(producerFactory);
//    }
//
//    @Bean
//    public DLQHandlerService dlqHandlerService(DLQProperties dlqProperties, KafkaTemplate<String, String> kafkaTemplate) {
//        return new DLQHandlerService(dlqProperties, kafkaTemplate);
//    }
//
//    @Bean
//    public DLQErrorHandler dlqErrorHandler(DLQHandlerService dlqHandlerService, DLQProperties dlqProperties) {
//        return new DLQErrorHandler(dlqHandlerService, dlqProperties);
//    }
//
//    @Bean
//    @ConditionalOnProperty(name = "spring.kafka.bootstrap-servers")
//    public ConcurrentKafkaListenerContainerFactory<String, String> kafkaListenerContainerFactory(
//            ConsumerFactory<String, String> consumerFactory,
//            DLQErrorHandler dlqErrorHandler) {
//        ConcurrentKafkaListenerContainerFactory<String, String> factory = new ConcurrentKafkaListenerContainerFactory<>();
//        factory.setConsumerFactory(consumerFactory);
//        factory.setCommonErrorHandler(dlqErrorHandler);
//        return factory;
//    }
//}