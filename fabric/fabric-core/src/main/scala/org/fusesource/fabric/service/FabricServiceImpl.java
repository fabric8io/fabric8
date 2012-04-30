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
package org.fusesource.fabric.service;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectInstance;
import javax.management.ObjectName;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooDefs;
import org.fusesource.fabric.api.Container;
import org.fusesource.fabric.api.ContainerProvider;
import org.fusesource.fabric.api.CreateContainerBasicMetadata;
import org.fusesource.fabric.api.CreateContainerMetadata;
import org.fusesource.fabric.api.CreateContainerOptions;
import org.fusesource.fabric.api.FabricException;
import org.fusesource.fabric.api.FabricService;
import org.fusesource.fabric.api.Profile;
import org.fusesource.fabric.api.Version;
import org.fusesource.fabric.internal.ContainerImpl;
import org.fusesource.fabric.internal.ProfileImpl;
import org.fusesource.fabric.internal.VersionImpl;
import org.fusesource.fabric.utils.Base64Encoder;
import org.fusesource.fabric.zookeeper.utils.ZooKeeperUtils;
import org.fusesource.fabric.zookeeper.ZkDefs;
import org.fusesource.fabric.zookeeper.ZkPath;
import org.linkedin.zookeeper.client.IZKClient;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.fusesource.fabric.zookeeper.ZkPath.CONTAINER_PARENT;

public class FabricServiceImpl implements FabricService {

    private static final Logger logger = LoggerFactory.getLogger(FabricServiceImpl.class);

    private IZKClient zooKeeper;
    private Map<String, ContainerProvider> providers;
    private ConfigurationAdmin configurationAdmin;
    private String profile = ZkDefs.DEFAULT_PROFILE;
    private ObjectName mbeanName;
    private String userName = "admin";
    private String password = "admin";
    private String defaultRepo = FabricServiceImpl.DEFAULT_REPO_URI;

    public FabricServiceImpl() {
        providers = new ConcurrentHashMap<String, ContainerProvider>();
        providers.put("child", new ChildContainerProvider(this));
    }

    public IZKClient getZooKeeper() {
        return zooKeeper;
    }

