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

public class Functions {

    public static Function<String, String> chopLength(final int maxLen) {
        return new Function<String, String>() {
            @Override
            public String toString() {
                return "chopLength(" + maxLen + ")";
            }

            @Override
            public String apply(String value) {
                if (value == null) {
                    return null;
                }
                if (value.length() > maxLen) {
                    return value.substring(0, maxLen);
                }
                return value;
            }
        };
    }

    public static <T> Function<T,T> noop() {
        return new Function<T,T>() {
            @Override
            public String toString() {
                return "noopFunction()";
            }

            @Override
            public T apply(T value) {
                return value;
            }
        };
    }
}
