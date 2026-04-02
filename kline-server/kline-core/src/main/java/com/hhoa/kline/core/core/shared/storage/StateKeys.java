package com.hhoa.kline.core.core.shared.storage;

public final class StateKeys {
    private StateKeys() {}

    public static final class SecretKey {
        private SecretKey() {}

        public static final String API_KEY = "apiKey";
        public static final String CLINE_ACCOUNT_ID = "clineAccountId";
        public static final String CLINE_CLINE_ACCOUNT_ID = "cline:clineAccountId";
        public static final String OPEN_ROUTER_API_KEY = "openRouterApiKey";
        public static final String AWS_ACCESS_KEY = "awsAccessKey";
        public static final String AWS_SECRET_KEY = "awsSecretKey";
        public static final String AWS_SESSION_TOKEN = "awsSessionToken";
        public static final String AWS_BEDROCK_API_KEY = "awsBedrockApiKey";
        public static final String OPEN_AI_API_KEY = "openAiApiKey";
        public static final String GEMINI_API_KEY = "geminiApiKey";
        public static final String OPEN_AI_NATIVE_API_KEY = "openAiNativeApiKey";
        public static final String OLLAMA_API_KEY = "ollamaApiKey";
        public static final String DEEP_SEEK_API_KEY = "deepSeekApiKey";
        public static final String REQUESTY_API_KEY = "requestyApiKey";
        public static final String TOGETHER_API_KEY = "togetherApiKey";
        public static final String FIREWORKS_API_KEY = "fireworksApiKey";
        public static final String QWEN_API_KEY = "qwenApiKey";
        public static final String DOUBAO_API_KEY = "doubaoApiKey";
        public static final String MISTRAL_API_KEY = "mistralApiKey";
        public static final String LITE_LLM_API_KEY = "liteLlmApiKey";
        public static final String AUTH_NONCE = "authNonce";
        public static final String ASKSAGE_API_KEY = "asksageApiKey";
        public static final String XAI_API_KEY = "xaiApiKey";
        public static final String MOONSHOT_API_KEY = "moonshotApiKey";
        public static final String ZAI_API_KEY = "zaiApiKey";
        public static final String HUGGING_FACE_API_KEY = "huggingFaceApiKey";
        public static final String NEBIUS_API_KEY = "nebiusApiKey";
        public static final String SAMBANOVA_API_KEY = "sambanovaApiKey";
        public static final String CEREBRAS_API_KEY = "cerebrasApiKey";
        public static final String SAP_AI_CORE_CLIENT_ID = "sapAiCoreClientId";
        public static final String SAP_AI_CORE_CLIENT_SECRET = "sapAiCoreClientSecret";
        public static final String GROQ_API_KEY = "groqApiKey";
        public static final String HUAWEI_CLOUD_MAAS_API_KEY = "huaweiCloudMaasApiKey";
        public static final String BASETEN_API_KEY = "basetenApiKey";
        public static final String VERCEL_AI_GATEWAY_API_KEY = "vercelAiGatewayApiKey";
        public static final String DIFY_API_KEY = "difyApiKey";
        public static final String OCA_API_KEY = "ocaApiKey";
        public static final String OCA_REFRESH_TOKEN = "ocaRefreshToken";
        public static final String MINIMAX_API_KEY = "minimaxApiKey";
    }

    public static final class GlobalStateKey {
        private GlobalStateKey() {}

        public static final String LAST_SHOWN_ANNOUNCEMENT_ID = "lastShownAnnouncementId";
        public static final String TASK_HISTORY = "taskHistory";
        public static final String USER_INFO = "userInfo";
        public static final String FAVORITED_MODEL_IDS = "favoritedModelIds";
        public static final String MCP_MARKETPLACE_ENABLED = "mcpMarketplaceEnabled";
        public static final String MCP_RESPONSES_COLLAPSED = "mcpResponsesCollapsed";
        public static final String TERMINAL_REUSE_ENABLED = "terminalReuseEnabled";
        public static final String VSCODE_TERMINAL_EXECUTION_MODE = "vscodeTerminalExecutionMode";
        public static final String IS_NEW_USER = "isNewUser";
        public static final String WELCOME_VIEW_COMPLETED = "welcomeViewCompleted";
        public static final String MCP_DISPLAY_MODE = "mcpDisplayMode";
        public static final String WORKSPACE_ROOTS = "workspaceRoots";
        public static final String PRIMARY_ROOT_INDEX = "primaryRootIndex";
        public static final String MULTI_ROOT_ENABLED = "multiRootEnabled";
        public static final String HOOKS_ENABLED = "hooksEnabled";
        public static final String LAST_DISMISSED_INFO_BANNER_VERSION =
                "lastDismissedInfoBannerVersion";
        public static final String LAST_DISMISSED_MODEL_BANNER_VERSION =
                "lastDismissedModelBannerVersion";
        public static final String LAST_DISMISSED_CLI_BANNER_VERSION =
                "lastDismissedCliBannerVersion";
    }

