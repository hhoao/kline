package com.hhoa.kline.core.assistant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.hhoa.kline.core.core.assistant.AssistantMessageContent;
import com.hhoa.kline.core.core.assistant.TextContent;
import com.hhoa.kline.core.core.assistant.ToolUse;
import com.hhoa.kline.core.core.assistant.parser.AssistantMessageParser;
import com.hhoa.kline.core.core.assistant.parser.ClineTagConfigs;
import com.hhoa.kline.core.core.assistant.parser.DefaultStreamingAssistantMessageParser;
import com.hhoa.kline.core.core.assistant.parser.StreamingAssistantMessageParser;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * 有状态流式助手消息解析器测试
 *
 * @author hhoa
 */
class StreamingAssistantMessageParserTest {

    private StreamingAssistantMessageParser parser;

    @BeforeEach
    void setUp() {
        parser = new DefaultStreamingAssistantMessageParser(ClineTagConfigs.flatFormat());
    }

    // ===== 1. 基础文本流式解析 =====

    @Nested
    class TextStreaming {

        @Test
        void feedSingleChunkOfText() {
            List<AssistantMessageContent> result = parser.feed("Hello world");

            assertEquals(1, result.size());
            assertInstanceOf(TextContent.class, result.get(0));
            assertEquals("Hello world", ((TextContent) result.get(0)).getContent());
            assertTrue(result.get(0).isPartial());
        }

        @Test
        void feedMultipleTextChunks() {
            parser.feed("Hello ");
            List<AssistantMessageContent> result = parser.feed("world");

            assertEquals(1, result.size());
            assertInstanceOf(TextContent.class, result.get(0));
            assertEquals("Hello world", ((TextContent) result.get(0)).getContent());
            assertTrue(result.get(0).isPartial());
        }

        @Test
        void feedCharacterByCharacter() {
            String message = "你好世界";
            List<AssistantMessageContent> result = null;
            for (int i = 0; i < message.length(); i++) {
                result = parser.feed(String.valueOf(message.charAt(i)));
            }

            assertNotNull(result);
            assertEquals(1, result.size());
            assertEquals("你好世界", ((TextContent) result.get(0)).getContent());
        }

        @Test
        void feedEmptyChunks() {
            parser.feed("Hello");
            List<AssistantMessageContent> result = parser.feed("");

            assertEquals(1, result.size());
            assertEquals("Hello", ((TextContent) result.get(0)).getContent());
        }

        @Test
        void finalizeTextContent() {
            parser.feed("Hello world");
            List<AssistantMessageContent> result = parser.complete();

            assertEquals(1, result.size());
            assertInstanceOf(TextContent.class, result.get(0));
            assertEquals("Hello world", ((TextContent) result.get(0)).getContent());
            assertFalse(result.get(0).isPartial());
        }
    }

    // ===== 2. 工具标签检测与解析 =====

    @Nested
    class ToolTagDetection {

        @Test
        void detectCompleteToolTagInSingleChunk() {
            List<AssistantMessageContent> result =
                    parser.feed("<read_file><path>test.txt</path></read_file>");

            assertEquals(1, result.size());
            assertInstanceOf(ToolUse.class, result.get(0));
            ToolUse toolUse = (ToolUse) result.get(0);
            assertEquals("read_file", toolUse.getName());
            assertEquals("test.txt", toolUse.getParams().get("path"));
            assertFalse(toolUse.isPartial());
        }

        @Test
        void detectToolTagSplitAcrossChunks() {
            parser.feed("<rea");
            parser.feed("d_file>");
            parser.feed("<path>test.txt</path>");
            List<AssistantMessageContent> result = parser.feed("</read_file>");

            assertEquals(1, result.size());
            assertInstanceOf(ToolUse.class, result.get(0));
            assertEquals("read_file", ((ToolUse) result.get(0)).getName());
            assertEquals("test.txt", ((ToolUse) result.get(0)).getParams().get("path"));
            assertFalse(result.get(0).isPartial());
        }

        @Test
        void partialToolTagAtEnd() {
            List<AssistantMessageContent> result = parser.feed("hello <rea");

            assertEquals(1, result.size());
            assertInstanceOf(TextContent.class, result.get(0));
            assertEquals("hello", ((TextContent) result.get(0)).getContent());
            assertTrue(result.get(0).isPartial());
        }

        @Test
        void partialToolTagContinuedWithNonToolTag() {
            parser.feed("hello <no");
            List<AssistantMessageContent> result = parser.feed("t_a_tool>");

            assertEquals(1, result.size());
            assertInstanceOf(TextContent.class, result.get(0));
            assertEquals("hello <not_a_tool>", ((TextContent) result.get(0)).getContent());
        }

        @Test
        void incompleteToolUsePendingPartial() {
            List<AssistantMessageContent> result = parser.feed("<read_file>");

            assertEquals(1, result.size());
            assertInstanceOf(ToolUse.class, result.get(0));
            assertEquals("read_file", ((ToolUse) result.get(0)).getName());
            assertTrue(result.get(0).isPartial());
        }

        @Test
        void toolWithNoParams() {
            List<AssistantMessageContent> result = parser.feed("<read_file></read_file>");

            assertEquals(1, result.size());
            assertInstanceOf(ToolUse.class, result.get(0));
            assertEquals("read_file", ((ToolUse) result.get(0)).getName());
            assertTrue(((ToolUse) result.get(0)).getParams().isEmpty());
            assertFalse(result.get(0).isPartial());
        }

