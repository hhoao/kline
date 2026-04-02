package com.hhoa.kline.core.core.context.instructions.userinstructions;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hhoa.kline.core.core.shared.ClineAsk;
import com.hhoa.kline.core.core.shared.ClineMessageType;
import com.hhoa.kline.core.core.shared.ClineSay;
import com.hhoa.kline.core.core.task.ClineMessage;
import com.hhoa.kline.core.core.task.MessageStateHandler;
import com.hhoa.kline.core.core.workspace.WorkspaceRootManager;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.Builder;
import lombok.Getter;

/**
 * 构建条件规则评估上下文。
 *
 * <p>对齐 Cline 的 RuleContextBuilder，收集当前请求上下文中的路径证据，用于 frontmatter
 * `paths:` 条件激活。
 */
public final class RuleContextBuilder
{
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public static final int MAX_RULE_PATH_CANDIDATES = 100;

    private static final Pattern APPLY_PATCH_FILE_HEADER =
            Pattern.compile("^\\*\\*\\* (?:Add|Update|Delete) File: (.+?)(?:\\R|$)", Pattern.MULTILINE);

    private RuleContextBuilder()
    {
    }

    @Getter
    @Builder
    public static class RuleContextBuilderDeps
    {
        private String cwd;
        private MessageStateHandler messageStateHandler;
        private WorkspaceRootManager workspaceManager;
        @Builder.Default
        private List<String> openTabPaths = Collections.emptyList();
        @Builder.Default
        private List<String> visibleTabPaths = Collections.emptyList();
    }

    public static RuleConditionals.RuleEvaluationContext buildEvaluationContext(
            RuleContextBuilderDeps deps)
    {
        return new RuleConditionals.RuleEvaluationContext(getRulePathContext(deps));
    }

    static List<String> getRulePathContext(RuleContextBuilderDeps deps)
    {
        if (deps == null)
        {
            return Collections.emptyList();
        }

        List<ClineMessage> clineMessages =
                deps.getMessageStateHandler() != null
                        ? deps.getMessageStateHandler().getClineMessages()
                        : Collections.emptyList();
        List<String> candidates = new ArrayList<>();

        collectLatestUserMessagePaths(clineMessages, candidates);
        collectEditorTabPaths(deps, candidates);
        collectCompletedToolPaths(clineMessages, candidates);
        collectPendingToolPaths(clineMessages, candidates);

        return normalizeDedupeAndCap(candidates);
    }

    static List<String> extractPathsFromApplyPatch(String input)
    {
        if (input == null || input.isBlank())
        {
            return Collections.emptyList();
        }

        List<String> paths = new ArrayList<>();
        Matcher matcher = APPLY_PATCH_FILE_HEADER.matcher(input);
        while (matcher.find())
        {
            String filePath = matcher.group(1);
            if (filePath != null && !filePath.isBlank())
            {
                paths.add(filePath.trim());
            }
        }
        return paths;
    }

    private static void collectLatestUserMessagePaths(
            List<ClineMessage> clineMessages, List<String> candidates)
    {
        for (int i = clineMessages.size() - 1; i >= 0; i--)
        {
            ClineMessage message = clineMessages.get(i);
            if (message == null
                    || !ClineMessageType.SAY.equals(message.getType())
                    || message.getText() == null)
            {
                continue;
            }

            if (ClineSay.USER_FEEDBACK.equals(message.getSay())
                    || ClineSay.TASK.equals(message.getSay()))
            {
                candidates.addAll(RuleConditionals.extractPathLikeStrings(message.getText()));
                return;
            }
        }
    }

    private static void collectEditorTabPaths(
            RuleContextBuilderDeps deps, List<String> candidates)
    {
        List<String> workspaceRoots = new ArrayList<>();
        if (deps.getWorkspaceManager() != null && deps.getWorkspaceManager().getRoots() != null)
        {
            deps.getWorkspaceManager().getRoots().forEach(root -> workspaceRoots.add(root.getPath()));
        }

        if (workspaceRoots.isEmpty() && deps.getCwd() != null)
        {
            workspaceRoots.add(deps.getCwd());
        }

        addTabPaths(deps.getVisibleTabPaths(), workspaceRoots, candidates);
        addTabPaths(deps.getOpenTabPaths(), workspaceRoots, candidates);
    }

