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
package io.fabric8.testkit.jolokia;

import io.fabric8.api.jmx.FabricManagerMBean;
import org.jolokia.client.J4pClient;
import org.json.simple.JSONArray;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import java.lang.reflect.Array;

/**
 * Factory method of JMX MBean proxies for working with Fabric
 */
public class JolokiaClients {
    public static final ObjectName FABRIC_MANAGER_MBEAN = createObjectName("io.fabric8:type=Fabric");

    public static FabricManagerMBean createFabricManager(J4pClient jolokia) {
        return JolokiaInvocationHandler.newProxyInstance(jolokia, FABRIC_MANAGER_MBEAN, FabricManagerMBean.class);
    }

    protected static ObjectName createObjectName(String objectName) {
        try {
            return new ObjectName(objectName);
        } catch (MalformedObjectNameException e) {
            throw new RuntimeException(e);
        }
    }

    public static Object convertJolokiaToJavaType(Class<?> clazz, Object value) {
        if (clazz.isArray()) {
            if (value instanceof JSONArray) {
                JSONArray jsonArray = (JSONArray) value;
                Object[] javaArray = (Object[]) Array.newInstance(clazz.getComponentType(), jsonArray.size());
                int idx = 0;
                for (Object element : jsonArray) {
                    Array.set(javaArray, idx++, convertJolokiaToJavaType(clazz.getComponentType(), element));
                }
                return javaArray;
            } else {
                return null;
            }
        } else if (String.class.equals(clazz)) {
            return (value != null) ? value.toString() : null;
        } else if (clazz.equals(Byte.class) || clazz.equals(byte.class)) {
            Number number = asNumber(value);
            return number != null ? number.byteValue() : null;
        } else if (clazz.equals(Short.class) || clazz.equals(short.class)) {
            Number number = asNumber(value);
            return number != null ? number.shortValue() : null;
        } else if (clazz.equals(Integer.class) || clazz.equals(int.class)) {
            Number number = asNumber(value);
            return number != null ? number.intValue() : null;
        } else if (clazz.equals(Long.class) || clazz.equals(long.class)) {
            Number number = asNumber(value);
            return number != null ? number.longValue() : null;
        } else if (clazz.equals(Float.class) || clazz.equals(float.class)) {
            Number number = asNumber(value);
            return number != null ? number.floatValue() : null;
        } else if (clazz.equals(Double.class) || clazz.equals(double.class)) {
            Number number = asNumber(value);
            return number != null ? number.doubleValue() : null;
        }
        return value;
    }

    protected static Number asNumber(Object value) {
        if (value instanceof Number) {
            return (Number) value;
        } else {
            return null;
        }
    }
}
