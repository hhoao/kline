package com.hhoa.kline.core.core.tools;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ToolParameterSpecTest {
    @Test
    void builderStoresParameterIdentityInConfigOption() {
        ToolParameterSpec parameter =
                ToolParameterSpec.builder()
                        .name("path")
                        .required(true)
                        .instruction("Path to read")
                        .usage("src/App.java")
                        .build();

        assertEquals("path", parameter.getOption().key());
        assertEquals("path", parameter.getName());
        assertTrue(parameter.getOption().isRequired());
        assertTrue(parameter.isRequired());
        assertEquals("string", parameter.getType());
        assertEquals("Path to read", parameter.getDescription());
    }

    @Test
    void builderPreservesSchemaTypeSeparatelyFromConfigOption() {
        ToolParameterSpec optionalList =
                ToolParameterSpec.builder()
                        .name("paths")
                        .required(false)
                        .type("array")
                        .instruction("Paths to inspect")
                        .build();

        assertEquals("paths", optionalList.getOption().key());
        assertFalse(optionalList.getOption().isRequired());
        assertEquals("array", optionalList.getType());
    }
}
