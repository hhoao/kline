package com.hhoa.kline.core.core.prompts.systemprompt.components;

import com.hhoa.kline.core.core.prompts.systemprompt.PromptVariant;
import com.hhoa.kline.core.core.prompts.systemprompt.SystemPromptComponent;
import com.hhoa.kline.core.core.prompts.systemprompt.SystemPromptContext;
import com.hhoa.kline.core.core.prompts.systemprompt.SystemPromptSection;
import com.hhoa.kline.core.core.prompts.systemprompt.templates.TemplateEngine;
import java.util.Map;
import lombok.RequiredArgsConstructor;

/**
 * CLI 子代理组件
 *
 * @author hhoa
 */
@RequiredArgsConstructor
public class CliSubagentsComponent implements SystemPromptComponent {

    private final TemplateEngine templateEngine;

    @Override
    public String apply(PromptVariant variant, SystemPromptContext context) {
        if (Boolean.TRUE.equals(context.getIsCliSubagent())) {
            return null;
        }

        if (!Boolean.TRUE.equals(context.getIsSubagentsEnabledAndCliInstalled())) {
            return null;
        }

        String template = getTemplateText();

        if (variant.getComponentOverrides() != null
                && variant.getComponentOverrides().containsKey(SystemPromptSection.CLI_SUBAGENTS)) {
            var override = variant.getComponentOverrides().get(SystemPromptSection.CLI_SUBAGENTS);
            if (override.getTemplate() != null) {
                template = override.getTemplate();
            }
        }

        return templateEngine.resolve(template, context, Map.of());
    }

    @Override
    public SystemPromptSection getSystemPromptSection() {
        return SystemPromptSection.CLI_SUBAGENTS;
    }

    private String getTemplateText() {
        return """
            USING THE CLINE CLI TOOL

            The Cline CLI tool can be used to assign Cline AI agents with focused tasks. This can be used to keep you focused by delegating information-gathering and exploration to separate Cline instances. Use the Cline CLI tool to research large codebases, explore file structures, gather information from multiple files, analyze dependencies, or summarize code sections when the complete context may be too large or overwhelming.

            ## Creating Cline AI agents

            Cline AI agents may be referred to as agents, subagents, or subtasks. Requests may not specifically invoke agents, but you may invoke them directly if warranted. Unless you are specifically asked to use this tool, only create agents when it seems likely you may be exploring across 10 or more files. If users specifically ask that you use this tool, you then must use this tool. Do not use subagents for editing code or executing commands- they should only be used for reading and research to help you better answer questions or build useful context for future coding tasks. If you are performing a search via search_files or the terminal (grep etc.), and the results are long and overwhleming, it is reccomended that you switch to use Cline CLI agents to perform this task. You may perform code edits directly using the write_to_file and replace_in_file tools, and commands using the execute_command tool.

            ## Command Syntax

            You must use the following command syntax for creating Cline AI agents:

            ```bash
            cline "your prompt here"
            ```

            ## Examples of how you might use this tool

            ```bash
            # Find specific patterns
            cline "find all React components that use the useState hook and list their names"

            # Analyze code structure
            cline "analyze the authentication flow. Reverse trace through all relevant functions and methods, and provide a summary of how it works. Include file/class references in your summary."

            # Gather targeted information
            cline "list all API endpoints and their HTTP methods"

            # Summarize directories
            cline "summarize the purpose of all files in the src/services directory"

            # Research implementations
            cline "find how error handling is implemented across the application"
            ```

            ## Tips
            - Request brief, technically dense summaries over full file dumps.
            - Be specific with your instructions to get focused results.
            - Request summaries rather than full file contents. Encourage the agent to be brief, but specific and technically dense with their response.
            - If files you want to read are large or complicated, use Cline CLI agents for exploration before instead of reading these files.
            """;
    }
}
