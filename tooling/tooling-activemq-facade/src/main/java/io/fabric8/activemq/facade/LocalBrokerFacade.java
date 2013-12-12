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

package io.fabric8.activemq.facade;

import org.apache.activemq.broker.Broker;
import org.apache.activemq.broker.BrokerService;
import org.apache.activemq.broker.jmx.BrokerView;
import org.apache.activemq.broker.jmx.ManagedRegionBroker;
import org.apache.activemq.broker.jmx.ManagementContext;
import org.apache.activemq.broker.region.Destination;
import org.apache.activemq.broker.region.Queue;
import org.apache.activemq.command.ActiveMQDestination;

import javax.management.ObjectName;
import javax.management.QueryExp;
import java.util.Iterator;
import java.util.Set;

/**
 * An implementation of {@link BrokerFacade} which uses a local in JVM broker
 * 
 * 
 */
public class LocalBrokerFacade extends BrokerFacadeSupport {
	private BrokerService brokerService;

    public LocalBrokerFacade(BrokerService brokerService) {
		this.brokerService = brokerService;
	}

	public BrokerService getBrokerService() {
		return brokerService;
	}

    @Override
    public String getId() throws Exception {
        return brokerService.getBrokerName();
    }

    public BrokerFacade[] getBrokers() throws Exception {
        return new BrokerFacade[]{this};
    }

	public String getBrokerName() throws Exception {
		return brokerService.getBrokerName();
	}
	public Broker getBroker() throws Exception {
		return brokerService.getBroker();
	}
	public ManagementContext getManagementContext() {
		return brokerService.getManagementContext();
	}
	public BrokerViewFacade getBrokerAdmin() throws Exception {
		return proxy(BrokerViewFacade.class, brokerService.getAdminView(), brokerService.getBrokerName());
	}
	public ManagedRegionBroker getManagedBroker() throws Exception {
		BrokerView adminView = brokerService.getAdminView();
		if (adminView == null) {
			return null;
		}
		return adminView.getBroker();
	}

    public void purgeQueue(ActiveMQDestination destination) throws Exception {
        Set destinations = getManagedBroker().getQueueRegion().getDestinations(destination);
        for (Iterator i = destinations.iterator(); i.hasNext();) {
            Destination dest = (Destination) i.next();
            if (dest instanceof Queue) {
                Queue regionQueue = (Queue) dest;
                regionQueue.purge();
            }
        }
    }

    @Override
    public Set queryNames(ObjectName name, QueryExp query) throws Exception {
        return getManagementContext().queryNames(name, query);
    }

    @Override
    public Object newProxyInstance(ObjectName objectName, Class interfaceClass, boolean notificationBroadcaster) {
        return getManagementContext().newProxyInstance(objectName, interfaceClass, notificationBroadcaster);
    }
    
}
