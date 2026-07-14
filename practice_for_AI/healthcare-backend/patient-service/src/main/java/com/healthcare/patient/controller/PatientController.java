package com.healthcare.patient.controller;

import com.healthcare.patient.dto.MapIdentityRequest;
import com.healthcare.patient.dto.PatientResponse;
import com.healthcare.patient.dto.RegisterPatientRequest;
import com.healthcare.patient.service.PatientService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/patients")
public class PatientController {

    private final PatientService patientService;

    public PatientController(PatientService patientService) {
        this.patientService = patientService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PatientResponse register(@Valid @RequestBody RegisterPatientRequest request) {
        return patientService.register(request);
    }

    @GetMapping("/{id}")
    public PatientResponse getPatient(@PathVariable UUID id) {
        return patientService.getById(id);
    }

    @PutMapping("/{id}/identity")
    public PatientResponse mapIdentity(@PathVariable UUID id,
                                       @Valid @RequestBody MapIdentityRequest request) {
        return patientService.mapExternalIdentity(id, request);
    }
}
