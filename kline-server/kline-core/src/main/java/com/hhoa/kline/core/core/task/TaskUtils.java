package com.hhoa.kline.core.core.task;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class TaskUtils {

    private TaskUtils() {}

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public interface NotificationSender {
        /**
         * 发送系统通知。
         *
         * @param subtitle 副标题
         * @param message 内容
         */
        void send(String subtitle, String message);
    }

    public static void showNotificationForApprovalIfAutoApprovalEnabled(
            String message,
            boolean autoApprovalSettingsEnabled,
            boolean notificationsEnabled,
            NotificationSender sender) {
        if (autoApprovalSettingsEnabled && notificationsEnabled && sender != null) {
            sender.send("Approval Required", message);
        }
    }

    @lombok.Data
    public static final class UpdateApiReqMsgParams {
        public MessageStateHandler messageStateHandler;
        public int lastApiReqIndex;
        public int inputTokens;
        public int outputTokens;
        public int cacheWriteTokens;
        public int cacheReadTokens;
        public Double totalCost;
        public String cancelReason;
        public String streamingFailedMessage;
    }

    /** 使用 Token 与成本信息更新 API 请求消息。 会解析现有消息的JSON文本，保留所有字段（除了retryStatus），然后更新tokens和cost等信息。 */
    public static void updateApiReqMsg(UpdateApiReqMsgParams params) {
        List<ClineMessage> clineMessages = params.messageStateHandler.getClineMessages();
        if (params.lastApiReqIndex < 0 || params.lastApiReqIndex >= clineMessages.size()) {
            throw new IllegalArgumentException("Invalid message index: " + params.lastApiReqIndex);
        }

        ClineMessage targetMessage = clineMessages.get(params.lastApiReqIndex);
        String currentText = targetMessage.getText();

        ObjectNode apiReqInfo;
        try {
            if (currentText == null || currentText.trim().isEmpty()) {
                apiReqInfo = objectMapper.createObjectNode();
            } else {
                apiReqInfo = (ObjectNode) objectMapper.readTree(currentText);
            }
        } catch (Exception e) {
            log.warn(
                    "Failed to parse existing API req info JSON, creating new: {}", e.getMessage());
            apiReqInfo = objectMapper.createObjectNode();
        }

        apiReqInfo.remove("retryStatus");

        apiReqInfo.put("tokensIn", params.inputTokens);
        apiReqInfo.put("tokensOut", params.outputTokens);
        apiReqInfo.put("cacheWrites", params.cacheWriteTokens);
        apiReqInfo.put("cacheReads", params.cacheReadTokens);

        if (params.totalCost != null) {
            apiReqInfo.put("cost", params.totalCost);
        } else if (!apiReqInfo.has("cost")) {
            apiReqInfo.put("cost", 0.0);
        }

        if (params.cancelReason != null) {
            apiReqInfo.put("cancelReason", params.cancelReason);
        }

        if (params.streamingFailedMessage != null) {
            apiReqInfo.put("streamingFailedMessage", params.streamingFailedMessage);
        }

        try {
            String updatedJson = objectMapper.writeValueAsString(apiReqInfo);
            ClineMessage updates = new ClineMessage();
            updates.setText(updatedJson);
            params.messageStateHandler.updateClineMessage(params.lastApiReqIndex, updates);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize updated API req info", e);
        }
    }

    private static final List<String> CLI_TOOLS =
            Arrays.asList(
                    "gh",
                    "git",
                    "docker",
                    "podman",
                    "kubectl",
                    "aws",
                    "gcloud",
                    "az",
                    "terraform",
                    "pulumi",
                    "npm",
                    "yarn",
                    "pnpm",
                    "pip",
                    "cargo",
                    "go",
                    "curl",
                    "jq",
                    "make",
                    "cmake",
                    "python",
                    "node",
                    "psql",
                    "mysql",
                    "redis-cli",
                    "sqlite3",
                    "mongosh",
                    "code",
                    "grep",
                    "sed",
                    "awk",
                    "brew",
                    "apt",
                    "yum",
                    "gradle",
                    "mvn",
                    "bundle",
                    "dotnet",
                    "helm",
                    "ansible",
                    "wget");

    /** 检测系统 PATH 中可用的常见 CLI 工具。 */
    public static List<String> detectAvailableCliTools() {
        List<String> available = new ArrayList<>();
        boolean isWindows = System.getProperty("os.name").toLowerCase().contains("win");
        String check = isWindows ? "where" : "which";
        for (String cmd : CLI_TOOLS) {
            try {
                Process process = new ProcessBuilder(check, cmd).redirectErrorStream(true).start();
                int code = process.waitFor();
                if (code == 0) {
                    available.add(cmd);
                }
            } catch (IOException | InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
        }
        return available;
    }
}
