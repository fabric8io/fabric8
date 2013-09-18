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
package org.fusesource.common.util;

import java.util.Dictionary;
import java.util.Map;
import java.util.Properties;

/**
 * Some helper methods for working with maps
 */
public class Maps {

    /**
     * Copies the entries for the given keys form the input map to the output map
     */
    public static <K,V> void putAll(Map<K,V> output, Map<K,V> input, K... keys) {
        for (K key : keys) {
            V value = input.get(key);
            if (value != null) {
                output.put(key, value);
            }
        }
    }

    /**
     * Returns the boolean value of the given property in the map or false
     */
    public static boolean booleanValue(Map<String,?> map, String key) {
        return booleanValue(map, key, false);
    }

    /**
     * Returns the boolean value of the given property in the map or returns the default value if its not present
     */
    public static boolean booleanValue(Map<String,?> map, String key, boolean defaultValue) {
        Object obj = map.get(key);
        if (obj == null) {
            return defaultValue;
        } else if (obj instanceof Boolean) {
            Boolean value = (Boolean)obj;
            return value.booleanValue();
        }
        else {
            String text = obj.toString();
            return Boolean.parseBoolean(text);
        }
    }

    /**
     * Returns the value of the given property in the map
     */
    public static Object value(Map<String,Object> map, String key, Object defaultValue) {
        Object obj = map.get(key);
        if (obj == null) {
            return defaultValue;
        } else {
            return obj;
        }
    }

    /**
     * Returns the String value of the given property in the map or null
     */
    public static String stringValue(Map map, String key) {
        return stringValue(map, key, null);
    }

    /**
     * Returns the String value of the given property in the map if its defined or the default value
     */
    public static String stringValue(Map map, String key, String defaultValue) {
        Object obj = map.get(key);
        if (obj == null) {
            return defaultValue;
        } else if (obj instanceof String) {
            return (String) obj;
        } else {
            return String.valueOf(obj);
        }
    }

    /**
     * Sets the value in the map for the given key; if the value is null then remove the value from the map
     */
    public static void setValue(Map map, String key, Object value) {
        if (value != null) {
            map.put(key, value);
        } else {
            map.remove(key);
        }
    }
}
