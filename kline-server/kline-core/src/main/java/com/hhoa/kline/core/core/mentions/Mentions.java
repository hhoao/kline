package com.hhoa.kline.core.core.mentions;

import com.hhoa.kline.core.core.context.tracking.FileContextTracker;
import com.hhoa.kline.core.core.integrations.misc.ExtractText;
import com.hhoa.kline.core.core.services.telemetry.TelemetryService;
import com.hhoa.kline.core.core.shared.ContextMentions;
import com.hhoa.kline.core.core.shared.multiroot.WorkspaceRoot;
import com.hhoa.kline.core.core.shared.proto.host.OpenInFileExplorerPanelRequest;
import com.hhoa.kline.core.core.shared.proto.host.OpenProblemsPanelRequest;
import com.hhoa.kline.core.core.shared.proto.host.OpenTerminalRequest;
import com.hhoa.kline.core.core.shared.proto.host.ShowTextDocumentOptions;
import com.hhoa.kline.core.core.shared.proto.host.ShowTextDocumentRequest;
import com.hhoa.kline.core.core.workspace.WorkspaceRootManager;
import com.hhoa.kline.core.subscription.DefaultSubscriptionManager;
import com.hhoa.kline.core.subscription.SubscriptionManager;
import com.hhoa.kline.core.subscription.message.WindowShowTextDocumentMessage;
import com.hhoa.kline.core.subscription.message.WorkspaceOpenInFileExplorerPanelRequestMessage;
import com.hhoa.kline.core.subscription.message.WorkspaceOpenProblemsPanelMessage;
import com.hhoa.kline.core.subscription.message.WorkspaceOpenTerminalMessage;
import java.awt.Desktop;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

/**
 * 处理文本中的 @mention 格式的提及，支持：
 *
 * <ul>
 *   <li>@/path/to/file - 文件提及
 *   <li>@/path/to/folder/ - 文件夹提及
 *   <li>@problems - 工作区问题
 *   <li>@terminal - 终端输出
 *   <li>@git-changes - Git 变更
 *   <li>@http://... - URL 提及
 *   <li>@workspace:name/path - 工作区前缀的文件路径
 *   <li>@commit-hash - Git 提交哈希
 * </ul>
 */
@Slf4j
public class Mentions {
    private static final Pattern GIT_COMMIT_HASH_PATTERN = Pattern.compile("^[a-f0-9]{7,40}$");
    private static final SubscriptionManager subscriptionManager =
            DefaultSubscriptionManager.getInstance();

    /**
     * 打开提及。
     *
     * @param mention 提及字符串
     * @param cwd 当前工作目录
     * @param workspaceClient 工作区客户端（可选）
     * @param windowClient 窗口客户端（可选，用于打开文件）
     */
    public static CompletableFuture<Void> openMention(
            String mention,
            String cwd,
            Object workspaceClient, // 移除 HostProvider 依赖，改为 Object
            Object windowClient) { // 移除 HostProvider 依赖，改为 Object
        if (mention == null || mention.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }

        if (cwd == null || cwd.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }

        return CompletableFuture.runAsync(
                () -> {
                    try {
                        if (isFileMention(mention)) {
                            String relPath = getFilePathFromMention(mention);
                            Path absPath = Paths.get(cwd, relPath).normalize();

                            if (Files.isDirectory(absPath)) {
                                if (workspaceClient != null) {
                                    OpenInFileExplorerPanelRequest explorerRequest =
                                            OpenInFileExplorerPanelRequest.builder()
                                                    .path(absPath.toString())
                                                    .build();
                                    subscriptionManager.send(
                                            new WorkspaceOpenInFileExplorerPanelRequestMessage(
                                                    explorerRequest));
                                }
                            } else {
                                if (windowClient != null) {
                                    ShowTextDocumentRequest textDocRequest =
                                            ShowTextDocumentRequest.builder()
                                                    .path(absPath.toString())
                                                    .options(
                                                            ShowTextDocumentOptions.builder()
                                                                    .preserveFocus(false)
                                                                    .preview(false)
                                                                    .build())
                                                    .build();
                                    subscriptionManager.send(
                                            new WindowShowTextDocumentMessage(textDocRequest));
                                }
                            }
                        } else if ("problems".equals(mention)) {
                            if (workspaceClient != null) {
                                OpenProblemsPanelRequest problemsRequest =
                                        new OpenProblemsPanelRequest();
                                subscriptionManager.send(
                                        new WorkspaceOpenProblemsPanelMessage(problemsRequest));
                            }
                        } else if ("terminal".equals(mention)) {
                            if (workspaceClient != null) {
                                OpenTerminalRequest terminalRequest = new OpenTerminalRequest();
                                subscriptionManager.send(
                                        new WorkspaceOpenTerminalMessage(terminalRequest));
                            }
                        } else if (mention.startsWith("http")) {
                            try {
                                Desktop.getDesktop().browse(new URI(mention));
                            } catch (Exception e) {
                                log.error("Error opening URL: {}", mention, e);
                            }
                        }
                    } catch (Exception e) {
                        log.error("Error opening mention: {}", mention, e);
                    }
                });
    }

