package com.healthcare.integration.service;

import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.context.FhirContext;
import com.healthcare.common.events.DataFetchedEvent;
import com.healthcare.integration.client.FhirClientFactory;
import com.healthcare.integration.dto.FetchExternalDataRequest;
import com.healthcare.integration.dto.FetchExternalDataResponse;
import com.healthcare.integration.kafka.DataFetchedEventProducer;
import org.hl7.fhir.r4.model.Bundle;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class ExternalDataService {

    private final FhirClientFactory fhirClientFactory;
    private final StringRedisTemplate redisTemplate;
    private final DataFetchedEventProducer dataFetchedEventProducer;
    private final RestTemplate restTemplate;
    private final String consentServiceUrl;
    private final String externalTokenUrl;
    private final long cacheTtlMinutes;
    private final IParser fhirParser = FhirContext.forR4().newJsonParser();

    public ExternalDataService(FhirClientFactory fhirClientFactory,
                               StringRedisTemplate redisTemplate,
                               DataFetchedEventProducer dataFetchedEventProducer,
                               RestTemplate restTemplate,
                               @Value("${services.consent-url}") String consentServiceUrl,
                               @Value("${fhir.token-url}") String externalTokenUrl,
                               @Value("${cache.ttl-minutes:30}") long cacheTtlMinutes) {
        this.fhirClientFactory = fhirClientFactory;
        this.redisTemplate = redisTemplate;
        this.dataFetchedEventProducer = dataFetchedEventProducer;
        this.restTemplate = restTemplate;
        this.consentServiceUrl = consentServiceUrl;
        this.externalTokenUrl = externalTokenUrl;
        this.cacheTtlMinutes = cacheTtlMinutes;
    }

    public FetchExternalDataResponse fetchWithConsent(FetchExternalDataRequest request) {
        validateConsent(request.getConsentId());

        String accessToken = obtainExternalAccessToken(request.getConsentId());
        Bundle bundle = fhirClientFactory.searchPatientResources(
                request.getExternalPatientId(),
                request.getResourceType(),
                accessToken
        );

        String payload = fhirParser.encodeResourceToString(bundle);
        String cacheKey = buildCacheKey(request.getPatientId(), request.getResourceType());
        redisTemplate.opsForValue().set(cacheKey, payload, cacheTtlMinutes, TimeUnit.MINUTES);

        String fetchId = UUID.randomUUID().toString();
        Instant fetchedAt = Instant.now();
        int recordCount = bundle.getEntry() == null ? 0 : bundle.getEntry().size();

        DataFetchedEvent event = new DataFetchedEvent(
                fetchId,
                request.getPatientId(),
                request.getConsentId(),
                request.getResourceType(),
                recordCount,
                cacheKey,
                fetchedAt
        );
        dataFetchedEventProducer.publish(event);

        FetchExternalDataResponse response = new FetchExternalDataResponse();
        response.setFetchId(fetchId);
        response.setPatientId(request.getPatientId());
        response.setConsentId(request.getConsentId());
        response.setResourceType(request.getResourceType());
        response.setRecordCount(recordCount);
        response.setCacheKey(cacheKey);
        response.setFetchedAt(fetchedAt);
        response.setSummary("Fetched " + recordCount + " " + request.getResourceType() + " records (cached temporarily)");
        return response;
    }

    private void validateConsent(String consentId) {
        restTemplate.getForObject(consentServiceUrl + "/api/v1/consents/" + consentId + "/validate", Object.class);
    }

    private String obtainExternalAccessToken(String consentId) {
        // In production: exchange consent artifact for scoped OAuth2 token from HIE
        return "simulated-access-token-for-consent-" + consentId;
    }

    private String buildCacheKey(String patientId, String resourceType) {
        return "fhir:" + patientId + ":" + resourceType;
    }
}
