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

package org.fusesource.eca.util;

import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.beans.PropertyEditor;
import java.beans.PropertyEditorManager;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URLDecoder;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Utilities for properties
 */
@SuppressWarnings("unchecked")
public class PropertyUtil {

    /**
     * Get properties from a URI
     *
     * @return <Code>Map</Code> of properties
     */
    public static Map<String, String> parseParameters(URI uri) throws Exception {
        return uri.getQuery() == null ? Collections.EMPTY_MAP : parseQuery(stripPrefix(uri.getQuery(), "?"));
    }

    /**
     * Parse properties from a named resource -eg. a URI or a simple name e.g. foo?name="fred"&size=2
     *
     * @return <Code>Map</Code> of properties
     */
    public static Map<String, String> parseParameters(String uri) throws Exception {
        return uri == null ? Collections.EMPTY_MAP : parseQuery(stripUpto(uri, '?'));
    }

    /**
     * Get properties from a uri
     *
     * @return <Code>Map</Code> of properties
     */
    public static Map<String, String> parseQuery(String uri) throws Exception {
        if (uri != null) {
            Map<String, String> rc = new HashMap<String, String>();
            String[] parameters = uri.split("&");
            for (int i = 0; i < parameters.length; i++) {
                int p = parameters[i].indexOf("=");
                if (p >= 0) {
                    String name = URLDecoder.decode(parameters[i].substring(0, p), "UTF-8");
                    String value = URLDecoder.decode(parameters[i].substring(p + 1), "UTF-8");
                    rc.put(name, value);
                } else {
                    rc.put(parameters[i], null);
                }
            }
            return rc;
        }
        return Collections.EMPTY_MAP;
    }

    /**
     * Add bean properties to a URI
     *
     * @return <Code>Map</Code> of properties
     */
    public static String addPropertiesToURIFromBean(String uri, Object bean) throws Exception {
        Map<String, String> props = PropertyUtil.getProperties(bean);
        return PropertyUtil.addPropertiesToURI(uri, props);
    }

    /**
     * Add properties to a URI
     *
     * @return uri with properties on
     */
    public static String addPropertiesToURI(String uri, Map<String, String> props) throws Exception {
        String result = uri;
        if (uri != null && props != null) {
            StringBuilder base = new StringBuilder(stripBefore(uri, '?'));
            Map<String, String> map = parseParameters(uri);
            if (!map.isEmpty()) {
                map.putAll(props);
            }
            if (!map.isEmpty()) {
                base.append('?');
                boolean first = true;
                for (Map.Entry<String, String> entry : map.entrySet()) {
                    if (!first) {
                        base.append('&');
                    }
                    first = false;
                    base.append(entry.getKey()).append("=").append(entry.getValue());
                }
                result = base.toString();
            }
        }
        return result;
    }

    /**
     * Set properties on an object
     */
    public static void setProperties(Object target, Map<String, String> props) {
        if (target == null) {
            throw new IllegalArgumentException("target was null.");
        }
        if (props == null) {
            throw new IllegalArgumentException("props was null.");
        }
        for (Map.Entry<String, String> entry : props.entrySet()) {
            if (setProperty(target, (String) entry.getKey(), entry.getValue())) {
            }
        }
    }

    /**
     * Get properties from an object
     *
     * @return <Code>Map</Code> of properties
     */
    public static Map<String, String> getProperties(Object object) throws Exception {
        Map<String, String> props = new HashMap<String, String>();
        BeanInfo beanInfo = Introspector.getBeanInfo(object.getClass());
        Object[] NULL_ARG = {};
        PropertyDescriptor[] propertyDescriptors = beanInfo.getPropertyDescriptors();
        if (propertyDescriptors != null) {
            for (int i = 0; i < propertyDescriptors.length; i++) {
                PropertyDescriptor pd = propertyDescriptors[i];
                if (pd.getReadMethod() != null && !pd.getName().equals("class") && !pd.getName().equals("properties") && !pd.getName().equals("reference")) {
                    props.put(pd.getName(), ("" + pd.getReadMethod().invoke(object, NULL_ARG)));
                }
            }
        }
        return props;
    }

