package com.hhoa.kline.core.core.tools.specs;

import com.hhoa.kline.core.core.prompts.systemprompt.ModelFamily;
import com.hhoa.kline.core.core.tools.ToolSpecProvider;
import com.hhoa.kline.core.core.tools.args.ReplaceInFileInput;
import com.hhoa.kline.core.core.tools.handlers.ReplaceInFileToolHandler;
import com.hhoa.kline.core.core.tools.handlers.ToolHandler;
import com.hhoa.kline.core.core.tools.ClineDefaultTool;

/**
 * 替换文件内容工具规格
 *
 * @author hhoa
 */
public final class ReplaceInFileTool implements ToolSpecProvider<ReplaceInFileInput> {
    private static final ReplaceInFileToolHandler HANDLER = new ReplaceInFileToolHandler();

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

    private static final String DESCRIPTION = "Replace sections of an existing file.";

    private static final String GENERIC_PROMPT =
            """
            Request to replace sections of content in an existing file using SEARCH/REPLACE blocks that define exact changes to specific parts of the file. This tool should be used when you need to make targeted changes to specific parts of a file.

            Diff parameter format:
            %s%s"""
                    .formatted(BASE_DIFF_INSTRUCTIONS, NOTEBOOK_INSTRUCTIONS);

    private static final String NATIVE_PROMPT =
            """
            [IMPORTANT: Always output the absolutePath first] Request to replace sections of content in an existing file using SEARCH/REPLACE blocks that define exact changes to specific parts of the file. This tool should be used when you need to make targeted changes to specific parts of a file.

            Diff parameter format:
            %s%s"""
                    .formatted(BASE_DIFF_INSTRUCTIONS, NOTEBOOK_INSTRUCTIONS);

    @Override
    public String name() {
        return ClineDefaultTool.FILE_EDIT.getValue();
    }

    @Override
    public String description(ModelFamily family) {
        return DESCRIPTION;
    }

    @Override
    public String prompt(ModelFamily family) {
        return switch (family) {
            case NATIVE_GPT_5, NATIVE_GPT_5_1, NATIVE_NEXT_GEN -> NATIVE_PROMPT;
            default -> GENERIC_PROMPT;
        };
    }

    @Override
    public Class<ReplaceInFileInput> inputType(ModelFamily family) {
        return ReplaceInFileInput.class;
    }

    @Override
    public ToolHandler<ReplaceInFileInput> handler(ModelFamily family) {
        return HANDLER;
    }
}