    /**
     * 从文件路径获取文件提及。
     *
     * @param filePath 文件路径
     * @param cwd 当前工作目录
     * @return 文件提及字符串
     */
    public static CompletableFuture<String> getFileMentionFromPath(String filePath, String cwd) {
        if (filePath == null || filePath.isEmpty()) {
            return CompletableFuture.completedFuture("@/" + filePath);
        }

        if (cwd == null || cwd.isEmpty()) {
            return CompletableFuture.completedFuture("@/" + filePath);
        }

        return CompletableFuture.supplyAsync(
                () -> {
                    try {
                        Path filePathObj = Paths.get(filePath).toAbsolutePath().normalize();
                        Path cwdPath = Paths.get(cwd).toAbsolutePath().normalize();
                        Path relativePath = cwdPath.relativize(filePathObj);
                        return "@/" + relativePath.toString().replace('\\', '/');
                    } catch (Exception e) {
                        log.error("Error getting file mention from path: {}", filePath, e);
                        return "@/" + filePath;
                    }
                });
    }

    /**
     * 解析提及。
     *
     * @param text 文本内容
     * @param cwd 当前工作目录
     * @param urlContentFetcher URL 内容获取器
     * @param fileContextTracker 文件上下文跟踪器（可选）
     * @param workspaceManager 工作区管理器（可选）
     * @param workspaceClient 工作区客户端（可选，用于获取诊断信息）
     * @param windowClient 窗口客户端（可选，用于显示错误消息）
     * @param telemetryService 遥测服务（可选，用于记录遥测数据）
     * @return 解析后的文本
     */
    public static CompletableFuture<String> parseMentions(
            String text,
            String cwd,
            UrlContentFetcher urlContentFetcher,
            FileContextTracker fileContextTracker,
            WorkspaceRootManager workspaceManager,
            Object workspaceClient, // 移除 HostProvider 依赖，改为 Object
            Object windowClient, // 移除 HostProvider 依赖，改为 Object
            TelemetryService telemetryService) {
        if (text == null || text.isEmpty()) {
            return CompletableFuture.completedFuture(text);
        }

        return CompletableFuture.supplyAsync(
                        () -> {
                            Set<String> mentions = new LinkedHashSet<>();
                            Matcher matcher = ContextMentions.MENTION_REGEX_GLOBAL.matcher(text);

                            // 第一步：替换文本中的提及为占位符，并收集所有提及
                            StringBuffer parsedText = new StringBuffer();
                            while (matcher.find()) {
                                String mention = matcher.group(1);
                                mentions.add(mention);

                                String replacement;
                                if (mention.startsWith("http")) {
                                    replacement = "'" + mention + "' (see below for site content)";
                                } else if (isFileMention(mention)) {
                                    String mentionPath = getFilePathFromMention(mention);
                                    String workspaceHint = getWorkspaceHintFromMention(mention);
                                    boolean isDirectory = mentionPath.endsWith("/");

                                    if (workspaceHint != null) {
                                        replacement =
                                                isDirectory
                                                        ? "'"
                                                                + workspaceHint
                                                                + ":"
                                                                + mentionPath
                                                                + "' (see below for folder content)"
                                                        : "'"
                                                                + workspaceHint
                                                                + ":"
                                                                + mentionPath
                                                                + "' (see below for file content)";
                                    } else {
                                        replacement =
                                                isDirectory
                                                        ? "'"
                                                                + mentionPath
                                                                + "' (see below for folder content)"
                                                        : "'"
                                                                + mentionPath
                                                                + "' (see below for file content)";
                                    }
                                } else if ("problems".equals(mention)) {
                                    replacement = "Workspace Problems (see below for diagnostics)";
                                } else if ("terminal".equals(mention)) {
                                    replacement = "Terminal Output (see below for output)";
                                } else if ("git-changes".equals(mention)) {
                                    replacement =
                                            "Working directory changes (see below for details)";
                                } else if (GIT_COMMIT_HASH_PATTERN.matcher(mention).matches()) {
                                    replacement =
                                            "Git commit '"
                                                    + mention
                                                    + "' (see below for commit info)";
                                } else {
                                    replacement = matcher.group(0); // 保留原始格式
                                }

                                matcher.appendReplacement(
                                        parsedText, Matcher.quoteReplacement(replacement));
                            }
                            matcher.appendTail(parsedText);

                            return new ParseResult(parsedText.toString(), mentions);
                        })
                .thenCompose(
                        parseResult -> {
                            String parsedText = parseResult.parsedText;
                            Set<String> uniqueMentions = parseResult.mentions;

                            Optional<String> urlMention =
                                    uniqueMentions.stream()
                                            .filter(m -> m.startsWith("http"))
                                            .findFirst();

                            CompletableFuture<Exception> browserLaunchFuture =
                                    urlMention.isPresent()
                                            ? urlContentFetcher
                                                    .launchBrowser()
                                                    .<Exception>thenApply(v -> null)
                                                    .exceptionally(
                                                            error -> {
                                                                Exception exception =
                                                                        error instanceof Exception
                                                                                ? (Exception) error
                                                                                : new RuntimeException(
                                                                                        error
                                                                                                .getMessage());
                                                                String errorMessage =
                                                                        "Error fetching content for "
                                                                                + urlMention.get()
                                                                                + ": "
                                                                                + exception
                                                                                        .getMessage();
                                                                log.error(errorMessage);
                                                                if (windowClient != null) {
                                                                    MentionsHelper.showErrorMessage(
                                                                            windowClient,
                                                                            errorMessage);
                                                                }
                                                                return exception;
                                                            })
                                            : CompletableFuture.completedFuture(null);

                            return browserLaunchFuture.thenCompose(
                                    launchBrowserError -> {
                                        CompletableFuture<String> result =
                                                CompletableFuture.completedFuture(parsedText);

                                        for (String mention : uniqueMentions) {
                                            if ("/".equals(mention)) {
                                                continue;
                                            }

                                            final String currentMention = mention;
                                            final Exception browserError = launchBrowserError;
                                            result =
                                                    result.thenCompose(
                                                            textResult -> {
                                                                if (currentMention.startsWith(
                                                                        "http")) {
                                                                    return handleUrlMention(
                                                                            textResult,
                                                                            currentMention,
                                                                            urlContentFetcher,
                                                                            browserError,
                                                                            windowClient,
                                                                            telemetryService);
                                                                } else if (isFileMention(
                                                                        currentMention)) {
                                                                    return handleFileMention(
                                                                            textResult,
                                                                            currentMention,
                                                                            cwd,
                                                                            fileContextTracker,
                                                                            workspaceManager,
                                                                            telemetryService);
                                                                } else if ("problems"
                                                                        .equals(currentMention)) {
                                                                    return handleProblemsMention(
                                                                            textResult,
                                                                            workspaceClient,
                                                                            cwd,
                                                                            telemetryService);
                                                                } else if ("terminal"
                                                                        .equals(currentMention)) {
                                                                    return handleTerminalMention(
                                                                            textResult,
                                                                            cwd,
                                                                            telemetryService);
                                                                } else if ("git-changes"
                                                                        .equals(currentMention)) {
                                                                    return handleGitChangesMention(
                                                                            textResult,
                                                                            cwd,
                                                                            telemetryService);
                                                                } else if (GIT_COMMIT_HASH_PATTERN
                                                                        .matcher(currentMention)
                                                                        .matches()) {
                                                                    return handleCommitMention(
                                                                            textResult,
                                                                            currentMention,
                                                                            cwd,
                                                                            telemetryService);
                                                                }
                                                                return CompletableFuture
                                                                        .completedFuture(
                                                                                textResult);
                                                            });
                                        }

                                        if (urlMention.isPresent()) {
                                            result =
                                                    result.thenCompose(
                                                            finalText ->
                                                                    urlContentFetcher
                                                                            .closeBrowser()
                                                                            .exceptionally(
                                                                                    error -> {
                                                                                        log.error(
                                                                                                "Error closing browser: {}",
                                                                                                error
                                                                                                        .getMessage());
                                                                                        return null;
                                                                                    })
                                                                            .thenApply(
                                                                                    v2 ->
                                                                                            finalText));
                                        }

                                        return result;
                                    });
                        });
    }

