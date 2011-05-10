/**
 * Copyright (C) 2011, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.fabric.service;

import org.fusesource.fabric.api.Agent;
import org.fusesource.fabric.jmx.JmxTemplate;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import java.util.*;

import java.util.Collections;

/**
 * An factory class which indicates agent type and creates new instances of concrete classes.
 *
 * @author ldywicki
 */
public class AgentFactory {

    public static Agent[] createChildren(final Agent parent, final FabricServiceImpl service) {
        if (!parent.isAlive()) {
            return new Agent[0];
        }
        List<Agent> agents = new JmxTemplate().execute(parent, new JmxTemplate.JmxConnectorCallback<List<Agent>>() {
            @Override
            public List<Agent> doWithJmxConnector(JMXConnector connector) throws Exception {
                List<Agent> agents = new ArrayList<Agent>();
                MBeanServerConnection connection = connector.getMBeanServerConnection();
                for (String domain : connection.getDomains()) {
                    if ("osgi.core".equals(domain)) {
                        agents.add(new OSGiAgentImpl(parent, parent.getId(), service));
                    } else if ("org.apache.camel".equals(domain)) {
                        Set<ObjectName> names = connection.queryNames(new ObjectName("org.apache.camel:type=context,*"), null);
                        for (ObjectName name : names) {
                            agents.add(new CamelAgentImpl(parent, name.getKeyProperty("context"), service));
                        }
                    } else if ("org.apache.servicemix.nmr".equals(domain)) {
                        // TODO create servicemix agent instance
                    } else if ("org.apache.activemq".equals(domain)) {
                        // TODO create activemq agent instance
                    }
                }

                return agents;
            }
        });

        return agents.toArray(new Agent[agents.size()]);
    }

}
