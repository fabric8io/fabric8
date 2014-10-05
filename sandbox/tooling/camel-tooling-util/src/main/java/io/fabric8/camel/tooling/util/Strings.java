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
package io.fabric8.camel.tooling.util;

import java.beans.Introspector;

public class Strings {

    public static boolean isEmpty(String text) {
        return text == null || text.length() == 0;
    }

    public String splitCamelCase(String text) {
        StringBuilder buffer = new StringBuilder();
        char last = 'A';
        for (char c: text.toCharArray()) {
            if (Character.isLowerCase(last) && Character.isUpperCase(c)) {
                buffer.append(" ");
            }
            buffer.append(c);
            last = c;
        }
        return buffer.toString();
    }

    public String capitalize(String text) {
        if (!isEmpty(text)) {
            return text.substring(0, 1).toUpperCase() + text.substring(1);
        }
        return text;
    }

    public String decapitalize(String text) {
        return Introspector.decapitalize(text);
    }

    public String toJson(Object n) {
        if (n == null) {
            return "null";
        }
        if (n instanceof Number) {
            return n.toString();
        }
        if (n instanceof String) {
            return "\"" + ((String) n).replaceAll("\\n", "\\\\n") + "\"";
        }
        return "\"" + n.toString() + "\"";
    }

}
