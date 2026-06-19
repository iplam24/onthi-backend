package com.onthi.v_edu.common.ai;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiConfigService {

    private final AiConfigRepository aiConfigRepository;

    @Value("${app.gemini.api-key:}")
    private String defaultGeminiApiKey;

    @Value("${app.gemini.model:gemini-3-flash-preview}")
    private String defaultGeminiModel;

    @Value("${app.github-models.api-key:}")
    private String defaultGithubApiKey;

    @Value("${app.github-models.model:gpt-4o-mini}")
    private String defaultGithubModel;

    @Value("${app.github-models.endpoint:https://models.github.ai/inference/chat/completions}")
    private String defaultGithubEndpoint;

    @Value("${app.gemini.enabled:true}")
    private boolean defaultGeminiEnabled;

    private volatile AiConfig cachedConfig;

    @PostConstruct
    public void init() {
        try {
            // Find first config in DB
            AiConfig config = aiConfigRepository.findAll().stream().findFirst().orElse(null);
            if (config == null) {
                log.info("[AI CONFIG] No configuration found in database. Initializing with properties defaults.");
                config = new AiConfig();
                config.setActiveProvider(defaultGeminiEnabled ? "GEMINI" : "GITHUB_MODELS");
                config.setGeminiApiKey(defaultGeminiApiKey);
                config.setGeminiModel(defaultGeminiModel);
                config.setGithubModelsApiKey(defaultGithubApiKey);
                config.setGithubModelsModel(defaultGithubModel);
                config.setGithubModelsEndpoint(defaultGithubEndpoint);
                config = aiConfigRepository.save(config);
            }
            cachedConfig = config;
            log.info("[AI CONFIG] Loaded AI provider configuration. Active provider: {}", cachedConfig.getActiveProvider());
        } catch (Exception e) {
            log.error("[AI CONFIG] Error initializing AI configuration: {}", e.getMessage());
        }
    }

    public AiConfig getConfig() {
        if (cachedConfig == null) {
            // Fallback in case of init issues
            init();
        }
        return cachedConfig;
    }

    public synchronized AiConfig updateConfig(AiConfigRequest request) {
        AiConfig config = getConfig();
        if (config == null) {
            config = new AiConfig();
        }
        config.setActiveProvider(request.getActiveProvider());
        if (request.getGeminiApiKey() != null) config.setGeminiApiKey(request.getGeminiApiKey());
        if (request.getGeminiModel() != null) config.setGeminiModel(request.getGeminiModel());
        if (request.getGithubModelsApiKey() != null) config.setGithubModelsApiKey(request.getGithubModelsApiKey());
        if (request.getGithubModelsModel() != null) config.setGithubModelsModel(request.getGithubModelsModel());
        if (request.getGithubModelsEndpoint() != null) config.setGithubModelsEndpoint(request.getGithubModelsEndpoint());
        if (request.getCustomOpenaiApiKey() != null) config.setCustomOpenaiApiKey(request.getCustomOpenaiApiKey());
        if (request.getCustomOpenaiModel() != null) config.setCustomOpenaiModel(request.getCustomOpenaiModel());
        if (request.getCustomOpenaiEndpoint() != null) config.setCustomOpenaiEndpoint(request.getCustomOpenaiEndpoint());
        
        AiConfig saved = aiConfigRepository.save(config);
        cachedConfig = saved;
        log.info("[AI CONFIG] Updated AI provider configuration. Active provider: {}", cachedConfig.getActiveProvider());
        return saved;
    }
}
