package com.onthi.v_edu.common.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class GitHubModelsClientService {

    private static final Logger logger = LoggerFactory.getLogger(GitHubModelsClientService.class);

    @Value("${app.github-models.api-key:}")
    private String apiKey;

    @Value("${app.github-models.endpoint:https://models.github.ai/inference/chat/completions}")
    private String endpoint;

    @Value("${app.github-models.model:gpt-4o}")
    private String model;

    @Value("${app.github-models.enabled:true}")
    private boolean enabled;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public String generateContent(String prompt, String systemPrompt) {
        if (!enabled) {
            logger.warn("GitHub Models AI is disabled");
            return null;
        }

        if (apiKey == null || apiKey.isBlank()) {
            logger.error("GitHub Models API key not configured");
            return null;
        }

        try {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", model);
            requestBody.put("temperature", 0.7);
            requestBody.put("messages", List.of(
                    Map.of("role", "system", "content", systemPrompt != null ? systemPrompt : "You are a helpful assistant."),
                    Map.of("role", "user", "content", prompt)
            ));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey.trim());
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.postForObject(endpoint, request, Map.class);

            if (response == null) {
                logger.error("[GITHUB MODELS] Empty response from API");
                return null;
            }

            return extractAssistantText(response);

        } catch (Exception e) {
            logger.error("[GITHUB MODELS] Unexpected error: {}", e.getMessage());
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private String extractAssistantText(Map<String, Object> response) {
        Object choices = response.get("choices");
        if (choices instanceof List<?> choiceList && !choiceList.isEmpty()) {
            Object first = choiceList.get(0);
            if (first instanceof Map<?, ?> firstMap) {
                Object message = ((Map<String, Object>) firstMap).get("message");
                if (message instanceof Map<?, ?> messageMap) {
                    Object content = ((Map<String, Object>) messageMap).get("content");
                    return content == null ? null : content.toString();
                }
            }
        }
        return null;
    }

    public ObjectMapper getObjectMapper() {
        return objectMapper;
    }

    public boolean isEnabled() {
        return enabled;
    }
}
