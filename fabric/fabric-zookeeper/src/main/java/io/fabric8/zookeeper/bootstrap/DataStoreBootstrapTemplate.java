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

import io.fabric8.api.CreateEnsembleOptions;
import io.fabric8.api.DataStore;
import io.fabric8.api.DataStoreTemplate;
import io.fabric8.api.FabricException;
import io.fabric8.api.Profile;
import io.fabric8.api.ProfileBuilder;
import io.fabric8.api.ProfileRegistry;
import io.fabric8.utils.DataStoreUtils;
import io.fabric8.utils.PasswordEncoder;
import io.fabric8.utils.Ports;
import io.fabric8.zookeeper.ZkPath;
import io.fabric8.zookeeper.bootstrap.BootstrapConfiguration.DataStoreOptions;
import io.fabric8.zookeeper.curator.CuratorACLManager;
import io.fabric8.zookeeper.utils.ZooKeeperUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
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
import org.jboss.gravia.utils.IllegalStateAssertion;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DataStoreBootstrapTemplate implements DataStoreTemplate {

    private final String connectionUrl;
    private final CreateEnsembleOptions options;
    private final String name;
    private final File homeDir;
    private final CuratorACLManager aclManager = new CuratorACLManager();

    private static final Logger LOGGER = LoggerFactory.getLogger(DataStoreBootstrapTemplate.class);
    
    public DataStoreBootstrapTemplate(DataStoreOptions bootOptions) {
        this.name = bootOptions.getContainerId();
        this.homeDir = bootOptions.getHomeDir();
        this.connectionUrl = bootOptions.getConnectionUrl();
        this.options = bootOptions.getCreateOptions();
    }

    @Override
    public void doWith(ProfileRegistry profileRegistry, DataStore dataStore) {
        String versionId = options.getVersion();
        int minimumPort = options.getMinimumPort();
        int maximumPort = options.getMaximumPort();
        String zooKeeperServerHost = options.getBindAddress();
        int zooKeeperServerPort = options.getZooKeeperServerPort();
        int zooKeeperServerConnectionPort = options.getZooKeeperServerConnectionPort();
        int mappedPort = Ports.mapPortToRange(zooKeeperServerPort, minimumPort, maximumPort);
        CuratorFramework curator = null;

        try {
            curator = createCuratorFramework(connectionUrl, options);
            curator.start();
            curator.getZookeeperClient().blockUntilConnectedOrTimedOut();

            // Make the import path absolute
            File importPath = new File(options.getImportPath());
            if (!importPath.isAbsolute()) {
                importPath = new File(homeDir, options.getImportPath());
            }

            // Import data into the DataStore
            if (options.isAutoImportEnabled()) {
                profileRegistry.importFromFileSystem(importPath.getAbsolutePath());
            }

            // set the fabric configuration
            ZooKeeperUtils.setData(curator, ZkPath.CONFIG_DEFAULT_VERSION.getPath(), versionId);

            // configure default profile
            Map<String, String> jaasConfig = Collections.singletonMap("encryption.enabled", "${zk:/fabric/authentication/encryption.enabled}");
            Profile defaultProfile = profileRegistry.getProfile(versionId, "default");
            if (defaultProfile == null) {
                ProfileBuilder builder = ProfileBuilder.Factory.create(versionId, "default");
                builder.addConfiguration("io.fabric8.jaas", jaasConfig);
                String createdId = profileRegistry.createProfile(builder.getProfile());
                defaultProfile = profileRegistry.getRequiredProfile(versionId, createdId);
            } else {
                ProfileBuilder builder = ProfileBuilder.Factory.createFrom(defaultProfile);
                builder.addConfiguration("io.fabric8.jaas", jaasConfig);
                String updatedId = profileRegistry.updateProfile(builder.getProfile());
                defaultProfile = profileRegistry.getRequiredProfile(versionId, updatedId);
            }
            String defaultProfileId = defaultProfile.getId();

            ZooKeeperUtils.setData(curator, ZkPath.CONFIG_ENSEMBLE_URL.getPath(), "${zk:" + name + "/ip}:" + zooKeeperServerConnectionPort);
            ZooKeeperUtils.setData(curator, ZkPath.CONFIG_ENSEMBLE_PASSWORD.getPath(), PasswordEncoder.encode(options.getZookeeperPassword()));

            Properties zkProps = new Properties();
            zkProps.setProperty("zookeeper.url", "${zk:" + ZkPath.CONFIG_ENSEMBLE_URL.getPath() + "}");
            zkProps.setProperty("zookeeper.password", "${zk:" + ZkPath.CONFIG_ENSEMBLE_PASSWORD.getPath() + "}");
            profileRegistry.setFileConfiguration(versionId, defaultProfileId, "io.fabric8.zookeeper.properties", DataStoreUtils.toBytes(zkProps));

            // configure the ensemble
            String profileId = "fabric-ensemble-0000";
            IllegalStateAssertion.assertFalse(profileRegistry.hasProfile(versionId, profileId), "Profile already exists: " + versionId + "/" + profileId);
            ProfileBuilder ensembleProfileBuilder = ProfileBuilder.Factory.create(versionId, profileId);
            String ensembleProfileId = profileRegistry.createProfile(ensembleProfileBuilder.getProfile());
            profileRegistry.setProfileAttribute(versionId, ensembleProfileId, "abstract", "true");
            profileRegistry.setProfileAttribute(versionId, ensembleProfileId, "hidden", "true");

            Properties ensembleProps = new Properties();
            ensembleProps.put("tickTime", String.valueOf(options.getZooKeeperServerTickTime()));
            ensembleProps.put("initLimit", String.valueOf(options.getZooKeeperServerInitLimit()));
            ensembleProps.put("syncLimit", String.valueOf(options.getZooKeeperServerSyncLimit()));
            ensembleProps.put("dataDir", options.getZooKeeperServerDataDir() + File.separator + "0000");

            loadPropertiesFrom(ensembleProps, importPath + "/fabric/profiles/default.profile/io.fabric8.zookeeper.server.properties");
            profileRegistry.setFileConfiguration(versionId, ensembleProfileId, "io.fabric8.zookeeper.server-0000.properties", DataStoreUtils.toBytes(ensembleProps));

            // configure this server in the ensemble
            profileId = "fabric-ensemble-0000-1";
            IllegalStateAssertion.assertFalse(profileRegistry.hasProfile(versionId, profileId), "Profile already exists: " + versionId + "/" + profileId);
            ProfileBuilder serverProfileBuilder = ProfileBuilder.Factory.create(versionId, profileId);
            String ensembleServerProfileId = profileRegistry.createProfile(serverProfileBuilder.getProfile());
            profileRegistry.setProfileAttribute(versionId, ensembleServerProfileId, "hidden", "true");
            profileRegistry.setProfileAttribute(versionId, ensembleServerProfileId, "parents", ensembleProfileId);
            Properties serverProps = new Properties();
            serverProps.put("clientPort", String.valueOf(mappedPort));
            serverProps.put("clientPortAddress", zooKeeperServerHost);
            profileRegistry.setFileConfiguration(versionId, ensembleServerProfileId, "io.fabric8.zookeeper.server-0000.properties", DataStoreUtils.toBytes(serverProps));

            ZooKeeperUtils.setData(curator, ZkPath.CONFIG_ENSEMBLES.getPath(), "0000");
            ZooKeeperUtils.setData(curator, ZkPath.CONFIG_ENSEMBLE.getPath("0000"), name);

            // configure fabric profile
            Profile fabricProfile = profileRegistry.getProfile(versionId, "fabric");
            if (fabricProfile == null) {
                ProfileBuilder builder = ProfileBuilder.Factory.create(versionId, "fabric");
                String createdId = profileRegistry.createProfile(builder.getProfile());
                fabricProfile = profileRegistry.getRequiredProfile(versionId, createdId);
            }
            String fabricProfileId = fabricProfile.getId();
            
            Properties agentProps = DataStoreUtils.toProperties(fabricProfile.getFileConfiguration("io.fabric8.agent.properties"));
            agentProps.put("feature.fabric-commands", "fabric-commands");
            profileRegistry.setFileConfiguration(versionId, "fabric", "io.fabric8.agent.properties", DataStoreUtils.toBytes(agentProps));

            ZooKeeperUtils.createDefault(curator, ZkPath.CONFIG_CONTAINER.getPath(name), versionId);

            StringBuilder profilesBuilder = new StringBuilder();
            Set<String> profiles = options.getProfiles();
            profilesBuilder.append("fabric").append(" ").append("fabric-ensemble-0000-1");
            for (String p : profiles) {
                profilesBuilder.append(" ").append(p);
            }
            if (!options.isAgentEnabled()) {
                profilesBuilder.append(" ").append("unmanaged");
            }

            ZooKeeperUtils.createDefault(curator, ZkPath.CONFIG_VERSIONS_CONTAINER.getPath(versionId, name), profilesBuilder.toString());

            // outside of the profile storage area, so we'll keep these in zk
            EncryptionSupport encryption = addUsersToZookeeper(curator, options.getUsers());
            ZooKeeperUtils.createDefault(curator, "/fabric/authentication/encryption.enabled", Boolean.valueOf(encryption != null).toString());
            ZooKeeperUtils.createDefault(curator, "/fabric/authentication/domain", "karaf");

            ZooKeeperUtils.createDefault(curator, ZkPath.AUTHENTICATION_CRYPT_ALGORITHM.getPath(), "PBEWithMD5AndDES");
            ZooKeeperUtils.createDefault(curator, ZkPath.AUTHENTICATION_CRYPT_PASSWORD.getPath(), PasswordEncoder.encode(options.getZookeeperPassword()));

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
        Pattern p = Pattern.compile("([^,]+),(.+)");
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
                String roles = m.group(2).trim();
                sb.append(user).append("=").append(password).append(",").append(roles).append("\n");
            }
        }
        String allUsers = sb.toString();
        ZooKeeperUtils.createDefault(curator, "/fabric/authentication/users", allUsers);

        return encryptionSupport;
    }

    @Override
    public String toString() {
        return "DataStoreBootstrapTemplate{" +
                "name='" + name + '\'' +
                ", connectionUrl='" + connectionUrl + '\'' +
                '}';
    }
}