    public void setZooKeeper(IZKClient zooKeeper) {
        this.zooKeeper = zooKeeper;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getDefaultRepo() {
        return defaultRepo;
    }

    public void setDefaultRepo(String defaultRepo) {
        this.defaultRepo = defaultRepo;
    }

    @Override
    public Container getCurrentContainer() {
        String name = getCurrentContainerName();
        return getContainer(name);
    }

    @Override
    public String getCurrentContainerName() {
        // TODO is there any other way to find this?
        return System.getProperty("karaf.name");
    }

    public ObjectName getMbeanName() throws MalformedObjectNameException {
        if (mbeanName == null) {
            mbeanName = new ObjectName("org.fusesource.fabric:type=FabricService");
        }
        return mbeanName;
    }

    public void setMbeanName(ObjectName mbeanName) {
        this.mbeanName = mbeanName;
    }

    public ConfigurationAdmin getConfigurationAdmin() {
        return configurationAdmin;
    }

    public void setConfigurationAdmin(ConfigurationAdmin configurationAdmin) {
        this.configurationAdmin = configurationAdmin;
    }

    public Container[] getContainers() {
        try {
            Map<String, Container> containers = new HashMap<String, Container>();
            List<String> configs = zooKeeper.getChildren(ZkPath.CONFIGS_CONTAINERS.getPath());
            for (String name : configs) {
                String parentId = getParentOf(name);
                if (parentId.isEmpty()) {
                    if (!containers.containsKey(name)) {
                        Container container = new ContainerImpl(null, name, this);
                        containers.put(name, container);
                    }
                } else {
                    Container parent = containers.get(parentId);
                    if (parent == null) {
                        parent = new ContainerImpl(null, parentId, this);
                        containers.put(parentId, parent);
                    }
                    Container container = new ContainerImpl(parent, name, this);
                    containers.put(name, container);
                }
            }

            return containers.values().toArray(new Container[containers.size()]);
        } catch (Exception e) {
            throw new FabricException(e);
        }
    }

    private String getParentOf(String name) throws InterruptedException, KeeperException {
        if (zooKeeper != null) {
            try {
                return zooKeeper.getStringData(ZkPath.CONTAINER_PARENT.getPath(name)).trim();
            } catch (KeeperException.NoNodeException e) {
                // Ignore
            } catch (Throwable e) {
                logger.warn("Failed to find parent " + name + ". This exception will be ignored.", e);
            }
        }
        return "";
    }

    public Container getContainer(String name) {
        if (name == null) {
            return null;
        }
        try {
            Container parent = null;
            String parentId = getParentOf(name);
            if (parentId != null && !parentId.isEmpty()) {
                parent = getContainer(parentId);
            }
            return new ContainerImpl(parent, name, this);
        } catch (FabricException e) {
            throw e;
        } catch (Exception e) {
            throw new FabricException(e);
        }
    }

    public void startContainer(final Container container) {
        logger.info("Starting container {}", container.getId());
        ContainerProvider provider = getProvider(container);
        if (!container.isAlive()) {
            provider.start(container);
        }
    }

    public void stopContainer(final Container container) {
        logger.info("Stopping container {}", container.getId());
        ContainerProvider provider = getProvider(container);
        if (container.isAlive()) {
            provider.stop(container);
        }
    }

    public void destroyContainer(Container container) {
        logger.info("Destroying container {}", container.getId());
        ContainerProvider provider = getProvider(container);
        try {
            provider.destroy(container);
        } catch (Exception e) {
        }
        try {
            zooKeeper.deleteWithChildren(ZkPath.CONFIG_CONTAINER.getPath(container.getId()));
            zooKeeper.deleteWithChildren(ZkPath.CONTAINER.getPath(container.getId()));
            zooKeeper.deleteWithChildren(ZkPath.CONTAINER_DOMAINS.getPath(container.getId()));
            zooKeeper.deleteWithChildren(ZkPath.CONTAINER_PROVISION.getPath(container.getId()));
        } catch (Exception e) {
        }
    }
    
    protected ContainerProvider getProvider(Container container) {
        CreateContainerMetadata metadata = container.getMetadata();
        String type = metadata != null ? metadata.getCreateOptions().getProviderType() : null;
        if (type == null) {
            throw new UnsupportedOperationException("Container " + container.getId() + " has not been created using Fabric");
        }
        ContainerProvider provider = getProvider(type);
        if (provider == null) {
            throw new UnsupportedOperationException("Container provider " + type + " not supported");
        }
        return provider;
    }

    public static String getParentFromURI(URI uri) {
        String parent = uri.getHost();
        if (parent == null) {
            parent = uri.getSchemeSpecificPart();
        }
        return parent;
    }

    public CreateContainerMetadata[] createContainers(final CreateContainerOptions options) {
        if (options.getZookeeperUrl() == null && !options.isEnsembleServer()) {
            options.setZookeeperUrl(getZookeeperUrl());
        }
        if (options.getProxyUri() == null) {
            options.setProxyUri(getMavenRepoURI());
        }
        try {
            ContainerProvider provider = getProvider(options.getProviderType());
            if (provider == null) {
                throw new FabricException("Unable to find a container provider supporting '" + options.getProviderType() + "'");
            }

            Container parent = options.getParent() != null ? getContainer(options.getParent()) : null;
            Set<? extends CreateContainerMetadata>  metadatas = provider.create(options);

            for (CreateContainerMetadata metadata : metadatas) {
                if (metadata.isSuccess()) {
                    //An ensemble server can be created without an existing ensemble.
                    //In this case container config will be created by the newly created container.
                    //TODO: We need to make sure that this entries are somehow added even to ensemble servers.
                    if (!options.isEnsembleServer()) {
                        createContainerConfig(parent != null ? parent.getId() : "", metadata.getContainerName());
                        // Store metadata
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        ObjectOutputStream oos = new ObjectOutputStream(baos);
                        oos.writeObject(metadata);
                        oos.close();
                        //We encode the metadata so that they are more friendly to import/export.
                        ZooKeeperUtils.set(zooKeeper, ZkPath.CONTAINER_METADATA.getPath(metadata.getContainerName()), Base64Encoder.encode(baos.toByteArray()));

                        Map<String,String> configuration = metadata.getContainerConfguration();
                        for (Map.Entry<String, String> entry : configuration.entrySet()) {
                            String key = entry.getKey();
                            String value = entry.getValue();
                            ZooKeeperUtils.set(zooKeeper, ZkPath.CONTAINER_ENTRY.getPath(metadata.getContainerName(),key),value);
                        }
                        ZooKeeperUtils.set(zooKeeper, ZkPath.CONTAINER_RESOLVER.getPath(metadata.getContainerName()),options.getResolver());
                    }
                    metadata.setContainer(new ContainerImpl(parent, metadata.getContainerName(), FabricServiceImpl.this));
                    ((ContainerImpl) metadata.getContainer()).setMetadata(metadata);
                    logger.info("The container " + metadata.getContainerName() + " has been successfully created");
                } else {
                    logger.info("The creation of the container " + metadata.getContainerName() + " has failed", metadata.getFailure());
                }
            }
            return metadatas.toArray(new CreateContainerMetadata[metadatas.size()]);
        } catch (FabricException e) {
            throw e;
        } catch (Exception e) {
            throw new FabricException(e);
        }
    }

    public ContainerProvider getProvider(final String scheme) {
        return providers.get(scheme);
    }

    public Map<String, ContainerProvider> getProviders() {
        return Collections.unmodifiableMap(providers);
    }

    @Override
    public URI getMavenRepoURI() {
        URI uri = URI.create(defaultRepo);
        try {
            if (zooKeeper != null && zooKeeper.exists(ZkPath.MAVEN_PROXY.getPath("download")) != null) {
                List<String> children = zooKeeper.getChildren(ZkPath.MAVEN_PROXY.getPath("download"));
                if (children != null && !children.isEmpty()) {
                    Collections.sort(children);
                }

                String mavenRepo = ZooKeeperUtils.getSubstitutedPath(zooKeeper, ZkPath.MAVEN_PROXY.getPath("download") + "/" + children.get(0));
                if(mavenRepo != null && !mavenRepo.endsWith("/")) {
                    mavenRepo+="/";
                }
                uri = new URI(mavenRepo);
            }
        } catch (Exception e) {
            //On exception just return uri.
        }
        return uri;
    }

    @Override
    public URI getMavenRepoUploadURI() {
        URI uri = URI.create(defaultRepo);
        try {
            if (zooKeeper != null && zooKeeper.exists(ZkPath.MAVEN_PROXY.getPath("upload")) != null) {
                List<String> children = zooKeeper.getChildren(ZkPath.MAVEN_PROXY.getPath("upload"));
                if (children != null && !children.isEmpty()) {
                    Collections.sort(children);
                }

                String mavenRepo = ZooKeeperUtils.getSubstitutedPath(zooKeeper, ZkPath.MAVEN_PROXY.getPath("upload") + "/" + children.get(0));
                if(mavenRepo != null && !mavenRepo.endsWith("/")) {
                    mavenRepo+="/";
                }
                uri = new URI(mavenRepo);
            }
        } catch (Exception e) {
            //On exception just return uri.
        }
        return uri;
    }

    public void registerProvider(String scheme, ContainerProvider provider) {
        providers.put(scheme, provider);
    }

    public void registerProvider(ContainerProvider provider, Map<String, Object> properties) {
        String scheme = (String) properties.get(ContainerProvider.PROTOCOL);
        registerProvider(scheme, provider);
    }

    public void unregisterProvider(String scheme) {
        if (providers != null && scheme != null) {
            providers.remove(scheme);
        }
    }

    public void unregisterProvider(ContainerProvider provider, Map<String, Object> properties) {
        String scheme = (String) properties.get(ContainerProvider.PROTOCOL);
        unregisterProvider(scheme);
    }

    public void registerMBeanServer(MBeanServer mbeanServer) {
        try {
            ObjectName name = getMbeanName();
            ObjectInstance objectInstance = mbeanServer.registerMBean(this, name);
        } catch (Exception e) {
            logger.warn("An error occurred during mbean server registration. This exception will be ignored.", e);
        }
    }

    public void unregisterMBeanServer(MBeanServer mbeanServer) {
        if (mbeanServer != null) {
            try {
                mbeanServer.unregisterMBean(getMbeanName());
            } catch (Exception e) {
                logger.warn("An error occurred during mbean server un-registration. This exception will be ignored.", e);
            }
        }
    }


    public String getZookeeperUrl() {
        String zooKeeperUrl = null;
        try {
            Configuration config = configurationAdmin.getConfiguration("org.fusesource.fabric.zookeeper", null);
            zooKeeperUrl = (String) config.getProperties().get("zookeeper.url");
        } catch (Exception e) {
            //Ignore it.
        }
        return zooKeeperUrl;
    }

    private void createContainerConfig(String parent, String name) {
        try {
            String configVersion = getDefaultVersion().getName();
            ZooKeeperUtils.createDefault(zooKeeper, ZkPath.CONFIG_CONTAINER.getPath(name), configVersion);
            ZooKeeperUtils.createDefault(zooKeeper, ZkPath.CONFIG_VERSIONS_CONTAINER.getPath(configVersion, name), profile);
            zooKeeper.createOrSetWithParents(CONTAINER_PARENT.getPath(name), parent, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
        } catch (FabricException e) {
            throw e;
        } catch (Exception e) {
            throw new FabricException(e);
        }
    }

    @Override
    public Version getDefaultVersion() {
        try {
            String version = null;
            if (zooKeeper.exists(ZkPath.CONFIG_DEFAULT_VERSION.getPath()) != null) {
                version = zooKeeper.getStringData(ZkPath.CONFIG_DEFAULT_VERSION.getPath());
            }
            if (version == null || version.isEmpty()) {
                version = ZkDefs.DEFAULT_VERSION;
                ZooKeeperUtils.createDefault(zooKeeper, ZkPath.CONFIG_DEFAULT_VERSION.getPath(), version);
                ZooKeeperUtils.createDefault(zooKeeper, ZkPath.CONFIG_VERSION.getPath(version), null);
            }
            return new VersionImpl(version, this);
        } catch (Exception e) {
            throw new FabricException(e);
        }
    }

    @Override
    public void setDefaultVersion(Version version) {
        try {
            ZooKeeperUtils.set(zooKeeper, ZkPath.CONFIG_DEFAULT_VERSION.getPath(), version.getName());
        } catch (Exception e) {
            throw new FabricException(e);
        }
    }

    public Version createVersion(String version) {
        try {
            zooKeeper.createWithParents(ZkPath.CONFIG_VERSION.getPath(version), null, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
            zooKeeper.createWithParents(ZkPath.CONFIG_VERSIONS_PROFILES.getPath(version), null, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
            return new VersionImpl(version, this);
        } catch (Exception e) {
            throw new FabricException(e);
        }
    }

    public Version createVersion(Version parent, String toVersion) {
        try {
            ZooKeeperUtils.copy(zooKeeper, ZkPath.CONFIG_VERSION.getPath(parent.getName()), ZkPath.CONFIG_VERSION.getPath(toVersion));
            return new VersionImpl(toVersion, this);
        } catch (Exception e) {
            throw new FabricException(e);
        }
    }

    public void deleteVersion(String version) {
        try {
            zooKeeper.deleteWithChildren(ZkPath.CONFIG_VERSION.getPath(version));
        } catch (Exception e) {
            throw new FabricException(e);
        }
    }

    public Version[] getVersions() {
        try {
            List<Version> versions = new ArrayList<Version>();
            List<String> children = zooKeeper.getChildren(ZkPath.CONFIG_VERSIONS.getPath());
            for (String child : children) {
                versions.add(new VersionImpl(child, this));
            }
            Collections.sort(versions);
            return versions.toArray(new Version[versions.size()]);
        } catch (Exception e) {
            throw new FabricException(e);
        }
    }

    public Version getVersion(String name) {
        try {
            if (zooKeeper != null && zooKeeper.isConnected() && zooKeeper.exists(ZkPath.CONFIG_VERSION.getPath(name)) == null) {
                throw new FabricException("Version '" + name + "' does not exist!");
            }
            return new VersionImpl(name, this);
        } catch (FabricException e) {
            throw e;
        } catch (Exception e) {
            throw new FabricException(e);
        }
    }

    @Override
    public Profile[] getProfiles(String version) {
        try {

            List<String> names = zooKeeper.getChildren(ZkPath.CONFIG_VERSIONS_PROFILES.getPath(version));
            List<Profile> profiles = new ArrayList<Profile>();
            for (String name : names) {
                profiles.add(new ProfileImpl(name, version, this));
            }
            return profiles.toArray(new Profile[profiles.size()]);
        } catch (Exception e) {
            throw new FabricException(e);
        }
    }

    @Override
    public Profile getProfile(String version, String name) {
        try {

            String path = ZkPath.CONFIG_VERSIONS_PROFILE.getPath(version, name);
            if (zooKeeper.exists(path) == null) {
                return null;
            }
            return new ProfileImpl(name, version, this);
        } catch (Exception e) {
            throw new FabricException(e);
        }
    }

    @Override
    public Profile createProfile(String version, String name) {
        try {
            ZooKeeperUtils.create(zooKeeper, ZkPath.CONFIG_VERSIONS_PROFILE.getPath(version, name));
            return new ProfileImpl(name, version, this);
        } catch (Exception e) {
            throw new FabricException(e);
        }
    }

    @Override
    public void deleteProfile(Profile profile) {
        try {
            zooKeeper.deleteWithChildren(ZkPath.CONFIG_VERSIONS_PROFILE.getPath(profile.getVersion(), profile.getId()));
        } catch (Exception e) {
            throw new FabricException(e);
        }
    }

    protected ContainerTemplate getContainerTemplate(Container container) {
        // there's no point caching the JMX Connector as we are unsure if we'll communicate again with the same container any time soon
        // though in the future we could possibly pool them
        boolean cacheJmx = false;
        return new ContainerTemplate(container, cacheJmx, userName, password);
    }

}
