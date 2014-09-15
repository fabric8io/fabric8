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

import java.lang.reflect.Array;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

import javax.management.ObjectName;
import javax.management.openmbean.OpenDataException;
import javax.management.openmbean.SimpleType;

import org.jolokia.client.J4pClient;
import org.jolokia.client.request.J4pExecRequest;
import org.jolokia.client.request.J4pReadRequest;
import org.jolokia.client.request.J4pWriteRequest;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

/**
 * A Jolokia proxy for MXBean compliant MBeans.
 *
 * It utilises the {@link JSONTypeGenerator}.
 *
 * @author thomas.diesler@jboss.com
 * @since 23-Jun-2014
 */
public final class JolokiaMXBeanProxy {

    static final SimpleType<?>[] simpleTypes = new SimpleType[] { 
            SimpleType.BOOLEAN, SimpleType.CHARACTER, SimpleType.BYTE, SimpleType.SHORT, 
            SimpleType.INTEGER, SimpleType.LONG, SimpleType.FLOAT, SimpleType.DOUBLE, 
            SimpleType.STRING, SimpleType.BIGDECIMAL, SimpleType.BIGINTEGER, SimpleType.DATE, 
            SimpleType.OBJECTNAME };
    
    // Hide ctor
    private JolokiaMXBeanProxy() {
    }

    @SuppressWarnings("unchecked")
    public static <T extends Object> T getMXBeanProxy(String serviceURL, ObjectName objectName, Class<T> mxbean) {
        ClassLoader classLoader = mxbean.getClassLoader();
        InvocationHandler handler = new MXBeanInvocationHandler(serviceURL, objectName, null, null);
        return (T) Proxy.newProxyInstance(classLoader, new Class<?>[] { mxbean}, handler);
    }

    @SuppressWarnings("unchecked")
    public static <T extends Object> T getMXBeanProxy(String serviceURL, String username, String password, ObjectName objectName, Class<T> mxbean) {
        ClassLoader classLoader = mxbean.getClassLoader();
        InvocationHandler handler = new MXBeanInvocationHandler(serviceURL, objectName, username, password);
        return (T) Proxy.newProxyInstance(classLoader, new Class<?>[] { mxbean}, handler);
    }

    private static class MXBeanInvocationHandler implements InvocationHandler {

        private final ObjectName objectName;
        private final J4pClient client;

        private MXBeanInvocationHandler(String serviceURL, ObjectName objectName, String username, String password) {
            this.client = J4pClient.url(serviceURL).user(username).password(password).connectionTimeout(3000).build();
            this.objectName = objectName;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if (isGetter(method)) {
                String attname = method.getName().substring(3);
                J4pReadRequest readReq = new J4pReadRequest(objectName, attname);
                Object result = client.execute(readReq).getValue();
                Class<?> returnType = method.getReturnType();
                return unmarshalResult(returnType, result);
            } else if (isSetter(method)) {
                Object param = marshalParameter(args[0]);
                String attname = method.getName().substring(3);
                J4pWriteRequest writeReq = new J4pWriteRequest(objectName, attname, param);
                writeReq.setPreferredHttpMethod("POST");
                client.execute(writeReq);
                return null;
            } else {
                Object[] params = null;
                if (args != null) {
                    List<Object> list = new ArrayList<>();
                    for (Object arg : args) {
                        Object value = marshalParameter(arg);
                        list.add(value);
                    }
                    params = list.toArray(new Object[list.size()]);
                }
                J4pExecRequest execReq = new J4pExecRequest(objectName, method.getName(), params);
                execReq.setPreferredHttpMethod("POST");
                Object result = client.execute(execReq).getValue();
                Class<?> returnType = method.getReturnType();
                return unmarshalResult(returnType, result);
            }
        }

        private Object marshalParameter(Object arg) throws OpenDataException {
            Object param = arg;
            Class<?> argClass = arg.getClass();
            if (!argClass.getName().startsWith("java.")) {
                param = JSONTypeGenerator.toJSONObject(arg);
            }
            return param;
        }

        private Object unmarshalResult(Class<?> returnType, Object value) throws OpenDataException {
            Object result;
            if (value instanceof JSONObject) {
                result = JSONTypeGenerator.fromJSONObject(returnType, (JSONObject) value);
            } else if (value instanceof JSONArray) {
                List<Object> resultList = new ArrayList<>();
                Class<?> componentType = returnType.getComponentType();
                for (Object obj : ((JSONArray)value).toArray()) {
                    resultList.add(unmarshalResult(componentType, obj));
                }
                if (componentType != null) {
                    Object[] array = (Object[]) Array.newInstance(componentType, resultList.size());
                    result = resultList.toArray(array);
                } else {
                    result = resultList;
                }
            } else if (isSimpleType(value)) {
                result = value;
            } else{
                throw new IllegalArgumentException("Unsupported value type: " + value);
            }
            return result;
        }

        private boolean isGetter(Method method) {
            String methodName = method.getName();
            Class<?>[] paramTypes = method.getParameterTypes();
            Class<?> returnType = method.getReturnType();
            return Modifier.isPublic(method.getModifiers()) && returnType != void.class && methodName.startsWith("get") && paramTypes.length == 0;
        }

        private boolean isSetter(Method method) {
            String methodName = method.getName();
            Class<?>[] paramTypes = method.getParameterTypes();
            Class<?> returnType = method.getReturnType();
            return Modifier.isPublic(method.getModifiers()) && returnType == void.class && methodName.startsWith("set") && paramTypes.length == 1;
        }
        
        private boolean isSimpleType(Object value) {
            for (SimpleType<?> type : simpleTypes) {
                if (type.isValue(value)) {
                    return true;
                }
            }
            return false;
        }
    }
}
