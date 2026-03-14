package com.hhoa.kline.core.core.shared.storage;

import com.hhoa.kline.core.core.shared.LanguageModelChatSelector;
import com.hhoa.kline.core.core.shared.api.ModelInfo;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ModelConfig {
    private String modelId;
    private ModelInfo modelInfo;
    private String baseUrl;
    private Boolean awsBedrockCustomSelected;
    private String awsBedrockCustomModelBaseId;
    private String sapAiCoreDeploymentId;
    private LanguageModelChatSelector vsCodeLmModelSelector;
    private boolean usePromptCache;
}
