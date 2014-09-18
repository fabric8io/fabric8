/*
 * #%L
 * Fabric8 :: SPI
 * %%
 * Copyright (C) 2014 Red Hat
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package io.fabric8.jolokia.client;

import java.beans.ConstructorProperties;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.management.openmbean.ArrayType;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeDataSupport;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.OpenDataException;
import javax.management.openmbean.OpenType;
import javax.management.openmbean.SimpleType;
import javax.management.openmbean.TabularData;
import javax.management.openmbean.TabularDataSupport;
import javax.management.openmbean.TabularType;

import org.jboss.gravia.utils.IllegalStateAssertion;

/**
 * A utility that can marshal an MXBean compliant type into an open data type and vise versa.
 *
 * @author thomas.diesler@jboss.com
 * @since 23-Jun-2014
 */
public final class OpenTypeGenerator {

    // Hide ctor
    private OpenTypeGenerator() {
    }

    public static Object toOpenData(OpenType<?> type, Object value) throws OpenDataException {
        Object result;
        if (value != null && !type.isValue(value)) {
            if (type instanceof CompositeType) {
                result = toCompositeData((CompositeType) type, value); 
            } else if (type instanceof TabularType) {
                result = toTabularData((TabularType) type, (Map<?, ?>) value); 
            } else if (type instanceof ArrayType) {
                result = toArrayData((ArrayType<?>) type, value); 
            } else {
                throw new OpenDataException("Unsupported open type: " + type);
            }
        } else {
            result = value;
        }
        boolean isAssignable = result == null || type.isValue(result);
        IllegalStateAssertion.assertTrue(isAssignable, "Value " + result + " is not a value of: " + type);
        return result;
    }

    private static CompositeData toCompositeData(CompositeType ctype, Object value) throws OpenDataException {
        Map<String, Object> items = new HashMap<>();
        for(String key : ctype.keySet()) {
            OpenType<?> itemType = ctype.getType(key);
            Object rawValue;
            if (value instanceof Map) {
                rawValue = ((Map<?, ?>) value).get(key);
            } else {
                rawValue = getterValue(value, itemType, key);
            }
            Object openValue = toOpenData(itemType, rawValue);
            items.put(key, openValue);
        }
        return new CompositeDataSupport(ctype, items);
    }

    private static TabularData toTabularData(TabularType ttype, Map<?, ?> value) throws OpenDataException {
        TabularDataSupport tdata = new TabularDataSupport(ttype);
        CompositeType rowType = ttype.getRowType();
        OpenType<?> keyType = rowType.getType("key");
        OpenType<?> valueType = rowType.getType("value");
        for (Map.Entry<?, ?> entry : value.entrySet()) {
            Map<String, Object> rowData = new HashMap<>();
            Object keyData = toOpenData(keyType, entry.getKey());
            Object valData = toOpenData(valueType, entry.getValue());
            rowData.put("key", keyData);
            rowData.put("value", valData);
            tdata.put(toCompositeData(rowType, rowData));
        }
        return tdata;
    }

    private static Object toArrayData(ArrayType<?> atype, Object value) throws OpenDataException {
        if (atype.isPrimitiveArray() && value.getClass().isArray()) {
            return value;
        }
        List<?> items;
        if (value instanceof Collection) {
            items = new ArrayList<Object>((Collection<?>) value);
        } else {
            items = Arrays.asList(value);
        }
        OpenType<?> elementType = atype.getElementOpenType();
        Object[] array = getOpenTypeArray(elementType, items.size());
        for (int i = 0; i < items.size(); i++) {
            Object openValue = toOpenData(elementType, items.get(i));
            array[i] = openValue;
        }
        return array;
    }

