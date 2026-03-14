package com.hhoa.kline.core.core.shared;

import com.hhoa.kline.core.core.controller.HistoryItem;
import com.hhoa.kline.core.core.shared.api.ApiConfiguration;
import com.hhoa.kline.core.core.shared.multiroot.WorkspaceRoot;
import com.hhoa.kline.core.core.shared.proto.cline.BrowserSettings;
import com.hhoa.kline.core.core.shared.storage.types.Mode;
import com.hhoa.kline.core.core.shared.storage.types.OpenaiReasoningEffort;
import com.hhoa.kline.core.core.task.ClineMessage;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExtensionState {
    private boolean isNewUser;
    private boolean welcomeViewCompleted;
    private ApiConfiguration apiConfiguration;
    private AutoApprovalSettings autoApprovalSettings;
    private BrowserSettings browserSettings;
    private String remoteBrowserHost;
    private String preferredLanguage;
    private OpenaiReasoningEffort openaiReasoningEffort;
    private Mode mode;
    private String checkpointManagerErrorMessage;
    private List<ClineMessage> clineMessages;
    private HistoryItem currentTaskItem;
    private String currentFocusChainChecklist;
    private Boolean mcpMarketplaceEnabled;
    private McpDisplayMode mcpDisplayMode;
    private boolean planActSeparateModelsSetting;
    private Boolean enableCheckpointsSetting;
    private Platform platform;

    private String environment;

    private boolean shouldShowAnnouncement;
    private List<HistoryItem> taskHistory;
    private TelemetrySetting telemetrySetting;
    private int shellIntegrationTimeout;
    private Boolean terminalReuseEnabled;
    private TerminalExecutionMode terminalExecutionMode;
    private int terminalOutputLineLimit;
    private int maxConsecutiveMistakes;
    private int subagentTerminalOutputLineLimit;
    private String defaultTerminalProfile;
    private Boolean backgroundCommandRunning;
    private String backgroundCommandTaskId;
    private Long lastCompletedCommandTs;
    private String version;
    private String distinctId;
    private Map<String, Boolean> globalClineRulesToggles;
    private Map<String, Boolean> globalWorkflowToggles;
    private Boolean mcpResponsesCollapsed;
    private Boolean strictPlanModeEnabled;
    private Boolean yoloModeToggled;
    private Boolean useAutoCondense;
    private FocusChainSettings focusChainSettings;
    private DictationSettings dictationSettings;
    private String customPrompt;
    private Double autoCondenseThreshold;
    private List<String> favoritedModelIds;

    private List<WorkspaceRoot> workspaceRoots;

    private int primaryRootIndex;
    private int lastDismissedInfoBannerVersion;
    private int lastDismissedModelBannerVersion;
    private int lastDismissedCliBannerVersion;
    private ClineFeatureSetting hooksEnabled;

    private Boolean subagentsEnabled;
}
