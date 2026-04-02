package com.hhoa.kline.core.core.shared.storage;

import com.hhoa.kline.core.core.services.mcp.McpServerConfig;
import com.hhoa.kline.core.core.shared.AutoApprovalSettings;
import com.hhoa.kline.core.core.shared.DictationSettings;
import com.hhoa.kline.core.core.shared.FocusChainSettings;
import com.hhoa.kline.core.core.shared.TelemetrySetting;
import com.hhoa.kline.core.core.shared.api.ApiProvider;
import com.hhoa.kline.core.core.shared.proto.cline.BrowserSettings;
import com.hhoa.kline.core.core.shared.storage.types.Mode;
import com.hhoa.kline.core.core.shared.storage.types.OpenaiReasoningEffort;
import java.util.HashMap;
import java.util.Map;
import lombok.Data;

@Data
public class Settings {
    private String awsRegion = null;
    private boolean awsUseCrossRegionInference = false;
    private boolean awsUseGlobalInference = false;
    private String awsProfile = null;
    private String awsAuthentication = null;
    private boolean awsUseProfile = false;

    private String vertexProjectId = null;
    private String vertexRegion = null;

    private Map<String, String> openAiHeaders = new HashMap<>();
    private String azureApiVersion = null;
    private String openRouterProviderSorting = null;

    private AutoApprovalSettings autoApprovalSettings = new AutoApprovalSettings();
    private Map<String, Boolean> globalWorkflowToggles = new HashMap<>();
    private Map<String, Boolean> globalClineRulesToggles = new HashMap<>();
    private Map<String, Boolean> globalSkillsToggles = new HashMap<>();
    private Map<String, Boolean> remoteRulesToggles = new HashMap<>();
    private BrowserSettings browserSettings = new BrowserSettings();

    private TelemetrySetting telemetrySetting = TelemetrySetting.UNSET;
    private boolean planActSeparateModelsSetting = false;
    private boolean enableCheckpointsSetting = true;

    private int requestTimeoutMs = 0;
    private int shellIntegrationTimeout = 4000;
    private String defaultTerminalProfile = "default";
    private int terminalOutputLineLimit = 500;
    private int maxConsecutiveMistakes = 3;
    private int subagentTerminalOutputLineLimit = 2000;

    private String sapAiResourceGroup = null;
    private boolean sapAiCoreUseOrchestrationMode = true;

    private String claudeCodePath = null;
    private String qwenCodeOauthPath = null;
    private boolean strictPlanModeEnabled = true;
    private boolean yoloModeToggled = false;

    /** 与 Cline {@code autoApproveAllToggled} 一致：一键自动批准（会话/全局由存储层决定）。 */
    private boolean autoApproveAllToggled = false;

    private boolean useAutoCondense = false;
    private String preferredLanguage = "English";
    private OpenaiReasoningEffort openaiReasoningEffort = OpenaiReasoningEffort.MEDIUM;
    private Mode mode = Mode.ACT;
    private DictationSettings dictationSettings = new DictationSettings();
    private FocusChainSettings focusChainSettings = new FocusChainSettings();
    private String customPrompt = null;
    private double autoCondenseThreshold = 0.75;
    private String ocaMode = "internal";
    private boolean hooksEnabled = false;
    private boolean subagentsEnabled = false;

    private ApiProvider planModeApiProvider = null;
    private String planModeApiModelId = null;
    private int planModeThinkingBudgetTokens = 1024;
    private String planModeReasoningEffort = null;
    private Map<ApiProvider, ModelConfig> planModeModelConfigs = new HashMap<>();

    private ApiProvider actModeApiProvider = null;
    private String actModeApiModelId = null;
    private Integer actModeThinkingBudgetTokens = 1024;
    private String actModeReasoningEffort = null;
    private Map<ApiProvider, ModelConfig> actModeModelConfigs = new HashMap<>();

    private boolean openTelemetryEnabled = true;
    private String openTelemetryMetricsExporter = null;
    private String openTelemetryLogsExporter = null;
    private String openTelemetryOtlpProtocol = "http/json";
    private String openTelemetryOtlpEndpoint = "http://localhost:4318";
    private String openTelemetryOtlpMetricsProtocol = null;
    private String openTelemetryOtlpMetricsEndpoint = null;
    private String openTelemetryOtlpLogsProtocol = null;
    private String openTelemetryOtlpLogsEndpoint = null;
    private int openTelemetryMetricExportInterval = 60000;
    private boolean openTelemetryOtlpInsecure = false;
    private int openTelemetryLogBatchSize = 512;
    private int openTelemetryLogBatchTimeout = 5000;
    private int openTelemetryLogMaxQueueSize = 2048;

    private Map<String, McpServerConfig> mcpServers = new HashMap<>();
}
