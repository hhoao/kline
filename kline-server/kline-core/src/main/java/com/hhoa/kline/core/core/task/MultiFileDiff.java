package com.hhoa.kline.core.core.task;

import com.hhoa.kline.core.core.shared.ClineSay;
import com.hhoa.kline.core.core.shared.proto.host.ShowMessageRequest;
import com.hhoa.kline.core.core.shared.proto.host.ShowMessageType;
import com.hhoa.kline.core.subscription.DefaultSubscriptionManager;
import com.hhoa.kline.core.subscription.SubscriptionManager;
import com.hhoa.kline.core.subscription.message.WindowShowMessageRequestMessage;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class MultiFileDiff {

    private static final SubscriptionManager subscriptionManager =
            DefaultSubscriptionManager.getInstance();

    private MultiFileDiff() {}

    /**
     * 展示多文件差异。
     *
     * @param messageStateHandler 消息状态
     * @param checkpointTracker 检查点追踪器（提供 diff 集合）
     * @param messageTs 目标消息时间戳
     * @param seeNewChangesSinceLastTaskCompletion 是否仅查看上次完成后的新变更
     */
    public static void showChangedFilesDiff(
            MessageStateHandler messageStateHandler,
            CheckpointTracker checkpointTracker,
            long messageTs,
            boolean seeNewChangesSinceLastTaskCompletion) {
        List<ClineMessage> clineMessages = messageStateHandler.getClineMessages();
        int messageIndex = -1;
        for (int i = 0; i < clineMessages.size(); i++) {
            ClineMessage m = clineMessages.get(i);
            if (m.getTs() != null && m.getTs() == messageTs) {
                messageIndex = i;
                break;
            }
        }
        if (messageIndex < 0) {
            sendErrorMessage("Message not found");
            return;
        }
        ClineMessage message = clineMessages.get(messageIndex);
        String lastCheckpointHash = message.getLastCheckpointHash();
        if (lastCheckpointHash == null || lastCheckpointHash.isEmpty()) {
            sendErrorMessage("No checkpoint hash found");
            return;
        }

        List<ChangedFile> changedFiles =
                getChangedFiles(
                        messageStateHandler,
                        checkpointTracker,
                        seeNewChangesSinceLastTaskCompletion,
                        messageIndex,
                        lastCheckpointHash);
        if (changedFiles.isEmpty()) return;

        String title =
                seeNewChangesSinceLastTaskCompletion ? "New changes" : "Changes since snapshot";
        List<DiffEntry> diffs =
                changedFiles.stream()
                        .map(
                                f -> {
                                    DiffEntry e = new DiffEntry();
                                    e.filePath = f.absolutePath;
                                    e.leftContent = f.before;
                                    e.rightContent = f.after;
                                    return e;
                                })
                        .collect(Collectors.toList());
        sendOpenMultiFileDiff(title, diffs);
    }

    private static List<ChangedFile> getChangedFiles(
            MessageStateHandler messageStateHandler,
            CheckpointTracker checkpointTracker,
            boolean changesSinceLastTaskCompletion,
            int messageIndex,
            String lastCheckpointHash) {
        try {
            List<ChangedFile> changedFiles;
            if (changesSinceLastTaskCompletion) {
                changedFiles =
                        getChangesSinceLastTaskCompletion(
                                messageStateHandler,
                                checkpointTracker,
                                messageIndex,
                                lastCheckpointHash);
            } else {
                changedFiles = checkpointTracker.getDiffSet(lastCheckpointHash);
            }
            if (changedFiles == null || changedFiles.isEmpty()) {
                sendInfoMessage("No changes found");
            }
            return changedFiles == null ? List.of() : changedFiles;
        } catch (Exception e) {
            sendErrorMessage("Failed to retrieve diff set: " + e.getMessage());
            return List.of();
        }
    }

    private static List<ChangedFile> getChangesSinceLastTaskCompletion(
            MessageStateHandler messageStateHandler,
            CheckpointTracker checkpointTracker,
            int messageIndex,
            String lastCheckpointHash) {
        List<ClineMessage> msgs = messageStateHandler.getClineMessages();

        Optional<String> lastTaskCompletedHash =
                findLastSay(msgs.subList(0, Math.max(0, messageIndex)), ClineSay.COMPLETION_RESULT)
                        .map(ClineMessage::getLastCheckpointHash)
                        .filter(Objects::nonNull);

        Optional<String> firstCheckpointHash =
                msgs.stream()
                        .filter(m -> ClineSay.CHECKPOINT_CREATED.equals(m.getSay()))
                        .map(ClineMessage::getLastCheckpointHash)
                        .filter(Objects::nonNull)
                        .findFirst();

        String previousCheckpointHash =
                lastTaskCompletedHash.orElse(firstCheckpointHash.orElse(null));
        if (previousCheckpointHash == null) {
            sendErrorMessage("Unexpected error: No checkpoint hash found");
            return List.of();
        }
        return checkpointTracker.getDiffSet(previousCheckpointHash, lastCheckpointHash);
    }

    private static Optional<ClineMessage> findLastSay(List<ClineMessage> list, ClineSay sayType) {
        for (int i = list.size() - 1; i >= 0; i--) {
            ClineMessage m = list.get(i);
            if (sayType.equals(m.getSay())) return Optional.of(m);
        }
        return Optional.empty();
    }

    @Data
    public static final class ChangedFile {
        private String relativePath;
        private String absolutePath;
        private String before;
        private String after;
    }

    public interface CheckpointTracker {
        List<ChangedFile> getDiffSet(String fromHash);

        List<ChangedFile> getDiffSet(String fromHash, String toHash);
    }

    private static void sendInfoMessage(String message) {
        log.info("[INFO] " + message);
        ShowMessageRequest request =
                ShowMessageRequest.builder()
                        .type(ShowMessageType.INFORMATION)
                        .message(message)
                        .build();
        subscriptionManager.send(new WindowShowMessageRequestMessage(request));
    }

    private static void sendErrorMessage(String message) {
        log.error("[ERROR] " + message);
        ShowMessageRequest request =
                ShowMessageRequest.builder().type(ShowMessageType.ERROR).message(message).build();
        subscriptionManager.send(new WindowShowMessageRequestMessage(request));
    }

    private static void sendOpenMultiFileDiff(String title, List<DiffEntry> diffs) {
        log.info(
                "[MULTIFILE DIFF] "
                        + title
                        + " ("
                        + (diffs == null ? 0 : diffs.size())
                        + " files)");
        if (diffs != null) {
            diffs.stream()
                    .sorted(Comparator.comparing(d -> d.filePath == null ? "" : d.filePath))
                    .forEach(
                            d -> {
                                log.info("- " + d.filePath);
                            });
        }
        // TODO: 通过订阅消息发送打开多文件 Diff 请求
        // 需要创建相应的请求消息类型
    }

    @Data
    public static final class DiffEntry {
        private String filePath;
        private String leftContent;
        private String rightContent;
    }
}
