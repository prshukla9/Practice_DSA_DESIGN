package com.healthcare.common.events;

import java.io.Serializable;
import java.time.Instant;

public class DataFetchedEvent implements Serializable {
    private String fetchId;
    private String patientId;
    private String consentId;
    private String resourceType;
    private int recordCount;
    private String cacheKey;
    private Instant fetchedAt;

    public DataFetchedEvent() {}

    public DataFetchedEvent(String fetchId, String patientId, String consentId,
                            String resourceType, int recordCount, String cacheKey, Instant fetchedAt) {
        this.fetchId = fetchId;
        this.patientId = patientId;
        this.consentId = consentId;
        this.resourceType = resourceType;
        this.recordCount = recordCount;
        this.cacheKey = cacheKey;
        this.fetchedAt = fetchedAt;
    }

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
}
