package com.hhoa.kline.core.core.prompts.systemprompt.components;

import com.hhoa.kline.core.core.prompts.systemprompt.PromptVariant;
import com.hhoa.kline.core.core.prompts.systemprompt.SystemPromptComponent;
import com.hhoa.kline.core.core.prompts.systemprompt.SystemPromptContext;
import com.hhoa.kline.core.core.prompts.systemprompt.SystemPromptSection;
import com.hhoa.kline.core.core.prompts.systemprompt.registry.PromptBuilder;
import com.hhoa.kline.core.core.prompts.systemprompt.templates.TemplateEngine;
import com.hhoa.kline.core.core.shared.proto.cline.Viewport;
import com.hhoa.kline.core.core.tools.ToolSchema;
import com.hhoa.kline.core.core.tools.ToolSpec;
import java.util.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 工具使用组件
 *
 * @author hhoa
 */
@Slf4j
@RequiredArgsConstructor
public class ToolUseComponent implements SystemPromptComponent {

    private final TemplateEngine templateEngine;

    private static final String TOOL_USE_TEMPLATE_TEXT =
            """
            TOOL USE

            You have access to a set of tools that are executed upon the user's approval. You can use one tool per message, and will receive the result of that tool use in the user's response. You use tools step-by-step to accomplish a given task, with each tool use informed by the result of the previous tool use.

            {{TOOL_USE_FORMATTING_SECTION}}

            {{TOOLS_SECTION}}

            {{TOOL_USE_EXAMPLES_SECTION}}

            {{TOOL_USE_GUIDELINES_SECTION}}
            """;

    private static final String TOOL_USE_FORMATTING_TEMPLATE_TEXT =
            """
            # Tool Use Formatting

            Tool use is formatted using XML-style tags. The tool name is enclosed in opening and closing tags, and each parameter is similarly enclosed within its own set of tags. Here's the structure:

            <tool_name>
            <parameter1_name>value1</parameter1_name>
            <parameter2_name>value2</parameter2_name>
            ...
            </tool_name>

            For example:

            <read_file>
            <path>src/main.js</path>
            {{FOCUS_CHAIN_FORMATTING}}</read_file>

            Or to read a specific range of lines:

            <read_file>
            <path>src/main.js</path>
            <start_line>0</start_line>
            <end_line>250</end_line>
            {{FOCUS_CHAIN_FORMATTING}}</read_file>

            Always adhere to this format for the tool use to ensure proper parsing and execution.
            """;

    private static final String FOCUS_CHAIN_FORMATTING_TEMPLATE =
            """
            <task_progress>
            Checklist here (optional)
            </task_progress>
            """;

    private static final String TOOL_USE_EXAMPLES_TEMPLATE_TEXT =
            """
            # Tool Use Examples

            ## Example 1: Requesting to execute a command

            <execute_command>
            <command>npm run dev</command>
            <requires_approval>false</requires_approval>
            {{FOCUS_CHAIN_EXAMPLE_BASH}}</execute_command>

            ## Example 2: Requesting to create a new file

            <write_to_file>
            <path>src/frontend-config.json</path>
            <content>
            {
              "apiEndpoint": "https://api.example.com",
              "theme": {
                "primaryColor": "#007bff",
                "secondaryColor": "#6c757d",
                "fontFamily": "Arial, sans-serif"
              },
              "features": {
                "darkMode": true,
                "notifications": true,
                "analytics": false
              },
              "version": "1.0.0"
            }
            </content>
            {{FOCUS_CHAIN_EXAMPLE_NEW_FILE}}</write_to_file>

            ## Example 3: Creating a new task

            <new_task>
            <context>
            1. Current Work:
               [Detailed description]

            2. Key Technical Concepts:
               - [Concept 1]
               - [Concept 2]
               - [...]

            3. Relevant Files and Code:
               - [File Name 1]
                  - [Summary of why this file is important]
                  - [Summary of the changes made to this file, if any]
                  - [Important Code Snippet]
               - [File Name 2]
                  - [Important Code Snippet]
               - [...]

            4. Problem Solving:
               [Detailed description]

            5. Pending Tasks and Next Steps:
               - [Task 1 details & next steps]
               - [Task 2 details & next steps]
               - [...]
            </context>
            </new_task>

            ## Example 4: Requesting to make targeted edits to a file

            <replace_in_file>
            <path>src/components/App.tsx</path>
            <diff>
            ------- SEARCH
            import React from 'react';
            =======
            import React, { useState } from 'react';
            +++++++ REPLACE

            ------- SEARCH
            function handleSubmit() {
              saveData();
              setLoading(false);
            }

            =======
            +++++++ REPLACE

            ------- SEARCH
            return (
              <div>
            =======
            function handleSubmit() {
              saveData();
              setLoading(false);
            }

            return (
              <div>
            +++++++ REPLACE
            </diff>
            {{FOCUS_CHAIN_EXAMPLE_EDIT}}</replace_in_file>


            ## Example 5: Requesting to use an MCP tool

            <use_mcp_tool>
            <server_name>weather-server</server_name>
            <tool_name>get_forecast</tool_name>
            <arguments>
            {
              "city": "San Francisco",
              "days": 5
            }
            </arguments>
            </use_mcp_tool>

            ## Example 6: Another example of using an MCP tool (where the server name is a unique identifier such as a URL)

            <use_mcp_tool>
            <server_name>github.com/modelcontextprotocol/servers/tree/main/src/github</server_name>
            <tool_name>create_issue</tool_name>
            <arguments>
            {
              "owner": "octocat2",
              "repo": "hello-world",
              "title": "Found a bug",
              "body": "I'm having a problem with this.",
              "labels": ["bug", "help wanted"],
              "assignees": ["octocat"]
            }
            </arguments>
            </use_mcp_tool>
            """;

