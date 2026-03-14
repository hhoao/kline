package com.hhoa.kline.core.core.context.management;

import com.fasterxml.jackson.core.type.TypeReference;
import com.hhoa.ai.kline.commons.utils.JsonUtils;
import com.hhoa.kline.core.core.assistant.AssistantMessage;
import com.hhoa.kline.core.core.assistant.ContentBlockType;
import com.hhoa.kline.core.core.assistant.ImageContentBlock;
import com.hhoa.kline.core.core.assistant.MessageParam;
import com.hhoa.kline.core.core.assistant.MessageRole;
import com.hhoa.kline.core.core.assistant.TextContentBlock;
import com.hhoa.kline.core.core.assistant.UserContentBlock;
import com.hhoa.kline.core.core.assistant.UserMessage;
import com.hhoa.kline.core.core.prompts.ResponseFormatter;
import com.hhoa.kline.core.core.shared.ClineApiReqInfo;
import com.hhoa.kline.core.core.shared.ClineSay;
import com.hhoa.kline.core.core.storage.GlobalFileNames;
import com.hhoa.kline.core.core.task.ClineMessage;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;

/** 上下文管理器 负责管理对话历史的上下文优化和截断 */
@Slf4j
public class LocalContextManager implements ContextManager {
    /**
     * 从 apiMessages 外部索引到内部消息索引的映射，再到实际更改的列表，按时间戳排序 时间戳是必需的，以支持完整的检查点，其中我们应用的更改需要能够在
     * 移动到较早的对话历史检查点时撤消 - 这种排序直观地允许对截断进行二分搜索 每个还存储一个数字（EditType），用于定义它是哪种消息类型，以进行自定义处理
     *
     * <p>格式：{ outerIndex => [EditType, { innerIndex => [[timestamp, updateType, update, metadata],
     * ...] }] } 示例：{ 1 => [0, { 0 => [[<timestamp>, "text", ["[NOTE] Some previous conversation
     * history..."], []] }] } 上面的示例是我们如何更新第一条助手消息以指示我们截断了文本
     */
    private final Map<Integer, InnerTuple> contextHistoryUpdates;

    private final ResponseFormatter responseFormatter;
    private final String taskDirectory;

    public LocalContextManager(String taskDirectory) {
        this.contextHistoryUpdates = new ConcurrentHashMap<>();
        this.responseFormatter = new ResponseFormatter();
        this.taskDirectory = taskDirectory;
    }

    @Override
    public void initializeContextHistory() {
        Map<Integer, InnerTuple> loaded = getSavedContextHistory();
        contextHistoryUpdates.clear();
        contextHistoryUpdates.putAll(loaded);
    }

    private Map<Integer, InnerTuple> getSavedContextHistory() {
        try {
            String filePath = Paths.get(taskDirectory, GlobalFileNames.CONTEXT_HISTORY).toString();
            File file = new File(filePath);
            if (file.exists()) {
                String json = new String(Files.readAllBytes(Paths.get(filePath)));
                // 反序列化格式：Array<[messageIndex, [EditType, Array<[blockIndex, ContextUpdate[]]>]>]>
                List<SerializedEntry> serializedUpdates =
                        JsonUtils.parseObject(json, new TypeReference<List<SerializedEntry>>() {});

                Map<Integer, InnerTuple> result = new HashMap<>();
                for (SerializedEntry entry : serializedUpdates) {
                    int messageIndex = entry.getMessageIndex();
                    InnerTuple innerTuple = entry.getTuple();
                    result.put(messageIndex, innerTuple);
                }
                return result;
            }
        } catch (Exception e) {
            log.error("Failed to load context history: " + e.getMessage());
        }
        return new HashMap<>();
    }

    private void saveContextHistory() {
        try {
            String filePath = Paths.get(taskDirectory, GlobalFileNames.CONTEXT_HISTORY).toString();
            // 序列化格式：Array<[messageIndex, [EditType, Array<[blockIndex, ContextUpdate[]]>]>]>
            List<SerializedEntry> serializedUpdates = new ArrayList<>();

            for (Map.Entry<Integer, InnerTuple> entry : contextHistoryUpdates.entrySet()) {
                Integer messageIndex = entry.getKey();
                InnerTuple innerTuple = entry.getValue();
                serializedUpdates.add(new SerializedEntry(messageIndex, innerTuple));
            }

            String json = JsonUtils.toJsonString(serializedUpdates);
            Files.write(Paths.get(filePath), json.getBytes());
        } catch (Exception e) {
            log.error("Failed to save context history: " + e.getMessage());
        }
    }

    @Override
    public boolean shouldCompactContextWindow(
            List<ClineMessage> clineMessages,
            int contextWindow,
            int maxAllowedSize,
            int previousApiReqIndex,
            Double thresholdPercentage) {

        if (previousApiReqIndex >= 0 && previousApiReqIndex < clineMessages.size()) {
            ClineMessage previousRequest = clineMessages.get(previousApiReqIndex);
            if (previousRequest != null && previousRequest.getText() != null) {
                int totalTokens =
                        JsonUtils.parseObject(previousRequest.getText(), ClineApiReqInfo.class)
                                .getTotalTokens();

                int thresholdTokens;
                if (thresholdPercentage != null) {
                    int roundedThreshold = (int) Math.floor(contextWindow * thresholdPercentage);
                    thresholdTokens = Math.min(roundedThreshold, maxAllowedSize);
                } else {
                    thresholdTokens = maxAllowedSize;
                }

                return totalTokens >= thresholdTokens;
            }
        }

        return false;
    }

