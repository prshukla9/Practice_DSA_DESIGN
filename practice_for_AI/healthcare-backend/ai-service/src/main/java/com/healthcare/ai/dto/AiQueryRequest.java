package com.healthcare.ai.dto;

import javax.validation.constraints.NotBlank;

public class AiQueryRequest {

    @NotBlank
    private String patientId;

    @NotBlank
    private String consentId;

    @NotBlank
    private String query;

    private String cacheKey;

    public String getPatientId() { return patientId; }
    public void setPatientId(String patientId) { this.patientId = patientId; }
    public String getConsentId() { return consentId; }
    public void setConsentId(String consentId) { this.consentId = consentId; }
    public String getQuery() { return query; }
    public void setQuery(String query) { this.query = query; }
    public String getCacheKey() { return cacheKey; }
    public void setCacheKey(String cacheKey) { this.cacheKey = cacheKey; }
}
