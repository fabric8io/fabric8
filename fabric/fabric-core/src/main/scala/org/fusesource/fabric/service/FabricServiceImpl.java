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

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.fusesource.fabric.api.*;
import org.fusesource.fabric.api.jmx.FabricManager;
import org.fusesource.fabric.api.jmx.FileSystem;
import org.fusesource.fabric.api.jmx.HealthCheck;
import org.fusesource.fabric.api.jmx.ZooKeeperFacade;
import org.fusesource.fabric.internal.ContainerImpl;
import org.fusesource.fabric.internal.ProfileImpl;
import org.fusesource.fabric.internal.RequirementsJson;
import org.fusesource.fabric.internal.VersionImpl;
import org.fusesource.fabric.utils.Base64Encoder;
import org.fusesource.fabric.utils.ObjectUtils;
import org.fusesource.fabric.utils.SystemProperties;
import org.fusesource.fabric.zookeeper.IZKClient;
import org.fusesource.fabric.zookeeper.ZkDefs;
import org.fusesource.fabric.zookeeper.ZkPath;
import org.fusesource.fabric.zookeeper.utils.ZooKeeperUtils;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.MBeanServer;
import java.io.IOException;
import java.net.URI;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static org.fusesource.fabric.zookeeper.ZkPath.CONTAINER_PARENT;

public class FabricServiceImpl implements FabricService {

    public static final String REQUIREMENTS_JSON_PATH = "/fabric/configs/org.fusesource.fabric.requirements.json";
    public static final String JVM_OPTIONS_PATH = "/fabric/configs/org.fusesource.fabric.containers.jvmOptions";

    private static final Logger LOGGER = LoggerFactory.getLogger(FabricServiceImpl.class);

    private IZKClient zooKeeper;
    private DataStore dataStore;
    private Map<String, ContainerProvider> providers;
    private ConfigurationAdmin configurationAdmin;
    private String defaultRepo = FabricServiceImpl.DEFAULT_REPO_URI;
    private final HealthCheck healthCheck = new HealthCheck(this);
    private final FabricManager managerMBean = new FabricManager(this);
    private final ZooKeeperFacade zooKeeperMBean = new ZooKeeperFacade(this);
    private final FileSystem fileSystemMBean = new FileSystem();
    private MBeanServer mbeanServer;

    public FabricServiceImpl() {
        providers = new ConcurrentHashMap<String, ContainerProvider>();
        providers.put("child", new ChildContainerProvider(this));
    }

    public void bindMBeanServer(MBeanServer mbeanServer) {
        unbindMBeanServer(this.mbeanServer);
        this.mbeanServer = mbeanServer;
        if (mbeanServer != null) {
            healthCheck.registerMBeanServer(this.mbeanServer);
            managerMBean.registerMBeanServer(this.mbeanServer);
            fileSystemMBean.registerMBeanServer(this.mbeanServer);
            zooKeeperMBean.registerMBeanServer(this.mbeanServer);
        }
    }

    public void unbindMBeanServer(MBeanServer mbeanServer) {
        if (mbeanServer != null) {
            zooKeeperMBean.unregisterMBeanServer(mbeanServer);
            fileSystemMBean.unregisterMBeanServer(mbeanServer);
            managerMBean.unregisterMBeanServer(mbeanServer);
            healthCheck.unregisterMBeanServer(mbeanServer);
            this.mbeanServer = null;
        }
    }

    public IZKClient getZooKeeper() {
        return zooKeeper;
    }

    public void setDataStore(DataStore dataStore) {
        this.dataStore = dataStore;
    }

    public DataStore getDataStore() {
        return dataStore;
    }

    public void setZooKeeper(IZKClient zooKeeper) {
        this.zooKeeper = zooKeeper;
    }

    public HealthCheck getHealthCheck() {
        return healthCheck;
    }

    public FabricManager getManagerMBean() {
        return managerMBean;
    }

    public FileSystem getFileSystem() {
        return fileSystemMBean;
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
        return System.getProperty(SystemProperties.KARAF_NAME);
    }

    public ConfigurationAdmin getConfigurationAdmin() {
        return configurationAdmin;
    }

    public void setConfigurationAdmin(ConfigurationAdmin configurationAdmin) {
        this.configurationAdmin = configurationAdmin;
    }

    public Container[] getContainers() {
        Map<String, Container> containers = new HashMap<String, Container>();
        List<String> containerIds = getDataStore().getContainers();
        for (String containerId : containerIds) {
            String parentId = getDataStore().getContainerParent(containerId);
            if (parentId.isEmpty()) {
                if (!containers.containsKey(containerId)) {
                    Container container = new ContainerImpl(null, containerId, this);
                    containers.put(containerId, container);
                }
            } else {
                Container parent = containers.get(parentId);
                if (parent == null) {
                    parent = new ContainerImpl(null, parentId, this);
                    containers.put(parentId, parent);
                }
                Container container = new ContainerImpl(parent, containerId, this);
                containers.put(containerId, container);
            }
        }
        return containers.values().toArray(new Container[containers.size()]);
    }

