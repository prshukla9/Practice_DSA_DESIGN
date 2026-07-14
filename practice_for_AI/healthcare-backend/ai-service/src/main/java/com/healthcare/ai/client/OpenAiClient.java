package com.healthcare.ai.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

@Component
public class OpenAiClient {

    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String chatModel;
    private final String embeddingModel;

    public OpenAiClient(@Value("${openai.api-key}") String apiKey,
                        @Value("${openai.chat-model:gpt-4o-mini}") String chatModel,
                        @Value("${openai.embedding-model:text-embedding-3-small}") String embeddingModel) {
        this.apiKey = apiKey;
        this.chatModel = chatModel;
        this.embeddingModel = embeddingModel;
        this.objectMapper = new ObjectMapper();
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .build();
    }

    public String chatCompletion(String systemPrompt, String userPrompt) throws IOException {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("model", chatModel);
        root.put("temperature", 0.2);

        ArrayNode messages = root.putArray("messages");
        ObjectNode system = messages.addObject();
        system.put("role", "system");
        system.put("content", systemPrompt);

        ObjectNode user = messages.addObject();
        user.put("role", "user");
        user.put("content", userPrompt);

        Request request = new Request.Builder()
                .url("https://api.openai.com/v1/chat/completions")
                .addHeader("Authorization", "Bearer " + apiKey)
                .post(RequestBody.create(root.toString(), JSON))
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                throw new IOException("OpenAI chat failed: " + response.code());
            }
            JsonNode body = objectMapper.readTree(response.body().string());
            return body.path("choices").path(0).path("message").path("content").asText();
        }
    }

    public String createEmbedding(String text) throws IOException {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("model", embeddingModel);
        root.put("input", text);

        Request request = new Request.Builder()
                .url("https://api.openai.com/v1/embeddings")
                .addHeader("Authorization", "Bearer " + apiKey)
                .post(RequestBody.create(root.toString(), JSON))
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                throw new IOException("OpenAI embedding failed: " + response.code());
            }
            JsonNode body = objectMapper.readTree(response.body().string());
            JsonNode embeddingArray = body.path("data").path(0).path("embedding");
            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < embeddingArray.size(); i++) {
                if (i > 0) sb.append(",");
                sb.append(embeddingArray.get(i).asDouble());
            }
            sb.append("]");
            return sb.toString();
        }
    }
}
