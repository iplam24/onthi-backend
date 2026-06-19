package com.onthi.v_edu.common.ai;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AiConfigRequest {

    @NotBlank(message = "Active provider cannot be blank")
    private String activeProvider;

    private String geminiApiKey;
    private String geminiModel;

    private String githubModelsApiKey;
    private String githubModelsModel;
    private String githubModelsEndpoint;

    private String customOpenaiApiKey;
    private String customOpenaiModel;
    private String customOpenaiEndpoint;
}
