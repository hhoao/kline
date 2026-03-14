package com.hhoa.kline.core.core.commands;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hhoa.kline.core.core.context.tracking.TaskMetadata;
import com.hhoa.kline.core.core.controller.HistoryItem;
import com.hhoa.kline.core.core.shared.ClineMessageType;
import com.hhoa.kline.core.core.shared.ClineSay;
import com.hhoa.kline.core.core.task.ClineMessage;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
 * 从现有任务文件夹重建任务历史
 *
 * @author hhoa
 */
@Slf4j
public class ReconstructTaskHistory {

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final Pattern NUMERIC_TASK_ID_PATTERN = Pattern.compile("^\\d+$");

    /** 任务重建结果 */
    @Data
    public static class TaskReconstructionResult {
        private int totalTasks = 0;
        private int reconstructedTasks = 0;
        private int skippedTasks = 0;
        private List<String> errors = new ArrayList<>();
        private List<HistoryItem> reconstructedItems;
    }

    /**
     * 重建任务历史（主入口方法）
     *
     * @return 重建结果
     * @throws Exception 如果重建失败
     */
    public static TaskReconstructionResult reconstructTaskHistory(
            String globalStoragePath,
            List<HistoryItem> existingHistory,
            List<ClineMessage> clineMessages,
            TaskMetadata metadata)
            throws Exception {
        try {
            log.info("开始重建任务历史...");

            TaskReconstructionResult result =
                    performTaskHistoryReconstruction(
                            globalStoragePath, existingHistory, clineMessages, metadata);

            if (!result.getErrors().isEmpty()) {
                String errorMessage =
                        String.format(
                                "重建完成，但有警告:\n- 已重建: %d 个任务\n- 已跳过: %d 个任务\n- 错误数: %d\n\n前几个错误:\n%s",
                                result.getReconstructedTasks(),
                                result.getSkippedTasks(),
                                result.getErrors().size(),
                                result.getErrors().stream()
                                        .limit(3)
                                        .collect(Collectors.joining("\n")));
                log.warn(errorMessage);
            } else {
                log.info("任务历史重建成功！找到并恢复了 {} 个任务。", result.getReconstructedTasks());
            }

            return result;
        } catch (Exception error) {
            String errorMessage = error.getMessage();
            log.error("重建任务历史失败: {}", errorMessage, error);
            throw new Exception("重建任务历史失败: " + errorMessage, error);
        }
    }

    /**
     * 执行任务历史重建
     *
     * @return 重建结果
     * @throws Exception 如果重建失败
     */
    private static TaskReconstructionResult performTaskHistoryReconstruction(
            String globalStoragePath,
            List<HistoryItem> existingHistory,
            List<ClineMessage> clineMessages,
            TaskMetadata metadata)
            throws Exception {
        TaskReconstructionResult result = new TaskReconstructionResult();

        backupExistingTaskHistory(globalStoragePath, existingHistory);

        String tasksDir = Paths.get(globalStoragePath, "tasks").toString();

        if (!Files.exists(Paths.get(tasksDir))) {
            throw new Exception("未找到任务目录。没有可重建的内容。");
        }

        List<String> taskIds = scanTaskDirectories(tasksDir);
        result.setTotalTasks(taskIds.size());

        if (taskIds.isEmpty()) {
            throw new Exception("未找到任务目录。没有可重建的内容。");
        }

        List<HistoryItem> reconstructedItems = new ArrayList<>();

        for (String taskId : taskIds) {
            try {
                HistoryItem historyItem =
                        reconstructTaskHistoryItem(clineMessages, taskId, metadata);
                if (historyItem != null) {
                    reconstructedItems.add(historyItem);
                    result.setReconstructedTasks(result.getReconstructedTasks() + 1);
                } else {
                    result.setSkippedTasks(result.getSkippedTasks() + 1);
                }
            } catch (Exception error) {
                result.setSkippedTasks(result.getSkippedTasks() + 1);
                String errorMsg = error.getMessage();
                result.getErrors().add("Task " + taskId + ": " + errorMsg);
            }
        }

        // 按时间戳排序（最新的在前）
        reconstructedItems.sort((a, b) -> Long.compare(b.getTs(), a.getTs()));

        result.setReconstructedItems(reconstructedItems);

        return result;
    }

