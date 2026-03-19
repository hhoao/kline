package com.hhoa.kline.core.core.task.tools.handlers;

import com.hhoa.kline.core.core.assistant.ToolUse;
import com.hhoa.kline.core.core.task.AskResult;
import com.hhoa.kline.core.core.task.tools.types.ToolContext;
import com.hhoa.kline.core.core.task.tools.types.ToolExecuteResult;
import com.hhoa.kline.core.core.task.tools.types.ToolState;

/**
 * 有状态的工具处理器接口。当工具需要 ask 用户时，execute 返回 PendingAsk， 状态保存在对应的 ToolState 子类中；用户响应后框架调用 resume 恢复执行。
 */
public interface StateFullToolHandler extends ToolHandler {

    /**
     * 创建该工具专属的 ToolState 子类实例。 框架在执行工具前调用此方法，创建的 ToolState 会被存入 TaskState.toolStates 中。
     *
     * @return 该 handler 对应的 ToolState 子类实例
     */
    ToolState createToolState();

    /**
     * 用户响应 ask 后恢复执行。
     *
     * @param context 工具上下文（已包含 ToolState）
     * @param block 原始 ToolUse 块
     * @param state 工具状态（包含阶段信息和 handler 特有的中间数据）
     * @param askResult 用户的响应结果
     * @return 工具执行结果
     */
    ToolExecuteResult resume(
            ToolContext context, ToolUse block, ToolState state, AskResult askResult);
}
