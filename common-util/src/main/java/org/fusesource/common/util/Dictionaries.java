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