    /** 备份现有任务历史 */
    private static void backupExistingTaskHistory(
            String globalStoragePath, List<HistoryItem> existingHistory) {
        try {
            if (!existingHistory.isEmpty()) {
                String backupPath =
                        Paths.get(
                                        globalStoragePath,
                                        "state",
                                        "taskHistory.backup."
                                                + System.currentTimeMillis()
                                                + ".json")
                                .toString();

                Path backupDir = Paths.get(backupPath).getParent();
                if (backupDir != null) {
                    Files.createDirectories(backupDir);
                }
                objectMapper
                        .writerWithDefaultPrettyPrinter()
                        .writeValue(new File(backupPath), existingHistory);
                log.info("已备份现有任务历史到: {}", backupPath);
            }
        } catch (Exception error) {
            // 非致命错误，仅记录
            log.warn("备份现有任务历史失败: {}", error.getMessage());
        }
    }

    /**
     * 扫描任务目录
     *
     * @param tasksDir 任务目录路径
     * @return 任务ID列表
     * @throws Exception 如果扫描失败
     */
    private static List<String> scanTaskDirectories(String tasksDir) throws Exception {
        try {
            Path tasksPath = Paths.get(tasksDir);
            if (!Files.exists(tasksPath) || !Files.isDirectory(tasksPath)) {
                return new ArrayList<>();
            }

            return Files.list(tasksPath)
                    .filter(Files::isDirectory)
                    .map(path -> path.getFileName().toString())
                    .filter(name -> NUMERIC_TASK_ID_PATTERN.matcher(name).matches())
                    .collect(Collectors.toList());
        } catch (Exception error) {
            throw new Exception("Failed to scan tasks directory: " + error.getMessage(), error);
        }
    }

    /**
     * 重建单个任务历史项
     *
     * @param taskId 任务ID
     * @return 历史项，如果任务为空则返回 null
     * @throws Exception 如果重建失败
     */
    private static HistoryItem reconstructTaskHistoryItem(
            List<ClineMessage> clineMessages, String taskId, TaskMetadata metadata)
            throws Exception {
        try {
            if (clineMessages.isEmpty()) {
                return null;
            }

            TaskInfo taskInfo = extractTaskInformation(clineMessages, metadata);

            HistoryItem historyItem =
                    HistoryItem.builder()
                            .id(taskId)
                            .ulid(taskInfo.getUlid() != null ? taskInfo.getUlid() : generateUlid())
                            .ts(taskInfo.getTimestamp())
                            .task(taskInfo.getTaskDescription())
                            .tokensIn(taskInfo.getTokensIn())
                            .tokensOut(taskInfo.getTokensOut())
                            .cacheWrites(taskInfo.getCacheWrites())
                            .cacheReads(taskInfo.getCacheReads())
                            .totalCost(taskInfo.getTotalCost())
                            .size(taskInfo.getSize())
                            .favorited(taskInfo.getIsFavorited())
                            .conversationHistoryDeletedRange(
                                    taskInfo.getConversationHistoryDeletedRange())
                            .build();

            return historyItem;
        } catch (Exception error) {
            throw new Exception(
                    "Failed to reconstruct task " + taskId + ": " + error.getMessage(), error);
        }
    }

    /** 任务信息 */
    @Data
    private static class TaskInfo {
        private String ulid;
        private Long timestamp;
        private String taskDescription;
        private Integer tokensIn;
        private Integer tokensOut;
        private Integer cacheWrites;
        private Integer cacheReads;
        private Double totalCost;
        private Long size;
        private Boolean isFavorited;
        private int[] conversationHistoryDeletedRange;
    }

