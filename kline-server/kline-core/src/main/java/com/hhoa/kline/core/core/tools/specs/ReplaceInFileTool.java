package com.hhoa.kline.core.core.tools.specs;

import com.hhoa.kline.core.core.prompts.systemprompt.ModelFamily;
import com.hhoa.kline.core.core.prompts.systemprompt.SystemPromptContext;
import com.hhoa.kline.core.core.tools.ToolSpecProvider;
import com.hhoa.kline.core.core.tools.args.ReplaceInFileInput;
import com.hhoa.kline.core.core.tools.handlers.ToolHandler;
import com.hhoa.kline.core.enums.ClineDefaultTool;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * 替换文件内容工具规格
 *
 * @author hhoa
 */
public final class ReplaceInFileTool extends BaseToolSpec
        implements ToolSpecProvider<ReplaceInFileInput, ToolHandler> {

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

    /** 动态 diff instruction：当编辑器中有打开的 .ipynb 文件时，追加 notebook 指令。 */
    private static final Function<SystemPromptContext, String> DIFF_INSTRUCTION_FN =
            context -> {
                if (shouldIncludeNotebookInstructions(context)) {
                    return BASE_DIFF_INSTRUCTIONS + NOTEBOOK_INSTRUCTIONS;
                }
                return BASE_DIFF_INSTRUCTIONS;
            };

    private static boolean shouldIncludeNotebookInstructions(SystemPromptContext context) {
        if (context == null || context.getEditorTabs() == null) {
            return false;
        }
        List<String> paths = getOpenOrVisibleTabPaths(context);
        return paths.stream().anyMatch(p -> p.endsWith(".ipynb"));
    }

    private static List<String> getOpenOrVisibleTabPaths(SystemPromptContext context) {
        SystemPromptContext.EditorTabs tabs = context.getEditorTabs();
        if (tabs == null) {
            return Collections.emptyList();
        }
        List<String> result = new ArrayList<>();
        if (tabs.getOpen() != null) {
            result.addAll(tabs.getOpen());
        }
        if (tabs.getVisible() != null) {
            result.addAll(tabs.getVisible());
        }
        return result;
    }

    @Override
    public String id() {
        return ClineDefaultTool.FILE_EDIT.getValue();
    }

    @Override
    public String description(ModelFamily family) {
        return switch (family) {
            case NATIVE_GPT_5, NATIVE_GPT_5_1, NATIVE_NEXT_GEN -> NATIVE_DESCRIPTION;
            default -> GENERIC_DESCRIPTION;
        };
    }

    @Override
    public void customizeInputSchema(ModelFamily family, Map<String, Object> inputSchema) {
        require(inputSchema, "diff");
        instructionFn(inputSchema, "diff", DIFF_INSTRUCTION_FN);
        if (isNative(family)) {
            require(inputSchema, "absolutePath");
            describe(inputSchema, "absolutePath", "The absolute path to the file to write to.");
            return;
        }
        require(inputSchema, "path");
        describe(
                inputSchema,
                "path",
                "The path of the file to modify (relative to the current working directory {{CWD}})");
        usage(inputSchema, "path", "File path here");
        usage(inputSchema, "diff", "Search and replace blocks here");
    }

    @Override
    public Set<String> excludedParameters(ModelFamily family) {
        return isNative(family) ? Set.of("path") : Set.of("absolutePath");
    }

    private static boolean isNative(ModelFamily family) {
        return family == ModelFamily.NATIVE_GPT_5
                || family == ModelFamily.NATIVE_GPT_5_1
                || family == ModelFamily.NATIVE_NEXT_GEN;
    }
}
