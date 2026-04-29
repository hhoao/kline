package com.hhoa.kline.core.core.tools;

import com.hhoa.ai.kline.commons.utils.JsonUtils;
import com.hhoa.kline.core.core.assistant.ToolUse;
import java.util.Collections;
import java.util.Map;

/** Maps normalized tool parameters to a strongly typed argument object. */
public final class ToolArgumentMapper {
    private ToolArgumentMapper() {}

    public static <A> A map(ToolUse toolUse, Class<A> argumentType) {
        if (argumentType == null
                || Void.class.equals(argumentType)
                || Void.TYPE.equals(argumentType)) {
            return null;
        }
        Map<String, Object> params =
                toolUse.getParams() != null ? toolUse.getParams() : Collections.emptyMap();
        try {
            return JsonUtils.convertValue(params, argumentType);
        } catch (IllegalArgumentException error) {
            throw new IllegalArgumentException(
                    "Failed to map tool '%s' parameters to %s"
                            .formatted(toolUse.getName(), argumentType.getSimpleName()),
                    error);
        }
    }
}