    /**
     * 提取任务信息
     *
     * @param clineMessages Cline 消息列表
     * @param metadata 任务元数据
     * @return 任务信息
     */
    private static TaskInfo extractTaskInformation(
            List<ClineMessage> clineMessages, TaskMetadata metadata) {
        TaskInfo taskInfo = new TaskInfo();

        // 查找第一个用户消息（任务描述）
        ClineMessage firstUserMessage =
                clineMessages.stream()
                        .filter(
                                msg ->
                                        ClineMessageType.SAY.equals(msg.getType())
                                                && ClineSay.TEXT.equals(msg.getSay())
                                                && msg.getText() != null)
                        .findFirst()
                        .orElse(null);

        // 从第一条消息提取时间戳，或使用任务ID作为回退
        Long timestamp =
                clineMessages.isEmpty() ? System.currentTimeMillis() : clineMessages.get(0).getTs();

        String taskDescription = "Untitled Task";
        if (firstUserMessage != null && firstUserMessage.getText() != null) {
            String cleanText =
                    firstUserMessage
                            .getText()
                            .replaceAll("<task>\\s*", "")
                            .replaceAll("\\s*</task>", "")
                            .trim();

            String[] lines = cleanText.split("\n");
            if (lines.length > 0 && !lines[0].isEmpty()) {
                taskDescription = lines[0].substring(0, Math.min(lines[0].length(), 100));
            }
        }

        // 从 API 请求消息计算令牌使用情况
        int tokensIn = 0;
        int tokensOut = 0;
        int cacheWrites = 0;
        int cacheReads = 0;
        double totalCost = 0.0;

        // 查找带有令牌信息的 api_req_started 消息
        List<ClineMessage> apiReqMessages =
                clineMessages.stream()
                        .filter(
                                msg ->
                                        ClineMessageType.SAY.equals(msg.getType())
                                                && ClineSay.API_REQ_STARTED.equals(msg.getSay())
                                                && msg.getText() != null)
                        .toList();

        for (ClineMessage msg : apiReqMessages) {
            try {
                if (msg.getText() != null) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> apiInfo = objectMapper.readValue(msg.getText(), Map.class);
                    if (apiInfo.get("tokensIn") != null) {
                        tokensIn += ((Number) apiInfo.get("tokensIn")).intValue();
                    }
                    if (apiInfo.get("tokensOut") != null) {
                        tokensOut += ((Number) apiInfo.get("tokensOut")).intValue();
                    }
                    if (apiInfo.get("cacheWrites") != null) {
                        cacheWrites += ((Number) apiInfo.get("cacheWrites")).intValue();
                    }
                    if (apiInfo.get("cacheReads") != null) {
                        cacheReads += ((Number) apiInfo.get("cacheReads")).intValue();
                    }
                    if (apiInfo.get("cost") != null) {
                        totalCost += ((Number) apiInfo.get("cost")).doubleValue();
                    }
                }
            } catch (Exception e) {
                // 忽略解析错误
                log.debug("解析 API 信息失败: {}", e.getMessage());
            }
        }

        // 计算近似大小（粗略估计）
        try {
            String messageSize = objectMapper.writeValueAsString(clineMessages);
            long size = (long) Math.floor(messageSize.length() / 1024.0);
            taskInfo.setSize(size);
        } catch (Exception e) {
            log.debug("计算消息大小失败: {}", e.getMessage());
        }

        taskInfo.setTimestamp(timestamp);
        taskInfo.setTaskDescription(taskDescription);
        taskInfo.setTokensIn(tokensIn);
        taskInfo.setTokensOut(tokensOut);
        taskInfo.setCacheWrites(cacheWrites > 0 ? cacheWrites : null);
        taskInfo.setCacheReads(cacheReads > 0 ? cacheReads : null);
        taskInfo.setTotalCost(totalCost);

        return taskInfo;
    }

    /**
     * 注意：这是简化实现，实际应使用 ULID 库
     *
     * @return ULID 字符串
     */
    private static String generateUlid() {
        // 简化实现：使用 UUID 并截取前 26 个字符
        // 实际应使用 ULID 库（如 de.huxhorn.sulky:ulid）
        return UUID.randomUUID().toString().replace("-", "").substring(0, 26);
    }
}
