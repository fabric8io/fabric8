/**
 *  Copyright 2005-2015 Red Hat, Inc.
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
package io.fabric8.selenium.support;

/**
 */
public class NameGenerator {
    private static String chars = "0123456789abcdefghijklmnopqrstuvwzyz";

    public static String generateName() {
        int charCount = chars.length();
        long value = System.currentTimeMillis();
        StringBuilder buffer = new StringBuilder();
        while (value > 0L) {
            int digit = (int) (value % charCount);
            value = (value - digit) / charCount;
            buffer.append(chars.charAt(digit));
        }
        return buffer.reverse().toString();

    }
}
