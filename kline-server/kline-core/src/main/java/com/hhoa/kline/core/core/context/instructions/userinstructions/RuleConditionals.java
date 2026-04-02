package com.hhoa.kline.core.core.context.instructions.userinstructions;

import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Rule frontmatter 条件评估。
 *
 * <p>实现 Cline Rules YAML frontmatter 的条件 DSL。 用于决定规则是否应在给定请求上下文中激活。
 *
 * <p>注意事项：
 *
 * <ul>
 *   <li>未知的条件键被忽略（向前兼容）
 *   <li>{@code paths} 条件在任意候选路径匹配任意 glob 模式时通过
 *   <li>候选路径应为工作区根目录相对的 POSIX 路径
 * </ul>
 */
public class RuleConditionals {

    /** 规则评估上下文 */
    public static class RuleEvaluationContext {
        private final List<String> paths;

        public RuleEvaluationContext(List<String> paths) {
            this.paths = paths != null ? paths : Collections.emptyList();
        }

        public List<String> getPaths() {
            return paths;
        }
    }

    /** 条件评估结果 */
    public static class ConditionalResult {
        private final boolean passed;
        private final Map<String, List<String>> matchedConditions;

        public ConditionalResult(boolean passed, Map<String, List<String>> matchedConditions) {
            this.passed = passed;
            this.matchedConditions = matchedConditions;
        }

        public boolean isPassed() {
            return passed;
        }

        public Map<String, List<String>> getMatchedConditions() {
            return matchedConditions;
        }
    }

    /**
     * 评估 frontmatter 条件
     *
     * @param frontmatter 解析后的 frontmatter 数据
     * @param context 评估上下文
     * @return 评估结果
     */
    public static ConditionalResult evaluateRuleConditionals(
            Map<String, Object> frontmatter, RuleEvaluationContext context) {
        Map<String, List<String>> matchedConditions = new HashMap<>();

        for (Map.Entry<String, Object> entry : frontmatter.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            if ("paths".equals(key)) {
                PathsResult result = evaluatePathsConditional(value, context);
                if (!result.passed) {
                    return new ConditionalResult(false, Collections.emptyMap());
                }
                if (result.matched != null && !result.matched.isEmpty()) {
                    matchedConditions.put(key, result.matched);
                }
            }
            // 未知条件键被忽略（向前兼容）
        }

        return new ConditionalResult(true, matchedConditions);
    }

    /** paths 条件评估结果 */
    private static class PathsResult {
        final boolean passed;
        final List<String> matched;

        PathsResult(boolean passed, List<String> matched) {
            this.passed = passed;
            this.matched = matched;
        }
    }

    /**
     * 评估 paths 条件
     *
     * <p>策略：
     *
     * <ul>
     *   <li>paths 省略 → 通用（此评估器不会被调用）
     *   <li>paths: [] → 不匹配任何（fail-closed，用户显式禁用规则）
     *   <li>无候选路径 → 不激活路径范围的规则
     * </ul>
     */
    private static PathsResult evaluatePathsConditional(
            Object frontmatterValue, RuleEvaluationContext context) {
        // 无效类型 → 忽略条件（fail-open）
        if (!(frontmatterValue instanceof List)) {
            return new PathsResult(true, null);
        }

        List<?> rawList = (List<?>) frontmatterValue;
        List<String> patterns =
                rawList.stream()
                        .filter(v -> v instanceof String && !((String) v).trim().isEmpty())
                        .map(v -> ((String) v).trim())
                        .collect(Collectors.toList());

        if (patterns.isEmpty()) {
            return new PathsResult(false, null);
        }

        List<String> candidatePaths =
                context.getPaths().stream()
                        .map(RuleConditionals::toPosix)
                        .filter(p -> !p.isEmpty())
                        .collect(Collectors.toList());

        if (candidatePaths.isEmpty()) {
            return new PathsResult(false, null);
        }

        List<String> matchedPatterns = new ArrayList<>();
        for (String pattern : patterns) {
            PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:" + pattern);
            boolean anyMatch =
                    candidatePaths.stream()
                            .anyMatch(candidate -> matcher.matches(Path.of(candidate)));
            if (anyMatch) {
                matchedPatterns.add(pattern);
            }
        }

        return new PathsResult(
                !matchedPatterns.isEmpty(), matchedPatterns.isEmpty() ? null : matchedPatterns);
    }

    /**
     * 从用户文本中提取类似路径的字符串，用于首轮激活。 这是启发式的且保守的。
     *
     * @param text 用户输入文本
     * @return 提取的路径列表
     */
    public static List<String> extractPathLikeStrings(String text) {
        if (text == null || text.isEmpty()) {
            return Collections.emptyList();
        }

        // 去除代码块
        String withoutCodeFences = text.replaceAll("```[\\s\\S]*?```", " ");
        // 去除 URL
        String withoutUrls = withoutCodeFences.replaceAll("\\b\\w+://[^\\s]+", " ");

        // 匹配看起来像路径的 token
        Pattern tokenRegex =
                Pattern.compile(
                        "(?:^|[\\s\\(\\[\\{\\\"'`])"
                                + "((?:[A-Za-z0-9_.-]+(?:/[A-Za-z0-9_.-]+)+/?|[A-Za-z0-9_.-]+\\.[A-Za-z0-9]{1,10}))"
                                + "(?=$|[\\s\\)\\]\\}\\\"'`,.;:!?])");

        Matcher matcher = tokenRegex.matcher(withoutUrls);
        Set<String> seen = new LinkedHashSet<>();

        while (matcher.find()) {
            String candidate = matcher.group(1);
            if (candidate == null) continue;

            // 去除前导 ./
            String normalized = candidate.startsWith("./") ? candidate.substring(2) : candidate;
            if (normalized.length() > 300) continue;

            String posix = toPosix(normalized);
            if ("/".equals(posix) || posix.startsWith("/") || posix.contains("..")) {
                continue;
            }
            seen.add(posix);
        }

        return new ArrayList<>(seen);
    }

    /**
     * 将绝对文件系统路径规范化为工作区根目录相对的 POSIX 路径
     *
     * @param absPath 绝对路径
     * @param workspaceRoot 工作区根目录
     * @return 相对 POSIX 路径，如果不在工作区内则返回 null
     */
    public static String toWorkspaceRelativePosixPath(String absPath, String workspaceRoot) {
        try {
            Path relative = Path.of(workspaceRoot).relativize(Path.of(absPath));
            String rel = relative.toString();
            if (rel.startsWith("..") || Path.of(rel).isAbsolute()) {
                return null;
            }
            return toPosix(rel);
        } catch (Exception e) {
            return null;
        }
    }

    private static String toPosix(String path) {
        return path.replace('\\', '/');
    }
}
