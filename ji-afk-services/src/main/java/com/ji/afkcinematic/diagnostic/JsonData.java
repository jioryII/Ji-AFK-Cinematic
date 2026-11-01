package com.ji.afkcinematic.diagnostic;

import java.util.*;
import java.math.BigDecimal;

/** Bounded JSON data parser. No class names, reflection, executable objects or permissive coercions. */
final class JsonData {
    private final String text;
    private final int maximumDepth;
    private int position;
    private JsonData(String text, int maximumDepth) { this.text = text; this.maximumDepth = maximumDepth; }
    static Object parse(String text, int maximumDepth) {
        var parser = new JsonData(Objects.requireNonNull(text), maximumDepth);
        Object value = parser.value(0); parser.space();
        if (parser.position != text.length()) throw invalid();
        return value;
    }
    private Object value(int depth) {
        if (depth > maximumDepth) throw invalid();
        space(); if (position >= text.length()) throw invalid();
        char c = text.charAt(position);
        if (c == '"') return string();
        if (c == '{') {
            position++; space(); var result = new LinkedHashMap<String, Object>();
            if (take('}')) return result;
            do {
                space(); if (position >= text.length() || text.charAt(position) != '"') throw invalid();
                String key = string(); space(); if (!take(':') || result.containsKey(key)) throw invalid();
                result.put(key, value(depth + 1)); space();
                if (take('}')) return result;
            } while (take(','));
            throw invalid();
        }
        if (c == '[') {
            position++; space(); var result = new ArrayList<Object>();
            if (take(']')) return result;
            do { result.add(value(depth + 1)); space(); if (take(']')) return result; } while (take(','));
            throw invalid();
        }
        for (String literal : List.of("true", "false", "null")) {
            if (text.startsWith(literal, position)) { position += literal.length(); return literal.equals("null") ? null : literal.equals("true"); }
        }
        int start = position;
        if (take('-') && position == text.length()) throw invalid();
        if (!take('0')) { if (!digit() || text.charAt(position) == '0') throw invalid(); while (digit()) position++; }
        if (take('.')) { if (!digit()) throw invalid(); while (digit()) position++; }
        if (take('e') || take('E')) { if (!take('+')) take('-'); if (!digit()) throw invalid(); while (digit()) position++; }
        try { var number = new BigDecimal(text.substring(start, position));
            try { return number.intValueExact(); } catch (ArithmeticException wide) { return number; }
        } catch (NumberFormatException failure) { throw invalid(); }
    }
    private boolean digit() { return position < text.length() && text.charAt(position) >= '0' && text.charAt(position) <= '9'; }
    private String string() {
        if (!take('"')) throw invalid(); var out = new StringBuilder();
        while (position < text.length()) {
            char c = text.charAt(position++);
            if (c == '"') {
                String value = out.toString();
                for (int i = 0; i < value.length(); i++) {
                    char part = value.charAt(i);
                    if (Character.isHighSurrogate(part)) {
                        if (++i >= value.length() || !Character.isLowSurrogate(value.charAt(i))) throw invalid();
                    } else if (Character.isLowSurrogate(part)) throw invalid();
                }
                return value;
            }
            if (c < 32) throw invalid();
            if (c == '\\') {
                if (position >= text.length()) throw invalid();
                char escaped = text.charAt(position++);
                c = switch (escaped) {
                    case '"', '\\', '/' -> escaped;
                    case 'b' -> '\b'; case 'f' -> '\f'; case 'n' -> '\n'; case 'r' -> '\r'; case 't' -> '\t';
                    case 'u' -> {
                        if (position + 4 > text.length()) throw invalid();
                        String hex = text.substring(position, position + 4);
                        if (!hex.matches("[0-9A-Fa-f]{4}")) throw invalid();
                        position += 4; yield (char)Integer.parseInt(hex, 16);
                    }
                    default -> throw invalid();
                };
            }
            out.append(c);
        }
        throw invalid();
    }
    private boolean take(char c) { if (position < text.length() && text.charAt(position) == c) { position++; return true; } return false; }
    private void space() { while (position < text.length() && " \r\n\t".indexOf(text.charAt(position)) >= 0) position++; }
    private static IllegalArgumentException invalid() { return new IllegalArgumentException("INVALID_JSON"); }
}
