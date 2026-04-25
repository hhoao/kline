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
            return new HttpChatResponse(
                    root.path("choices").path(0).path("message").path("content").asText(),
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

    @Override
    public Flux<HttpChatChunk> stream(HttpChatRequest request) {
        return Flux.defer(
                () -> {
                    HttpResponse<InputStream> response;
                    try {
                        response =
                                httpClient.send(
                                        httpRequest(request, true),
                                        HttpResponse.BodyHandlers.ofInputStream());
                    } catch (IOException e) {
                        return Flux.error(
                                new IllegalStateException("HTTP model request failed", e));
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return Flux.error(
                                new IllegalStateException("HTTP model request interrupted", e));
                    }

                    InputStream body = response.body();
                    if (!isSuccess(response.statusCode())) {
                        try (body) {
                            return Flux.error(
                                    new HttpChatException(
                                            response.statusCode(),
                                            new String(
                                                    body.readAllBytes(), StandardCharsets.UTF_8)));
                        } catch (IOException e) {
                            return Flux.error(
                                    new IllegalStateException("HTTP model request failed", e));
                        }
                    }

                    return Flux.using(
                            () ->
                                    new BufferedReader(
                                            new InputStreamReader(body, StandardCharsets.UTF_8)),
                            reader ->
                                    Flux.fromStream(reader.lines())
                                            .<HttpChatChunk>handle(
                                                    (line, sink) -> {
                                                        try {
                                                            for (HttpChatChunk chunk :
                                                                    parseSseLine(line)) {
                                                                sink.next(chunk);
                                                            }
                                                        } catch (RuntimeException e) {
                                                            sink.error(e);
                                                        }
                                                    })
                                            .takeUntil(
                                                    chunk ->
                                                            chunk.type()
                                                                    == HttpChatChunk.Type.DONE),
                            this::close);
                });
    }

    private HttpRequest httpRequest(HttpChatRequest request, boolean stream)
            throws JsonProcessingException {
        HttpRequest.Builder builder =
                HttpRequest.newBuilder()
                        .uri(URI.create(request.baseUrl() + "/v1/chat/completions"))
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
        if (request.temperature() != null) {
            root.put("temperature", request.temperature());
        }
        if (request.maxTokens() != null) {
            root.put("max_tokens", request.maxTokens());
        }
        return objectMapper.writeValueAsString(root);
    }

    private List<HttpChatChunk> parseSseLine(String line) {
        if (line == null || line.isBlank() || !line.startsWith("data:")) {
            return List.of();
        }

        String raw = line.substring("data:".length()).trim();
        if (raw.isEmpty()) {
            return List.of();
        }
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
}
