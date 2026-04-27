package com.hhoa.kline.core.core.tools.specs;

import com.hhoa.kline.core.core.prompts.systemprompt.ClineToolSpec;
import com.hhoa.kline.core.core.prompts.systemprompt.ModelFamily;
import com.hhoa.kline.core.enums.ClineDefaultTool;
import java.util.List;

/**
 * Generate Explanation 工具规格 - 生成 AI 驱动的代码变更解释
 *
 * @author hhoa
 */
public class GenerateExplanationTool extends BaseToolSpec {

    public static ClineToolSpec create(ModelFamily modelFamily) {
        return ClineToolSpec.builder()
                .variant(modelFamily)
                .id(ClineDefaultTool.GENERATE_EXPLANATION.getValue())
                .name(ClineDefaultTool.GENERATE_EXPLANATION.getValue())
                .description(
                        "Opens a multi-file diff view and generates AI-powered inline comments explaining the changes "
                                + "between two git references. Use this tool to help users understand code changes from git commits, "
                                + "pull requests, branches, or any git refs. The tool uses git to retrieve file contents and "
                                + "displays a side-by-side diff view with explanatory comments.")
                .contextRequirements(context -> !Boolean.TRUE.equals(context.getIsCliEnvironment()))
                .parameters(
                        List.of(
                                createParameter(
                                        "title",
                                        true,
                                        "A descriptive title for the diff view (e.g., 'Changes in commit abc123', "
                                                + "'PR #42: Add authentication', 'Changes between main and feature-branch')",
                                        "Changes in last commit"),
                                createParameter(
                                        "from_ref",
                                        true,
                                        "The git reference for the 'before' state. Can be a commit hash, branch name, "
                                                + "tag, or relative reference like HEAD~1, HEAD^, origin/main, etc.",
                                        "HEAD~1"),
                                createParameter(
                                        "to_ref",
                                        false,
                                        "The git reference for the 'after' state. Can be a commit hash, branch name, "
                                                + "tag, or relative reference. If not provided, compares to the current working "
                                                + "directory (including uncommitted changes).",
                                        "HEAD")))
                .build();
    }
}
