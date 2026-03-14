package com.hhoa.kline.core.core.workspace.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * WorkspacePathParser - 用于解析带工作区前缀的路径的工具类
 *
 * <p>此工具使用 @workspace:path 语法从路径中提取工作区提示。 这允许工具在多根环境中定位特定工作区。
 *
 * <p>示例： "@frontend:src/index.ts" -> { workspaceHint: "frontend", relPath: "src/index.ts" }
 * "@backend:package.json" -> { workspaceHint: "backend", relPath: "package.json" } "src/index.ts"
 * -> { workspaceHint: null, relPath: "src/index.ts" } "@my-app:src/components/Button.tsx" -> {
 * workspaceHint: "my-app", relPath: "src/components/Button.tsx" }
 */
public class WorkspacePathParser {

    private static final Pattern WORKSPACE_PATTERN = Pattern.compile("^@([^:]+):(.*)$");

    private static final Pattern HAS_HINT_PATTERN = Pattern.compile("^@[^:]+:");

    /**
     * 解析可能包含工作区提示前缀的路径
     *
     * @param value 可能包含 @workspace: 前缀的输入路径
     * @return 带有可选工作区提示和相对路径的解析结果
     */
    public static ParsedWorkspacePath parseWorkspaceInlinePath(String value) {
        if (value == null || value.isEmpty()) {
            return new ParsedWorkspacePath(null, value != null ? value : "");
        }

        Matcher matcher = WORKSPACE_PATTERN.matcher(value);

        if (matcher.matches()) {
            String workspaceHint = matcher.group(1).trim();
            String relPath = matcher.group(2).trim();
            return new ParsedWorkspacePath(workspaceHint, relPath);
        }

        return new ParsedWorkspacePath(null, value);
    }

    /**
     * 检查路径是否包含工作区提示
     *
     * @param value 要检查的路径
     * @return 如果路径包含工作区提示则返回 true
     */
    public static boolean hasWorkspaceHint(String value) {
        if (value == null || value.isEmpty()) {
            return false;
        }
        return HAS_HINT_PATTERN.matcher(value).find();
    }

    /**
     * 向路径添加工作区提示
     *
     * @param workspaceName 要添加为提示的工作区名称
     * @param path 相对路径
     * @return 带有工作区提示前缀的路径
     */
    public static String addWorkspaceHint(String workspaceName, String path) {
        ParsedWorkspacePath parsed = parseWorkspaceInlinePath(path);
        return "@" + workspaceName + ":" + parsed.getRelPath();
    }

    /**
     * 从路径中删除工作区提示（如果存在）
     *
     * @param value 可能包含工作区提示的路径
     * @return 不带工作区提示的路径
     */
    public static String removeWorkspaceHint(String value) {
        ParsedWorkspacePath parsed = parseWorkspaceInlinePath(value);
        return parsed.getRelPath();
    }

    /**
     * 解析可能包含工作区提示的多个路径
     *
     * @param paths 可能包含工作区提示的路径数组
     * @return 解析结果列表
     */
    public static List<ParsedWorkspacePath> parseMultipleWorkspacePaths(List<String> paths) {
        List<ParsedWorkspacePath> results = new ArrayList<>();
        for (String path : paths) {
            results.add(parseWorkspaceInlinePath(path));
        }
        return results;
    }
}