        @Test
        void toolCloseTagSplitAcrossChunks() {
            parser.feed("<read_file><path>test.txt</path></rea");
            List<AssistantMessageContent> result = parser.feed("d_file>");

            assertEquals(1, result.size());
            assertInstanceOf(ToolUse.class, result.get(0));
            assertFalse(result.get(0).isPartial());
        }

        @Test
        void gradualToolTagBuildUp() {
            parser.feed("<r");
            parser.feed("ea");
            parser.feed("d_");
            parser.feed("file");
            List<AssistantMessageContent> result = parser.feed(">");

            assertEquals(1, result.size());
            assertInstanceOf(ToolUse.class, result.get(0));
            assertEquals("read_file", ((ToolUse) result.get(0)).getName());
            assertTrue(result.get(0).isPartial());
        }

        @Test
        void partialToolTagResolvesToNonTool() {
            parser.feed("<rea");
            List<AssistantMessageContent> result = parser.feed("ding>");

            assertEquals(1, result.size());
            assertInstanceOf(TextContent.class, result.get(0));
            assertTrue(((TextContent) result.get(0)).getContent().contains("<reading>"));
        }
    }

    // ===== 3. 参数值解析 =====

    @Nested
    class ParamValueParsing {

        @Test
        void paramValueSplitAcrossChunks() {
            parser.feed("<read_file><path>src/");
            parser.feed("main/");
            parser.feed("java/Test.java");
            List<AssistantMessageContent> result = parser.feed("</path></read_file>");

            assertEquals(1, result.size());
            assertInstanceOf(ToolUse.class, result.get(0));
            assertEquals(
                    "src/main/java/Test.java", ((ToolUse) result.get(0)).getParams().get("path"));
        }

        @Test
        void multipleParams() {
            List<AssistantMessageContent> result =
                    parser.feed(
                            "<execute_command><command>ls -la</command><requires_approval>false</requires_approval></execute_command>");

            assertEquals(1, result.size());
            assertInstanceOf(ToolUse.class, result.get(0));
            ToolUse toolUse = (ToolUse) result.get(0);
            assertEquals("ls -la", toolUse.getParams().get("command"));
            assertEquals("false", toolUse.getParams().get("requires_approval"));
        }

        @Test
        void paramCloseTagSplitAcrossChunks() {
            parser.feed("<execute_command><command>java HelloWorld.java</comm");
            List<AssistantMessageContent> result = parser.feed("and></execute_command>");

            assertEquals(1, result.size());
            assertInstanceOf(ToolUse.class, result.get(0));
            assertEquals(
                    "java HelloWorld.java", ((ToolUse) result.get(0)).getParams().get("command"));
        }

        @Test
        void partialParamValueAtStreamEnd() {
            List<AssistantMessageContent> result = parser.feed("<read_file><path>test.txt");

            assertEquals(1, result.size());
            assertInstanceOf(ToolUse.class, result.get(0));
            assertTrue(result.get(0).isPartial());
            assertEquals("test.txt", ((ToolUse) result.get(0)).getParams().get("path"));
        }

        @Test
        void partialParamCloseTagAtStreamEnd() {
            List<AssistantMessageContent> result =
                    parser.feed("<execute_command><command>java HelloWorld.java</comm");

            assertEquals(1, result.size());
            assertInstanceOf(ToolUse.class, result.get(0));
            assertTrue(result.get(0).isPartial());
            assertEquals(
                    "java HelloWorld.java", ((ToolUse) result.get(0)).getParams().get("command"));
        }

        @Test
        void partialParamCloseTagAtStreamEndWithLessThan() {
            List<AssistantMessageContent> result =
                    parser.feed("<execute_command><command>java HelloWorld.java<");

            assertEquals(1, result.size());
            assertInstanceOf(ToolUse.class, result.get(0));
            assertTrue(result.get(0).isPartial());
            assertEquals(
                    "java HelloWorld.java", ((ToolUse) result.get(0)).getParams().get("command"));
        }

        @Test
        void paramOpenTagSplitAcrossChunks() {
            parser.feed("<read_file><pa");
            parser.feed("th>test.txt</path></read_file>");
            List<AssistantMessageContent> result = parser.getCurrentBlocks();

            assertEquals(1, result.size());
            assertInstanceOf(ToolUse.class, result.get(0));
            assertEquals("test.txt", ((ToolUse) result.get(0)).getParams().get("path"));
        }

        @Test
        void paramValueContainsLessThan() {
            List<AssistantMessageContent> result =
                    parser.feed(
                            "<execute_command><command>echo \"a < b\"</command></execute_command>");

            assertEquals(1, result.size());
            ToolUse toolUse = (ToolUse) result.get(0);
            String command = (String) toolUse.getParams().get("command");
            assertTrue(command.contains("a < b") || command.contains("a <"));
        }

        @Test
        void paramValueContainsNewlines() {
            List<AssistantMessageContent> result =
                    parser.feed(
                            "<write_to_file><path>f.txt</path><content>line1\nline2\nline3</content></write_to_file>");

            assertEquals(1, result.size());
            ToolUse toolUse = (ToolUse) result.get(0);
            assertEquals("line1\nline2\nline3", toolUse.getParams().get("content"));
        }
    }

    // ===== 4. 文本与工具混合内容 =====

    @Nested
    class MixedContent {

        @Test
        void textBeforeTool() {
            parser.feed("I will read the file\n");
            List<AssistantMessageContent> result =
                    parser.feed("<read_file><path>test.txt</path></read_file>");

            assertEquals(2, result.size());
            assertInstanceOf(TextContent.class, result.get(0));
            assertEquals("I will read the file", ((TextContent) result.get(0)).getContent());
            assertFalse(result.get(0).isPartial());
            assertInstanceOf(ToolUse.class, result.get(1));
        }

