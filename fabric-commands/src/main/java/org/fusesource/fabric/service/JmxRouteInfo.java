/**
 * Copyright (C) 2011, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.fabric.service;

import org.fusesource.fabric.api.data.RouteInfo;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;

/**
 * Route info based on JMX calls.
 *
 * @author ldywicki
 */
public class JmxRouteInfo implements RouteInfo {

    private MBeanServerConnection connection;
    private ObjectName name;

    public JmxRouteInfo(MBeanServerConnection connection, ObjectName route) {
        this.connection = connection;
        this.name = route;
    }

    public String getState() {
        try {
            return (String) connection.invoke(name, "getState", new Class[0], new String[0]);
        } catch (Exception e) {
            return "unknown";
        }
    }
}
