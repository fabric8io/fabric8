/*
 * Copyright (C) 2011, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */

package org.fusesource.fabric.jaas;

import java.util.Dictionary;
import org.linkedin.zookeeper.client.IZKClient;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JaasRealmManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(JaasRealmManager.class);

    private static final String KARAF_SHELL_PID = "org.apache.karaf.shell";
    private static final String KARAF_MANAGEMENT_PID = "org.apache.karaf.management";

    private static final String SSH_REALM = "sshRealm";
    private static final String JMX_REALM = "jmxRealm";
    private static final String ZOOKEEPER_REALM = "zookeeper";
    private static final String ZOOKEEPER_CLIENT = "org.linkedin.zookeeper.client.IZKClient";

    private BundleContext bundleContext;
    private ConfigurationAdmin configAdmin;
    private ServiceReference configAdminServiceReference;

    private String defaultSshRealm = "karaf";
    private String defaultJmxRealm = "karaf";

    private boolean realmsUpdated;

    public void init() {
        configAdminServiceReference = bundleContext.getServiceReference(ConfigurationAdmin.class.getName());
        configAdmin = (ConfigurationAdmin) bundleContext.getService(configAdminServiceReference);

        defaultJmxRealm = getRealm(KARAF_MANAGEMENT_PID, JMX_REALM);
        defaultSshRealm = getRealm(KARAF_SHELL_PID, SSH_REALM);

        if (isZookeeperAvailable() && !realmsUpdated) {
            updateRealm(KARAF_SHELL_PID, SSH_REALM, ZOOKEEPER_REALM);
            updateRealm(KARAF_MANAGEMENT_PID, JMX_REALM, ZOOKEEPER_REALM);
            realmsUpdated = true;
        }
    }

    public void destroy() {
        updateRealm(KARAF_SHELL_PID, SSH_REALM, defaultSshRealm);
        updateRealm(KARAF_MANAGEMENT_PID, JMX_REALM, defaultJmxRealm);
        bundleContext.ungetService(configAdminServiceReference);
    }

    /**
     * Checks if Zookeeper client is registered.
     *
     * @return Returns true if {@code IZKClinet} is found in the service registry.
     */
    public boolean isZookeeperAvailable() {
        boolean available = false;
        ServiceReference serviceReference = null;
        try {
            serviceReference = bundleContext.getServiceReference(ZOOKEEPER_CLIENT);
            if (serviceReference != null) {
                IZKClient client = (IZKClient) bundleContext.getService(serviceReference);
                if (client != null && client.isConnected()) {
                    available = true;
                }
            }
        } catch (Exception ex) {
            //Ignore
        }
        finally {
            if (serviceReference != null) {
                bundleContext.ungetService(serviceReference);
            }
        }
        return available;
    }

    /**
     * Enables the Zookeeper Realm
     */
    public void updateRealm(String pid, String realmProperty, String realm) {
        try {
            Configuration config = configAdmin.getConfiguration(pid);
            Dictionary props = config.getProperties();
            props.put(realmProperty, realm);
            config.setBundleLocation(null);
            config.update(props);
        } catch (Exception e) {
            LOGGER.error("Error enabling zookeeper realm for " + realmProperty,e);
        }
    }


    /**
     * Enables the Zookeeper Realm
     */
    public String getRealm(String pid, String realmProperty) {
        String realm = "karaf";
        try {
            Configuration config = configAdmin.getConfiguration(pid);
            if (config != null) {
                Dictionary props = config.getProperties();
                realm = (String) props.get(realmProperty);
            }
        } catch (Exception e) {
            LOGGER.warn("Error reading the realm " + realmProperty, e);
        }
        return realm;
    }

    public void bind(IZKClient izkClient) {
        if (isZookeeperAvailable() && !realmsUpdated) {
            updateRealm(KARAF_SHELL_PID, SSH_REALM,  ZOOKEEPER_REALM);
            updateRealm(KARAF_MANAGEMENT_PID, JMX_REALM, ZOOKEEPER_REALM);
            realmsUpdated = true;
        }
    }

    public void unbind(IZKClient izkClient) {
        //We don't want to unset the realm just because the client is gone.
    }

    public BundleContext getBundleContext() {
        return bundleContext;
    }

    public void setBundleContext(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }
}

