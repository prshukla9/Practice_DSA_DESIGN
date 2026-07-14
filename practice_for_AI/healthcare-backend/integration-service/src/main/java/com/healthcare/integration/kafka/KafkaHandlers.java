package com.healthcare.integration.kafka;

import com.healthcare.common.events.ConsentApprovedEvent;
import com.healthcare.common.kafka.KafkaTopics;
import com.healthcare.integration.dto.FetchExternalDataRequest;
import com.healthcare.integration.service.ExternalDataService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class ConsentApprovedConsumer {

    private final ExternalDataService externalDataService;

    public ConsentApprovedConsumer(ExternalDataService externalDataService) {
        this.externalDataService = externalDataService;
    }

    @KafkaListener(topics = KafkaTopics.CONSENT_APPROVED, groupId = "integration-service")
    public void onConsentApproved(ConsentApprovedEvent event) {
        if (event.getExternalPatientId() == null) {
            return;
        }
        FetchExternalDataRequest request = new FetchExternalDataRequest();
        request.setPatientId(event.getPatientId());
        request.setConsentId(event.getConsentId());
        request.setExternalPatientId(event.getExternalPatientId());
        request.setResourceType("Observation");
        externalDataService.fetchWithConsent(request);
    }
}
