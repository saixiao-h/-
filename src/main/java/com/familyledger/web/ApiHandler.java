package com.familyledger.web;

import com.familyledger.model.ParsedLedgerEntry;
import com.familyledger.service.LedgerService;
import com.familyledger.service.VoiceLedgerService;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

public final class ApiHandler implements HttpHandler {
    private final LedgerService ledgerService;
    private final VoiceLedgerService voiceLedgerService;

    public ApiHandler(LedgerService ledgerService, VoiceLedgerService voiceLedgerService) {
        this.ledgerService = ledgerService;
        this.voiceLedgerService = voiceLedgerService;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            route(exchange);
        } catch (IllegalArgumentException exception) {
            HttpUtil.error(exchange, 400, exception.getMessage());
        } catch (Exception exception) {
            HttpUtil.error(exchange, 500, "服务器错误：" + exception.getMessage());
        }
    }

    private void route(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();

        if ("GET".equals(method) && "/api/accounts".equals(path)) {
            HttpUtil.json(exchange, 200, ledgerService.accounts());
            return;
        }
        if ("GET".equals(method) && "/api/categories".equals(path)) {
            HttpUtil.json(exchange, 200, ledgerService.categories());
            return;
        }
        if ("GET".equals(method) && "/api/transactions".equals(path)) {
            HttpUtil.json(exchange, 200, ledgerService.transactions(HttpUtil.query(exchange)));
            return;
        }
        if ("POST".equals(method) && "/api/transactions".equals(path)) {
            HttpUtil.json(exchange, 201, ledgerService.create(Json.parseObject(HttpUtil.body(exchange))));
            return;
        }
        if ("PUT".equals(method) && path.startsWith("/api/transactions/")) {
            long id = Long.parseLong(path.substring(path.lastIndexOf('/') + 1));
            var updated = ledgerService.update(id, Json.parseObject(HttpUtil.body(exchange)));
            if (updated.isPresent()) HttpUtil.json(exchange, 200, updated.get());
            else HttpUtil.error(exchange, 404, "账目不存在");
            return;
        }
        if ("DELETE".equals(method) && path.startsWith("/api/transactions/")) {
            long id = Long.parseLong(path.substring(path.lastIndexOf('/') + 1));
            if (ledgerService.delete(id)) HttpUtil.noContent(exchange);
            else HttpUtil.error(exchange, 404, "账目不存在");
            return;
        }
        if ("GET".equals(method) && "/api/reports/overview".equals(path)) {
            HttpUtil.json(exchange, 200, ledgerService.overview(HttpUtil.query(exchange).get("month")));
            return;
        }
        if ("GET".equals(method) && "/api/reports/reconcile".equals(path)) {
            HttpUtil.json(exchange, 200, ledgerService.reconcile());
            return;
        }
        if ("POST".equals(method) && "/api/ai/parse-voice".equals(path)) {
            Map<String, Object> body = Json.parseObject(HttpUtil.body(exchange));
            String voiceText = String.valueOf(body.getOrDefault("voiceText", ""));
            ParsedLedgerEntry parsed = voiceLedgerService.mockModelParse(voiceText);
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("prompt", voiceLedgerService.buildPrompt(voiceText));
            response.put("modelJson", voiceLedgerService.toModelJson(parsed));
            HttpUtil.json(exchange, 200, response);
            return;
        }

        HttpUtil.error(exchange, 404, "接口不存在：" + path);
    }
}
