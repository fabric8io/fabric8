package org.fusesource.fabric.zookeeper.bootstrap;

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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.UnknownHostException;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.fusesource.fabric.api.Constants;
import org.fusesource.fabric.api.CreateEnsembleOptions;
import org.fusesource.fabric.api.DataStoreRegistrationHandler;
import org.fusesource.fabric.api.RuntimeProperties;
import org.fusesource.fabric.api.jcip.ThreadSafe;
import org.fusesource.fabric.api.scr.AbstractComponent;
import org.fusesource.fabric.api.scr.ValidatingReference;
import org.fusesource.fabric.utils.HostUtils;
import org.fusesource.fabric.utils.Ports;
import org.fusesource.fabric.utils.SystemProperties;
import org.fusesource.fabric.zookeeper.ZkDefs;
import org.osgi.framework.BundleContext;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ThreadSafe
@Component(name = BootstrapConfiguration.COMPONENT_NAME, immediate = true)
@Service({ BootstrapConfiguration.class, RuntimeProperties.class })
public class BootstrapConfiguration extends AbstractComponent implements RuntimeProperties {

    static final Logger LOGGER = LoggerFactory.getLogger(BootstrapConfiguration.class);

    public static final String COMPONENT_NAME = "org.fusesource.fabric.zookeeper.configuration";

    @Reference(referenceInterface = ConfigurationAdmin.class)
    private final ValidatingReference<ConfigurationAdmin> configAdmin = new ValidatingReference<ConfigurationAdmin>();
    @Reference(referenceInterface = DataStoreRegistrationHandler.class)
    private final ValidatingReference<DataStoreRegistrationHandler> registrationHandler = new ValidatingReference<DataStoreRegistrationHandler>();

    private final Map<String, String> systemProperties = new ConcurrentHashMap<String, String>();

    private CreateEnsembleOptions options;
    private ComponentContext componentContext;

    @Activate
    @SuppressWarnings("unchecked")
    void activate(ComponentContext componentContext) throws Exception {
        this.componentContext = componentContext;

        String karafHome = getPropertyInternal(SystemProperties.KARAF_HOME, null);

        // [TODO] abstract access to karaf users.properties
        org.apache.felix.utils.properties.Properties userProps = null;
        try {
            userProps = new org.apache.felix.utils.properties.Properties(new File(karafHome + "/etc/users.properties"));
        } catch (IOException e) {
            LOGGER.warn("Failed to load users from etc/users.properties. No users will be imported.", e);
        }

        options = CreateEnsembleOptions.builder().fromSystemProperties().users(userProps).build();
        if (options.isEnsembleStart()) {
            String connectionUrl = getConnectionUrl(options);
            registrationHandler.get().setRegistrationCallback(new DataStoreBootstrapTemplate(this, connectionUrl, options));

            createOrUpdateDataStoreConfig(options);
            createZooKeeeperServerConfig(options);
            createZooKeeeperClientConfig(connectionUrl, options);

            setPropertyInternal(CreateEnsembleOptions.ENSEMBLE_AUTOSTART, Boolean.FALSE.toString());
            File file = new File(karafHome + "/etc/system.properties");
            org.apache.felix.utils.properties.Properties props = new org.apache.felix.utils.properties.Properties(file);
            props.put(CreateEnsembleOptions.ENSEMBLE_AUTOSTART, Boolean.FALSE.toString());
            props.save();
        }

        activateComponent();
    }

    @Deactivate
    void deactivate() {
        deactivateComponent();
    }

    @Override
    public String getProperty(String key) {
        assertValid();
        return getPropertyInternal(key, null);
    }

    @Override
    public String getProperty(String key, String defaultValue) {
        assertValid();
        return getPropertyInternal(key, defaultValue);
    }

    @Override
    public void setProperty(String key, String value) {
        assertValid();
        setPropertyInternal(key, value);
    }

    @Override
    public void removeProperty(String key) {
        assertValid();
        systemProperties.remove(key);
    }

    private String getPropertyInternal(String key, String defaultValue) {
        String result = systemProperties.get(key);
        if (result == null) {
            BundleContext syscontext = componentContext.getBundleContext();
            result = syscontext.getProperty(key);
        }
        return result != null ? result : defaultValue;
    }

    private void setPropertyInternal(String key, String value) {
        if (value != null) {
            systemProperties.put(key, value);
        }
    }

    public CreateEnsembleOptions getBootstrapOptions() {
        assertValid();
        return options;
    }

    public String getConnectionUrl(CreateEnsembleOptions options) throws UnknownHostException {
        int zooKeeperServerConnectionPort = options.getZooKeeperServerConnectionPort();
        String connectionUrl = getConnectionAddress() + ":" + zooKeeperServerConnectionPort;
        return connectionUrl;
    }

