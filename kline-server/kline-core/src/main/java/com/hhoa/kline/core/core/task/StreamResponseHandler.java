package com.hhoa.kline.core.core.task;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hhoa.kline.core.core.assistant.ToolUse;
import com.hhoa.kline.core.core.tools.ClineDefaultTool;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.Data;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * Handles streaming response content, converting native tool use blocks and reasoning deltas into
 * the internal message format.
 *
 * <p>Composed of two inner handlers:
 *
 * <ul>
 *   <li>{@link ToolUseHandler} — accumulates tool use deltas and produces finalized tool use blocks
 *   <li>{@link ReasoningHandler} — accumulates reasoning/thinking deltas
 * </ul>
 */
@Slf4j
public class StreamResponseHandler {

    private ToolUseHandler toolUseHandler = new ToolUseHandler();
    private ReasoningHandler reasoningHandler = new ReasoningHandler();

    @Getter private String requestId;

    public void setRequestId(String id) {
        if (this.requestId == null && id != null) {
            this.requestId = id;
        }
    }

    public ToolUseHandler getToolUseHandler() {
        return toolUseHandler;
    }

    public ReasoningHandler getReasoningHandler() {
        return reasoningHandler;
    }

    public void reset() {
        this.requestId = null;
        this.toolUseHandler = new ToolUseHandler();
        this.reasoningHandler = new ReasoningHandler();
    }

    // ──────────────────── ToolUseHandler ────────────────────

    /** Handles streaming native tool use blocks and converts them to ToolUse format. */
    @Slf4j
    public static class ToolUseHandler {

        private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

        private static final Map<String, String> ESCAPE_MAP =
                Map.of(
                        "\\n", "\n",
                        "\\t", "\t",
                        "\\r", "\r",
                        "\\\"", "\"",
                        "\\\\", "\\");

        private static final Pattern ESCAPE_PATTERN = Pattern.compile("\\\\[ntr\"\\\\]");

        private static final Pattern PARTIAL_JSON_FIELD_PATTERN =
                Pattern.compile("\"(\\w+)\":\\s*\"((?:[^\"\\\\]|\\\\.)*)\"?");

        private final Map<String, PendingToolUse> pendingToolUses = new LinkedHashMap<>();

        /**
         * Process an incoming tool use delta from the stream.
         *
         * @param chunk the ApiChunk with type TOOL_USE
         */
        public void processToolUseDelta(ApiChunk chunk) {
            String id = chunk.toolId();
            if (id == null || id.isEmpty()) {
                return;
            }

            PendingToolUse pending =
                    pendingToolUses.computeIfAbsent(
                            id,
                            k ->
                                    new PendingToolUse(
                                            id,
                                            chunk.toolName() != null ? chunk.toolName() : "",
                                            chunk.callId() != null ? chunk.callId() : id));

            if (chunk.toolName() != null) {
                pending.setName(chunk.toolName());
            }

            if (chunk.signature() != null) {
                pending.setSignature(chunk.signature());
            }

            // Accumulate input JSON fragments
            if (chunk.text() != null) {
                pending.appendInput(chunk.text());
            }
        }

        /**
         * Get the finalized tool use block for the given ID.
         *
         * @param id the tool use block ID
         * @return the finalized ToolUse, or null if not found or incomplete
         */
        public ToolUse getFinalizedToolUse(String id) {
            PendingToolUse pending = pendingToolUses.get(id);
            if (pending == null || pending.getName() == null || pending.getName().isEmpty()) {
                return null;
            }

            Map<String, Object> input = parseInput(pending);

            ToolUse toolUse = new ToolUse();
            toolUse.setId(pending.getId());
            toolUse.setName(pending.getName());
            toolUse.setParams(input);
            toolUse.setPartial(false);
            toolUse.setSignature(pending.getSignature());
            toolUse.setCallId(pending.getCallId());
            return toolUse;
        }

