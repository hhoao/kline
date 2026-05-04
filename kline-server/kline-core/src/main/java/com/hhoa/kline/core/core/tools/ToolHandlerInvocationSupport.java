package com.hhoa.kline.core.core.tools;

import com.hhoa.ai.kline.commons.utils.JsonUtils;
import com.hhoa.kline.core.core.assistant.ToolUse;
import com.hhoa.kline.core.core.tools.handlers.ToolHandler;
import com.hhoa.kline.core.core.tools.types.ToolContext;
import com.hhoa.kline.core.core.tools.types.ToolExecuteResult;
import java.util.Collections;
import java.util.Map;

/** Maps tool parameters to the handler's generic input type and invokes the typed handler. */
public final class ToolHandlerInvocationSupport {
    private ToolHandlerInvocationSupport() {}

    public static ToolExecuteResult invoke(
            ToolHandler<?> handler, ToolContext context, ToolUse toolUse) {
        Object input = mapInput(handler, toolUse);
        return invokeTyped(handler, input, context, toolUse);
    }

    public static void handlePartialBlock(
            ToolHandler<?> handler, ToolContext context, ToolUse toolUse) {
        Object input = mapInput(handler, toolUse);
        handleTypedPartialBlock(handler, input, context, toolUse);
    }

    private static Object mapInput(ToolHandler<?> handler, ToolUse toolUse) {
        Class<?> inputType = ToolSpecResolver.requireArgumentType(handler.getClass());
        if (Void.class.equals(inputType) || Void.TYPE.equals(inputType)) {
            return null;
        }
        Map<String, Object> params =
                toolUse.getParams() != null ? toolUse.getParams() : Collections.emptyMap();
        try {
            return JsonUtils.convertValue(params, inputType);
        } catch (IllegalArgumentException error) {
            throw new IllegalArgumentException(
                    "Failed to map tool '%s' parameters to %s"
                            .formatted(toolUse.getName(), inputType.getSimpleName()),
                    error);
        }
    }

    @SuppressWarnings("unchecked")
    private static ToolExecuteResult invokeTyped(
            ToolHandler<?> handler, Object input, ToolContext context, ToolUse toolUse) {
        return ((ToolHandler<Object>) handler).execute(input, context, toolUse);
    }

    @SuppressWarnings("unchecked")
    private static void handleTypedPartialBlock(
            ToolHandler<?> handler, Object input, ToolContext context, ToolUse toolUse) {
        ((ToolHandler<Object>) handler).handlePartialBlock(input, context, toolUse);
    }
}
