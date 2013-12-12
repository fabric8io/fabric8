/**
 * Copyright (C) FuseSource, Inc.
 * http://fusesource.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.fabric8.service;

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
