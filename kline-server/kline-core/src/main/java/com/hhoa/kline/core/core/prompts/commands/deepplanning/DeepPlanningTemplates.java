package com.hhoa.kline.core.core.prompts.commands.deepplanning;

/**
 * Shared template fragments for deep-planning variants.
 * Contains reusable sections used across multiple variants.
 *
 * @author hhoa
 */
final class DeepPlanningTemplates {

    private DeepPlanningTemplates() {}

    static final String FOCUS_CHAIN_INTRO =
            """
            **Task Progress Parameter:**
            When creating the new task, you must include a task_progress parameter that breaks down the implementation into trackable steps. This parameter should be included inside the tool call, but not located inside of other content/argument blocks. This should follow the standard Markdown checklist format with "- [ ]" for incomplete items.""";

    static final String BASH_INVESTIGATION_COMMANDS =
            """

            # Discover project structure and file types
            find . -type f -name "*.py" -o -name "*.js" -o -name "*.ts" -o -name "*.java" -o -name "*.cpp" -o -name "*.go" | head -30 | cat

            # Find all class and function definitions
            grep -r "class\\|function\\|def\\|interface\\|struct\\|func\\|type.*struct\\|type.*interface" --include="*.py" --include="*.js" --include="*.ts" --include="*.java" --include="*.cpp" --include="*.go" . | cat

            # Analyze import patterns and dependencies
            grep -r "import\\|from\\|require\\|#include" --include="*.py" --include="*.js" --include="*.ts" --include="*.java" --include="*.cpp" . | sort | uniq | cat

            # Find dependency manifests
            find . -name "requirements*.txt" -o -name "package.json" -o -name "Cargo.toml" -o -name "pom.xml" -o -name "Gemfile" -o -name "go.mod" | xargs cat

            # Identify technical debt and TODOs
            grep -r "TODO\\|FIXME\\|XXX\\|HACK\\|NOTE" --include="*.py" --include="*.js" --include="*.ts" --include="*.java" --include="*.cpp" --include="*.go" . | cat
            """;

    static final String BASH_PLAN_NAVIGATION_COMMANDS =
            """

            # Read Overview section
            sed -n '/\\[Overview\\]/,/\\[Types\\]/p' implementation_plan.md | head -n 1 | cat

            # Read Types section
            sed -n '/\\[Types\\]/,/\\[Files\\]/p' implementation_plan.md | head -n 1 | cat

            # Read Files section
            sed -n '/\\[Files\\]/,/\\[Functions\\]/p' implementation_plan.md | head -n 1 | cat

            # Read Functions section
            sed -n '/\\[Functions\\]/,/\\[Classes\\]/p' implementation_plan.md | head -n 1 | cat

            # Read Classes section
            sed -n '/\\[Classes\\]/,/\\[Dependencies\\]/p' implementation_plan.md | head -n 1 | cat

            # Read Dependencies section
            sed -n '/\\[Dependencies\\]/,/\\[Testing\\]/p' implementation_plan.md | head -n 1 | cat

            # Read Testing section
            sed -n '/\\[Testing\\]/,/\\[Implementation Order\\]/p' implementation_plan.md | head -n 1 | cat

            # Read Implementation Order section
            sed -n '/\\[Implementation Order\\]/,$p' implementation_plan.md | cat
            """;

    static final String PLAN_DOCUMENT_STRUCTURE =
            """

            # Implementation Plan

            [Overview]
            Single sentence describing the overall goal.

            Multiple paragraphs outlining the scope, context, and high-level approach. Explain why this implementation is needed and how it fits into the existing system.

            [Types]
            Single sentence describing the type system changes.

            Detailed type definitions, interfaces, enums, or data structures with complete specifications. Include field names, types, validation rules, and relationships.

            [Files]
            Single sentence describing file modifications.

            Detailed breakdown:
            - New files to be created (with full paths and purpose)
            - Existing files to be modified (with specific changes)
            - Files to be deleted or moved
            - Configuration file updates

            [Functions]
            Single sentence describing function modifications.

            Detailed breakdown:
            - New functions (name, signature, file path, purpose)
            - Modified functions (exact name, current file path, required changes)
            - Removed functions (name, file path, reason, migration strategy)

            [Classes]
            Single sentence describing class modifications.

            Detailed breakdown:
            - New classes (name, file path, key methods, inheritance)
            - Modified classes (exact name, file path, specific modifications)
            - Removed classes (name, file path, replacement strategy)

            [Dependencies]
            Single sentence describing dependency modifications.

            Details of new packages, version changes, and integration requirements.

            [Testing]
            Single sentence describing testing approach.

            Test file requirements, existing test modifications, and validation strategies.

            [Implementation Order]
            Single sentence describing the implementation sequence.

            Numbered steps showing the logical order of changes to minimize conflicts and ensure successful integration.
            """;

    static final String QUALITY_STANDARDS =
            """
            ## Quality Standards

            You must be specific with exact file paths, function names, and class names. You must be comprehensive and avoid assuming implicit understanding. You must be practical and consider real-world constraints and edge cases. You must use precise technical language and avoid ambiguity.

            Your implementation plan should be detailed enough that another developer could execute it without additional investigation.

            ---

            **Execute all four steps in sequence. Your role is to plan thoroughly, not to implement. Code creation begins only after the new task is created and you receive explicit instruction to proceed.**

            Below is the user's input when they indicated that they wanted to create a comprehensive implementation plan.
            </explicit_instructions>""";

