package com.hhoa.kline.core.assistant;

import static org.junit.jupiter.api.Assertions.*;

import com.hhoa.kline.core.core.assistant.DiffProcessor;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * 差异处理器测试
 *
 * @author hhoa
 */
class DiffProcessorTest {

    private DiffProcessor diffProcessor;

    @BeforeEach
    void beforeEach() {
        diffProcessor = new DiffProcessor();
    }

    @Test
    void testSimpleReplacement() {
        String originalContent =
                "public class Test {\n    public void method() {\n        log.info(\"Hello\");\n    }\n}";
        String diffContent =
                "------- SEARCH\n    public void method() {\n        log.info(\"Hello\");\n    }\n=======\n    public void method() {\n        log.info(\"Hello World\");\n    }\n+++++++ REPLACE";

        String result = diffProcessor.constructNewFileContent(diffContent, originalContent, true);

        assertTrue(result.contains("Hello World"));
        assertFalse(result.contains("Hello\");"));
    }

    @Test
    void testNewFileCreation() {
        String originalContent = "";
        String diffContent =
                "------- SEARCH\n=======\npublic class NewTest {\n    public static void main(String[] args) {\n        log.info(\"New file\");\n    }\n}\n+++++++ REPLACE";

        String result = diffProcessor.constructNewFileContent(diffContent, originalContent, true);

        assertTrue(result.contains("public class NewTest"));
        assertTrue(result.contains("New file"));
    }

    @Test
    void testCompleteFileReplacement() {
        String originalContent = "old content";
        String diffContent = "------- SEARCH\n=======\nnew content\n+++++++ REPLACE";

        String result = diffProcessor.constructNewFileContent(diffContent, originalContent, true);

        assertEquals("new content\n", result);
    }

    @Test
    void testMultipleReplacements() {
        String originalContent = "line1\nline2\nline3\nline4";
        String diffContent =
                "------- SEARCH\nline2\n=======\nline2_modified\n+++++++ REPLACE\n------- SEARCH\nline4\n=======\nline4_modified\n+++++++ REPLACE";

        String result = diffProcessor.constructNewFileContent(diffContent, originalContent, true);

        assertTrue(result.contains("line2_modified"));
        assertTrue(result.contains("line4_modified"));
    }

    @Test
    void testV2Version() {
        String originalContent =
                "public class Test {\n    public void method() {\n        log.info(\"Hello\");\n    }\n}";
        String diffContent =
                "------- SEARCH\n    public void method() {\n        log.info(\"Hello\");\n    }\n=======\n    public void method() {\n        log.info(\"Hello World\");\n    }\n+++++++ REPLACE";

        String result = diffProcessor.constructNewFileContent(diffContent, originalContent, true);

        assertTrue(result.contains("Hello World"));
        assertFalse(result.contains("Hello\");"));
    }
}