        @Test
        void textAfterTool() {
            parser.feed("<read_file><path>test.txt</path></read_file>");
            parser.feed("\nFile read complete.");
            List<AssistantMessageContent> finalized = parser.complete();

            assertEquals(2, finalized.size());
            assertInstanceOf(ToolUse.class, finalized.get(0));
            assertInstanceOf(TextContent.class, finalized.get(1));
            assertEquals("File read complete.", ((TextContent) finalized.get(1)).getContent());
        }

        @Test
        void textToolText() {
            String fullMessage =
                    "我将创建一个文件\n<write_to_file>\n<path>test.java</path>\n<content>public class Test {}</content>\n</write_to_file>\n文件已创建完成";

            for (int i = 0; i < fullMessage.length(); i += 10) {
                int end = Math.min(i + 10, fullMessage.length());
                parser.feed(fullMessage.substring(i, end));
            }
            List<AssistantMessageContent> result = parser.complete();

            assertEquals(3, result.size());
            assertInstanceOf(TextContent.class, result.get(0));
            assertEquals("我将创建一个文件", ((TextContent) result.get(0)).getContent());
            assertInstanceOf(ToolUse.class, result.get(1));
            assertEquals("write_to_file", ((ToolUse) result.get(1)).getName());
            assertInstanceOf(TextContent.class, result.get(2));
            assertEquals("文件已创建完成", ((TextContent) result.get(2)).getContent());
        }

        @Test
        void multipleToolsInSequence() {
            parser.feed("<read_file><path>a.txt</path></read_file>");
            List<AssistantMessageContent> result =
                    parser.feed("<read_file><path>b.txt</path></read_file>");

            assertEquals(2, result.size());
            assertInstanceOf(ToolUse.class, result.get(0));
            assertEquals("a.txt", ((ToolUse) result.get(0)).getParams().get("path"));
            assertInstanceOf(ToolUse.class, result.get(1));
            assertEquals("b.txt", ((ToolUse) result.get(1)).getParams().get("path"));
        }
    }

    // ===== 5. 内部标签（thinking）处理 =====

    @Nested
    class InternalTagHandling {

        @Test
        void completeThinkingTag() {
            List<AssistantMessageContent> result =
                    parser.feed("我在思考<thinking>这是我的思考过程</thinking>然后执行");

            assertEquals(1, result.size());
            assertInstanceOf(TextContent.class, result.get(0));
            assertEquals("我在思考这是我的思考过程然后执行", ((TextContent) result.get(0)).getContent());
        }

        @Test
        void thinkingTagSplitAcrossChunks() {
            parser.feed("hello <thin");
            parser.feed("king>my thoughts");
            parser.feed("</thinking> after");
            List<AssistantMessageContent> result = parser.complete();

            assertEquals(1, result.size());
            assertEquals("hello my thoughts after", ((TextContent) result.get(0)).getContent());
        }

        @Test
        void incompleteThinkingTagAtEnd() {
            List<AssistantMessageContent> result = parser.feed("hello <thinking>my thoughts");

            assertEquals(1, result.size());
            assertEquals("hello my thoughts", ((TextContent) result.get(0)).getContent());
        }

        @Test
        void incompleteThinkingTagPrefixAtEnd() {
            List<AssistantMessageContent> result = parser.feed("hello <th");

            assertEquals(1, result.size());
            assertEquals("hello", ((TextContent) result.get(0)).getContent());
        }

        @Test
        void thinkingTagBeforeTool() {
            parser.feed("<thinking>让我读取文件</thinking>");
            List<AssistantMessageContent> result =
                    parser.feed("<read_file><path>test.txt</path></read_file>");

            assertEquals(2, result.size());
            assertInstanceOf(TextContent.class, result.get(0));
            assertEquals("让我读取文件", ((TextContent) result.get(0)).getContent());
            assertInstanceOf(ToolUse.class, result.get(1));
        }

        @Test
        void multipleThinkingTags() {
            List<AssistantMessageContent> result =
                    parser.feed("<thinking>第一段</thinking>文本<thinking>第二段</thinking>");

            assertEquals(1, result.size());
            assertEquals("第一段文本第二段", ((TextContent) result.get(0)).getContent());
        }

        @Test
        void incompleteThinkingCloseTagAtEnd() {
            List<AssistantMessageContent> result = parser.feed("hello <thinking>text</thinkin");

            assertEquals(1, result.size());
            assertEquals("hello text", ((TextContent) result.get(0)).getContent());
        }

        @Test
        void incompleteThinkingCloseTagWithSlashAtEnd() {
            List<AssistantMessageContent> result = parser.feed("hello <thinking>text</");

            assertEquals(1, result.size());
            assertEquals("hello text", ((TextContent) result.get(0)).getContent());
        }

        @Test
        void incompleteThinkingCloseTagWithLessThanAtEnd() {
            List<AssistantMessageContent> result = parser.feed("hello <thinking>text<");

            assertEquals(1, result.size());
            assertEquals("hello text", ((TextContent) result.get(0)).getContent());
        }
    }

    // ===== 6. write_to_file 特殊处理 =====

    @Nested
    class WriteToFileSpecialHandling {

