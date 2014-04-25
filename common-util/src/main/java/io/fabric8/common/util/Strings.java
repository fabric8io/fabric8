package io.fabric8.common.util;

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
}
