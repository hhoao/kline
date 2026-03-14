package com.hhoa.kline.core.core.task.tools.handlers;

import com.hhoa.ai.kline.commons.utils.JsonUtils;
import com.hhoa.kline.core.core.assistant.ToolUse;
import com.hhoa.kline.core.core.assistant.UserContentBlock;
import com.hhoa.kline.core.core.prompts.ResponseFormatter;
import com.hhoa.kline.core.core.prompts.systemprompt.ClineToolSpec;
import com.hhoa.kline.core.core.prompts.systemprompt.ModelFamily;
import com.hhoa.kline.core.core.prompts.systemprompt.tools.SearchFilesTool;
import com.hhoa.kline.core.core.services.ripgrep.RipgrepService;
import com.hhoa.kline.core.core.services.telemetry.TelemetryService;
import com.hhoa.kline.core.core.shared.ClineAsk;
import com.hhoa.kline.core.core.shared.ClineMessageFormat;
import com.hhoa.kline.core.core.shared.ClineSay;
import com.hhoa.kline.core.core.task.TaskUtils;
import com.hhoa.kline.core.core.task.tools.types.TaskConfig;
import com.hhoa.kline.core.core.task.tools.types.UIHelpers;
import com.hhoa.kline.core.core.task.tools.utils.ToolResultUtils;
import com.hhoa.kline.core.core.workspace.WorkspaceConfig;
import com.hhoa.kline.core.core.workspace.WorkspacePathAdapter;
import com.hhoa.kline.core.core.workspace.WorkspaceRootManager;
import com.hhoa.kline.core.core.workspace.utils.ParsedWorkspacePath;
import com.hhoa.kline.core.core.workspace.utils.WorkspacePathParser;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SearchFilesToolHandler implements FullyManagedTool {

    private static final String NAME = "search_files";

    private final ResponseFormatter formatResponse = new ResponseFormatter();
    private final RipgrepService ripgrepService = new RipgrepService();

    private static boolean arePathsEqual(String path1, String path2) {
        if (path1 == null && path2 == null) {
            return true;
        }
        if (path1 == null || path2 == null) {
            return false;
        }
        try {
            Path p1 = Paths.get(path1).normalize().toAbsolutePath();
            Path p2 = Paths.get(path2).normalize().toAbsolutePath();
            return p1.equals(p2);
        } catch (Exception e) {
            return path1.equals(path2);
        }
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String getDescription(ToolUse block) {
        String regex = HandlerUtils.getStringParam(block, "regex");
        String pattern = HandlerUtils.getStringParam(block, "file_pattern");
        return "["
                + block.getName()
                + " for '"
                + (regex == null ? "" : regex)
                + "'"
                + (pattern != null ? (" in '" + pattern + "'") : "")
                + "]";
    }

    @Override
    public ClineToolSpec getClineToolSpec() {
        return SearchFilesTool.create(ModelFamily.GENERIC);
    }

    @Override
    public void handlePartialBlock(ToolUse block, UIHelpers ui) {
        String relPath = HandlerUtils.getStringParam(block, "path");
        String regex = HandlerUtils.getStringParam(block, "regex");
        String filePattern = HandlerUtils.getStringParam(block, "file_pattern");

        TaskConfig config = ui.getConfig();
        Map<String, Object> messageMap = new HashMap<>();
        messageMap.put("tool", "searchFiles");
        messageMap.put("path", HandlerUtils.getReadablePath(config.getCwd(), relPath));
        messageMap.put("content", "");
        messageMap.put("regex", regex);
        messageMap.put("filePattern", filePattern);
        messageMap.put(
                "operationIsLocatedInWorkspace",
                String.valueOf(HandlerUtils.isLocatedInWorkspace(relPath, config)));
        String message = JsonUtils.toJsonString(messageMap);

        Boolean approve = ui.shouldAutoApproveToolWithPath(block.getName(), relPath);
        if (Boolean.TRUE.equals(approve)) {
            ui.say(ClineSay.TOOL, message, null, null, block.isPartial(), ClineMessageFormat.JSON);
        } else {
            ui.ask(ClineAsk.TOOL, message, block.isPartial(), ClineMessageFormat.JSON);
        }
    }

    private List<SearchPathInfo> determineSearchPaths(
            TaskConfig config, String parsedPath, String workspaceHint, String originalPath) {
        List<SearchPathInfo> searchPaths = new ArrayList<>();

        WorkspaceRootManager manager = config.getWorkspaceManager();
        if (manager == null) {
            throw new IllegalStateException("workspaceManager 未配置，无法执行跨工作区搜索");
        }
        WorkspacePathAdapter adapter = new WorkspacePathAdapter(new WorkspaceConfig(manager));

        if (workspaceHint != null && !workspaceHint.isEmpty()) {
            String absolutePath = adapter.resolvePath(parsedPath, workspaceHint);
            List<WorkspacePathAdapter.WorkspaceInfo> workspaceRoots = adapter.getWorkspaceRoots();
            WorkspacePathAdapter.WorkspaceInfo root =
                    workspaceRoots.stream()
                            .filter(r -> r.name().equals(workspaceHint))
                            .findFirst()
                            .orElse(null);
            searchPaths.add(
                    new SearchPathInfo(
                            absolutePath, workspaceHint, root != null ? root.path() : null));
        } else {
            List<String> allPaths = adapter.getAllPossiblePaths(parsedPath);
            List<WorkspacePathAdapter.WorkspaceInfo> workspaceRoots = adapter.getWorkspaceRoots();
            for (int i = 0; i < allPaths.size(); i++) {
                String absPath = allPaths.get(i);
                WorkspacePathAdapter.WorkspaceInfo root =
                        i < workspaceRoots.size() ? workspaceRoots.get(i) : null;
                String workspaceName =
                        root != null ? root.name() : Paths.get(absPath).getFileName().toString();
                searchPaths.add(
                        new SearchPathInfo(
                                absPath, workspaceName, root != null ? root.path() : null));
            }
        }

        return searchPaths;
    }

    private SearchResult executeSearch(
            TaskConfig config,
            String absolutePath,
            String workspaceName,
            String workspaceRoot,
            String regex,
            String filePattern) {
        try {
            String basePathForRelative = workspaceRoot != null ? workspaceRoot : config.getCwd();

            String workspaceResults =
                    ripgrepService.regexSearchFiles(
                            basePathForRelative,
                            absolutePath,
                            regex,
                            filePattern,
                            config.getServices() != null
                                    ? config.getServices().getClineIgnoreController()
                                    : null);

            String firstLine = workspaceResults.split("\n")[0];
            int resultCount = 0;
            Pattern countPattern = Pattern.compile("Found (\\d+) result");
            Matcher matcher = countPattern.matcher(firstLine);
            if (matcher.find()) {
                resultCount = Integer.parseInt(matcher.group(1));
            }

            return new SearchResult(workspaceName, workspaceResults, resultCount, true);
        } catch (Exception error) {
            log.error("Search failed in " + absolutePath + ": " + error.getMessage());
            return new SearchResult(workspaceName, "", 0, false);
        }
    }

    private String formatSearchResults(
            TaskConfig config, List<SearchResult> searchResults, List<SearchPathInfo> searchPaths) {
        List<String> allResults = new ArrayList<>();
        int totalResultCount = 0;

        for (SearchResult result : searchResults) {
            if (!result.success
                    || result.workspaceResults == null
                    || result.workspaceResults.isEmpty()) {
                continue;
            }

            totalResultCount += result.resultCount;

            if (result.resultCount > 0) {
                String[] lines = result.workspaceResults.split("\n");
                String resultsWithoutHeader =
                        lines.length > 2
                                ? String.join("\n", Arrays.copyOfRange(lines, 2, lines.length))
                                : result.workspaceResults;

                if (!resultsWithoutHeader.trim().isEmpty()) {
                    allResults.add(
                            "## Workspace: " + result.workspaceName + "\n" + resultsWithoutHeader);
                }
            }
        }

        if (totalResultCount == 0) {
            return "Found 0 results.";
        } else {
            String workspaceText = searchPaths.size() == 1 ? "workspace" : "workspaces";
            String resultCountText =
                    totalResultCount == 1
                            ? "1 result"
                            : String.format("%,d results", totalResultCount);
            return String.format(
                    "Found %s across %d %s.\n\n%s",
                    resultCountText,
                    searchPaths.size(),
                    workspaceText,
                    String.join("\n\n", allResults));
        }
    }

    @Override
    public List<UserContentBlock> execute(TaskConfig config, ToolUse block) {
        String relDirPath = HandlerUtils.getStringParam(block, "path");
        String regex = HandlerUtils.getStringParam(block, "regex");
        String filePattern = HandlerUtils.getStringParam(block, "file_pattern");

        if (regex == null || regex.isEmpty()) {
            config.getTaskState()
                    .setConsecutiveMistakeCount(
                            config.getTaskState().getConsecutiveMistakeCount() + 1);
            String errorResult = config.getCallbacks().sayAndCreateMissingParamError(NAME, "regex");
            return HandlerUtils.createTextBlocks(errorResult);
        }

        config.getTaskState().setConsecutiveMistakeCount(0);

        ParsedWorkspacePath parsed = WorkspacePathParser.parseWorkspaceInlinePath(relDirPath);
        String workspaceHint = parsed.getWorkspaceHint();
        String parsedPath = parsed.getRelPath();

        List<SearchPathInfo> searchPaths =
                determineSearchPaths(config, parsedPath, workspaceHint, relDirPath);

        String primaryWorkspaceRoot =
                searchPaths.isEmpty() ? null : searchPaths.get(0).workspaceRoot;
        boolean resolvedToNonPrimary =
                searchPaths.isEmpty()
                        || (primaryWorkspaceRoot != null
                                && !arePathsEqual(primaryWorkspaceRoot, config.getCwd()));
        TelemetryService.WorkspaceContext workspaceContext =
                new TelemetryService.WorkspaceContext(
                        workspaceHint != null && !workspaceHint.isEmpty(),
                        resolvedToNonPrimary,
                        workspaceHint != null && !workspaceHint.isEmpty()
                                ? "hint"
                                : searchPaths.size() > 1 ? "path_detection" : "primary_fallback");

        if (config.getWorkspaceManager() != null
                && config.getServices() != null
                && config.getServices().getTelemetryService() != null) {
            String resolutionType =
                    workspaceHint != null && !workspaceHint.isEmpty()
                            ? "hint_provided"
                            : searchPaths.size() > 1
                                    ? "cross_workspace_search"
                                    : "fallback_to_primary";
            config.getServices()
                    .getTelemetryService()
                    .captureWorkspacePathResolved(
                            config.getUlid(),
                            "SearchFilesToolHandler",
                            resolutionType,
                            workspaceHint != null && !workspaceHint.isEmpty()
                                    ? "workspace_name"
                                    : null,
                            !searchPaths.isEmpty(), // resolution success = found paths to search
                            null, // TODO: could calculate primary workspace index
                            true);
        }

        long searchStartTime = System.nanoTime() / 1_000_000;
        List<SearchResult> searchResults = new ArrayList<>();
        for (SearchPathInfo pathInfo : searchPaths) {
            SearchResult result =
                    executeSearch(
                            config,
                            pathInfo.absolutePath,
                            pathInfo.workspaceName,
                            pathInfo.workspaceRoot,
                            regex,
                            filePattern);
            searchResults.add(result);
        }
        long searchDurationMs = (System.nanoTime() / 1_000_000) - searchStartTime;

        String results = formatSearchResults(config, searchResults, searchPaths);

        if (config.getWorkspaceManager() != null
                && config.getServices() != null
                && config.getServices().getTelemetryService() != null) {
            String searchType =
                    workspaceHint != null && !workspaceHint.isEmpty()
                            ? "targeted"
                            : searchPaths.size() > 1 ? "cross_workspace" : "primary_only";
            boolean resultsFound =
                    searchResults.stream().anyMatch(result -> result.resultCount > 0);

            config.getServices()
                    .getTelemetryService()
                    .captureWorkspaceSearchPattern(
                            config.getUlid(),
                            searchType,
                            searchPaths.size(),
                            workspaceHint != null && !workspaceHint.isEmpty(),
                            resultsFound,
                            searchDurationMs);
        }

        Map<String, Object> messageMap = new HashMap<>();
        messageMap.put("tool", "searchFiles");
        messageMap.put("path", HandlerUtils.getReadablePath(config.getCwd(), relDirPath));
        messageMap.put("content", results);
        messageMap.put("regex", regex);
        messageMap.put("filePattern", filePattern);
        messageMap.put(
                "operationIsLocatedInWorkspace",
                String.valueOf(HandlerUtils.isLocatedInWorkspace(parsedPath, config)));
        String message = JsonUtils.toJsonString(messageMap);

        Boolean approve =
                config.getCallbacks().shouldAutoApproveToolWithPath(block.getName(), relDirPath);
        if (Boolean.TRUE.equals(approve)) {
            config.getCallbacks()
                    .say(ClineSay.TOOL, message, null, null, false, ClineMessageFormat.JSON);
            if (!config.isYoloModeToggled()) {
                config.getTaskState()
                        .setConsecutiveAutoApprovedRequestsCount(
                                config.getTaskState().getConsecutiveAutoApprovedRequestsCount()
                                        + 1);
            }

            if (config.getServices() != null
                    && config.getServices().getTelemetryService() != null) {
                String modelId =
                        config.getApi() != null && config.getApi().getModel() != null
                                ? config.getApi().getModel().getId()
                                : "unknown";
                config.getServices()
                        .getTelemetryService()
                        .captureToolUsage(
                                config.getUlid(),
                                block.getName(),
                                modelId,
                                true,
                                true,
                                workspaceContext);
            }

            return HandlerUtils.createTextBlocks(results);
        } else {
            String notificationMessage = "Cline wants to search files for " + regex;
            TaskUtils.showNotificationForApprovalIfAutoApprovalEnabled(
                    notificationMessage,
                    config.getAutoApprovalSettings() != null
                            && config.getAutoApprovalSettings().isEnabled(),
                    config.getAutoApprovalSettings() != null
                            && config.getAutoApprovalSettings().isEnableNotifications(),
                    (subtitle, msg) -> {});

            Boolean didApprove =
                    ToolResultUtils.askApprovalAndPushFeedback(
                            ClineAsk.TOOL, message, config, ClineMessageFormat.JSON);
            if (!didApprove) {
                if (config.getServices() != null
                        && config.getServices().getTelemetryService() != null) {
                    String modelId =
                            config.getApi() != null && config.getApi().getModel() != null
                                    ? config.getApi().getModel().getId()
                                    : "unknown";
                    config.getServices()
                            .getTelemetryService()
                            .captureToolUsage(
                                    config.getUlid(),
                                    block.getName(),
                                    modelId,
                                    false,
                                    false,
                                    workspaceContext);
                }
                return HandlerUtils.createTextBlocks(formatResponse.toolDenied());
            }

            if (config.getServices() != null
                    && config.getServices().getTelemetryService() != null) {
                String modelId =
                        config.getApi() != null && config.getApi().getModel() != null
                                ? config.getApi().getModel().getId()
                                : "unknown";
                config.getServices()
                        .getTelemetryService()
                        .captureToolUsage(
                                config.getUlid(),
                                block.getName(),
                                modelId,
                                false,
                                true,
                                workspaceContext);
            }

            return HandlerUtils.createTextBlocks(results);
        }
    }

    private static class SearchPathInfo {
        String absolutePath;
        String workspaceName;
        String workspaceRoot;

        SearchPathInfo(String absolutePath, String workspaceName, String workspaceRoot) {
            this.absolutePath = absolutePath;
            this.workspaceName = workspaceName;
            this.workspaceRoot = workspaceRoot;
        }
    }

    private static class SearchResult {
        String workspaceName;
        String workspaceResults;
        int resultCount;
        boolean success;

        SearchResult(
                String workspaceName, String workspaceResults, int resultCount, boolean success) {
            this.workspaceName = workspaceName;
            this.workspaceResults = workspaceResults;
            this.resultCount = resultCount;
            this.success = success;
        }
    }
}
