package com.hhoa.kline.plugins.jdbc.dbfilemapping.helper;

import com.hhoa.kline.plugins.jdbc.dbfilemapping.model.MappingConfiguration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 记录字段过滤工具，用于 DB→JSON 序列化前应用 includedFields、excludedFields、fieldNameMapping、
 * fieldValueTransformers。供 SingleJsonFileProcessor、ConflictResolver 等调用。
 */
public final class RecordFieldFilter {

    private static final Logger logger = LoggerFactory.getLogger(RecordFieldFilter.class);

    private RecordFieldFilter() {}

    /**
     * 对记录应用字段过滤规则，得到适合序列化为 JSON 的 Map。
     *
     * @param record 原始数据库记录
     * @param config 映射配置
     * @return 过滤后的记录
     */
    public static Map<String, Object> applyForSerialization(
            Map<String, Object> record, MappingConfiguration config) {
        if (config == null) {
            return record;
        }

        List<String> includedFields = config.getIncludedFields();
        List<String> excludedFields = config.getExcludedFields();
        Map<String, String> fieldNameMapping = config.getFieldNameMapping();
        Map<String, Function<Object, Object>> fieldValueTransformers =
                config.getFieldValueTransformers();

        if ((includedFields == null || includedFields.isEmpty())
                && (excludedFields == null || excludedFields.isEmpty())
                && (fieldNameMapping == null || fieldNameMapping.isEmpty())
                && (fieldValueTransformers == null || fieldValueTransformers.isEmpty())) {
            return record;
        }

        Map<String, Object> filteredRecord = new HashMap<>();

        Map<String, Object> workingRecord;
        if (includedFields != null && !includedFields.isEmpty()) {
            workingRecord = new HashMap<>();
            for (String field : includedFields) {
                if (record.containsKey(field)) {
                    workingRecord.put(field, record.get(field));
                }
            }
        } else {
            workingRecord = new HashMap<>(record);
        }

        if (excludedFields != null) {
            excludedFields.forEach(workingRecord::remove);
        }

        for (Map.Entry<String, Object> entry : workingRecord.entrySet()) {
            String originalFieldName = entry.getKey();
            Object originalValue = entry.getValue();

            String mappedFieldName = originalFieldName;
            if (fieldNameMapping != null && fieldNameMapping.containsKey(originalFieldName)) {
                mappedFieldName = fieldNameMapping.get(originalFieldName);
            }

            Object transformedValue = originalValue;
            if (fieldValueTransformers != null
                    && fieldValueTransformers.containsKey(originalFieldName)) {
                try {
                    transformedValue =
                            fieldValueTransformers.get(originalFieldName).apply(originalValue);
                } catch (Exception e) {
                    logger.warn("字段 {} 转换失败，使用原始值: {}", originalFieldName, e.getMessage());
                }
            }

            filteredRecord.put(mappedFieldName, transformedValue);
        }

        return filteredRecord;
    }
}
