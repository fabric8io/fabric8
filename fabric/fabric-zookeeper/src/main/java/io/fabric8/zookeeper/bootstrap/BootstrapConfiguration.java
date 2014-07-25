/**
 *  Copyright 2005-2014 Red Hat, Inc.
 *
 *  Red Hat licenses this file to you under the Apache License, version
 *  2.0 (the "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 *  implied.  See the License for the specific language governing
 *  permissions and limitations under the License.
 */
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

import io.fabric8.api.Constants;
import io.fabric8.api.ContainerOptions;
import io.fabric8.api.CreateEnsembleOptions;
import io.fabric8.api.DataStoreTemplate;
import io.fabric8.api.RuntimeProperties;
import io.fabric8.api.ZkDefs;
import io.fabric8.api.jcip.ThreadSafe;
import io.fabric8.api.scr.AbstractComponent;
import io.fabric8.api.scr.Configurer;
import io.fabric8.api.scr.ValidatingReference;
import io.fabric8.common.util.Strings;
import io.fabric8.utils.HostUtils;
import io.fabric8.utils.PasswordEncoder;
import io.fabric8.utils.Ports;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Modified;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.felix.utils.properties.Properties;
import org.osgi.framework.BundleContext;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ThreadSafe
@Component(name = BootstrapConfiguration.COMPONENT_NAME, configurationPid = BootstrapConfiguration.COMPONENT_PID, policy = ConfigurationPolicy.OPTIONAL, label = "Fabric8 Bootstrap Configuration", immediate = true, metatype = false)
@Service(BootstrapConfiguration.class)
public class BootstrapConfiguration extends AbstractComponent {

    static final Logger LOGGER = LoggerFactory.getLogger(BootstrapConfiguration.class);

    public static final String ENSEMBLE_MARKER = "ensemble-created.properties";
    public static final String COMPONENT_PID = "io.fabric8.bootstrap.configuration";
    public static final String COMPONENT_NAME = COMPONENT_PID;

    public static final String DEFAULT_ADMIN_USER = "admin";
    public static final String DEFAULT_ADMIN_ROLE = "admin";
    public static final String ROLE_DELIMITER = ",";

    @Reference
    private Configurer configurer;
    @Reference(referenceInterface = ConfigurationAdmin.class)
    private final ValidatingReference<ConfigurationAdmin> configAdmin = new ValidatingReference<ConfigurationAdmin>();
    @Reference(referenceInterface = RuntimeProperties.class, bind = "bindRuntimeProperties", unbind = "unbindRuntimeProperties")
    private final ValidatingReference<RuntimeProperties> runtimeProperties = new ValidatingReference<RuntimeProperties>();

    private CreateEnsembleOptions options;

    @Property(name = "ensemble.auto.start", label = "Ensemble Auto Start", description = "Flag to automatically start a zookeeper ensemble", value = "${ensemble.auto.start}")
    private boolean ensembleAutoStart;
    @Property(name = "agent.auto.start", label = "Agent Auto Start", description = "Flag to automatically start the provisioning agent", value = "${agent.auto.start}")
    private boolean agentAutoStart = true;
    @Property(name = "bind.address", label = "Bind Address", description = "The Bind Address", value = "${bind.address}")
    private String bindAddress = "0.0.0.0";
    @Property(name = "zookeeper.password", label = "ZooKeeper Password", description = "The zookeeper password", value = "${zookeeper.password}")
    private String zookeeperPassword = null;
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
    @Property(name = "local.resolver", label = "Resolver", description = "The container resolver", value = "${local.resolver}")
    private String localResolver;
    @Property(name = "global.resolver", label = "Global Resolver", description = "The global resolver", value = "${global.resolver}")
    private String globalResolver = "localhostname";
    @Property(name = "manualip", label = "Global Resolver", description = "The manally set ip", value = "${manualip}")
    private String manualip;
    @Property(name = "publichostname", label = "Public Hostname", description = "The public hostname", value = "${publichostname}")
    private String publichostname;
    @Property(name = "runtime.id", label = "Container Name", description = "The name of the container", value = "${runtime.id}", propertyPrivate = true)
    private String runtimeId;
    @Property(name = "homeDir", label = "Container Home", description = "The homeDir directory of the container", value = "${runtime.home}", propertyPrivate = true)
    private File homeDir;
    @Property(name = "confDir", label = "Container Conf", description = "The configuration directory of the container", value = "${runtime.conf}", propertyPrivate = true)
    private File confDir;
    @Property(name = "dataDir", label = "Container Data Dir", description = "The data directory of the container", value = "${runtime.data}", propertyPrivate = true)
    private File dataDir;
    @Property(name = "zookeeper.url", label = "ZooKeeper URL", description = "The url to an existing zookeeper ensemble", value = "${zookeeper.url}", propertyPrivate = true)
    private String zookeeperUrl;

