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
package io.fabric8.zookeeper.bootstrap;

import static io.fabric8.utils.Ports.mapPortToRange;
import static io.fabric8.zookeeper.utils.ZooKeeperUtils.createDefault;
import static io.fabric8.zookeeper.utils.ZooKeeperUtils.setData;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.RetryNTimes;
import org.apache.karaf.jaas.modules.Encryption;
import org.apache.karaf.jaas.modules.encryption.EncryptionSupport;
import io.fabric8.api.CreateEnsembleOptions;
import io.fabric8.api.DataStore;
import io.fabric8.api.DataStoreTemplate;
import io.fabric8.api.FabricException;
import io.fabric8.utils.PasswordEncoder;
import io.fabric8.utils.DataStoreUtils;
import io.fabric8.zookeeper.ZkPath;
import io.fabric8.zookeeper.curator.CuratorACLManager;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DataStoreBootstrapTemplate implements DataStoreTemplate {

    private final String connectionUrl;
    private final CreateEnsembleOptions options;
    private final String name;
    private final String home;
    private final String version;
    private final CuratorACLManager aclManager = new CuratorACLManager();

    private static final Logger LOGGER = LoggerFactory.getLogger(DataStoreBootstrapTemplate.class);
    
    public DataStoreBootstrapTemplate(String name, String home, String connectionUrl, CreateEnsembleOptions options) {
        this.name = name;
        this.home = home;
        this.connectionUrl = connectionUrl;
        this.options = options;
        this.version = options.getVersion();
    }

    @Override
    public String toString() {
        return "DataStoreBootstrapTemplate{" +
                "name='" + name + '\'' +
                ", connectionUrl='" + connectionUrl + '\'' +
                '}';
    }

    @Override
    public void doWith(DataStore dataStore) {
        int minimumPort = options.getMinimumPort();
        int maximumPort = options.getMaximumPort();
        String zooKeeperServerHost = options.getBindAddress();
        int zooKeeperServerPort = options.getZooKeeperServerPort();
        int zooKeeperServerConnectionPort = options.getZooKeeperServerConnectionPort();
        int mappedPort = mapPortToRange(zooKeeperServerPort, minimumPort, maximumPort);
        CuratorFramework curator = null;

        try {
            curator = createCuratorFramework(connectionUrl, options);
            curator.start();
            curator.getZookeeperClient().blockUntilConnectedOrTimedOut();

            // Make the import path absolute
            File importPath = new File(options.getImportPath());
            if (!importPath.isAbsolute()) {
                importPath = new File(home, options.getImportPath());
            }

            // Import data into the DataStore
            if (options.isAutoImportEnabled()) {
                dataStore.importFromFileSystem(importPath.getAbsolutePath());
            }

            // set the fabric configuration
            setData(curator, ZkPath.CONFIG_DEFAULT_VERSION.getPath(), version);

            // configure default profile
            String defaultProfile = dataStore.getProfile(version, "default", true);


            setData(curator, ZkPath.CONFIG_ENSEMBLE_URL.getPath(), "${zk:" + name + "/ip}:" + zooKeeperServerConnectionPort);
            setData(curator, ZkPath.CONFIG_ENSEMBLE_PASSWORD.getPath(), PasswordEncoder.encode(options.getZookeeperPassword()));

            Properties zkProps = new Properties();
            zkProps.setProperty("zookeeper.url", "${zk:" + ZkPath.CONFIG_ENSEMBLE_URL.getPath() + "}");
            zkProps.setProperty("zookeeper.password", "${zk:" + ZkPath.CONFIG_ENSEMBLE_PASSWORD.getPath() + "}");
            dataStore.setFileConfiguration(version, defaultProfile, "io.fabric8.zookeeper.properties", DataStoreUtils.toBytes(zkProps));

            // configure the ensemble
            String ensembleProfile = dataStore.getProfile(version, "fabric-ensemble-0000", true);
            dataStore.setProfileAttribute(version, ensembleProfile, "abstract", "true");
            dataStore.setProfileAttribute(version, ensembleProfile, "hidden", "true");

            Properties ensembleProps = new Properties();
            ensembleProps.put("tickTime", String.valueOf(options.getZooKeeperServerTickTime()));
            ensembleProps.put("initLimit", String.valueOf(options.getZooKeeperServerInitLimit()));
            ensembleProps.put("syncLimit", String.valueOf(options.getZooKeeperServerSyncLimit()));
            ensembleProps.put("dataDir", options.getZooKeeperServerDataDir() + File.separator + "0000");

            loadPropertiesFrom(ensembleProps, importPath + "/fabric/configs/versions/1.0/profiles/default/io.fabric8.zookeeper.server.properties");
            dataStore.setFileConfiguration(version, ensembleProfile, "io.fabric8.zookeeper.server-0000.properties", DataStoreUtils.toBytes(ensembleProps));

            // configure this server in the ensemble
            String ensembleServerProfile = dataStore.getProfile(version, "fabric-ensemble-0000-1", true);
            dataStore.setProfileAttribute(version, ensembleServerProfile, "hidden", "true");
            dataStore.setProfileAttribute(version, ensembleServerProfile, "parents", ensembleProfile);
            Properties serverProps = new Properties();
            serverProps.put("clientPort", String.valueOf(mappedPort));
            serverProps.put("clientPortAddress", zooKeeperServerHost);
            dataStore.setFileConfiguration(version, ensembleServerProfile, "io.fabric8.zookeeper.server-0000.properties",
                    DataStoreUtils.toBytes(serverProps));

            setData(curator, ZkPath.CONFIG_ENSEMBLES.getPath(), "0000");
            setData(curator, ZkPath.CONFIG_ENSEMBLE.getPath("0000"), name);

            // configure fabric profile
            String fabricProfile = dataStore.getProfile(version, "fabric", true);
            Properties agentProps = DataStoreUtils.toProperties(dataStore.getFileConfiguration(version, fabricProfile, "io.fabric8.agent.properties"));
            agentProps.put("feature.fabric-commands", "fabric-commands");
            dataStore.setFileConfiguration(version, "fabric", "io.fabric8.agent.properties", DataStoreUtils.toBytes(agentProps));

            createDefault(curator, ZkPath.CONFIG_CONTAINER.getPath(name), version);

            StringBuilder profilesBuilder = new StringBuilder();
            Set<String> profiles = options.getProfiles();
            profilesBuilder.append("fabric").append(" ").append("fabric-ensemble-0000-1");
            for (String p : profiles) {
                profilesBuilder.append(" ").append(p);
            }
            if (!options.isAgentEnabled()) {
                profilesBuilder.append(" ").append("unmanaged");
            }

            createDefault(curator, ZkPath.CONFIG_VERSIONS_CONTAINER.getPath(version, name), profilesBuilder.toString());

            // add auth
            Map<String, String> configs = new HashMap<String, String>();
            configs.put("encryption.enabled", "${zk:/fabric/authentication/encryption.enabled}");
            dataStore.setConfiguration(version, defaultProfile, "io.fabric8.jaas", configs);

            // outside of the profile storage area, so we'll keep these in zk
            EncryptionSupport encryption = addUsersToZookeeper(curator, options.getUsers());
            createDefault(curator, "/fabric/authentication/encryption.enabled", Boolean.valueOf(encryption != null).toString());
            createDefault(curator, "/fabric/authentication/domain", "karaf");

            createDefault(curator, ZkPath.AUTHENTICATION_CRYPT_ALGORITHM.getPath(), "PBEWithMD5AndDES");
            createDefault(curator, ZkPath.AUTHENTICATION_CRYPT_PASSWORD.getPath(), PasswordEncoder.encode(options.getZookeeperPassword()));

            //Ensure ACLs are from the beggining of the fabric tree.
            aclManager.fixAcl(curator, "/fabric", true);
        } catch (Exception ex) {
            throw new FabricException("Unable to create bootstrap configuration", ex);
        } finally {
            curator.close();
        }
    }

    /**
     * Creates ZooKeeper client configuration.
     */
    private CuratorFramework createCuratorFramework(String connectionUrl, CreateEnsembleOptions options) throws IOException {
        return CuratorFrameworkFactory.builder().connectString(connectionUrl).connectionTimeoutMs(15000).sessionTimeoutMs(60000).aclProvider(aclManager)
                .authorization("digest", ("fabric:" + options.getZookeeperPassword()).getBytes()).retryPolicy(new RetryNTimes(3, 500)).build();
    }

    private void loadPropertiesFrom(Properties targetProperties, String from) {
        InputStream is = null;
        Properties properties = new Properties();
        try {
            is = new FileInputStream(from);
            properties.load(is);
            for (String key : properties.stringPropertyNames()) {
                targetProperties.put(key, properties.get(key));
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

    /**
     * Adds users to the Zookeeper registry.
     */
    private EncryptionSupport addUsersToZookeeper(CuratorFramework curator, Map<String, String> users) throws Exception {
        Pattern p = Pattern.compile("(.+),(.+)");
        Map<String, Object> options = new HashMap<String, Object>();
        options.put("encryption.prefix", "{CRYPT}");
        options.put("encryption.suffix", "{CRYPT}");
        options.put("encryption.enabled", "true");
        options.put("encryption.algorithm", "MD5");
        options.put("encryption.encoding", "hexadecimal");

        Encryption encryption = null;
        EncryptionSupport encryptionSupport = null;
        Bundle bundle = FrameworkUtil.getBundle(getClass());
        if (bundle != null) {
            options.put(BundleContext.class.getName(), bundle.getBundleContext());
            try {
            encryptionSupport = new EncryptionSupport(options);
            encryption = encryptionSupport.getEncryption();
            } catch (Exception e) {
                //Ignore
            }
        }

        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : users.entrySet()) {
            String user = entry.getKey();
            Matcher m = p.matcher(entry.getValue());
            if (m.matches() && m.groupCount() >= 2) {
                String password = m.group(1).trim();
                if (encryptionSupport != null && encryption != null) {
                    if (!password.startsWith(encryptionSupport.getEncryptionPrefix()) || !password.endsWith(encryptionSupport.getEncryptionSuffix())) {
                        password = encryptionSupport.getEncryptionPrefix() + encryption.encryptPassword(m.group(1)).trim() + encryptionSupport.getEncryptionSuffix();
                    }
                }
                String role = m.group(2).trim();
                sb.append(user).append("=").append(password).append(",").append(role).append("\n");
            }
        }
        String allUsers = sb.toString();
        createDefault(curator, "/fabric/authentication/users", allUsers);

        return encryptionSupport;
    }
}
