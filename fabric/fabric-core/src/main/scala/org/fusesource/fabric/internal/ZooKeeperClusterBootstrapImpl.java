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
package org.fusesource.fabric.internal;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.RetryNTimes;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.karaf.jaas.modules.Encryption;
import org.apache.karaf.jaas.modules.encryption.EncryptionSupport;
import org.apache.zookeeper.KeeperException;
import org.fusesource.fabric.api.CreateEnsembleOptions;
import org.fusesource.fabric.api.FabricException;
import org.fusesource.fabric.api.ZooKeeperClusterBootstrap;
import org.fusesource.fabric.service.ZooKeeperDataStore;
import org.fusesource.fabric.utils.HostUtils;
import org.fusesource.fabric.utils.SystemProperties;
import org.fusesource.fabric.zookeeper.ZkDefs;
import org.fusesource.fabric.zookeeper.ZkPath;
import org.fusesource.fabric.zookeeper.utils.ZookeeperImportUtils;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.FrameworkUtil;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.fusesource.fabric.utils.Ports.mapPortToRange;
import static org.fusesource.fabric.zookeeper.utils.ZooKeeperUtils.createDefault;
import static org.fusesource.fabric.zookeeper.utils.ZooKeeperUtils.getStringData;
import static org.fusesource.fabric.zookeeper.utils.ZooKeeperUtils.setData;
import static org.fusesource.fabric.utils.BundleUtils.instalBundle;
import static org.fusesource.fabric.utils.BundleUtils.installOrStopBundle;

@Component(name = "org.fusesource.fabric.zookeeper.cluster.bootstrap",
           description = "Fabric ZooKeeper Cluster Bootstrap",
           immediate = true)
@Service(ZooKeeperClusterBootstrap.class)
public class ZooKeeperClusterBootstrapImpl  implements ZooKeeperClusterBootstrap {

    private static final Logger LOGGER = LoggerFactory.getLogger(ZooKeeperClusterBootstrapImpl.class);
    private final boolean ensembleAutoStart = Boolean.parseBoolean(System.getProperty(SystemProperties.ENSEMBLE_AUTOSTART));
    private final BundleContext bundleContext = FrameworkUtil.getBundle(getClass()).getBundleContext();

    @Reference(cardinality = org.apache.felix.scr.annotations.ReferenceCardinality.MANDATORY_UNARY)
	private ConfigurationAdmin configurationAdmin;