    /**
     * Get properties from an object, that match a type
     *
     * @return <Code>Map</Code> of properties
     */
    public static <T> Map<String, T> getProperties(Class<T> type, Object object) throws Exception {
        Map<String, T> props = new LinkedHashMap<String, T>();
        BeanInfo beanInfo = Introspector.getBeanInfo(object.getClass());
        Object[] NULL_ARG = {};
        PropertyDescriptor[] propertyDescriptors = beanInfo.getPropertyDescriptors();
        if (propertyDescriptors != null) {
            for (int i = 0; i < propertyDescriptors.length; i++) {
                PropertyDescriptor pd = propertyDescriptors[i];
                if (pd.getReadMethod() != null && !pd.getName().equals("class") && !pd.getName().equals("properties") && !pd.getName().equals("reference")) {
                    if (type.isAssignableFrom(pd.getClass())) {
                        props.put(pd.getName(), (T) pd.getReadMethod().invoke(object, NULL_ARG));
                    }
                }
            }
        }
        return props;
    }

    /**
     * Get properties from an object, that match a type as a list
     *
     * @return <Code>Map</Code> of properties
     */
    public static <T> Map<String, T> getValues(Class<T> type, Object object) throws Exception {
        Map<String, T> result = new LinkedHashMap<String, T>();
        BeanInfo beanInfo = Introspector.getBeanInfo(object.getClass());
        Object[] NULL_ARG = {};
        PropertyDescriptor[] propertyDescriptors = beanInfo.getPropertyDescriptors();
        if (propertyDescriptors != null) {
            for (int i = 0; i < propertyDescriptors.length; i++) {
                PropertyDescriptor pd = propertyDescriptors[i];
                if (pd.getReadMethod() != null && !pd.getName().equals("class") && !pd.getName().equals("properties") && !pd.getName().equals("reference")) {
                    if (isPropertyAssignable(type, pd)) {
                        result.put(pd.getDisplayName(), (T) pd.getReadMethod().invoke(object, NULL_ARG));
                    }
                }
            }
        }
        return result;
    }

    private static boolean isPropertyAssignable(Class type, PropertyDescriptor pd) {
        boolean result = type.isAssignableFrom(pd.getPropertyType());
        if (!result) {
            if (Number.class.isAssignableFrom(type) && pd.getPropertyType().isPrimitive()) {
                Class pdType = pd.getPropertyType();
                if (pdType == int.class || pdType == long.class || pdType == short.class || pdType == float.class || pdType == double.class) {
                    result = true;
                }
            }
        }
        return result;
    }

    /**
     * Set a property
     *
     * @return true if set
     */
    public static boolean setProperty(Object target, String name, Object value) {
        try {
            Class<? extends Object> clazz = target.getClass();
            Method setter = findSetterMethod(clazz, name);
            if (setter == null) {
                return false;
            }
            // If the type is null or it matches the needed type, just use the
            // value directly
            if (value == null || value.getClass() == setter.getParameterTypes()[0]) {
                setter.invoke(target, new Object[]{value});
            } else {
                // We need to convert it
                setter.invoke(target, new Object[]{convert(value, setter.getParameterTypes()[0])});
            }
            return true;
        } catch (Throwable ignore) {
            return false;
        }
    }

    /**
     * Return a String past a prefix
     *
     * @return stripped
     */
    public static String stripPrefix(String value, String prefix) {
        if (value.startsWith(prefix)) {
            return value.substring(prefix.length());
        }
        return value;
    }

    /**
     * Return a String from to a character
     *
     * @return stripped
     */
    public static String stripUpto(String value, char c) {
        String result = null;
        int index = value.indexOf(c);
        if (index > 0) {
            result = value.substring(index + 1);
        }
        return result;
    }

    /**
     * Return a String up to and including character
     *
     * @return stripped
     */
    public static String stripBefore(String value, char c) {
        String result = value;
        int index = value.indexOf(c);
        if (index > 0) {
            result = value.substring(0, index);
        }
        return result;
    }

    private static Method findSetterMethod(Class<? extends Object> clazz, String name) {
        // Build the method name.
        name = "set" + name.substring(0, 1).toUpperCase() + name.substring(1);
        Method[] methods = clazz.getMethods();
        for (int i = 0; i < methods.length; i++) {
            Method method = methods[i];
            Class<? extends Object> params[] = method.getParameterTypes();
            if (method.getName().equals(name) && params.length == 1) {
                return method;
            }
        }
        return null;
    }

    private static Object convert(Object value, Class<?> type) throws Exception {
        PropertyEditor editor = PropertyEditorManager.findEditor(type);
        if (editor != null) {
            editor.setAsText(value.toString());
            return editor.getValue();
        }
        if (type == URI.class) {
            return new URI(value.toString());
        }
        return null;
    }
}