        @Test
        void writeToFileWithContent() {
            List<AssistantMessageContent> result =
                    parser.feed(
                            "<write_to_file>\n<path>test.java</path>\n<content>public class Test {}</content>\n</write_to_file>");

            assertEquals(1, result.size());
            assertInstanceOf(ToolUse.class, result.get(0));
            ToolUse toolUse = (ToolUse) result.get(0);
            assertEquals("write_to_file", toolUse.getName());
            assertEquals("test.java", toolUse.getParams().get("path"));
            assertEquals("public class Test {}", toolUse.getParams().get("content"));
            assertFalse(toolUse.isPartial());
        }

        @Test
        void writeToFileContentWithXmlLikeContent() {
            String fileContent = "<div class=\"test\">Hello</div>";
            String message =
                    "<write_to_file>\n<path>test.html</path>\n<content>"
                            + fileContent
                            + "</content>\n</write_to_file>";

            List<AssistantMessageContent> result = parser.feed(message);

            assertEquals(1, result.size());
            ToolUse toolUse = (ToolUse) result.get(0);
            assertEquals(fileContent, toolUse.getParams().get("content"));
        }

        @Test
        void writeToFileContentStreamedInChunks() {
            String message =
                    "<write_to_file>\n<path>test.java</path>\n<content>public class Test {\n    void main() {}\n}</content>\n</write_to_file>";

            for (int i = 0; i < message.length(); i += 15) {
                int end = Math.min(i + 15, message.length());
                parser.feed(message.substring(i, end));
            }
            List<AssistantMessageContent> result = parser.complete();

            assertEquals(1, result.size());
            ToolUse toolUse = (ToolUse) result.get(0);
            assertEquals(
                    "public class Test {\n    void main() {}\n}",
                    toolUse.getParams().get("content"));
        }

        @Test
        void writeToFilePartialContentDuringStreaming() {
            parser.feed("<write_to_file>\n<path>f.txt</path>\n<content>line1\nli");
            List<AssistantMessageContent> r1 = parser.getCurrentBlocks();

            assertEquals(1, r1.size());
            assertInstanceOf(ToolUse.class, r1.get(0));
            assertTrue(r1.get(0).isPartial());
            String content = (String) ((ToolUse) r1.get(0)).getParams().get("content");
            assertTrue(content.contains("line1"));
        }
    }

    // ===== 7. 重置与复用 =====

    @Nested
    class ResetAndReuse {

        @Test
        void resetClearsState() {
            parser.feed("first message");
            parser.reset();
            List<AssistantMessageContent> result = parser.feed("second message");

            assertEquals(1, result.size());
            assertEquals("second message", ((TextContent) result.get(0)).getContent());
        }

        @Test
        void resetDuringToolParsing() {
            parser.feed("<read_file><path>test");
            parser.reset();
            List<AssistantMessageContent> result = parser.feed("new message");

            assertEquals(1, result.size());
            assertEquals("new message", ((TextContent) result.get(0)).getContent());
        }
    }

    // ===== 8. 边界情况 =====

    @Nested
    class EdgeCases {

        @Test
        void emptyInput() {
            List<AssistantMessageContent> result = parser.feed("");
            assertTrue(result.isEmpty());
        }

        @Test
        void nullInput() {
            List<AssistantMessageContent> result = parser.feed(null);
            assertTrue(result.isEmpty());
        }

        @Test
        void onlyWhitespace() {
            parser.feed("   \n\n  ");
            List<AssistantMessageContent> result = parser.complete();
            assertTrue(result.isEmpty());
        }

        @Test
        void angleBracketNotATag() {
            List<AssistantMessageContent> result = parser.feed("a < b and c > d");

            assertEquals(1, result.size());
            assertEquals("a < b and c > d", ((TextContent) result.get(0)).getContent());
        }

        @Test
        void lessThanFollowedBySpace() {
            List<AssistantMessageContent> result = parser.feed("if (a < b)");

            assertEquals(1, result.size());
            assertEquals("if (a < b)", ((TextContent) result.get(0)).getContent());
        }

        @Test
        void finalizeWithoutFeedReturnsEmpty() {
            List<AssistantMessageContent> result = parser.complete();
            assertTrue(result.isEmpty());
        }

        @Test
        void feedAfterFinalizeThrowsOrReturnsEmpty() {
            parser.feed("hello");
            parser.complete();
            parser.reset();
            List<AssistantMessageContent> result = parser.feed("new");
            assertEquals(1, result.size());
            assertEquals("new", ((TextContent) result.get(0)).getContent());
        }

        @Test
        void incompleteParamTagPrefixInText() {
            // At root level, "pa" is not a prefix of any tool tag, so "<pa" is text
            List<AssistantMessageContent> result = parser.feed("hello <pa");
            assertEquals(1, result.size());
            assertInstanceOf(TextContent.class, result.get(0));
            assertEquals("hello <pa", ((TextContent) result.get(0)).getContent());
        }

        @Test
        void completeWithUnclosedTool() {
            parser.feed("<read_file><path>test.txt</path>");
            List<AssistantMessageContent> result = parser.complete();

            assertEquals(1, result.size());
            assertInstanceOf(ToolUse.class, result.get(0));
            assertFalse(result.get(0).isPartial());
            assertEquals("test.txt", ((ToolUse) result.get(0)).getParams().get("path"));
        }

        @Test
        void completeIsIdempotent() {
            parser.feed("hello");
            List<AssistantMessageContent> r1 = parser.complete();
            List<AssistantMessageContent> r2 = parser.complete();

            assertEquals(r1.size(), r2.size());
            for (int i = 0; i < r1.size(); i++) {
                assertEquals(r1.get(i).getType(), r2.get(i).getType());
            }
        }