    @Override
    public ContextMessagesResult getNewContextMessagesAndMetadata(
            List<MessageParam> apiConversationHistory,
            List<ClineMessage> clineMessages,
            int contextWindow,
            int maxAllowedSize,
            int[] conversationHistoryDeletedRange,
            int previousApiReqIndex,
            boolean useAutoCondense) {

        boolean updatedConversationHistoryDeletedRange = false;

        if (!useAutoCondense) {
            // 如果上一个 API 请求的总令牌使用量接近上下文窗口，则截断对话历史以为新请求腾出空间
            if (previousApiReqIndex >= 0 && previousApiReqIndex < clineMessages.size()) {
                ClineMessage previousRequest = clineMessages.get(previousApiReqIndex);
                if (previousRequest != null && previousRequest.getText() != null) {
                    long timestamp = previousRequest.getTs();

                    try {
                        ClineApiReqInfo apiReqInfo =
                                JsonUtils.parseObject(
                                        previousRequest.getText(), ClineApiReqInfo.class);

                        int totalTokens = apiReqInfo.getTotalTokens();

                        // 这是知道我们何时接近上下文窗口的最可靠方法
                        if (totalTokens >= maxAllowedSize) {
                            // 由于用户可能在具有不同上下文窗口的模型之间切换，截断一半可能不够
                            // 因此，如果 totalTokens/2 大于 maxAllowedSize，我们截断 3/4 而不是 1/2
                            KeepStrategy keep =
                                    totalTokens / 2 > maxAllowedSize
                                            ? KeepStrategy.QUARTER
                                            : KeepStrategy.HALF;

                            // 稍后我们检查修剪了多少字符以确定是否仍应截断历史
                            ContextOptimizationResult optimizationResult =
                                    applyContextOptimizations(
                                            apiConversationHistory,
                                            conversationHistoryDeletedRange != null
                                                    ? conversationHistoryDeletedRange[1] + 1
                                                    : 2,
                                            timestamp);

                            boolean needToTruncate = true;
                            if (optimizationResult.anyContextUpdates) {
                                double charactersSavedPercentage =
                                        calculateContextOptimizationMetrics(
                                                apiConversationHistory,
                                                conversationHistoryDeletedRange,
                                                optimizationResult.uniqueFileReadIndices);
                                if (charactersSavedPercentage >= 0.3) {
                                    needToTruncate = false;
                                }
                            }

                            if (needToTruncate) {
                                boolean truncationNoticeApplied =
                                        applyStandardContextTruncationNoticeChange(timestamp);
                                optimizationResult.anyContextUpdates =
                                        truncationNoticeApplied
                                                || optimizationResult.anyContextUpdates;

                                // 注意：在恢复任务中覆盖 ConversationHistory 是可以的，因为我们只删除最后一条用户消息
                                // 而不是中间的任何内容，这不会影响此范围
                                conversationHistoryDeletedRange =
                                        getNextTruncationRange(
                                                apiConversationHistory,
                                                conversationHistoryDeletedRange,
                                                keep);

                                updatedConversationHistoryDeletedRange = true;
                            }

                            if (optimizationResult.anyContextUpdates) {
                                saveContextHistory();
                            }
                        }
                    } catch (Exception e) {
                        log.error("Error processing context management: " + e.getMessage());
                    }
                }
            }
        }

        List<MessageParam> truncatedConversationHistory =
                getAndAlterTruncatedMessages(
                        apiConversationHistory, conversationHistoryDeletedRange);

        return new ContextMessagesResult(
                conversationHistoryDeletedRange,
                updatedConversationHistoryDeletedRange,
                truncatedConversationHistory);
    }

    @Override
    public ContextTelemetryData getContextTelemetryData(
            List<ClineMessage> clineMessages, int contextWindow, Integer triggerIndex) {

        int targetIndex;
        if (triggerIndex != null) {
            targetIndex = triggerIndex;
        } else {
            List<Integer> apiReqIndices = new ArrayList<>();
            for (int i = 0; i < clineMessages.size(); i++) {
                if (ClineSay.API_REQ_STARTED.equals(clineMessages.get(i).getSay())) {
                    apiReqIndices.add(i);
                }
            }

            // 我们想要倒数第二个 API 请求（导致摘要的那个）
            targetIndex =
                    apiReqIndices.size() >= 2 ? apiReqIndices.get(apiReqIndices.size() - 2) : -1;
        }

        if (targetIndex >= 0 && targetIndex < clineMessages.size()) {
            ClineMessage targetRequest = clineMessages.get(targetIndex);
            if (targetRequest != null && targetRequest.getText() != null) {
                try {
                    ClineApiReqInfo apiReqInfo =
                            JsonUtils.parseObject(targetRequest.getText(), ClineApiReqInfo.class);

                    int tokensUsed = apiReqInfo.getTotalTokens();

                    return new ContextTelemetryData(tokensUsed, contextWindow);
                } catch (Exception e) {
                    log.error(
                            "Error parsing API request info for context telemetry: "
                                    + e.getMessage());
                }
            }
        }
        return null;
    }