    static final String QUALITY_STANDARDS_5_STEP =
            """
            ## Quality Standards

            You must be specific with exact file paths, function names, and class names. You must be comprehensive and avoid assuming implicit understanding. You must be practical and consider real-world constraints and edge cases. You must use precise technical language and avoid ambiguity.

            Your implementation plan should be detailed enough that another developer could execute it without additional investigation.

            ---

            **Execute all five steps in sequence. Your role is to plan thoroughly, not to implement. Code creation begins only after the new task is created and you receive explicit instruction to proceed.**

            Below is the user's input from when they indicated that they wanted to create this comprehensive implementation plan.
            </explicit_instructions>""";

    /**
     * Generates the new_task tool instructions based on whether native tool calling is enabled.
     */
    static String generateNewTaskInstructions(boolean enableNativeToolCalls) {
        if (enableNativeToolCalls) {
            return """

            **new_task Tool Definition:**

            When you are ready to create the implementation task, you must call the new_task tool with the following structure:

            ```json
            {
              "name": "new_task",
              "arguments": {
                "context": "Your detailed context here following the 5-point structure..."
              }
            }
            ```

            The context parameter should include all five sections as described above.""";
        } else {
            return """

            **new_task Tool Definition:**

            When you are ready to create the implementation task, you must call the new_task tool with the following structure:

            ```xml
            <new_task>
            <context>Your detailed context here following the 5-point structure...</context>
            </new_task>
            ```

            The context parameter should include all five sections as described above.""";
        }
    }

    /**
     * Generates a standard 4-step template used by generic, anthropic variants.
     * @param researchInstructions The variant-specific research instructions for Step 1
     */
    static String generateFourStepTemplate(String researchInstructions) {
        return "<explicit_instructions type=\"deep-planning\">\n"
                + "Your task is to create a comprehensive implementation plan before writing any code. "
                + "This process has four distinct steps that must be completed in order.\n\n"
                + "Your behavior should be methodical and thorough - take time to understand the codebase "
                + "completely before making any recommendations. The quality of your investigation directly "
                + "impacts the success of the implementation.\n\n"
                + "## STEP 1: Silent Investigation\n\n"
                + "<important>\n"
                + "until explicitly instructed by the user to proceed with coding.\n"
                + "You must thoroughly understand the existing codebase before proposing any changes.\n"
                + "Perform your research without commentary or narration. Execute commands and read files "
                + "without explaining what you're about to do. Only speak up if you have specific questions for the user.\n"
                + "</important>\n\n"
                + researchInstructions + "\n\n"
                + "### Essential Terminal Commands\n"
                + "First, determine the language(s) used in the codebase, then execute these commands to build "
                + "your understanding. You must tailor them to the codebase and ensure the output is not overly verbose. "
                + "For example, you should exclude dependency folders such as node_modules, venv or php vendor, etc. "
                + "These are only examples, the exact commands will differ depending on the codebase.\n"
                + BASH_INVESTIGATION_COMMANDS + "\n\n"
                + "## STEP 2: Discussion and Questions\n\n"
                + "Ask the user brief, targeted questions that will influence your implementation plan. "
                + "Keep your questions concise and conversational. Ask only essential questions needed to create an accurate plan.\n\n"
                + "**Ask questions only when necessary for:**\n"
                + "- Clarifying ambiguous requirements or specifications\n"
                + "- Choosing between multiple equally valid implementation approaches\n"
                + "- Confirming assumptions about existing system behavior or constraints\n"
                + "- Understanding preferences for specific technical decisions that will affect the implementation\n\n"
                + "Your questions should be direct and specific. Avoid long explanations or multiple questions in one response.\n\n"
                + "## STEP 3: Create Implementation Plan Document\n\n"
                + "Create a structured markdown document containing your complete implementation plan. "
                + "The document must follow this exact format with clearly marked sections:\n\n"
                + "### Document Structure Requirements\n\n"
                + "Your implementation plan must be saved as implementation_plan.md, and *must* be structured as follows:\n"
                + PLAN_DOCUMENT_STRUCTURE + "\n\n"
                + "## STEP 4: Create Implementation Task\n\n"
                + "Use the new_task command to create a task for implementing the plan. "
                + "The task must include a <task_progress> list that breaks down the implementation into trackable steps.\n\n"
                + "### Task Creation Requirements\n\n"
                + "Your new task should be self-contained and reference the plan document rather than "
                + "requiring additional codebase investigation. Include these specific instructions in the task description:\n\n"
                + "**Plan Document Navigation Commands:**\n"
                + "The implementation agent should use these commands to read specific sections of the implementation plan. "
                + "You should adapt these examples to conform to the structure of the .md file you created, "
                + "and explicitly provide them when creating the new task:\n"
                + BASH_PLAN_NAVIGATION_COMMANDS + "\n\n"
                + "**Task Progress Format:**\n"
                + "<IMPORTANT>\n"
                + "You absolutely must include the task_progress contents in context when creating the new task. "
                + "When providing it, do not wrap it in XML tags- instead provide it like this:\n\n\n"
                + "task_progress Items:\n"
                + "- [ ] Step 1: Brief description of first implementation step\n"
                + "- [ ] Step 2: Brief description of second implementation step\n"
                + "- [ ] Step 3: Brief description of third implementation step\n"
                + "- [ ] Step N: Brief description of final implementation step\n\n\n"
                + "You also MUST include the path to the markdown file you have created in your new task prompt. "
                + "You should do this as follows:\n\n"
                + "Refer to @path/to/file/markdown.md for a complete breakdown of the task requirements and steps. "
                + "You should periodically read this file again.\n\n"
                + "{{FOCUS_CHAIN_PARAM}}\n\n"
                + "{{NEW_TASK_INSTRUCTIONS}}\n\n"
                + "### Mode Switching\n\n"
                + "When creating the new task, request a switch to \"act mode\" if you are currently in \"plan mode\". "
                + "This ensures the implementation agent operates in execution mode rather than planning mode.\n"
                + "</IMPORTANT>\n\n"
                + QUALITY_STANDARDS;
    }
}
