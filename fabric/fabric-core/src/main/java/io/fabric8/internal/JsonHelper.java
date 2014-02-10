/**
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.fabric8.internal;

/**
 * Some JSON helper functions
 */
public class JsonHelper {
    /**
     * Lets make sure we encode the given string so its a valid JSON value
     * which is wrapped in quotes if its not null
     */
    public static String jsonEncodeString(String text) {
        if (text == null) {
            return "null";
        }
        StringBuilder buffer = new StringBuilder("\"");
        int length = text.length();
        for (int i = 0; i < length; i++) {
            char ch = text.charAt(i);
            if (ch == '"') {
                buffer.append("\\\"");
            } else {
                buffer.append(ch);
            }
        }
        buffer.append("\"");
        return buffer.toString();
    }
}
