package com.hhoa.kline.plugins.jdbc.dbfilemapping.formatter;

import com.hhoa.kline.plugins.jdbc.dbfilemapping.enums.FieldValueFormat;
import java.util.EnumMap;
import java.util.Map;

/** 字段值格式化器注册表，按 {@link FieldValueFormat} 获取对应的格式化器单例 */
public final class FieldValueFormatters {

    private static final Map<FieldValueFormat, FieldValueFormatter> REGISTRY =
            new EnumMap<>(FieldValueFormat.class);

    static {
        register(new JsonFieldValueFormatter());
    }

    private FieldValueFormatters() {}

    private static void register(FieldValueFormatter formatter) {
        REGISTRY.put(formatter.getSupportedFormat(), formatter);
    }

    public static FieldValueFormatter get(FieldValueFormat format) {
        FieldValueFormatter formatter = REGISTRY.get(format);
        if (formatter == null) {
            throw new IllegalArgumentException("未注册的字段值格式: " + format);
        }
        return formatter;
    }
}