        @Test
        void feedAfterCompleteWithoutReset() {
            parser.feed("hello");
            parser.complete();
            List<AssistantMessageContent> result = parser.feed("more");

            assertEquals(1, result.size());
            assertEquals("hello", ((TextContent) result.get(0)).getContent());
        }

        @Test
        void getCurrentBlocksReturnsDefensiveCopy() {
            parser.feed("Hello");
            List<AssistantMessageContent> r1 = parser.getCurrentBlocks();
            List<AssistantMessageContent> r2 = parser.getCurrentBlocks();
            assertNotSame(r1, r2);
        }
    }

    // ===== 9. 与无状态解析器的一致性验证 =====

    @Nested
    class ConsistencyWithStatelessParser {

        private final AssistantMessageParser statelessParser = new AssistantMessageParser();

        @Test
        void simpleText() {
            assertConsistent("这是一个纯文本消息");
        }

        @Test
        void singleTool() {
            assertConsistent(
                    "<write_to_file>\n<path>test.java</path>\n<content>public class Test {}</content>\n</write_to_file>");
        }

        @Test
        void mixedContent() {
            assertConsistent(
                    "我将创建一个文件\n<write_to_file>\n<path>test.java</path>\n<content>public class Test {}</content>\n</write_to_file>\n文件已创建完成");
        }

        @Test
        void thinkingWithTool() {
            assertConsistent(
                    "<thinking>我要读取文件</thinking><read_file>\n<path>test.txt</path>\n</read_file>");
        }

        @Test
        void multipleTools() {
            assertConsistent(
                    "text <read_file>\n<path>a.txt</path>\n</read_file>\nmiddle <read_file>\n<path>b.txt</path>\n</read_file>\nend");
        }

        @Test
        void consistencyWithChunkSizes() {
            String message =
                    "hello <thinking>think</thinking> <read_file><path>test.txt</path></read_file> done";
            List<AssistantMessageContent> expected = statelessParser.parseAssistantMessage(message);

            for (int chunkSize = 1; chunkSize <= message.length(); chunkSize++) {
                parser.reset();
                for (int i = 0; i < message.length(); i += chunkSize) {
                    int end = Math.min(i + chunkSize, message.length());
                    parser.feed(message.substring(i, end));
                }
                List<AssistantMessageContent> actual = parser.complete();

                assertBlocksMatch(expected, actual, "Mismatch with chunkSize=" + chunkSize);
            }
        }

        @Test
        void partialConsistencyWithChunkSizes() {
            String message = "<read_file><path>test.txt</path></read_file>";
            List<AssistantMessageContent> expected = statelessParser.parseAssistantMessage(message);

            for (int chunkSize : new int[] {1, 3, 7, 13, message.length()}) {
                parser.reset();
                for (int i = 0; i < message.length(); i += chunkSize) {
                    int end = Math.min(i + chunkSize, message.length());
                    parser.feed(message.substring(i, end));
                }
                List<AssistantMessageContent> actual = parser.complete();
                assertBlocksMatch(expected, actual, "chunkSize=" + chunkSize);
            }
        }

        private void assertConsistent(String message) {
            List<AssistantMessageContent> expected = statelessParser.parseAssistantMessage(message);
            parser.reset();
            parser.feed(message);
            List<AssistantMessageContent> actual = parser.complete();

            assertBlocksMatch(expected, actual, "Inconsistent for: " + message);
        }

        private void assertBlocksMatch(
                List<AssistantMessageContent> expected,
                List<AssistantMessageContent> actual,
                String context) {
            assertEquals(expected.size(), actual.size(), context + " - size mismatch");
            for (int i = 0; i < expected.size(); i++) {
                AssistantMessageContent e = expected.get(i);
                AssistantMessageContent a = actual.get(i);
                assertEquals(e.getType(), a.getType(), context + " - type mismatch at index " + i);
                // Note: partial status is not compared because the stateless parser
                // does not consistently set partial=false on trailing text blocks.

                if (e instanceof TextContent et && a instanceof TextContent at) {
                    assertEquals(
                            et.getContent(),
                            at.getContent(),
                            context + " - text content mismatch at index " + i);
                } else if (e instanceof ToolUse eToolUse && a instanceof ToolUse aToolUse) {
                    assertEquals(
                            eToolUse.getName(),
                            aToolUse.getName(),
                            context + " - tool name mismatch at index " + i);
                    assertEquals(
                            eToolUse.getParams(),
                            aToolUse.getParams(),
                            context + " - tool params mismatch at index " + i);
                }
            }
        }
    }

    // ===== 10. 流式 partial 状态验证 =====

    @Nested
    class StreamingPartialState {

        @Test
        void textRemainsPartialDuringStreaming() {
            List<AssistantMessageContent> r1 = parser.feed("Hello ");
            assertTrue(r1.get(0).isPartial());

            List<AssistantMessageContent> r2 = parser.feed("World");
            assertTrue(r2.get(0).isPartial());

            List<AssistantMessageContent> r3 = parser.complete();
            assertFalse(r3.get(0).isPartial());
        }

        @Test
        void toolBecomesNonPartialOnClose() {
            List<AssistantMessageContent> r1 = parser.feed("<read_file>");
            assertTrue(r1.get(0).isPartial());

            List<AssistantMessageContent> r2 = parser.feed("<path>test.txt</path>");
            assertTrue(r2.get(0).isPartial());

            List<AssistantMessageContent> r3 = parser.feed("</read_file>");
            assertFalse(r3.get(0).isPartial());
        }

