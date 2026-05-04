package com.hhoa.kline.core.core.prompts.commands.deepplanning;

import static com.hhoa.kline.core.core.prompts.systemprompt.ModelFamilyMatchers.isGemini3ModelFamily;

/**
 * Gemini 3 variant for deep-planning prompt. Uses 5-step process with separate read and terminal
 * investigation phases. Template is dynamically generated based on focus chain and native tool call
 * settings. 对应 TS deep-planning/variants/gemini3.ts
 *
 * @author hhoa
 */
public final class Gemini3DeepPlanningVariant {

    private Gemini3DeepPlanningVariant() {}

    public static DeepPlanningVariant create() {
        return DeepPlanningVariant.builder()
                .id("gemini-3")
                .description("Deep-planning variant optimized for Gemini 3 models")
                .family("gemini-3")
                .version(1)
                .matcher(
                        context -> {
                            if (context == null
                                    || context.getProviderInfo() == null
                                    || context.getProviderInfo().getModel() == null) {
                                return false;
                            }
                            String modelId = context.getProviderInfo().getModel().getId();
                            return modelId != null && isGemini3ModelFamily(modelId);
                        })
                .template("") // Template is dynamically generated
                .build();
    }

    /**
     * Generates the deep-planning template dynamically.
     *
     * @param focusChainEnabled Whether focus chain (TodoWrite) is enabled
     * @param enableNativeToolCalls Whether native tool calling is enabled
     */
    public static String generateTemplate(
            boolean focusChainEnabled, boolean enableNativeToolCalls) {
        String focusChainTracker =
                focusChainEnabled
                        ? "You should track these five steps with TodoWrite, and update statuses only when steps are completed.\n"
                        : "";

        String implementationOrderExtra =
                focusChainEnabled
                        ? "A TodoWrite list of steps that will need to be completed during the implementation\n"
                        : "";

        String step5TaskProgress =
                focusChainEnabled
                        ? "The task must include a TodoWrite list that breaks down the implementation into trackable steps."
                        : "";

        String taskProgressFormat =
                focusChainEnabled
                        ? """
                **TodoWrite Task Progress Format:**
                You absolutely MUST include the TodoWrite contents in context when creating the new task. When providing it, do not wrap it in XML tags; provide it like this:

                TodoWrite Items:
                - content: Step 1: Brief description of first implementation step; status: pending; activeForm: Working on first implementation step
                - content: Step 2: Brief description of second implementation step; status: pending; activeForm: Working on second implementation step
                - content: Step N: Brief description of subsequent/final implementation step(s); status: pending; activeForm: Working on subsequent/final implementation step(s)

                **Markdown Implementation Plan Path:**
                You also MUST include the path to the markdown file you have created in your new task prompt. You should do this as follows:
                  Refer to @path/to/file/markdown.md for a complete breakdown of the task requirements and steps. You should periodically read this file again."""
                        : "";

        String newTaskInstructions =
                enableNativeToolCalls
                        ? """
                **new_task Tool Definition:**

                When you are ready to create the implementation task, you must call the new_task tool with the following structure:

                {
                  "name": "new_task",
                  "arguments": {
                    "context": "Your detailed context here following the 5-point structure..."
                  }
                }

                The context parameter should include all five sections as described above.
                """
                        : """
                **new_task Tool Definition:**

                When you are ready to create the implementation task, you must call the new_task tool with the following structure:

                <new_task>
                <context>Your detailed context here following the 5-point structure...</context>
                </new_task>

                The context parameter should include all five sections as described above.
                """;

        return "<explicit_instructions type=\"deep-planning\">\n"
                + "Your task is to create a comprehensive implementation plan before writing any code. "
                + "This process has five distinct steps that must be completed in order:\n"
                + "1. Silent Read Investigation\n"
                + "2. Silent Terminal Investigation\n"
                + "3. Discussion and Questions\n"
                + "4. Create Implementation Plan Document\n"
                + "5. Create new_task for Implementation Phase\n\n"
                + focusChainTracker
                + "Your behavior should be methodical and thorough - take time to understand the codebase "
                + "completely before making any recommendations. The quality of your investigation and use of "
                + "targeted reads/searches directly impacts the success of the implementation.\n\n"
                + "<IMPORTANT>\n"
                + "Execute only exploration and plan generation steps until explicitly instructed by the user to proceed with coding.\n"
                + "You must thoroughly understand the existing codebase before proposing any changes.\n"
                + "Perform your research without commentary or narration. Execute commands and read files "
                + "without explaining what you're about to do. Only speak up if you have specific questions for the user.\n"
                + "</IMPORTANT>\n\n"
                + "## STEP 1: Silent Read Investigation\n\n"
                + "### Required Research Activities\n"
                + "You MUST first use the read_file tool to examine several source files, configuration files, and "
                + "documentation to better inform subsequent research steps. You should only use read_file to prepare "
                + "for more granular searching. Use this step to get the big picture, then you will use the next step "
                + "for granular details by searching using terminal commands. Use this tool to determine the language(s) "
                + "used in the codebase, and to identify the domain(s) relevant to the user's request.\n\n\n"
                + "## STEP 2: Silent Terminal Investigation\n\n"
                + "### Required Research Activities\n"
                + "You MUST use terminal commands to gather information about the codebase structure and patterns "
                + "relevant to the user's request.\n"
                + "You will tailor these commands to explore and identify key functions, classes, methods, types, "
                + "and variables that are directly, or indirectly related to the task.\n"
                + "These commands must be crafted to not produce exceptionally long or verbose search results. "
                + "For example, you should exclude dependency folders such as node_modules, venv or php vendor, etc. "
                + "Carefully consider the scope of search patterns. Use the results of your read_file tool calls to "
                + "tailor the commands for balanced search result lengths. If a command returns no results, you may "
                + "loosen the search patterns or scope slightly. If a command returns hundreds or thousands of results, "
                + "you should adjust subsequent commands to be more targeted.\n"
                + "Execute these commands to build your understanding. Adjust subsequent commands based on the output "
                + "you have received from each previous command, informing the scope and direction of your search.\n"
                + "You should only execute one command at a time for the first 1-3 commands. Do not chain search commands "
                + "until you have executed and interpreted the results of several search commands, then use the context "
                + "you have gathered to inform more complex chained commands.\n\n"
                + "Here are some example commands, remember to adjust them as instructed previously:\n"
                + DeepPlanningTemplates.BASH_INVESTIGATION_COMMANDS
                + "\n\n"
                + "## STEP 3: Discussion and Questions\n\n"
                + "Ask the user brief, targeted questions that will influence your implementation plan. "
                + "Keep your questions concise and conversational. Ask only essential questions needed to create an accurate plan.\n\n"
                + "**Ask questions only when necessary for:**\n"
                + "- Clarifying ambiguous requirements or unclear specifications\n"
                + "- Choosing between multiple equally valid implementation approaches that have significant trade-offs\n"
                + "- Confirming non-trivial assumptions about existing system behavior or constraints\n"
                + "- Understanding preferences for specific technical decisions that will affect the final implementation's behavior or code maintainability\n\n"
                + "Your questions should be direct and specific. Avoid long explanations or multiple questions in one response. "
                + "Only ask one question at a time. You may ask several questions if required and within scope of the task.\n\n"
                + "## STEP 4: Create Implementation Plan Document\n\n"
                + "Once you have obtained sufficient context to understand all code modifications that will be required, "
                + "create a structured markdown document containing your complete implementation plan. "
                + "The document must follow this exact format with clearly marked sections:\n\n"
                + "### Document Structure Requirements\n\n"
                + "Your implementation plan must be saved as implementation_plan.md, and *must* be structured as follows:\n\n"
                + "<example_implementation_plan>\n"
                + DeepPlanningTemplates.PLAN_DOCUMENT_STRUCTURE
                + implementationOrderExtra
                + "\n"
                + "</example_implementation_plan>\n\n"
                + "## STEP 5: Create Implementation new_task\n\n"
                + "Use the new_task command to create a task for implementing the plan. "
                + step5TaskProgress
                + "\n\n"
                + "### Task Creation Requirements\n\n"
                + "<IMPORTANT>\n"
                + "**Standalone Product:**\n"
                + "Your new task should be self-contained and reference the plan document rather than "
                + "requiring additional codebase investigation. Include these specific instructions in the task description:\n\n"
                + taskProgressFormat
                + "\n"
                + "</IMPORTANT>\n\n"
                + newTaskInstructions
                + "### Mode Switching\n\n"
                + "<IMPORTANT>\n"
                + "When creating the new task, request a switch to \"act mode\" if you are currently in \"plan mode\". "
                + "This ensures the implementation agent operates in execution mode rather than planning mode.\n"
                + "</IMPORTANT>\n\n"
                + DeepPlanningTemplates.QUALITY_STANDARDS_5_STEP;
    }
}
