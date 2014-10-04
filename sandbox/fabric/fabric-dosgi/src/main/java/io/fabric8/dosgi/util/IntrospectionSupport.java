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

package io.fabric8.dosgi.util;

import java.beans.PropertyEditor;
import java.beans.PropertyEditorManager;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;



public final class IntrospectionSupport {
	
    private IntrospectionSupport() {
    }

    public static boolean getProperties(Object target, Map props, String optionPrefix) {

        boolean rc = false;
        if (target == null) {
            throw new IllegalArgumentException("target was null.");
        }
        if (props == null) {
            throw new IllegalArgumentException("props was null.");
        }

        if (optionPrefix == null) {
            optionPrefix = "";
        }

        Class<?> clazz = target.getClass();
        Method[] methods = clazz.getMethods();
        for (int i = 0; i < methods.length; i++) {
            Method method = methods[i];
            String name = method.getName();
            Class<?> type = method.getReturnType();
            Class<?> params[] = method.getParameterTypes();
            if ((name.startsWith("is") || name.startsWith("get")) && params.length == 0 && type != null && isSettableType(type)) {

                try {

                    Object value = method.invoke(target, new Object[] {});
                    if (value == null) {
                        continue;
                    }

                    String strValue = convertToString(value, type);
                    if (strValue == null) {
                        continue;
                    }
                    if (name.startsWith("get")) {
                        name = name.substring(3, 4).toLowerCase()
                                + name.substring(4);
                    } else {
                        name = name.substring(2, 3).toLowerCase()
                                + name.substring(3);
                    }
                    props.put(optionPrefix + name, strValue);
                    rc = true;

                } catch (Throwable ignore) {
                }

            }
        }

        return rc;
    }

    public static boolean setProperties(Object target, Map<String, ?> props, String optionPrefix) {
        boolean rc = false;
        if (target == null) {
            throw new IllegalArgumentException("target was null.");
        }
        if (props == null) {
            throw new IllegalArgumentException("props was null.");
        }

        for (Iterator<String> iter = props.keySet().iterator(); iter.hasNext();) {
            String name = iter.next();
            if (name.startsWith(optionPrefix)) {
                Object value = props.get(name);
                name = name.substring(optionPrefix.length());
                if (setProperty(target, name, value)) {
                    iter.remove();
                    rc = true;
                }
            }
        }
        return rc;
    }

    public static Map<String, Object> extractProperties(Map props, String optionPrefix) {
        if (props == null) {
            throw new IllegalArgumentException("props was null.");
        }

        HashMap<String, Object> rc = new HashMap<String, Object>(props.size());

        for (Iterator<Entry> iter = props.entrySet().iterator(); iter.hasNext();) {
            Entry entry = iter.next();
            String name = (String)entry.getKey();
            if (name.startsWith(optionPrefix)) {
                name = name.substring(optionPrefix.length());
                rc.put(name, entry.getValue());
                iter.remove();
            }
        }

        return rc;
    }

    public static boolean setProperties(Object target, Map props) {
        boolean rc = false;

        if (target == null) {
            throw new IllegalArgumentException("target was null.");
        }
        if (props == null) {
            throw new IllegalArgumentException("props was null.");
        }

        for (Iterator<Entry> iter = props.entrySet().iterator(); iter.hasNext();) {
            Map.Entry entry = iter.next();
            if (setProperty(target, (String)entry.getKey(), entry.getValue())) {
                iter.remove();
                rc = true;
            }
        }

        return rc;
    }

    public static Class<?> getPropertyType(Object target, String name) {
        Class<?> clazz = target.getClass();
        Method setter = findSetterMethod(clazz, name);
        if (setter == null) {
            return null;
        }
        return setter.getParameterTypes()[0];
    }
    
    public static boolean setProperty(Object target, String name, Object value) {
        try {
            Class<?> clazz = target.getClass();
            Method setter = findSetterMethod(clazz, name);
            if (setter == null) {
                return false;
            }

            // If the type is null or it matches the needed type, just use the
            // value directly
            if (value == null || value.getClass() == setter.getParameterTypes()[0]) {
                setter.invoke(target, new Object[] {value});
            } else {
                // We need to convert it
                setter.invoke(target, new Object[] {convert(value, setter.getParameterTypes()[0])});
            }
            return true;
        } catch (Throwable ignore) {
            return false;
        }
    }

    private static Object convert(Object value, Class<?> type) {
        if( type.isArray() ) {
            if( value.getClass().isArray() ) {
                int length = Array.getLength(value);
                Class<?> componentType = type.getComponentType();
                Object rc = Array.newInstance(componentType, length);
                for (int i = 0; i < length; i++) {
                    Object o = Array.get(value, i);
                    Array.set(rc, i, convert(o, componentType));
                }
                return rc;
            }
        }
        PropertyEditor editor = PropertyEditorManager.findEditor(type);
        if (editor != null) {
            editor.setAsText(value.toString());
            return editor.getValue();
        }
        return null;
    }

