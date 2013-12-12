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

/**
 * Helper class for working with arrays
 */
public class Arrays {

    /**
     * Joins the values together using the given separator between the values
     */
    public static String join(String separator, Object... values) {
        StringBuilder builder = new StringBuilder();
        boolean first = true;
        if (values != null) {
            for (Object value : values) {
                if (first) {
                    first = false;
                } else {
                    builder.append(separator);
                }
                builder.append(value);
            }
        }
        return builder.toString();
    }
}