    private static Object getterValue(Object bean, OpenType<?> itemType , String itemName) throws OpenDataException {
        try {
            Method method = null;
            Class<? extends Object> beanClass = bean.getClass();
            String prefix = itemType == SimpleType.BOOLEAN ? "is" : "get";
            String methodName = prefix + itemName.substring(0, 1).toUpperCase() + itemName.substring(1);
            for (Method aux : beanClass.getMethods()) {
                if (methodName.equals(aux.getName()) && aux.getParameterTypes().length == 0) {
                    method = aux;
                    break;
                }
            }
            IllegalStateAssertion.assertNotNull(method, "Cannot find getter: " + methodName);
            return method.invoke(bean, (Object[]) null);
        } catch (Exception ex) {
            OpenDataException odex = new OpenDataException("Cannot invoke getter for: " + itemName);
            odex.initCause(ex);
            throw odex;
        }
    }

    public static Object fromOpenData(OpenType<?> type, ClassLoader classLoader, Object value) throws OpenDataException {
        Object result;
        if (type instanceof CompositeType) {
            result = fromCompositeData(classLoader, (CompositeData) value); 
        } else if (type instanceof TabularType) {
            result = fromTabularData(classLoader, (TabularData) value); 
        } else if (type instanceof ArrayType) { 
            result = fromArrayData((ArrayType<?>) type, classLoader, value); 
        } else if (type instanceof SimpleType) {
            result = value;
        } else {
            throw new OpenDataException("Unsupported open type: " + type);
        }
        return result;
    }
    
