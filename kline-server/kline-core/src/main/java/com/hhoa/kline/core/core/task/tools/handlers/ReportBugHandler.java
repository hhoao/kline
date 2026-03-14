package com.hhoa.kline.core.core.task.tools.handlers;

import com.hhoa.ai.kline.commons.utils.JsonUtils;
import com.hhoa.kline.core.core.assistant.ToolUse;
import com.hhoa.kline.core.core.assistant.UserContentBlock;
import com.hhoa.kline.core.core.integrations.misc.ExtractText;
import com.hhoa.kline.core.core.prompts.ResponseFormatter;
import com.hhoa.kline.core.core.prompts.systemprompt.ClineToolSpec;
import com.hhoa.kline.core.core.shared.ClineAsk;
import com.hhoa.kline.core.core.shared.ClineMessageFormat;
import com.hhoa.kline.core.core.shared.ClineSay;
import com.hhoa.kline.core.core.shared.api.ApiProvider;
import com.hhoa.kline.core.core.shared.storage.types.Mode;
import com.hhoa.kline.core.core.task.AskResult;
import com.hhoa.kline.core.core.task.tools.types.TaskConfig;
import com.hhoa.kline.core.core.task.tools.types.UIHelpers;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

/** 报告问题（Bug）的工具处理器。 负责收集信息、走审批/反馈流程，并创建 GitHub Issue。 */
@Slf4j
public class ReportBugHandler implements FullyManagedTool {

    private final ResponseFormatter formatResponse = new ResponseFormatter();

    @Override
    public String getName() {
        return ClineAsk.REPORT_BUG.getValue();
    }

    @Override
    public String getDescription(ToolUse block) {
        return "[" + block.getName() + "]";
    }

    @Override
    public ClineToolSpec getClineToolSpec() {
        return ClineToolSpec.builder()
                .name(ClineAsk.REPORT_BUG.getValue())
                .parameters(
                        List.of(
                                ClineToolSpec.ClineToolSpecParameter.builder()
                                        .name("title")
                                        .required(true)
                                        .instruction("")
                                        .usage("")
                                        .build(),
                                ClineToolSpec.ClineToolSpecParameter.builder()
                                        .name("what_happened")
                                        .required(true)
                                        .instruction("")
                                        .usage("")
                                        .build(),
                                ClineToolSpec.ClineToolSpecParameter.builder()
                                        .name("steps_to_reproduce")
                                        .required(true)
                                        .instruction("")
                                        .usage("")
                                        .build(),
                                ClineToolSpec.ClineToolSpecParameter.builder()
                                        .name("api_request_output")
                                        .required(true)
                                        .instruction("")
                                        .usage("")
                                        .build(),
                                ClineToolSpec.ClineToolSpecParameter.builder()
                                        .name("additional_context")
                                        .required(true)
                                        .instruction("")
                                        .usage("")
                                        .build()))
                .build();
    }

    @Override
    public void handlePartialBlock(ToolUse block, UIHelpers ui) {
        Map<String, Object> partialMap = new HashMap<>();
        partialMap.put("title", getStringParam(block, "title"));
        partialMap.put("what_happened", getStringParam(block, "what_happened"));
        partialMap.put("steps_to_reproduce", getStringParam(block, "steps_to_reproduce"));
        partialMap.put("api_request_output", getStringParam(block, "api_request_output"));
        partialMap.put("additional_context", getStringParam(block, "additional_context"));
        String partial = JsonUtils.toJsonString(partialMap);
        ui.ask(ClineAsk.REPORT_BUG, partial, block.isPartial(), ClineMessageFormat.JSON);
    }

