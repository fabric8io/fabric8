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

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.camel.catalog.CamelCatalog;
import org.apache.camel.catalog.DefaultCamelCatalog;
import org.apache.camel.catalog.JSonSchemaHelper;

public final class CamelCatalogHelper {

    /**
     * Returns the text in title case
     */
    public static String asTitleCase(String text) {
        StringBuilder sb = new StringBuilder();
        boolean next = true;

        for (char c : text.toCharArray()) {
            if (Character.isSpaceChar(c)) {
                next = true;
            } else if (next) {
                c = Character.toTitleCase(c);
                next = false;
            }
            sb.append(c);
        }

        return sb.toString();
    }

    public static String endpointComponentName(String uri) {
        if (uri != null) {
            int idx = uri.indexOf(":");
            if (idx > 0) {
                return uri.substring(0, idx);
            }
        }
        return null;
    }

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
        CamelCatalog catalog = new DefaultCamelCatalog();
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
        CamelCatalog catalog = new DefaultCamelCatalog();
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
        CamelCatalog catalog = new DefaultCamelCatalog();
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

    public static Set<String> componentsFromArtifact(String artifactId) {
        Set<String> answer = new TreeSet<String>();

        // use the camel catalog to find what components the artifact has
        CamelCatalog catalog = new DefaultCamelCatalog();
        for (String name : catalog.findComponentNames()) {
            String json = catalog.componentJSonSchema(name);
            if (json != null) {
                List<Map<String, String>> data = JSonSchemaHelper.parseJsonSchema("component", json, false);
                String scheme = null;
                String artifact = null;
                for (Map<String, String> row : data) {
                    if (row.get("artifactId") != null) {
                        artifact = row.get("artifactId");
                    }
                    if (row.get("scheme") != null) {
                        scheme = row.get("scheme");
                    }
                }
                if (artifactId.equals(artifact) && scheme != null) {
                    answer.add(scheme);
                }
            }
        }

        return answer;
    }

    public static Set<String> dataFormatsFromArtifact(String artifactId) {
        Set<String> answer = new TreeSet<String>();

        // use the camel catalog to find what components the artifact has
        CamelCatalog catalog = new DefaultCamelCatalog();
        for (String name : catalog.findDataFormatNames()) {
            String json = catalog.dataFormatJSonSchema(name);
            if (json != null) {
                List<Map<String, String>> data = JSonSchemaHelper.parseJsonSchema("dataformat", json, false);
                String df = null;
                String artifact = null;
                for (Map<String, String> row : data) {
                    if (row.get("artifactId") != null) {
                        artifact = row.get("artifactId");
                    }
                    if (row.get("name") != null) {
                        df = row.get("name");
                    }
                }
                if (artifactId.equals(artifact) && df != null) {
                    answer.add(df);
                }
            }
        }

        return answer;
    }

    public static Set<String> languagesFromArtifact(String artifactId) {
        Set<String> answer = new TreeSet<String>();

        // use the camel catalog to find what components the artifact has
        CamelCatalog catalog = new DefaultCamelCatalog();
        for (String name : catalog.findLanguageNames()) {
            String json = catalog.languageJSonSchema(name);
            if (json != null) {
                List<Map<String, String>> data = JSonSchemaHelper.parseJsonSchema("language", json, false);
                String lan = null;
                String artifact = null;
                for (Map<String, String> row : data) {
                    if (row.get("artifactId") != null) {
                        artifact = row.get("artifactId");
                    }
                    if (row.get("name") != null) {
                        lan = row.get("name");
                    }
                }
                if (artifactId.equals(artifact) && lan != null) {
                    answer.add(lan);
                }
            }
        }

        return answer;
    }

    /**
     * Checks whether the given value is matching the default value from the given component.
     *
     * @param scheme     the component name
     * @param key        the option key
     * @param value      the option value
     * @return <tt>true</tt> if matching the default value, <tt>false</tt> otherwise
     */
    public static boolean isDefaultValue(String scheme, String key, String value) {
        // use the camel catalog
        CamelCatalog catalog = new DefaultCamelCatalog();
        String json = catalog.componentJSonSchema(scheme);
        if (json == null) {
            throw new IllegalArgumentException("Could not find catalog entry for component name: " + scheme);
        }

        List<Map<String, String>> data = JSonSchemaHelper.parseJsonSchema("properties", json, true);
        if (data != null) {
            for (Map<String, String> propertyMap : data) {
                String name = propertyMap.get("name");
                String defaultValue = propertyMap.get("defaultValue");
                if (key.equals(name)) {
                    return value.equalsIgnoreCase(defaultValue);
                }
            }
        }
        return false;
    }

    /**
     * Gets the description for this component.
     *
     * @param scheme     the component name
     */
    public static String getComponentDescription(String scheme) {
        // use the camel catalog
        CamelCatalog catalog = new DefaultCamelCatalog();
        String json = catalog.componentJSonSchema(scheme);
        if (json == null) {
            return null;
        }

        List<Map<String, String>> data = JSonSchemaHelper.parseJsonSchema("component", json, false);
        if (data != null) {
            for (Map<String, String> propertyMap : data) {
                String description = propertyMap.get("description");
                if (description != null) {
                    return description;
                }
            }
        }
        return null;
    }

    /**
     * Gets the description for this data format.
     *
     * @param dataFormat     the data format name
     */
    public static String getDataFormatDescription(String dataFormat) {
        // use the camel catalog
        CamelCatalog catalog = new DefaultCamelCatalog();
        String json = catalog.dataFormatJSonSchema(dataFormat);
        if (json == null) {
            return null;
        }

        List<Map<String, String>> data = JSonSchemaHelper.parseJsonSchema("dataformat", json, false);
        if (data != null) {
            for (Map<String, String> propertyMap : data) {
                String description = propertyMap.get("description");
                if (description != null) {
                    return description;
                }
            }
        }
        return null;
    }

    /**
     * Gets the description for this language.
     *
     * @param language     the language name
     */
    public static String getLanguageDescription(String language) {
        // use the camel catalog
        CamelCatalog catalog = new DefaultCamelCatalog();
        String json = catalog.languageJSonSchema(language);
        if (json == null) {
            return null;
        }

        List<Map<String, String>> data = JSonSchemaHelper.parseJsonSchema("language", json, false);
        if (data != null) {
            for (Map<String, String> propertyMap : data) {
                String description = propertyMap.get("description");
                if (description != null) {
                    return description;
                }
            }
        }
        return null;
    }
}
