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
package io.fabric8.forge.camel.commands.project;

import java.util.List;
import java.util.Map;

import org.apache.camel.catalog.CamelComponentCatalog;
import org.apache.camel.catalog.DefaultCamelComponentCatalog;
import org.apache.camel.catalog.JSonSchemaHelper;

public final class CamelCatalogHelper {

    /**
     * Attempts to find the maven archetype name for the given Camel component name.
     *
     * @param name the component name, such as <tt>xquery</tt> which has the archetype name <tt>camel-saxon</tt>
     * @return the archetype name, or <tt>null</tt> if not possible to find a valid archetype name
     */
    public static String findComponentArchetype(String name) {
        // return the name as-is if its already an archetype syntax
        if (name == null || name.startsWith("camel-")) {
            return name;
        }

        // use the camel catalog to lookup the component name -> artifact id
        CamelComponentCatalog catalog = new DefaultCamelComponentCatalog();
        String json = catalog.componentJSonSchema(name);
        if (json == null) {
            return null;
        }

        List<Map<String, String>> data = JSonSchemaHelper.parseJsonSchema("component", json, false);
        for (Map<String, String> row : data) {
            if (row.get("artifactId") != null) {
                return row.get("artifactId");
            }
        }

        return null;
    }

    /**
     * Attempts to find the maven archetype name for the given Camel data format name.
     *
     * @param name the data format name, such as <tt>json-jackson</tt> which has the archetype name <tt>camel-jackson</tt>
     * @return the archetype name, or <tt>null</tt> if not possible to find a valid archetype name
     */
    public static String findDataFormatArchetype(String name) {
        // return the name as-is if its already an archetype syntax
        if (name == null || name.startsWith("camel-")) {
            return name;
        }

        // use the camel catalog to lookup the dataformat name -> artifact id
        CamelComponentCatalog catalog = new DefaultCamelComponentCatalog();
        String json = catalog.dataFormatJSonSchema(name);
        if (json == null) {
            return null;
        }

        List<Map<String, String>> data = JSonSchemaHelper.parseJsonSchema("dataformat", json, false);
        for (Map<String, String> row : data) {
            if (row.get("artifactId") != null) {
                return row.get("artifactId");
            }
        }

        return null;
    }

    /**
     * Attempts to find the maven archetype name for the given Camel language name.
     *
     * @param name the language name, such as <tt>spel</tt> which has the archetype name <tt>camel-spring</tt>
     * @return the archetype name, or <tt>null</tt> if not possible to find a valid archetype name
     */
    public static String findLanguageArchetype(String name) {
        // return the name as-is if its already an archetype syntax
        if (name == null || name.startsWith("camel-")) {
            return name;
        }

        // use the camel catalog to lookup the language name -> artifact id
        CamelComponentCatalog catalog = new DefaultCamelComponentCatalog();
        String json = catalog.languageJSonSchema(name);
        if (json == null) {
            return null;
        }

        List<Map<String, String>> data = JSonSchemaHelper.parseJsonSchema("language", json, false);
        for (Map<String, String> row : data) {
            if (row.get("artifactId") != null) {
                return row.get("artifactId");
            }
        }

        return null;
    }
}