        /** Get all finalized tool uses, optionally attaching reasoning details. */
        public List<ToolUse> getAllFinalizedToolUses(Object reasoningDetails) {
            List<ToolUse> results = new ArrayList<>();
            for (String id : pendingToolUses.keySet()) {
                ToolUse toolUse = getFinalizedToolUse(id);
                if (toolUse != null) {
                    if (reasoningDetails != null) {
                        toolUse.setReasoningDetails(reasoningDetails);
                    }
                    results.add(toolUse);
                }
            }
            return results;
        }

        /** Get all finalized tool uses without reasoning details. */
        public List<ToolUse> getAllFinalizedToolUses() {
            return getAllFinalizedToolUses(null);
        }

        public boolean hasToolUse(String id) {
            return pendingToolUses.containsKey(id);
        }

        private static final String CLINE_MCP_TOOL_IDENTIFIER = "0mcp0";

        /** Get partial (in-progress) tool uses for intermediate presentation. */
        public List<ToolUse> getPartialToolUsesAsContent() {
            List<ToolUse> results = new ArrayList<>();
            for (PendingToolUse pending : pendingToolUses.values()) {
                if (pending.getName() == null || pending.getName().isEmpty()) {
                    continue;
                }

                Map<String, Object> input = parseInput(pending);

                if (pending.getName().contains(CLINE_MCP_TOOL_IDENTIFIER)) {
                    // MCP tool — split into server key + tool name
                    String[] parts = pending.getName().split(CLINE_MCP_TOOL_IDENTIFIER, 2);
                    String serverKey = parts.length > 0 ? parts[0] : "";
                    String toolName = parts.length > 1 ? parts[1] : "";
                    Map<String, Object> mcpParams = new LinkedHashMap<>();
                    mcpParams.put("server_name", serverKey);
                    mcpParams.put("tool_name", toolName);
                    try {
                        mcpParams.put("arguments", OBJECT_MAPPER.writeValueAsString(input));
                    } catch (Exception e) {
                        mcpParams.put("arguments", "{}");
                    }

                    ToolUse toolUse =
                            new ToolUse(ClineDefaultTool.MCP_USE.getValue(), mcpParams, true);
                    toolUse.setId(pending.getId());
                    toolUse.setNativeToolCall(true);
                    toolUse.setSignature(pending.getSignature());
                    toolUse.setCallId(pending.getCallId());
                    results.add(toolUse);
                } else {
                    // Regular tool
                    Map<String, Object> params = new LinkedHashMap<>();
                    if (input != null) {
                        for (Map.Entry<String, Object> entry : input.entrySet()) {
                            Object value = entry.getValue();
                            params.put(
                                    entry.getKey(),
                                    value instanceof String
                                            ? value
                                            : OBJECT_MAPPER.valueToTree(value).toString());
                        }
                    }

                    ToolUse toolUse = new ToolUse(pending.getName(), params, true);
                    toolUse.setId(pending.getId());
                    toolUse.setNativeToolCall(true);
                    toolUse.setSignature(pending.getSignature());
                    toolUse.setCallId(pending.getCallId());
                    results.add(toolUse);
                }
            }
            return results;
        }

        public void reset() {
            pendingToolUses.clear();
        }

        @SuppressWarnings("unchecked")
        private Map<String, Object> parseInput(PendingToolUse pending) {
            String rawInput = pending.getInputBuffer();
            if (rawInput == null || rawInput.isEmpty()) {
                return new LinkedHashMap<>();
            }

            // Try full JSON parse first
            try {
                return OBJECT_MAPPER.readValue(
                        rawInput, new TypeReference<Map<String, Object>>() {});
            } catch (Exception ignored) {
                // Fall back to extracting partial fields from incomplete JSON
            }

            return extractPartialJsonFields(rawInput);
        }

        private Map<String, Object> extractPartialJsonFields(String partialJson) {
            Map<String, Object> result = new LinkedHashMap<>();
            Matcher matcher = PARTIAL_JSON_FIELD_PATTERN.matcher(partialJson);

            while (matcher.find()) {
                String key = matcher.group(1);
                String value = matcher.group(2);
                result.put(key, unescapeJsonString(value));
            }

            return result;
        }

