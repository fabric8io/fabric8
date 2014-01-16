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
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Provides type coercion helper function for injecting fields with config admin values
 */
public class ConverterHelper {

    /**
     * The separator used to separate list or array values in a Config Admin String value
     */
    public static final String VALUE_SEPARATOR = ",";

    protected static String[] EMPTY_STRING_ARRAY = new String[0];

    public static Object convertValue(Object value, Class<?> clazz) {
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

            // lets default to JDK property editors
            String text = value.toString();
            if (clazz.isArray()) {
                String[] tokens = splitValues(text);
                Class<?> componentType = clazz.getComponentType();
                Object array = Array.newInstance(componentType, tokens.length);
                int index = 0;
                for (String token : tokens) {
                    Object item = convertValue(token, componentType);
                    if (item != null) {
                        Array.set(array, index++, item);
                    }
                }
                return array;
            } else if (Collection.class.isAssignableFrom(clazz)) {
                List list = new ArrayList();
                Class<?> componentType = Object.class;
                String[] tokens = splitValues(text);
                for (String token : tokens) {
                    Object item = convertValue(token, componentType);
                    list.add(item);
                }
                return list;
            }
            PropertyEditor editor = PropertyEditorManager.findEditor(clazz);
            if (editor != null) {
                editor.setAsText(text);
                return editor.getValue();
            }
        }
        return null;
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