    private static final String FOCUS_CHAIN_EXAMPLE_BASH =
            """
            <task_progress>
            - [x] Set up project structure
            - [x] Install dependencies
            - [ ] Run command to start server
            - [ ] Test application
            </task_progress>
            """;

    private static final String FOCUS_CHAIN_EXAMPLE_NEW_FILE =
            """
            <task_progress>
            - [x] Set up project structure
            - [x] Install dependencies
            - [ ] Create components
            - [ ] Test application
            </task_progress>
            """;

    private static final String FOCUS_CHAIN_EXAMPLE_EDIT =
            """
            <task_progress>
            - [x] Set up project structure
            - [x] Install dependencies
            - [ ] Create components
            - [ ] Test application
            </task_progress>
            """;

    private static final String TOOL_USE_GUIDELINES_TEMPLATE_TEXT =
            """
            # Tool Use Guidelines

            1. In <thinking> tags, assess what information you already have and what information you need to proceed with the task.
            2. Choose the most appropriate tool based on the task and the tool descriptions provided. Assess if you need additional information to proceed, and which of the available tools would be most effective for gathering this information. For example using the list_files tool is more effective than running a command like `ls` in the terminal. It's critical that you think about each available tool and use the one that best fits the current step in the task.
            3. If multiple actions are needed, use one tool at a time per message to accomplish the task iteratively, with each tool use being informed by the result of the previous tool use. Do not assume the outcome of any tool use. Each step must be informed by the previous step's result.
            4. Formulate your tool use using the XML format specified for each tool.
            5. After each tool use, the user will respond with the result of that tool use. This result will provide you with the necessary information to continue your task or make further decisions. This response may include:
              - Information about whether the tool succeeded or failed, along with any reasons for failure.
              - Linter errors that may have arisen due to the changes you made, which you'll need to address.
              - New terminal output in reaction to the changes, which you may need to consider or act upon.
              - Any other relevant feedback or information related to the tool use.
            6. ALWAYS wait for user confirmation after each tool use before proceeding. Never assume the success of a tool use without explicit confirmation of the result from the user.

            It is crucial to proceed step-by-step, waiting for the user's message after each tool use before moving forward with the task. This approach allows you to:
            1. Confirm the success of each step before proceeding.
            2. Address any issues or errors that arise immediately.
            3. Adapt your approach based on new information or unexpected results.
            4. Ensure that each action builds correctly on the previous ones.

            By waiting for and carefully considering the user's response after each tool use, you can react accordingly and make informed decisions about how to proceed with the task. This iterative process helps ensure the overall success and accuracy of your work.
            """;

    private static final String TASK_PROGRESS =
            "- task_progress: (optional) A checklist showing task progress after this tool use is completed. (See 'Updating Task Progress' section for more details)";
    private static final String FOCUS_CHAIN_ATTEMPT =
            "If you were using task_progress to update the task progress, you must include the completed list in the result as well.";
    private static final String FOCUS_CHAIN_USAGE =
            """
            <task_progress>
            Checklist here (optional)
            </task_progress>
            """;
    private static final String MULTI_ROOT_HINT =
            " Use @workspace:path syntax (e.g., @frontend:src/index.ts) to specify a workspace.";

