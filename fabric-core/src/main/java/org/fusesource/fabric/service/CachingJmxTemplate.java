/*
 * Copyright (C) 2011, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.fabric.service;

import org.apache.karaf.admin.management.AdminServiceMBean;
import org.fusesource.fabric.api.Agent;
import org.fusesource.fabric.api.FabricException;
import org.osgi.jmx.framework.BundleStateMBean;
import org.osgi.jmx.framework.ServiceStateMBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.remote.JMXConnector;
import java.io.IOException;

/**
 * A Caching implementation of JmxTemplate which caches a connector for a given Agent
 */
public class CachingJmxTemplate extends JmxTemplateSupport {
    private static final Logger LOGGER = LoggerFactory.getLogger(CachingJmxTemplate.class);

    private final Agent agent;
    private JMXConnector connector;

    public CachingJmxTemplate(Agent agent) {
        this.agent = agent;
    }

    public <T> T execute(JmxConnectorCallback<T> callback) {
        JMXConnector connector = createConnector(agent);
        try {
            return callback.doWithJmxConnector(getConnector());
        } catch (FabricException e) {
            throw e;
        } catch (Exception e) {
            try {
                close();
            } catch (Exception e2) {
                LOGGER.debug("Exception when attempting to close connection " + e2 + " after getting exception: " + e, e2);
            }
            throw new FabricException(e);
        }
    }

    public synchronized void close() {
        if (connector != null) {
            try {
                connector.close();
            } catch (IOException e) {
                throw new FabricException("Failed to close connection on agent " + agent + ". " + e, e);
            } finally {
                connector = null;
            }
        }
    }

    protected synchronized JMXConnector getConnector() {
        if (connector == null) {
            connector = createConnector(agent);
        }
        return connector;
    }

    // mBean specific callbacks

    public <T> T execute(final AdminServiceCallback<T> callback) {
        return execute(new JmxConnectorCallback<T>() {
            public T doWithJmxConnector(JMXConnector connector) throws Exception {
                String[] bean = new String[]{"type", "admin", "name", agent.getId()};
                return callback.doWithAdminService(getMBean(connector, AdminServiceMBean.class, "org.apache.karaf", bean));
            }
        });
    }

    public <T> T execute(final BundleStateCallback<T> callback) {
        return execute(new JmxConnectorCallback<T>() {
            public T doWithJmxConnector(JMXConnector connector) throws Exception {
                String[] bean = new String[]{"type", "bundleState", "version", "1.5"};
                return callback.doWithBundleState(getMBean(connector, BundleStateMBean.class, "osgi.core", bean));
            }
        });
    }

    public <T> T execute(final ServiceStateCallback<T> callback) {
        return execute(new JmxConnectorCallback<T>() {
            public T doWithJmxConnector(JMXConnector connector) throws Exception {
                String[] bean = new String[]{"type", "serviceState", "version", "1.5"};
                return callback.doWithServiceState(getMBean(connector, ServiceStateMBean.class, "osgi.core", bean));
            }
        });
    }
}
