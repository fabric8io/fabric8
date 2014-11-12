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
package io.fabric8.jolokia.client;

import java.lang.management.ManagementFactory;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanParameterInfo;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.StandardMBean;
import javax.management.openmbean.OpenDataException;
import javax.management.openmbean.OpenMBeanAttributeInfo;
import javax.management.openmbean.OpenMBeanOperationInfo;
import javax.management.openmbean.OpenMBeanParameterInfo;
import javax.management.openmbean.OpenType;

import io.fabric8.api.gravia.IllegalStateAssertion;
import org.jolokia.client.J4pClient;
import org.jolokia.client.request.J4pExecRequest;
import org.jolokia.client.request.J4pReadRequest;
import org.jolokia.client.request.J4pWriteRequest;
import org.json.simple.JSONAware;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A Jolokia proxy for MXBean compliant MBeans.
 *
 * It utilises the {@link JSONTypeGenerator}.
 *
 * @author thomas.diesler@jboss.com
 * @since 23-Jun-2014
 */
public final class JolokiaMXBeanProxy {

    private static final Logger LOGGER = LoggerFactory.getLogger(JolokiaMXBeanProxy.class);
    
    // Hide ctor
    private JolokiaMXBeanProxy() {
    }

    public static <T extends Object> T getMXBeanProxy(String serviceURL, ObjectName objectName, Class<T> mxbeanInterface) {
        return getMXBeanProxy(serviceURL, objectName, mxbeanInterface, null, null, null);
    }

    public static <T extends Object> T getMXBeanProxy(String serviceURL, ObjectName objectName, Class<T> mxbeanInterface, String username, String password) {
        return getMXBeanProxy(serviceURL, objectName, mxbeanInterface, username, password, null);
    }

    @SuppressWarnings("unchecked")
    public static <T extends Object> T getMXBeanProxy(String serviceURL, ObjectName objectName, Class<T> mxbeanInterface, String username, String password, MBeanInfo mbeanInfo) {
        
        // If the MBeanInfo is not given, get it from the the local MBeanServer
        // [TODO] this should be obtaind remotely over jolokia
        if (mbeanInfo == null) {
            MBeanServer server = ManagementFactory.getPlatformMBeanServer();
            try {
                try {
                    StandardMBean impl = new StandardMBean(Mockito.mock(mxbeanInterface), mxbeanInterface, true);
                    server.registerMBean(impl, objectName);
                    mbeanInfo = server.getMBeanInfo(objectName);
                } finally {
                    if (server.isRegistered(objectName)) {
                        server.unregisterMBean(objectName);
                    }
                }
            } catch (Exception ex) {
                throw new IllegalStateException("Cannot obtain MBeanInfo", ex);
            }
        }
        
        ClassLoader classLoader = mxbeanInterface.getClassLoader();
        InvocationHandler handler = new MXBeanInvocationHandler(serviceURL, objectName, username, password, classLoader, mbeanInfo);
        return (T) Proxy.newProxyInstance(classLoader, new Class<?>[] { mxbeanInterface}, handler);
    }

    private static class MXBeanInvocationHandler implements InvocationHandler {

        private final Map<String, MBeanAttributeInfo> attributes = new HashMap<>();
        private final Set<MBeanOperationInfo> operations = new HashSet<>();
        private final ClassLoader classLoader;
        private final ObjectName objectName;
        private final J4pClient client;

        private MXBeanInvocationHandler(String serviceURL, ObjectName objectName, String username, String password, ClassLoader classLoader, MBeanInfo mbeanInfo) {
            this.client = J4pClient.url(serviceURL).user(username).password(password).connectionTimeout(3000).build();
            this.classLoader = classLoader;
            this.objectName = objectName;
            for (MBeanAttributeInfo info : mbeanInfo.getAttributes()) {
                attributes.put(info.getName(), info);
            }
            for (MBeanOperationInfo info : mbeanInfo.getOperations()) {
                operations.add(info);
            }
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            try {
                if (isGetter(method)) {
                    String attname = getAttributeName(method);
                    J4pReadRequest readReq = new J4pReadRequest(objectName, attname);
                    Object result = client.execute(readReq).getValue();
                    return unmarshalResult(method, result);
                } else if (isSetter(method)) {
                    String attname = getAttributeName(method);
                    Object[] params = marshalParameters(method, args);
                    J4pWriteRequest writeReq = new J4pWriteRequest(objectName, attname, params);
                    writeReq.setPreferredHttpMethod("POST");
                    client.execute(writeReq);
                    return null;
                } else {
                    Object[] params = marshalParameters(method, args);
                    String operation = getOperationInfo(method).getName();
                    J4pExecRequest execReq = new J4pExecRequest(objectName, operation, params);
                    execReq.setPreferredHttpMethod("POST");
                    Object result = client.execute(execReq).getValue();
                    return unmarshalResult(method, result);
                }
            } catch (Throwable th) {
                LOGGER.error("Proxy invocation error on: " + method.getDeclaringClass().getName() + "." + method.getName(), th);
                throw th;
            }
        }

