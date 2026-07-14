package com.healthcare.integration.dto;

import java.time.Instant;

public class FetchExternalDataResponse {
    private String fetchId;
    private String patientId;
    private String consentId;
    private String resourceType;
    private int recordCount;
    private String cacheKey;
    private Instant fetchedAt;
    private String summary;

    public String getFetchId() { return fetchId; }
    public void setFetchId(String fetchId) { this.fetchId = fetchId; }
    public String getPatientId() { return patientId; }
    public void setPatientId(String patientId) { this.patientId = patientId; }
    public String getConsentId() { return consentId; }
    public void setConsentId(String consentId) { this.consentId = consentId; }
    public String getResourceType() { return resourceType; }
    public void setResourceType(String resourceType) { this.resourceType = resourceType; }
    public int getRecordCount() { return recordCount; }
    public void setRecordCount(int recordCount) { this.recordCount = recordCount; }
    public String getCacheKey() { return cacheKey; }
    public void setCacheKey(String cacheKey) { this.cacheKey = cacheKey; }
    public Instant getFetchedAt() { return fetchedAt; }
    public void setFetchedAt(Instant fetchedAt) { this.fetchedAt = fetchedAt; }
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
}