    @Override
    public String apply(PromptVariant variant, SystemPromptContext context) {
        String template = TOOL_USE_TEMPLATE_TEXT;

        if (variant.getComponentOverrides() != null
                && variant.getComponentOverrides().containsKey(SystemPromptSection.TOOL_USE)) {
            var override = variant.getComponentOverrides().get(SystemPromptSection.TOOL_USE);
            if (override.getTemplate() != null) {
                template = override.getTemplate();
            }
        }

        String toolUseFormattingSection = getToolUseFormattingSection(variant, context);
        String toolsSection = getToolsSection(variant, context);
        String toolUseExamplesSection = getToolUseExamplesSection(variant, context);
        String toolUseGuidelinesSection = getToolUseGuidelinesSection(variant, context);

        return templateEngine.resolve(
                template,
                context,
                Map.of(
                        "TOOL_USE_FORMATTING_SECTION", toolUseFormattingSection,
                        "TOOLS_SECTION", toolsSection,
                        "TOOL_USE_EXAMPLES_SECTION", toolUseExamplesSection,
                        "TOOL_USE_GUIDELINES_SECTION", toolUseGuidelinesSection,
                        "CWD",
                                context.getCwd() != null
                                        ? context.getCwd()
                                        : System.getProperty("user.dir")));
    }

    @Override
    public SystemPromptSection getSystemPromptSection() {
        return SystemPromptSection.TOOL_USE;
    }

    private String getToolUseFormattingSection(PromptVariant variant, SystemPromptContext context) {
        boolean focusChainEnabled = isFocusChainEnabled(context);
        String focusChainFormatting = focusChainEnabled ? FOCUS_CHAIN_FORMATTING_TEMPLATE : "";

        return templateEngine.resolve(
                TOOL_USE_FORMATTING_TEMPLATE_TEXT,
                context,
                Map.of("FOCUS_CHAIN_FORMATTING", focusChainFormatting));
    }

    private String getToolsSection(PromptVariant variant, SystemPromptContext context) {
        List<String> toolSpecs = new ArrayList<>();
        toolSpecs.add("# Tools");
        toolSpecs.addAll(PromptBuilder.getToolsPrompts(variant, context));
        String template = String.join("\n\n", toolSpecs);
        Map<String, String> placeholders = buildPlaceholders(context, isFocusChainEnabled(context));
        return templateEngine.resolve(template, context, placeholders);
    }

    private String getToolUseExamplesSection(PromptVariant variant, SystemPromptContext context) {
        boolean focusChainEnabled = isFocusChainEnabled(context);

        String focusChainExampleBash = focusChainEnabled ? FOCUS_CHAIN_EXAMPLE_BASH : "";
        String focusChainExampleNewFile = focusChainEnabled ? FOCUS_CHAIN_EXAMPLE_NEW_FILE : "";
        String focusChainExampleEdit = focusChainEnabled ? FOCUS_CHAIN_EXAMPLE_EDIT : "";

        return templateEngine.resolve(
                TOOL_USE_EXAMPLES_TEMPLATE_TEXT,
                context,
                Map.of(
                        "FOCUS_CHAIN_EXAMPLE_BASH", focusChainExampleBash,
                        "FOCUS_CHAIN_EXAMPLE_NEW_FILE", focusChainExampleNewFile,
                        "FOCUS_CHAIN_EXAMPLE_EDIT", focusChainExampleEdit));
    }

    private String getToolUseGuidelinesSection(PromptVariant variant, SystemPromptContext context) {
        return templateEngine.resolve(TOOL_USE_GUIDELINES_TEMPLATE_TEXT, context, Map.of());
    }

    private boolean isFocusChainEnabled(SystemPromptContext context) {
        return context.getFocusChainSettings() != null
                && Boolean.TRUE.equals(context.getFocusChainSettings().isEnabled());
    }

