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

import java.util.Map;
import java.util.Properties;

/**
 * Helper methods for extracting values form a Properties object
 */
public class PropertiesHelper {

    public static Long getLong(Properties properties, String key, Long defaultValue) {
        Object value = properties.get(key);
        if (value instanceof String) {
            return Long.parseLong(value.toString());
        } else if (value instanceof Long) {
            return (Long) value;
        } else if (value instanceof Number) {
            Number number = (Number)value;
            return number.longValue();
        }
        return defaultValue;
    }

    public static long getLongValue(Properties properties, String key, long defaultValue) {
        return getLong(properties, key, defaultValue);
    }

    public static long getLongValue(Map<String, String> map, String key, long defaultValue) {
        Properties properties = new Properties();
        properties.putAll(map);
        return getLong(properties, key, defaultValue);
    }
}
