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
package io.fabric8.api.jmx;

import java.util.Map;
import java.util.TreeMap;

/**
 * A helper class for making links to resources in REST APIs and DTOs
 */
public class Links {
    public static String getLink(String path, String baseUri) {
        if (baseUri != null) {
            String prefix = baseUri.toString();
            if (!prefix.endsWith("/")) {
                prefix += "/";
            }
            while (path.startsWith("/")) {
                path = path.substring(1);
            }
            return prefix + path;
        } else {
            return path;
        }
    }

    public static Map<String, String> mapIdsToLinks(Iterable<String> keys, String newBaseUri) {
        Map<String, String> answer = new TreeMap<String, String>();
        for (String key : keys) {
            String link = getLink(key, newBaseUri);
            answer.put(key, link);
        }
        return answer;
    }
}
