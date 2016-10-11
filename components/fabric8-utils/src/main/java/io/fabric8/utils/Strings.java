/**
 *  Copyright 2005-2016 Red Hat, Inc.
 *
 *  Red Hat licenses this file to you under the Apache License, version
 *  2.0 (the "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 *  implied.  See the License for the specific language governing
 *  permissions and limitations under the License.
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
     * Returns the first string value which is not null and not blank
     */
    public static String firstNonBlank(String... values) {
        for (String value : values) {
            if (notEmpty(value)) {
                return value;
            }
        }
        return null;
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

    /**
     * This method is used to pad a line by max length with a delimiter.
     * @param delimiter - String that will be used to concatenate to the end of line
     * @param str - string input that will be changed and returned
     * @param max - size of the maximum length of a string
     * @return
     */

    public static String rpad(String delimiter, String str, int max){
        for(int i=1; i<= max; i++){
            str = str.concat(delimiter);
        }
        return str;
    }

    /**
     *  When you have a line that has a maximum length and want to pad the remaining space with ' ' whitespace then
     *  this method is for you.
     * @param delimiter - String that will be used to concatenate to the end of line
     * @param str - string input that will be changed and returned
     * @param max - size of the maximum length of a string
     * @return
     */
    public static String rpadByMaxSize(String delimiter, String str, int max){
        int len= max- str.length();
        for(int i=1; i<= len; i++){
            str = str.concat(delimiter);
        }
        return str;
    }

    public static boolean isNullOrBlank(String value) {
        return value == null || value.length() == 0 || value.trim().length() == 0;
    }

    public static boolean isNotBlank(String text) {
        return !isNullOrBlank(text);
    }

    public static String stripPrefix(String value, String suffix) {
        if (!value.startsWith(suffix)) {
            return value;
        } else {
            return value.substring(suffix.length());
        }
    }
    public static String stripSuffix(String value, String suffix) {
        if (!value.endsWith(suffix)) {
            return value;
        } else {
            return value.substring(0, value.length() - suffix.length());
        }
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

    /**
     * Converts the given text with the given separator into camelCase
     */
    public static String convertToCamelCase(String text, String separator) {
        StringBuffer buffer = new StringBuffer();
        String[] words = text.split(separator);
        boolean first = true;
        for (String word : words) {
            if (first) {
                buffer.append(word);
                first = false;
            } else {
                if (word.length() > 0) {
                    buffer.append(Character.toUpperCase(word.charAt(0)));
                    if (word.length() > 1) {
                        buffer.append(word.substring(1));
                    }
                }
            }
        }
        return buffer.toString();
    }

    /**
     * Splits a CamelCase string using a space between them.
     */
    public static String splitCamelCase(String text) {
        return splitCamelCase(text, " ");
    }

    /**
     * Splits a CamelCase string using a separator string between them.
     */
    public static String splitCamelCase(String text, String separator) {
            StringBuilder buffer = new StringBuilder();
            char last = 'A';
            for (char c: text.toCharArray()) {
                if (Character.isLowerCase(last) && Character.isUpperCase(c)) {
                    buffer.append(separator);
                }
                buffer.append(c);
                last = c;
            }
            return buffer.toString();
        }


    // String based functions


    public static Function<String, String> toLowerCaseFunction() {
        return new Function<String, String>() {
            @Override
            public String toString() {
                return "toLowerCaseFunction()";
            }

            @Override
            public String apply(String value) {
                if (value != null) {
                    return value.toLowerCase();
                }
                return null;
            }
        };
    }

    public static Function<String, String> toUpperCaseFunction() {
        return new Function<String, String>() {
            @Override
            public String toString() {
                return "toUpperCaseFunction()";
            }

            @Override
            public String apply(String value) {
                if (value != null) {
                    return value.toUpperCase();
                }
                return null;
            }
        };
    }

    /**
     * Converts a string to a valid environment variable by removing bad characters like '.' and ' '
     */
    public static Function<String, String> toEnvironmentVariableFunction() {
        return new Function<String, String>() {
            @Override
            public String toString() {
                return "toEnvironmentVariableFunction()";
            }

            @Override
            public String apply(String key) {
                if (key != null) {
                    // lets replace any dots in the env var name
                    return key.replace('.', '_').replace(' ', '_');
                }
                return null;
            }
        };
    }

    /**
     * Replaces all occurrencies of the from text with to text without any regular expressions
     */
    public static String replaceAllWithoutRegex(String text, String from, String to) {
        if (text == null) {
            return null;
        }
        int idx = 0;
        while (true) {
            idx = text.indexOf(from, idx);
            if (idx >= 0) {
                text = text.substring(0, idx) + to + text.substring(idx + from.length());

                // lets start searching after the end of the `to` to avoid possible infinite recursion
                idx += to.length();
            } else {
                break;
            }
        }
        return text;
    }
}