    private static Object fromCompositeData(ClassLoader classLoader, CompositeData cdata) throws OpenDataException {
        CompositeType ctype = cdata.getCompositeType();
        String typeName = ctype.getTypeName();
        if (typeName.startsWith("java.util.Map")) {
            Object openKey = cdata.get("key");
            Object openVal = cdata.get("value");
            OpenType<?> keyType = ctype.getType("key");
            OpenType<?> valType = ctype.getType("value");
            Object key = fromOpenData(keyType, classLoader, openKey);
            Object value = fromOpenData(valType, classLoader, openVal);
            return Collections.singletonMap(key, value);
        }
        Class<?> targetType;
        try {
            targetType = classLoader.loadClass(typeName);
        } catch (ClassNotFoundException ex) {
            OpenDataException odex = new OpenDataException("Cannot load target type: " + typeName);
            odex.initCause(ex);
            throw odex;
        }
        Constructor<?> ctor = null;
        boolean isDefaultCtor = false;
        for (Constructor<?> aux : targetType.getConstructors()) {
            isDefaultCtor = aux.getParameterTypes().length == 0;
            if (isDefaultCtor) {
                ctor = aux;
                break;
            } else if (aux.getAnnotation(ConstructorProperties.class) != null) {
                ctor = aux;
            }
        }
        IllegalStateAssertion.assertNotNull(ctor, "Cannot mxbean compliant constructor for: " + targetType.getName());
        Object result;
        try {
            if (isDefaultCtor) {
                result = ctor.newInstance((Object[]) null);
                for (String key : ctype.keySet()) {
                    OpenType<?> itemType = ctype.getType(key);
                    Object itemValue = cdata.get(key);
                    Object javaValue = fromOpenData(itemType, classLoader, itemValue);
                    invokeSetter(result, key, javaValue);
                }
            } else {
                List<Object> params = new ArrayList<>();
                ConstructorProperties props = ctor.getAnnotation(ConstructorProperties.class);
                for (String key : props.value()) {
                    OpenType<?> itemType = ctype.getType(key);
                    Object itemValue = cdata.get(key);
                    Object javaValue = fromOpenData(itemType, classLoader, itemValue);
                    params.add(javaValue);
                }
                Class<?>[] paramTypes = ctor.getParameterTypes();
                for (Object param : params) {
                    int index = params.indexOf(param);
                    Class<?> paramType = paramTypes[index];
                    param = toTargetType(paramType, param);
                    if (param != params.get(index)) {
                        params.set(index, param);
                    }
                }
                result = ctor.newInstance(params.toArray());
            }
        } catch (Exception ex) {
            OpenDataException odex = new OpenDataException("Cannot construct object from: " + cdata);
            odex.initCause(ex);
            throw odex;
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private static Map<?, ?> fromTabularData(ClassLoader classLoader, TabularData tdata) throws OpenDataException {
        Map<Object, Object> result = new LinkedHashMap<>();
        for (CompositeData cdata : (Collection<CompositeData>)tdata.values()) {
            Map<Object, Object> rowValue = (Map<Object, Object>) fromCompositeData(classLoader, cdata);
            result.putAll(rowValue);
        }
        return result;
    }

    private static Object fromArrayData(ArrayType<?> atype, ClassLoader classLoader, Object value) throws OpenDataException {
        if (atype.isPrimitiveArray() && value.getClass().isArray()) {
            return value;
        }
        Object[] elements = (Object[]) value;
        OpenType<?> elementType = atype.getElementOpenType();
        Object[] array = getJavaTypeArray(elementType, classLoader, elements.length);
        for (int i = 0; i < elements.length; i++) {
            Object javaValue = fromOpenData(elementType, classLoader, elements[i]);
            array[i] = javaValue;
        }
        return array;
    }

    private static void invokeSetter(Object target, String itemName, Object value) throws OpenDataException {
        try {
            Method method = null;
            Class<? extends Object> beanClass = target.getClass();
            String methodName = "set" + itemName.substring(0, 1).toUpperCase() + itemName.substring(1);
            for (Method aux : beanClass.getMethods()) {
                if (methodName.equals(aux.getName()) && aux.getParameterTypes().length == 1) {
                    method = aux;
                    break;
                }
            }
            IllegalStateAssertion.assertNotNull(method, "Cannot find setter: " + methodName);
            Class<?> paramType = method.getParameterTypes()[0];
            value = toTargetType(paramType, value);
            method.invoke(target, value);
        } catch (Exception ex) {
            OpenDataException odex = new OpenDataException("Cannot invoke setter for: " + itemName);
            odex.initCause(ex);
            throw odex;
        }
    }
    
    static Object[] getOpenTypeArray(OpenType<?> elementType, int dimension) throws OpenDataException {
        Class<?> compType;
        try {
            compType = Class.forName(elementType.getClassName());
        } catch (ClassNotFoundException ex) {
            OpenDataException odex = new OpenDataException("Cannot load array type: " + elementType.getClassName());
            odex.initCause(ex);
            throw odex;
        }
        return (Object[]) Array.newInstance(compType, dimension);
    }

    static Object[] getJavaTypeArray(OpenType<?> elementType, ClassLoader classLoader, int dimension) throws OpenDataException {
        Class<?> compType;
        try {
            if (elementType instanceof CompositeType) {
                CompositeType ctype = (CompositeType) elementType;
                compType = classLoader.loadClass(ctype.getTypeName());
            } else if (elementType instanceof SimpleType) {
                compType = Class.forName(elementType.getClassName());
            } else {
                throw new IllegalArgumentException("Element type not supported: " + elementType);
            }
        } catch (ClassNotFoundException ex) {
            OpenDataException odex = new OpenDataException("Cannot load array type: " + elementType.getClassName());
            odex.initCause(ex);
            throw odex;
        }
        return (Object[]) Array.newInstance(compType, dimension);
    }

    static Object toTargetType(Class<?> targetType, Object value) {
        if (value == null || targetType.isPrimitive() || targetType.isAssignableFrom(value.getClass())) {
            return value;
        }
        Object result = value;
        if (value.getClass().isArray()) {
            if (targetType == List.class) {
                result = new ArrayList<Object>(Arrays.asList((Object[]) value));
            } else if (targetType == Set.class) {
                result = new HashSet<Object>(Arrays.asList((Object[]) value));
            }
        }
        boolean isAssignable = targetType.isAssignableFrom(result.getClass());
        IllegalStateAssertion.assertTrue(isAssignable, "Value '" + value + "' not assignable to: " + targetType.getName());
        return result;
    }
}
