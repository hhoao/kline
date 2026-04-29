package com.hhoa.kline.core.core.tools;

import com.hhoa.ai.kline.commons.utils.JsonUtils;
import com.hhoa.kline.core.core.assistant.ToolUse;
import com.hhoa.kline.core.core.tools.handlers.ToolHandler;
import com.hhoa.kline.core.core.tools.types.ToolContext;
import com.hhoa.kline.core.core.tools.types.ToolExecuteResult;
import com.hhoa.kline.core.core.tools.types.UIHelpers;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/** Invokes raw or typed handlers through one execution boundary. */
public final class ToolHandlerInvocationSupport {
    private ToolHandlerInvocationSupport() {}

    public static ToolExecuteResult invoke(
            ToolHandler handler, ToolContext context, ToolUse toolUse) {
        ToolSpecResolver.MethodInput methodInput =
                ToolSpecResolver.resolveMethodInput(handler.getClass(), "execute");
        if (methodInput.method() == null) {
            throw new UnsupportedOperationException(
                    "Tool handler %s does not declare a typed execute method for tool '%s'."
                            .formatted(handler.getClass().getName(), toolUse.getName()));
        }
        Object result = invokeMethod(handler, methodInput.method(), context, toolUse);
        return (ToolExecuteResult) result;
    }

    public static void handlePartialBlock(
            ToolHandler handler, ToolContext context, ToolUse toolUse) {
        ToolSpecResolver.MethodInput methodInput =
                ToolSpecResolver.resolveMethodInput(handler.getClass(), "handlePartialBlock");
        if (methodInput.method() == null) {
            return;
        }
        invokeMethod(handler, methodInput.method(), context, toolUse);
    }

    private static Object invokeMethod(
            ToolHandler handler, Method method, ToolContext context, ToolUse toolUse) {
        try {
            method.setAccessible(true);
            return method.invoke(handler, buildArguments(method, context, toolUse));
        } catch (IllegalAccessException error) {
            throw new IllegalStateException(
                    "Cannot access tool handler method %s".formatted(method), error);
        } catch (InvocationTargetException error) {
            Throwable cause = error.getCause() != null ? error.getCause() : error;
            if (cause instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw new IllegalStateException(
                    "Tool handler method %s failed".formatted(method), cause);
        }
    }

    private static Object[] buildArguments(Method method, ToolContext context, ToolUse toolUse) {
        Parameter[] parameters = method.getParameters();
        List<Parameter> inputParameters =
                ToolSpecResolver.resolveMethodInput(method.getDeclaringClass(), method.getName())
                        .inputParameters();
        Object[] args = new Object[parameters.length];
        Map<String, Object> params =
                toolUse.getParams() != null ? toolUse.getParams() : Collections.emptyMap();
        boolean singleAggregateInput =
                inputParameters.size() == 1
                        && isAggregateInput(inputParameters.getFirst().getType());
        int inputIndex = 0;

        for (int i = 0; i < parameters.length; i++) {
            Parameter parameter = parameters[i];
            Class<?> type = parameter.getType();
            if (ToolContext.class.isAssignableFrom(type)) {
                args[i] = context;
            } else if (ToolUse.class.isAssignableFrom(type)) {
                args[i] = toolUse;
            } else if (UIHelpers.class.isAssignableFrom(type)) {
                args[i] = UIHelpers.create(context);
            } else if (singleAggregateInput && inputIndex == 0) {
                args[i] = ToolArgumentMapper.map(toolUse, type);
                inputIndex++;
            } else {
                String name = ToolSpecResolver.parameterName(parameter);
                args[i] = JsonUtils.convertValue(params.get(name), type);
                inputIndex++;
            }
        }
        return args;
    }

    private static boolean isAggregateInput(Class<?> type) {
        return !(type.isPrimitive()
                || String.class.equals(type)
                || Number.class.isAssignableFrom(type)
                || Boolean.class.equals(type)
                || Character.class.equals(type)
                || Map.class.isAssignableFrom(type)
                || List.class.isAssignableFrom(type));
    }
}
