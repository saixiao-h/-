package com.familyledger.web;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class StaticFileHandler implements HttpHandler {
    private final Path publicDir;

    public StaticFileHandler(Path publicDir) {
        this.publicDir = publicDir.toAbsolutePath().normalize();
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String requestPath = exchange.getRequestURI().getPath();
        if (requestPath.equals("/")) {
            requestPath = "/index.html";
        }
        Path file = publicDir.resolve(requestPath.substring(1)).normalize();
        if (!file.startsWith(publicDir) || !Files.exists(file) || Files.isDirectory(file)) {
            HttpUtil.error(exchange, 404, "文件不存在");
            return;
        }
        HttpUtil.bytes(exchange, 200, contentType(file), Files.readAllBytes(file));
    }

    private String contentType(Path file) {
        String name = file.getFileName().toString();
        if (name.endsWith(".html")) return "text/html; charset=utf-8";
        if (name.endsWith(".css")) return "text/css; charset=utf-8";
        if (name.endsWith(".js")) return "application/javascript; charset=utf-8";
        if (name.endsWith(".json")) return "application/json; charset=utf-8";
        return "application/octet-stream";
    }
}
