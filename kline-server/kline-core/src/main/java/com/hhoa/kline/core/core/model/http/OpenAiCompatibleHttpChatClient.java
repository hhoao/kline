package com.hhoa.kline.core.core.model.http;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.SynchronousSink;
import reactor.core.scheduler.Schedulers;

public class OpenAiCompatibleHttpChatClient implements HttpChatClient {
    private static final ObjectMapper DEFAULT_OBJECT_MAPPER = new ObjectMapper();

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public OpenAiCompatibleHttpChatClient() {
        this(HttpClient.newHttpClient(), DEFAULT_OBJECT_MAPPER);
    }

    public OpenAiCompatibleHttpChatClient(HttpClient httpClient, ObjectMapper objectMapper) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public HttpChatResponse complete(HttpChatRequest request) {
        return Mono.fromCallable(() -> completeBlocking(request))
                .subscribeOn(Schedulers.boundedElastic())
                .block();
    }

    @Override
    public Flux<HttpChatChunk> stream(HttpChatRequest request) {
        return Flux.defer(() -> streamBlocking(request)).subscribeOn(Schedulers.boundedElastic());
    }

    private HttpChatResponse completeBlocking(HttpChatRequest request) {
        try {
            HttpResponse<String> response =
                    httpClient.send(
                            httpRequest(request, false),
                            HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            String body = response.body();
            if (!isSuccess(response.statusCode())) {
                throw new HttpChatException(response.statusCode(), body);
            }
            JsonNode root = objectMapper.readTree(body);
            JsonNode usage = root.path("usage");
            JsonNode content = root.path("choices").path(0).path("message").path("content");
            if (!content.isTextual()) {
                throw new IllegalStateException(
                        "Malformed chat completion response: missing choices[0].message.content in body: "
                                + body);
            }
            return new HttpChatResponse(
                    content.asText(),
                    optionalInt(usage.path("prompt_tokens")),
                    optionalInt(usage.path("completion_tokens")),
                    body);
        } catch (IOException e) {
            throw new IllegalStateException("HTTP model request failed", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("HTTP model request interrupted", e);
        }
    }

    private Flux<HttpChatChunk> streamBlocking(HttpChatRequest request) {
        HttpResponse<InputStream> response;
        try {
            response =
                    httpClient.send(
                            httpRequest(request, true), HttpResponse.BodyHandlers.ofInputStream());
        } catch (IOException e) {
            return Flux.error(new IllegalStateException("HTTP model request failed", e));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Flux.error(new IllegalStateException("HTTP model request interrupted", e));
        }

        InputStream body = response.body();
        if (!isSuccess(response.statusCode())) {
            try (body) {
                return Flux.error(
                        new HttpChatException(
                                response.statusCode(),
                                new String(body.readAllBytes(), StandardCharsets.UTF_8)));
            } catch (IOException e) {
                return Flux.error(new IllegalStateException("HTTP model request failed", e));
            }
        }

        return Flux.using(
                () -> new BufferedReader(new InputStreamReader(body, StandardCharsets.UTF_8)),
                reader ->
                        Flux.<HttpChatChunk, SseEventReader>generate(
                                        () -> new SseEventReader(reader),
                                        (state, sink) -> state.emitNext(sink))
                                .takeUntil(chunk -> chunk.type() == HttpChatChunk.Type.DONE),
                this::close);
    }

    private HttpRequest httpRequest(HttpChatRequest request, boolean stream)
            throws JsonProcessingException {
        HttpRequest.Builder builder =
                HttpRequest.newBuilder()
                        .uri(chatCompletionsUri(request.baseUrl()))
                        .header("Authorization", "Bearer " + request.apiKey())
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(requestJson(request, stream)));
        if (request.timeout() != null) {
            builder.timeout(request.timeout());
        }
        return builder.build();
    }

    private String requestJson(HttpChatRequest request, boolean stream)
            throws JsonProcessingException {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("model", request.model());
        ArrayNode messages = root.putArray("messages");
        for (HttpChatMessage message : request.messages()) {
            ObjectNode messageNode = messages.addObject();
            messageNode.put("role", message.role());
            messageNode.put("content", message.content());
        }
        root.put("stream", stream);
        if (stream) {
            ObjectNode streamOptions = root.putObject("stream_options");
            streamOptions.put("include_usage", true);
        }
        if (request.temperature() != null) {
            root.put("temperature", request.temperature());
        }
        if (request.maxTokens() != null) {
            root.put("max_tokens", request.maxTokens());
        }
        return objectMapper.writeValueAsString(root);
    }

    private URI chatCompletionsUri(String baseUrl) {
        String apiRoot = stripTrailingSlashes(baseUrl);
        String completionsPath =
                apiRoot.endsWith("/v1") ? "/chat/completions" : "/v1/chat/completions";
        return URI.create(apiRoot + completionsPath);
    }

    private String stripTrailingSlashes(String value) {
        int end = value.length();
        while (end > 0 && value.charAt(end - 1) == '/') {
            end--;
        }
        return value.substring(0, end);
    }

    private List<HttpChatChunk> parseSseEvent(String payload) {
        if (payload == null || payload.isBlank()) {
            return List.of();
        }

        String raw = payload.trim();
        if ("[DONE]".equals(raw)) {
            return List.of(HttpChatChunk.done());
        }

        JsonNode root;
        try {
            root = objectMapper.readTree(raw);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Malformed SSE JSON: " + raw, e);
        }

        List<HttpChatChunk> chunks = new ArrayList<>();
        JsonNode choices = root.path("choices");
        if (choices.isArray()) {
            for (JsonNode choice : choices) {
                JsonNode content = choice.path("delta").path("content");
                if (!content.isMissingNode() && !content.isNull()) {
                    chunks.add(HttpChatChunk.text(content.asText(), raw));
                }
            }
        }

        JsonNode usage = root.path("usage");
        if (!usage.isMissingNode() && !usage.isNull()) {
            chunks.add(
                    HttpChatChunk.usage(
                            optionalInt(usage.path("prompt_tokens")),
                            optionalInt(usage.path("completion_tokens")),
                            raw));
        }
        return chunks;
    }

    private boolean isSuccess(int statusCode) {
        return statusCode >= 200 && statusCode < 300;
    }

    private Integer optionalInt(JsonNode node) {
        return node.isMissingNode() || node.isNull() ? null : node.asInt();
    }

    private void close(BufferedReader reader) {
        try {
            reader.close();
        } catch (IOException ignored) {
            // Closing during cancellation should not mask the terminal signal.
        }
    }

    private class SseEventReader {
        private final BufferedReader reader;
        private final List<HttpChatChunk> pendingChunks = new ArrayList<>();

        private SseEventReader(BufferedReader reader) {
            this.reader = reader;
        }

        private SseEventReader emitNext(SynchronousSink<HttpChatChunk> sink) {
            try {
                while (pendingChunks.isEmpty()) {
                    String payload = readEventPayload();
                    if (payload == null) {
                        sink.complete();
                        return this;
                    }
                    pendingChunks.addAll(parseSseEvent(payload));
                }

                sink.next(pendingChunks.remove(0));
                return this;
            } catch (IOException e) {
                sink.error(new IllegalStateException("HTTP model request failed", e));
                return this;
            } catch (RuntimeException e) {
                sink.error(e);
                return this;
            }
        }

        private String readEventPayload() throws IOException {
            List<String> dataLines = new ArrayList<>();
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isEmpty()) {
                    if (!dataLines.isEmpty()) {
                        return String.join("\n", dataLines);
                    }
                    continue;
                }
                if (!line.startsWith("data:")) {
                    continue;
                }

                String data = line.substring("data:".length());
                if (data.startsWith(" ")) {
                    data = data.substring(1);
                }
                dataLines.add(data);
            }

            if (!dataLines.isEmpty()) {
                return String.join("\n", dataLines);
            }
            return null;
        }
    }
}