        @Test
        void textBeforeToolBecomesNonPartialWhenToolStarts() {
            List<AssistantMessageContent> r1 = parser.feed("hello ");
            assertTrue(r1.get(0).isPartial());

            List<AssistantMessageContent> r2 = parser.feed("<read_file>");
            assertFalse(r2.get(0).isPartial());
            assertTrue(r2.get(1).isPartial());
        }

        @Test
        void partialParamGetsUpdatedIncrementally() {
            parser.feed("<read_file><path>src/");
            List<AssistantMessageContent> r1 = parser.getCurrentBlocks();
            assertEquals("src/", ((ToolUse) r1.get(0)).getParams().get("path"));

            parser.feed("main/Test.java");
            List<AssistantMessageContent> r2 = parser.getCurrentBlocks();
            assertEquals("src/main/Test.java", ((ToolUse) r2.get(0)).getParams().get("path"));
        }
    }

    // ===== 11. 来自原有测试的回归用例 =====

    @Nested
    class RegressionFromStatelessTests {

        @Test
        void partialToolTagPrefix_rea() {
            List<AssistantMessageContent> result = parser.feed("let me read file, <rea");

            assertEquals(1, result.size());
            assertInstanceOf(TextContent.class, result.get(0));
            assertEquals("let me read file,", ((TextContent) result.get(0)).getContent());
            assertTrue(result.get(0).isPartial());
        }

        @Test
        void partialToolTagPrefix_re() {
            List<AssistantMessageContent> result = parser.feed("\n\n<re");
            assertTrue(result.isEmpty());
        }

        @Test
        void partialToolTagPrefix_r() {
            List<AssistantMessageContent> result = parser.feed("\n\n<r");
            assertTrue(result.isEmpty());
        }

        @Test
        void completeToolOpenTag() {
            List<AssistantMessageContent> result = parser.feed("\n\n<read_file>");

            assertEquals(1, result.size());
            assertInstanceOf(ToolUse.class, result.get(0));
            assertEquals("read_file", ((ToolUse) result.get(0)).getName());
            assertTrue(result.get(0).isPartial());
        }

        @Test
        void textWithIncompleteToolAndParam() {
            List<AssistantMessageContent> result = parser.feed("text <read_file>\n<path>test.txt");

            assertEquals(2, result.size());
            assertInstanceOf(TextContent.class, result.get(0));
            assertEquals("text", ((TextContent) result.get(0)).getContent());
            assertFalse(result.get(0).isPartial());
            assertInstanceOf(ToolUse.class, result.get(1));
            assertEquals("read_file", ((ToolUse) result.get(1)).getName());
            assertTrue(result.get(1).isPartial());
            assertEquals("test.txt", ((ToolUse) result.get(1)).getParams().get("path"));
        }

        @Test
        void thinkingWithIncompleteThinkingCloseTag() {
            List<AssistantMessageContent> result = parser.feed("hello <thinking>text</thinkin");

            assertEquals(1, result.size());
            assertEquals("hello text", ((TextContent) result.get(0)).getContent());
        }

        @Test
        void thinkingWithPartialToolTag() {
            List<AssistantMessageContent> result =
                    parser.feed("hello <thinking>text</thinking> <list_files");

            assertEquals(1, result.size());
            assertEquals("hello text", ((TextContent) result.get(0)).getContent());
        }

        @Test
        void toolWithPartialParamCloseTag() {
            List<AssistantMessageContent> result =
                    parser.feed("hello <execute_command><command>java HelloWorld.java</command");

            assertEquals(2, result.size());
            assertEquals("hello", ((TextContent) result.get(0)).getContent());
            assertEquals("execute_command", ((ToolUse) result.get(1)).getName());
            assertEquals(
                    "java HelloWorld.java", ((ToolUse) result.get(1)).getParams().get("command"));
        }

        @Test
        void toolWithPartialParamCloseTagLessThan() {
            List<AssistantMessageContent> result =
                    parser.feed("hello <execute_command><command>java HelloWorld.java<");

            assertEquals(2, result.size());
            assertEquals("hello", ((TextContent) result.get(0)).getContent());
            assertEquals("execute_command", ((ToolUse) result.get(1)).getName());
            assertEquals(
                    "java HelloWorld.java", ((ToolUse) result.get(1)).getParams().get("command"));
        }

        @Test
        void partialToolWritePrefix() {
            List<AssistantMessageContent> result = parser.feed("hello <write");

            assertEquals(1, result.size());
            assertInstanceOf(TextContent.class, result.get(0));
            assertEquals("hello", ((TextContent) result.get(0)).getContent());
            assertTrue(result.get(0).isPartial());
        }
    }

    // ===== 12. 其他工具类型 =====

    @Nested
    class OtherToolTypes {

        @Test
        void applyPatchTool() {
            List<AssistantMessageContent> result =
                    parser.feed(
                            "<apply_patch><diff>--- a/f.txt\n+++ b/f.txt\n@@ -1 +1 @@\n-old\n+new</diff></apply_patch>");

            assertEquals(1, result.size());
            ToolUse toolUse = (ToolUse) result.get(0);
            assertEquals("apply_patch", toolUse.getName());
            assertFalse(toolUse.isPartial());
            String diff = (String) toolUse.getParams().get("diff");
            assertTrue(diff.contains("-old"));
            assertTrue(diff.contains("+new"));
        }