    /** 处理 URL 提及。 */
    private static CompletableFuture<String> handleUrlMention(
            String text,
            String mention,
            UrlContentFetcher urlContentFetcher,
            Exception launchBrowserError,
            Object windowClient, // 移除 HostProvider 依赖，改为 Object
            TelemetryService telemetryService) {
        if (launchBrowserError != null) {
            String result = "Error fetching content: " + launchBrowserError.getMessage();
            if (telemetryService != null) {
                telemetryService.captureMentionFailed(
                        "url", "network_error", launchBrowserError.getMessage());
            }
            return CompletableFuture.completedFuture(
                    text
                            + "\n\n<url_content url=\""
                            + mention
                            + "\">\n"
                            + result
                            + "\n</url_content>");
        }

        return urlContentFetcher
                .urlToMarkdown(mention)
                .thenApply(
                        markdown -> {
                            if (telemetryService != null) {
                                telemetryService.captureMentionUsed("url", markdown.length());
                            }
                            return text
                                    + "\n\n<url_content url=\""
                                    + mention
                                    + "\">\n"
                                    + markdown
                                    + "\n</url_content>";
                        })
                .exceptionally(
                        error -> {
                            String errorMessage = error.getMessage();
                            log.error(
                                    "Error fetching URL content for {}: {}", mention, errorMessage);
                            if (windowClient != null) {
                                MentionsHelper.showErrorMessage(
                                        windowClient,
                                        "Error fetching content for "
                                                + mention
                                                + ": "
                                                + errorMessage);
                            }
                            if (telemetryService != null) {
                                telemetryService.captureMentionFailed(
                                        "url", "network_error", errorMessage);
                            }
                            return text
                                    + "\n\n<url_content url=\""
                                    + mention
                                    + "\">\nError fetching content: "
                                    + errorMessage
                                    + "\n</url_content>";
                        });
    }

