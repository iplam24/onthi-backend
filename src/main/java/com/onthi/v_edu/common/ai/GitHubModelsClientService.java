package com.onthi.v_edu.common.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private final AiConfigService aiConfigService;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public GitHubModelsClientService(AiConfigService aiConfigService) {
        this.aiConfigService = aiConfigService;
    }

    public String generateContent(String prompt, String systemPrompt) {
        AiConfig config = aiConfigService.getConfig();
        if (config == null) {
            logger.error("[AI CLIENT] AI configuration not loaded");
            return null;
        }

        String activeProvider = config.getActiveProvider();
        logger.info("[AI CLIENT] Routing generateContent request to active provider: {}", activeProvider);
        
        if ("GEMINI".equalsIgnoreCase(activeProvider)) {
            return generateContentGemini(prompt, systemPrompt, config);
        } else if ("GITHUB_MODELS".equalsIgnoreCase(activeProvider)) {
            return generateContentGithub(prompt, systemPrompt, config);
        } else {
            return generateContentCustomOpenai(prompt, systemPrompt, config);
        }
    }

    private String generateContentGemini(String prompt, String systemPrompt, AiConfig config) {
        String apiKey = config.getGeminiApiKey();
        String model = config.getGeminiModel();

        if (apiKey == null || apiKey.isBlank()) {
            logger.error("[GEMINI API] Gemini API key not configured");
            return null;
        }

        try {
            String url = String.format("https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent?key=%s", 
                    model != null ? model.trim() : "gemini-1.5-flash", 
                    apiKey.trim());

            Map<String, Object> requestBody = new HashMap<>();
            
            Map<String, Object> textPart = new HashMap<>();
            textPart.put("text", prompt);
            
            Map<String, Object> contentPart = new HashMap<>();
            contentPart.put("parts", List.of(textPart));
            contentPart.put("role", "user");
            
            requestBody.put("contents", List.of(contentPart));

            if (systemPrompt != null && !systemPrompt.isBlank()) {
                Map<String, Object> sysTextPart = new HashMap<>();
                sysTextPart.put("text", systemPrompt);
                
                Map<String, Object> sysInstruction = new HashMap<>();
                sysInstruction.put("parts", List.of(sysTextPart));
                
                requestBody.put("systemInstruction", sysInstruction);
            }

            Map<String, Object> genConfig = new HashMap<>();
            genConfig.put("temperature", 0.7);
            
            if (systemPrompt != null && systemPrompt.toLowerCase().contains("json")) {
                genConfig.put("responseMimeType", "application/json");
            }
            requestBody.put("generationConfig", genConfig);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            logger.info("[GEMINI API] Sending request to Gemini model: {}", model);
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.postForObject(url, request, Map.class);

            if (response == null) {
                logger.error("[GEMINI API] Empty response from Gemini API");
                return null;
            }

            return extractGeminiText(response);

        } catch (Exception e) {
            logger.error("[GEMINI API] Error calling Gemini API: {}", e.getMessage());
            return null;
        }
    }

    private String generateContentGithub(String prompt, String systemPrompt, AiConfig config) {
        String apiKey = config.getGithubModelsApiKey();
        String model = config.getGithubModelsModel();
        String endpoint = config.getGithubModelsEndpoint();

        if (apiKey == null || apiKey.isBlank()) {
            logger.error("[GITHUB MODELS API] GitHub Models API key not configured");
            return null;
        }

        if (endpoint == null || endpoint.isBlank()) {
            logger.error("[GITHUB MODELS API] Endpoint not configured");
            return null;
        }

        String resolvedEndpoint = endpoint.trim();
        // If the endpoint is configured as a base v1 URL (e.g. dev.vuxuanlam.me/v1), append chat/completions
        if (!resolvedEndpoint.endsWith("/chat/completions") && !resolvedEndpoint.endsWith("/completions")) {
            if (resolvedEndpoint.endsWith("/")) {
                resolvedEndpoint += "chat/completions";
            } else {
                resolvedEndpoint += "/chat/completions";
            }
        }

        try {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", model);
            requestBody.put("temperature", 0.7);
            
            if (systemPrompt != null && systemPrompt.toLowerCase().contains("json")) {
                requestBody.put("response_format", Map.of("type", "json_object"));
            }
            
            requestBody.put("messages", List.of(
                    Map.of("role", "system", "content", systemPrompt != null ? systemPrompt : "You are a helpful assistant."),
                    Map.of("role", "user", "content", prompt)
            ));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey.trim());
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            logger.info("[GITHUB MODELS API] Sending request to endpoint {} using model {}", resolvedEndpoint, model);
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.postForObject(resolvedEndpoint, request, Map.class);

            if (response == null) {
                logger.error("[GITHUB MODELS API] Empty response from API");
                return null;
            }

            return extractAssistantText(response);

        } catch (Exception e) {
            logger.error("[GITHUB MODELS API] Error calling GitHub Models: {}", e.getMessage());
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private String extractGeminiText(Map<String, Object> response) {
        Object candidates = response.get("candidates");
        if (candidates instanceof List<?> candidateList && !candidateList.isEmpty()) {
            Object firstCand = candidateList.get(0);
            if (firstCand instanceof Map<?, ?> candMap) {
                Object content = ((Map<String, Object>) candMap).get("content");
                if (content instanceof Map<?, ?> contentMap) {
                    Object parts = ((Map<String, Object>) contentMap).get("parts");
                    if (parts instanceof List<?> partList && !partList.isEmpty()) {
                        Object firstPart = partList.get(0);
                        if (firstPart instanceof Map<?, ?> partMap) {
                            Object text = ((Map<String, Object>) partMap).get("text");
                            return text == null ? null : text.toString();
                        }
                    }
                }
            }
        }
        return null;
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
        AiConfig config = aiConfigService.getConfig();
        if (config == null) return false;
        String active = config.getActiveProvider();
        if ("GEMINI".equalsIgnoreCase(active)) {
            return config.getGeminiApiKey() != null && !config.getGeminiApiKey().isBlank();
        } else if ("GITHUB_MODELS".equalsIgnoreCase(active)) {
            return config.getGithubModelsApiKey() != null && !config.getGithubModelsApiKey().isBlank();
        } else {
            return config.getCustomOpenaiApiKey() != null && !config.getCustomOpenaiApiKey().isBlank();
        }
    }

    private String generateContentCustomOpenai(String prompt, String systemPrompt, AiConfig config) {
        String apiKey = config.getCustomOpenaiApiKey();
        String model = config.getCustomOpenaiModel();
        String endpoint = config.getCustomOpenaiEndpoint();

        if (apiKey == null || apiKey.isBlank()) {
            logger.error("[CUSTOM OPENAI API] Custom OpenAI API key not configured");
            return null;
        }

        if (endpoint == null || endpoint.isBlank()) {
            logger.error("[CUSTOM OPENAI API] Custom OpenAI Endpoint not configured");
            return null;
        }

        String resolvedEndpoint = endpoint.trim();
        if (!resolvedEndpoint.endsWith("/chat/completions") && !resolvedEndpoint.endsWith("/completions")) {
            if (resolvedEndpoint.endsWith("/")) {
                resolvedEndpoint += "chat/completions";
            } else {
                resolvedEndpoint += "/chat/completions";
            }
        }

        try {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", model);
            requestBody.put("temperature", 0.7);
            
            if (systemPrompt != null && systemPrompt.toLowerCase().contains("json")) {
                requestBody.put("response_format", Map.of("type", "json_object"));
            }
            
            requestBody.put("messages", List.of(
                    Map.of("role", "system", "content", systemPrompt != null ? systemPrompt : "You are a helpful assistant."),
                    Map.of("role", "user", "content", prompt)
            ));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey.trim());
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            logger.info("[CUSTOM OPENAI API] Sending request to endpoint {} using model {}", resolvedEndpoint, model);
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.postForObject(resolvedEndpoint, request, Map.class);

            if (response == null) {
                logger.error("[CUSTOM OPENAI API] Empty response from API");
                return null;
            }

            return extractAssistantText(response);

        } catch (Exception e) {
            logger.error("[CUSTOM OPENAI API] Error calling Custom OpenAI: {}", e.getMessage());
            return null;
        }
    }
}
