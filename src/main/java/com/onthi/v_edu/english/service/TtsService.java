package com.onthi.v_edu.english.service;

import com.onthi.v_edu.common.ai.AiConfig;
import com.onthi.v_edu.common.ai.AiConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class TtsService {

    private final AiConfigService aiConfigService;
    private final RestTemplate restTemplate = new RestTemplate();

    public byte[] generateSpeech(String text, String voice) {
        AiConfig config = aiConfigService.getConfig();
        if (config == null) {
            log.error("[TTS SERVICE] AI configuration not loaded");
            return null;
        }

        String apiKey = config.getCustomOpenaiApiKey();
        String endpoint = config.getCustomOpenaiEndpoint();

        // Fallback check
        if (apiKey == null || apiKey.isBlank()) {
            log.error("[TTS SERVICE] API Key is missing in Custom OpenAI settings. Cannot generate TTS.");
            return null;
        }

        String resolvedEndpoint = "https://api.openai.com/v1/audio/speech";
        if (endpoint != null && !endpoint.isBlank()) {
            resolvedEndpoint = endpoint.trim();
            if (resolvedEndpoint.endsWith("/chat/completions")) {
                resolvedEndpoint = resolvedEndpoint.replace("/chat/completions", "/audio/speech");
            } else if (resolvedEndpoint.endsWith("/completions")) {
                resolvedEndpoint = resolvedEndpoint.replace("/completions", "/audio/speech");
            } else {
                if (resolvedEndpoint.endsWith("/")) {
                    resolvedEndpoint += "audio/speech";
                } else {
                    resolvedEndpoint += "/audio/speech";
                }
            }
        }

        log.info("[TTS SERVICE] Generating TTS at endpoint: {} using voice: {}", resolvedEndpoint, voice);

        try {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", "tts-1");
            requestBody.put("input", text);
            requestBody.put("voice", voice != null && !voice.isBlank() ? voice : "alloy"); 
            requestBody.put("response_format", "mp3");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey.trim());

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            ResponseEntity<byte[]> response = restTemplate.exchange(
                    resolvedEndpoint,
                    HttpMethod.POST,
                    request,
                    byte[].class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                log.info("[TTS SERVICE] Speech generated successfully: {} bytes", response.getBody().length);
                return response.getBody();
            } else {
                log.error("[TTS SERVICE] Failed to generate speech. HTTP status: {}", response.getStatusCode());
                return null;
            }
        } catch (Exception e) {
            log.error("[TTS SERVICE] Error generating TTS: {}", e.getMessage());
            return null;
        }
    }
}
