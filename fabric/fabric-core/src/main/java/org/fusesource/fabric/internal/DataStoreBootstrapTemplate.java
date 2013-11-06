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

import static org.fusesource.fabric.utils.Ports.mapPortToRange;
import static org.fusesource.fabric.zookeeper.utils.ZooKeeperUtils.createDefault;
import static org.fusesource.fabric.zookeeper.utils.ZooKeeperUtils.setData;

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
import org.fusesource.fabric.api.CreateEnsembleOptions;
import org.fusesource.fabric.api.DataStore;
import org.fusesource.fabric.api.DataStoreTemplate;
import org.fusesource.fabric.api.FabricException;
import org.fusesource.fabric.utils.SystemProperties;
import org.fusesource.fabric.zookeeper.ZkPath;
import org.fusesource.fabric.zookeeper.curator.CuratorACLManager;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;

public class DataStoreBootstrapTemplate implements DataStoreTemplate {

    private final String connectionUrl;
    private final CreateEnsembleOptions options;

    private final String karafName = System.getProperty(SystemProperties.KARAF_NAME);
    private final String version;
    private final CuratorACLManager aclManager = new CuratorACLManager();


    public DataStoreBootstrapTemplate(String connectionUrl, CreateEnsembleOptions options) {
        this.connectionUrl = connectionUrl;
        this.options = options;
        this.version = options.getVersion();
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

            // import data into the DataStore
            if (options.isAutoImportEnabled()) {
                dataStore.importFromFileSystem(options.getImportPath());
            }

            // set the fabric configuration
            setData(curator, ZkPath.CONFIG_DEFAULT_VERSION.getPath(), version);

            // configure default profile
            String defaultProfile = dataStore.getProfile(version, "default", true);

            setData(curator, ZkPath.CONFIG_ENSEMBLE_URL.getPath(), "${zk:" + karafName + "/ip}:" + zooKeeperServerConnectionPort);
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
            configs.put("encryption.enabled", "${zk:/fabric/authentication/encryption.enabled}");
            dataStore.setConfiguration(version, defaultProfile, "org.fusesource.fabric.jaas", configs);

            // outside of the profile storage area, so we'll keep these in zk
            EncryptionSupport encryption = addUsersToZookeeper(curator, options.getUsers());
            createDefault(curator, "/fabric/authentication/encryption.enabled", Boolean.valueOf(encryption != null).toString());
            createDefault(curator, "/fabric/authentication/domain", "karaf");

            createDefault(curator, ZkPath.AUTHENTICATION_CRYPT_ALGORITHM.getPath(), "PBEWithMD5AndDES");
            createDefault(curator, ZkPath.AUTHENTICATION_CRYPT_PASSWORD.getPath(), options.getZookeeperPassword());

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
        return CuratorFrameworkFactory.builder()
                .connectString(connectionUrl)
                .connectionTimeoutMs(15000)
                .sessionTimeoutMs(60000)
                .aclProvider(aclManager)
                .authorization("digest", ("fabric:" + options.getZookeeperPassword()).getBytes())
                .retryPolicy(new RetryNTimes(3, 500)).build();
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
        options.put("encryption.enabled", "true");
        options.put("encryption.algorithm", "MD5");
        options.put("encryption.encoding", "hexadecimal");

        Encryption encryption = null;
        EncryptionSupport encryptionSupport = null;
        Bundle bundle = FrameworkUtil.getBundle(getClass());
        if (bundle != null) {
            options.put(BundleContext.class.getName(), bundle.getBundleContext());
            encryptionSupport = new EncryptionSupport(options);
            encryption = encryptionSupport.getEncryption();
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
