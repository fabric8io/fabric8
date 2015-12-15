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
package io.fabric8.jolokia.support;

import org.jolokia.client.J4pClient;
import org.jolokia.client.exception.J4pException;
import org.jolokia.client.request.AbtractJ4pMBeanRequest;
import org.jolokia.client.request.J4pExecRequest;
import org.jolokia.client.request.J4pReadRequest;
import org.jolokia.client.request.J4pResponse;
import org.jolokia.client.request.J4pWriteRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.ObjectName;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.List;

public class JolokiaInvocationHandler implements InvocationHandler {
    private static final transient Logger LOG = LoggerFactory.getLogger(JolokiaInvocationHandler.class);

    private final J4pClient jolokia;
    private final ObjectName objectName;
    private final Class<?> interfaceClass;

    public static <T> T newProxyInstance(J4pClient jolokia, ObjectName objectName, Class<T> interfaceClass) {
        return (T) Proxy.newProxyInstance(interfaceClass.getClassLoader(), new Class[]{interfaceClass}, new JolokiaInvocationHandler(jolokia, objectName, interfaceClass));
    }

    public JolokiaInvocationHandler(J4pClient jolokia, ObjectName objectName, Class<?> interfaceClass) {
        this.jolokia = jolokia;
        this.objectName = objectName;
        this.interfaceClass = interfaceClass;
    }

    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        String name = method.getName();
        String attribute;
        AbtractJ4pMBeanRequest request;
        if ((attribute = getterAttributeName(method)) != null) {
            request = new J4pReadRequest(objectName, attribute);
        } else if ((attribute = setterAttributeName(method)) != null) {
            request = new J4pWriteRequest(objectName, attribute, args[0]);
        } else {
            name = executeMethodName(method);
            if (args == null | method.getParameterTypes().length == 0) {
                request = new J4pExecRequest(objectName, name);
            } else {
                request = new J4pExecRequest(objectName, name, args);
            }
        }
        try {
            request.setPreferredHttpMethod("POST");
            J4pResponse response = jolokia.execute(request);
            Object value = response.getValue();
            return JolokiaHelpers.convertJolokiaToJavaType(method.getReturnType(), value);
        } catch (J4pException e) {
            List<Object> argsList = args == null ? null : Arrays.asList(args);
            LOG.warn("Failed to invoke " + objectName + " method: " + name + " with arguments: " + argsList + ". " + e, e);
            throw e;
        }
    }

    protected String getterAttributeName(Method method) {
        String name = method.getName();
        int length = name.length();
        Class<?>[] parameterTypes = method.getParameterTypes();
        Class<?> returnType = method.getReturnType();
        if (parameterTypes.length == 0 && !Void.class.equals(returnType)) {
            boolean returnsBool = returnType.equals(Boolean.class) || returnType.equals(boolean.class);
            if (name.startsWith("get") && length > 3) {
                return name.substring(3);
            } else if (returnsBool && name.startsWith("is") && length > 2) {
                return name.substring(2);
            }
        }
        return null;
    }

    protected String setterAttributeName(Method method) {
        Class<?>[] parameterTypes = method.getParameterTypes();
        if (parameterTypes.length == 1) {
            String name = method.getName();
            int nameLength = name.length();
            if (name.startsWith("set") && nameLength > 3) {
                return name.substring(3);
            }
        }
        return null;
    }

    /**
     * Returns the method name with parameter types if there is more than one method with the same name on the interface class
     */
    protected String executeMethodName(Method method) {
        String name = method.getName();
        Class<?>[] parameterTypes = method.getParameterTypes();
        if (methodCount(interfaceClass, name) > 1) {
            StringBuilder buffer = new StringBuilder(name);
            buffer.append("(");
            boolean first = true;
            for (Class<?> parameterType : parameterTypes) {
                if (first) {
                    first = false;
                } else {
                    buffer.append(",");
                }
                buffer.append(parameterType.getCanonicalName());
            }
            buffer.append(")");
            return buffer.toString();
        }
        return name;
    }

    /**
     * Returns the number of declared methods on the given clazz
     */
    protected static int methodCount(Class<?> clazz, String name) {
        int answer = 0;
        Method[] declaredMethods = clazz.getDeclaredMethods();
        for (Method method : declaredMethods) {
            if (name.equals(method.getName())) {
                answer++;
            }
        }
        if (!clazz.equals(Object.class)) {
            Class<?> superclass = clazz.getSuperclass();
            if (superclass != null && !superclass.equals(Object.class)) {
                answer += methodCount(superclass, name);
            }
        }
        return answer;
    }

}
