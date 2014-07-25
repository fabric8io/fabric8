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

import io.fabric8.common.util.ShutdownTracker;
import io.fabric8.gateway.fabric.jmx.FabricGatewayInfoMBean;

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

    private final FabricHTTPGateway fabricHTTPGateway;
    private ObjectName objectName;
    private long numberOfInvocations = 0l;
    private long averageCallTimeNanos = 0l;
    private String lastError;
    private String lastCallDate;
    
    public FabricHTTPGatewayInfo(FabricHTTPGateway fabricHTTPGateway) {
		super();
		this.fabricHTTPGateway = fabricHTTPGateway;
	}
    
    protected FabricHTTPGateway getFabricHTTPGateway() {
    	return fabricHTTPGateway;
    }

	@Override
	public int getPort() {
		return getFabricHTTPGateway().getPort();
	}
	
	@Override
	public String getHost() {
		return getFabricHTTPGateway().getHost();
	}
	
	@Override
	public String getGatewayVersion() {
		return getFabricHTTPGateway().getGatewayVersion();
	}
	
	@Override
	public String getLocalAddress() {
		return getFabricHTTPGateway().getLocalAddress().toString();
	}
	
	@Override
	public boolean isEnableIndex() {
		return getFabricHTTPGateway().isEnableIndex();
	}
	
	@Override
	public String getMappedServices() {
		String mappedServices = "";
		if (getFabricHTTPGateway().getMappedServices()!=null) {
			for (String mappedServiceKey : getFabricHTTPGateway().getMappedServices().keySet()) {
				mappedServices += mappedServiceKey + ":" + getFabricHTTPGateway().getMappedServices().get(mappedServiceKey) + "<BR>";
			}
		}
		return mappedServices;
	}

    @Override
	public long getNumberOfInvocations() {
		return numberOfInvocations;
	}
    
    public void registerCall(long callTimeNanos) {
    	averageCallTimeNanos = (averageCallTimeNanos * numberOfInvocations + callTimeNanos)/++numberOfInvocations;
    }
    
    public void setLastError(String error) {
    	lastError = error;
    }
    
    @Override
    public String getLastError() {
    	return lastError;
    }
    
    public void setLastCallDate(String callDate) {
    	lastCallDate = callDate;
    }
    
    @Override
    public String getLastCallDate() {
    	if (lastCallDate!=null)
    		return lastCallDate.toString();
    	else
    		return null;
    }
    
    @Override 
    public long getAvarageCallTimeNanos() {
    	return averageCallTimeNanos;
    }
    
    @Override
    public void resetStatistics() {
    	averageCallTimeNanos = 0l;
    	numberOfInvocations = 0l;
    	lastCallDate = null;
    	lastError = null;
    }
   
    public ObjectName getObjectName() throws MalformedObjectNameException {
        if (objectName == null) {
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
