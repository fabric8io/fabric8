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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.MBeanServerConnection;
import javax.management.remote.JMXConnector;
import java.io.IOException;

/**
 * A {@link BrokerFacade} which uses a JMX Connector to communicate with a
 * broker
 */
public class JMXConnectorBrokerFacade extends RemoteBrokerFacade {

    private static final transient Logger LOG = LoggerFactory.getLogger(JMXConnectorBrokerFacade.class);

    private final JMXConnector connector;
    private MBeanServerConnection mbeanServerConnection;

    public JMXConnectorBrokerFacade(JMXConnector connector) {
        super(null);
        this.connector = connector;
    }

    public JMXConnector getConnector() {
        return connector;
    }

    @Override
    protected MBeanServerConnection getMBeanServerConnection() throws Exception {
        if (mbeanServerConnection == null) {
            mbeanServerConnection = connector.getMBeanServerConnection();
        }
        return mbeanServerConnection;
    }

    public synchronized void closeConnection() {
        if (connector != null) {
            try {
                LOG.debug("Closing a connection to a broker (" + connector.getConnectionId() + ")");

                connector.close();
            } catch (IOException e) {
                // Ignore the exception, since it most likly won't matter
                // anymore
            }
        }
    }


}