    /** 处理文件提及。 */
    private static CompletableFuture<String> handleFileMention(
            String text,
            String mention,
            String cwd,
            FileContextTracker fileContextTracker,
            WorkspaceRootManager workspaceManager,
            TelemetryService telemetryService) {
        String mentionPath = getFilePathFromMention(mention);
        String mentionType = mention.endsWith("/") ? "folder" : "file";
        String workspaceHint = getWorkspaceHintFromMention(mention);

        boolean isMultiRoot =
                workspaceManager != null
                        && !workspaceManager.getRoots().isEmpty()
                        && workspaceManager.getRoots().size() > 1;

        if (isMultiRoot && workspaceHint == null && workspaceManager != null) {
            // 并行搜索所有工作区
            List<WorkspaceRoot> workspaceRoots = workspaceManager.getRoots();
            List<CompletableFuture<FileSearchResult>> searchFutures =
                    workspaceRoots.stream()
                            .map(
                                    root ->
                                            getFileOrFolderContent(mentionPath, root.getPath())
                                                    .thenApply(
                                                            content ->
                                                                    new FileSearchResult(
                                                                            root.getName() != null
                                                                                    ? root.getName()
                                                                                    : Paths.get(
                                                                                                    root
                                                                                                            .getPath())
                                                                                            .getFileName()
                                                                                            .toString(),
                                                                            content,
                                                                            true,
                                                                            null))
                                                    .exceptionally(
                                                            error ->
                                                                    new FileSearchResult(
                                                                            root.getName() != null
                                                                                    ? root.getName()
                                                                                    : Paths.get(
                                                                                                    root
                                                                                                            .getPath())
                                                                                            .getFileName()
                                                                                            .toString(),
                                                                            null,
                                                                            false,
                                                                            error.getMessage())))
                            .collect(Collectors.toList());

            return CompletableFuture.allOf(searchFutures.toArray(new CompletableFuture[0]))
                    .thenApply(
                            v -> {
                                List<FileSearchResult> results =
                                        searchFutures.stream()
                                                .map(CompletableFuture::join)
                                                .collect(Collectors.toList());

                                List<FileSearchResult> successfulResults =
                                        results.stream()
                                                .filter(r -> r.success && r.content != null)
                                                .collect(Collectors.toList());

                                if (successfulResults.isEmpty()) {
                                    String errorMsg =
                                            "File not found in any workspace. Searched: "
                                                    + results.stream()
                                                            .map(r -> r.workspaceName)
                                                            .collect(Collectors.joining(", "));
                                    // 记录失败的 mention 遥测
                                    if (telemetryService != null) {
                                        telemetryService.captureMentionFailed(
                                                mentionType, "not_found", errorMsg);
                                    }
                                    return addFileContentToText(
                                            text, mentionPath, mentionType, null, errorMsg, false);
                                } else if (successfulResults.size() == 1) {
                                    FileSearchResult result = successfulResults.get(0);
                                    String newText =
                                            addFileContentToText(
                                                    text,
                                                    mentionPath,
                                                    mentionType,
                                                    result.workspaceName,
                                                    result.content,
                                                    true);
                                    // 跟踪文件上下文
                                    if (fileContextTracker != null && !mention.endsWith("/")) {
                                        fileContextTracker
                                                .trackFileContext(mentionPath, "file_mentioned")
                                                .exceptionally(
                                                        error -> {
                                                            log.error(
                                                                    "Error tracking file context: {}",
                                                                    error.getMessage());
                                                            return null;
                                                        });
                                    }
                                    // 记录成功的 mention 遥测
                                    if (telemetryService != null) {
                                        telemetryService.captureMentionUsed(
                                                mentionType, result.content.length());
                                    }
                                    return newText;
                                } else {
                                    // 在多个工作区中找到，包含所有候选项
                                    StringBuilder newText = new StringBuilder(text);
                                    int totalLength = 0;
                                    for (FileSearchResult result : successfulResults) {
                                        newText.append(
                                                "\n\n"
                                                        + getFileContentTag(
                                                                mentionPath,
                                                                mentionType,
                                                                result.workspaceName)
                                                        + "\n"
                                                        + result.content
                                                        + "\n"
                                                        + getFileContentClosingTag(mentionType));
                                        if (result.content != null) {
                                            totalLength += result.content.length();
                                        }
                                    }
                                    // 记录成功的 mention 遥测
                                    if (telemetryService != null) {
                                        telemetryService.captureMentionUsed(
                                                mentionType, totalLength);
                                    }
                                    return newText.toString();
                                }
                            });
        } else if (isMultiRoot && workspaceHint != null && workspaceManager != null) {
            // 仅在指定工作区中搜索
            WorkspaceRoot targetRoot = workspaceManager.getRootByName(workspaceHint);
            if (targetRoot == null) {
                String errorMsg = "Workspace '" + workspaceHint + "' not found";
                // 记录失败的 mention 遥测
                if (telemetryService != null) {
                    telemetryService.captureMentionFailed(mentionType, "not_found", errorMsg);
                }
                return CompletableFuture.completedFuture(
                        addFileContentToText(
                                text, mentionPath, mentionType, workspaceHint, errorMsg, false));
            }

            return getFileOrFolderContent(mentionPath, targetRoot.getPath())
                    .thenApply(
                            content -> {
                                String newText =
                                        addFileContentToText(
                                                text,
                                                mentionPath,
                                                mentionType,
                                                workspaceHint,
                                                content,
                                                true);
                                // 跟踪文件上下文
                                if (fileContextTracker != null && !mention.endsWith("/")) {
                                    fileContextTracker
                                            .trackFileContext(mentionPath, "file_mentioned")
                                            .exceptionally(
                                                    error -> {
                                                        log.error(
                                                                "Error tracking file context: {}",
                                                                error.getMessage());
                                                        return null;
                                                    });
                                }
                                // 记录成功的 mention 遥测
                                if (telemetryService != null) {
                                    telemetryService.captureMentionUsed(
                                            mentionType, content.length());
                                }
                                return newText;
                            })
                    .exceptionally(
                            error -> {
                                String errorMessage = error.getMessage();
                                String errorType = MentionsHelper.determineErrorType(errorMessage);
                                // 记录失败的 mention 遥测
                                if (telemetryService != null) {
                                    telemetryService.captureMentionFailed(
                                            mentionType, errorType, errorMessage);
                                }
                                return addFileContentToText(
                                        text,
                                        mentionPath,
                                        mentionType,
                                        workspaceHint,
                                        errorMessage,
                                        false);
                            });
        } else {
            // 传统单工作区模式
            return getFileOrFolderContent(mentionPath, cwd)
                    .thenApply(
                            content -> {
                                String newText =
                                        addFileContentToText(
                                                text,
                                                mentionPath,
                                                mentionType,
                                                null,
                                                content,
                                                true);
                                // 跟踪文件上下文
                                if (fileContextTracker != null && !mention.endsWith("/")) {
                                    fileContextTracker
                                            .trackFileContext(mentionPath, "file_mentioned")
                                            .exceptionally(
                                                    error -> {
                                                        log.error(
                                                                "Error tracking file context: {}",
                                                                error.getMessage());
                                                        return null;
                                                    });
                                }
                                // 记录成功的 mention 遥测
                                if (telemetryService != null) {
                                    telemetryService.captureMentionUsed(
                                            mentionType, content.length());
                                }
                                return newText;
                            })
                    .exceptionally(
                            error -> {
                                String errorMessage = error.getMessage();
                                String errorType = MentionsHelper.determineErrorType(errorMessage);
                                // 记录失败的 mention 遥测
                                if (telemetryService != null) {
                                    telemetryService.captureMentionFailed(
                                            mentionType, errorType, errorMessage);
                                }
                                return addFileContentToText(
                                        text, mentionPath, mentionType, null, errorMessage, false);
                            });
        }
    }

