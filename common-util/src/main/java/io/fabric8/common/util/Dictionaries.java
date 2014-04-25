package io.fabric8.common.util;

import java.util.Dictionary;

/**
 * A helper class for working with {@link Dictionary}
 */
public class Dictionaries {

    /**
     * Reads the specified key as a String from configuration.
     */
    public static String readString(Dictionary dictionary, String key) {
        return readString(dictionary, key, "null");
    }

    /**
     * Reads the specified key as a String from configuration or returns the default value
     */
    public static String readString(Dictionary dictionary, String key, String defaultValue) {
        Object obj = dictionary.get(key);
        if (obj == null) {
            return defaultValue;
        } else if (obj instanceof String) {
            return (String) obj;
        } else {
            return String.valueOf(obj);
        }
    }
}