    /**
     * 构建工具描述
     *
     * @param spec 工具规格
     * @param context 系统提示上下文
     * @param allToolIds 所有工具 ID 列表，用于依赖检查
     * @return 工具描述字符串，如果工具应被跳过则返回 null
     */
    private String buildToolDescription(
            ToolSpec spec, SystemPromptContext context, List<String> allToolIds) {
        boolean hasDesc = spec.getDescription() != null && !spec.getDescription().isBlank();
        boolean hasParams = !ToolSchema.properties(spec.getInputSchema()).isEmpty();
        if (!hasDesc && !hasParams) {
            return null;
        }

        // 上下文要求过滤
        if (spec.getContextRequirements() != null) {
            try {
                if (!Boolean.TRUE.equals(spec.getContextRequirements().apply(context))) {
                    return null;
                }
            } catch (Exception e) {
                log.warn("Tool contextRequirements evaluation failed for {}", spec.getId(), e);
                return null;
            }
        }

        StringBuilder sb = new StringBuilder();
        sb.append("## ").append(spec.getId()).append('\n');

        List<Map.Entry<String, Map<String, Object>>> filteredParams =
                filterParameters(spec.getInputSchema(), context, allToolIds);

        List<String> descriptionLines = new ArrayList<>();
        String mainDescription = spec.getDescription() != null ? spec.getDescription() : "";
        descriptionLines.add("Description: " + mainDescription);

        for (var p : filteredParams) {
            Object description = p.getValue().get("description");
            if (description != null && !String.valueOf(description).isBlank()) {
                descriptionLines.add(String.valueOf(description));
            }
        }

        sb.append(String.join("\n", descriptionLines)).append('\n');

        if (filteredParams.isEmpty()) {
            sb.append("Parameters: None\n");
        } else {
            sb.append("Parameters:\n");
            for (var p : filteredParams) {
                String requiredText =
                        ToolSchema.required(spec.getInputSchema()).contains(p.getKey())
                                ? "required"
                                : "optional";
                sb.append("- ")
                        .append(p.getKey())
                        .append(": (")
                        .append(requiredText)
                        .append(") ")
                        .append(ToolSchema.instruction(p.getValue(), context))
                        .append('\n');
            }
        }

        sb.append("Usage:\n");
        String toolId = spec.getId();
        sb.append('<').append(toolId).append('>').append('\n');
        for (var p : filteredParams) {
            sb.append('<')
                    .append(p.getKey())
                    .append('>')
                    .append(ToolSchema.usage(p.getValue()))
                    .append("</")
                    .append(p.getKey())
                    .append('>')
                    .append('\n');
        }
        sb.append("</").append(toolId).append('>');

        return sb.toString();
    }

    /**
     * 过滤参数
     *
     * @param parameters 参数列表
     * @param context 系统提示上下文
     * @param allToolIds 所有工具 ID 列表
     * @return 过滤后的参数列表
     */
    private List<Map.Entry<String, Map<String, Object>>> filterParameters(
            Map<String, Object> inputSchema, SystemPromptContext context, List<String> allToolIds) {
        Map<String, Map<String, Object>> parameters = ToolSchema.properties(inputSchema);
        if (parameters.isEmpty()) {
            return List.of();
        }

        return parameters.entrySet().stream()
                .filter(entry -> ToolSchema.parameterEnabled(entry.getValue(), context, allToolIds))
                .toList();
    }

    private Map<String, String> buildPlaceholders(
            SystemPromptContext context, boolean focusChainEnabled) {
        Map<String, String> placeholders = new HashMap<>();

        if (focusChainEnabled) {
            placeholders.put("TASK_PROGRESS", TASK_PROGRESS);
            placeholders.put("FOCUS_CHAIN_ATTEMPT", FOCUS_CHAIN_ATTEMPT);
            placeholders.put("FOCUS_CHAIN_USAGE", FOCUS_CHAIN_USAGE);
        } else {
            placeholders.put("TASK_PROGRESS", "");
            placeholders.put("FOCUS_CHAIN_ATTEMPT", "");
            placeholders.put("FOCUS_CHAIN_USAGE", "");
        }

        int[] viewport = getBrowserViewport(context);
        placeholders.put("BROWSER_VIEWPORT_WIDTH", String.valueOf(viewport[0]));
        placeholders.put("BROWSER_VIEWPORT_HEIGHT", String.valueOf(viewport[1]));

        placeholders.put(
                "CWD",
                context.getCwd() != null ? context.getCwd() : System.getProperty("user.dir"));

        String multiRootHint =
                Boolean.TRUE.equals(context.getIsMultiRootEnabled()) ? MULTI_ROOT_HINT : "";
        placeholders.put("MULTI_ROOT_HINT", multiRootHint);

        return placeholders;
    }

    private int[] getBrowserViewport(SystemPromptContext context) {
        if (context.getBrowserSettings() == null
                || context.getBrowserSettings().getViewport() == null) {
            return new int[] {0, 0};
        }

        Viewport viewport = context.getBrowserSettings().getViewport();
        return new int[] {viewport.getWidth(), viewport.getHeight()};
    }
}
