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

import java.util.HashMap;
import java.util.Map;

public class XmlHelper {

    protected static Map<Character, String> encodingMap = new HashMap<Character, String>();

    static {
        encodingMap.put('"', "&quot;");
        encodingMap.put('&', "&amp;");
        encodingMap.put('<', "&lt;");
        encodingMap.put('>', "&gt;");
    }

//    /**
//     * Returns the integer value of the given attribute or return the default value if none is provided
//     */
//    public static int attributeIntValue(Node e, String name, int defaultValue) {
//        e.get
//        e.attribute(name) match {
//    case Some(s) =>
//        if (s.isEmpty) {
//            defaultValue
//        }
//        else {
//            s.head.text.toInt
//        }
//    case _ => defaultValue
//        }
//        }
//        /**
//         * Returns the double value of the given attribute or return the default value if none is provided
//         */
//        def attributeDoubleValue(e: Node, name: String, defaultValue: Double = -1): Double = {
//        e.attribute(name) match {
//    case Some(s) =>
//        if (s.isEmpty) {
//            defaultValue
//        }
//        else {
//            s.head.text.toDouble
//        }
//    case _ => defaultValue
//        }
//        }

    public static String escape(String text) {
        StringBuffer sb = new StringBuffer();
        for (char c : text.toCharArray()) {
            escape(c, sb);
        }
        return sb.toString();
    }

    public static String unescape(String text) {
        // TODO would be much more efficient to find all & first!
        String answer = text;
        for (Map.Entry<Character, String> e : encodingMap.entrySet()) {
            answer = answer.replaceAll(e.getValue(), e.getKey().toString());
        }
        return answer;
    }

    private static StringBuffer escape(char c, StringBuffer buffer) {
        String encoded = encodingMap.get(c);
        return buffer.append(encoded == null ? Character.toString(c) : encoded);
    }

}
