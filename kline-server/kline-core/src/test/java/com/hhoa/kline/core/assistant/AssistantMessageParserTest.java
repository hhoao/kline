package com.hhoa.kline.core.assistant;

import static org.junit.jupiter.api.Assertions.*;

import com.hhoa.kline.core.core.assistant.AssistantMessageContent;
import com.hhoa.kline.core.core.assistant.TextContent;
import com.hhoa.kline.core.core.assistant.ToolUse;
import com.hhoa.kline.core.core.assistant.parser.AssistantMessageParser;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * 助手消息解析器测试
 *
 * @author hhoa
 */
class AssistantMessageParserTest {
    private final AssistantMessageParser messageParser = new AssistantMessageParser();

    @Test
    void testPartialMessage() {
        // 测试案例1: "let me read file, <rea" - 应该解析为单个TextContent，内容为"let me read file,"，partial=true
        List<AssistantMessageContent> result1 =
                messageParser.parseAssistantMessage("let me read file, <rea", true);
        assertEquals(1, result1.size());
        assertInstanceOf(TextContent.class, result1.get(0));
        assertEquals("let me read file,", ((TextContent) result1.get(0)).getContent());
        assertTrue(result1.get(0).isPartial()); // partial 模式下，文本内容标记为 partial=true

        // 测试案例2: "\n\n<re" - 应该解析为空（去掉<re后只剩空白）
        List<AssistantMessageContent> result2 =
                messageParser.parseAssistantMessage("\n\n<re", true);
        assertEquals(0, result2.size());

        // 测试案例3: "\n\n<r" - 应该解析为空
        List<AssistantMessageContent> result3 = messageParser.parseAssistantMessage("\n\n<r", true);
        assertEquals(0, result3.size());

        // 测试案例4: "\n\n<read_file>" - 完整的工具标签开始，应该解析为ToolUse（partial=true）
        List<AssistantMessageContent> result4 =
                messageParser.parseAssistantMessage("\n\n<read_file>", true);
        assertEquals(1, result4.size());
        assertInstanceOf(ToolUse.class, result4.get(0));
        assertEquals("read_file", ((ToolUse) result4.get(0)).getName());
        assertTrue(result4.get(0).isPartial()); // 未闭合的工具应该标记为partial=true

        // 测试案例5: "let me read file, <rea" with isPartial=false - 应该保留不完整标签
        List<AssistantMessageContent> result5 =
                messageParser.parseAssistantMessage("let me read file, <rea", false);
        assertEquals(1, result5.size());
        assertInstanceOf(TextContent.class, result5.get(0));
        assertEquals("let me read file, <rea", ((TextContent) result5.get(0)).getContent());

        // 测试案例6: "hello <write" - 应该解析为"hello"，partial=true
        List<AssistantMessageContent> result6 =
                messageParser.parseAssistantMessage("hello <write", true);
        assertEquals(1, result6.size());
        assertInstanceOf(TextContent.class, result6.get(0));
        assertEquals("hello", ((TextContent) result6.get(0)).getContent());
        assertTrue(result6.get(0).isPartial());

        // 测试案例7: "text <read_file>\n<path>test.txt" - 不完整的工具使用
        List<AssistantMessageContent> result7 =
                messageParser.parseAssistantMessage("text <read_file>\n<path>test.txt", true);
        assertEquals(2, result7.size());
        assertInstanceOf(TextContent.class, result7.get(0));
        assertEquals("text", ((TextContent) result7.get(0)).getContent());
        assertFalse(result7.get(0).isPartial()); // 后面有工具，所以 partial=false
        assertInstanceOf(ToolUse.class, result7.get(1));
        assertEquals("read_file", ((ToolUse) result7.get(1)).getName());
        assertTrue(result7.get(1).isPartial());
        assertEquals("test.txt", ((ToolUse) result7.get(1)).getParams().get("path"));

        List<AssistantMessageContent> result8 =
                messageParser.parseAssistantMessage("hello <thinking>text</thinkin", true);
        assertEquals("hello text", ((TextContent) result8.get(0)).getContent());

        List<AssistantMessageContent> result9 =
                messageParser.parseAssistantMessage("hello <thinking>text</thinking", true);
        assertEquals("hello text", ((TextContent) result9.get(0)).getContent());

        List<AssistantMessageContent> result10 =
                messageParser.parseAssistantMessage("hello <thinking>text</", true);
        assertEquals("hello text", ((TextContent) result10.get(0)).getContent());

        List<AssistantMessageContent> result11 =
                messageParser.parseAssistantMessage("hello <thinking>text<", true);
        assertEquals("hello text", ((TextContent) result11.get(0)).getContent());

        List<AssistantMessageContent> result12 =
                messageParser.parseAssistantMessage(
                        "hello <thinking>text</thinking> <list_files", true);
        assertEquals("hello text", ((TextContent) result12.getFirst()).getContent());

        List<AssistantMessageContent> result13 =
                messageParser.parseAssistantMessage(
                        "hello <execute_command><command>java HelloWorld.java</command", true);
        assertEquals("hello", ((TextContent) result13.getFirst()).getContent());
        assertEquals("execute_command", ((ToolUse) result13.get(1)).getName());
        assertEquals(
                "java HelloWorld.java", ((ToolUse) result13.get(1)).getParams().get("command"));

        List<AssistantMessageContent> result14 =
                messageParser.parseAssistantMessage(
                        "hello <execute_command><command>java HelloWorld.java<", true);
        assertEquals("hello", ((TextContent) result14.getFirst()).getContent());
        assertEquals("execute_command", ((ToolUse) result14.get(1)).getName());
        assertEquals(
                "java HelloWorld.java", ((ToolUse) result14.get(1)).getParams().get("command"));
    }

