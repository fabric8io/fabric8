/**
 * Copyright (C) 2011, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.fabric.service;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import javax.management.JMX;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import org.apache.karaf.admin.management.AdminServiceMBean;
import org.fusesource.fabric.api.Agent;
import org.fusesource.fabric.api.FabricException;
import org.osgi.jmx.framework.BundleStateMBean;
import org.osgi.jmx.framework.ServiceStateMBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class which contains code related to JMX connectivity.
 *
 * @author ldywicki
 */
public class JmxTemplate extends JmxTemplateSupport {

    public <T> T execute(Agent agent, JmxConnectorCallback<T> callback) {
        JMXConnector connector = createConnector(agent);
        try {
            return callback.doWithJmxConnector(connector);
        } catch (FabricException e) {
            throw e;
        } catch (Exception e) {
            throw new FabricException(e);
        } finally {
            try {
                connector.close();
            } catch (IOException e) {
            }
        }
    }

    // mBean specific callbacks

    public <T> T execute(final Agent agent, final AdminServiceCallback<T> callback) {
        return execute(agent, new JmxConnectorCallback<T>() {
            public T doWithJmxConnector(JMXConnector connector) throws Exception {
                String[] bean = new String[] {"type", "admin", "name", agent.getId()};
                return callback.doWithAdminService(getMBean(connector, AdminServiceMBean.class, "org.apache.karaf", bean));
            }
        });
    }

    public <T> T execute(final Agent agent, final BundleStateCallback<T> callback) {
        return execute(agent, new JmxConnectorCallback<T>() {
            public T doWithJmxConnector(JMXConnector connector) throws Exception {
                String[] bean = new String[] {"type", "bundleState", "version", "1.5"};
                return callback.doWithBundleState(getMBean(connector, BundleStateMBean.class, "osgi.core", bean));
            }
        });
    }

    public <T> T execute(final Agent agent, final ServiceStateCallback<T> callback) {
        return execute(agent, new JmxConnectorCallback<T>() {
            public T doWithJmxConnector(JMXConnector connector) throws Exception {
                String[] bean = new String[] {"type", "serviceState", "version", "1.5"};
                return callback.doWithServiceState(getMBean(connector, ServiceStateMBean.class, "osgi.core", bean));
            }
        });
    }

}