        private Object[] marshalParameters(Method method, Object[] params) throws OpenDataException {
            Object[] result = new Object[params.length];
            if (isSetter(method)) {
                String attname = getAttributeName(method);
                MBeanAttributeInfo attInfo = attributes.get(attname);
                if (attInfo instanceof OpenMBeanAttributeInfo) {
                    OpenType<?> openType = ((OpenMBeanAttributeInfo) attInfo).getOpenType();
                    Object openData = OpenTypeGenerator.toOpenData(openType, params[0]);
                    Object jsonAware = JSONTypeGenerator.toJSON(openData);
                    result[0] = jsonAware;
                } else {
                    Object jsonAware = JSONTypeGenerator.toJSON(params[0]);
                    result[0] = jsonAware;
                }
            } else {
                MBeanOperationInfo opinfo = getOperationInfo(method);
                MBeanParameterInfo[] signature = opinfo.getSignature();
                for (int i = 0; i < params.length; i++) {
                    MBeanParameterInfo paramInfo = signature[i];
                    if (paramInfo instanceof OpenMBeanParameterInfo) {
                        OpenType<?> openType = ((OpenMBeanParameterInfo) paramInfo).getOpenType();
                        Object openData = OpenTypeGenerator.toOpenData(openType, params[i]);
                        Object jsonAware = JSONTypeGenerator.toJSON(openData);
                        result[i] = jsonAware;
                    } else {
                        Object jsonAware = JSONTypeGenerator.toJSON(params[i]);
                        result[i] = jsonAware;
                    }
                }
            }
            return result;
        }

        private MBeanOperationInfo getOperationInfo(Method method) {
            MBeanOperationInfo result = null;
            for (MBeanOperationInfo opinfo : operations) {
                MBeanParameterInfo[] signature = opinfo.getSignature();
                if (opinfo.getName().equals(method.getName()) && signature.length == method.getParameterTypes().length) {
                    result = opinfo;
                    break;
                }
            }
            IllegalStateAssertion.assertNotNull(result, "Cannot find MBeanOperationInfo for: " + method);
            return result;
        }

        private Object unmarshalResult(Method method, Object value) throws OpenDataException {

            if (value == null) 
                return null;
            
            OpenType<?> openType = null;
            if (isGetter(method)) {
                String attname = getAttributeName(method);
                MBeanAttributeInfo attInfo = attributes.get(attname);
                if (attInfo instanceof OpenMBeanAttributeInfo) {
                    openType = ((OpenMBeanAttributeInfo) attInfo).getOpenType();
                }
            } else {
                MBeanOperationInfo opinfo = getOperationInfo(method);
                if (opinfo instanceof OpenMBeanOperationInfo) {
                    openType = ((OpenMBeanOperationInfo) opinfo).getReturnOpenType();
                }
            }
            
            // Convert a JSON return values to openmbean types
            // [TODO] this should be handles by jolokia internally for openmbean results
            if (value instanceof JSONAware) {
                value = JSONTypeGenerator.toOpenData(openType, classLoader, (JSONAware) value);
            }
            
            // Convert open type to java type
            if (openType != null) {
                value = OpenTypeGenerator.fromOpenData(openType, classLoader, value);
            }
            
            // Convert to actual method return type
            return OpenTypeGenerator.toTargetType(method.getReturnType(), value);
        }

        private boolean isGetter(Method method) {
            String methodName = method.getName();
            Class<?>[] paramTypes = method.getParameterTypes();
            Class<?> returnType = method.getReturnType();
            return Modifier.isPublic(method.getModifiers()) && returnType != void.class && (methodName.startsWith("get") || methodName.startsWith("is")) && paramTypes.length == 0;
        }

        private boolean isSetter(Method method) {
            String methodName = method.getName();
            Class<?>[] paramTypes = method.getParameterTypes();
            Class<?> returnType = method.getReturnType();
            return Modifier.isPublic(method.getModifiers()) && returnType == void.class && methodName.startsWith("set") && paramTypes.length == 1;
        }
        
        private String getAttributeName(Method method) {
            String methodName = method.getName();
            return methodName.startsWith("is") ? methodName.substring(2) : methodName.substring(3);
        }
    }
}