    /** 处理问题提及。 */
    private static CompletableFuture<String> handleProblemsMention(
            String text,
            Object workspaceClient, // 移除 HostProvider 依赖，改为 Object
            String cwd,
            TelemetryService telemetryService) {
        return MentionsHelper.getWorkspaceProblems(workspaceClient, cwd)
                .thenApply(
                        problems -> {
                            // 记录成功的 problems mention 遥测
                            if (telemetryService != null) {
                                telemetryService.captureMentionUsed("problems", problems.length());
                            }
                            return text
                                    + "\n\n<workspace_diagnostics>\n"
                                    + problems
                                    + "\n</workspace_diagnostics>";
                        })
                .exceptionally(
                        error -> {
                            String errorMessage = error.getMessage();
                            // 记录失败的 problems mention 遥测
                            if (telemetryService != null) {
                                telemetryService.captureMentionFailed(
                                        "problems", "unknown", errorMessage);
                            }
                            return text
                                    + "\n\n<workspace_diagnostics>\nError fetching diagnostics: "
                                    + errorMessage
                                    + "\n</workspace_diagnostics>";
                        });
    }

    /** 处理终端提及。 */
    private static CompletableFuture<String> handleTerminalMention(
            String text, String cwd, TelemetryService telemetryService) {
        return getLatestTerminalOutput(cwd)
                .thenApply(
                        output -> {
                            // 记录成功的 terminal mention 遥测
                            if (telemetryService != null) {
                                telemetryService.captureMentionUsed("terminal", output.length());
                            }
                            return text
                                    + "\n\n<terminal_output>\n"
                                    + output
                                    + "\n</terminal_output>";
                        })
                .exceptionally(
                        error -> {
                            String errorMessage = error.getMessage();
                            // 记录失败的 terminal mention 遥测
                            if (telemetryService != null) {
                                telemetryService.captureMentionFailed(
                                        "terminal", "unknown", errorMessage);
                            }
                            return text
                                    + "\n\n<terminal_output>\nError fetching terminal output: "
                                    + errorMessage
                                    + "\n</terminal_output>";
                        });
    }

    /** 处理 Git 变更提及。 */
    private static CompletableFuture<String> handleGitChangesMention(
            String text, String cwd, TelemetryService telemetryService) {
        return getWorkingState(cwd)
                .thenApply(
                        workingState -> {
                            // 记录成功的 git-changes mention 遥测
                            if (telemetryService != null) {
                                telemetryService.captureMentionUsed(
                                        "git-changes", workingState.length());
                            }
                            return text
                                    + "\n\n<git_working_state>\n"
                                    + workingState
                                    + "\n</git_working_state>";
                        })
                .exceptionally(
                        error -> {
                            String errorMessage = error.getMessage();
                            // 记录失败的 git-changes mention 遥测
                            if (telemetryService != null) {
                                telemetryService.captureMentionFailed(
                                        "git-changes", "unknown", errorMessage);
                            }
                            return text
                                    + "\n\n<git_working_state>\nError fetching working state: "
                                    + errorMessage
                                    + "\n</git_working_state>";
                        });
    }

