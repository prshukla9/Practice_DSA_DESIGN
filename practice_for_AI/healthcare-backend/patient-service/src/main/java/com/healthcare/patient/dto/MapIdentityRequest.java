package com.healthcare.patient.dto;

import javax.validation.constraints.NotBlank;

public class MapIdentityRequest {

    @NotBlank
    private String externalPatientId;

    public String getExternalPatientId() { return externalPatientId; }
    public void setExternalPatientId(String externalPatientId) { this.externalPatientId = externalPatientId; }
}
