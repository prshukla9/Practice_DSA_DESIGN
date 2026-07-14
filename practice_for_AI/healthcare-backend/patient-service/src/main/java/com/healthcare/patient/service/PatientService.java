package com.healthcare.patient.service;

import com.healthcare.patient.dto.MapIdentityRequest;
import com.healthcare.patient.dto.PatientResponse;
import com.healthcare.patient.dto.RegisterPatientRequest;
import com.healthcare.patient.entity.Patient;
import com.healthcare.patient.repository.PatientRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class PatientService {

    private final PatientRepository patientRepository;

    public PatientService(PatientRepository patientRepository) {
        this.patientRepository = patientRepository;
    }

    @Transactional
    public PatientResponse register(RegisterPatientRequest request) {
        patientRepository.findByEmail(request.getEmail()).ifPresent(p -> {
            throw new IllegalArgumentException("Patient already exists with email: " + request.getEmail());
        });

        Patient patient = new Patient();
        patient.setFullName(request.getFullName());
        patient.setEmail(request.getEmail());
        patient.setPhone(request.getPhone());
        patient.setExternalPatientId(request.getExternalPatientId());
        patient.setIdentityVerified(request.getExternalPatientId() != null);
        patient.setCreatedAt(Instant.now());

        return toResponse(patientRepository.save(patient));
    }

    @Transactional(readOnly = true)
    public PatientResponse getById(UUID id) {
        return patientRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new IllegalArgumentException("Patient not found: " + id));
    }

    @Transactional
    public PatientResponse mapExternalIdentity(UUID id, MapIdentityRequest request) {
        Patient patient = patientRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Patient not found: " + id));

        patient.setExternalPatientId(request.getExternalPatientId());
        patient.setIdentityVerified(true);
        return toResponse(patientRepository.save(patient));
    }

    private PatientResponse toResponse(Patient patient) {
        PatientResponse response = new PatientResponse();
        response.setId(patient.getId());
        response.setFullName(patient.getFullName());
        response.setEmail(patient.getEmail());
        response.setPhone(patient.getPhone());
        response.setExternalPatientId(patient.getExternalPatientId());
        response.setIdentityVerified(patient.isIdentityVerified());
        response.setCreatedAt(patient.getCreatedAt());
        return response;
    }
}
