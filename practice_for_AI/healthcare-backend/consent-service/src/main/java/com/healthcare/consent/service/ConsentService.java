package com.healthcare.consent.service;

import com.healthcare.common.events.ConsentApprovedEvent;
import com.healthcare.consent.dto.ConsentRequestDto;
import com.healthcare.consent.dto.ConsentResponseDto;
import com.healthcare.consent.entity.Consent;
import com.healthcare.consent.kafka.ConsentEventProducer;
import com.healthcare.consent.repository.ConsentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
public class ConsentService {

    private final ConsentRepository consentRepository;
    private final ConsentEventProducer consentEventProducer;

    public ConsentService(ConsentRepository consentRepository, ConsentEventProducer consentEventProducer) {
        this.consentRepository = consentRepository;
        this.consentEventProducer = consentEventProducer;
    }

    @Transactional
    public ConsentResponseDto requestConsent(ConsentRequestDto request) {
        Consent consent = new Consent();
        consent.setPatientId(request.getPatientId());
        consent.setExternalPatientId(request.getExternalPatientId());
        consent.setPurpose(request.getPurpose());
        consent.setStatus(Consent.Status.PENDING);
        consent.setExpiresAt(Instant.now().plus(24, ChronoUnit.HOURS));
        consent.setCreatedAt(Instant.now());
        return toResponse(consentRepository.save(consent));
    }

    @Transactional
    public ConsentResponseDto approveConsent(UUID consentId) {
        Consent consent = consentRepository.findById(consentId)
                .orElseThrow(() -> new IllegalArgumentException("Consent not found: " + consentId));

        if (consent.getStatus() != Consent.Status.PENDING) {
            throw new IllegalStateException("Consent is not pending");
        }

        consent.setStatus(Consent.Status.APPROVED);
        consent.setApprovedAt(Instant.now());
        Consent saved = consentRepository.save(consent);

        consentEventProducer.publishConsentApproved(new ConsentApprovedEvent(
                saved.getId().toString(),
                saved.getPatientId(),
                saved.getExternalPatientId(),
                saved.getPurpose(),
                saved.getApprovedAt()
        ));

        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public ConsentResponseDto validateActiveConsent(UUID consentId) {
        Consent consent = consentRepository.findByIdAndStatus(consentId, Consent.Status.APPROVED)
                .orElseThrow(() -> new IllegalArgumentException("Active consent not found: " + consentId));

        if (consent.getExpiresAt() != null && consent.getExpiresAt().isBefore(Instant.now())) {
            throw new IllegalStateException("Consent expired");
        }
        return toResponse(consent);
    }

    private ConsentResponseDto toResponse(Consent consent) {
        ConsentResponseDto dto = new ConsentResponseDto();
        dto.setId(consent.getId());
        dto.setPatientId(consent.getPatientId());
        dto.setExternalPatientId(consent.getExternalPatientId());
        dto.setPurpose(consent.getPurpose());
        dto.setStatus(consent.getStatus().name());
        dto.setExpiresAt(consent.getExpiresAt());
        dto.setApprovedAt(consent.getApprovedAt());
        dto.setCreatedAt(consent.getCreatedAt());
        return dto;
    }
}
