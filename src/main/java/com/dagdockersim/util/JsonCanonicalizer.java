package com.dagdockersim.util;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class JsonCanonicalizer {
    private JsonCanonicalizer() {
    }

    public static String canonicalJson(Object value) {
        StringBuilder builder = new StringBuilder();
        appendCanonical(builder, value);
        return builder.toString();
    }

    public static Object deepCopy(Object value) {
        if (value == null || value instanceof String || value instanceof Number || value instanceof Boolean) {
            return value;
        }
        if (value instanceof Map<?, ?>) {
            Map<String, Object> copied = new LinkedHashMap<String, Object>();
            List<String> keys = new ArrayList<String>();
            for (Object key : ((Map<?, ?>) value).keySet()) {
                keys.add(String.valueOf(key));
            }
            Collections.sort(keys);
            for (String key : keys) {
                copied.put(key, deepCopy(((Map<?, ?>) value).get(key)));
            }
            return copied;
        }
        if (value instanceof Collection<?>) {
            List<Object> copied = new ArrayList<Object>();
            for (Object item : (Collection<?>) value) {
                copied.add(deepCopy(item));
            }
            return copied;
        }
        if (value.getClass().isArray()) {
            List<Object> copied = new ArrayList<Object>();
            int length = Array.getLength(value);
            for (int index = 0; index < length; index++) {
                copied.add(deepCopy(Array.get(value, index)));
            }
            return copied;
        }
        return String.valueOf(value);
    }

    @SuppressWarnings("unchecked")
    private static void appendCanonical(StringBuilder builder, Object value) {
        if (value == null) {
            builder.append("null");
            return;
        }
        if (value instanceof String) {
            appendEscaped(builder, (String) value);
            return;
        }
        if (value instanceof Number || value instanceof Boolean) {
            builder.append(value.toString());
            return;
        }
        if (value instanceof Map<?, ?>) {
            builder.append('{');
            List<Map.Entry<Object, Object>> entries = new ArrayList<Map.Entry<Object, Object>>();
            for (Map.Entry<?, ?> entry : ((Map<?, ?>) value).entrySet()) {
                entries.add((Map.Entry<Object, Object>) entry);
            }
            Collections.sort(entries, Comparator.comparing(entry -> String.valueOf(entry.getKey())));
            for (int index = 0; index < entries.size(); index++) {
                if (index > 0) {
                    builder.append(',');
                }
                Map.Entry<Object, Object> entry = entries.get(index);
                appendEscaped(builder, String.valueOf(entry.getKey()));
                builder.append(':');
                appendCanonical(builder, entry.getValue());
            }
            builder.append('}');
            return;
        }
        if (value instanceof Collection<?>) {
            builder.append('[');
            int index = 0;
            for (Object item : (Collection<?>) value) {
                if (index > 0) {
                    builder.append(',');
                }
                appendCanonical(builder, item);
                index++;
            }
            builder.append(']');
            return;
        }
        if (value.getClass().isArray()) {
            builder.append('[');
            int length = Array.getLength(value);
            for (int index = 0; index < length; index++) {
                if (index > 0) {
                    builder.append(',');
                }
                appendCanonical(builder, Array.get(value, index));
            }
            builder.append(']');
            return;
        }
        appendEscaped(builder, String.valueOf(value));
    }

    private static void appendEscaped(StringBuilder builder, String text) {
        builder.append('"');
        for (int index = 0; index < text.length(); index++) {
            char ch = text.charAt(index);
            switch (ch) {
                case '"':
                    builder.append("\\\"");
                    break;
                case '\\':
                    builder.append("\\\\");
                    break;
                case '\b':
                    builder.append("\\b");
                    break;
                case '\f':
                    builder.append("\\f");
                    break;
                case '\n':
                    builder.append("\\n");
                    break;
                case '\r':
                    builder.append("\\r");
                    break;
                case '\t':
                    builder.append("\\t");
                    break;
                default:
                    if (ch < 0x20) {
                        builder.append(String.format("\\u%04x", (int) ch));
                    } else {
                        builder.append(ch);
                    }
            }
        }
        builder.append('"');
    }
}
