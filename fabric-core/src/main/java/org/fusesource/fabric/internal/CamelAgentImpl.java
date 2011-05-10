/**
 * Copyright (C) 2011, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.fabric.internal;

import org.fusesource.fabric.api.Agent;
import org.fusesource.fabric.api.CamelAgent;
import org.fusesource.fabric.api.data.RouteInfo;
import org.fusesource.fabric.service.FabricServiceImpl;
import org.fusesource.fabric.service.JmxTemplate;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import java.util.Set;

/**
 * Camel agent implementation.
 *
 * @author ldywicki
 */
public class CamelAgentImpl extends AgentImpl implements CamelAgent {

    public CamelAgentImpl(Agent parent, String id, FabricServiceImpl service) {
        super(parent, id, service);
    }

    @Override
    public RouteInfo[] getRoutes() {
        return new JmxTemplate().execute(getParent(), new JmxTemplate.JmxConnectorCallback<RouteInfo[]>() {
            @Override
            public RouteInfo[] doWithJmxConnector(JMXConnector connector) throws Exception {
                MBeanServerConnection connection = connector.getMBeanServerConnection();
                Set<ObjectName> routes = connection.queryNames(new ObjectName("org.apache.camel:type=route,context=" + getId()), null);
                RouteInfo[] routesArray = new RouteInfo[routes.size()];
                int i = 0;
                for (ObjectName route : routes) {
                    routesArray[i++] = new JmxRouteInfo(connection, route);
                }
                return routesArray;
            }
        });
    }


    @Override
    public String getType() {
        return "camel";
    }
}
