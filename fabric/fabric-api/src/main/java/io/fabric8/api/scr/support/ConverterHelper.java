/**
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.fabric8.api.scr.support;

import java.beans.PropertyEditor;
import java.beans.PropertyEditorManager;
import java.io.File;
import java.lang.reflect.Array;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Provides type coercion helper function for injecting fields with config admin values
 */
public class ConverterHelper {

    /**
     * The separator used to separate list or array values in a Config Admin String value
     */
    public static final String VALUE_SEPARATOR = ",";

    protected static String[] EMPTY_STRING_ARRAY = new String[0];

    public static Object convertValue(Object value, Type type) throws Exception {
        Class<?> clazz = null;
        Class<?> componentType = Object.class;

        if (type instanceof ParameterizedType) {
            clazz = (Class<?>) ((ParameterizedType) type).getRawType();
            componentType = (Class<?>) ((ParameterizedType) type).getActualTypeArguments()[0];
        } else if (type instanceof Class) {
            clazz = (Class) type;
        } else {
            throw new IllegalArgumentException();
        }

        if (value != null) {
            if (clazz.isInstance(value)) {
                return value;
            }
            if (clazz == String.class) {
                return value.toString();
            }
            if (clazz == Character.class || clazz == char.class) {
                String text = value.toString();
                if (text.length() > 0) {
                    return text.charAt(0);
                }
            }
            if (clazz == Boolean.class || clazz == boolean.class) {
                return convertBoolean(value.toString());
            }

            if (clazz == Integer.class || clazz == int.class) {
                return convertInt(value.toString());
            }

            if (clazz == File.class) {
                return new File(value.toString());
            }

            if (clazz == URI.class) {
                return new URI(value.toString());
            }

            if (clazz == URL.class) {
                return new URL(value.toString());
            }

            // lets default to JDK property editors
            String text = value.toString();
            if (clazz.isArray()) {
                String[] tokens = splitValues(text);
                componentType = clazz.getComponentType();
                Object array = Array.newInstance(componentType, tokens.length);
                int index = 0;
                for (String token : tokens) {
                    Object item = convertValue(token, componentType);
                    if (item != null) {
                        Array.set(array, index++, item);
                    }
                }
                return array;
            } else if (List.class.isAssignableFrom(clazz)) {
                List list = new ArrayList();
                String[] tokens = splitValues(text);
                for (String token : tokens) {
                    Object item = convertValue(token, componentType);
                    list.add(item);
                }
                return list;
            } else if (Set.class.isAssignableFrom(clazz)) {
                Set set = new HashSet();
                String[] tokens = splitValues(text);
                for (String token : tokens) {
                    Object item = convertValue(token, componentType);
                    set.add(item);
                }
                return set;
            }
            PropertyEditor editor = PropertyEditorManager.findEditor(clazz);
            if (editor != null) {
                editor.setAsText(text);
                return editor.getValue();
            }
        }
        return null;
    }

    private static int convertInt(String value) {
        try {
            return Integer.parseInt(value);
        } catch (Exception e) {
            return 0;
        }
    }

    private static boolean convertBoolean(String value) {
        try {
            return Boolean.parseBoolean(value);
        } catch (Exception e) {
            return false;
        }
    }

    private static String[] splitValues(String text) {
        if (text != null) {
            String[] split = text.split(VALUE_SEPARATOR);
            if (split != null) {
                return split;
            }
        }
        return EMPTY_STRING_ARRAY;
    }
}