    @Override
    public List<UserContentBlock> execute(TaskConfig config, ToolUse block) {
        String title = getStringParam(block, "title");
        String what = getStringParam(block, "what_happened");
        String steps = getStringParam(block, "steps_to_reproduce");
        String output = getStringParam(block, "api_request_output");
        String ctx = getStringParam(block, "additional_context");

        config.getTaskState().setConsecutiveMistakeCount(0);

        if (config.getAutoApprovalSettings() != null
                && config.getAutoApprovalSettings().isEnabled()
                && config.getAutoApprovalSettings().isEnableNotifications()) {
            showSystemNotification(
                    "Cline wants to create a github issue...",
                    "Cline is suggesting to create a github issue with the title: " + title);
        }

        String operatingSystem =
                System.getProperty("os.name") + " " + System.getProperty("os.version");
        Mode currentMode = config.getMode();
        String clineVersion = getClineVersion();
        String hostPlatform = getHostPlatform();
        String hostVersion = getHostVersion();
        String systemInfo =
                String.format(
                        "%s: %s, Java: %s, Architecture: %s",
                        hostPlatform,
                        hostVersion,
                        System.getProperty("java.version"),
                        System.getProperty("os.arch"));

        ApiProvider apiProvider = getApiProvider(config, currentMode);
        String providerAndModel =
                String.format("%s / %s", apiProvider, config.getApi().getModel().getId());

        Map<String, Object> dataMap = new HashMap<>();
        dataMap.put("title", title);
        dataMap.put("what_happened", what);
        dataMap.put("steps_to_reproduce", steps);
        dataMap.put("api_request_output", output);
        dataMap.put("additional_context", ctx);
        dataMap.put("provider_and_model", providerAndModel);
        dataMap.put("operating_system", operatingSystem);
        dataMap.put("system_info", systemInfo);
        dataMap.put("cline_version", clineVersion);
        String data = JsonUtils.toJsonString(dataMap);

        AskResult res =
                config.getCallbacks()
                        .ask(ClineAsk.REPORT_BUG, data, false, ClineMessageFormat.JSON);
        String text = res != null ? res.getText() : null;
        String[] images =
                res != null && res.getImages() != null
                        ? res.getImages().toArray(new String[0])
                        : null;
        String[] files =
                res != null && res.getFiles() != null
                        ? res.getFiles().toArray(new String[0])
                        : null;

        boolean hasFeedback =
                (text != null && !text.isEmpty())
                        || (images != null && images.length > 0)
                        || (files != null && files.length > 0);
        if (hasFeedback) {
            String fileContentString = "";
            if (files != null && files.length > 0) {
                List<Path> filePaths = new ArrayList<>();
                for (String file : files) {
                    filePaths.add(Paths.get(file));
                }
                fileContentString = ExtractText.processFilesIntoText(filePaths);
            }

            config.getCallbacks()
                    .say(
                            ClineSay.USER_FEEDBACK,
                            text == null ? "" : text,
                            images,
                            files,
                            false,
                            null);

            return HandlerUtils.createTextBlocks(
                    formatResponse.toolResult(
                            "The user did not submit the bug, and provided feedback on the Github issue generated instead:\n<feedback>\n"
                                    + (text == null ? "" : text)
                                    + "\n</feedback>",
                            images,
                            fileContentString));
        }

        try {
            Map<String, String> params = new HashMap<>();
            params.put("title", title);
            params.put("operating-system", operatingSystem);
            params.put("cline-version", clineVersion);
            params.put("system-info", systemInfo);
            params.put("additional-context", ctx);
            params.put("what-happened", what);
            params.put("steps", steps);
            params.put("provider-model", providerAndModel);
            params.put("logs", output);

            String issueUrl = createGitHubIssueUrl("cline", "cline", "bug_report.yml", params);
            openUrlInBrowser(issueUrl);
        } catch (Exception error) {
            log.error(
                    "An error occurred while attempting to report the bug: {}",
                    error.getMessage(),
                    error);
        }

        return HandlerUtils.createTextBlocks(
                formatResponse.toolResult(
                        "The user accepted the creation of the Github issue.", null, null));
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private static String getStringParam(ToolUse block, String key) {
        return HandlerUtils.getStringParam(block, key);
    }

    private static String getClineVersion() {
        // TODO: 从配置或环境变量中获取实际版本号
        try {
            // 尝试从类路径或配置中读取版本
            return "unknown";
        } catch (Exception e) {
            return "unknown";
        }
    }

    private static String getHostPlatform() {
        // TODO: 从 HostProvider 获取实际平台信息
        return System.getProperty("os.name", "Unknown");
    }

    private static String getHostVersion() {
        // TODO: 从 HostProvider 获取实际版本信息
        return System.getProperty("os.version", "Unknown");
    }

    private static ApiProvider getApiProvider(TaskConfig config, Mode mode) {
        try {
            if (config.getServices() != null
                    && config.getServices().getStateManager() != null
                    && config.getServices().getStateManager()
                            instanceof TaskConfig.ApiConfiguration apiConfig) {
                return mode == Mode.PLAN
                        ? apiConfig.getPlanModeApiProvider()
                        : apiConfig.getActModeApiProvider();
            }
        } catch (Exception e) {
            log.debug("Failed to get API provider from config", e);
        }
        return ApiProvider.UNKNOWN;
    }

    private static String createGitHubIssueUrl(
            String repoOwner, String repoName, String issueTemplate, Map<String, String> params) {
        String baseUrl = String.format("https://github.com/%s/%s/issues/new", repoOwner, repoName);

        if (issueTemplate != null && !issueTemplate.isEmpty()) {
            params = new HashMap<>(params);
            params.put("template", issueTemplate);
        }

        StringBuilder queryParts = new StringBuilder();
        boolean first = true;
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (!first) {
                queryParts.append("&");
            }
            first = false;
            String encodedKey = URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8);
            String encodedValue = URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8);
            queryParts.append(encodedKey).append("=").append(encodedValue);
        }

        return baseUrl + "?" + queryParts;
    }

    private static void openUrlInBrowser(String url) {
        try {
            String os = System.getProperty("os.name").toLowerCase();
            ProcessBuilder pb;

            if (os.contains("win")) {
                pb = new ProcessBuilder("cmd", "/c", "start", "\"\"", url);
            } else if (os.contains("mac")) {
                pb = new ProcessBuilder("open", url);
            } else {
                pb = new ProcessBuilder("xdg-open", url);
            }

            pb.start();
            log.info("Opened GitHub issue URL in browser: {}", url);
        } catch (Exception e) {
            log.error("Failed to open URL in browser: {}", url, e);
            log.info("Please manually open this URL: {}", url);
        }
    }

    private static void showSystemNotification(String subtitle, String message) {
        // TODO: 实现实际的系统通知功能
        log.info("[Notification] {}: {}", subtitle, message);
    }
}
