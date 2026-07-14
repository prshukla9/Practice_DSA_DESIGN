package com.healthcare.integration.client;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.interceptor.BearerTokenAuthInterceptor;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.ResourceType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class FhirClientFactory {

    private final FhirContext fhirContext = FhirContext.forR4();
    private final String fhirBaseUrl;

    public FhirClientFactory(@Value("${fhir.base-url}") String fhirBaseUrl) {
        this.fhirBaseUrl = fhirBaseUrl;
    }

    public Bundle searchPatientResources(String externalPatientId, String resourceType, String accessToken) {
        IGenericClient client = fhirContext.newRestfulGenericClient(fhirBaseUrl);
        client.registerInterceptor(new BearerTokenAuthInterceptor(accessToken));

        return client.search()
                .forResource(ResourceType.fromCode(resourceType))
                .where(org.hl7.fhir.r4.model.Patient.PATIENT.hasId(externalPatientId))
                .returnBundle(Bundle.class)
                .execute();
    }
}
