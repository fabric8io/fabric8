package io.fabric8.zookeeper.bootstrap;

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
import java.util.Collections;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import io.fabric8.api.ContainerOptions;
import io.fabric8.api.scr.Configurer;
import io.fabric8.utils.PasswordEncoder;
import io.fabric8.utils.Strings;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;

import io.fabric8.api.Constants;
import io.fabric8.api.CreateEnsembleOptions;
import io.fabric8.api.DataStoreRegistrationHandler;
import io.fabric8.api.jcip.ThreadSafe;
import io.fabric8.api.scr.AbstractComponent;
import io.fabric8.api.scr.ValidatingReference;
import io.fabric8.utils.HostUtils;
import io.fabric8.utils.Ports;
import io.fabric8.zookeeper.ZkDefs;

import org.osgi.framework.BundleContext;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ThreadSafe
@Component(name = BootstrapConfiguration.COMPONENT_NAME, label = "Fabric8 Bootstrap Configuration", immediate = true, metatype = false)
@Service(BootstrapConfiguration.class)
public class BootstrapConfiguration extends AbstractComponent {

    static final Logger LOGGER = LoggerFactory.getLogger(BootstrapConfiguration.class);

    public static final String ENSEMBLE_MARKER = "ensemble-created.properties";
    public static final String COMPONENT_NAME = "io.fabric8.zookeeper.configuration";

    @Reference
    private Configurer configurer;
    @Reference(referenceInterface = ConfigurationAdmin.class)
    private final ValidatingReference<ConfigurationAdmin> configAdmin = new ValidatingReference<ConfigurationAdmin>();
    @Reference(referenceInterface = DataStoreRegistrationHandler.class)
    private final ValidatingReference<DataStoreRegistrationHandler> registrationHandler = new ValidatingReference<DataStoreRegistrationHandler>();

    private CreateEnsembleOptions options;

    @Property(name = "ensemble.auto.start", label = "Ensemble Auto Start", description = "Flag to automatically start a zookeeper ensemble", value = "${ensemble.auto.start}")
    private boolean ensembleAutoStart;

    @Property(name = "agent.auto.start", label = "Agent Auto Start", description = "Flag to automatically start the provisioning agent", value = "${agent.auto.start}")
    private boolean agentAutoStart = true;

    @Property(name = "bind.address", label = "Bind Address", description = "The Bind Address", value = "${bind.address}")
    private String bindAddress = "0.0.0.0";

    @Property(name = "zookeeper.password", label = "ZooKeeper Password", description = "The zookeeper password", value = "${zookeeper.password}")
    private String zookeeperPassword = PasswordEncoder.encode(CreateEnsembleOptions.generatePassword());

    @Property(name = "zookeeper.server.port", label = "ZooKeeper Server Port", description = "The zookeeper server binding port", value = "${zookeeper.server.port}")
    private int zookeeperServerPort = 2181;

    @Property(name = "zookeeper.server.connection.port", label = "ZooKeeper Client Port", description = "The zookeeper server connection port", value = "${zookeeper.server.connection.port}")
    private int zookeeperServerConnectionPort = 2181;

    @Property(name = "profiles.auto.import", label = "Auto Import Enabled", description = "Flag to automatically import the default profiles", value = "${profiles.auto.import}")
    private boolean profilesAutoImport = true;

    @Property(name = "profiles.auto.import.path", label = "Auto Import Enabled", description = "Flag to automatically import the default profiles", value = "${profiles.auto.import.path}")
    private String profilesAutoImportPath = "fabric/import";

    @Property(name = "profiles", value = "${profiles}")
    private Set<String> profiles = Collections.emptySet();

    @Property(name = "version", value = "${version}")
    private String version = ContainerOptions.DEFAULT_VERSION;

    @Property(name = "resolver", label = "Global Resolver", description = "The global resolver", value = "${global.resolver}")
    private String resolver = "localhostname";

    @Property(name = "manualip", label = "Global Resolver", description = "The global resolver", value = "${manualip}")
    private String manualip;

    @Property(name = "name", label = "Container Name", description = "The name of the container", value = "${karaf.name}", propertyPrivate = true)
    private String name;
    @Property(name = "home", label = "Container Home", description = "The home directory of the container", value = "${karaf.home}", propertyPrivate = true)
    private String home;
    @Property(name = "zookeeper.url", label = "ZooKeeper URL", description = "The url to an existing zookeeper ensemble", value = "${zookeeper.url}", propertyPrivate = true)
    private String zookeeperUrl;

    private ComponentContext componentContext;

