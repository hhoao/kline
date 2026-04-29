package com.hhoa.kline.core.core.tools;

import com.hhoa.kline.core.core.prompts.systemprompt.SystemPromptContext;
import java.util.*;

/**
 * 将 ToolSpec 转换为不同 API 提供商的工具定义格式。 对应 TS 的 spec.ts 中的转换函数。
 *
 * @author hhoa
 */
public class ToolSpecConverter {
    /** 工具转换输入 */
    public record ToolConversionInput(ToolSpec tool, SystemPromptContext context) {}
}
