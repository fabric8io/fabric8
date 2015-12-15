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
package io.fabric8.camel;

import io.fabric8.jolokia.support.JolokiaInvocationHandler;
import org.apache.camel.api.management.mbean.ManagedCamelContextMBean;
import org.jolokia.client.J4pClient;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

/**
 */
public class CamelClient {
    private final J4pClient jolokia;

    public static final String CAMEL_CONTEXT_MBEAN = "org.apache.camel:context=%s,type=context,name=\"%s\"";

    public CamelClient(J4pClient jolokia) {
        this.jolokia = jolokia;
    }

    public ManagedCamelContextMBean getCamelContextMBean(String contextId) throws MalformedObjectNameException {
        String nameText = String.format(CAMEL_CONTEXT_MBEAN, contextId, contextId);
        ObjectName objectName = new ObjectName(nameText);
        return JolokiaInvocationHandler.newProxyInstance(jolokia, objectName, ManagedCamelContextMBean.class);
    }
}