    @Activate
    public void init() {
        if (ensembleAutoStart) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    create();
                }
            }).start();

        }
    }

    public void create() {
        org.apache.felix.utils.properties.Properties userProps = null;

        try {
            userProps = new org.apache.felix.utils.properties.Properties(new File(System.getProperty("karaf.home") + "/etc/users.properties"));
        } catch (IOException e) {
            LOGGER.warn("Failed to load users from etc/users.properties. No users will be imported.", e);
        }
        CreateEnsembleOptions createOpts = CreateEnsembleOptions.builder().fromSystemProperties().users(userProps).build();
        create(createOpts);
    }

    public void create(CreateEnsembleOptions options) {
        try {

            Hashtable<String, Object> properties;
            String version = ZkDefs.DEFAULT_VERSION;
            String karafName = System.getProperty(SystemProperties.KARAF_NAME);
            int minimumPort = options.getMinimumPort();
            int maximumPort = options.getMaximumPort();
            String zooKeeperServerHost = options.getBindAddress();
            int zooKeeperServerPort = options.getZooKeeperServerPort();
            int mappedPort = mapPortToRange(zooKeeperServerPort, minimumPort, maximumPort);
			String connectionUrl = getConnectionAddress() + ":" + Integer.toString(mappedPort);


            // Create configuration
            createZooKeeeperServerConfig(zooKeeperServerHost, mappedPort, options);
            CuratorFramework curator = createCuratorFramework(connectionUrl, options);
            curator.start();
            curator.getZookeeperClient().blockUntilConnectedOrTimedOut();

            //Initialize a temporary DataStore
            ZooKeeperDataStore dataStore = new ZooKeeperDataStore();
            dataStore.setCurator(curator);
            dataStore.init();

            // Import data into zookeeper
            if (options.isAutoImportEnabled()) {
                ZookeeperImportUtils.importFromFileSystem(curator, options.getImportPath(), "/", null, null, false, false, false);
            }
            createDefault(curator, ZkPath.CONFIG_DEFAULT_VERSION.getPath(), version);

            // configure default profile
            String defaultProfile = dataStore.getProfile(version, "default", true);

            setData(curator, ZkPath.CONFIG_ENSEMBLE_URL.getPath(), "${zk:" + karafName + "/ip}:" + Integer.toString(mappedPort));
            setData(curator, ZkPath.CONFIG_ENSEMBLE_PASSWORD.getPath(), options.getZookeeperPassword());

            Properties zkProps = new Properties();
            zkProps.setProperty("zookeeper.url", "${zk:" + ZkPath.CONFIG_ENSEMBLE_URL.getPath() + "}");
            zkProps.setProperty("zookeeper.password", "${zk:" + ZkPath.CONFIG_ENSEMBLE_PASSWORD.getPath() + "}");
            dataStore.setFileConfiguration(version, defaultProfile, "org.fusesource.fabric.zookeeper.properties", DataStoreHelpers.toBytes(zkProps));

            // configure the ensemble
            String ensembleProfile = dataStore.getProfile(version, "fabric-ensemble-0000", true);
            dataStore.setProfileAttribute(version, ensembleProfile, "abstract", "true");
            dataStore.setProfileAttribute(version, ensembleProfile, "hidden", "true");

            Properties ensembleProps = new Properties();
            ensembleProps.put("tickTime", "2000");
            ensembleProps.put("initLimit", "10");
            ensembleProps.put("syncLimit", "5");
            ensembleProps.put("dataDir", "data/zookeeper/0000");

            loadPropertiesFrom(ensembleProps, options.getImportPath() + "/fabric/configs/versions/1.0/profiles/default/org.fusesource.fabric.zookeeper.server.properties");
            dataStore.setFileConfiguration(version, ensembleProfile, "org.fusesource.fabric.zookeeper.server-0000.properties", DataStoreHelpers.toBytes(ensembleProps));

            // configure this server in the ensemble
            String ensembleServerProfile = dataStore.getProfile(version, "fabric-ensemble-0000-1", true);
            dataStore.setProfileAttribute(version, ensembleServerProfile, "hidden", "true");
            dataStore.setProfileAttribute(version, ensembleServerProfile, "parents", ensembleProfile);
            Properties serverProps = new Properties();
            serverProps.put("clientPort", String.valueOf(mappedPort));
            serverProps.put("clientPortAddress", zooKeeperServerHost);
            dataStore.setFileConfiguration(version, ensembleServerProfile, "org.fusesource.fabric.zookeeper.server-0000.properties", DataStoreHelpers.toBytes(serverProps));

            setData(curator, ZkPath.CONFIG_ENSEMBLES.getPath(), "0000");
            setData(curator, ZkPath.CONFIG_ENSEMBLE.getPath("0000"), karafName);

            // configure fabric profile
            String fabricProfile = dataStore.getProfile(version, "fabric", true);
            Properties agentProps = DataStoreHelpers.toProperties(dataStore.getFileConfiguration(version, fabricProfile, "org.fusesource.fabric.agent.properties"));
            agentProps.put("feature.fabric-commands", "fabric-commands");
            dataStore.setFileConfiguration(version, "fabric", "org.fusesource.fabric.agent.properties", DataStoreHelpers.toBytes(agentProps));

            createDefault(curator, ZkPath.CONFIG_CONTAINER.getPath(karafName), version);


            StringBuilder profilesBuilder = new StringBuilder();
            Set<String> profiles = options.getProfiles();
            profilesBuilder.append("fabric").append(" ").append("fabric-ensemble-0000-1");
            for (String p : profiles) {
                profilesBuilder.append(" ").append(p);
            }

            createDefault(curator, ZkPath.CONFIG_VERSIONS_CONTAINER.getPath(version, karafName), profilesBuilder.toString());

            // add auth
            Map<String, String> configs = new HashMap<String, String>();
            configs.put("encryption.enabled", "${zk:/fabric/authentication/encryption.enabled}" );
            dataStore.setConfiguration(version, defaultProfile, "org.fusesource.fabric.jaas", configs);

            // outside of the profile storage area, so we'll keep these in zk
            createDefault(curator, "/fabric/authentication/encryption.enabled", "true");
            createDefault(curator, "/fabric/authentication/domain", "karaf");
            addUsersToZookeeper(curator, options.getUsers());

            createDefault(curator, ZkPath.AUTHENTICATION_CRYPT_ALGORITHM.getPath(), "PBEWithMD5AndDES");
            createDefault(curator, ZkPath.AUTHENTICATION_CRYPT_PASSWORD.getPath(), options.getZookeeperPassword());

            // Create the client configuration
            createZooKeeeperConfig(connectionUrl, options);
            startBundles(options);
		} catch (Exception e) {
			throw new FabricException("Unable to create zookeeper server configuration", e);
		}
	}

    public void clean() {
        try {
            Bundle bundleFabricZooKeeper = installOrStopBundle(bundleContext, "org.fusesource.fabric.fabric-zookeeper",
                    "mvn:org.fusesource.fabric/fabric-zookeeper/" + FabricConstants.FABRIC_VERSION);

            for (; ; ) {
                Configuration[] configs = configurationAdmin.listConfigurations("(|(service.factoryPid=org.fusesource.fabric.zookeeper.server)(service.pid=org.fusesource.fabric.zookeeper))");
                if (configs != null && configs.length > 0) {
                    for (Configuration config : configs) {
                        config.delete();
                    }
                    Thread.sleep(100);
                } else {
                    break;
                }
            }

            File zkDir = new File("data/zookeeper");
            if (zkDir.isDirectory()) {
                File newZkDir = new File("data/zookeeper." + System.currentTimeMillis());
                if (!zkDir.renameTo(newZkDir)) {
                    newZkDir = zkDir;
                }
                delete(newZkDir);
            }

            bundleFabricZooKeeper.start();
        } catch (Exception e) {
            throw new FabricException("Unable to delete zookeeper configuration", e);
        }
    }

    /**
     * Creates ZooKeeper server configuration
     * @param serverHost
     * @param serverPort
     * @param options
     * @throws IOException
     */
    private void createZooKeeeperServerConfig(String serverHost, int serverPort, CreateEnsembleOptions options) throws IOException {
        Configuration config = configurationAdmin.createFactoryConfiguration("org.fusesource.fabric.zookeeper.server");
        Hashtable properties = new Hashtable<String, Object>();
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
        config.setBundleLocation(null);
        config.update(properties);
    }

    /**
     * Creates ZooKeeper client configuration.
     * @param connectionUrl
     * @param options
     * @throws IOException
     */
    private void createZooKeeeperConfig(String connectionUrl, CreateEnsembleOptions options) throws IOException {
        Configuration config = configurationAdmin.getConfiguration("org.fusesource.fabric.zookeeper");
        Hashtable properties = new Hashtable<String, Object>();
        if (options.isAutoImportEnabled()) {
            loadPropertiesFrom(properties, options.getImportPath() + "/fabric/configs/versions/1.0/profiles/default/org.fusesource.fabric.zookeeper.properties");
        }
        properties.put("zookeeper.url", connectionUrl);
        properties.put("zookeeper.timeout", System.getProperties().containsKey("zookeeper.timeout") ? System.getProperties().getProperty("zookeeper.timeout") : "30000");
        properties.put("fabric.zookeeper.pid", "org.fusesource.fabric.zookeeper");
        properties.put("zookeeper.password", options.getZookeeperPassword());
        config.setBundleLocation(null);
        config.update(properties);
    }


    /**
     * Creates ZooKeeper client configuration.
     * @param connectionUrl
     * @param options
     * @throws IOException
     */
    private CuratorFramework createCuratorFramework(String connectionUrl, CreateEnsembleOptions options) throws IOException {
        return CuratorFrameworkFactory.builder()
                .connectString(connectionUrl)
                .connectionTimeoutMs(15000)
                .sessionTimeoutMs(60000)
                .authorization("digest",  ("fabric:" + options.getZookeeperPassword()).getBytes())
                .retryPolicy(new RetryNTimes(3,500)).build();
    }


    public void startBundles(CreateEnsembleOptions options) throws BundleException {
        // Install or stop the fabric-configadmin bridge
        Bundle bundleFabricAgent = instalBundle(bundleContext, "org.fusesource.fabric.fabric-agent",
                "mvn:org.fusesource.fabric/fabric-agent/" + FabricConstants.FABRIC_VERSION);
        Bundle bundleFabricConfigAdmin = instalBundle(bundleContext, "org.fusesource.fabric.fabric-configadmin",
                "mvn:org.fusesource.fabric/fabric-configadmin/" + FabricConstants.FABRIC_VERSION);
        Bundle bundleFabricJaas = instalBundle(bundleContext, "org.fusesource.fabric.fabric-jaas  ",
                "mvn:org.fusesource.fabric/fabric-jaas/" + FabricConstants.FABRIC_VERSION);
        Bundle bundleFabricCommands = instalBundle(bundleContext, "org.fusesource.fabric.fabric-commands  ",
                "mvn:org.fusesource.fabric/fabric-commands/" + FabricConstants.FABRIC_VERSION);

        bundleFabricJaas.start();
        bundleFabricCommands.start();
        bundleFabricConfigAdmin.start();
        //Check if the agent is configured to auto start.
        if (options.isAgentEnabled()) {
            bundleFabricAgent.start();
        }
    }


    private void loadPropertiesFrom(Hashtable hashtable, String from) {
        InputStream is = null;
        Properties properties = new Properties();
        try {
            is = new FileInputStream(from);
            properties.load(is);
            for (String key : properties.stringPropertyNames()) {
                hashtable.put(key, properties.get(key));
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

    private static void delete(File dir) {
        if (dir.isDirectory()) {
            for (File child : dir.listFiles()) {
                delete(child);
            }
        }
        if (dir.exists()) {
            dir.delete();
        }
    }
    /**
     * Adds users to the Zookeeper registry.
     *
     * @param curator
     * @param users
     * @throws KeeperException
     * @throws InterruptedException
     */
    private void addUsersToZookeeper(CuratorFramework curator, Map<String, String> users) throws Exception {
        Pattern p = Pattern.compile("(.+),(.+)");
        Map<String, Object> options = new HashMap<String, Object>();
        options.put("encryption.prefix", "{CRYPT}");
        options.put("encryption.suffix", "{CRYPT}");
        options.put("encryption.enabled", "true");
        options.put("encryption.enabled", "true");
        options.put("encryption.algorithm", "MD5");
        options.put("encryption.encoding", "hexadecimal");
        options.put(BundleContext.class.getName(), FrameworkUtil.getBundle(getClass()).getBundleContext());
        EncryptionSupport encryptionSupport = new EncryptionSupport(options);
        Encryption encryption = encryptionSupport.getEncryption();

        StringBuilder sb = new StringBuilder();

        for (Map.Entry<String, String> entry : users.entrySet()) {
            String user = entry.getKey();
            Matcher m = p.matcher(entry.getValue());
            if (m.matches() && m.groupCount() >= 2) {
                String password = m.group(1).trim();
                if (!password.startsWith(encryptionSupport.getEncryptionPrefix()) || !password.endsWith(encryptionSupport.getEncryptionSuffix())) {
                    password = encryptionSupport.getEncryptionPrefix() + encryption.encryptPassword(m.group(1)).trim() + encryptionSupport.getEncryptionSuffix();
                }
                String role = m.group(2).trim();
                sb.append(user).append("=").append(password).append(",").append(role).append("\n");
            }
        }
        String allUsers = sb.toString();
        createDefault(curator, "/fabric/authentication/users", allUsers);
    }

    private static String getConnectionAddress() throws UnknownHostException {
        String resolver = System.getProperty(ZkDefs.LOCAL_RESOLVER_PROPERTY, System.getProperty(ZkDefs.GLOBAL_RESOLVER_PROPERTY, ZkDefs.LOCAL_HOSTNAME));
        if (resolver.equals(ZkDefs.LOCAL_HOSTNAME)) {
            return HostUtils.getLocalHostName();
        } else if (resolver.equals(ZkDefs.LOCAL_IP)) {
            return HostUtils.getLocalIp();
        } else if (resolver.equals(ZkDefs.MANUAL_IP) && System.getProperty(ZkDefs.MANUAL_IP) != null) {
            return System.getProperty(ZkDefs.MANUAL_IP);
        }  else return HostUtils.getLocalHostName();
    }

    private static String toString(Properties source) throws IOException {
        StringWriter writer = new StringWriter();
        source.store(writer, null);
        return writer.toString();
    }

    public static Properties getProperties(CuratorFramework client, String file, Properties defaultValue) throws Exception {
        try {
            String v = getStringData(client, file);
            if (v != null) {
                return DataStoreHelpers.toProperties(v);
            } else {
                return defaultValue;
            }
        } catch (KeeperException.NoNodeException e) {
            return defaultValue;
        }
    }

    public ConfigurationAdmin getConfigurationAdmin() {
        return configurationAdmin;
    }

    public void setConfigurationAdmin(ConfigurationAdmin configurationAdmin) {
        this.configurationAdmin = configurationAdmin;
    }
}