    /** 处理提交提及。 */
    private static CompletableFuture<String> handleCommitMention(
            String text, String mention, String cwd, TelemetryService telemetryService) {
        return getCommitInfo(mention, cwd)
                .thenApply(
                        commitInfo -> {
                            // 记录成功的 commit mention 遥测
                            if (telemetryService != null) {
                                telemetryService.captureMentionUsed("commit", commitInfo.length());
                            }
                            return text
                                    + "\n\n<git_commit hash=\""
                                    + mention
                                    + "\">\n"
                                    + commitInfo
                                    + "\n</git_commit>";
                        })
                .exceptionally(
                        error -> {
                            String errorMessage = error.getMessage();
                            // 记录失败的 commit mention 遥测
                            if (telemetryService != null) {
                                telemetryService.captureMentionFailed(
                                        "commit", "unknown", errorMessage);
                            }
                            return text
                                    + "\n\n<git_commit hash=\""
                                    + mention
                                    + "\">\nError fetching commit info: "
                                    + errorMessage
                                    + "\n</git_commit>";
                        });
    }

    /** 获取文件或文件夹内容。 */
    private static CompletableFuture<String> getFileOrFolderContent(
            String mentionPath, String cwd) {
        return CompletableFuture.supplyAsync(
                () -> {
                    try {
                        Path absPath = Paths.get(cwd, mentionPath).normalize();

                        if (!Files.exists(absPath)) {
                            throw new IOException(
                                    "Failed to access path \""
                                            + mentionPath
                                            + "\": File not found");
                        }

                        if (Files.isRegularFile(absPath)) {
                            byte[] fileBytes = Files.readAllBytes(absPath);
                            if (isBinaryFile(fileBytes)) {
                                return "(Binary file, unable to display content)";
                            }
                            return ExtractText.extractTextFromFile(absPath);
                        } else if (Files.isDirectory(absPath)) {
                            List<Path> entries = Files.list(absPath).collect(Collectors.toList());

                            StringBuilder folderContent = new StringBuilder();
                            List<CompletableFuture<String>> fileContentFutures = new ArrayList<>();

                            for (int i = 0; i < entries.size(); i++) {
                                Path entry = entries.get(i);
                                boolean isLast = i == entries.size() - 1;
                                String linePrefix = isLast ? "└── " : "├── ";

                                if (Files.isRegularFile(entry)) {
                                    folderContent
                                            .append(linePrefix)
                                            .append(entry.getFileName())
                                            .append("\n");

                                    Path filePath = absPath.resolve(entry.getFileName());
                                    final String relativeFilePath =
                                            ((mentionPath.isEmpty()
                                                                    ? ""
                                                                    : (mentionPath.endsWith("/")
                                                                            ? mentionPath
                                                                            : mentionPath + "/"))
                                                            + entry.getFileName())
                                                    .replace('\\', '/');

                                    fileContentFutures.add(
                                            CompletableFuture.supplyAsync(
                                                    () -> {
                                                        try {
                                                            byte[] fileBytes =
                                                                    Files.readAllBytes(filePath);
                                                            if (isBinaryFile(fileBytes)) {
                                                                return null;
                                                            }
                                                            String content =
                                                                    ExtractText.extractTextFromFile(
                                                                            filePath);
                                                            return "<file_content path=\""
                                                                    + relativeFilePath
                                                                    + "\">\n"
                                                                    + content
                                                                    + "\n</file_content>";
                                                        } catch (Exception e) {
                                                            log.debug(
                                                                    "Error reading file content: {}",
                                                                    filePath,
                                                                    e);
                                                            return null;
                                                        }
                                                    }));
                                } else if (Files.isDirectory(entry)) {
                                    folderContent
                                            .append(linePrefix)
                                            .append(entry.getFileName())
                                            .append("/\n");
                                } else {
                                    folderContent
                                            .append(linePrefix)
                                            .append(entry.getFileName())
                                            .append("\n");
                                }
                            }

                            List<String> fileContents =
                                    fileContentFutures.stream()
                                            .map(CompletableFuture::join)
                                            .filter(Objects::nonNull)
                                            .collect(Collectors.toList());

                            String folderContentStr = folderContent.toString();
                            if (!fileContents.isEmpty()) {
                                return folderContentStr + "\n" + String.join("\n\n", fileContents);
                            }
                            return folderContentStr.trim();
                        } else {
                            return "(Failed to read contents of " + mentionPath + ")";
                        }
                    } catch (IOException e) {
                        throw new RuntimeException(
                                "Failed to access path \"" + mentionPath + "\": " + e.getMessage(),
                                e);
                    }
                });
    }

    /** 获取最新终端输出。 */
    private static CompletableFuture<String> getLatestTerminalOutput(String cwd) {
        return CompletableFuture.supplyAsync(
                () -> {
                    try {
                        return "Terminal output not available in Java implementation";
                    } catch (Exception e) {
                        log.error("Error getting terminal output: {}", e.getMessage(), e);
                        throw new RuntimeException(
                                "Error fetching terminal output: " + e.getMessage(), e);
                    }
                });
    }

