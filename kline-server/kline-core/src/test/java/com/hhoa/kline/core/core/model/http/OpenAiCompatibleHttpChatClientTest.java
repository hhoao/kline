package com.hhoa.kline.core.core.model.http;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

class OpenAiCompatibleHttpChatClientTest {
    private HttpServer server;

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void completesNonStreamingRequest() throws IOException {
        AtomicReference<String> authorization = new AtomicReference<>();
        AtomicReference<String> requestBody = new AtomicReference<>();
        startServer(
                exchange -> {
                    authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
                    requestBody.set(readBody(exchange));
                    sendJson(
                            exchange,
                            200,
                            "{\"choices\":[{\"message\":{\"content\":\"hello\"}}],"
                                    + "\"usage\":{\"prompt_tokens\":2,\"completion_tokens\":3}}");
                });

        HttpChatResponse response = new OpenAiCompatibleHttpChatClient().complete(request());

        assertEquals("hello", response.text());
        assertEquals(2, response.inputTokens());
        assertEquals(3, response.outputTokens());
        assertEquals(
                "{\"choices\":[{\"message\":{\"content\":\"hello\"}}],"
                        + "\"usage\":{\"prompt_tokens\":2,\"completion_tokens\":3}}",
                response.raw());
        assertEquals("Bearer test-key", authorization.get());
        assertTrue(requestBody.get().contains("\"stream\":false"));
    }

    @Test
    void streamsSseChunks() throws IOException {
        startServer(
                exchange ->
                        sendSse(
                                exchange,
                                200,
                                """
                                data: {"choices":[{"delta":{"content":"你"}}]}

                                data: {"choices":[{"delta":{"content":"好"}}]}

                                data: {"usage":{"prompt_tokens":4,"completion_tokens":5},"choices":[]}

                                data: [DONE]

                                """));

        StepVerifier.create(new OpenAiCompatibleHttpChatClient().stream(request()))
                .assertNext(
                        chunk -> {
                            assertEquals(HttpChatChunk.Type.TEXT, chunk.type());
                            assertEquals("你", chunk.text());
                        })
                .assertNext(
                        chunk -> {
                            assertEquals(HttpChatChunk.Type.TEXT, chunk.type());
                            assertEquals("好", chunk.text());
                        })
                .assertNext(
                        chunk -> {
                            assertEquals(HttpChatChunk.Type.USAGE, chunk.type());
                            assertEquals(4, chunk.inputTokens());
                            assertEquals(5, chunk.outputTokens());
                        })
                .assertNext(chunk -> assertEquals(HttpChatChunk.Type.DONE, chunk.type()))
                .verifyComplete();
    }

    @Test
    void failsOnNonSuccessStatus() throws IOException {
        startServer(exchange -> sendJson(exchange, 401, "{\"error\":\"bad key\"}"));

        HttpChatException exception =
                assertThrows(
                        HttpChatException.class,
                        () -> new OpenAiCompatibleHttpChatClient().complete(request()));

        assertEquals(401, exception.statusCode());
        assertEquals("{\"error\":\"bad key\"}", exception.responseBody());
    }

    @Test
    void failsOnMalformedSseJson() throws IOException {
        startServer(exchange -> sendSse(exchange, 200, "data: {bad-json}\n\n"));

        StepVerifier.create(new OpenAiCompatibleHttpChatClient().stream(request()))
                .expectError()
                .verify();
    }

    private HttpChatRequest request() {
        return new HttpChatRequest(
                baseUrl(),
                "test-key",
                "test-model",
                List.of(new HttpChatMessage("user", "hello")),
                0.2,
                32,
                Duration.ofSeconds(5));
    }

    private String baseUrl() {
        return "http://localhost:" + server.getAddress().getPort();
    }

    private void startServer(ExchangeHandler handler) throws IOException {
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/v1/chat/completions", handler::handle);
        server.start();
    }

    private String readBody(HttpExchange exchange) throws IOException {
        return new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
    }

    private void sendJson(HttpExchange exchange, int status, String body) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        send(exchange, status, body);
    }

    private void sendSse(HttpExchange exchange, int status, String body) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "text/event-stream");
        send(exchange, status, body);
    }

    private void send(HttpExchange exchange, int status, String body) throws IOException {
        byte[] response = body.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(status, response.length);
        exchange.getResponseBody().write(response);
        exchange.close();
    }

    @FunctionalInterface
    private interface ExchangeHandler {
        void handle(HttpExchange exchange) throws IOException;
    }
}
