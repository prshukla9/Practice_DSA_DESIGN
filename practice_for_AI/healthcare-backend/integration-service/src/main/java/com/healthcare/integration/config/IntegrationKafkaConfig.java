package com.healthcare.integration.config;

import com.healthcare.common.events.ConsentApprovedEvent;
import com.healthcare.common.events.DataFetchedEvent;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class IntegrationKafkaConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Bean
    public ConsumerFactory<String, ConsentApprovedEvent> consentConsumerFactory() {
        Map<String, Object> config = new HashMap<String, Object>();
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ConsumerConfig.GROUP_ID_CONFIG, "integration-service");
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        config.put(JsonDeserializer.TRUSTED_PACKAGES, "com.healthcare.common.events");
        return new DefaultKafkaConsumerFactory<String, ConsentApprovedEvent>(config,
                new StringDeserializer(),
                new JsonDeserializer<ConsentApprovedEvent>(ConsentApprovedEvent.class, false));
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, ConsentApprovedEvent> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, ConsentApprovedEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<String, ConsentApprovedEvent>();
        factory.setConsumerFactory(consentConsumerFactory());
        return factory;
    }

    @Bean
    public ProducerFactory<String, DataFetchedEvent> dataFetchedProducerFactory() {
        Map<String, Object> config = new HashMap<String, Object>();
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        return new DefaultKafkaProducerFactory<String, DataFetchedEvent>(config);
    }

    @Bean
    public KafkaTemplate<String, DataFetchedEvent> dataFetchedKafkaTemplate() {
        return new KafkaTemplate<String, DataFetchedEvent>(dataFetchedProducerFactory());
    }
}
