package com.healthcare.common.events;

import java.io.Serializable;
import java.time.Instant;

public class ConsentApprovedEvent implements Serializable {
    private String consentId;
    private String patientId;
    private String externalPatientId;
    private String purpose;
    private Instant approvedAt;

    public ConsentApprovedEvent() {}

    public ConsentApprovedEvent(String consentId, String patientId, String externalPatientId,
                                String purpose, Instant approvedAt) {
        this.consentId = consentId;
        this.patientId = patientId;
        this.externalPatientId = externalPatientId;
        this.purpose = purpose;
        this.approvedAt = approvedAt;
    }

    public String getConsentId() { return consentId; }
    public void setConsentId(String consentId) { this.consentId = consentId; }
    public String getPatientId() { return patientId; }
    public void setPatientId(String patientId) { this.patientId = patientId; }
    public String getExternalPatientId() { return externalPatientId; }
    public void setExternalPatientId(String externalPatientId) { this.externalPatientId = externalPatientId; }
    public String getPurpose() { return purpose; }
    public void setPurpose(String purpose) { this.purpose = purpose; }
    public Instant getApprovedAt() { return approvedAt; }
    public void setApprovedAt(Instant approvedAt) { this.approvedAt = approvedAt; }
}