    /** 获取工作区状态。 */
    private static CompletableFuture<String> getWorkingState(String cwd) {
        return CompletableFuture.supplyAsync(
                () -> {
                    try {
                        if (!checkGitInstalled()) {
                            return "Git is not installed";
                        }

                        if (!checkGitRepo(cwd)) {
                            return "Not a git repository";
                        }

                        String status = executeGitCommand(cwd, "git status --short");
                        if (status == null || status.trim().isEmpty()) {
                            return "No changes in working directory";
                        }

                        String diff = "";
                        if (checkGitRepoHasCommits(cwd)) {
                            String diffOutput = executeGitCommand(cwd, "git diff HEAD");
                            if (diffOutput != null) {
                                diff = diffOutput;
                            }
                        } else {
                            return "Working directory changes (new repository):\n\n" + status;
                        }

                        String output = "Working directory changes:\n\n" + status + "\n\n" + diff;
                        return truncateOutput(output.trim());
                    } catch (Exception e) {
                        log.error("Error getting working state: {}", e.getMessage(), e);
                        return "Failed to get working state: " + e.getMessage();
                    }
                });
    }

    /** 获取提交信息。 */
    private static CompletableFuture<String> getCommitInfo(String hash, String cwd) {
        return CompletableFuture.supplyAsync(
                () -> {
                    try {
                        if (!checkGitInstalled()) {
                            return "Git is not installed";
                        }

                        if (!checkGitRepo(cwd)) {
                            return "Not a git repository";
                        }

                        if (!checkGitRepoHasCommits(cwd)) {
                            return "Repository has no commits yet";
                        }

                        String info =
                                executeGitCommand(
                                        cwd,
                                        "git show --format=\"%H%n%h%n%s%n%an%n%ad%n%b\" --no-patch "
                                                + hash);
                        if (info == null || info.trim().isEmpty()) {
                            return "Commit not found: " + hash;
                        }

                        String[] lines = info.trim().split("\n", 6);
                        String fullHash = lines.length > 0 ? lines[0] : hash;
                        String shortHash = lines.length > 1 ? lines[1] : hash;
                        String subject = lines.length > 2 ? lines[2] : "";
                        String author = lines.length > 3 ? lines[3] : "";
                        String date = lines.length > 4 ? lines[4] : "";
                        String body = lines.length > 5 ? lines[5] : "";

                        String stats =
                                executeGitCommand(cwd, "git show --stat --format=\"\" " + hash);
                        if (stats == null) {
                            stats = "";
                        }

                        String diff = executeGitCommand(cwd, "git show --format=\"\" " + hash);
                        if (diff == null) {
                            diff = "";
                        }

                        StringBuilder summary = new StringBuilder();
                        summary.append("Commit: ")
                                .append(shortHash)
                                .append(" (")
                                .append(fullHash)
                                .append(")\n");
                        summary.append("Author: ").append(author).append("\n");
                        summary.append("Date: ").append(date).append("\n");
                        summary.append("\nMessage: ").append(subject).append("\n");
                        if (!body.isEmpty()) {
                            summary.append("\nDescription:\n").append(body).append("\n");
                        }
                        summary.append("\nFiles Changed:\n").append(stats.trim()).append("\n");
                        summary.append("\nFull Changes:\n");

                        String output = summary + "\n\n" + diff.trim();
                        return truncateOutput(output);
                    } catch (Exception e) {
                        log.error("Error getting commit info: {}", e.getMessage(), e);
                        return "Failed to get commit info: " + e.getMessage();
                    }
                });
    }

    /** 检查 Git 是否安装 */
    private static boolean checkGitInstalled() {
        try {
            Process process = new ProcessBuilder("git", "--version").start();
            int exitCode = process.waitFor();
            return exitCode == 0;
        } catch (Exception e) {
            return false;
        }
    }

    /** 检查是否为 Git 仓库 */
    private static boolean checkGitRepo(String cwd) {
        try {
            ProcessBuilder pb = new ProcessBuilder("git", "rev-parse", "--git-dir");
            pb.directory(Paths.get(cwd).toFile());
            Process process = pb.start();
            int exitCode = process.waitFor();
            return exitCode == 0;
        } catch (Exception e) {
            return false;
        }
    }

    /** 检查 Git 仓库是否有提交 */
    private static boolean checkGitRepoHasCommits(String cwd) {
        try {
            ProcessBuilder pb = new ProcessBuilder("git", "rev-parse", "HEAD");
            pb.directory(Paths.get(cwd).toFile());
            Process process = pb.start();
            int exitCode = process.waitFor();
            return exitCode == 0;
        } catch (Exception e) {
            return false;
        }
    }

    /** 执行 Git 命令 */
    private static String executeGitCommand(String cwd, String command) {
        try {
            ProcessBuilder pb = new ProcessBuilder("bash", "-c", command);
            pb.directory(Paths.get(cwd).toFile());
            pb.redirectErrorStream(true);
            Process process = pb.start();

            try (BufferedReader reader =
                    new BufferedReader(
                            new InputStreamReader(
                                    process.getInputStream(), StandardCharsets.UTF_8))) {
                StringBuilder output = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
                int exitCode = process.waitFor();
                if (exitCode == 0) {
                    return output.toString();
                }
            }
        } catch (Exception e) {
            log.debug("Error executing git command: {}", command, e);
        }
        return null;
    }

