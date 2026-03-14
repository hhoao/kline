package com.hhoa.kline.plugins.jdbc.dbfilemapping.formatter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.hhoa.kline.plugins.jdbc.dbfilemapping.enums.FieldValueFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** 将字段值作为 JSON 解析并进行美化输出（pretty-print） */
public class JsonFieldValueFormatter implements FieldValueFormatter {

    private static final Logger logger = LoggerFactory.getLogger(JsonFieldValueFormatter.class);

    private static final ObjectMapper MAPPER =
            new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    @Override
    public String format(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return rawValue;
        }
        try {
            Object parsed = MAPPER.readValue(rawValue, Object.class);
            return MAPPER.writeValueAsString(parsed);
        } catch (JsonProcessingException e) {
            logger.warn("字段值不是合法 JSON，跳过格式化: {}", e.getMessage());
            return rawValue;
        }
    }

    @Override
    public FieldValueFormat getSupportedFormat() {
        return FieldValueFormat.JSON;
    }
}
