package com.hhoa.kline.core.core.shared.storage;

import java.util.HashMap;
import java.util.Map;
import lombok.Data;

@Data
public class Secrets {
    /** 按 ApiProvider.getValue() 为 key 的 API Key，如 "mistral" -> key */
    private Map<String, String> apiKeys = new HashMap<>();

    private String clineAccountId;
    private String clineClineAccountId;
    private String authNonce;
    private String awsAccessKey;
    private String awsSecretKey;
    private String awsSessionToken;
    private String awsBedrockApiKey;
    private String sapAiCoreClientId;
    private String sapAiCoreClientSecret;
    private String ocaRefreshToken;
}
