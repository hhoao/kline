package com.hhoa.kline.core.core.mentions;

import com.hhoa.kline.core.core.shared.proto.host.GetWorkspaceProblemsRequest;
import com.hhoa.kline.core.core.shared.proto.host.ShowMessageRequest;
import com.hhoa.kline.core.core.shared.proto.host.ShowMessageType;
import com.hhoa.kline.core.subscription.HostRequestResponseManager;
import com.hhoa.kline.core.subscription.MessageSender;
import com.hhoa.kline.core.subscription.message.WindowShowMessageRequestMessage;
import com.hhoa.kline.core.subscription.message.WorkspaceGetWorkspaceProblemMessage;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import lombok.extern.slf4j.Slf4j;

/** Mentions 辅助类，提供与主流程一致的辅助功能实现。 */
@Slf4j
public class MentionsHelper {
    private static final HostRequestResponseManager requestResponseManager =
            HostRequestResponseManager.getInstance();
    private static final Duration WORKSPACE_PROBLEMS_TIMEOUT = Duration.ofSeconds(30);
    private static final String DEFAULT_WORKSPACE_PROBLEMS = "No errors or warnings detected.";

    /** 显示错误消息。 */
    public static CompletableFuture<Void> showErrorMessage(
            Object windowClient, String message, MessageSender messageSender) {
        ShowMessageRequest request =
                ShowMessageRequest.builder().type(ShowMessageType.ERROR).message(message).build();
        messageSender.send(new WindowShowMessageRequestMessage(request));
        return CompletableFuture.completedFuture(null);
    }

    /** 获取工作区问题。 */
    public static CompletableFuture<String> getWorkspaceProblems(
            Object workspaceClient, String cwd, MessageSender messageSender) {
        GetWorkspaceProblemsRequest request =
                GetWorkspaceProblemsRequest.builder().cwd(cwd).build();
        return requestResponseManager
                .<String>sendRequest(
                        messageSender,
                        new WorkspaceGetWorkspaceProblemMessage(request),
                        WORKSPACE_PROBLEMS_TIMEOUT)
                .thenApply(response -> response != null ? response : DEFAULT_WORKSPACE_PROBLEMS)
                .exceptionally(
                        error -> {
                            log.warn("[getWorkspaceProblems] 请求失败，返回默认文案", error);
                            return DEFAULT_WORKSPACE_PROBLEMS;
                        });
    }

    /** 确定错误类型。 */
    public static String determineErrorType(String errorMessage) {
        if (errorMessage == null) {
            return "unknown";
        }
        if (errorMessage.contains("ENOENT") || errorMessage.contains("Failed to access")) {
            return "not_found";
        } else if (errorMessage.contains("EACCES") || errorMessage.contains("permission")) {
            return "permission_denied";
        } else {
            return "unknown";
        }
    }
}
