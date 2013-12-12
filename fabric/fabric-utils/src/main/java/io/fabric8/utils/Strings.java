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
package io.fabric8.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

public class Strings {

    /**
     * Returns true if the given text is not null and not empty
     */
    public static boolean notEmpty(String text) {
        return text != null && text.length() > 0;
    }

    public static String nullIfEmpty(String value) {
        if (value == null || value.length() == 0) {
            return null;
        } else {
            return value;
        }
    }

    public static String emptyIfNull(String value) {
        if (value == null) {
            return "";
        } else {
            return value;
        }
    }

    public static String defaultIfEmpty(String value, String defaultValue) {
        return notEmpty(value) ? value : defaultValue;
    }

    /**
     * splits a string into a list of strings, ignoring the empty string
     */
    public static List<String> splitAsList(String text, String delimiter) {
        List<String> answer = new ArrayList<String>();
        if (text != null && text.length() > 0) {
            answer.addAll(Arrays.asList(text.split(delimiter)));
        }
        return answer;
    }

    /**
     * splits a string into a list of strings.  Trims the results and ignores empty strings
     */
    public static List<String> splitAndTrimAsList(String text, String sep) {
        ArrayList<String> answer = new ArrayList<String>();
        if (text != null && text.length() > 0) {
            for (String v : text.split(sep)) {
                String trim = v.trim();
                if (trim.length() > 0) {
                    answer.add(trim);
                }
            }
        }
        return answer;
    }

    /**
     * joins a collection of objects together as a String using a separator
     */
    public static String join(final Collection<?> collection, final String separator) {
        StringBuffer buffer = new StringBuffer();
        boolean first = true;
        Iterator<?> iter = collection.iterator();
        while (iter.hasNext()) {
            Object next = iter.next();
            if (first) {
                first = false;
            } else {
                buffer.append(separator);
            }
            buffer.append(next);
        }
        return buffer.toString();
    }

    /**
     * joins a collection of objects together as a String using a separator
     */
    public static String join(final String separator, Object... objects) {
        StringBuffer buffer = new StringBuffer();
        boolean first = true;
        for (Object object : objects) {
            if (first) {
                first = false;
            } else {
                buffer.append(separator);
            }
            buffer.append(object);
        }
        return buffer.toString();
    }

    /**
     * joins a collection of objects together as a String using a separator, filtering out null values
     */
    public static String joinNotNull(final String separator, Object... objects) {
        StringBuffer buffer = new StringBuffer();
        boolean first = true;
        for (Object object : objects) {
            if (object == null) continue;
            if (first) {
                first = false;
            } else {
                buffer.append(separator);
            }
            buffer.append(object);
        }
        return buffer.toString();
    }


    public static String toString(Object object) {
        if (object == null) {
            return "null";
        } else if (object instanceof Object[]) {
            return Arrays.asList((Object[]) object).toString();
        } else {
            return object.toString();
        }
    }

    public static String unquote(String text) {
        if (text != null && text.startsWith("\"") && text.endsWith("\"")) {
            return text.substring(1, text.length() - 1);
        } else {
            return text;
        }
    }

    public static boolean isNullOrBlank(String value) {
        return value == null || value.length() == 0 || value.trim().length() == 0;
    }

    public static boolean isNotBlank(String text) {
        return !isNullOrBlank(text);
    }

    public static List<String> parseDelimitedString(String value, String delim) {
        return parseDelimitedString(value, delim, true);
    }

    /**
     * Parses delimited string and returns an array containing the tokens. This
     * parser obeys quotes, so the delimiter character will be ignored if it is
     * inside of a quote. This method assumes that the quote character is not
     * included in the set of delimiter characters.
     *
     * @param value the delimited string to parse.
     * @param delim the characters delimiting the tokens.
     * @return a list of string or an empty list if there are none.
     */
    public static List<String> parseDelimitedString(String value, String delim, boolean trim) {
        if (value == null) {
            value = "";
        }

        List<String> list = new ArrayList();

        int CHAR = 1;
        int DELIMITER = 2;
        int STARTQUOTE = 4;
        int ENDQUOTE = 8;

        StringBuffer sb = new StringBuffer();

        int expecting = (CHAR | DELIMITER | STARTQUOTE);

        boolean isEscaped = false;
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);

            boolean isDelimiter = (delim.indexOf(c) >= 0);

            if (!isEscaped && (c == '\\')) {
                isEscaped = true;
                continue;
            }

            if (isEscaped) {
                sb.append(c);
            } else if (isDelimiter && ((expecting & DELIMITER) > 0)) {
                if (trim) {
                    list.add(sb.toString().trim());
                } else {
                    list.add(sb.toString());
                }
                sb.delete(0, sb.length());
                expecting = (CHAR | DELIMITER | STARTQUOTE);
            } else if ((c == '"') && ((expecting & STARTQUOTE) > 0)) {
                sb.append(c);
                expecting = CHAR | ENDQUOTE;
            } else if ((c == '"') && ((expecting & ENDQUOTE) > 0)) {
                sb.append(c);
                expecting = (CHAR | STARTQUOTE | DELIMITER);
            } else if ((expecting & CHAR) > 0) {
                sb.append(c);
            } else {
                throw new IllegalArgumentException("Invalid delimited string: " + value);
            }

            isEscaped = false;
        }

        if (sb.length() > 0) {
            if (trim) {
                list.add(sb.toString().trim());
            } else {
                list.add(sb.toString());
            }
        }

        return list;
    }
}
