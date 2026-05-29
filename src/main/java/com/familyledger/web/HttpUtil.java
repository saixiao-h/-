package com.familyledger.web;

import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

public final class HttpUtil {
    private HttpUtil() {
    }

    public static String body(HttpExchange exchange) throws IOException {
        try (InputStream input = exchange.getRequestBody()) {
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    public static Map<String, String> query(HttpExchange exchange) {
        Map<String, String> result = new LinkedHashMap<>();
        String raw = exchange.getRequestURI().getRawQuery();
        if (raw == null || raw.isBlank()) return result;
        for (String part : raw.split("&")) {
            String[] pair = part.split("=", 2);
            String key = decode(pair[0]);
            String value = pair.length > 1 ? decode(pair[1]) : "";
            result.put(key, value);
        }
        return result;
    }

    public static void json(HttpExchange exchange, int status, Object payload) throws IOException {
        bytes(exchange, status, "application/json; charset=utf-8", Json.stringify(payload).getBytes(StandardCharsets.UTF_8));
    }

    public static void text(HttpExchange exchange, int status, String contentType, String payload) throws IOException {
        bytes(exchange, status, contentType + "; charset=utf-8", payload.getBytes(StandardCharsets.UTF_8));
    }

    public static void bytes(HttpExchange exchange, int status, String contentType, byte[] payload) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.sendResponseHeaders(status, payload.length);
        try (OutputStream output = exchange.getResponseBody()) {
            output.write(payload);
        }
    }

    public static void noContent(HttpExchange exchange) throws IOException {
        exchange.sendResponseHeaders(204, -1);
    }

    public static void error(HttpExchange exchange, int status, String message) throws IOException {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("error", message);
        json(exchange, status, payload);
    }

    private static String decode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }
}
