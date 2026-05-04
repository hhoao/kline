package com.hhoa.kline.core.core.assistant.parser;

import com.hhoa.kline.core.core.tools.ClineDefaultTool;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 预置的 Cline 标签层级配置工厂
 *
 * @author hhoa
 */
public final class ClineTagConfigs {
    private final Set<String> allParamNames;

    public ClineTagConfigs(Set<String> allParamNames) {
        if (allParamNames == null || allParamNames.isEmpty()) {
            throw new IllegalArgumentException("allParamNames must not be null or empty");
        }
        this.allParamNames = allParamNames;
    }

    /**
     * Cline 扁平格式：工具名作为根标签，参数名作为工具的子标签
     *
     * <pre>{@code
     * <read_file>
     *   <path>test.txt</path>
     * </read_file>
     * }</pre>
     */
    public TagHierarchyConfig flatFormat() {
        Set<String> allToolNames =
                Arrays.stream(ClineDefaultTool.values())
                        .map(ClineDefaultTool::getValue)
                        .collect(Collectors.toSet());

        TagHierarchyConfig.Builder builder =
                TagHierarchyConfig.builder().rootTags(allToolNames).rootTag("thinking");

        for (String toolName : allToolNames) {
            builder.childTags(toolName, allParamNames);
        }

        return builder.build();
    }

    /**
     * 嵌套 invoke 格式：function_calls → invoke → parameter
     *
     * <pre>{@code
     * <function_calls>
     *   <invoke name="read_file">
     *     <parameter name="path">test.txt</parameter>
     *   </invoke>
     * </function_calls>
     * }</pre>
     */
    public TagHierarchyConfig nestedFormat() {
        return TagHierarchyConfig.builder()
                .rootTag("function_calls")
                .childTags("function_calls", Set.of("invoke"))
                .childTags("invoke", Set.of("parameter"))
                .build();
    }
}