    @Override
    public int[] getNextTruncationRange(
            List<MessageParam> apiMessages, int[] currentDeletedRange, KeepStrategy keep) {

        int rangeStartIndex = 2;
        int startOfRest = currentDeletedRange != null ? currentDeletedRange[1] + 1 : 2;

        int messagesToRemove;
        switch (keep) {
            case NONE:
                messagesToRemove = Math.max(apiMessages.size() - startOfRest, 0);
                break;
            case LAST_TWO:
                messagesToRemove = Math.max(apiMessages.size() - startOfRest - 2, 0);
                break;
            case HALF:
                messagesToRemove = (int) Math.floor((apiMessages.size() - startOfRest) / 4.0) * 2;
                break;
            case QUARTER:
                messagesToRemove =
                        (int) Math.floor(((apiMessages.size() - startOfRest) * 3) / 4.0 / 2) * 2;
                break;
            default:
                messagesToRemove = 0;
        }

        int rangeEndIndex = startOfRest + messagesToRemove - 1;

        // 确保被删除的最后一条消息是助手消息，这样初始用户-助手对之后的下一条消息是助手消息。
        // 这保留了用户-助手-用户-助手结构。
        // 注意：anthropic 格式消息始终是用户-助手-用户-助手，而 openai 格式消息可以有多个连续的用户消息
        // （我们在整个 cline 中使用 anthropic 格式）
        if (rangeEndIndex >= 0 && rangeEndIndex < apiMessages.size()) {
            MessageParam messageParam = apiMessages.get(rangeEndIndex);
            if (messageParam.getRole() != MessageRole.ASSISTANT) {
                rangeEndIndex -= 1;
            }
        }

        return new int[] {rangeStartIndex, rangeEndIndex};
    }

    @Override
    public List<MessageParam> getTruncatedMessages(
            List<MessageParam> messages, int[] deletedRange) {
        return getAndAlterTruncatedMessages(messages, deletedRange);
    }

    private List<MessageParam> getAndAlterTruncatedMessages(
            List<MessageParam> messages, int[] deletedRange) {
        if (messages.size() <= 1) {
            return messages;
        }

        int startFromIndex = deletedRange != null ? deletedRange[1] + 1 : 2;
        return applyContextHistoryUpdates(messages, startFromIndex);
    }

    private List<MessageParam> applyContextHistoryUpdates(
            List<MessageParam> messages, int startFromIndex) {
        // 运行时间与用户消息长度成线性关系，如果期望有限数量的更改，
        // 可以更优化地循环更改

        int firstChunkSize = Math.min(2, messages.size());
        List<MessageParam> firstChunk = new ArrayList<>(messages.subList(0, firstChunkSize));
        List<MessageParam> secondChunk =
                new ArrayList<>(messages.subList(startFromIndex, messages.size()));
        List<MessageParam> messagesToUpdate = new ArrayList<>();
        messagesToUpdate.addAll(firstChunk);
        messagesToUpdate.addAll(secondChunk);

        // 我们需要从 messagesToUpdate 中的本地索引到 this.contextHistoryUpdates 中的全局更新数组的映射
        List<Integer> originalIndices = new ArrayList<>();
        for (int i = 0; i < firstChunkSize; i++) {
            originalIndices.add(i);
        }
        for (int i = startFromIndex; i < messages.size(); i++) {
            originalIndices.add(i);
        }

        for (int arrayIndex = 0; arrayIndex < messagesToUpdate.size(); arrayIndex++) {
            int messageIndex = originalIndices.get(arrayIndex);

            InnerTuple innerTuple = contextHistoryUpdates.get(messageIndex);
            if (innerTuple == null) {
                continue;
            }

            MessageParam messageParam = messagesToUpdate.get(arrayIndex);
            List<UserContentBlock> originalContent = messageParam.getContent();
            if (originalContent == null || originalContent.isEmpty()) {
                continue;
            }

            List<UserContentBlock> newContent = new ArrayList<>();
            for (UserContentBlock block : originalContent) {
                if (block instanceof TextContentBlock) {
                    newContent.add(new TextContentBlock(((TextContentBlock) block).getText()));
                } else {
                    // 对于其他类型的块，直接添加（如果需要深拷贝其他类型，可以在这里扩展）
                    newContent.add(block);
                }
            }

            Map<Integer, List<ContextUpdate>> innerMap = innerTuple.getInnerMap();
            for (Map.Entry<Integer, List<ContextUpdate>> entry : innerMap.entrySet()) {
                int blockIndex = entry.getKey();
                List<ContextUpdate> changes = entry.getValue();

                // 应用最新的更改 - [timestamp, updateType, update, metadata]
                if (!changes.isEmpty()) {
                    ContextUpdate latestChange = changes.get(changes.size() - 1);

                    if ("text".equals(latestChange.getUpdateType())) {
                        // 目前仅更改文本
                        if (blockIndex < newContent.size()) {
                            UserContentBlock block = newContent.get(blockIndex);
                            if (block instanceof TextContentBlock) {
                                if (!latestChange.getUpdate().isEmpty()) {
                                    String newText = latestChange.getUpdate().get(0);
                                    newContent.set(blockIndex, new TextContentBlock(newText));
                                }
                            }
                        }
                    }
                }
            }

            MessageParam updatedMessage;
            if (messageParam.getRole() == MessageRole.ASSISTANT) {
                updatedMessage = AssistantMessage.builder().content(newContent).build();
            } else {
                updatedMessage = UserMessage.builder().content(newContent).build();
            }
            messagesToUpdate.set(arrayIndex, updatedMessage);
        }

        return messagesToUpdate;
    }