    /** 截断输出。 */
    private static String truncateOutput(String content) {
        int GIT_OUTPUT_LINE_LIMIT = 500;
        if (GIT_OUTPUT_LINE_LIMIT <= 0) {
            return content;
        }

        String[] lines = content.split("\n");
        if (lines.length <= GIT_OUTPUT_LINE_LIMIT) {
            return content;
        }

        int beforeLimit = (int) (GIT_OUTPUT_LINE_LIMIT * 0.2); // 20% of lines before
        int afterLimit = GIT_OUTPUT_LINE_LIMIT - beforeLimit; // remaining 80% after

        StringBuilder result = new StringBuilder();
        for (int i = 0; i < beforeLimit; i++) {
            result.append(lines[i]).append("\n");
        }
        result.append("\n[...")
                .append(lines.length - GIT_OUTPUT_LINE_LIMIT)
                .append(" lines omitted...]\n");
        for (int i = lines.length - afterLimit; i < lines.length; i++) {
            result.append(lines[i]).append("\n");
        }
        return result.toString();
    }

    private static boolean isFileMention(String mention) {
        if (parseWorkspaceMention(mention) != null) {
            return true;
        }
        return mention.startsWith("/") || mention.startsWith("\"/");
    }

    private static String getFilePathFromMention(String mention) {
        WorkspaceMention workspaceMention = parseWorkspaceMention(mention);
        if (workspaceMention != null) {
            String path = workspaceMention.path;
            return path.startsWith("/") ? path.substring(1) : path;
        }

        String filePath = mention;
        if (mention.startsWith("\"") && mention.endsWith("\"")) {
            filePath = mention.substring(1, mention.length() - 1);
        }
        return filePath.startsWith("/") ? filePath.substring(1) : filePath;
    }

    /** 从提及中获取工作区提示。 */
    private static String getWorkspaceHintFromMention(String mention) {
        WorkspaceMention workspaceMention = parseWorkspaceMention(mention);
        return workspaceMention != null ? workspaceMention.workspaceHint : null;
    }

    private static WorkspaceMention parseWorkspaceMention(String mention) {
        Pattern pattern = Pattern.compile("^([\\w-]+):(.+)$");
        Matcher matcher = pattern.matcher(mention);
        if (!matcher.matches()) {
            return null;
        }

        String workspaceHint = matcher.group(1);
        String pathPart = matcher.group(2);

        if (mention.contains("://")) {
            return null;
        }

        String cleanPath = pathPart;
        if (pathPart.startsWith("\"") && pathPart.endsWith("\"")) {
            cleanPath = pathPart.substring(1, pathPart.length() - 1);
        }

        return new WorkspaceMention(workspaceHint, cleanPath);
    }

    private static boolean isBinaryFile(byte[] buffer) {
        if (buffer.length == 0) {
            return false;
        }

        int nullCount = 0;
        int checkLength = Math.min(buffer.length, 512);
        for (int i = 0; i < checkLength; i++) {
            if (buffer[i] == 0) {
                nullCount++;
            }
        }

        return (nullCount * 100.0 / checkLength) > 5.0;
    }

    /** 添加文件内容到文本 */
    private static String addFileContentToText(
            String text,
            String mentionPath,
            String mentionType,
            String workspaceName,
            String content,
            boolean isSuccess) {
        String tag = getFileContentTag(mentionPath, mentionType, workspaceName);
        String closingTag = getFileContentClosingTag(mentionType);
        String prefix = isSuccess ? "" : "Error fetching content: ";
        return text + "\n\n" + tag + "\n" + prefix + content + "\n" + closingTag;
    }

    /** 获取文件内容标签 */
    private static String getFileContentTag(
            String mentionPath, String mentionType, String workspaceName) {
        if ("folder".equals(mentionType)) {
            if (workspaceName != null) {
                return "<folder_content path=\""
                        + mentionPath
                        + "\" workspace=\""
                        + workspaceName
                        + "\">";
            } else {
                return "<folder_content path=\"" + mentionPath + "\">";
            }
        } else {
            if (workspaceName != null) {
                return "<file_content path=\""
                        + mentionPath
                        + "\" workspace=\""
                        + workspaceName
                        + "\">";
            } else {
                return "<file_content path=\"" + mentionPath + "\">";
            }
        }
    }

    /** 获取文件内容闭合标签 */
    private static String getFileContentClosingTag(String mentionType) {
        return "folder".equals(mentionType) ? "</folder_content>" : "</file_content>";
    }

    private record ParseResult(String parsedText, Set<String> mentions) {}

    private record FileSearchResult(
            String workspaceName,
            String content,
            boolean success,
            @SuppressWarnings("unused") String error) {
        private FileSearchResult(
                String workspaceName, String content, boolean success, String error) {
            this.workspaceName = workspaceName;
            this.content = content;
            this.success = success;
            this.error = error;
        }
    }

    private record WorkspaceMention(String workspaceHint, String path) {}
}
