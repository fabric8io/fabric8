/**
 *  Copyright 2005-2014 Red Hat, Inc.
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
package io.fabric8.gateway.support;

/**
 */
public class Paths {
    protected static final String[] EMPTY_ARRAY = new String[0];

    /**
     * Splits the request URI by "/" characters into an array of paths such that
     * "foo/bar" or "/foo/bar" is turned into { "foo", "bar" } and "" or "/" is an empty array.
     */
    public static String[] splitPaths(String requestURI) {
        String[] paths = EMPTY_ARRAY;
        if (requestURI != null && requestURI.length() > 0) {
            String text = requestURI;
            int idx = text.indexOf('?');
            if (idx >= 0) {
                text = text.substring(0, idx);
            }
            while (text.startsWith("/")) {
                text = text.substring(1);
            }
            while (text.endsWith("/")) {
                text = text.substring(0, text.length() - 1);
            }
            if (text.length() > 0) {
                paths = text.split("/");
            }
        }
        return paths;
    }
}