    public static String convertToString(Object value, Class<?> type) {
        PropertyEditor editor = PropertyEditorManager.findEditor(type);
        if (editor != null) {
            editor.setValue(value);
            return editor.getAsText();
        }
        return null;
    }

    private static Method findSetterMethod(Class<?> clazz, String name) {
        // Build the method name.
        name = "set" + name.substring(0, 1).toUpperCase() + name.substring(1);
        Method[] methods = clazz.getMethods();
        for (int i = 0; i < methods.length; i++) {
            Method method = methods[i];
            Class<?> params[] = method.getParameterTypes();
            if (method.getName().equals(name) && params.length == 1 ) {
                return method;
            }
        }
        return null;
    }

    private static boolean isSettableType(Class<?> clazz) {
        if (PropertyEditorManager.findEditor(clazz) != null) {
            return true;
        }
        	
        return false;
    }

    public static String toString(Object target) {
        return toString(target, Object.class, null, (String[])null);
    }
    
    public static String toString(Object target, String...fields) {
        return toString(target, Object.class, null, fields);
    }
    
    public static String toString(Object target, Class<?> stopClass) {
    	return toString(target, stopClass, null, (String[])null);
    }

    public static String toString(Object target, Map<String, Object> overrideFields, String...fields) {
        return toString(target, Object.class, overrideFields, fields);
    }

    public static String toString(Object target, Class<?> stopClass, Map<String, Object> overrideFields, String ... fields) {
        try {
            LinkedHashMap<String, Object> map = new LinkedHashMap<String, Object>();
            addFields(target, target.getClass(), stopClass, map);
            if (overrideFields != null) {
            	for(String key : overrideFields.keySet()) {
            	    Object value = overrideFields.get(key);
            	    map.put(key, value);
            	}
            }
            
            if( fields!=null ) {
                map.keySet().retainAll(Arrays.asList(fields));
            }
           
            boolean useMultiLine=false;
            LinkedHashMap<String, String> props = new LinkedHashMap<String, String>();
            for (Entry<String, Object> entry : map.entrySet()) {
                String key = entry.getKey();
                String value = null;
                if( entry.getValue() !=null ) {
                    value = entry.getValue().toString();
                    if( value!=null && ( value.indexOf('\n')>=0 || (key.length()+value.length())>70 ) ) {
                        useMultiLine=true;
                    }
                }
                props.put(key, value);
            }
            
            StringBuffer buffer = new StringBuffer();
            if( useMultiLine) {
                buffer.append("{\n");
                boolean first = true;
                for (Entry<String, String> entry : props.entrySet()) {
                    if (first) {
                        first = false;
                    } else {
                        buffer.append(",\n");
                    }
                    buffer.append("  ");
                    buffer.append(entry.getKey());
                    buffer.append(": ");
                    buffer.append(StringSupport.indent(entry.getValue(), 2));
                }
                buffer.append("\n}");
            } else {
                buffer.append("{");
                boolean first = true;
                for (Entry<String, String> entry : props.entrySet()) {
                    if (first) {
                        first = false;
                    } else {
                        buffer.append(", ");
                    }
                    buffer.append(entry.getKey());
                    buffer.append(": ");
                    String value = entry.getValue();
                    buffer.append(value);
                }
                buffer.append("}");
            }
            return buffer.toString();
        } catch (Throwable e) {
            e.printStackTrace();
            return "Could not toString: "+e.toString();
        }
    }


    public static String simpleName(Class<?> clazz) {
        String name = clazz.getName();
        int p = name.lastIndexOf(".");
        if (p >= 0) {
            name = name.substring(p + 1);
        }
        return name;
    }

    private static void addFields(Object target, Class<?> startClass, Class<?> stopClass, LinkedHashMap<String, Object> map) {

        if (startClass != stopClass) {
            addFields(target, startClass.getSuperclass(), stopClass, map);
        }

        Field[] fields = startClass.getDeclaredFields();
        for (int i = 0; i < fields.length; i++) {
            Field field = fields[i];
            if (Modifier.isStatic(field.getModifiers())) {
                continue;
            }

            try {
                field.setAccessible(true);
                Object o = field.get(target);
                if (o != null && o.getClass().isArray()) {
                    try {
                        o = Arrays.asList((Object[])o);
                    } catch (Throwable e) {
                    }
                }
                map.put(field.getName(), o);
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }

    }

}
