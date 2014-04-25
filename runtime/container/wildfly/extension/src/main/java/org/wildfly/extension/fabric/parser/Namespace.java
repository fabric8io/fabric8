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

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

/**
 * An enumeration of the supported Fabric subsystem namespaces.
 *
 * @since 13-Nov-2013
 */
enum Namespace {

    // must be first
    UNKNOWN(null),
    VERSION_1_0("urn:jboss:domain:fabric:1.0")
    ;

    /**
     * The current namespace version.
     */
    static final Namespace CURRENT = VERSION_1_0;

    private final String name;

    Namespace(final String name) {
        this.name = name;
    }

    /**
     * Get the URI of this namespace.
     */
    String getUriString() {
        return name;
    }

    /**
     * Set of all namespaces, excluding the special {@link #UNKNOWN} value.
     */
    static final EnumSet<Namespace> STANDARD_NAMESPACES = EnumSet.complementOf(EnumSet.of(Namespace.UNKNOWN));

    private static final Map<String, Namespace> MAP;

    static {
        final Map<String, Namespace> map = new HashMap<String, Namespace>();
        for (Namespace namespace : values()) {
            final String name = namespace.getUriString();
            if (name != null) map.put(name, namespace);
        }
        MAP = map;
    }

    static Namespace forUri(String uri) {
        final Namespace element = MAP.get(uri);
        return element == null ? UNKNOWN : element;
    }
}
