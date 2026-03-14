package com.hhoa.kline.core.core.shared.api;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiHandlerSecrets {
    private String apiKey;
    private String liteLlmApiKey;
    private String awsAccessKey;
    private String awsSecretKey;
    private String openRouterApiKey;
    private String clineAccountId;
    private String awsSessionToken;
    private String awsBedrockApiKey;
    private String openAiApiKey;
    private String geminiApiKey;
    private String openAiNativeApiKey;
    private String ollamaApiKey;
    private String deepSeekApiKey;
    private String requestyApiKey;
    private String togetherApiKey;
    private String fireworksApiKey;
    private String qwenApiKey;
    private String doubaoApiKey;
    private String mistralApiKey;
    private String authNonce;
    private String asksageApiKey;
    private String xaiApiKey;
    private String moonshotApiKey;
    private String zaiApiKey;
    private String huggingFaceApiKey;
    private String nebiusApiKey;
    private String sambanovaApiKey;
    private String cerebrasApiKey;
    private String sapAiCoreClientId;
    private String sapAiCoreClientSecret;
    private String groqApiKey;
    private String huaweiCloudMaasApiKey;
    private String basetenApiKey;
    private String vercelAiGatewayApiKey;
    private String difyApiKey;
    private String minimaxApiKey;
}
