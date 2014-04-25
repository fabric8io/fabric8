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

package org.wildfly.extension.fabric.parser;

import java.util.HashMap;
import java.util.Map;


/**
 * Constants related to namespace {@link Namespace#VERSION_1_0}.
 *
 * @since 13-Nov-2013
 */
interface Namespace10 {

    enum Attribute {
        UNKNOWN(null),
        ID("id"),
        ;
        private final String name;

        Attribute(final String name) {
            this.name = name;
        }

        /**
         * Get the local name of this attribute.
         *
         * @return the local name
         */
        public String getLocalName() {
            return name;
        }

        private static final Map<String, Attribute> MAP;

        static {
            final Map<String, Attribute> map = new HashMap<String, Attribute>();
            for (Attribute element : values()) {
                final String name = element.getLocalName();
                if (name != null) map.put(name, element);
            }
            MAP = map;
        }

        public static Attribute forName(String localName) {
            final Attribute element = MAP.get(localName);
            return element == null ? UNKNOWN : element;
        }

        @Override
        public String toString() {
            return getLocalName();
        }
    }

    enum Element {
        // must be first
        UNKNOWN(null),
        ;

        private final String name;

        Element(final String name) {
            this.name = name;
        }

        /**
         * Get the local name of this element.
         *
         * @return the local name
         */
        public String getLocalName() {
            return name;
        }

        private static final Map<String, Element> MAP;

        static {
            final Map<String, Element> map = new HashMap<String, Element>();
            for (Element element : values()) {
                final String name = element.getLocalName();
                if (name != null) map.put(name, element);
            }
            MAP = map;
        }

        public static Element forName(String localName) {
            final Element element = MAP.get(localName);
            return element == null ? UNKNOWN : element;
        }
    }
}
