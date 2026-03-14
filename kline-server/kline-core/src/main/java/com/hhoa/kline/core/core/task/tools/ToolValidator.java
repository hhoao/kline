package com.hhoa.kline.core.core.task.tools;

import com.hhoa.kline.core.core.assistant.ToolUse;
import com.hhoa.kline.core.core.ignore.ClineIgnoreController;

/**
 * 工具参数校验器 用于验证工具参数和访问权限
 *
 * @author hhoa
 */
public class ToolValidator {

    private final ClineIgnoreController clineIgnoreController;

    public ToolValidator(ClineIgnoreController clineIgnoreController) {
        this.clineIgnoreController = clineIgnoreController;
    }

    /** 默认构造函数（不使用 ClineIgnore） */
    public ToolValidator() {
        this(null);
    }

    /**
     * 验证必需参数是否存在
     *
     * @param block 工具使用块
     * @param requiredKeys 必需参数名列表
     * @return 验证结果
     */
    public Result assertRequiredParams(ToolUse block, String... requiredKeys) {
        for (String k : requiredKeys) {
            Object v = block.getParams() == null ? null : block.getParams().get(k);
            if (v == null || String.valueOf(v).trim().isEmpty()) {
                return new Result(
                        false,
                        String.format(
                                "Missing required parameter '%s' for tool '%s'.",
                                k, block.getName()));
            }
        }
        return new Result(true, null);
    }

    /**
     * 检查路径是否被 .clineignore 阻止
     *
     * @param relPath 相对路径
     * @return 验证结果
     */
    public Result checkClineIgnorePath(String relPath) {
        if (clineIgnoreController == null) {
            // 如果没有 ClineIgnoreController，默认允许访问
            return new Result(true, null);
        }

        boolean accessAllowed = clineIgnoreController.validateAccess(relPath);
        if (!accessAllowed) {
            return new Result(
                    false,
                    String.format(
                            "Access to path '%s' is blocked by .clineignore settings.", relPath));
        }
        return new Result(true, null);
    }

    public record Result(boolean ok, String message) {}
}
