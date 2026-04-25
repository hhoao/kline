package com.hhoa.kline.core.core.model.http;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class HttpChatRequestTest {
    @Test
    void blankBaseUrlThrows() {
        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () ->
                                request(
                                        " ",
                                        "api-key",
                                        "model",
                                        List.of(new HttpChatMessage("user", "Hello"))));

        assertEquals("baseUrl must not be blank", exception.getMessage());
    }

    @Test
    void blankApiKeyThrows() {
        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () ->
                                request(
                                        "https://api.example.com",
                                        " ",
                                        "model",
                                        List.of(new HttpChatMessage("user", "Hello"))));

        assertEquals("apiKey must not be blank", exception.getMessage());
    }

    @Test
    void blankModelThrows() {
        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () ->
                                request(
                                        "https://api.example.com",
                                        "api-key",
                                        " ",
                                        List.of(new HttpChatMessage("user", "Hello"))));

        assertEquals("model must not be blank", exception.getMessage());
    }

    @Test
    void nullModelThrows() {
        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () ->
                                request(
                                        "https://api.example.com",
                                        "api-key",
                                        null,
                                        List.of(new HttpChatMessage("user", "Hello"))));

        assertEquals("model must not be blank", exception.getMessage());
    }

    @Test
    void emptyMessagesThrows() {
        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> request("https://api.example.com", "api-key", "model", List.of()));

        assertEquals("messages must not be empty", exception.getMessage());
    }

    @Test
    void trailingSlashIsRemovedFromBaseUrl() {
        HttpChatRequest request =
                request(
                        "https://api.example.com/",
                        "api-key",
                        "model",
                        List.of(new HttpChatMessage("user", "Hello")));

        assertEquals("https://api.example.com", request.baseUrl());
    }

    @Test
    void allTrailingSlashesAreRemovedFromBaseUrl() {
        HttpChatRequest request =
                request(
                        "https://api.example.com///",
                        "api-key",
                        "model",
                        List.of(new HttpChatMessage("user", "Hello")));

        assertEquals("https://api.example.com", request.baseUrl());
    }

    @Test
    void messagesAreDefensivelyCopied() {
        List<HttpChatMessage> messages = new ArrayList<>();
        messages.add(new HttpChatMessage("user", "Hello"));

        HttpChatRequest request = request("https://api.example.com", "api-key", "model", messages);
        messages.add(new HttpChatMessage("assistant", "Hi"));

        assertEquals(1, request.messages().size());
        assertEquals(new HttpChatMessage("user", "Hello"), request.messages().get(0));
    }

    private HttpChatRequest request(
            String baseUrl, String apiKey, String model, List<HttpChatMessage> messages) {
        return new HttpChatRequest(
                baseUrl, apiKey, model, messages, 0.7, 1024, Duration.ofSeconds(30));
    }
}
