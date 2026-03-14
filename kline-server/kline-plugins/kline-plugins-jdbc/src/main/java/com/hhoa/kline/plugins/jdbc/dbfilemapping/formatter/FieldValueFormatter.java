package com.hhoa.kline.plugins.jdbc.dbfilemapping.formatter;

import com.hhoa.kline.plugins.jdbc.dbfilemapping.enums.FieldValueFormat;

/** 字段值格式化器接口，用于 DB→文件 同步时对字段值进行格式化，格式化结果同步回 DB */
public interface FieldValueFormatter {

    /**
     * 对字段原始值进行格式化
     *
     * @param rawValue 原始字段值字符串
     * @return 格式化后的字符串；若原始值为 null 或不合法则原样返回
     */
    String format(String rawValue);

    FieldValueFormat getSupportedFormat();
}
