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
package io.fabric8.forge.camel.commands.project.helper;

public final class StringHelper {

    /**
     * Replaces all the from tokens in the given input string.
     * <p/>
     * This implementation is not recursive, not does it check for tokens in the replacement string.
     *
     * @param input the input string
     * @param from  the from string, must <b>not</b> be <tt>null</tt> or empty
     * @param to    the replacement string, must <b>not</b> be empty
     * @return the replaced string, or the input string if no replacement was needed
     * @throws IllegalArgumentException if the input arguments is invalid
     */
    public static String replaceAll(String input, String from, String to) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        if (from == null) {
            throw new IllegalArgumentException("from cannot be null");
        }
        if (to == null) {
            // to can be empty, so only check for null
            throw new IllegalArgumentException("to cannot be null");
        }

        // fast check if there is any from at all
        if (!input.contains(from)) {
            return input;
        }

        final int len = from.length();
        final int max = input.length();
        StringBuilder sb = new StringBuilder(max);
        for (int i = 0; i < max; ) {
            if (i + len <= max) {
                String token = input.substring(i, i + len);
                if (from.equals(token)) {
                    sb.append(to);
                    // fast forward
                    i = i + len;
                    continue;
                }
            }

            // append single char
            sb.append(input.charAt(i));
            // forward to next
            i++;
        }
        return sb.toString();
    }

    public static String removeLeadingAndEndingQuotes(String s) {
        if (s == null || s.isEmpty()) {
            return s;
        }

        String copy = s.trim();
        if (copy.startsWith("'") && copy.endsWith("'")) {
            return copy.substring(1, copy.length() - 1);
        }
        if (copy.startsWith("\"") && copy.endsWith("\"")) {
            return copy.substring(1, copy.length() - 1);
        }

        // no quotes, so return as-is
        return s;
    }

}
