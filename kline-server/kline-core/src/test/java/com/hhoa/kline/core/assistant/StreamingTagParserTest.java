package com.hhoa.kline.core.assistant;

import static org.junit.jupiter.api.Assertions.*;

import com.hhoa.kline.core.core.assistant.parser.ClineTagConfigs;
import com.hhoa.kline.core.core.assistant.parser.StreamingTagParser;
import com.hhoa.kline.core.core.assistant.parser.TagEventHandler;
import com.hhoa.kline.core.core.assistant.parser.TagHierarchyConfig;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Layer 1 测试：StreamingTagParser
 *
 * @author hhoa
 */
class StreamingTagParserTest {

    private RecordingHandler handler;
    private StreamingTagParser parser;

    @BeforeEach
    void setUp() {
        handler = new RecordingHandler();
        parser = new StreamingTagParser(ClineTagConfigs.flatFormat(), handler);
    }

    // ==================== 录制回调处理器 ====================

    enum EventType {
        START,
        END,
        CHARS
    }

    record Event(EventType type, String tagName, Map<String, String> attrs, String text) {}

    static class RecordingHandler implements TagEventHandler {
        final List<Event> events = new ArrayList<>();

        @Override
        public void onStartElement(String tagName, Map<String, String> attributes) {
            events.add(new Event(EventType.START, tagName, Map.copyOf(attributes), null));
        }

        @Override
        public void onEndElement(String tagName) {
            events.add(new Event(EventType.END, tagName, null, null));
        }

        @Override
        public void onCharacters(String text) {
            events.add(new Event(EventType.CHARS, null, null, text));
        }

        String allText() {
            return events.stream()
                    .filter(e -> e.type == EventType.CHARS)
                    .map(Event::text)
                    .collect(Collectors.joining());
        }

        List<Event> starts() {
            return events.stream().filter(e -> e.type == EventType.START).toList();
        }

        List<Event> ends() {
            return events.stream().filter(e -> e.type == EventType.END).toList();
        }

        void clear() {
            events.clear();
        }
    }

    // ===== A. 基础文本 =====

    @Nested
    class BasicText {

        @Test
        void pureText() {
            parser.feed("Hello world, no tags here.");
            parser.endOfInput();
            assertEquals("Hello world, no tags here.", handler.allText());
            assertTrue(handler.starts().isEmpty());
            assertTrue(handler.ends().isEmpty());
        }

        @Test
        void textWithAngleBrackets() {
            parser.feed("a < b > c");
            parser.endOfInput();
            assertEquals("a < b > c", handler.allText());
        }

        @Test
        void emptyInput() {
            parser.feed("");
            parser.endOfInput();
            assertTrue(handler.events.isEmpty());
        }

        @Test
        void multipleTextChunks() {
            parser.feed("Hello ");
            parser.feed("world ");
            parser.feed("!");
            parser.endOfInput();
            assertEquals("Hello world !", handler.allText());
        }

        @Test
        void textWithNewlines() {
            parser.feed("line1\nline2\r\nline3");
            parser.endOfInput();
            assertEquals("line1\nline2\r\nline3", handler.allText());
        }
    }

    // ===== B. 结构标签检测 =====

    @Nested
    class StructuralTagDetection {

        @Test
        void singleStructuralTag() {
            parser.feed("<read_file>content</read_file>");
            parser.endOfInput();
            assertEquals(1, handler.starts().size());
            assertEquals("read_file", handler.starts().get(0).tagName());
            assertEquals(1, handler.ends().size());
            assertEquals("read_file", handler.ends().get(0).tagName());
            assertEquals("content", handler.allText());
        }

        @Test
        void structuralTagWithAttributes() {
            TagHierarchyConfig nestedConfig = ClineTagConfigs.nestedFormat();
            RecordingHandler h = new RecordingHandler();
            StreamingTagParser p = new StreamingTagParser(nestedConfig, h);
            p.feed("<function_calls><invoke name=\"read_file\"></invoke></function_calls>");
            p.endOfInput();
            Event invokeStart =
                    h.starts().stream()
                            .filter(e -> "invoke".equals(e.tagName()))
                            .findFirst()
                            .orElseThrow();
            assertEquals("read_file", invokeStart.attrs().get("name"));
        }

        @Test
        void structuralTagSplitAcrossChunks() {
            parser.feed("<rea");
            parser.feed("d_file>");
            parser.feed("content");
            parser.feed("</read_file>");
            parser.endOfInput();
            assertEquals(1, handler.starts().size());
            assertEquals("read_file", handler.starts().get(0).tagName());
            assertEquals("content", handler.allText());
        }