        private String unescapeJsonString(String s) {
            return ESCAPE_PATTERN
                    .matcher(s)
                    .replaceAll(
                            m ->
                                    Matcher.quoteReplacement(
                                            ESCAPE_MAP.getOrDefault(m.group(), m.group())));
        }
    }

    // ──────────────────── PendingToolUse ────────────────────

    @Data
    public static class PendingToolUse {
        private final String id;
        private String name;
        private final String callId;
        private String signature;
        private final StringBuilder inputBuffer = new StringBuilder();

        public PendingToolUse(String id, String name, String callId) {
            this.id = id;
            this.name = name;
            this.callId = callId;
        }

        public void appendInput(String fragment) {
            inputBuffer.append(fragment);
        }

        public String getInputBuffer() {
            return inputBuffer.toString();
        }
    }

    // ──────────────────── ReasoningHandler ────────────────────

    /** Handles streaming reasoning content and converts it to the appropriate message format. */
    public static class ReasoningHandler {

        private PendingReasoning pendingReasoning;

        /**
         * Process an incoming reasoning delta from the stream.
         *
         * @param chunk the ApiChunk with type REASONING, REASONING_DETAILS, ANT_THINKING, or
         *     ANT_REDACTED_THINKING
         */
        public void processReasoningDelta(ApiChunk chunk) {
            if (pendingReasoning == null) {
                pendingReasoning = new PendingReasoning();
            }

            if (chunk.reasoning() != null) {
                pendingReasoning.appendContent(chunk.reasoning());
            }

            if (chunk.thinking() != null) {
                pendingReasoning.appendContent(chunk.thinking());
            }

            if (chunk.signature() != null) {
                pendingReasoning.setSignature(chunk.signature());
            }

            if (chunk.reasoningDetails() != null) {
                pendingReasoning.addSummaryDetail(chunk.reasoningDetails());
            }

            if (chunk.data() != null && chunk.type() == ApiChunk.ChunkType.ANT_REDACTED_THINKING) {
                pendingReasoning.addRedactedThinking(chunk.data());
            }
        }

        /** Get the current accumulated reasoning content, or null if none. */
        public PendingReasoning getCurrentReasoning() {
            if (pendingReasoning == null) {
                return null;
            }
            if (pendingReasoning.getSummary().isEmpty()
                    && pendingReasoning.getContent().isEmpty()) {
                return null;
            }

            // Ensure signature is set if it's hidden in the summary / reasoning details
            if ((pendingReasoning.getSignature() == null
                            || pendingReasoning.getSignature().isEmpty())
                    && !pendingReasoning.getSummary().isEmpty()) {
                Object lastSummary =
                        pendingReasoning.getSummary().get(pendingReasoning.getSummary().size() - 1);
                if (lastSummary instanceof Map<?, ?> map) {
                    Object sig = map.get("signature");
                    if (sig instanceof String s && !s.isEmpty()) {
                        pendingReasoning.setSignature(s);
                    }
                }
            }

            return pendingReasoning;
        }

        /** Get the accumulated redacted thinking blocks. */
        public List<String> getRedactedThinking() {
            if (pendingReasoning == null) {
                return Collections.emptyList();
            }
            return pendingReasoning.getRedactedThinking();
        }

        public void reset() {
            pendingReasoning = null;
        }
    }

    // ──────────────────── PendingReasoning ────────────────────

    @Getter
    public static class PendingReasoning {
        private final StringBuilder contentBuffer = new StringBuilder();
        private String signature = "";
        private final List<String> redactedThinking = new ArrayList<>();
        private final List<Object> summary = new ArrayList<>();

        public void appendContent(String text) {
            contentBuffer.append(text);
        }

        public String getContent() {
            return contentBuffer.toString();
        }

        public void setSignature(String signature) {
            this.signature = signature;
        }

        public void addRedactedThinking(String data) {
            redactedThinking.add(data);
        }

        public void addSummaryDetail(Object detail) {
            if (detail instanceof List<?> list) {
                summary.addAll(list);
            } else {
                summary.add(detail);
            }
        }
    }
}
