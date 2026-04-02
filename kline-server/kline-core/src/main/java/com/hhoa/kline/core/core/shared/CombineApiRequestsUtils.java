package com.hhoa.kline.core.core.shared;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hhoa.kline.core.core.task.ClineMessage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 在 ClineMessage 数组中组合 API 请求开始和完成消息。
 *
 * <p>此函数查找 'api_req_started' 和 'api_req_finished' 消息对。 当找到一对时，将它们组合成单个 'api_req_combined' 消息。 两个消息的
 * text 字段中的 JSON 数据会被合并。
 *
 * @param messages 要处理的 ClineMessage 对象数组
 * @return 组合了 API 请求的 ClineMessage 对象新数组
 */
public class CombineApiRequestsUtils {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static List<ClineMessage> combineApiRequests(List<ClineMessage> messages) {
        if (messages == null || messages.isEmpty()) {
            return new ArrayList<>();
        }

        List<ClineMessage> combinedApiRequests = new ArrayList<>();

        for (int i = 0; i < messages.size(); i++) {
            ClineMessage message = messages.get(i);
            if (message != null
                    && "say".equals(message.getType())
                    && "api_req_started".equals(message.getSay())) {
                try {
                    Map<String, Object> startedRequest =
                            objectMapper.readValue(
                                    message.getText() != null ? message.getText() : "{}",
                                    objectMapper
                                            .getTypeFactory()
                                            .constructMapType(
                                                    Map.class, String.class, Object.class));
                    int j = i + 1;
                    boolean foundMatch = false;

                    while (j < messages.size()) {
                        ClineMessage finishMessage = messages.get(j);
                        if (finishMessage != null
                                && ClineMessageType.SAY.equals(finishMessage.getType())
                                && ClineSay.API_REQ_FINISHED.equals(finishMessage.getSay())) {
                            try {
                                Map<String, Object> finishedRequest =
                                        objectMapper.readValue(
                                                finishMessage.getText() != null
                                                        ? finishMessage.getText()
                                                        : "{}",
                                                objectMapper
                                                        .getTypeFactory()
                                                        .constructMapType(
                                                                Map.class,
                                                                String.class,
                                                                Object.class));

                                Map<String, Object> combinedRequest = new HashMap<>(startedRequest);
                                combinedRequest.putAll(finishedRequest);

                                ClineMessage combinedMessage = new ClineMessage();
                                combinedMessage.setTs(message.getTs());
                                combinedMessage.setType(message.getType());
                                combinedMessage.setAsk(message.getAsk());
                                combinedMessage.setSay(message.getSay());
                                combinedMessage.setText(
                                        objectMapper.writeValueAsString(combinedRequest));
                                combinedMessage.setReasoning(message.getReasoning());
                                combinedMessage.setImages(message.getImages());
                                combinedMessage.setFiles(message.getFiles());
                                combinedMessage.setPartial(message.getPartial());
                                combinedMessage.setCommandCompleted(message.getCommandCompleted());
                                combinedMessage.setLastCheckpointHash(
                                        message.getLastCheckpointHash());
                                combinedMessage.setIsCheckpointCheckedOut(
                                        message.getIsCheckpointCheckedOut());
                                combinedMessage.setIsOperationOutsideWorkspace(
                                        message.getIsOperationOutsideWorkspace());
                                combinedMessage.setConversationHistoryIndex(
                                        message.getConversationHistoryIndex());
                                combinedMessage.setConversationHistoryDeletedRange(
                                        message.getConversationHistoryDeletedRange());
                                combinedApiRequests.add(combinedMessage);

                                // 在 Java 中，我们需要将 i 设置为 j-1，这样在循环递增后 i 会变成 j，从而跳过已处理的
                                // api_req_finished
                                i = j - 1;
                                foundMatch = true;
                                break;
                            } catch (Exception e) {
                                // 忽略解析错误，继续处理
                            }
                        }
                        j++;
                    }

                    if (!foundMatch) {
                        // 如果没有找到匹配的 api_req_finished，保留原始的 api_req_started
                        combinedApiRequests.add(message);
                    }
                } catch (Exception e) {
                    // 忽略解析错误，保留原始消息
                    combinedApiRequests.add(message);
                }
            }
        }

        return messages.stream()
                .filter(
                        msg ->
                                !(msg != null
                                        && "say".equals(msg.getType())
                                        && ClineSay.API_REQ_FINISHED.equals(msg.getSay())))
                .map(
                        msg -> {
                            if (msg != null
                                    && ClineMessageType.SAY.equals(msg.getType())
                                    && ClineSay.API_REQ_STARTED.equals(msg.getSay())) {
                                ClineMessage combinedRequest =
                                        combinedApiRequests.stream()
                                                .filter(
                                                        req ->
                                                                req != null
                                                                        && req.getTs() != null
                                                                        && req.getTs()
                                                                                .equals(
                                                                                        msg
                                                                                                .getTs()))
                                                .findFirst()
                                                .orElse(null);
                                return combinedRequest != null ? combinedRequest : msg;
                            }
                            return msg;
                        })
                .collect(Collectors.toList());
    }
}
