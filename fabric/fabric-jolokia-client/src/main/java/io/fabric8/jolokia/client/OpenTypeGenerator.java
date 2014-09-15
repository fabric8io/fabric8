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
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import javax.management.openmbean.ArrayType;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeDataSupport;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.OpenDataException;
import javax.management.openmbean.OpenType;
import javax.management.openmbean.SimpleType;

/**
 * A utility that can marshal an MXBean compliant type into
 * CompositeData and vise versa.
 *
 * The MXBeanProxy in the JDK can do this internally, it is however tied to the
 * MBeanServerConnection and cannot be used with other JMX transports (i.e. Jolokia).
 *
 * @author thomas.diesler@jboss.com
 * @since 23-Jun-2014
 */
public final class OpenTypeGenerator {

    // Hide ctor
    private OpenTypeGenerator() {
    }

    public static CompositeType getCompositeType(Class<?> beanClass) throws OpenDataException {
        String typeName = beanClass.getSimpleName() + "Type";
        String[] itemNames = getItemNames(beanClass);
        OpenType<?>[] itemTypes = getItemTypes(beanClass);
        return new CompositeType(typeName, typeName, itemNames, itemNames, itemTypes);
    }

    public static CompositeData toCompositeData(Object bean) throws OpenDataException {
        Class<?> beanClass = bean.getClass();
        CompositeType dataType = getCompositeType(beanClass);
        String[] itemNames = getItemNames(beanClass);
        Object[] itemValues = getItemValues(bean);
        return new CompositeDataSupport(dataType, itemNames, itemValues);
    }

    @SuppressWarnings("unchecked")
    public static <T extends Object> T fromCompositeData(Class<T> beanClass, CompositeData cdata) throws OpenDataException {

        // Try from(CompositeData)
        Method method = null;
        try {
            method = beanClass.getDeclaredMethod("from", new Class[] { CompositeData.class });
        } catch (NoSuchMethodException | SecurityException ex) {
            // ignore
        }
        if (method != null && Modifier.isStatic(method.getModifiers()) && method.getReturnType() == beanClass) {
            try {
                return (T) method.invoke(beanClass, new Object[] { cdata });
            } catch (Exception ex) {
                OpenDataException ode = new OpenDataException("Cannot invoke from method: " + method);
                ode.initCause(ex);
                throw ode;
            }
        }

        // Try constructor with @ConstructorProperties
        for (Constructor<?> ctor : beanClass.getDeclaredConstructors()) {
            ConstructorProperties props = ctor.getAnnotation(ConstructorProperties.class);
            if (props != null) {
                Class<?>[] paramTypes = ctor.getParameterTypes();
                String[] paramNames = props.value();
                List<Object> params = new ArrayList<>();
                for (int i = 0; i < paramNames.length; i++) {
                    Object value = cdata.get(paramNames[i]);
                    if (value instanceof CompositeData) {
                        Class<?> paramType = paramTypes[i];
                        params.add(fromCompositeData(paramType, (CompositeData) value));
                    } else {
                        params.add(value);
                    }
                }
                try {
                    return (T) ctor.newInstance(params.toArray(new Object[params.size()]));
                } catch (Exception ex) {
                    OpenDataException ode = new OpenDataException("Cannot invoke constructor: " + ctor);
                    ode.initCause(ex);
                    throw ode;
                }
            }
        }

        throw new OpenDataException("Cannot construct target type from: " + cdata);
    }

    public static String[] getItemNames(Class<?> beanClass) {
        List<String> names = new ArrayList<>();
        for (Method method : getGetters(beanClass)) {
            String methodName = method.getName();
            String name = methodName.substring(3);
            names.add(name.substring(0, 1).toLowerCase() + name.substring(1));
        }
        return names.toArray(new String[names.size()]);
    }

    public static OpenType<?>[] getItemTypes(Class<?> beanClass) throws OpenDataException {
        List<OpenType<?>> types = new ArrayList<>();
        for (Method method : getGetters(beanClass)) {
            Class<?> returnType = method.getReturnType();
            types.add(getOpenType(returnType));
        }
        return types.toArray(new OpenType<?>[types.size()]);
    }

    public static OpenType<?> getOpenType(Class<?> javaType) throws OpenDataException {
        if (javaType == String.class) {
            return SimpleType.STRING;
        } else if (javaType.isArray()) {
            Class<?> compType = javaType.getComponentType();
            return new ArrayType<>(1, getOpenType(compType));
        } else if (!javaType.getName().startsWith("java.")) {
            return getCompositeType(javaType);
        } else {
            throw new OpenDataException("Unsupported java type for: " + javaType);
        }
    }

    public static Object[] getItemValues(Object bean) throws OpenDataException {
        Class<?> beanClass = bean.getClass();
        List<Object> items = new ArrayList<>();
        for (Method method : getGetters(beanClass)) {
            Object value;
            try {
                value = method.invoke(bean, (Object[]) null);
            } catch (Exception ex) {
                OpenDataException ode = new OpenDataException("Cannot obtain vaue from: " + method);
                ode.initCause(ex);
                throw ode;
            }
            Class<?> valueType = value.getClass();
            OpenType<?> openType = getOpenType(valueType);
            if (openType == SimpleType.STRING) {
                items.add(value);
            } else if (openType instanceof ArrayType) {
                items.add(value);
            } else if (openType instanceof CompositeType) {
                items.add(toCompositeData(value));
            } else {
                throw new OpenDataException("Unsupported open type for: " + openType);
            }
        }
        return items.toArray(new Object[items.size()]);
    }

    private static Method[] getGetters(Class<?> beanClass) {
        List<Method> methods = new ArrayList<>();
        for (Method method : beanClass.getDeclaredMethods()) {
            String methodName = method.getName();
            Class<?>[] paramTypes = method.getParameterTypes();
            if (Modifier.isPublic(method.getModifiers()) && methodName.startsWith("get") && paramTypes.length == 0) {
                methods.add(method);
            }
        }
        return methods.toArray(new Method[methods.size()]);
    }
}
