package com.hhoa.kline.core.core.storage;

import java.io.File;

/** 全局文件名常量 */
public class GlobalFileNames {
    public static final String BASE_DIR =
            System.getProperty("user.home") + File.separator + ".cline";
    public static final String DB_MAPPING_METADATA_DIR =
            BASE_DIR + File.separator + "db_mapping_metadata";
    public static final String API_CONVERSATION_HISTORY = "api_conversation_history.json";

    public static final String CONTEXT_HISTORY = "context_history.json";
    public static final String UI_MESSAGES = "ui_messages.json";
    public static final String OPEN_ROUTER_MODELS = "openrouter_models.json";
    public static final String VERCEL_AI_GATEWAY_MODELS = "vercel_ai_gateway_models.json";
    public static final String GROQ_MODELS = "groq_models.json";
    public static final String BASETEN_MODELS = "baseten_models.json";
    public static final String MCP_SETTINGS = "cline_mcp_settings.json";
    public static final String CLINE_RULES = ".clinerules";
    public static final String WORKFLOWS = ".clinerules/workflows";
    public static final String HOOKS_DIR = ".clinerules/hooks";
    public static final String CURSOR_RULES_DIR = ".cursor/rules";
    public static final String CURSOR_RULES_FILE = ".cursorrules";
    public static final String WINDSURF_RULES = ".windsurfrules";
    public static final String TASK_METADATA = "task_metadata.json";
    public static final String MCP_MARKETPLACE_CATALOG = "mcp_marketplace_catalog.json";

    /**
     * 获取远程配置文件名
     *
     * @param orgId 组织ID
     * @return 远程配置文件名
     */
    public static String remoteConfig(String orgId) {
        return "remote_config_" + orgId + ".json";
    }

    private GlobalFileNames() {}
}
