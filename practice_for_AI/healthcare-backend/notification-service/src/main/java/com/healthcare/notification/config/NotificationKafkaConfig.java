package com.healthcare.notification.config;

import com.healthcare.common.events.DataFetchedEvent;
import com.healthcare.common.events.NotificationEvent;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class NotificationKafkaConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Bean
    public ConsumerFactory<String, DataFetchedEvent> dataFetchedConsumerFactory() {
        Map<String, Object> config = new HashMap<String, Object>();
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ConsumerConfig.GROUP_ID_CONFIG, "notification-service");
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        config.put(JsonDeserializer.TRUSTED_PACKAGES, "com.healthcare.common.events");
        return new DefaultKafkaConsumerFactory<String, DataFetchedEvent>(config,
                new StringDeserializer(),
                new JsonDeserializer<DataFetchedEvent>(DataFetchedEvent.class, false));
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, DataFetchedEvent> dataFetchedKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, DataFetchedEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<String, DataFetchedEvent>();
        factory.setConsumerFactory(dataFetchedConsumerFactory());
        return factory;
    }

    @Bean
    public ConsumerFactory<String, NotificationEvent> notificationConsumerFactory() {
        Map<String, Object> config = new HashMap<String, Object>();
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ConsumerConfig.GROUP_ID_CONFIG, "notification-service");
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        config.put(JsonDeserializer.TRUSTED_PACKAGES, "com.healthcare.common.events");
        return new DefaultKafkaConsumerFactory<String, NotificationEvent>(config,
                new StringDeserializer(),
                new JsonDeserializer<NotificationEvent>(NotificationEvent.class, false));
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, NotificationEvent> notificationKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, NotificationEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<String, NotificationEvent>();
        factory.setConsumerFactory(notificationConsumerFactory());
        return factory;
    }
}
