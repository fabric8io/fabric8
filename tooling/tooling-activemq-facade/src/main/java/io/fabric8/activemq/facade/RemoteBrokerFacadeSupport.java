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

import org.apache.activemq.broker.jmx.BrokerViewMBean;
import org.apache.activemq.broker.jmx.ManagementContext;
import org.apache.activemq.broker.jmx.QueueViewMBean;
import org.apache.activemq.command.ActiveMQDestination;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.MBeanServerConnection;
import javax.management.MBeanServerInvocationHandler;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.QueryExp;
import javax.management.remote.JMXConnector;
import java.io.IOException;
import java.util.*;

/**
 */
public abstract class RemoteBrokerFacadeSupport extends BrokerFacadeSupport {
    private static final transient Logger LOG = LoggerFactory.getLogger(RemoteBrokerFacade.class);
    private String brokerName;

    public RemoteBrokerFacadeSupport() {
    }

    public RemoteBrokerFacadeSupport(String brokerName) {
        this.brokerName = brokerName;
    }

    public String getBrokerName() throws Exception,
            MalformedObjectNameException {
        return getBrokerAdmin().getBrokerName();
    }

    public void setBrokerName(String brokerName) {
        this.brokerName = brokerName;
    }

    private ObjectName getBrokerObjectName(MBeanServerConnection connection)
            throws IOException, MalformedObjectNameException {
        Set<ObjectName> brokers = findBrokers(connection);
        if (brokers.size() == 0) {
            throw new IOException("No broker could be found in the JMX.");
        }
        ObjectName name = brokers.iterator().next();
        return name;
    }

    public BrokerViewFacade getBrokerAdmin() throws Exception {
        MBeanServerConnection connection = getMBeanServerConnection();

        Set brokers = findBrokers(connection);
        if (brokers.size() == 0) {
            throw new IOException("No broker could be found in the JMX.");
        }
        ObjectName name = (ObjectName) brokers.iterator().next();
        BrokerViewMBean mbean = (BrokerViewMBean) MBeanServerInvocationHandler.newProxyInstance(connection, name, BrokerViewMBean.class, true);
        return proxy(BrokerViewFacade.class, mbean, name.getCanonicalName());
    }

    @Override
    public String getId() throws Exception {
        Set brokers = findBrokers(getMBeanServerConnection());
        if (brokers.size() == 0) {
            throw new IOException("No broker could be found in the JMX.");
        }
        ObjectName name = (ObjectName) brokers.iterator().next();
        return name.getCanonicalName();
    }

    public String[] getBrokerNames() throws Exception {
        MBeanServerConnection connection = getMBeanServerConnection();
        ObjectName names = new ObjectName("org.apache.activemq:type=Broker,brokerName=*");
        Set<String> rc = new HashSet<String>();
        Set<ObjectName> objectNames = connection.queryNames(names, null);
        for(ObjectName name: objectNames) {
            String bn = name.getKeyProperty("brokerName");
            if(bn!=null) {
                rc.add(bn);
            }
        }
        return rc.toArray(new String[rc.size()]);
    }

    protected abstract MBeanServerConnection getMBeanServerConnection() throws Exception;

    /**
     * Finds all ActiveMQ-Brokers registered on a certain JMX-Server or, if a
     * JMX-BrokerName has been set, the broker with that name.
     *
     * @param connection not <code>null</code>
     * @return Set with ObjectName-elements
     * @throws java.io.IOException
     * @throws javax.management.MalformedObjectNameException
     *
     */
    @SuppressWarnings("unchecked")
    protected Set<ObjectName> findBrokers(MBeanServerConnection connection)
            throws IOException, MalformedObjectNameException {
        ObjectName name;
        if (this.brokerName == null) {
            name = new ObjectName("org.apache.activemq:type=Broker,brokerName=*");
        } else {
            name = new ObjectName("org.apache.activemq:brokerName="
                    + this.brokerName + ",type=Broker");
        }

        Set<ObjectName> brokers = connection.queryNames(name, null);
        return brokers;
    }

    public void purgeQueue(ActiveMQDestination destination) throws Exception {
        QueueViewMBean queue = getQueue(destination.getPhysicalName());
        queue.purge();
    }

    public ManagementContext getManagementContext() {
        throw new IllegalStateException("not supported");
    }

    @SuppressWarnings("unchecked")
    protected <T> Collection<T> getManagedObjects(ObjectName[] names,
                                                  Class<T> type) {
        MBeanServerConnection connection;
        try {
            connection = getMBeanServerConnection();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        List<T> answer = new ArrayList<T>();
        if (connection != null) {
            for (int i = 0; i < names.length; i++) {
                ObjectName name = names[i];
                T value = (T) MBeanServerInvocationHandler.newProxyInstance(
                        connection, name, type, true);
                if (value != null) {
                    answer.add(value);
                }
            }
        }
        return answer;
    }

    @Override
    public Set queryNames(ObjectName name, QueryExp query) throws Exception {
        return getMBeanServerConnection().queryNames(name, query);
    }


    @Override
    public Object newProxyInstance(ObjectName objectName, Class interfaceClass, boolean notificationBroadcaster) throws Exception {
        return MBeanServerInvocationHandler.newProxyInstance(getMBeanServerConnection(), objectName, interfaceClass, notificationBroadcaster);
    }

    protected boolean isConnectionActive(JMXConnector connector) {
        if (connector == null) {
            return false;
        }

        try {
            MBeanServerConnection connection = connector.getMBeanServerConnection();
            int brokerCount = findBrokers(connection).size();
            return brokerCount > 0;
        } catch (Exception e) {
            return false;
        }
    }
}
