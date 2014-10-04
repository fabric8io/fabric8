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
package io.fabric8.api.scr.support;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

public class ConfigInjection {

    private static final Logger LOG = LoggerFactory.getLogger(ConfigInjection.class);

    /**
     * Applies configuration specified in {@link java.util.Map} to the specified target.
     */
    public static <T> void applyConfiguration(Map<String, ?> configuration, T target, String... ignorePrefixes) throws Exception {
        Class<?> clazz = target.getClass();
        while (clazz != null && clazz != Object.class) {
            applyConfiguration(configuration, target, clazz, ignorePrefixes);
            clazz = clazz.getSuperclass();
        }
    }

    /**
     * Applies configuration specified in {@link java.util.Map} to the specified target.
     */
    private static <T> void applyConfiguration(Map<String, ?> configuration, T target, Class<?> clazz, String... ignorePrefixes) throws Exception {
        injectValues(clazz, target, configuration, ignorePrefixes);

    }

    private static void injectValues(Class<?> clazz, Object instance, Map<String, ?> configuration, String... ignorePrefixes) throws Exception {

        for (Map.Entry<String, ?> entry : configuration.entrySet()) {
            String name = entry.getKey();
            Object value = entry.getValue();
            try {
                Field field = clazz.getDeclaredField(normalizePropertyName(name, ignorePrefixes));
                if (field != null) {
                    Object convertedValue = ConverterHelper.convertValue(value, field.getGenericType());
                    if (convertedValue != null) {
                        ReflectionHelper.setField(field, instance, convertedValue);
                    }
                }
            } catch (NoSuchFieldException e) {
                LOG.debug("No matching field for property with name {}.", name);
            }
        }
    }

    /**
     * Strips all prefixes from the name.
     * @param name      The name of the property to strip.
     * @param prefixes  The list of prefixes to remove.
     * @return          The stripepd property name.
     */
    static String stripPrefixes(String name, String... prefixes) {
        if (name == null || name.isEmpty()) {
            return name;
        } else {
            String result = name;
            for (String prefix : prefixes) {
                if (result.startsWith(prefix)) {
                    result = result.substring(prefix.length());
                }
            }
            return result;
        }
    }


    /**
     * Utility to transform name containing dots to valid java identifiers.
     */
    static String normalizePropertyName(String name, String... ignorePrefixes) {
        if (ignorePrefixes.length > 0) {
            return normalizePropertyName(stripPrefixes(name, ignorePrefixes));
        }
        if (name == null || name.isEmpty()) {
            return name;
        } else if (!name.contains(".") && !name.contains("-")) {
            return name;
        } else {
            String[] parts = name.replaceAll(" ", "").split("-|\\.");
            StringBuilder sb = new StringBuilder();
            if (parts.length > 0) {
                sb.append(parts[0]);
                for (int i = 1; i < parts.length; i++) {
                    String s = parts[i-1].length() > 0 ?
                            parts[i].substring(0, 1).toUpperCase() + parts[i].substring(1) :
                            parts[i];
                    sb.append(s);
                }
            }
            return sb.toString();
        }
    }
}
