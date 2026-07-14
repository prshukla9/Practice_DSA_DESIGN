package com.healthcare.integration.dto;

import javax.validation.constraints.NotBlank;

public class FetchExternalDataRequest {

    @NotBlank
    private String patientId;

    @NotBlank
    private String consentId;

    @NotBlank
    private String externalPatientId;

    @NotBlank
    private String resourceType;

    public String getPatientId() { return patientId; }
    public void setPatientId(String patientId) { this.patientId = patientId; }
    public String getConsentId() { return consentId; }
    public void setConsentId(String consentId) { this.consentId = consentId; }
    public String getExternalPatientId() { return externalPatientId; }
    public void setExternalPatientId(String externalPatientId) { this.externalPatientId = externalPatientId; }
    public String getResourceType() { return resourceType; }
    public void setResourceType(String resourceType) { this.resourceType = resourceType; }
}
