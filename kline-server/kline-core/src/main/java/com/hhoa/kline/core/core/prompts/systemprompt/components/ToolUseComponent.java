package com.hhoa.kline.core.core.prompts.systemprompt.components;

import com.hhoa.kline.core.core.prompts.systemprompt.PromptVariant;
import com.hhoa.kline.core.core.prompts.systemprompt.SystemPromptComponent;
import com.hhoa.kline.core.core.prompts.systemprompt.SystemPromptContext;
import com.hhoa.kline.core.core.prompts.systemprompt.SystemPromptSection;
import com.hhoa.kline.core.core.prompts.systemprompt.registry.PromptBuilder;
import com.hhoa.kline.core.core.prompts.systemprompt.templates.TemplateEngine;
import com.hhoa.kline.core.core.shared.proto.cline.Viewport;
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
    private final PromptBuilder promptBuilder;

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
            </read_file>

            Or to read a specific range of lines:

            <read_file>
            <path>src/main.js</path>
            <start_line>0</start_line>
            <end_line>250</end_line>
            </read_file>

            Always adhere to this format for the tool use to ensure proper parsing and execution.
            """;

    private static final String FOCUS_CHAIN_FORMATTING_TEMPLATE = "";

    private static final String TOOL_USE_EXAMPLES_TEMPLATE_TEXT =
            """
            # Tool Use Examples

            ## Example 1: Requesting to execute a command

            <execute_command>
            <command>npm run dev</command>
            <requires_approval>false</requires_approval>
            </execute_command>

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
            </write_to_file>

            ## Example 3: Updating the todo list

            <TodoWrite>
            <todos>
            [
              {"content":"Inspect project structure","status":"completed","activeForm":"Inspecting project structure"},
              {"content":"Implement requested change","status":"in_progress","activeForm":"Implementing requested change"},
              {"content":"Run verification","status":"pending","activeForm":"Running verification"}
            ]
            </todos>
            </TodoWrite>

            ## Example 4: Creating a new task

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

            ## Example 5: Requesting to make targeted edits to a file

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
            </replace_in_file>


            ## Example 6: Requesting to use an MCP tool

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

            ## Example 7: Another example of using an MCP tool (where the server name is a unique identifier such as a URL)

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

    private static final String FOCUS_CHAIN_EXAMPLE_BASH = "";

    private static final String FOCUS_CHAIN_EXAMPLE_NEW_FILE = "";

    private static final String FOCUS_CHAIN_EXAMPLE_EDIT = "";

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
            "Use the TodoWrite tool to update the structured todo list when task progress changes.";
    private static final String FOCUS_CHAIN_ATTEMPT =
            "Before attempting completion, make sure all tracked TodoWrite items are completed.";
    private static final String FOCUS_CHAIN_USAGE = "";
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
        toolSpecs.addAll(promptBuilder.getToolsPrompts(variant, context));
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
