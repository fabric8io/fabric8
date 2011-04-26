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
public class JmxTemplate {

    private static final Logger LOGGER = LoggerFactory.getLogger(JmxTemplate.class);

    private String login = "karaf";
    private String password = "karaf";


    public interface JmxConnectorCallback<T> {

        T doWithJmxConnector(JMXConnector connector) throws Exception;

    }

    public interface AdminServiceCallback<T> {

        T doWithAdminService(AdminServiceMBean adminService) throws Exception;

    }

    public interface BundleStateCallback<T> {

        T doWithBundleState(BundleStateMBean bundleState) throws Exception;
    }

    public interface ServiceStateCallback<T> {

        T doWithServiceState(ServiceStateMBean serviceState) throws Exception;
    }

    public <T> T execute(Agent agent, JmxConnectorCallback<T> callback) {
        String rootUrl = agent.getJmxUrl();
        JMXConnector connector;
        try {
            connector = JMXConnectorFactory.connect(
                    new JMXServiceURL(rootUrl),
                    getEnvCred(login, password));
        } catch (IOException e) {
            throw new FabricException(e);
        }
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

    private <T> T getMBean(JMXConnector connector, Class<T> type, String domain, String ... params) {
        try {
            return JMX.newMBeanProxy(connector.getMBeanServerConnection(), safeObjectName(domain, params), type);
        } catch (IOException e) {
            throw new FabricException(e);
        }
    }

    public static ObjectName safeObjectName(String domain, String ... args) {
        if ((args.length % 2) != 0) {
             LOGGER.warn("Not all values were defined for arguments %", Arrays.toString(args));
        }
        Hashtable<String, String> table = new Hashtable<String, String>();
        for (int i = 0; i < args.length; i += 2) {
            table.put(args[i], args[i + 1]);
        }
        try {
            return new ObjectName(domain, table);
        } catch (MalformedObjectNameException e) {
            throw new RuntimeException("Object name is invalid", e);
        }
    }

    public static Map getEnvCred(String login, String password) {
        Map env = new HashMap<String, Object>();
        env.put(JMXConnector.CREDENTIALS, new String[] {login, password});
        return env;
    }
}
