package com.hhoa.kline.core.core.prompts.systemprompt.tools;

import com.hhoa.kline.core.core.prompts.systemprompt.ClineToolSpec;
import com.hhoa.kline.core.core.prompts.systemprompt.ModelFamily;
import com.hhoa.kline.core.core.prompts.systemprompt.SystemPromptContext;
import com.hhoa.kline.core.enums.ClineDefaultTool;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

/**
 * 替换文件内容工具规格
 *
 * @author hhoa
 */
public class ReplaceInFileTool extends BaseToolSpec
{

    private static final String GENERIC_DESCRIPTION =
            "Request to replace sections of content in an existing file using SEARCH/REPLACE blocks that define exact changes to specific parts of the file. This tool should be used when you need to make targeted changes to specific parts of a file.";

    private static final String NATIVE_DESCRIPTION =
            "[IMPORTANT: Always output the absolutePath first] Request to replace sections of content in an existing file using SEARCH/REPLACE blocks that define exact changes to specific parts of the file. This tool should be used when you need to make targeted changes to specific parts of a file.";

    private static final String BASE_DIFF_INSTRUCTIONS =
            """
            One or more SEARCH/REPLACE blocks following this exact format:
              ```
              ------- SEARCH
              [exact content to find]
              =======
              [new content to replace with]
              +++++++ REPLACE
              ```
              Critical rules:
              1. SEARCH content must match the associated file section to find EXACTLY:
                 * Match character-for-character including whitespace, indentation, line endings
                 * Include all comments, docstrings, etc.
              2. SEARCH/REPLACE blocks will ONLY replace the first match occurrence.
                 * Including multiple unique SEARCH/REPLACE blocks if you need to make multiple changes.
                 * Include *just* enough lines in each SEARCH section to uniquely match each set of lines that need to change.
                 * When using multiple SEARCH/REPLACE blocks, list them in the order they appear in the file.
              3. Keep SEARCH/REPLACE blocks concise:
                 * Break large SEARCH/REPLACE blocks into a series of smaller blocks that each change a small portion of the file.
                 * Include just the changing lines, and a few surrounding lines if needed for uniqueness.
                 * Do not include long runs of unchanging lines in SEARCH/REPLACE blocks.
                 * Each line must be complete. Never truncate lines mid-way through as this can cause matching failures.
              4. Special operations:
                 * To move code: Use two SEARCH/REPLACE blocks (one to delete from original + one to insert at new location)
                 * To delete code: Use empty REPLACE section""";

    private static final String NOTEBOOK_INSTRUCTIONS =
            """

              5. For Jupyter Notebook (.ipynb) files:
                 * Match the exact JSON structure including quotes, commas, and \\n characters
                 * Each line in "source" array (except last) must end with "\\n"
                 * Each source line is a separate JSON string in the array
                 * Example SEARCH block for notebook:
                   ------- SEARCH
                     "source": [
                       "x = 10\\n",
                       "print(x)"
                     ]
                   =======
                     "source": [
                       "x = 100\\n",
                       "print(x)"
                     ]
                   +++++++ REPLACE""";

    /**
     * 动态 diff instruction：当编辑器中有打开的 .ipynb 文件时，追加 notebook 指令。
     */
    private static final Function<SystemPromptContext, String> DIFF_INSTRUCTION_FN =
            context -> {
                if (shouldIncludeNotebookInstructions(context))
                {
                    return BASE_DIFF_INSTRUCTIONS + NOTEBOOK_INSTRUCTIONS;
                }
                return BASE_DIFF_INSTRUCTIONS;
            };

    private static boolean shouldIncludeNotebookInstructions(SystemPromptContext context)
    {
        if (context == null || context.getEditorTabs() == null)
        {
            return false;
        }
        List<String> paths = getOpenOrVisibleTabPaths(context);
        return paths.stream().anyMatch(p -> p.endsWith(".ipynb"));
    }

    private static List<String> getOpenOrVisibleTabPaths(SystemPromptContext context)
    {
        SystemPromptContext.EditorTabs tabs = context.getEditorTabs();
        if (tabs == null)
        {
            return Collections.emptyList();
        }
        List<String> result = new ArrayList<>();
        if (tabs.getOpen() != null)
        {
            result.addAll(tabs.getOpen());
        }
        if (tabs.getVisible() != null)
        {
            result.addAll(tabs.getVisible());
        }
        return result;
    }

    public static ClineToolSpec create(ModelFamily modelFamily)
    {
        if (modelFamily == ModelFamily.NATIVE_GPT_5
                || modelFamily == ModelFamily.NATIVE_GPT_5_1
                || modelFamily == ModelFamily.NATIVE_NEXT_GEN)
        {
            return createNativeVariant(modelFamily);
        }

        return createGenericVariant(modelFamily);
    }

    private static ClineToolSpec createGenericVariant(ModelFamily modelFamily)
    {
        return ClineToolSpec.builder()
                .variant(modelFamily)
                .id(ClineDefaultTool.FILE_EDIT.getValue())
                .name(ClineDefaultTool.FILE_EDIT.getValue())
                .description(GENERIC_DESCRIPTION)
                .parameters(
                        List.of(
                                createParameter(
                                        "path",
                                        true,
                                        "The path of the file to modify (relative to the current working directory {{CWD}})",
                                        "File path here"),
                                createParameterWithInstructionFn(
                                        "diff",
                                        true,
                                        DIFF_INSTRUCTION_FN,
                                        "Search and replace blocks here"),
                                createTaskProgressParameter()))
                .build();
    }

    private static ClineToolSpec createNativeVariant(ModelFamily modelFamily)
    {
        return ClineToolSpec.builder()
                .variant(modelFamily)
                .id(ClineDefaultTool.FILE_EDIT.getValue())
                .name(ClineDefaultTool.FILE_EDIT.getValue())
                .description(NATIVE_DESCRIPTION)
                .parameters(
                        List.of(
                                createParameter(
                                        "absolutePath",
                                        true,
                                        "The absolute path to the file to write to.",
                                        null),
                                createParameterWithInstructionFn(
                                        "diff",
                                        true,
                                        DIFF_INSTRUCTION_FN,
                                        null),
                                createTaskProgressParameter()))
                .build();
    }
}
