package com.ordersaga.payment.infrastructure.kafka;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaOperations;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JacksonJsonDeserializer;
import org.springframework.kafka.support.serializer.JacksonJsonSerializer;

import java.util.HashMap;
import java.util.Map;

@Configuration(proxyBeanMethods = false)
@EnableKafka
public class PaymentKafkaConfiguration {

    @Bean
    ProducerFactory<Object, Object> producerFactory(Environment environment) {
        Map<String, Object> properties = new HashMap<>();
        properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, environment.getRequiredProperty("spring.kafka.bootstrap-servers"));
        properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JacksonJsonSerializer.class);
        return new DefaultKafkaProducerFactory<>(properties);
    }

    @Bean
    ConsumerFactory<Object, Object> consumerFactory(Environment environment) {
        Map<String, Object> properties = new HashMap<>();
        properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, environment.getRequiredProperty("spring.kafka.bootstrap-servers"));
        properties.put(ConsumerConfig.GROUP_ID_CONFIG, environment.getRequiredProperty("spring.kafka.consumer.group-id"));
        properties.put(
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,
                environment.getProperty("spring.kafka.consumer.auto-offset-reset", "earliest")
        );
        properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JacksonJsonDeserializer.class);
        properties.put(
                JacksonJsonDeserializer.TRUSTED_PACKAGES,
                environment.getProperty("spring.kafka.consumer.properties.spring.json.trusted.packages", "com.ordersaga.saga.event")
        );
        return new DefaultKafkaConsumerFactory<>(properties);
    }

    @Bean
    ConcurrentKafkaListenerContainerFactory<Object, Object> kafkaListenerContainerFactory(
            ConsumerFactory<Object, Object> consumerFactory
    ) {
        ConcurrentKafkaListenerContainerFactory<Object, Object> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        return factory;
    }

    @Bean
    KafkaOperations<Object, Object> kafkaOperations(ProducerFactory<Object, Object> producerFactory) {
        return new KafkaTemplate<>(producerFactory);
    }
}