    private ComponentContext componentContext;
    private Map<String, ?> configuration;

    @Activate
    void activate(ComponentContext componentContext, Map<String, ?> conf) throws Exception {
        this.componentContext = componentContext;
        configureInternal(conf);
        bootIfNeeded();
        activateComponent();
    }

    @Modified
    void modified(Map<String, ?> conf) throws Exception {
        configureInternal(conf);
    }

    @Deactivate
    void deactivate() {
        deactivateComponent();
    }

    void configureInternal(Map<String, ?> conf) throws Exception {
        configuration = configurer.configure(conf, this);

        if (Strings.isNullOrBlank(runtimeId)) {
            throw new IllegalArgumentException("Runtime id must not be null or empty.");
        }

        if (Strings.isNullOrBlank(localResolver)) {
            localResolver = globalResolver;
        }

        String decodedZookeeperPassword = null;

        Properties userProps = new Properties();
        // [TODO] abstract access to karaf users.properties
        try {
            userProps.load(new File(confDir , "users.properties"));
        } catch (IOException e) {
            LOGGER.warn("Failed to load users from etc/users.properties. No users will be imported.", e);
        }

        if (Strings.isNotBlank(zookeeperPassword)) {
            decodedZookeeperPassword = PasswordEncoder.decode(zookeeperPassword);
        } else if (userProps.containsKey(DEFAULT_ADMIN_USER)) {
            String passwordAndRole = userProps.getProperty(DEFAULT_ADMIN_USER).trim();
            decodedZookeeperPassword = passwordAndRole.substring(0, passwordAndRole.indexOf(ROLE_DELIMITER));
        } else {
            decodedZookeeperPassword = PasswordEncoder.encode(CreateEnsembleOptions.generatePassword());
        }

        if (userProps.isEmpty()) {
            userProps.put(DEFAULT_ADMIN_USER, decodedZookeeperPassword+ ROLE_DELIMITER + DEFAULT_ADMIN_ROLE);
        }

        options = CreateEnsembleOptions.builder().bindAddress(bindAddress).agentEnabled(agentAutoStart).ensembleStart(ensembleAutoStart).zookeeperPassword(decodedZookeeperPassword)
                .zooKeeperServerPort(zookeeperServerPort).zooKeeperServerConnectionPort(zookeeperServerConnectionPort).autoImportEnabled(profilesAutoImport)
                .importPath(profilesAutoImportPath).resolver(localResolver).globalResolver(globalResolver).users(userProps).profiles(profiles).version(version).build();
    }

    void bootIfNeeded() throws IOException {
        BundleContext bundleContext = componentContext.getBundleContext();
        boolean isCreated = checkCreated(bundleContext);

        if (!Strings.isNotBlank(zookeeperUrl) && !isCreated && options.isEnsembleStart()) {
            String connectionUrl = getConnectionUrl(options);
            DataStoreOptions bootOptions = new DataStoreOptions(runtimeId, homeDir, connectionUrl, options);
            runtimeProperties.get().putRuntimeAttribute(DataStoreTemplate.class, new DataStoreBootstrapTemplate(bootOptions));

            createOrUpdateDataStoreConfig(options);
            createZooKeeeperServerConfig(options);
            createZooKeeeperClientConfig(connectionUrl, options);

            markCreated(bundleContext);
        }
    }