        @Test
        void structuralCloseTagSplitAcrossChunks() {
            parser.feed("<read_file>content</read_");
            parser.feed("file>");
            parser.endOfInput();
            assertEquals(1, handler.ends().size());
            assertEquals("read_file", handler.ends().get(0).tagName());
        }

        @Test
        void attributeSplitAcrossChunks() {
            TagHierarchyConfig nestedConfig = ClineTagConfigs.nestedFormat();
            RecordingHandler h = new RecordingHandler();
            StreamingTagParser p = new StreamingTagParser(nestedConfig, h);
            p.feed("<function_calls><invoke na");
            p.feed("me=\"rea");
            p.feed("d_file\">");
            p.feed("</invoke></function_calls>");
            p.endOfInput();
            Event invokeStart =
                    h.starts().stream()
                            .filter(e -> "invoke".equals(e.tagName()))
                            .findFirst()
                            .orElseThrow();
            assertEquals("read_file", invokeStart.attrs().get("name"));
        }

        @Test
        void gradualTagBuildUp() {
            parser.feed("<r");
            parser.feed("ea");
            parser.feed("d_");
            parser.feed("file");
            parser.feed(">");
            parser.feed("x");
            parser.feed("</read_file>");
            parser.endOfInput();
            assertEquals(1, handler.starts().size());
            assertEquals("read_file", handler.starts().get(0).tagName());
            assertEquals("x", handler.allText());
        }

        @Test
        void multipleAttributesOnTag() {
            TagHierarchyConfig nestedConfig = ClineTagConfigs.nestedFormat();
            RecordingHandler h = new RecordingHandler();
            StreamingTagParser p = new StreamingTagParser(nestedConfig, h);
            p.feed("<function_calls><invoke name=\"tool\" id=\"123\"></invoke></function_calls>");
            p.endOfInput();
            Event invokeStart =
                    h.starts().stream()
                            .filter(e -> "invoke".equals(e.tagName()))
                            .findFirst()
                            .orElseThrow();
            assertEquals("tool", invokeStart.attrs().get("name"));
            assertEquals("123", invokeStart.attrs().get("id"));
        }

        @Test
        void singleQuotedAttribute() {
            TagHierarchyConfig nestedConfig = ClineTagConfigs.nestedFormat();
            RecordingHandler h = new RecordingHandler();
            StreamingTagParser p = new StreamingTagParser(nestedConfig, h);
            p.feed("<function_calls><invoke name='tool'></invoke></function_calls>");
            p.endOfInput();
            Event invokeStart =
                    h.starts().stream()
                            .filter(e -> "invoke".equals(e.tagName()))
                            .findFirst()
                            .orElseThrow();
            assertEquals("tool", invokeStart.attrs().get("name"));
        }
    }

    // ===== C. 非结构标签文本还原 =====

    @Nested
    class NonStructuralTagTextRestore {

        @Test
        void nonStructuralTagAsText() {
            parser.feed("<div>hello</div>");
            parser.endOfInput();
            String text = handler.allText();
            assertTrue(text.contains("<div>"));
            assertTrue(text.contains("hello"));
            assertTrue(text.contains("</div>"));
            assertTrue(handler.starts().isEmpty());
        }

        @Test
        void incompleteNonStructuralTag() {
            parser.feed("<div>hello");
            parser.endOfInput();
            String text = handler.allText();
            assertTrue(text.contains("<div>"));
            assertTrue(text.contains("hello"));
        }

        @Test
        void nonStructuralTagWithAttributes() {
            parser.feed("<div class=\"x\">hello</div>");
            parser.endOfInput();
            String text = handler.allText();
            assertTrue(text.contains("<div class=\"x\">"));
            assertTrue(text.contains("hello"));
            assertTrue(text.contains("</div>"));
        }

        @Test
        void nestedNonStructuralTags() {
            parser.feed("<div><span>text</span></div>");
            parser.endOfInput();
            String text = handler.allText();
            assertTrue(text.contains("<div>"));
            assertTrue(text.contains("<span>"));
            assertTrue(text.contains("text"));
            assertTrue(text.contains("</span>"));
            assertTrue(text.contains("</div>"));
            assertTrue(handler.starts().isEmpty());
        }

