package com.hhoa.kline.plugins.jdbc.dbfilemapping.serializer;

import com.hhoa.kline.plugins.jdbc.dbfilemapping.model.MappingConfiguration;
import java.util.Map;

/** JSON序列化器接口 Interface for JSON serialization and deserialization */
public interface JsonSerializer {

    /**
     * 将数据库记录序列化为JSON字符串 Serialize a database record to a formatted JSON string
     *
     * @param record 数据库记录 (字段名 -> 字段值)
     * @param config 映射配置
     * @return 格式化的JSON字符串
     * @throws SerializationException 序列化失败时抛出
     */
    String serialize(Map<String, Object> record, MappingConfiguration config)
            throws SerializationException;

    /**
     * 将JSON字符串反序列化为数据库记录 Deserialize a JSON string to a database record
     *
     * @param json JSON字符串
     * @param config 映射配置
     * @return 数据库记录 (字段名 -> 字段值)
     * @throws SerializationException 反序列化失败时抛出
     */
    Map<String, Object> deserialize(String json, MappingConfiguration config)
            throws SerializationException;

    /**
     * 格式化JSON字符串 Format a JSON string with proper indentation
     *
     * @param json 原始JSON字符串
     * @return 格式化后的JSON字符串
     * @throws SerializationException 格式化失败时抛出
     */
    String formatJson(String json) throws SerializationException;
}
