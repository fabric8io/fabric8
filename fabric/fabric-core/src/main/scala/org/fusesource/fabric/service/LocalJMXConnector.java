/*
 * Copyright (C) 2011, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.fabric.service;

import javax.management.ListenerNotFoundException;
import javax.management.MBeanServerConnection;
import javax.management.NotificationFilter;
import javax.management.NotificationListener;
import javax.management.remote.JMXConnector;
import javax.security.auth.Subject;
import java.io.IOException;
import java.util.Map;

public class LocalJMXConnector implements JMXConnector {

    private final MBeanServerConnection mbeanServerConnection;

    public LocalJMXConnector(MBeanServerConnection mbeanServerConnection) {
        this.mbeanServerConnection = mbeanServerConnection;
    }

    @Override
    public void connect() throws IOException {
    }

    @Override
    public void connect(Map<String, ?> stringMap) throws IOException {
    }

    @Override
    public MBeanServerConnection getMBeanServerConnection() throws IOException {
        return mbeanServerConnection;
    }

    @Override
    public MBeanServerConnection getMBeanServerConnection(Subject subject) throws IOException {
        return mbeanServerConnection;
    }

    @Override
    public void close() throws IOException {
    }

    @Override
    public void addConnectionNotificationListener(NotificationListener notificationListener, NotificationFilter notificationFilter, Object o) {
        // TODO
        // mbeanServerConnection.addNotificationListener(notificationListener, notificationFilter, o);
    }

    @Override
    public void removeConnectionNotificationListener(NotificationListener notificationListener) throws ListenerNotFoundException {
        // TODO

    }

    @Override
    public void removeConnectionNotificationListener(NotificationListener notificationListener, NotificationFilter notificationFilter, Object o) throws ListenerNotFoundException {
        // TODO

    }

    @Override
    public String getConnectionId() throws IOException {
        return mbeanServerConnection.toString();
    }
}