    @Override
    public void truncateContextHistory(long timestamp) {
        truncateContextHistoryAtTimestamp(contextHistoryUpdates, timestamp);
        saveContextHistory();
    }

    /**
     * 更改上下文历史以删除给定时间戳之后的所有更改 如果不再有更改，则删除索引，包括外部和内部索引
     *
     * @param contextHistory 上下文历史映射
     * @param timestamp 时间戳
     */
    private void truncateContextHistoryAtTimestamp(
            Map<Integer, InnerTuple> contextHistory, long timestamp) {

        List<Integer> messagesToDelete = new ArrayList<>();

        for (Map.Entry<Integer, InnerTuple> entry : contextHistory.entrySet()) {
            int messageIndex = entry.getKey();
            InnerTuple innerTuple = entry.getValue();
            Map<Integer, List<ContextUpdate>> innerMap = innerTuple.getInnerMap();

            List<Integer> blockIndicesToDelete = new ArrayList<>();

            for (Map.Entry<Integer, List<ContextUpdate>> innerEntry : innerMap.entrySet()) {
                int blockIndex = innerEntry.getKey();
                List<ContextUpdate> updates = innerEntry.getValue();

                // 更新按时间戳排序，因此通过从右到左迭代找到截止点
                int cutoffIndex = updates.size() - 1;
                while (cutoffIndex >= 0 && updates.get(cutoffIndex).getTimestamp() > timestamp) {
                    cutoffIndex--;
                }

                if (cutoffIndex < updates.size() - 1) {
                    while (updates.size() > cutoffIndex + 1) {
                        updates.remove(updates.size() - 1);
                    }

                    if (updates.isEmpty()) {
                        blockIndicesToDelete.add(blockIndex);
                    }
                }
            }

            for (int blockIndex : blockIndicesToDelete) {
                innerMap.remove(blockIndex);
            }

            if (innerMap.isEmpty()) {
                messagesToDelete.add(messageIndex);
            }
        }

        for (int messageIndex : messagesToDelete) {
            contextHistory.remove(messageIndex);
        }
    }

    /**
     * 应用上下文优化步骤并返回是否进行了任何更改
     *
     * @param apiMessages API 消息列表
     * @param startFromIndex 开始索引
     * @param timestamp 时间戳
     * @return 上下文优化结果
     */
    private ContextOptimizationResult applyContextOptimizations(
            List<MessageParam> apiMessages, int startFromIndex, long timestamp) {

        boolean[] fileReadUpdatesBool = new boolean[1];
        Set<Integer> uniqueFileReadIndices = new HashSet<>();

        findAndPotentiallySaveFileReadContextHistoryUpdates(
                apiMessages, startFromIndex, timestamp, fileReadUpdatesBool, uniqueFileReadIndices);

        return new ContextOptimizationResult(fileReadUpdatesBool[0], uniqueFileReadIndices);
    }

    /**
     * 查找并可能保存文件读取的上下文历史更新
     *
     * @param apiMessages API 消息列表
     * @param startFromIndex 开始索引
     * @param timestamp 时间戳
     * @param fileReadUpdatesBool 输出参数：是否有更新
     * @param uniqueFileReadIndices 输出参数：唯一文件读取索引
     */
    private void findAndPotentiallySaveFileReadContextHistoryUpdates(
            List<MessageParam> apiMessages,
            int startFromIndex,
            long timestamp,
            boolean[] fileReadUpdatesBool,
            Set<Integer> uniqueFileReadIndices) {

        FileReadResult result = getPossibleDuplicateFileReads(apiMessages, startFromIndex);
        applyFileReadContextHistoryUpdates(
                result.fileReadIndices,
                result.messageFilePaths,
                apiMessages,
                timestamp,
                fileReadUpdatesBool,
                uniqueFileReadIndices);
    }

    /**
     * 计算上下文优化指标 计算范围内对话中的总字符节省百分比
     *
     * @param apiMessages API 消息列表
     * @param conversationHistoryDeletedRange 对话历史删除范围
     * @param uniqueFileReadIndices 唯一文件读取索引
     * @return 字符节省百分比
     */
    private double calculateContextOptimizationMetrics(
            List<MessageParam> apiMessages,
            int[] conversationHistoryDeletedRange,
            Set<Integer> uniqueFileReadIndices) {

        // 计算第一个用户-助手消息对
        CharacterCount firstChunkResult =
                countCharactersAndSavingsInRange(apiMessages, 0, 2, uniqueFileReadIndices);

        // 计算范围内的剩余消息
        int startIndex =
                conversationHistoryDeletedRange != null
                        ? conversationHistoryDeletedRange[1] + 1
                        : 2;
        CharacterCount secondChunkResult =
                countCharactersAndSavingsInRange(
                        apiMessages, startIndex, apiMessages.size(), uniqueFileReadIndices);

        int totalCharacters = firstChunkResult.totalCharacters + secondChunkResult.totalCharacters;
        int totalCharactersSaved =
                firstChunkResult.charactersSaved + secondChunkResult.charactersSaved;

        return totalCharacters == 0 ? 0 : (double) totalCharactersSaved / totalCharacters;
    }

