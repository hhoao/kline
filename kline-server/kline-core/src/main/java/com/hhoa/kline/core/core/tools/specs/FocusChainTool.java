package com.hhoa.kline.core.core.tools.specs;

import com.hhoa.kline.core.core.prompts.systemprompt.ModelFamily;
import com.hhoa.kline.core.core.prompts.systemprompt.SystemPromptContext;
import com.hhoa.kline.core.core.tools.ToolSpecProvider;
import com.hhoa.kline.core.core.tools.args.FocusChainInput;
import com.hhoa.kline.core.core.tools.handlers.ToolHandler;
import com.hhoa.kline.core.enums.ClineDefaultTool;
import java.util.function.Function;

/**
 * 焦点链工具规格（占位符工具）
 *
 * @author hhoa
 */
public final class FocusChainTool extends BaseToolSpec
        implements ToolSpecProvider<FocusChainInput, ToolHandler> {

    @Override
    public String id() {
        return ClineDefaultTool.TODO.getValue();
    }

    @Override
    public String description(ModelFamily family) {
        return "";
    }

    @Override
    public Function<SystemPromptContext, Boolean> contextRequirements(ModelFamily family) {
        return context ->
                context.getFocusChainSettings() != null
                        && Boolean.TRUE.equals(context.getFocusChainSettings().isEnabled());
    }
}
