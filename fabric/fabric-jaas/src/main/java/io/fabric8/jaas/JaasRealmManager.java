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

package io.fabric8.jaas;

import java.util.Dictionary;

import org.apache.curator.framework.CuratorFramework;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Deprecated
public class JaasRealmManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(JaasRealmManager.class);

    private static final String KARAF_SHELL_PID = "org.apache.karaf.shell";
    private static final String KARAF_MANAGEMENT_PID = "org.apache.karaf.management";

    private static final String SSH_REALM = "sshRealm";
    private static final String JMX_REALM = "jmxRealm";
    private static final String ZOOKEEPER_REALM = "zookeeper";
    private static final String ZOOKEEPER_CLIENT = "org.apache.curator.framework.CuratorFramework";

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
                CuratorFramework client = (CuratorFramework) bundleContext.getService(serviceReference);
                if (client != null && client.getZookeeperClient().isConnected()) {
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
            if (!realm.equals(props.get(realmProperty))) {
                LOGGER.debug("Changing pid {} to {} realm.",pid, realm);
                props.put(realmProperty, realm);
                config.setBundleLocation(null);
                config.update(props);
            }
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

    public void bind(CuratorFramework curator) {
        if (isZookeeperAvailable() && !realmsUpdated) {
            updateRealm(KARAF_SHELL_PID, SSH_REALM,  ZOOKEEPER_REALM);
            updateRealm(KARAF_MANAGEMENT_PID, JMX_REALM, ZOOKEEPER_REALM);
            realmsUpdated = true;
        }
    }

    public void unbind(CuratorFramework curator) {
        //We don't want to unset the realm just because the client is gone.
    }

    public BundleContext getBundleContext() {
        return bundleContext;
    }

    public void setBundleContext(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }
}

