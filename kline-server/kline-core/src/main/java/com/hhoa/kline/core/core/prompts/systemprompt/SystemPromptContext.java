package com.hhoa.kline.core.core.prompts.systemprompt;

import com.hhoa.kline.core.core.services.mcp.IMcpHub;
import com.hhoa.kline.core.core.shared.FocusChainSettings;
import com.hhoa.kline.core.core.shared.api.ModelInfo;
import com.hhoa.kline.core.core.shared.multiroot.WorkspaceRoot;
import com.hhoa.kline.core.core.shared.proto.cline.BrowserSettings;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 系统提示上下文
 *
 * @author hhoa
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SystemPromptContext {

    private ApiProviderInfo providerInfo;

    private String cwd;

    private Boolean supportsBrowserUse;

    private IMcpHub mcpHub;

    private FocusChainSettings focusChainSettings;

    private String globalClineRulesFileInstructions;

    private String clineIgnoreInstructions;

    private String preferredLanguageInstructions;

    private BrowserSettings browserSettings;

    private Boolean yoloModeToggled;

    private List<WorkspaceRoot> workspaceRoots;

    private Boolean isSubagentsEnabledAndCliInstalled;

    private Boolean isCliSubagent;

    private Boolean hasTruncatedConversationHistory;

    /** Skills 相关 */
    private List<SkillInfo> skills;

    /** 子代理相关 */
    private Boolean subagentsEnabled;

    private Boolean isSubagentRun;

    /** Web 搜索相关 */
    private Boolean clineWebToolsEnabled;

    /** 是否为 CLI 环境 */
    private Boolean isCliEnvironment;

    /** 本地 Cline 规则文件指令 */
    private String localClineRulesFileInstructions;

    /** 本地 Cursor 规则文件指令 */
    private String localCursorRulesFileInstructions;

    /** 本地 Cursor 规则目录指令 */
    private String localCursorRulesDirInstructions;

    /** 本地 Windsurf 规则文件指令 */
    private String localWindsurfRulesFileInstructions;

    /** 本地 Agents 规则文件指令 */
    private String localAgentsRulesFileInstructions;

    /** 是否启用原生工具调用 */
    private Boolean enableNativeToolCalls;

    /** 是否启用并行工具调用 */
    private Boolean enableParallelToolCalling;

    /** 终端执行模式 */
    private String terminalExecutionMode;

    /** 是否为多工作区模式 */
    private Boolean isMultiRootEnabled;

    /** IDE 平台标识 */
    private String ide;

    /** 编辑器打开/可见的标签页路径 */
    private EditorTabs editorTabs;

    /** 是否为测试环境 */
    private Boolean isTesting;

    /** 运行时占位符，用于动态注入额外的模板变量 */
    private Map<String, Object> runtimePlaceholders;

    /** 提供商 ID 快捷访问 */
    public String getProviderId() {
        return providerInfo != null ? providerInfo.getProviderId() : null;
    }

    /** Skill 信息 */
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class SkillInfo {
        private String name;
        private String description;
    }

    /** 编辑器标签页信息 */
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class EditorTabs {
        private List<String> open;
        private List<String> visible;
    }

    /** API 提供商信息 */
    @Data
    public static class ApiProviderInfo {
        private ApiHandlerModel model;
        private String customPrompt;
        private String providerId;
    }

    @Data
    public static class ApiHandlerModel {
        private String id;
        private ModelInfo modelInfo;
    }
}
