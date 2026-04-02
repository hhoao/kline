package com.hhoa.kline.core.utils;

import static org.junit.jupiter.api.Assertions.*;

import com.hhoa.kline.core.core.utils.PartialJsonUtils;
import java.util.List;
import org.junit.jupiter.api.Test;

/** 测试 parsePartialArrayString 方法（流式传输场景） */
class JsonPartialParseArrayTest {
    @Test
    void testCompleteArray() {
        String input = "[\"Create\", \"Modify\", \"Delete\"]";
        List<String> result = PartialJsonUtils.parseArrayString(input);

        assertEquals(3, result.size());
        assertEquals("Create", result.get(0));
        assertEquals("Modify", result.get(1));
        assertEquals("Delete", result.get(2));
    }

    @Test
    void testIncompleteElement() {
        // AI 正在生成 "Modify"，只生成了 "Mod"
        String input = "[\"Create\", \"Mod";
        List<String> result = PartialJsonUtils.parseArrayString(input);

        // 应该返回完整的第一个元素 + 正在生成的第二个元素
        assertEquals(2, result.size());
        assertEquals("Create", result.get(0));
        assertEquals("Mod", result.get(1)); // 保留未完成的元素
    }

    @Test
    void testTrailingComma() {
        // AI 生成了逗号，准备生成下一个元素
        String input = "[\"Create\", \"Modify\",";
        List<String> result = PartialJsonUtils.parseArrayString(input);

        // 应该返回前两个完整的元素
        assertEquals(2, result.size());
        assertEquals("Create", result.get(0));
        assertEquals("Modify", result.get(1));
    }

    @Test
    void testStartOfNewElement() {
        // AI 开始生成第三个元素
        String input = "[\"Create\", \"Modify\", \"Del";
        List<String> result = PartialJsonUtils.parseArrayString(input);

        assertEquals(3, result.size());
        assertEquals("Create", result.get(0));
        assertEquals("Modify", result.get(1));
        assertEquals("Del", result.get(2)); // 保留未完成的元素
    }

    @Test
    void testEmptyArray() {
        String input = "[]";
        List<String> result = PartialJsonUtils.parseArrayString(input);

        assertEquals(0, result.size());
    }

    @Test
    void testArrayWithCommasInElements() {
        // 元素中包含逗号（完整JSON）
        String input = "[\"Create, please\", \"Modify, thanks\"]";
        List<String> result = PartialJsonUtils.parseArrayString(input);

        assertEquals(2, result.size());
        assertEquals("Create, please", result.get(0));
        assertEquals("Modify, thanks", result.get(1));
    }

    @Test
    void testIncompleteElementWithComma() {
        // 正在生成包含逗号的元素
        String input = "[\"Create, ple";
        List<String> result = PartialJsonUtils.parseArrayString(input);

        assertEquals(1, result.size());
        assertEquals("Create, ple", result.get(0)); // 保留未完成的元素
    }

    @Test
    void testEscapedQuotes() {
        // 包含转义引号（完整JSON）
        String input = "[\"Say \\\"hello\\\"\", \"world\"]";
        List<String> result = PartialJsonUtils.parseArrayString(input);

        assertEquals(2, result.size());
        assertEquals("Say \"hello\"", result.get(0));
        assertEquals("world", result.get(1));
    }
}