    public void createOrUpdateDataStoreConfig(CreateEnsembleOptions options) throws IOException {
        Configuration config = configAdmin.get().getConfiguration(Constants.DATASTORE_TYPE_PID, null);
        Dictionary<String, Object> properties = config.getProperties();
        if (properties == null || properties.isEmpty()) {
            boolean updateConfig = false;
            properties = new Hashtable<String, Object>();
            Map<String, String> dataStoreProperties = options.getDataStoreProperties();
            for (Map.Entry<String, String> entry : dataStoreProperties.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();
                Object oldval = properties.put(key, value);
                updateConfig = updateConfig || !value.equals(oldval);
            }
            if (updateConfig) {
                config.update(properties);
            }
        }
    }

    /**
     * Creates ZooKeeper server configuration
     */
    public void createZooKeeeperServerConfig(CreateEnsembleOptions options) throws IOException {
        int serverPort = Ports.mapPortToRange(options.getZooKeeperServerPort(), options.getMinimumPort(), options.getMaximumPort());
        String serverHost = options.getBindAddress();
        Dictionary<String, Object> properties = new Hashtable<String, Object>();
        if (options.isAutoImportEnabled()) {
            loadPropertiesFrom(properties, options.getImportPath() + "/fabric/configs/versions/1.0/profiles/default/org.fusesource.fabric.zookeeper.server.properties");
        }
        properties.put("tickTime", "2000");
        properties.put("initLimit", "10");
        properties.put("syncLimit", "5");
        properties.put("dataDir", "data/zookeeper/0000");
        properties.put("clientPort", Integer.toString(serverPort));
        properties.put("clientPortAddress", serverHost);
        properties.put("fabric.zookeeper.pid", "org.fusesource.fabric.zookeeper.server-0000");
        Configuration config = configAdmin.get().createFactoryConfiguration(Constants.ZOOKEEPER_SERVER_PID, null);
        config.update(properties);
    }

    /**
     * Creates ZooKeeper client configuration.
     */
    public void createZooKeeeperClientConfig(String connectionUrl, CreateEnsembleOptions options) throws IOException {
        Dictionary<String, Object> properties = new Hashtable<String, Object>();
        if (options.isAutoImportEnabled()) {
            loadPropertiesFrom(properties, options.getImportPath() + "/fabric/configs/versions/1.0/profiles/default/org.fusesource.fabric.zookeeper.properties");
        }
        properties.put("zookeeper.url", connectionUrl);
        properties
                .put("zookeeper.timeout", System.getProperties().containsKey("zookeeper.timeout") ? System.getProperties().getProperty("zookeeper.timeout") : "30000");
        properties.put("fabric.zookeeper.pid", Constants.ZOOKEEPER_CLIENT_PID);
        properties.put("zookeeper.password", options.getZookeeperPassword());
        Configuration config = configAdmin.get().getConfiguration(Constants.ZOOKEEPER_CLIENT_PID, null);
        config.update(properties);
    }

    private String getConnectionAddress() throws UnknownHostException {
        String resolver = getPropertyInternal(ZkDefs.LOCAL_RESOLVER_PROPERTY, getPropertyInternal(ZkDefs.GLOBAL_RESOLVER_PROPERTY, ZkDefs.LOCAL_HOSTNAME));
        if (resolver.equals(ZkDefs.LOCAL_HOSTNAME)) {
            return HostUtils.getLocalHostName();
        } else if (resolver.equals(ZkDefs.LOCAL_IP)) {
            return HostUtils.getLocalIp();
        } else if (resolver.equals(ZkDefs.MANUAL_IP) && getPropertyInternal(ZkDefs.MANUAL_IP, null) != null) {
            return getPropertyInternal(ZkDefs.MANUAL_IP, null);
        } else
            return HostUtils.getLocalHostName();
    }

    private void loadPropertiesFrom(Dictionary<String, Object> dictionary, String from) {
        InputStream is = null;
        Properties properties = new Properties();
        try {
            is = new FileInputStream(from);
            properties.load(is);
            for (String key : properties.stringPropertyNames()) {
                dictionary.put(key, properties.get(key));
            }
        } catch (Exception e) {
            // Ignore
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (Exception e) {
                    // Ignore
                }
            }
        }
    }

    void bindConfigAdmin(ConfigurationAdmin service) {
        this.configAdmin.bind(service);
    }

    void unbindConfigAdmin(ConfigurationAdmin service) {
        this.configAdmin.unbind(service);
    }

    void bindRegistrationHandler(DataStoreRegistrationHandler service) {
        this.registrationHandler.bind(service);
    }

    void unbindRegistrationHandler(DataStoreRegistrationHandler service) {
        this.registrationHandler.unbind(service);
    }
}