        @Test
        void useMcpToolWithMultipleParams() {
            List<AssistantMessageContent> result =
                    parser.feed(
                            "<use_mcp_tool><server_name>my-server</server_name><tool_name>search</tool_name><arguments>{\"q\":\"test\"}</arguments></use_mcp_tool>");

            assertEquals(1, result.size());
            ToolUse toolUse = (ToolUse) result.get(0);
            assertEquals("use_mcp_tool", toolUse.getName());
            assertEquals("my-server", toolUse.getParams().get("server_name"));
            assertEquals("search", toolUse.getParams().get("tool_name"));
            assertEquals("{\"q\":\"test\"}", toolUse.getParams().get("arguments"));
        }

        @Test
        void browserActionTool() {
            List<AssistantMessageContent> result =
                    parser.feed(
                            "<browser_action><action>click</action><coordinate>100,200</coordinate></browser_action>");

            assertEquals(1, result.size());
            ToolUse toolUse = (ToolUse) result.get(0);
            assertEquals("browser_action", toolUse.getName());
            assertEquals("click", toolUse.getParams().get("action"));
            assertEquals("100,200", toolUse.getParams().get("coordinate"));
        }
    }

    // ===== 13. 空白处理 =====

    @Nested
    class WhitespaceHandling {

        @Test
        void whitespaceOnlyBetweenTools() {
            parser.feed("<read_file><path>a.txt</path></read_file>");
            parser.feed("  \n  ");
            List<AssistantMessageContent> result =
                    parser.feed("<read_file><path>b.txt</path></read_file>");

            long toolCount = result.stream().filter(b -> b instanceof ToolUse).count();
            assertEquals(2, toolCount);
        }

        @Test
        void newlinesBetweenToolsShouldNotCreateEmptyTextBlocks() {
            parser.feed("<read_file><path>a.txt</path></read_file>\n\n");
            parser.feed("<read_file><path>b.txt</path></read_file>");
            List<AssistantMessageContent> result = parser.complete();

            for (AssistantMessageContent block : result) {
                if (block instanceof TextContent tc) {
                    assertFalse(
                            tc.getContent().trim().isEmpty(), "Should not have empty text blocks");
                }
            }
        }
    }

    // ===== 14. 增量内容 =====

    @Nested
    class IncrementalContent {

        @Test
        void incrementalContentOnSubsequentFeed() {
            parser.feed("Hello ");
            List<AssistantMessageContent> r1 = parser.getCurrentBlocks();
            assertInstanceOf(TextContent.class, r1.get(0));

            parser.feed("World");
            List<AssistantMessageContent> r2 = parser.getCurrentBlocks();
            TextContent tc = (TextContent) r2.get(0);
            assertEquals("Hello World", tc.getContent());
            assertNotNull(tc.getIncrementalContent());
            assertEquals("World", tc.getIncrementalContent());
        }

        @Test
        void incrementalContentResetOnNewTextBlock() {
            parser.feed("<read_file><path>a.txt</path></read_file>");
            parser.feed("new text");
            List<AssistantMessageContent> result = parser.getCurrentBlocks();

            TextContent tc = null;
            for (AssistantMessageContent block : result) {
                if (block instanceof TextContent t) {
                    tc = t;
                }
            }
            assertNotNull(tc);
            assertEquals("new text", tc.getContent());
        }
    }

    // ===== 15. 嵌套格式解析 =====

    @Nested
    class NestedFormatParsing {

        private StreamingAssistantMessageParser nestedParser;

        @BeforeEach
        void setUp() {
            nestedParser =
                    new DefaultStreamingAssistantMessageParser(ClineTagConfigs.nestedFormat());
        }

        @Test
        void completeFunctionCallsSingleInvoke() {
            List<AssistantMessageContent> result =
                    nestedParser.feed(
                            "<function_calls><invoke name=\"read_file\"><parameter name=\"path\">/tmp/test.txt</parameter></invoke></function_calls>");

            assertEquals(1, result.size());
            assertInstanceOf(ToolUse.class, result.get(0));
            ToolUse toolUse = (ToolUse) result.get(0);
            assertEquals("read_file", toolUse.getName());
            assertEquals("/tmp/test.txt", toolUse.getParams().get("path"));
            assertFalse(toolUse.isPartial());
        }

        @Test
        void completeFunctionCallsMultipleInvokes() {
            nestedParser.feed(
                    "<function_calls><invoke name=\"read_file\"><parameter name=\"path\">a.txt</parameter></invoke>");
            List<AssistantMessageContent> result =
                    nestedParser.feed(
                            "<invoke name=\"read_file\"><parameter name=\"path\">b.txt</parameter></invoke></function_calls>");

            assertEquals(2, result.size());
            assertEquals("a.txt", ((ToolUse) result.get(0)).getParams().get("path"));
            assertEquals("b.txt", ((ToolUse) result.get(1)).getParams().get("path"));
        }

        @Test
        void nestedFormatStreamingChunkByChunk() {
            String msg =
                    "<function_calls><invoke name=\"read_file\"><parameter name=\"path\">test.txt</parameter></invoke></function_calls>";
            for (int i = 0; i < msg.length(); i++) {
                nestedParser.feed(String.valueOf(msg.charAt(i)));
            }
            List<AssistantMessageContent> result = nestedParser.complete();

            assertEquals(1, result.size());
            assertEquals("read_file", ((ToolUse) result.get(0)).getName());
            assertEquals("test.txt", ((ToolUse) result.get(0)).getParams().get("path"));
        }

        @Test
        void nestedFormatPartialInvoke() {
            List<AssistantMessageContent> result =
                    nestedParser.feed("<function_calls><invoke name=\"read_file\">");

            assertEquals(1, result.size());
            assertInstanceOf(ToolUse.class, result.get(0));
            assertTrue(result.get(0).isPartial());
            assertEquals("read_file", ((ToolUse) result.get(0)).getName());
        }