        @Test
        void nonStructuralInsideStructural() {
            parser.feed("<read_file><path><div>hello</div></path></read_file>");
            parser.endOfInput();
            assertEquals(2, handler.starts().size());
            assertEquals("read_file", handler.starts().get(0).tagName());
            assertEquals("path", handler.starts().get(1).tagName());
            String text = handler.allText();
            assertTrue(text.contains("<div>"));
            assertTrue(text.contains("hello"));
            assertTrue(text.contains("</div>"));
        }

        @Test
        void mixedStructuralAndNonStructural() {
            parser.feed("text<div>html</div><read_file><path>f</path></read_file>end");
            parser.endOfInput();
            assertEquals(2, handler.starts().size());
            String text = handler.allText();
            assertTrue(text.contains("text"));
            assertTrue(text.contains("<div>"));
            assertTrue(text.contains("html"));
            assertTrue(text.contains("</div>"));
            assertTrue(text.contains("f"));
            assertTrue(text.contains("end"));
        }

        @Test
        void nonStructuralSelfLikeTag() {
            parser.feed("<br/>");
            parser.endOfInput();
            String text = handler.allText();
            assertTrue(text.contains("<br/>"));
            assertTrue(handler.starts().isEmpty());
        }

        @Test
        void nonStructuralTagPrefixMatchesStructural() {
            parser.feed("<reading>not a tool</reading>");
            parser.endOfInput();
            String text = handler.allText();
            assertTrue(text.contains("<reading>"));
            assertTrue(text.contains("not a tool"));
            assertTrue(text.contains("</reading>"));
            assertTrue(handler.starts().isEmpty());
        }
    }

    // ===== D. 分层配置 =====

    @Nested
    class HierarchyConfig {

        @Test
        void childTagRecognizedInsideParent() {
            parser.feed("<read_file><path>test.txt</path></read_file>");
            parser.endOfInput();
            assertEquals(2, handler.starts().size());
            assertEquals("read_file", handler.starts().get(0).tagName());
            assertEquals("path", handler.starts().get(1).tagName());
            assertEquals(2, handler.ends().size());
            assertEquals("test.txt", handler.allText());
        }

        @Test
        void childTagNotRecognizedAtRoot() {
            parser.feed("<path>hello</path>");
            parser.endOfInput();
            assertTrue(handler.starts().isEmpty());
            String text = handler.allText();
            assertTrue(text.contains("<path>"));
            assertTrue(text.contains("hello"));
            assertTrue(text.contains("</path>"));
        }

        @Test
        void deepNesting3Levels() {
            TagHierarchyConfig config =
                    TagHierarchyConfig.builder()
                            .rootTag("root")
                            .childTags("root", Set.of("mid"))
                            .childTags("mid", Set.of("leaf"))
                            .build();
            RecordingHandler h = new RecordingHandler();
            StreamingTagParser p = new StreamingTagParser(config, h);
            p.feed("<root><mid><leaf>value</leaf></mid></root>");
            p.endOfInput();
            assertEquals(3, h.starts().size());
            assertEquals("root", h.starts().get(0).tagName());
            assertEquals("mid", h.starts().get(1).tagName());
            assertEquals("leaf", h.starts().get(2).tagName());
            assertEquals("value", h.allText());
        }

        @Test
        void functionCallsInvokeParameter() {
            TagHierarchyConfig nestedConfig = ClineTagConfigs.nestedFormat();
            RecordingHandler h = new RecordingHandler();
            StreamingTagParser p = new StreamingTagParser(nestedConfig, h);
            p.feed(
                    "<function_calls><invoke name=\"x\"><parameter name=\"y\">val</parameter></invoke></function_calls>");
            p.endOfInput();

            List<Event> starts = h.starts();
            assertEquals(3, starts.size());
            assertEquals("function_calls", starts.get(0).tagName());
            assertEquals("invoke", starts.get(1).tagName());
            assertEquals("x", starts.get(1).attrs().get("name"));
            assertEquals("parameter", starts.get(2).tagName());
            assertEquals("y", starts.get(2).attrs().get("name"));
            assertEquals("val", h.allText());
        }