    /**
     * 计算范围内的总字符和总节省
     *
     * @param apiMessages API 消息列表
     * @param startIndex 开始索引
     * @param endIndex 结束索引
     * @param uniqueFileReadIndices 唯一文件读取索引
     * @return 字符计数结果
     */
    private CharacterCount countCharactersAndSavingsInRange(
            List<MessageParam> apiMessages,
            int startIndex,
            int endIndex,
            Set<Integer> uniqueFileReadIndices) {

        int totalCharCount = 0;
        int totalCharactersSaved = 0;

        for (int i = startIndex; i < endIndex; i++) {
            MessageParam messageParam = apiMessages.get(i);
            List<UserContentBlock> content = messageParam.getContent();
            if (content == null || content.isEmpty()) {
                continue;
            }

            boolean hasExistingAlterations = contextHistoryUpdates.containsKey(i);
            boolean hasNewAlterations = uniqueFileReadIndices.contains(i);

            for (int blockIndex = 0; blockIndex < content.size(); blockIndex++) {
                UserContentBlock block = content.get(blockIndex);
                if (block == null) {
                    continue;
                }

                if (block.getType() == ContentBlockType.TEXT) {
                    TextContentBlock textBlock = (TextContentBlock) block;
                    String text = textBlock.getText();
                    if (text == null) {
                        continue;
                    }

                    if (hasExistingAlterations) {
                        InnerTuple innerTuple = contextHistoryUpdates.get(i);
                        if (innerTuple != null) {
                            Map<Integer, List<ContextUpdate>> innerMap = innerTuple.getInnerMap();
                            List<ContextUpdate> updates = innerMap.get(blockIndex);

                            if (updates != null && !updates.isEmpty()) {
                                ContextUpdate latestUpdate = updates.get(updates.size() - 1);

                                if (hasNewAlterations) {
                                    int originalTextLength;
                                    if (updates.size() > 1) {
                                        originalTextLength =
                                                updates.get(updates.size() - 2)
                                                        .getUpdate()
                                                        .get(0)
                                                        .length();
                                    } else {
                                        originalTextLength = text.length();
                                    }

                                    int newTextLength = latestUpdate.getUpdate().get(0).length();
                                    totalCharactersSaved += originalTextLength - newTextLength;
                                    totalCharCount += originalTextLength;
                                } else {
                                    totalCharCount += latestUpdate.getUpdate().get(0).length();
                                }
                            } else {
                                totalCharCount += text.length();
                            }
                        } else {
                            totalCharCount += text.length();
                        }
                    } else {
                        totalCharCount += text.length();
                    }
                } else if (block.getType() == ContentBlockType.IMAGE) {
                    ImageContentBlock imageBlock = (ImageContentBlock) block;
                    if ("base64".equals(imageBlock.getSourceType())
                            && imageBlock.getSource() != null) {
                        totalCharCount += imageBlock.getSource().length();
                    }
                }
            }
        }

        return new CharacterCount(totalCharCount, totalCharactersSaved);
    }

    /**
     * 如果有任何截断且尚未设置其他更改，则更改助手消息以指示发生了这种情况
     *
     * @param timestamp 时间戳
     * @return 如果应用了更改则返回 true
     */
    private boolean applyStandardContextTruncationNoticeChange(long timestamp) {
        if (!contextHistoryUpdates.containsKey(1)) {
            // 第一条助手消息始终在索引 1
            Map<Integer, List<ContextUpdate>> innerMap = new HashMap<>();
            List<String> content = new ArrayList<>(Arrays.asList(getContextTruncationNotice()));
            List<List<String>> metadata = new ArrayList<>();
            List<ContextUpdate> updates = new ArrayList<>();
            updates.add(new ContextUpdate(timestamp, "text", content, metadata));
            innerMap.put(0, updates);
            contextHistoryUpdates.put(1, new InnerTuple(EditType.UNDEFINED, innerMap));
            return true;
        }
        return false;
    }

    /**
     * 替换第一条用户消息（当上下文窗口被压缩时）
     *
     * @param timestamp 时间戳
     * @param apiConversationHistory API 对话历史
     * @return 如果应用了更改则返回 true
     */
    private boolean applyFirstUserMessageReplacement(
            long timestamp, List<MessageParam> apiConversationHistory) {

        if (!contextHistoryUpdates.containsKey(0)) {
            try {
                String firstUserMessage = "";

                if (!apiConversationHistory.isEmpty()) {
                    MessageParam messageParam = apiConversationHistory.get(0);
                    List<UserContentBlock> content = messageParam.getContent();
                    if (content != null && !content.isEmpty()) {
                        UserContentBlock userContentBlock = content.get(0);
                        if (userContentBlock != null) {
                            if (userContentBlock.getType() == ContentBlockType.TEXT) {
                                firstUserMessage = ((TextContentBlock) userContentBlock).getText();
                            }
                        }
                    }
                }

                if (firstUserMessage != null && !firstUserMessage.isEmpty()) {
                    String processedFirstUserMessage = getProcessFirstUserMessageForTruncation();

                    Map<Integer, List<ContextUpdate>> innerMap = new HashMap<>();
                    List<String> content =
                            new ArrayList<>(Arrays.asList(processedFirstUserMessage));
                    List<List<String>> metadata = new ArrayList<>();
                    List<ContextUpdate> updates = new ArrayList<>();
                    updates.add(new ContextUpdate(timestamp, "text", content, metadata));
                    innerMap.put(0, updates);
                    contextHistoryUpdates.put(0, new InnerTuple(EditType.UNDEFINED, innerMap));

                    return true;
                }
            } catch (Exception e) {
                log.error("applyFirstUserMessageReplacement: " + e.getMessage());
            }
        }
        return false;
    }