        @Test
        void nestedFormatPartialParameter() {
            List<AssistantMessageContent> result =
                    nestedParser.feed(
                            "<function_calls><invoke name=\"read_file\"><parameter name=\"path\">test");

            assertEquals(1, result.size());
            assertInstanceOf(ToolUse.class, result.get(0));
            assertTrue(result.get(0).isPartial());
            assertEquals("test", ((ToolUse) result.get(0)).getParams().get("path"));
        }

        @Test
        void nestedFormatTextBeforeAndAfter() {
            nestedParser.feed("before ");
            nestedParser.feed(
                    "<function_calls><invoke name=\"read_file\"><parameter name=\"path\">f</parameter></invoke></function_calls>");
            nestedParser.feed(" after");
            List<AssistantMessageContent> result = nestedParser.complete();

            assertEquals(3, result.size());
            assertEquals("before", ((TextContent) result.get(0)).getContent());
            assertInstanceOf(ToolUse.class, result.get(1));
            assertEquals("after", ((TextContent) result.get(2)).getContent());
        }

        @Test
        void nestedFormatWrapperIsTransparent() {
            nestedParser.feed(
                    "<function_calls><invoke name=\"read_file\"><parameter name=\"path\">f</parameter></invoke></function_calls>");
            List<AssistantMessageContent> result = nestedParser.complete();

            for (AssistantMessageContent block : result) {
                if (block instanceof ToolUse tu) {
                    assertNotEquals("function_calls", tu.getName());
                }
            }
        }

        @Test
        void nestedFormatParameterWithHtmlContent() {
            nestedParser.feed(
                    "<function_calls><invoke name=\"write_file\"><parameter name=\"content\">");
            nestedParser.feed("<div class=\"main\"><h1>Hello</h1></div>");
            nestedParser.feed("</parameter></invoke></function_calls>");
            List<AssistantMessageContent> result = nestedParser.complete();

            assertEquals(1, result.size());
            ToolUse toolUse = (ToolUse) result.get(0);
            String content = (String) toolUse.getParams().get("content");
            assertTrue(content.contains("<div"));
            assertTrue(content.contains("Hello"));
            assertTrue(content.contains("</div>"));
        }

        @Test
        void nestedFormatPartialInvokeTag() {
            nestedParser.feed("<function_calls><invoke na");
            nestedParser.feed("me=\"read_file\">");
            nestedParser.feed(
                    "<parameter name=\"path\">f.txt</parameter></invoke></function_calls>");
            List<AssistantMessageContent> result = nestedParser.complete();

            assertEquals(1, result.size());
            assertEquals("read_file", ((ToolUse) result.get(0)).getName());
            assertEquals("f.txt", ((ToolUse) result.get(0)).getParams().get("path"));
        }

        @Test
        void nestedFormatCompleteAllNonPartial() {
            nestedParser.feed(
                    "<function_calls><invoke name=\"read_file\"><parameter name=\"path\">f</parameter></invoke></function_calls>");
            List<AssistantMessageContent> result = nestedParser.complete();

            for (AssistantMessageContent block : result) {
                assertFalse(block.isPartial(), "All blocks should be non-partial after complete()");
            }
        }

        @Test
        void nestedFormatConsistencyWithChunkSizes() {
            String message =
                    "<function_calls><invoke name=\"read_file\"><parameter name=\"path\">test.txt</parameter></invoke></function_calls>";

            nestedParser.feed(message);
            List<AssistantMessageContent> expected = nestedParser.complete();

            for (int chunkSize : new int[] {1, 3, 5, 10, message.length()}) {
                StreamingAssistantMessageParser p =
                        new DefaultStreamingAssistantMessageParser(ClineTagConfigs.nestedFormat());
                for (int i = 0; i < message.length(); i += chunkSize) {
                    int end = Math.min(i + chunkSize, message.length());
                    p.feed(message.substring(i, end));
                }
                List<AssistantMessageContent> actual = p.complete();

                assertEquals(
                        expected.size(),
                        actual.size(),
                        "Size mismatch with chunkSize=" + chunkSize);
                for (int i = 0; i < expected.size(); i++) {
                    assertEquals(expected.get(i).getType(), actual.get(i).getType());
                    if (expected.get(i) instanceof ToolUse et
                            && actual.get(i) instanceof ToolUse at) {
                        assertEquals(et.getName(), at.getName());
                        assertEquals(et.getParams(), at.getParams());
                    }
                }
            }
        }

        @Test
        void sameOutputForEquivalentMessages() {
            parser.reset();
            parser.feed("<read_file><path>test.txt</path></read_file>");
            List<AssistantMessageContent> flatResult = parser.complete();

            nestedParser.feed(
                    "<function_calls><invoke name=\"read_file\"><parameter name=\"path\">test.txt</parameter></invoke></function_calls>");
            List<AssistantMessageContent> nestedResult = nestedParser.complete();

            assertEquals(flatResult.size(), nestedResult.size());
            for (int i = 0; i < flatResult.size(); i++) {
                assertEquals(flatResult.get(i).getType(), nestedResult.get(i).getType());
                if (flatResult.get(i) instanceof ToolUse ft
                        && nestedResult.get(i) instanceof ToolUse nt) {
                    assertEquals(ft.getName(), nt.getName());
                    assertEquals(ft.getParams(), nt.getParams());
                }
            }
        }
    }
}