        @Test
        void parameterContainsHtmlTags() {
            TagHierarchyConfig nestedConfig = ClineTagConfigs.nestedFormat();
            RecordingHandler h = new RecordingHandler();
            StreamingTagParser p = new StreamingTagParser(nestedConfig, h);
            p.feed(
                    "<function_calls><invoke name=\"w\"><parameter name=\"content\"><div>Hello <b>World</b></div></parameter></invoke></function_calls>");
            p.endOfInput();

            String paramText = h.allText();
            assertTrue(paramText.contains("<div>"));
            assertTrue(paramText.contains("<b>"));
            assertTrue(paramText.contains("Hello"));
            assertTrue(paramText.contains("World"));
            assertTrue(paramText.contains("</b>"));
            assertTrue(paramText.contains("</div>"));
        }

        @Test
        void mismatchedCloseTag() {
            parser.feed("<read_file></unknown></read_file>");
            parser.endOfInput();
            assertEquals(1, handler.starts().size());
            assertEquals(1, handler.ends().size());
            String text = handler.allText();
            assertTrue(text.contains("</unknown>"));
        }
    }

    // ===== E. 流式 partial 处理 =====

    @Nested
    class StreamingPartialHandling {

        @Test
        void partialStructuralTagPrefix() {
            parser.feed("<rea");
            assertTrue(handler.events.isEmpty());
        }

        @Test
        void partialStructuralTagPrefixThenMore() {
            parser.feed("<rea");
            handler.clear();
            parser.feed("d_file>");
            parser.endOfInput();
            assertEquals(1, handler.starts().size());
            assertEquals("read_file", handler.starts().get(0).tagName());
        }

        @Test
        void partialStructuralTagPrefixThenNonMatch() {
            parser.feed("<rea");
            handler.clear();
            parser.feed("ding>");
            parser.endOfInput();
            String text = handler.allText();
            assertTrue(text.contains("<reading>"));
        }

        @Test
        void partialCloseTagPrefix() {
            parser.feed("<read_file>text</rea");
            assertEquals(1, handler.starts().size());
            String textSoFar = handler.allText();
            assertEquals("text", textSoFar);
        }

        @Test
        void partialAttributeValue() {
            TagHierarchyConfig nestedConfig = ClineTagConfigs.nestedFormat();
            RecordingHandler h = new RecordingHandler();
            StreamingTagParser p = new StreamingTagParser(nestedConfig, h);
            p.feed("<function_calls><invoke name=\"rea");
            assertTrue(h.starts().size() <= 1);
        }

        @Test
        void partialNonStructuralTag() {
            parser.feed("hello<di");
            parser.endOfInput();
            String text = handler.allText();
            assertTrue(text.contains("hello"));
            assertTrue(text.contains("<di"));
        }

        @Test
        void endOfInputFlushesBuffer() {
            parser.feed("hello ");
            handler.clear();
            parser.feed("<rea");
            parser.endOfInput();
            String text = handler.allText();
            assertTrue(text.contains("<rea"));
        }

        @Test
        void endOfInputWithPartialTag() {
            parser.feed("<read_file>text</rea");
            handler.clear();
            parser.endOfInput();
            String text = handler.allText();
            assertTrue(text.contains("</rea"));
        }
    }

    // ===== F. 碎片化输入重放 =====

    @Nested
    class FragmentedInputReplay {

        @Test
        void functionCallParserMainScenario() {
            TagHierarchyConfig nestedConfig = ClineTagConfigs.nestedFormat();
            RecordingHandler h = new RecordingHandler();
            StreamingTagParser p = new StreamingTagParser(nestedConfig, h);

            String[] chunks = {
                "<function_calls>\n",
                "  <invoke name=\"write_",
                "file\">\n",
                "    <parameter name=\"path\">/tmp/index.html</parameter>\n",
                "    <parameter name=\"con",
                "tent\"><div class=",
                "\"main\">",
                "<h1>Hel",
                "lo</h1>",
                "<p>This is <b>",
                "bold</b> and <i>it",
                "alic</i>",
                " text.</p>",
                "</div></param",
                "eter>\n",
                "    <parameter name=\"encoding\">utf-8</parameter>\n",
                "  </invoke>\n",
                "</function_calls>"
            };
            for (String chunk : chunks) {
                p.feed(chunk);
            }
            p.endOfInput();

            List<Event> starts = h.events.stream().filter(e -> e.type == EventType.START).toList();
            assertTrue(starts.stream().anyMatch(e -> "invoke".equals(e.tagName())));
            assertTrue(
                    starts.stream()
                            .anyMatch(
                                    e ->
                                            "invoke".equals(e.tagName())
                                                    && "write_file".equals(e.attrs().get("name"))));
            long paramCount = starts.stream().filter(e -> "parameter".equals(e.tagName())).count();
            assertEquals(3, paramCount);
        }

