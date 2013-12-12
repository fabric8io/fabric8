/*
 * Copyright 2010 Red Hat, Inc.
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
package io.fabric8.servicemix.facade;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import javax.management.MBeanServerConnection;
import javax.management.MBeanServerInvocationHandler;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.QueryExp;

import org.apache.servicemix.nmr.management.ManagedEndpointMBean;

/**
 * Common facade support for both local and remote.
 * <p/>
 * This implementation will provide most implementation supports as it turns out
 * that both the local and remote {@link org.apache.servicemix.ServiceMixContext} will use the JMX API to
 * gather information.
 */
public abstract class ServiceMixFacadeSupport implements ServiceMixFacade {
    
    protected final MBeanServerConnection mBeanServer;

    protected ServiceMixFacadeSupport(MBeanServerConnection mBeanServer) throws Exception {
        this.mBeanServer = mBeanServer;
    }

    protected MBeanServerConnection getMBeanServerConnection() throws Exception {
        return mBeanServer;
    }

    protected Set<ObjectInstance> queryNames(ObjectName name, QueryExp query) throws Exception {
        return getMBeanServerConnection().queryMBeans(name, query);
    }

    @SuppressWarnings("unchecked")
    protected Object newProxyInstance(ObjectName objectName, Class interfaceClass, boolean notificationBroadcaster) throws Exception {
        return MBeanServerInvocationHandler.newProxyInstance(getMBeanServerConnection(), objectName, interfaceClass, notificationBroadcaster);
    }


    // ServiceMixFacade
    //---------------------------------------------------------------
    public List<ManagedEndpointMBean> getEndpoints() throws Exception {
        ObjectName query = ObjectName.getInstance("org.apache.servicemix:Type=Endpoint,Id=*");

        Set<ObjectInstance> names = queryNames(query, null);
        List<ManagedEndpointMBean> answer = new ArrayList<ManagedEndpointMBean>();
        for (ObjectInstance on : names) {
            ManagedEndpointMBean component = (ManagedEndpointMBean) newProxyInstance(on.getObjectName(), ManagedEndpointMBean.class, true);
            answer.add(component);
        }
        return answer;
    }

}
