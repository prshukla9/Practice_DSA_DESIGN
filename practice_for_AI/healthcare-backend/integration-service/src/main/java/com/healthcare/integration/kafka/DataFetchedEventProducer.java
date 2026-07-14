package com.healthcare.integration.kafka;

import com.healthcare.common.events.DataFetchedEvent;
import com.healthcare.common.kafka.KafkaTopics;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class DataFetchedEventProducer {

    private final KafkaTemplate<String, DataFetchedEvent> kafkaTemplate;

    public DataFetchedEventProducer(KafkaTemplate<String, DataFetchedEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publish(DataFetchedEvent event) {
        kafkaTemplate.send(KafkaTopics.DATA_FETCHED, event.getPatientId(), event);
    }
}
