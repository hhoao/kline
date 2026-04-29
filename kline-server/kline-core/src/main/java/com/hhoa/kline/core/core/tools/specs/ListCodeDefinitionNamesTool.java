package com.hhoa.kline.core.core.tools.specs;

import com.hhoa.kline.core.core.prompts.systemprompt.ModelFamily;
import com.hhoa.kline.core.core.tools.ToolSpecProvider;
import com.hhoa.kline.core.core.tools.args.ListCodeDefinitionNamesInput;
import com.hhoa.kline.core.core.tools.handlers.ListCodeDefinitionNamesToolHandler;
import com.hhoa.kline.core.enums.ClineDefaultTool;

/**
 * 列出代码定义名称工具规格
 *
 * @author hhoa
 */
public final class ListCodeDefinitionNamesTool extends BaseToolSpec
        implements ToolSpecProvider<
                ListCodeDefinitionNamesInput, ListCodeDefinitionNamesToolHandler> {

    private static final String DESCRIPTION =
            "Request to list definition names (classes, functions, methods, etc.) used in source code files at the top level of the specified directory. This tool provides insights into the codebase structure and important constructs, encapsulating high-level concepts and relationships that are crucial for understanding the overall architecture.";

    @Override
    public String id() {
        return ClineDefaultTool.LIST_CODE_DEF.getValue();
    }

    @Override
    public String description(ModelFamily family) {
        return DESCRIPTION;
    }
}
