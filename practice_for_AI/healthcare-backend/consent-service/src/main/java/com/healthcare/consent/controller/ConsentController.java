package com.healthcare.consent.controller;

import com.healthcare.consent.dto.ConsentRequestDto;
import com.healthcare.consent.dto.ConsentResponseDto;
import com.healthcare.consent.service.ConsentService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/consents")
public class ConsentController {

    private final ConsentService consentService;

    public ConsentController(ConsentService consentService) {
        this.consentService = consentService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ConsentResponseDto requestConsent(@Valid @RequestBody ConsentRequestDto request) {
        return consentService.requestConsent(request);
    }

    @PostMapping("/{id}/approve")
    public ConsentResponseDto approve(@PathVariable UUID id) {
        return consentService.approveConsent(id);
    }

    @GetMapping("/{id}/validate")
    public ConsentResponseDto validate(@PathVariable UUID id) {
        return consentService.validateActiveConsent(id);
    }
}
