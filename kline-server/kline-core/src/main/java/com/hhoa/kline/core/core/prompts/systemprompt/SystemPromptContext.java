package com.hhoa.kline.core.core.prompts.systemprompt;

import com.hhoa.kline.core.core.services.mcp.IMcpHub;
import com.hhoa.kline.core.core.shared.FocusChainSettings;
import com.hhoa.kline.core.core.shared.api.ModelInfo;
import com.hhoa.kline.core.core.shared.multiroot.WorkspaceRoot;
import com.hhoa.kline.core.core.shared.proto.cline.BrowserSettings;
import java.util.List;
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