    private static void addTabPaths(
            List<String> rawPaths, List<String> workspaceRoots, List<String> candidates)
    {
        if (rawPaths == null || rawPaths.isEmpty())
        {
            return;
        }

        for (String absolutePath : rawPaths)
        {
            if (absolutePath == null || absolutePath.isBlank())
            {
                continue;
            }

            for (String workspaceRoot : workspaceRoots)
            {
                String relative =
                        RuleConditionals.toWorkspaceRelativePosixPath(absolutePath, workspaceRoot);
                if (relative != null)
                {
                    candidates.add(relative);
                    break;
                }
            }
        }
    }

    private static void collectCompletedToolPaths(
            List<ClineMessage> clineMessages, List<String> candidates)
    {
        for (ClineMessage message : clineMessages)
        {
            if (message == null
                    || !ClineMessageType.SAY.equals(message.getType())
                    || !ClineSay.TOOL.equals(message.getSay())
                    || message.getText() == null)
            {
                continue;
            }

            Map<String, Object> toolPayload = parseToolPayload(message.getText());
            if (toolPayload == null)
            {
                continue;
            }

            String toolName = stringValue(toolPayload.get("tool"));
            String toolPath = stringValue(toolPayload.get("path"));
            if (("editedExistingFile".equals(toolName)
                            || "newFileCreated".equals(toolName)
                            || "fileDeleted".equals(toolName))
                    && toolPath != null)
            {
                candidates.add(toolPath);
            }
        }
    }

    private static void collectPendingToolPaths(
            List<ClineMessage> clineMessages, List<String> candidates)
    {
        for (ClineMessage message : clineMessages)
        {
            if (message == null
                    || !ClineMessageType.ASK.equals(message.getType())
                    || !ClineAsk.TOOL.equals(message.getAsk())
                    || message.getText() == null)
            {
                continue;
            }

            Map<String, Object> toolPayload = parseToolPayload(message.getText());
            if (toolPayload == null)
            {
                continue;
            }

            String toolPath = stringValue(toolPayload.get("path"));
            if (toolPath == null)
            {
                toolPath = stringValue(toolPayload.get("absolutePath"));
            }
            if (toolPath != null)
            {
                candidates.add(toolPath);
            }

            String toolName = stringValue(toolPayload.get("tool"));
            if ("apply_patch".equals(toolName) || "applyPatch".equals(toolName))
            {
                String patchInput = stringValue(toolPayload.get("input"));
                if (patchInput == null)
                {
                    patchInput = stringValue(toolPayload.get("content"));
                }
                candidates.addAll(extractPathsFromApplyPatch(patchInput));
            }
        }
    }

    private static Map<String, Object> parseToolPayload(String text)
    {
        try
        {
            return OBJECT_MAPPER.readValue(text, new TypeReference<Map<String, Object>>() {});
        }
        catch (Exception ignored)
        {
            return null;
        }
    }

    private static String stringValue(Object value)
    {
        if (value == null)
        {
            return null;
        }
        String result = String.valueOf(value).trim();
        return result.isEmpty() ? null : result;
    }

    private static List<String> normalizeDedupeAndCap(List<String> candidates)
    {
        LinkedHashSet<String> deduped = new LinkedHashSet<>();

        for (String candidate : candidates)
        {
            if (candidate == null || candidate.isBlank())
            {
                continue;
            }

            String normalized = candidate.replace('\\', '/');
            while (normalized.startsWith("/"))
            {
                normalized = normalized.substring(1);
            }
            if (normalized.isBlank() || "/".equals(normalized))
            {
                continue;
            }

            deduped.add(normalized);
            if (deduped.size() >= MAX_RULE_PATH_CANDIDATES)
            {
                break;
            }
        }

        List<String> result = new ArrayList<>(deduped);
        Collections.sort(result);
        return result;
    }
}
