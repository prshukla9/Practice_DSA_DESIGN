package com.healthcare.consent.dto;

import javax.validation.constraints.NotBlank;

public class ConsentRequestDto {

    @NotBlank
    private String patientId;

    private String externalPatientId;

    @NotBlank
    private String purpose;

    public String getPatientId() { return patientId; }
    public void setPatientId(String patientId) { this.patientId = patientId; }
    public String getExternalPatientId() { return externalPatientId; }
    public void setExternalPatientId(String externalPatientId) { this.externalPatientId = externalPatientId; }
    public String getPurpose() { return purpose; }
    public void setPurpose(String purpose) { this.purpose = purpose; }
}
