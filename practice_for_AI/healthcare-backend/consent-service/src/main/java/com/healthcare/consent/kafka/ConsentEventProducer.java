package com.healthcare.consent.kafka;

import com.healthcare.common.events.ConsentApprovedEvent;
import com.healthcare.common.kafka.KafkaTopics;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class ConsentEventProducer {

    private final KafkaTemplate<String, ConsentApprovedEvent> kafkaTemplate;

    public ConsentEventProducer(KafkaTemplate<String, ConsentApprovedEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishConsentApproved(ConsentApprovedEvent event) {
        kafkaTemplate.send(KafkaTopics.CONSENT_APPROVED, event.getPatientId(), event);
    }
}
