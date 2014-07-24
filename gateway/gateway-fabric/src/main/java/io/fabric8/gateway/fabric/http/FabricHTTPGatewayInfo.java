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
package io.fabric8.gateway.fabric.http;

import io.fabric8.api.RuntimeProperties;
import io.fabric8.api.jmx.FileSystemMBean;
import io.fabric8.common.util.ShutdownTracker;
import io.fabric8.gateway.fabric.jmx.FabricGatewayInfoMBean;

import java.io.File;
import java.io.IOException;

import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.StandardMBean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 */
public class FabricHTTPGatewayInfo implements FabricGatewayInfoMBean {

    private static final transient Logger LOG = LoggerFactory.getLogger(FabricHTTPGatewayInfo.class);

    private ObjectName objectName;

    
    @Override
	public long getNumberOfInvocations() {
		// TODO Auto-generated method stub
		return 0;
	}
   
    public ObjectName getObjectName() throws MalformedObjectNameException {
        if (objectName == null) {
            // TODO to avoid mbean clashes if ever a JVM had multiple FabricService instances, we may
            // want to add a parameter of the fabric ID here...
            objectName = new ObjectName("io.fabric8.gateway-fabric:service=FabricHTTPGatewayInfo");
        }
        return objectName;
    }

    public void setObjectName(ObjectName objectName) {
        this.objectName = objectName;
    }

    public void registerMBeanServer(ShutdownTracker shutdownTracker, MBeanServer mbeanServer) {
        try {
            ObjectName name = getObjectName();
            if (!mbeanServer.isRegistered(name)) {
                StandardMBean mbean = new StandardMBean(this, FabricGatewayInfoMBean.class);
                mbeanServer.registerMBean(mbean, name);
            }
        } catch (Exception e) {
            LOG.warn("An error occurred during mbean server registration: " + e, e);
        }
    }

    public void unregisterMBeanServer(MBeanServer mbeanServer) {
        if (mbeanServer != null) {
            try {
                ObjectName name = getObjectName();
                if (mbeanServer.isRegistered(name)) {
                    mbeanServer.unregisterMBean(name);
                }
            } catch (Exception e) {
                LOG.warn("An error occurred during mbean server registration: " + e, e);
            }
        }
    }

	
}
