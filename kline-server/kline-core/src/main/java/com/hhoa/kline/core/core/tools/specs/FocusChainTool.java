package com.hhoa.kline.core.core.tools.specs;

import com.hhoa.kline.core.core.prompts.systemprompt.ClineToolSpec;
import com.hhoa.kline.core.core.prompts.systemprompt.ModelFamily;
import com.hhoa.kline.core.core.prompts.systemprompt.SystemPromptContext;
import com.hhoa.kline.core.enums.ClineDefaultTool;
import java.util.Collections;
import java.util.function.Function;

/**
 * 焦点链工具规格（占位符工具）
 *
 * @author hhoa
 */
public class FocusChainTool extends BaseToolSpec {

    public static ClineToolSpec create(ModelFamily modelFamily) {
        // HACK: Placeholder to act as tool dependency
        // This is a placeholder tool with empty description, used as a dependency for other tools
        Function<SystemPromptContext, Boolean> contextRequirements =
                (context) ->
                        context.getFocusChainSettings() != null
                                && Boolean.TRUE.equals(context.getFocusChainSettings().isEnabled());

        return ClineToolSpec.builder()
                .variant(modelFamily)
                .id(ClineDefaultTool.TODO.getValue())
                .name(ClineDefaultTool.TODO.getValue())
                .description("")
                .contextRequirements(contextRequirements)
                .parameters(Collections.emptyList())
                .build();
    }
}
