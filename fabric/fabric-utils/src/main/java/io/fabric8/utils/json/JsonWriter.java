/**
 * Copyright (C) FuseSource, Inc.
 * http://fusesource.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.fabric8.utils.json;

import java.io.IOException;
import java.io.Writer;
import java.util.Collection;
import java.util.Map;

/**
 */
public class JsonWriter {

    public static void write(Writer writer, Object value) throws IOException {
        if (value instanceof Map) {
            writeObject(writer, (Map) value);
        } else if (value instanceof Collection) {
            writeArray(writer, (Collection) value);
        } else if (value instanceof Number) {
            writeNumber(writer, (Number) value);
        } else if (value instanceof String) {
            writeString(writer, (String) value);
        } else if (value instanceof Boolean) {
            writeBoolean(writer, (Boolean) value);
        } else if (value == null) {
            writeNull(writer);
        } else {
            throw new IllegalArgumentException("Unsupported value: " + value);
        }
    }

    private static void writeObject(Writer writer, Map<?, ?> value) throws IOException {
        writer.append('{');
        boolean first = true;
        for (Map.Entry entry : value.entrySet()) {
            if (!first) {
                writer.append(',');
            } else {
                first = false;
            }
            writeString(writer, (String) entry.getKey());
            writer.append(':');
            write(writer, entry.getValue());
        }
        writer.append('}');
    }

    private static void writeString(Writer writer, String value) throws IOException {
        writer.append('"');
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '\"':
                case '\\':
                case '\b':
                case '\f':
                case '\n':
                case '\r':
                case '\t':
                    writer.append('\\');
                    writer.append(c);
                    break;
                default:
                    if (c < ' ' || (c >= '\u0080' && c < '\u00a0') || (c >= '\u2000' && c < '\u2100')) {
                        String s = Integer.toHexString(c);
                        writer.append('\\');
                        writer.append('u');
                        for (int j = s.length(); j < 4; j++) {
                            writer.append('0');
                        }
                        writer.append(s);
                    } else {
                        writer.append(c);
                    }
                    break;
            }
        }
        writer.append('"');
    }

    private static void writeNumber(Writer writer, Number value) throws IOException {
        writer.append(value.toString());
    }

    private static void writeBoolean(Writer writer, Boolean value) throws IOException {
        writer.append(Boolean.toString(value));
    }

    private static void writeArray(Writer writer, Collection<?> value) throws IOException {
        writer.append('[');
        boolean first = true;
        for (Object obj : value) {
            if (!first) {
                writer.append(',');
            } else {
                first = false;
            }
            write(writer, obj);
        }
        writer.append(']');
    }

    private static void writeNull(Writer writer) throws IOException {
        writer.append("null");
    }
}
