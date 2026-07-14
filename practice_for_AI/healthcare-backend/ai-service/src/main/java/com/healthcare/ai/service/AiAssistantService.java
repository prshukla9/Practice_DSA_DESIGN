package com.healthcare.ai.service;

import com.healthcare.ai.client.OpenAiClient;
import com.healthcare.ai.dto.AiQueryRequest;
import com.healthcare.ai.dto.AiQueryResponse;
import com.healthcare.ai.prompt.MedicalPromptBuilder;
import com.healthcare.ai.repository.PatientContextEmbeddingRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class AiAssistantService {

    private static final String DISCLAIMER =
            "This is not medical advice. Consult your physician.";

    private final OpenAiClient openAiClient;
    private final PatientContextEmbeddingRepository embeddingRepository;
    private final StringRedisTemplate redisTemplate;
    private final RestTemplate restTemplate;
    private final String consentServiceUrl;

    public AiAssistantService(OpenAiClient openAiClient,
                              PatientContextEmbeddingRepository embeddingRepository,
                              StringRedisTemplate redisTemplate,
                              RestTemplate restTemplate,
                              @Value("${services.consent-url}") String consentServiceUrl) {
        this.openAiClient = openAiClient;
        this.embeddingRepository = embeddingRepository;
        this.redisTemplate = redisTemplate;
        this.restTemplate = restTemplate;
        this.consentServiceUrl = consentServiceUrl;
    }

    public AiQueryResponse ask(AiQueryRequest request) {
        validateConsent(request.getConsentId());

        List<String> contextChunks = retrieveContext(request);
        String contextBlock = String.join("\n---\n", contextChunks);

        AiQueryResponse response = new AiQueryResponse();
        response.setRetrievedContext(contextChunks);
        response.setDisclaimer(DISCLAIMER);

        if (contextChunks.isEmpty()) {
            response.setAnswer("No consented patient data is available to answer this question.");
            response.setUsedFallback(true);
            return response;
        }

        try {
            String userPrompt = MedicalPromptBuilder.buildUserPrompt(request.getQuery(), contextBlock);
            String answer = openAiClient.chatCompletion(MedicalPromptBuilder.SYSTEM_PROMPT, userPrompt);
            response.setAnswer(sanitizeAnswer(answer));
            response.setUsedFallback(false);
        } catch (Exception ex) {
            response.setAnswer("AI assistant is temporarily unavailable. Please review fetched records manually.");
            response.setUsedFallback(true);
        }
        return response;
    }

    private void validateConsent(String consentId) {
        restTemplate.getForObject(consentServiceUrl + "/api/v1/consents/" + consentId + "/validate", Object.class);
    }

    private List<String> retrieveContext(AiQueryRequest request) {
        List<String> chunks = new ArrayList<String>();

        if (request.getCacheKey() != null) {
            String cached = redisTemplate.opsForValue().get(request.getCacheKey());
            if (cached != null) {
                chunks.add(truncate(cached, 4000));
            }
        }

        try {
            String queryEmbedding = openAiClient.createEmbedding(request.getQuery());
            List<String> vectorHits = embeddingRepository.findSimilarChunks(
                    request.getPatientId(),
                    request.getConsentId(),
                    queryEmbedding,
                    5
            );
            chunks.addAll(vectorHits);
        } catch (Exception ignored) {
            // vector search is best-effort; redis cache may still provide context
        }

        if (chunks.isEmpty()) {
            return Collections.emptyList();
        }
        return chunks;
    }

    private String sanitizeAnswer(String answer) {
        String lower = answer.toLowerCase();
        if (lower.contains("diagnose") || lower.contains("prescribe") || lower.contains("emergency")) {
            return "I can summarize available records but cannot provide diagnosis or treatment guidance. " + DISCLAIMER;
        }
        if (!answer.contains(DISCLAIMER)) {
            return answer + "\n\n" + DISCLAIMER;
        }
        return answer;
    }

    private String truncate(String value, int max) {
        return value.length() <= max ? value : value.substring(0, max) + "...";
    }
}
