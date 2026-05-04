package com.hhoa.kline.core.core.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.Error;
import com.networknt.schema.Schema;
import com.networknt.schema.SchemaRegistry;
import com.networknt.schema.SpecificationVersion;
import java.util.Map;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/** Shared JSON schema validation utilities for tool calls. */
public final class JsonSchemaValidationSupport {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final SchemaRegistry SCHEMA_REGISTRY =
            SchemaRegistry.withDefaultDialect(SpecificationVersion.DRAFT_2020_12);
    private static final Map<String, Schema> SCHEMA_CACHE = new ConcurrentHashMap<>();

    private JsonSchemaValidationSupport() {}

    public static List<Error> validate(
            String cacheKey, Map<String, Object> inputSchema, Map<String, Object> params) {
        Schema schema = SCHEMA_CACHE.computeIfAbsent(cacheKey, key -> buildSchema(inputSchema));
        JsonNode payload = MAPPER.valueToTree(params);
        return schema.validate(payload);
    }

    public static String cacheKey(String toolName, Map<String, Object> inputSchema) {
        return toolName + ":" + inputSchema.hashCode();
    }

    private static Schema buildSchema(Map<String, Object> inputSchema) {
        JsonNode schemaNode = MAPPER.valueToTree(inputSchema);
        return SCHEMA_REGISTRY.getSchema(schemaNode);
    }
}
