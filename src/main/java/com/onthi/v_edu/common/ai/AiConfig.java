package com.onthi.v_edu.common.ai;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "ai_configs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AiConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "active_provider", length = 50, nullable = false)
    private String activeProvider = "GITHUB_MODELS";

    @Column(name = "gemini_api_key", length = 500)
    private String geminiApiKey;

    @Column(name = "gemini_model", length = 100)
    private String geminiModel = "gemini-3-flash-preview";

    @Column(name = "github_models_api_key", length = 500)
    private String githubModelsApiKey;

    @Column(name = "github_models_model", length = 100)
    private String githubModelsModel = "gpt-4o-mini";

    @Column(name = "github_models_endpoint", length = 500)
    private String githubModelsEndpoint = "https://models.github.ai/inference/chat/completions";

    @Column(name = "custom_openai_api_key", length = 500)
    private String customOpenaiApiKey;

    @Column(name = "custom_openai_model", length = 100)
    private String customOpenaiModel = "gpt-4o";

    @Column(name = "custom_openai_endpoint", length = 500)
    private String customOpenaiEndpoint = "https://dev.vuxuanlam.me/v1";
}
