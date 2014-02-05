/**
 * Copyright (C) FuseSource, Inc.
 * http://fusesource.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
     * Applies configuration specified in {@link Map} to the specified target.
     *
     * @param configuration The configuration.
     * @param target        The target.
     * @param <T>
     * @throws Exception
     */
    public static <T> void applyConfiguration(Map<String, ?> configuration, T target) throws Exception {
        Class<?> clazz = target.getClass();
        while (clazz != null && clazz != Object.class) {
            applyConfiguration(configuration, target, clazz);
            clazz = clazz.getSuperclass();
        }
    }

    /**
     * Applies configuration specified in {@link Map} to the specified target.
     *
     * @param configuration The configuration.
     * @param target        The target.
     * @param clazz         The target Class.
     * @param <T>
     * @throws Exception
     */
    private static <T> void applyConfiguration(Map<String, ?> configuration, T target, Class<?> clazz) throws Exception {
        injectValues(clazz, target, configuration);

    }

    static void injectValues(Class<?> clazz, Object instance, Map<String, ?> configuration) throws Exception {
        for (Map.Entry<String, ?> entry : configuration.entrySet()) {
            String name = entry.getKey();
            Object value = entry.getValue();
            try {
                Field field = clazz.getDeclaredField(normalizePropertyName(name));
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
     * Utility to transform name containing dots to valid java identifiers.
     * @param name
     * @return
     */
     static String normalizePropertyName(String name) {
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
                     String s = parts[i].length() > 0 ? parts[i].substring(0, 1).toUpperCase() + parts[i].substring(1) : "";
                     sb.append(s);
                 }
             }
             return sb.toString();
         }
    }
}