    @Test
    void testParseTextOnly() {
        String message = "这是一个纯文本消息";
        List<AssistantMessageContent> contents = messageParser.parseAssistantMessage(message);

        assertEquals(1, contents.size());
        assertInstanceOf(TextContent.class, contents.get(0));
        assertEquals("text", contents.get(0).getType());
        assertEquals("这是一个纯文本消息", ((TextContent) contents.get(0)).getContent());
        assertTrue(contents.get(0).isPartial());
    }

    @Test
    void testParseToolUse() {
        String message =
                "<write_to_file>\n<path>test.java</path>\n<content>public class Test {}</content>\n</write_to_file>";
        List<AssistantMessageContent> contents = messageParser.parseAssistantMessage(message);

        assertEquals(1, contents.size());
        assertInstanceOf(ToolUse.class, contents.get(0));
        assertEquals("tool_use", contents.get(0).getType());

        ToolUse toolUse = (ToolUse) contents.get(0);
        assertEquals("write_to_file", toolUse.getName());
        assertEquals("test.java", toolUse.getParams().get("path"));
        assertEquals("public class Test {}", toolUse.getParams().get("content"));
        assertFalse(toolUse.isPartial());
    }

    @Test
    void testParseMixedContent() {
        String message =
                "我将创建一个文件\n<write_to_file>\n<path>test.java</path>\n<content>public class Test {}</content>\n</write_to_file>\n文件已创建完成";
        List<AssistantMessageContent> contents = messageParser.parseAssistantMessage(message);

        assertEquals(3, contents.size());

        // 第一个文本块
        assertInstanceOf(TextContent.class, contents.get(0));
        assertEquals("我将创建一个文件", ((TextContent) contents.get(0)).getContent());

        // 工具使用
        assertInstanceOf(ToolUse.class, contents.get(1));
        assertEquals("write_to_file", ((ToolUse) contents.get(1)).getName());

        // 第二个文本块
        assertInstanceOf(TextContent.class, contents.get(2));
        assertEquals("文件已创建完成", ((TextContent) contents.get(2)).getContent());
    }

    @Test
    void testParseEmptyMessage() {
        List<AssistantMessageContent> contents = messageParser.parseAssistantMessage("");
        assertTrue(contents.isEmpty());
    }

    @Test
    void testParseNullMessage() {
        List<AssistantMessageContent> contents = messageParser.parseAssistantMessage(null);
        assertTrue(contents.isEmpty());
    }

    @Test
    void testInternalTags() {
        // 测试案例1: 完整的 thinking 标签应该被移除，但保留内容
        String message1 = "我在思考<thinking>这是我的思考过程</thinking>然后执行";
        List<AssistantMessageContent> result1 = messageParser.parseAssistantMessage(message1);
        assertEquals(1, result1.size());
        assertInstanceOf(TextContent.class, result1.get(0));
        assertEquals("我在思考这是我的思考过程然后执行", ((TextContent) result1.get(0)).getContent());

        // 测试案例2: 多个 thinking 标签
        String message2 = "<thinking>第一段思考</thinking>文本<thinking>第二段思考</thinking>";
        List<AssistantMessageContent> result2 = messageParser.parseAssistantMessage(message2);
        assertEquals(1, result2.size());
        assertEquals("第一段思考文本第二段思考", ((TextContent) result2.get(0)).getContent());

        // 测试案例3: isPartial=true 时，不完整的 thinking 标签应该被移除
        String message3 = "我在思考<thinking>这是我的思考";
        List<AssistantMessageContent> result3 = messageParser.parseAssistantMessage(message3, true);
        assertEquals(1, result3.size());
        assertEquals("我在思考这是我的思考", ((TextContent) result3.get(0)).getContent());

        // 测试案例4: isPartial=true 时，不完整的标签前缀应该被移除 "<th"
        String message4 = "我在思考<th";
        List<AssistantMessageContent> result4 = messageParser.parseAssistantMessage(message4, true);
        assertEquals(1, result4.size());
        assertEquals("我在思考", ((TextContent) result4.get(0)).getContent());

        // 测试案例5: thinking 标签和工具混合使用
        String message5 =
                "<thinking>我要读取文件</thinking><read_file>\n<path>test.txt</path>\n</read_file>";
        List<AssistantMessageContent> result5 = messageParser.parseAssistantMessage(message5);
        assertEquals(2, result5.size());
        assertInstanceOf(TextContent.class, result5.get(0));
        assertEquals("我要读取文件", ((TextContent) result5.get(0)).getContent());
        assertInstanceOf(ToolUse.class, result5.get(1));
        assertEquals("read_file", ((ToolUse) result5.get(1)).getName());

        // 测试案例6: isPartial=false 时，不完整的 thinking 标签保留开始标签
        String message6 = "我在思考<thinking>这是我的思考";
        List<AssistantMessageContent> result6 =
                messageParser.parseAssistantMessage(message6, false);
        assertEquals(1, result6.size());
        // 在 non-partial 模式下，未闭合的 thinking 标签会移除开始标签但保留内容
        assertEquals("我在思考这是我的思考", ((TextContent) result6.get(0)).getContent());

        List<AssistantMessageContent> contents = messageParser.parseAssistantMessage("<thinking");
        assertTrue(contents.isEmpty());

        String message7 = "我在思考<thinking>这是我的思考</think";
        List<AssistantMessageContent> result7 =
                messageParser.parseAssistantMessage(message7, false);
        assertEquals(1, result7.size());
        assertEquals("我在思考这是我的思考", ((TextContent) result7.getFirst()).getContent());
    }
}
