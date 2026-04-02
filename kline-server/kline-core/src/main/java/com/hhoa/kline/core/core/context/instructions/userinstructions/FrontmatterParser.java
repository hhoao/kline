package com.hhoa.kline.core.core.context.instructions.userinstructions;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;

/**
 * YAML frontmatter 解析器。
 *
 * <p>行为是 fail-open 的：
 *
 * <ul>
 *   <li>YAML 解析失败时，返回 data={} 和 body=原始 markdown
 *   <li>没有 frontmatter 时，返回 data={} 和 body=原始 markdown
 * </ul>
 *
 * <p>安全：使用 SnakeYAML 的安全加载模式，防止通过 YAML 标签进行 RCE。
 */
@Slf4j
public class FrontmatterParser {

    private static final Pattern FRONTMATTER_REGEX =
            Pattern.compile("^---\\r?\\n([\\s\\S]*?)\\r?\\n---\\r?\\n?([\\s\\S]*)$");

    /** Frontmatter 解析结果 */
    @Getter
    @Builder
    public static class FrontmatterParseResult {
        /** 解析出的 frontmatter 数据 */
        @Builder.Default private final Map<String, Object> data = new HashMap<>();

        /** 去除 frontmatter 后的 markdown 正文 */
        private final String body;

        /** 输入是否包含 frontmatter 块 */
        private final boolean hadFrontmatter;

        /** YAML 解析错误（仅当检测到 frontmatter 但解析失败时存在） */
        private final String parseError;
    }

    /**
     * 从 markdown 内容解析 YAML frontmatter
     *
     * @param markdown 原始 markdown 内容
     * @return 解析结果
     */
    @SuppressWarnings("unchecked")
    public static FrontmatterParseResult parseYamlFrontmatter(String markdown) {
        if (markdown == null || markdown.isEmpty()) {
            return FrontmatterParseResult.builder().body("").hadFrontmatter(false).build();
        }

        Matcher matcher = FRONTMATTER_REGEX.matcher(markdown);
        if (!matcher.matches()) {
            return FrontmatterParseResult.builder().body(markdown).hadFrontmatter(false).build();
        }

        String yamlContent = matcher.group(1);
        String body = matcher.group(2);

        try {
            // 使用安全的 YAML 加载选项
            LoaderOptions options = new LoaderOptions();
            options.setAllowDuplicateKeys(false);
            Yaml yaml = new Yaml(options);
            Object loaded = yaml.load(yamlContent);

            Map<String, Object> data;
            if (loaded instanceof Map) {
                data = (Map<String, Object>) loaded;
            } else {
                data = new HashMap<>();
            }

            return FrontmatterParseResult.builder()
                    .data(data)
                    .body(body)
                    .hadFrontmatter(true)
                    .build();
        } catch (Exception e) {
            return FrontmatterParseResult.builder()
                    .body(markdown)
                    .hadFrontmatter(true)
                    .parseError(e.getMessage())
                    .build();
        }
    }
}
