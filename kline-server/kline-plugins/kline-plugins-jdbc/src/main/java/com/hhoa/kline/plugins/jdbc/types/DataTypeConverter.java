package com.hhoa.kline.plugins.jdbc.types;

/**
 * DataTypeConverter
 *
 * @author hhoa
 * @since 2025/10/17
 */
public interface DataTypeConverter {
    /**
     * 转换数据类型
     *
     * @param value 原始数据
     * @return 转换后的数据
     */
    Object convert(AbstractBaseColumn value) throws Exception;
}