    public ComponentContext getComponentContext() {
        return componentContext;
    }

    private boolean checkCreated(BundleContext bundleContext) throws IOException {
        org.apache.felix.utils.properties.Properties props = new org.apache.felix.utils.properties.Properties(new File(dataDir, ENSEMBLE_MARKER));
        return props.containsKey("created");
    }

    private void markCreated(BundleContext bundleContext) throws IOException {
        File marker = new File(dataDir, ENSEMBLE_MARKER);
        if (!marker.exists() && !marker.getParentFile().exists() && !marker.getParentFile().mkdirs()) {
            throw new IOException("Cannot create marker file");
        }
        org.apache.felix.utils.properties.Properties props = new org.apache.felix.utils.properties.Properties(marker);
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
            loadPropertiesFrom(properties, options.getImportPath() + "/fabric/profiles/default.profile/io.fabric8.zookeeper.server.properties");
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
            loadPropertiesFrom(properties, options.getImportPath() + "/fabric/profiles/default.profile/io.fabric8.zookeeper.properties");
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
        String oResolver = Strings.isNotBlank(options.getResolver()) ? options.getResolver() : localResolver;
        String oManualIp = Strings.isNotBlank(options.getManualIp()) ? options.getManualIp() : manualip;

        if (oResolver.equals(ZkDefs.LOCAL_HOSTNAME)) {
            return HostUtils.getLocalHostName();
        } else if (oResolver.equals(ZkDefs.LOCAL_IP)) {
            return HostUtils.getLocalIp();
        } else if (oResolver.equals(ZkDefs.PUBLIC_HOSTNAME) && (publichostname != null && !publichostname.isEmpty())) {
            return publichostname;
        } else if (oResolver.equals(ZkDefs.MANUAL_IP) && (oManualIp != null && !oManualIp.isEmpty())) {
            return oManualIp;
        } else {
            return HostUtils.getLocalHostName();
        }
    }

    private void loadPropertiesFrom(Dictionary<String, Object> dictionary, String from) {
        InputStream is = null;
        Properties properties = new Properties();
        try {
            is = new FileInputStream(from);
            properties.load(is);
            for (String key : properties.keySet()) {
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

    public String getLocalResolver() {
        return localResolver;
    }

    public String getGlobalResolver() {
        return globalResolver;
    }

    public String getManualip() {
        return manualip;
    }

    public String getVersion() {
        return version;
    }

    public Set<String> getProfiles() {
        return Collections.unmodifiableSet(profiles);
    }

    public String getBindAddress() {
        return bindAddress;
    }

    public Map<String, ?> getConfiguration() {
        return Collections.unmodifiableMap(configuration);
    }

    public static class DataStoreOptions {
        private final String containerId;
        private final File homeDir; 
        private final String connectionUrl;
        private final CreateEnsembleOptions options;
        public DataStoreOptions(String containerId, File homeDir, String connectionUrl, CreateEnsembleOptions options) {
            this.connectionUrl = connectionUrl;
            this.containerId = containerId;
            this.homeDir = homeDir;
            this.options = options;
        }
        public String getContainerId() {
            return containerId;
        }
        public File getHomeDir() {
            return homeDir;
        }
        public String getConnectionUrl() {
            return connectionUrl;
        }
        public CreateEnsembleOptions getCreateOptions() {
            return options;
        }
    }
    
    void bindConfigAdmin(ConfigurationAdmin service) {
        this.configAdmin.bind(service);
    }
    void unbindConfigAdmin(ConfigurationAdmin service) {
        this.configAdmin.unbind(service);
    }

    void bindRuntimeProperties(RuntimeProperties service) {
        this.runtimeProperties.bind(service);
    }
    void unbindRuntimeProperties(RuntimeProperties service) {
        this.runtimeProperties.unbind(service);
    }
}