    public static final class LocalStateKey {
        private LocalStateKey() {}

        public static final String LOCAL_CLINE_RULES_TOGGLES = "localMap<String, Boolean>";
        public static final String LOCAL_CURSOR_RULES_TOGGLES = "localCursorRulesToggles";
        public static final String LOCAL_WINDSURF_RULES_TOGGLES = "localWindsurfRulesToggles";
        public static final String WORKFLOW_TOGGLES = "workflowToggles";
    }

    /** 注意：由于 Settings 接口字段很多，这里只列出主要键。 完整的键列表请参考 Settings 接口定义。 */
    public static final class SettingsKey {
        private SettingsKey() {}

        public static final String AWS_REGION = "awsRegion";
        public static final String AWS_USE_CROSS_REGION_INFERENCE = "awsUseCrossRegionInference";
        public static final String AWS_USE_GLOBAL_INFERENCE = "awsUseGlobalInference";
        public static final String AWS_BEDROCK_USE_PROMPT_CACHE = "awsBedrockUsePromptCache";
        public static final String AWS_BEDROCK_ENDPOINT = "awsBedrockEndpoint";
        public static final String AWS_PROFILE = "awsProfile";
        public static final String AWS_AUTHENTICATION = "awsAuthentication";
        public static final String AWS_USE_PROFILE = "awsUseProfile";

        public static final String REQUESTY_BASE_URL = "requestyBaseUrl";
        public static final String OPEN_AI_BASE_URL = "openAiBaseUrl";
        public static final String OPEN_AI_HEADERS = "openAiHeaders";
        public static final String OLLAMA_BASE_URL = "ollamaBaseUrl";
        public static final String OLLAMA_API_OPTIONS_CTX_NUM = "ollamaApiOptionsCtxNum";
        public static final String LM_STUDIO_BASE_URL = "lmStudioBaseUrl";
        public static final String LM_STUDIO_MAX_TOKENS = "lmStudioMaxTokens";
        public static final String ANTHROPIC_BASE_URL = "anthropicBaseUrl";
        public static final String GEMINI_BASE_URL = "geminiBaseUrl";
        public static final String AZURE_API_VERSION = "azureApiVersion";
        public static final String OPEN_ROUTER_PROVIDER_SORTING = "openRouterProviderSorting";

        public static final String AUTO_APPROVAL_SETTINGS = "autoApprovalSettings";
        public static final String GLOBAL_CLINE_RULES_TOGGLES = "globalMap<String, Boolean>";
        public static final String GLOBAL_WORKFLOW_TOGGLES = "globalWorkflowToggles";
        public static final String BROWSER_SETTINGS = "browserSettings";
        public static final String TELEMETRY_SETTING = "telemetrySetting";
        public static final String DICTATION_SETTINGS = "dictationSettings";
        public static final String FOCUS_CHAIN_SETTINGS = "focusChainSettings";

        public static final String PLAN_ACT_SEPARATE_MODELS_SETTING =
                "planActSeparateModelsSetting";
        public static final String ENABLE_CHECKPOINTS_SETTING = "enableCheckpointsSetting";
        public static final String STRICT_PLAN_MODE_ENABLED = "strictPlanModeEnabled";
        public static final String YOLO_MODE_TOGGLED = "yoloModeToggled";
        public static final String AUTO_APPROVE_ALL_TOGGLED = "autoApproveAllToggled";
        public static final String USE_AUTO_CONDENSE = "useAutoCondense";
        public static final String HOOKS_ENABLED = "hooksEnabled";
        public static final String SUBAGENTS_ENABLED = "subagentsEnabled";

        public static final String PREFERRED_LANGUAGE = "preferredLanguage";
        public static final String OPENAI_REASONING_EFFORT = "openaiReasoningEffort";
        public static final String MODE = "mode";
        public static final String CUSTOM_PROMPT = "customPrompt";
        public static final String AUTO_CONDENSE_THRESHOLD = "autoCondenseThreshold";

        // 更多键请参考 Settings 接口定义...
    }
}
