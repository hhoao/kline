package com.hhoa.kline.core.core.task;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hhoa.ai.kline.commons.utils.JsonUtils;
import com.hhoa.kline.core.common.Tuple2;
import com.hhoa.kline.core.core.shared.ClineApiReqCancelReason;
import com.hhoa.kline.core.core.shared.ClineApiReqInfo;
import com.hhoa.kline.core.core.shared.ClineAsk;
import com.hhoa.kline.core.core.shared.ClineMessageType;
import com.hhoa.kline.core.core.shared.ClineSay;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class MessageUtils {

    public static final String COMMAND_OUTPUT_STRING = "Output:";

    public static final String COMMAND_REQ_APP_STRING = "REQ_APP";

    private static final ObjectMapper objectMapper = new ObjectMapper();

    private MessageUtils() {}

    /**
     * 合并 API 请求消息：将 api_req_started 和 api_req_finished 合并。
     *
     * <p>查找 api_req_started 消息及其对应的 api_req_finished 消息，将它们合并成一个消息。 合并时会将两个消息的 JSON 数据合并。
     *
     * @param messages ClineMessage 列表
     * @return 合并后的消息列表
     */
    public static List<ClineMessage> combineApiRequests(List<ClineMessage> messages) {
        List<ClineMessage> combinedApiRequests = new ArrayList<>();

        for (int i = 0; i < messages.size(); i++) {
            ClineMessage msg = messages.get(i);
            if (ClineMessageType.SAY.equals(msg.getType())
                    && ClineSay.API_REQ_STARTED.equals(msg.getSay())) {
                Map<String, Object> startedRequest =
                        JsonUtils.parseObject(
                                msg.getText(), new TypeReference<Map<String, Object>>() {});
                int j = i + 1;
                boolean foundFinished = false;

                while (j < messages.size()) {
                    ClineMessage finishedMsg = messages.get(j);
                    if (ClineMessageType.SAY.equals(finishedMsg.getType())
                            && ClineSay.API_REQ_FINISHED.equals(finishedMsg.getSay())) {
                        Map<String, Object> finishedRequest =
                                JsonUtils.parseObject(
                                        finishedMsg.getText(),
                                        new TypeReference<Map<String, Object>>() {});
                        Map<String, Object> combinedRequest = new HashMap<>(startedRequest);
                        combinedRequest.putAll(finishedRequest);

                        ClineMessage combined = new ClineMessage();
                        combined.setType(msg.getType());
                        combined.setSay(msg.getSay());
                        combined.setTs(msg.getTs());
                        combined.setReasoning(msg.getReasoning());
                        combined.setImages(msg.getImages());
                        combined.setFiles(msg.getFiles());
                        combined.setPartial(msg.getPartial());
                        combined.setCommandCompleted(msg.getCommandCompleted());
                        combined.setLastCheckpointHash(msg.getLastCheckpointHash());
                        combined.setIsCheckpointCheckedOut(msg.getIsCheckpointCheckedOut());
                        combined.setIsOperationOutsideWorkspace(
                                msg.getIsOperationOutsideWorkspace());
                        combined.setConversationHistoryIndex(msg.getConversationHistoryIndex());
                        combined.setConversationHistoryDeletedRange(
                                msg.getConversationHistoryDeletedRange());

                        try {
                            combined.setText(objectMapper.writeValueAsString(combinedRequest));
                        } catch (Exception e) {
                            log.warn("Failed to serialize combined request: {}", e.getMessage());
                            combined.setText(msg.getText());
                        }
                        combinedApiRequests.add(combined);
                        foundFinished = true;
                        i = j;
                        break;
                    }
                    j++;
                }

                if (!foundFinished) {
                    combinedApiRequests.add(msg);
                }
            }
        }

        List<ClineMessage> result = new ArrayList<>();
        for (ClineMessage msg : messages) {
            if (ClineMessageType.SAY.equals(msg.getType())
                    && ClineSay.API_REQ_FINISHED.equals(msg.getSay())) {
                continue;
            } else if (ClineMessageType.SAY.equals(msg.getType())
                    && ClineSay.API_REQ_STARTED.equals(msg.getSay())) {
                ClineMessage combined =
                        combinedApiRequests.stream()
                                .filter(m -> m.getTs() != null && m.getTs().equals(msg.getTs()))
                                .findFirst()
                                .orElse(msg);
                result.add(combined);
            } else {
                result.add(msg);
            }
        }

        return result;
    }

    /**
     * 合并命令序列：将 command 和 command_output 合并。
     *
     * <p>查找 command 消息及其后续的 command_output 消息，将它们合并成一个消息。 第一个输出前会添加 "Output:" 标记。
     *
     * @param messages ClineMessage 列表
     * @return 合并后的消息列表
     */
    public static List<ClineMessage> combineCommandSequences(List<ClineMessage> messages) {
        List<ClineMessage> combinedCommands = new ArrayList<>();

        for (int i = 0; i < messages.size(); i++) {
            ClineMessage msg = messages.get(i);
            if (ClineAsk.COMMAND.equals(msg.getAsk()) || ClineSay.COMMAND.equals(msg.getSay())) {
                StringBuilder combinedText = new StringBuilder();
                String originalText = msg.getText() != null ? msg.getText() : "";
                combinedText.append(originalText);
                boolean didAddOutput = false;
                int j = i + 1;

                while (j < messages.size()) {
                    ClineMessage nextMsg = messages.get(j);
                    if (ClineAsk.COMMAND.equals(nextMsg.getAsk())
                            || ClineSay.COMMAND.equals(nextMsg.getSay())) {
                        break;
                    }
                    if (ClineAsk.COMMAND_OUTPUT.equals(nextMsg.getAsk())
                            || ClineSay.COMMAND_OUTPUT.equals(nextMsg.getSay())) {
                        if (!didAddOutput) {
                            combinedText.append("\n").append(COMMAND_OUTPUT_STRING);
                            didAddOutput = true;
                        }
                        String output = nextMsg.getText() != null ? nextMsg.getText() : "";
                        if (!output.isEmpty()) {
                            combinedText.append("\n").append(output);
                        }
                    }
                    j++;
                }

                ClineMessage combined = new ClineMessage();
                combined.setType(msg.getType());
                combined.setAsk(msg.getAsk());
                combined.setSay(msg.getSay());
                combined.setTs(msg.getTs());
                combined.setText(combinedText.toString());
                combined.setReasoning(msg.getReasoning());
                combined.setImages(msg.getImages());
                combined.setFiles(msg.getFiles());
                combined.setPartial(msg.getPartial());
                combined.setCommandCompleted(msg.getCommandCompleted());
                combined.setLastCheckpointHash(msg.getLastCheckpointHash());
                combined.setIsCheckpointCheckedOut(msg.getIsCheckpointCheckedOut());
                combined.setIsOperationOutsideWorkspace(msg.getIsOperationOutsideWorkspace());
                combined.setConversationHistoryIndex(msg.getConversationHistoryIndex());
                combined.setConversationHistoryDeletedRange(
                        msg.getConversationHistoryDeletedRange());

                combinedCommands.add(combined);
                i = j - 1;
            }
        }

        List<ClineMessage> result = new ArrayList<>();
        for (ClineMessage msg : messages) {
            if (ClineAsk.COMMAND_OUTPUT.equals(msg.getAsk())
                    || ClineSay.COMMAND_OUTPUT.equals(msg.getSay())) {
                continue;
            }
            if (ClineAsk.COMMAND.equals(msg.getAsk()) || ClineSay.COMMAND.equals(msg.getSay())) {
                ClineMessage combined =
                        combinedCommands.stream()
                                .filter(
                                        cmd ->
                                                cmd.getTs() != null
                                                        && cmd.getTs().equals(msg.getTs()))
                                .findFirst()
                                .orElse(msg);
                result.add(combined);
            } else {
                result.add(msg);
            }
        }

        return result;
    }

    /**
     * 查找最后一个匹配的索引：从后往前查找满足条件的消息索引。
     *
     * @param messages 消息列表
     * @param predicate 匹配条件
     * @return 最后一个匹配的索引，如果没有找到返回 -1
     */
    public static int findLastIndex(
            List<ClineMessage> messages, Predicate<ClineMessage> predicate) {
        for (int i = messages.size() - 1; i >= 0; i--) {
            if (predicate.test(messages.get(i))) {
                return i;
            }
        }
        return -1;
    }

    /**
     * 查找最后一个匹配的消息：从后往前查找满足条件的消息。
     *
     * @param messages 消息列表
     * @param predicate 匹配条件
     * @return 最后一个匹配的消息，如果没有找到返回 null
     */
    public static ClineMessage findLast(
            List<ClineMessage> messages, Predicate<ClineMessage> predicate) {
        int index = findLastIndex(messages, predicate);
        return index == -1 ? null : messages.get(index);
    }

    public static Tuple2<Integer, ClineApiReqInfo> getApiReqMessage(List<ClineMessage> messages) {
        for (int i = messages.size() - 1; i >= 0; i--) {
            ClineMessage msg = messages.get(i);
            if (ClineMessageType.SAY.equals(msg.getType())
                    && ClineSay.API_REQ_STARTED.equals(msg.getSay())) {
                if (msg.getText() != null && !msg.getText().isEmpty()) {
                    return Tuple2.of(i, JsonUtils.readValue(msg.getText(), ClineApiReqInfo.class));
                }
            }
        }
        return new Tuple2<>(-1, new ClineApiReqInfo());
    }

    public static Tuple2<Integer, ClineApiReqInfo> getApiReqInfo(List<ClineMessage> clineMessages) {
        return getApiReqMessage(clineMessages);
    }

    public static Tuple2<Integer, ClineMessage> updateApiReqMessage(
            List<ClineMessage> messages,
            Integer inputTokens,
            Integer outputTokens,
            Integer cacheWriteTokens,
            Integer cacheReadTokens,
            Double totalCost,
            ClineApiReqCancelReason cancelReason,
            String streamingFailedMessage) {
        Tuple2<Integer, ClineApiReqInfo> tuple = getApiReqMessage(messages);
        if (tuple != null) {
            ClineApiReqInfo apiRequestInfo = tuple.f1;
            apiRequestInfo.setRetryStatus(null);
            if (inputTokens != null) {
                apiRequestInfo.setTokensIn(inputTokens);
            }
            if (outputTokens != null) {
                apiRequestInfo.setTokensOut(outputTokens);
            }
            if (cacheWriteTokens != null) {
                apiRequestInfo.setCacheWrites(cacheWriteTokens);
            }
            if (cacheReadTokens != null) {
                apiRequestInfo.setCacheReads(cacheReadTokens);
            }
            if (totalCost != null) {
                apiRequestInfo.setCost(totalCost);
            }
            if (cancelReason != null) {
                apiRequestInfo.setCancelReason(cancelReason);
            }
            if (streamingFailedMessage != null) {
                apiRequestInfo.setStreamingFailedMessage(streamingFailedMessage);
            }
            messages.get(tuple.f0).setText(JsonUtils.toJsonString(apiRequestInfo));
            return Tuple2.of(tuple.f0, messages.get(tuple.f0));
        }
        return null;
    }
}