    @Override
    public void triggerApplyStandardContextTruncationNoticeChange(
            long timestamp, List<MessageParam> apiConversationHistory) {

        boolean assistantUpdated = applyStandardContextTruncationNoticeChange(timestamp);
        boolean userUpdated = applyFirstUserMessageReplacement(timestamp, apiConversationHistory);
        if (assistantUpdated || userUpdated) {
            saveContextHistory();
        }
    }

    private String getContextTruncationNotice() {
        return responseFormatter.contextTruncationNotice();
    }

    private String getProcessFirstUserMessageForTruncation() {
        return responseFormatter.processFirstUserMessageForTruncation();
    }

    /**
     * 获取可能的重复文件读取
     *
     * @param apiMessages API 消息列表
     * @param startFromIndex 开始索引
     * @return 文件读取结果
     */
    private FileReadResult getPossibleDuplicateFileReads(
            List<MessageParam> apiMessages, int startFromIndex) {

        // fileReadIndices: { fileName => [outerIndex, EditType, searchText, replaceText] }
        Map<String, List<FileReadInfo>> fileReadIndices = new HashMap<>();
        // messageFilePaths: { outerIndex => [fileRead1, fileRead2, ..] }
        Map<Integer, List<String>> messageFilePaths = new HashMap<>();

        for (int i = startFromIndex; i < apiMessages.size(); i++) {
            List<String> thisExistingFileReads = new ArrayList<>();

            if (contextHistoryUpdates.containsKey(i)) {
                InnerTuple innerTuple = contextHistoryUpdates.get(i);
                if (innerTuple != null) {
                    EditType editType = innerTuple.getEditType();

                    if (editType == EditType.FILE_MENTION) {
                        Map<Integer, List<ContextUpdate>> innerMap = innerTuple.getInnerMap();
                        int blockIndex = 1; // file mention blocks assumed to be at index 1
                        List<ContextUpdate> blockUpdates = innerMap.get(blockIndex);

                        if (blockUpdates != null && !blockUpdates.isEmpty()) {
                            ContextUpdate latestUpdate = blockUpdates.get(blockUpdates.size() - 1);
                            List<List<String>> metadata = latestUpdate.getMetadata();
                            if (metadata != null && metadata.size() >= 2) {
                                List<String> replacedFiles = metadata.get(0);
                                List<String> allFiles = metadata.get(1);

                                if (replacedFiles.size() == allFiles.size()) {
                                    continue;
                                } else {
                                    thisExistingFileReads = replacedFiles;
                                }
                            }
                        }
                    } else {
                        continue;
                    }
                }
            }

            MessageParam messageParam = apiMessages.get(i);

            if (messageParam.getRole() != MessageRole.USER) {
                continue;
            }

            List<UserContentBlock> content = messageParam.getContent();
            if (content == null || content.isEmpty()) {
                continue;
            }

            UserContentBlock firstBlock = content.get(0);
            if (firstBlock == null || firstBlock.getType() != ContentBlockType.TEXT) {
                continue;
            }

            String firstBlockText = ((TextContentBlock) firstBlock).getText();
            ToolCallMatch matchTup = parsePotentialToolCall(firstBlockText);

            boolean foundNormalFileRead = false;
            if (matchTup != null) {
                if ("read_file".equals(matchTup.toolName)) {
                    handleReadFileToolCall(i, matchTup.filePath, fileReadIndices);
                    foundNormalFileRead = true;
                } else if ("replace_in_file".equals(matchTup.toolName)
                        || "write_to_file".equals(matchTup.toolName)) {
                    if (content.size() > 1) {
                        UserContentBlock secondBlock = content.get(1);
                        if (secondBlock != null && secondBlock.getType() == ContentBlockType.TEXT) {
                            handlePotentialFileChangeToolCalls(
                                    i,
                                    matchTup.filePath,
                                    ((TextContentBlock) secondBlock).getText(),
                                    fileReadIndices);
                            foundNormalFileRead = true;
                        }
                    }
                }
            }

            // 文件提及可能发生在大多数其他用户消息块中
            if (!foundNormalFileRead && content.size() > 1) {
                UserContentBlock secondBlock = content.get(1);
                if (secondBlock != null && secondBlock.getType() == ContentBlockType.TEXT) {
                    FileMentionResult mentionResult =
                            handlePotentialFileMentionCalls(
                                    i,
                                    ((TextContentBlock) secondBlock).getText(),
                                    fileReadIndices,
                                    thisExistingFileReads);
                    if (mentionResult.hasFileRead) {
                        messageFilePaths.put(i, mentionResult.filePaths);
                    }
                }
            }
        }

        return new FileReadResult(fileReadIndices, messageFilePaths);
    }

