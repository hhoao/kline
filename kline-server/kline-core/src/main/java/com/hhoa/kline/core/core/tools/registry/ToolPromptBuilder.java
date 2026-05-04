package com.hhoa.kline.core.core.tools.registry;

import com.hhoa.kline.core.core.prompts.systemprompt.SystemPromptContext;
import com.hhoa.kline.core.core.tools.ToolSpec;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/** 构建单个工具在 system prompt 中的文本说明。 */
public final class ToolPromptBuilder {
    private ToolPromptBuilder() {}

    public static String render(
            ToolSpec config, List<String> registry, SystemPromptContext context) {
        if (config.getPrompt() == null || config.getPrompt().isBlank()) {
            return "";
        }

        String displayName =
                config.getName() != null && !config.getName().isBlank()
                        ? config.getName()
                        : config.getName();
        String title = "## " + displayName;
        String prompt = config.getPrompt().trim();

        List<String> sections = new ArrayList<>();
        sections.add(title);
        sections.add(prompt);

        return sections.stream()
                .filter(s -> s != null && !s.isBlank())
                .collect(Collectors.joining("\n"));
    }
}