        @Test
        void chunkSize1() {
            String input = "<read_file><path>test.txt</path></read_file>";
            for (int i = 0; i < input.length(); i++) {
                parser.feed(String.valueOf(input.charAt(i)));
            }
            parser.endOfInput();
            assertEquals(
                    1,
                    handler.starts().stream().filter(e -> "read_file".equals(e.tagName())).count());
            assertEquals(
                    1,
                    handler.ends().stream().filter(e -> "read_file".equals(e.tagName())).count());
            assertEquals("test.txt", handler.allText());
        }

        @Test
        void chunkSize5() {
            String input = "<execute_command><command>ls -la</command></execute_command>";
            for (int i = 0; i < input.length(); i += 5) {
                int end = Math.min(i + 5, input.length());
                parser.feed(input.substring(i, end));
            }
            parser.endOfInput();
            assertTrue(
                    handler.starts().stream().anyMatch(e -> "execute_command".equals(e.tagName())));
            assertEquals("ls -la", handler.allText());
        }

        @Test
        void randomChunkSizes() {
            String input = "text<read_file><path>a.txt</path></read_file>end";
            Random rng = new Random(42);
            int i = 0;
            while (i < input.length()) {
                int size = rng.nextInt(1, 8);
                int end = Math.min(i + size, input.length());
                parser.feed(input.substring(i, end));
                i = end;
            }
            parser.endOfInput();
            assertEquals(
                    1,
                    handler.starts().stream().filter(e -> "read_file".equals(e.tagName())).count());
            String text = handler.allText();
            assertTrue(text.contains("text"));
            assertTrue(text.contains("a.txt"));
            assertTrue(text.contains("end"));
        }

        @Test
        void singleChunkFullMessage() {
            String input = "hello <read_file><path>test.txt</path></read_file> done";
            parser.feed(input);
            parser.endOfInput();
            assertEquals(
                    1,
                    handler.starts().stream().filter(e -> "read_file".equals(e.tagName())).count());
            String text = handler.allText();
            assertTrue(text.contains("hello"));
            assertTrue(text.contains("test.txt"));
            assertTrue(text.contains("done"));
        }
    }

    // ===== G. reset 和复用 =====

    @Nested
    class ResetAndReuse {

        @Test
        void resetClearsAllState() {
            parser.feed("<read_file><path>old");
            parser.reset();
            handler.clear();
            parser.feed("new text");
            parser.endOfInput();
            assertTrue(handler.starts().isEmpty());
            assertEquals("new text", handler.allText());
        }

        @Test
        void resetDuringTagParsing() {
            parser.feed("<rea");
            parser.reset();
            handler.clear();
            parser.feed("hello");
            parser.endOfInput();
            assertEquals("hello", handler.allText());
        }

        @Test
        void multipleResetCycles() {
            for (int cycle = 0; cycle < 3; cycle++) {
                parser.reset();
                handler.clear();
                parser.feed("<read_file><path>f" + cycle + "</path></read_file>");
                parser.endOfInput();
                assertEquals(
                        1,
                        handler.starts().stream()
                                .filter(e -> "read_file".equals(e.tagName()))
                                .count());
                assertEquals("f" + cycle, handler.allText());
            }
        }
    }

    // ===== H. 边界情况 =====

    @Nested
    class BoundaryEdgeCases {

        @Test
        void emptyTag() {
            parser.feed("<>");
            parser.endOfInput();
            String text = handler.allText();
            assertEquals("<>", text);
            assertTrue(handler.starts().isEmpty());
        }

        @Test
        void justLessThan() {
            parser.feed("<");
            parser.endOfInput();
            String text = handler.allText();
            assertEquals("<", text);
        }

        @Test
        void justLessThanSlash() {
            parser.feed("</");
            parser.endOfInput();
            String text = handler.allText();
            assertEquals("</", text);
        }

        @Test
        void lessThanFollowedBySpace() {
            parser.feed("< div>");
            parser.endOfInput();
            String text = handler.allText();
            assertEquals("< div>", text);
            assertTrue(handler.starts().isEmpty());
        }

        @Test
        void consecutiveLessThanSigns() {
            parser.feed("<<div>");
            parser.endOfInput();
            String text = handler.allText();
            assertTrue(text.contains("<"));
            assertTrue(text.contains("<div>"));
            assertTrue(handler.starts().isEmpty());
        }
    }
}
