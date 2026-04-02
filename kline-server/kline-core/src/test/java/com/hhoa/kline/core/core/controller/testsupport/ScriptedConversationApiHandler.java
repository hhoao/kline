package com.hhoa.kline.core.core.controller.testsupport;

import com.hhoa.kline.core.core.assistant.MessageParam;
import com.hhoa.kline.core.core.task.ApiChunk;
import com.hhoa.kline.core.core.task.ApiHandler;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import reactor.core.publisher.Flux;

public final class ScriptedConversationApiHandler implements ApiHandler {

    @FunctionalInterface
    public interface StreamFactory {
        Flux<ApiChunk> create(
                int roundOneBased, String systemPrompt, List<MessageParam> conversationHistory);
    }

    private final String modelId;
    private final String providerId;
    private final AtomicInteger round = new AtomicInteger(0);
    private final StreamFactory streamFactory;

    public ScriptedConversationApiHandler(StreamFactory streamFactory) {
        this("integration-test-model", "integration-test-provider", streamFactory);
    }

    public ScriptedConversationApiHandler(
            String modelId, String providerId, StreamFactory streamFactory) {
        this.modelId = modelId;
        this.providerId = providerId;
        this.streamFactory = streamFactory;
    }

    public static ScriptedConversationApiHandler singleTurnTextReply(String completeText) {
        return singleTurnTextReply(completeText, 2);
    }

    public static ScriptedConversationApiHandler singleTurnTextReply(
            String completeText, int codePointsPerChunk) {
        Objects.requireNonNull(completeText, "completeText");
        if (completeText.isEmpty()) {
            throw new IllegalArgumentException("completeText must not be empty");
        }
        if (codePointsPerChunk < 1) {
            throw new IllegalArgumentException("codePointsPerChunk must be >= 1");
        }
        return new ScriptedConversationApiHandler(
                (round, sys, hist) -> {
                    if (round == 1) {
                        return buildChunkedFlux(completeText, codePointsPerChunk);
                    }
                    return Flux.just(SimpleTestApiChunk.textChunk(""))
                            .cast(ApiChunk.class)
                            .concatWith(Flux.just(SimpleTestApiChunk.usageChunk(0, 0)));
                });
    }

    /**
     * 多轮 API：第 n 轮返回 {@code assistantMessages[n-1]}。若某轮为 Cline 工具 XML（如 {@code
     * <read_file>...}），整段作为单次 TEXT 发出，避免拆块破坏标签。
     */
    public static ScriptedConversationApiHandler multiTurnRounds(String... assistantMessages) {
        List<String> rounds = List.of(assistantMessages);
        if (rounds.isEmpty()) {
            throw new IllegalArgumentException("assistantMessages must not be empty");
        }
        return new ScriptedConversationApiHandler(
                (round, sys, hist) -> {
                    if (round < 1 || round > rounds.size()) {
                        return Flux.just(SimpleTestApiChunk.usageChunk(0, 0));
                    }
                    return fluxForAssistantMessage(rounds.get(round - 1));
                });
    }

    private static Flux<ApiChunk> fluxForAssistantMessage(String text) {
        if (looksLikeToolCallingXml(text)) {
            return Flux.just(SimpleTestApiChunk.textChunk(text))
                    .cast(ApiChunk.class)
                    .concatWith(Flux.just(SimpleTestApiChunk.usageChunk(8, 24)));
        }
        return buildChunkedFlux(text, 2);
    }

    private static boolean looksLikeToolCallingXml(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        String t = text.trim();
        return t.startsWith("<") && t.contains(">") && t.lastIndexOf("</") > t.indexOf('<');
    }

    private static Flux<ApiChunk> buildChunkedFlux(String text, int codePointsPerChunk) {
        List<String> pieces = splitIntoCodePointChunks(text, codePointsPerChunk);
        return Flux.fromIterable(pieces)
                .map(SimpleTestApiChunk::textChunk)
                .cast(ApiChunk.class)
                .concatWith(Flux.just(SimpleTestApiChunk.usageChunk(8, 24)));
    }

    private static List<String> splitIntoCodePointChunks(String s, int codePointsPerChunk) {
        List<String> chunks = new ArrayList<>();
        int i = 0;
        while (i < s.length()) {
            int end = i;
            int n = 0;
            while (n < codePointsPerChunk && end < s.length()) {
                int cp = s.codePointAt(end);
                end += Character.charCount(cp);
                n++;
            }
            chunks.add(s.substring(i, end));
            i = end;
        }
        return chunks;
    }

    @Override
    public String getLastRequestId() {
        return "scripted-req-" + round.get();
    }

    @Override
    public Flux<ApiChunk> createMessageStream(
            String systemPrompt, List<MessageParam> conversationHistory) {
        int n = round.incrementAndGet();
        return streamFactory.create(n, systemPrompt, conversationHistory);
    }

    public int getStreamInvocationCount() {
        return round.get();
    }

    @Override
    public String getModelId() {
        return modelId;
    }

    @Override
    public String getProviderId() {
        return providerId;
    }
}
