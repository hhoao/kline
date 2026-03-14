package com.hhoa.kline.plugins.jdbc.dbfilemapping.serializer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.hhoa.kline.plugins.jdbc.dbfilemapping.model.MappingConfiguration;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Jackson实现的JSON序列化器 Jackson-based implementation of JsonSerializer */
public class JacksonJsonSerializer implements JsonSerializer {

    private static final Logger logger = LoggerFactory.getLogger(JacksonJsonSerializer.class);

    private final ObjectMapper objectMapper;

    public JacksonJsonSerializer() {
        this.objectMapper = createObjectMapper();
    }

    /** 创建配置好的ObjectMapper */
    private ObjectMapper createObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();

        // 注册Java 8时间模块以支持LocalDateTime等类型
        mapper.registerModule(new JavaTimeModule());

        // 启用格式化输出（美化JSON）
        mapper.enable(SerializationFeature.INDENT_OUTPUT);

        // 禁用将日期写为时间戳
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // 保留NULL值
        mapper.setSerializationInclusion(
                com.fasterxml.jackson.annotation.JsonInclude.Include.ALWAYS);

        return mapper;
    }

    @Override
    public String serialize(Map<String, Object> record, MappingConfiguration config)
            throws SerializationException {
        if (record == null) {
            throw new SerializationException("Record cannot be null");
        }

        try {
            return objectMapper.writeValueAsString(record);
        } catch (JsonProcessingException e) {
            logger.error(
                    "Failed to serialize record for table {}: {}",
                    config != null ? config.getTableName() : "unknown",
                    e.getMessage());
            throw new SerializationException("Failed to serialize record", e);
        }
    }

    @Override
    public Map<String, Object> deserialize(String json, MappingConfiguration config)
            throws SerializationException {
        if (json == null || json.trim().isEmpty()) {
            throw new SerializationException("JSON string cannot be null or empty");
        }

        try {
            // 反序列化为Map
            TypeReference<HashMap<String, Object>> typeRef =
                    new TypeReference<HashMap<String, Object>>() {};
            Map<String, Object> record = objectMapper.readValue(json, typeRef);

            if (record == null) {
                throw new SerializationException("Deserialized record is null");
            }

            // 应用反向字段名称映射（从JSON字段名映射回数据库字段名）
            if (config != null
                    && config.getFieldNameMapping() != null
                    && !config.getFieldNameMapping().isEmpty()) {
                record = applyReverseFieldNameMapping(record, config);
            }

            return record;
        } catch (JsonProcessingException e) {
            logger.error(
                    "Failed to deserialize JSON for table {}: {}",
                    config != null ? config.getTableName() : "unknown",
                    e.getMessage());
            throw new SerializationException("Failed to deserialize JSON", e);
        }
    }

    @Override
    public String formatJson(String json) throws SerializationException {
        if (json == null || json.trim().isEmpty()) {
            throw new SerializationException("JSON string cannot be null or empty");
        }

        try {
            // 先解析再格式化
            Object obj = objectMapper.readValue(json, Object.class);
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            logger.error("Failed to format JSON: {}", e.getMessage());
            throw new SerializationException("Failed to format JSON", e);
        }
    }

    /**
     * 应用反向字段名称映射（从JSON字段名映射回数据库字段名） Apply reverse field name mapping (from JSON field names back to
     * database field names)
     *
     * @param record JSON记录
     * @param config 映射配置
     * @return 映射后的记录
     */
    private Map<String, Object> applyReverseFieldNameMapping(
            Map<String, Object> record, MappingConfiguration config) {
        Map<String, String> fieldNameMapping = config.getFieldNameMapping();
        if (fieldNameMapping == null || fieldNameMapping.isEmpty()) {
            return record;
        }

        // 创建反向映射 (JSON字段名 -> 数据库字段名)
        Map<String, String> reverseMapping = new HashMap<>();
        for (Map.Entry<String, String> entry : fieldNameMapping.entrySet()) {
            reverseMapping.put(entry.getValue(), entry.getKey());
        }

        // 应用反向映射
        Map<String, Object> mappedRecord = new HashMap<>();
        for (Map.Entry<String, Object> entry : record.entrySet()) {
            String jsonFieldName = entry.getKey();
            String dbFieldName = reverseMapping.getOrDefault(jsonFieldName, jsonFieldName);
            mappedRecord.put(dbFieldName, entry.getValue());
        }

        return mappedRecord;
    }
}
