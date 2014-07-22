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

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

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
}