    @Activate
    void activate(ComponentContext componentContext, Map<String, ?> configuration) throws Exception {
        this.componentContext = componentContext;
        configurer.configure(configuration, this);

        org.apache.felix.utils.properties.Properties userProps = new org.apache.felix.utils.properties.Properties();
        // [TODO] abstract access to karaf users.properties
        try {
            userProps.load(new File(new File(home) , "etc" + File.separator + "users.properties"));
        } catch (IOException e) {
            LOGGER.warn("Failed to load users from etc/users.properties. No users will be imported.", e);
        }

        options = CreateEnsembleOptions.builder().bindAddress(bindAddress).agentEnabled(agentAutoStart).ensembleStart(ensembleAutoStart).zookeeperPassword(PasswordEncoder.decode(zookeeperPassword))
                .zooKeeperServerPort(zookeeperServerPort).zooKeeperServerConnectionPort(zookeeperServerConnectionPort).autoImportEnabled(profilesAutoImport)
                .importPath(profilesAutoImportPath).users(userProps).profiles(profiles).version(version).build();

        BundleContext bundleContext = componentContext.getBundleContext();
        boolean isCreated = checkCreated(bundleContext);

        if (!Strings.isNotBlank(zookeeperUrl) && !isCreated && options.isEnsembleStart()) {
            String connectionUrl = getConnectionUrl(options);
            registrationHandler.get().setRegistrationCallback(new DataStoreBootstrapTemplate(name, home, connectionUrl, options));

            createOrUpdateDataStoreConfig(options);
            createZooKeeeperServerConfig(options);
            createZooKeeeperClientConfig(connectionUrl, options);

            markCreated(bundleContext);
        }

        activateComponent();
    }

    @Deactivate
    void deactivate() {
        deactivateComponent();
    }

    public ComponentContext getComponentContext() {
        return componentContext;
    }

    private boolean checkCreated(BundleContext bundleContext) throws IOException {
        org.apache.felix.utils.properties.Properties props = new org.apache.felix.utils.properties.Properties(bundleContext.getDataFile(ENSEMBLE_MARKER));
        return props.containsKey("created");
    }

    private void markCreated(BundleContext bundleContext) throws IOException {
        org.apache.felix.utils.properties.Properties props = new org.apache.felix.utils.properties.Properties(bundleContext.getDataFile(ENSEMBLE_MARKER));
        props.put("created", "true");
        props.save();
    }

    public CreateEnsembleOptions getBootstrapOptions() {
        assertValid();
        return options;
    }

    public String getConnectionUrl(CreateEnsembleOptions options) throws UnknownHostException {
        int zooKeeperServerConnectionPort = options.getZooKeeperServerConnectionPort();
        String connectionUrl = getConnectionAddress(options) + ":" + zooKeeperServerConnectionPort;
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
            loadPropertiesFrom(properties, options.getImportPath() + "/fabric/configs/versions/1.0/profiles/default/io.fabric8.zookeeper.server.properties");
        }
        properties.put("tickTime", String.valueOf(options.getZooKeeperServerTickTime()));
        properties.put("initLimit", String.valueOf(options.getZooKeeperServerInitLimit()));
        properties.put("syncLimit", String.valueOf(options.getZooKeeperServerSyncLimit()));
        properties.put("dataDir", options.getZooKeeperServerDataDir() + File.separator + "0000");
        properties.put("clientPort", Integer.toString(serverPort));
        properties.put("clientPortAddress", serverHost);
        properties.put("fabric.zookeeper.pid", "io.fabric8.zookeeper.server-0000");
        Configuration config = configAdmin.get().createFactoryConfiguration(Constants.ZOOKEEPER_SERVER_PID, null);
        config.update(properties);
    }

    /**
     * Creates ZooKeeper client configuration.
     */
    public void createZooKeeeperClientConfig(String connectionUrl, CreateEnsembleOptions options) throws IOException {
        Dictionary<String, Object> properties = new Hashtable<String, Object>();
        if (options.isAutoImportEnabled()) {
            loadPropertiesFrom(properties, options.getImportPath() + "/fabric/configs/versions/1.0/profiles/default/io.fabric8.zookeeper.properties");
        }
        properties.put("zookeeper.url", connectionUrl);
        properties
                .put("zookeeper.timeout", System.getProperties().containsKey("zookeeper.timeout") ? System.getProperties().getProperty("zookeeper.timeout") : "30000");
        properties.put("fabric.zookeeper.pid", Constants.ZOOKEEPER_CLIENT_PID);
        properties.put("zookeeper.password", PasswordEncoder.encode(options.getZookeeperPassword()));
        Configuration config = configAdmin.get().getConfiguration(Constants.ZOOKEEPER_CLIENT_PID, null);
        config.update(properties);
    }

    private String getConnectionAddress(CreateEnsembleOptions options) throws UnknownHostException {
        String oResolver = Strings.isNotBlank(options.getResolver()) ? options.getResolver() : resolver;
        String oManualIp = Strings.isNotBlank(options.getManualIp()) ? options.getManualIp() : manualip;

        if (oResolver.equals(ZkDefs.LOCAL_HOSTNAME)) {
            return HostUtils.getLocalHostName();
        } else if (oResolver.equals(ZkDefs.LOCAL_IP)) {
            return HostUtils.getLocalIp();
        } else if (oResolver.equals(ZkDefs.MANUAL_IP) && (oManualIp != null && !oManualIp.isEmpty())) {
            return options.getManualIp();
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
