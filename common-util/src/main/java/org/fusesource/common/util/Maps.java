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

import java.util.Map;

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
}
