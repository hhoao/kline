package com.hhoa.kline.core.utils;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hhoa.kline.core.core.utils.PartialJsonUtils;
import org.junit.jupiter.api.Test;

/**
 * 测试 JSON 增量内容的计算逻辑
 *
 * @author hhoa
 */
class JsonPartialContentTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void testJsonPartialContent_EmptyOldText() throws Exception {
        String oldText = "{}";
        String newText = "{\"question\":\"I\"}";

        String result = PartialJsonUtils.getJsonPartialContent(newText, newText, oldText);

        assertNotNull(result);
        JsonNode resultNode = objectMapper.readTree(result);
        assertEquals("I", resultNode.get("question").asText());
    }

    @Test
    void testJsonPartialContent_IncrementalQuestion() throws Exception {
        String oldText = "{\"question\":\"I\",\"options\":[]}";
        String newText = "{\"question\":\"I can\",\"options\":[]}";

        String result = PartialJsonUtils.getJsonPartialContent(newText, newText, oldText);

        assertNotNull(result);
        JsonNode resultNode = objectMapper.readTree(result);
        assertEquals(" can", resultNode.get("question").asText());
        assertNull(resultNode.get("options"));
    }

    @Test
    void testJsonPartialContent_IncrementalOptions_NewElement() throws Exception {
        String oldText = "{\"question\":\"What?\",\"options\":[\"Create\"]}";
        String newText = "{\"question\":\"What?\",\"options\":[\"Create\",\"Modify\"]}";

        String result = PartialJsonUtils.getJsonPartialContent(newText, newText, oldText);

        assertNotNull(result);
        JsonNode resultNode = objectMapper.readTree(result);
        assertNull(resultNode.get("question"));
        assertTrue(resultNode.get("options").isArray());
        assertEquals(2, resultNode.get("options").size());
        assertTrue(resultNode.get("options").get(0).isNull());
        assertEquals("Modify", resultNode.get("options").get(1).asText());
    }

    @Test
    void testJsonPartialContent_IncrementalOptions_ElementUpdate() throws Exception {
        String oldText = "{\"question\":\"What?\",\"options\":[\"Create\",\"Modi\"]}";
        String newText = "{\"question\":\"What?\",\"options\":[\"Create\",\"Modify\"]}";

        String result = PartialJsonUtils.getJsonPartialContent(newText, newText, oldText);

        assertNotNull(result);
        JsonNode resultNode = objectMapper.readTree(result);
        assertNull(resultNode.get("question"));
        assertTrue(resultNode.get("options").isArray());
        assertEquals(2, resultNode.get("options").size());
        assertTrue(resultNode.get("options").get(0).isNull());
        assertEquals("fy", resultNode.get("options").get(1).asText());
    }

    @Test
    void testJsonPartialContent_ArrayReordering_FullReplace() throws Exception {
        String oldText =
                "{\"question\":\"What?\",\"options\":[\"Ask about a specific file\",\"Ask\",\"Ask about a\"]}";
        String newText =
                "{\"question\":\"What?\",\"options\":[\"Ask about a specific file\",\"Ask about a technical concept\"]}";

        String result = PartialJsonUtils.getJsonPartialContent(newText, newText, oldText);

        assertNotNull(result);
        JsonNode resultNode = objectMapper.readTree(result);
        assertNull(resultNode.get("question"));
        assertTrue(resultNode.get("options").isArray());
        assertEquals(2, resultNode.get("options").size());
        assertEquals("Ask about a specific file", resultNode.get("options").get(0).asText());
        assertEquals("Ask about a technical concept", resultNode.get("options").get(1).asText());
    }

    @Test
    void testJsonPartialContent_IncrementalOptions_Mixed() throws Exception {
        String oldText = "{\"question\":\"What?\",\"options\":[\"Create\",\"Mod\"]}";
        String newText = "{\"question\":\"What?\",\"options\":[\"Create\",\"Modify\",\"Delete\"]}";

        String result = PartialJsonUtils.getJsonPartialContent(newText, newText, oldText);

        assertNotNull(result);
        JsonNode resultNode = objectMapper.readTree(result);
        assertNull(resultNode.get("question"));
        assertTrue(resultNode.get("options").isArray());
        assertEquals(3, resultNode.get("options").size());
        assertTrue(resultNode.get("options").get(0).isNull());
        assertEquals("ify", resultNode.get("options").get(1).asText());
        assertEquals("Delete", resultNode.get("options").get(2).asText());
    }

    @Test
    void testJsonPartialContent_NoChange() throws Exception {
        String oldText = "{\"question\":\"What?\",\"options\":[]}";
        String newText = "{\"question\":\"What?\",\"options\":[]}";

        String result = PartialJsonUtils.getJsonPartialContent(newText, newText, oldText);

        assertNull(result);
    }
}
