package com.healthcare.integration.controller;

import com.healthcare.integration.dto.FetchExternalDataRequest;
import com.healthcare.integration.dto.FetchExternalDataResponse;
import com.healthcare.integration.service.ExternalDataService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/v1/integration")
public class IntegrationController {

    private final ExternalDataService externalDataService;

    public IntegrationController(ExternalDataService externalDataService) {
        this.externalDataService = externalDataService;
    }

    @PostMapping("/fetch")
    public FetchExternalDataResponse fetchExternalData(@Valid @RequestBody FetchExternalDataRequest request) {
        return externalDataService.fetchWithConsent(request);
    }
}