    /**
     * 解析潜在的工具调用格式
     *
     * @param text 文本
     * @return 工具调用匹配结果，如果没有找到则返回 null
     */
    private ToolCallMatch parsePotentialToolCall(String text) {
        if (text == null) {
            return null;
        }
        Pattern pattern = Pattern.compile("^\\[([^\\s]+) for '([^']+)'\\] Result:$");
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            return new ToolCallMatch(matcher.group(1), matcher.group(2));
        }
        return null;
    }

    /**
     * 处理 read_file 工具调用
     *
     * @param i 消息索引
     * @param filePath 文件路径
     * @param fileReadIndices 文件读取索引映射
     */
    private void handleReadFileToolCall(
            int i, String filePath, Map<String, List<FileReadInfo>> fileReadIndices) {
        List<FileReadInfo> indices = fileReadIndices.getOrDefault(filePath, new ArrayList<>());
        indices.add(
                new FileReadInfo(
                        i,
                        EditType.READ_FILE_TOOL,
                        "",
                        responseFormatter.duplicateFileReadNotice()));
        fileReadIndices.put(filePath, indices);
    }

    /**
     * 处理 write_to_file 和 replace_in_file 工具输出
     *
     * @param i 消息索引
     * @param filePath 文件路径
     * @param secondBlockText 第二个块文本
     * @param fileReadIndices 文件读取索引映射
     */
    private void handlePotentialFileChangeToolCalls(
            int i,
            String filePath,
            String secondBlockText,
            Map<String, List<FileReadInfo>> fileReadIndices) {

        Pattern pattern =
                Pattern.compile(
                        "(<final_file_content path=\"[^\"]*\">)[\\s\\S]*?(</final_file_content>)");
        if (pattern.matcher(secondBlockText).find()) {
            String replacementText =
                    secondBlockText.replaceAll(
                            pattern.pattern(),
                            "$1 " + responseFormatter.duplicateFileReadNotice() + " $2");
            List<FileReadInfo> indices = fileReadIndices.getOrDefault(filePath, new ArrayList<>());
            indices.add(new FileReadInfo(i, EditType.ALTER_FILE_TOOL, "", replacementText));
            fileReadIndices.put(filePath, indices);
        }
    }

    /**
     * 处理文本块中的潜在文件内容提及
     *
     * @param i 消息索引
     * @param secondBlockText 第二个块文本
     * @param fileReadIndices 文件读取索引映射
     * @param thisExistingFileReads 已存在的文件读取列表
     * @return 文件提及结果
     */
    private FileMentionResult handlePotentialFileMentionCalls(
            int i,
            String secondBlockText,
            Map<String, List<FileReadInfo>> fileReadIndices,
            List<String> thisExistingFileReads) {

        Pattern pattern =
                Pattern.compile("<file_content path=\"([^\"]*)\">([\\s\\S]*?)</file_content>");
        Matcher matcher = pattern.matcher(secondBlockText);

        boolean foundMatch = false;
        List<String> filePaths = new ArrayList<>();

        while (matcher.find()) {
            foundMatch = true;
            String filePath = matcher.group(1);
            filePaths.add(filePath);

            if (!thisExistingFileReads.contains(filePath)) {
                String entireMatch = matcher.group(0);
                String replacementText =
                        "<file_content path=\""
                                + filePath
                                + "\">"
                                + responseFormatter.duplicateFileReadNotice()
                                + "</file_content>";

                List<FileReadInfo> indices =
                        fileReadIndices.getOrDefault(filePath, new ArrayList<>());
                indices.add(
                        new FileReadInfo(i, EditType.FILE_MENTION, entireMatch, replacementText));
                fileReadIndices.put(filePath, indices);
            }
        }

        return new FileMentionResult(foundMatch, filePaths);
    }

    /**
     * 应用所有文件读取操作的更改并跟踪哪些消息被更新
     *
     * @param fileReadIndices 文件读取索引映射
     * @param messageFilePaths 消息文件路径映射
     * @param apiMessages API 消息列表
     * @param timestamp 时间戳
     * @param didUpdate 输出参数：是否更新
     * @param updatedMessageIndices 输出参数：更新的消息索引
     */
    private void applyFileReadContextHistoryUpdates(
            Map<String, List<FileReadInfo>> fileReadIndices,
            Map<Integer, List<String>> messageFilePaths,
            List<MessageParam> apiMessages,
            long timestamp,
            boolean[] didUpdate,
            Set<Integer> updatedMessageIndices) {

        Map<Integer, FileMentionUpdate> fileMentionUpdates = new HashMap<>();

        for (Map.Entry<String, List<FileReadInfo>> entry : fileReadIndices.entrySet()) {
            String filePath = entry.getKey();
            List<FileReadInfo> indices = entry.getValue();

            if (indices.size() > 1) {
                // 处理除最后一个索引外的所有索引，因为我们要保留该文件读取的最新实例
                for (int i = 0; i < indices.size() - 1; i++) {
                    FileReadInfo info = indices.get(i);
                    int messageIndex = info.outerIndex;
                    EditType messageType = info.editType;
                    String searchText = info.searchText;
                    String messageString = info.replaceText;

                    didUpdate[0] = true;
                    updatedMessageIndices.add(messageIndex);

                    if (messageType == EditType.FILE_MENTION) {
                        if (!fileMentionUpdates.containsKey(messageIndex)) {
                            String baseText = "";
                            List<String> prevFilesReplaced = new ArrayList<>();

                            InnerTuple innerTuple = contextHistoryUpdates.get(messageIndex);
                            if (innerTuple != null) {
                                Map<Integer, List<ContextUpdate>> innerMap =
                                        innerTuple.getInnerMap();
                                List<ContextUpdate> blockUpdates = innerMap.get(1);
                                if (blockUpdates != null && !blockUpdates.isEmpty()) {
                                    ContextUpdate latestUpdate =
                                            blockUpdates.get(blockUpdates.size() - 1);
                                    baseText = latestUpdate.getUpdate().get(0);
                                    if (latestUpdate.getMetadata() != null
                                            && !latestUpdate.getMetadata().isEmpty()) {
                                        prevFilesReplaced = latestUpdate.getMetadata().get(0);
                                    }
                                }
                            }

                            if (baseText.isEmpty()) {
                                MessageParam messageParam = apiMessages.get(messageIndex);
                                List<UserContentBlock> content = messageParam.getContent();
                                if (content != null && content.size() > 1) {
                                    UserContentBlock contentBlock = content.get(1);
                                    if (contentBlock != null
                                            && contentBlock.getType() == ContentBlockType.TEXT) {
                                        baseText = ((TextContentBlock) contentBlock).getText();
                                    }
                                }
                            }

                            fileMentionUpdates.put(
                                    messageIndex,
                                    new FileMentionUpdate(baseText, prevFilesReplaced));
                        }

                        if (searchText != null && !searchText.isEmpty()) {
                            FileMentionUpdate currentTuple = fileMentionUpdates.get(messageIndex);
                            if (currentTuple != null && !currentTuple.getBaseText().isEmpty()) {
                                String updatedText =
                                        currentTuple
                                                .getBaseText()
                                                .replace(searchText, messageString);
                                List<String> updatedFileReads =
                                        new ArrayList<>(currentTuple.getFilePathsUpdated());
                                updatedFileReads.add(filePath);
                                currentTuple.setUpdatedText(updatedText);
                                currentTuple.setFilePathsUpdated(updatedFileReads);
                            }
                        }
                    } else {
                        InnerTuple innerTuple = contextHistoryUpdates.get(messageIndex);
                        Map<Integer, List<ContextUpdate>> innerMap;
                        if (innerTuple == null) {
                            innerMap = new HashMap<>();
                            contextHistoryUpdates.put(
                                    messageIndex, new InnerTuple(messageType, innerMap));
                        } else {
                            innerMap = innerTuple.getInnerMap();
                        }

                        int blockIndex = 1;
                        List<ContextUpdate> updates =
                                innerMap.getOrDefault(blockIndex, new ArrayList<>());
                        List<String> content =
                                new ArrayList<>(Collections.singletonList(messageString));
                        List<List<String>> metadata = new ArrayList<>();
                        updates.add(new ContextUpdate(timestamp, "text", content, metadata));
                        innerMap.put(blockIndex, updates);
                    }
                }
            }
        }

        for (Map.Entry<Integer, FileMentionUpdate> entry : fileMentionUpdates.entrySet()) {
            int messageIndex = entry.getKey();
            FileMentionUpdate update = entry.getValue();

            InnerTuple innerTuple = contextHistoryUpdates.get(messageIndex);
            Map<Integer, List<ContextUpdate>> innerMap;
            if (innerTuple == null) {
                innerMap = new HashMap<>();
                contextHistoryUpdates.put(
                        messageIndex, new InnerTuple(EditType.FILE_MENTION, innerMap));
            } else {
                innerMap = innerTuple.getInnerMap();
            }

            int blockIndex = 1;
            List<ContextUpdate> updates = innerMap.getOrDefault(blockIndex, new ArrayList<>());

            List<String> content =
                    new ArrayList<>(Collections.singletonList(update.getUpdatedText()));
            List<List<String>> metadata = new ArrayList<>();
            List<String> allFileReads = messageFilePaths.get(messageIndex);
            if (allFileReads != null) {
                metadata.add(update.getFilePathsUpdated());
                metadata.add(allFileReads);
            }
            updates.add(new ContextUpdate(timestamp, "text", content, metadata));
            innerMap.put(blockIndex, updates);
        }
    }

    private static class ContextOptimizationResult {
        boolean anyContextUpdates;
        Set<Integer> uniqueFileReadIndices;

        ContextOptimizationResult(boolean anyContextUpdates, Set<Integer> uniqueFileReadIndices) {
            this.anyContextUpdates = anyContextUpdates;
            this.uniqueFileReadIndices = uniqueFileReadIndices;
        }
    }

    private static class FileReadResult {
        Map<String, List<FileReadInfo>> fileReadIndices;
        Map<Integer, List<String>> messageFilePaths;

        FileReadResult(
                Map<String, List<FileReadInfo>> fileReadIndices,
                Map<Integer, List<String>> messageFilePaths) {
            this.fileReadIndices = fileReadIndices;
            this.messageFilePaths = messageFilePaths;
        }
    }

    private static class FileReadInfo {
        int outerIndex;
        EditType editType;
        String searchText;
        String replaceText;

        FileReadInfo(int outerIndex, EditType editType, String searchText, String replaceText) {
            this.outerIndex = outerIndex;
            this.editType = editType;
            this.searchText = searchText;
            this.replaceText = replaceText;
        }
    }

    private static class CharacterCount {
        int totalCharacters;
        int charactersSaved;

        CharacterCount(int totalCharacters, int charactersSaved) {
            this.totalCharacters = totalCharacters;
            this.charactersSaved = charactersSaved;
        }
    }

    private static class ToolCallMatch {
        String toolName;
        String filePath;

        ToolCallMatch(String toolName, String filePath) {
            this.toolName = toolName;
            this.filePath = filePath;
        }
    }

    private static class FileMentionResult {
        boolean hasFileRead;
        List<String> filePaths;

        FileMentionResult(boolean hasFileRead, List<String> filePaths) {
            this.hasFileRead = hasFileRead;
            this.filePaths = filePaths;
        }
    }
}