    public Container getContainer(String name) {
        if (getDataStore().hasContainer(name)) {
            Container parent = null;
            String parentId = getDataStore().getContainerParent(name);
            if (parentId != null && !parentId.isEmpty()) {
                parent = getContainer(parentId);
            }
            return new ContainerImpl(parent, name, this);
        }
        throw new FabricException("Container '" + name + "' does not exist");
    }

    public void startContainer(String containerId) {
        Container container = getContainer(containerId);
        if (container != null) {
            startContainer(container);
        }
    }

    public void startContainer(final Container container) {
        LOGGER.info("Starting container {}", container.getId());
        ContainerProvider provider = getProvider(container);
        if (!container.isAlive()) {
            provider.start(container);
        }
    }

    public void stopContainer(String containerId) {
        Container container = getContainer(containerId);
        if (container != null) {
            stopContainer(container);
        }
    }

    public void stopContainer(final Container container) {
        LOGGER.info("Stopping container {}", container.getId());
        ContainerProvider provider = getProvider(container);
        if (container.isAlive()) {
            provider.stop(container);
        }
    }


    public void destroyContainer(String containerId) {
        Container container = getContainer(containerId);
        if (container != null) {
            destroyContainer(container);
        }
    }

    public void destroyContainer(Container container) {
        String containerId = container.getId();
        LOGGER.info("Destroying container {}", containerId);
        ContainerProvider provider = getProvider(container);
        provider.destroy(container);

        try {
            getDataStore().deleteContainer(container.getId());
        } catch (Exception e) {
           LOGGER.warn("Failed to cleanup container {} entries due to: {}. This will be ignored.", containerId, e.getMessage());
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
        if (options.getJvmOpts() == null || options.getJvmOpts().length() == 0) {
            options.setJvmOpts(getDefaultJvmOptions());
        }

        if (options.isEnsembleServer() && (options.getZookeeperPassword() == null || options.getZookeeperPassword().isEmpty())) {
            options.setZookeeperPassword(ZooKeeperUtils.generatePassword());
        }

        try {
            ContainerProvider provider = getProvider(options.getProviderType());
            if (provider == null) {
                throw new FabricException("Unable to find a container provider supporting '" + options.getProviderType() + "'");
            }

            Container parent = options.getParent() != null ? getContainer(options.getParent()) : null;
            Set<? extends CreateContainerMetadata> metadatas = provider.create(options);

            for (CreateContainerMetadata metadata : metadatas) {
                if (metadata.isSuccess()) {
                    //An ensemble server can be created without an existing ensemble.
                    //In this case container config will be created by the newly created container.
                    //TODO: We need to make sure that this entries are somehow added even to ensemble servers.
                    if (!options.isEnsembleServer()) {
                        getDataStore().createContainerConfig(parent != null ? parent.getId() : "", metadata.getContainerName());
                        // Store metadata
                        //We encode the metadata so that they are more friendly to import/export.
                        ZooKeeperUtils.set(zooKeeper, ZkPath.CONTAINER_METADATA.getPath(metadata.getContainerName()), Base64Encoder.encode(ObjectUtils.toBytes(metadata)));

                        Map<String, String> configuration = metadata.getContainerConfiguration();
                        for (Map.Entry<String, String> entry : configuration.entrySet()) {
                            String key = entry.getKey();
                            String value = entry.getValue();
                            ZooKeeperUtils.set(zooKeeper, ZkPath.CONTAINER_ENTRY.getPath(metadata.getContainerName(), key), value);
                        }

                        //If no resolver specified but a resolver is already present in the registry, use the registry value
                        if (options.getResolver() == null && zooKeeper.exists(ZkPath.CONTAINER_RESOLVER.getPath(metadata.getContainerName())) != null) {
                            options.setResolver(zooKeeper.getStringData(ZkPath.CONTAINER_RESOLVER.getPath(metadata.getContainerName())));
                        } else if (options.getResolver() != null) {
                            //use the resolver specified in the options and do nothing.
                        } else if (zooKeeper.exists(ZkPath.POLICIES.getPath(ZkDefs.RESOLVER)) != null) {
                            //If there is a globlal resolver specified use it.
                            options.setResolver(zooKeeper.getStringData(ZkPath.POLICIES.getPath(ZkDefs.RESOLVER)));
                        } else {
                            //Fallback to the default resolver
                            options.setResolver(ZkDefs.DEFAULT_RESOLVER);
                        }
                        //Set the resolver if not exists
                        ZooKeeperUtils.set(zooKeeper, ZkPath.CONTAINER_RESOLVER.getPath(metadata.getContainerName()), options.getResolver());
                    }
                    ContainerImpl container = new ContainerImpl(parent, metadata.getContainerName(), FabricServiceImpl.this);
                    metadata.setContainer(container);
                    container.setMetadata(metadata);
                    LOGGER.info("The container " + metadata.getContainerName() + " has been successfully created");
                } else {
                    LOGGER.info("The creation of the container " + metadata.getContainerName() + " has failed", metadata.getFailure());
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
                if (mavenRepo != null && !mavenRepo.endsWith("/")) {
                    mavenRepo += "/";
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
                if (mavenRepo != null && !mavenRepo.endsWith("/")) {
                    mavenRepo += "/";
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

    public String getZookeeperUrl() {
        return getZookeeperInfo("zookeeper.url");
    }

    public String getZookeeperPassword() {
        return getZookeeperInfo("zookeeper.password");
    }

    public String getZookeeperInfo(String name) {
        String zooKeeperUrl = null;
        //We are looking directly for at the zookeeper for the url, since container might not even be mananaged.
        //Also this is required for the integration with the IDE.
        try {
            if (zooKeeper != null && zooKeeper.isConnected()) {
                Version defaultVersion = getDefaultVersion();
                if (defaultVersion != null) {
                    Profile profile = defaultVersion.getProfile("default");
                    if (profile != null) {
                        Map<String, Map<String, String>> configurations = profile.getConfigurations();
                        if (configurations != null) {
                            Map<String, String> zookeeperConfig = configurations.get("org.fusesource.fabric.zookeeper");
                            if (zookeeperConfig != null) {
                                zooKeeperUrl = ZooKeeperUtils.getSubstitutedData(zooKeeper, zookeeperConfig.get(name));
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            //Ignore it.
        }

        if (zooKeeperUrl == null) {
            try {
                Configuration config = configurationAdmin.getConfiguration("org.fusesource.fabric.zookeeper", null);
                zooKeeperUrl = (String) config.getProperties().get(name);
            } catch (Exception e) {
                //Ignore it.
            }
        }
        return zooKeeperUrl;
    }

    @Override
    public Version getDefaultVersion() {
        return new VersionImpl(getDataStore().getDefaultVersion(), this);
    }

    @Override
    public void setDefaultVersion(Version version) {
        setDefaultVersion(version.getId());
    }

    public void setDefaultVersion(String versionId) {
        getDataStore().setDefaultVersion(versionId);
    }

    public Version createVersion(String version) {
        getDataStore().createVersion(version);
        return new VersionImpl(version, this);
    }

    public Version createVersion(Version parent, String toVersion) {
        return createVersion(parent.getId(), toVersion);
    }

    public Version createVersion(String parentVersionId, String toVersion) {
        getDataStore().createVersion(parentVersionId, toVersion);
        return new VersionImpl(toVersion, this);
    }

    public void deleteVersion(String version) {
        getVersion(version).delete();
    }

    public Version[] getVersions() {
        List<Version> versions = new ArrayList<Version>();
        List<String> children = getDataStore().getVersions();
        for (String child : children) {
            versions.add(new VersionImpl(child, this));
        }
        Collections.sort(versions);
        return versions.toArray(new Version[versions.size()]);
    }

    public Version getVersion(String name) {
        if (getDataStore().hasVersion(name)) {
            return new VersionImpl(name, this);
        }
        throw new FabricException("Version '" + name + "' does not exist");
    }

    @Override
    public Profile[] getProfiles(String version) {
        return getVersion(version).getProfiles();
    }

    @Override
    public Profile getProfile(String version, String name) {
        return getVersion(version).getProfile(name);
    }

    @Override
    public Profile createProfile(String version, String name) {
        getDataStore().createProfile(version, name);
        return new ProfileImpl(name, version, this);
    }

    @Override
    public void deleteProfile(Profile profile) {
        deleteProfile(profile.getVersion(), profile.getId());
    }

    public void deleteProfile(String versionId, String profileId) {
        getDataStore().deleteProfile(versionId, profileId);
    }

    protected ContainerTemplate getContainerTemplate(Container container, String jmxUser, String jmxPassword) {
        // there's no point caching the JMX Connector as we are unsure if we'll communicate again with the same container any time soon
        // though in the future we could possibly pool them
        boolean cacheJmx = false;
        return new ContainerTemplate(container, jmxUser, jmxPassword, cacheJmx);
    }

    @Override
    public void setRequirements(FabricRequirements requirements) throws IOException {
        getDataStore().setRequirements(requirements);
    }

    @Override
    public FabricRequirements getRequirements() {
        return getDataStore().getRequirements();
    }

    @Override
    public FabricStatus getFabricStatus() {
        return new FabricStatus(this);
    }

    @Override
    public PatchService getPatchService() {
        return new PatchServiceImpl(this, configurationAdmin);
    }

    @Override
    public String getDefaultJvmOptions() {
        return getDataStore().getDefaultJvmOptions();
    }

    @Override
    public void setDefaultJvmOptions(String jvmOptions) {
        getDataStore().setDefaultJvmOptions(jvmOptions);
    }
}
