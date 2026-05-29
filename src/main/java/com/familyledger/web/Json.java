package com.familyledger.web;

import com.familyledger.model.Account;
import com.familyledger.model.Category;
import com.familyledger.model.Transaction;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class Json {
    private Json() {
    }

    public static String stringify(Object value) {
        if (value == null) return "null";
        if (value instanceof String text) return quote(text);
        if (value instanceof Number || value instanceof Boolean) return String.valueOf(value);
        if (value instanceof BigDecimal decimal) return decimal.toPlainString();
        if (value instanceof Account account) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", account.id());
            map.put("name", account.name());
            map.put("type", account.type());
            map.put("openingBalance", account.openingBalance());
            return stringify(map);
        }
        if (value instanceof Category category) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", category.id());
            map.put("name", category.name());
            map.put("type", category.type().name());
            return stringify(map);
        }
        if (value instanceof Transaction transaction) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", transaction.id());
            map.put("type", transaction.type().name());
            map.put("amount", transaction.amount());
            map.put("accountId", transaction.accountId());
            map.put("targetAccountId", transaction.targetAccountId());
            map.put("categoryId", transaction.categoryId());
            map.put("date", transaction.transactionDate().toString());
            map.put("note", transaction.note());
            map.put("createdAt", transaction.createdAt().toString());
            map.put("updatedAt", transaction.updatedAt().toString());
            return stringify(map);
        }
        if (value instanceof Map<?, ?> map) {
            StringBuilder builder = new StringBuilder("{");
            boolean first = true;
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (!first) builder.append(',');
                first = false;
                builder.append(quote(String.valueOf(entry.getKey()))).append(':').append(stringify(entry.getValue()));
            }
            return builder.append('}').toString();
        }
        if (value instanceof Iterable<?> iterable) {
            StringBuilder builder = new StringBuilder("[");
            boolean first = true;
            for (Object item : iterable) {
                if (!first) builder.append(',');
                first = false;
                builder.append(stringify(item));
            }
            return builder.append(']').toString();
        }
        return quote(String.valueOf(value));
    }

    public static Map<String, Object> parseObject(String json) {
        Parser parser = new Parser(json == null ? "" : json);
        Object value = parser.parseValue();
        if (value instanceof Map<?, ?> raw) {
            Map<String, Object> result = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : raw.entrySet()) {
                result.put(String.valueOf(entry.getKey()), entry.getValue());
            }
            return result;
        }
        throw new IllegalArgumentException("请求体必须是 JSON 对象");
    }

    private static String quote(String text) {
        StringBuilder builder = new StringBuilder("\"");
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            switch (ch) {
                case '"' -> builder.append("\\\"");
                case '\\' -> builder.append("\\\\");
                case '\n' -> builder.append("\\n");
                case '\r' -> builder.append("\\r");
                case '\t' -> builder.append("\\t");
                default -> builder.append(ch);
            }
        }
        return builder.append('"').toString();
    }

    private static final class Parser {
        private final String text;
        private int index;

        private Parser(String text) {
            this.text = text.trim();
        }

        private Object parseValue() {
            skipWhitespace();
            if (peek('{')) return parseObjectValue();
            if (peek('[')) return parseArray();
            if (peek('"')) return parseString();
            if (startsWith("true")) {
                index += 4;
                return true;
            }
            if (startsWith("false")) {
                index += 5;
                return false;
            }
            if (startsWith("null")) {
                index += 4;
                return null;
            }
            return parseNumber();
        }

        private Map<String, Object> parseObjectValue() {
            expect('{');
            Map<String, Object> map = new LinkedHashMap<>();
            skipWhitespace();
            if (peek('}')) {
                index++;
                return map;
            }
            while (true) {
                String key = parseString();
                skipWhitespace();
                expect(':');
                map.put(key, parseValue());
                skipWhitespace();
                if (peek('}')) {
                    index++;
                    return map;
                }
                expect(',');
            }
        }

        private List<Object> parseArray() {
            expect('[');
            List<Object> list = new ArrayList<>();
            skipWhitespace();
            if (peek(']')) {
                index++;
                return list;
            }
            while (true) {
                list.add(parseValue());
                skipWhitespace();
                if (peek(']')) {
                    index++;
                    return list;
                }
                expect(',');
            }
        }

        private String parseString() {
            expect('"');
            StringBuilder builder = new StringBuilder();
            while (index < text.length()) {
                char ch = text.charAt(index++);
                if (ch == '"') return builder.toString();
                if (ch == '\\' && index < text.length()) {
                    char escaped = text.charAt(index++);
                    switch (escaped) {
                        case '"' -> builder.append('"');
                        case '\\' -> builder.append('\\');
                        case 'n' -> builder.append('\n');
                        case 'r' -> builder.append('\r');
                        case 't' -> builder.append('\t');
                        default -> builder.append(escaped);
                    }
                } else {
                    builder.append(ch);
                }
            }
            throw new IllegalArgumentException("JSON 字符串未闭合");
        }

        private BigDecimal parseNumber() {
            int start = index;
            while (index < text.length()) {
                char ch = text.charAt(index);
                if ((ch >= '0' && ch <= '9') || ch == '-' || ch == '+'
                        || ch == '.' || ch == 'e' || ch == 'E') {
                    index++;
                } else {
                    break;
                }
            }
            if (start == index) throw new IllegalArgumentException("无法解析 JSON");
            return new BigDecimal(text.substring(start, index));
        }

        private boolean startsWith(String value) {
            return text.startsWith(value, index);
        }

        private boolean peek(char expected) {
            skipWhitespace();
            return index < text.length() && text.charAt(index) == expected;
        }

        private void expect(char expected) {
            skipWhitespace();
            if (index >= text.length() || text.charAt(index) != expected) {
                throw new IllegalArgumentException("JSON 格式错误，期望：" + expected);
            }
            index++;
        }

        private void skipWhitespace() {
            while (index < text.length() && Character.isWhitespace(text.charAt(index))) {
                index++;
            }
        }
    }
}
