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

import org.apache.curator.framework.CuratorFramework;
import org.fusesource.fabric.api.Container;
import org.fusesource.fabric.api.ContainerProvider;
import org.fusesource.fabric.api.CreateContainerMetadata;
import org.fusesource.fabric.api.CreateContainerOptions;
import org.fusesource.fabric.api.DataStore;
import org.fusesource.fabric.api.FabricException;
import org.fusesource.fabric.api.FabricRequirements;
import org.fusesource.fabric.api.FabricService;
import org.fusesource.fabric.api.FabricStatus;
import org.fusesource.fabric.api.PatchService;
import org.fusesource.fabric.api.PortService;
import org.fusesource.fabric.api.Profile;
import org.fusesource.fabric.api.Version;
import org.fusesource.fabric.api.jmx.FabricManager;
import org.fusesource.fabric.api.jmx.FileSystem;
import org.fusesource.fabric.api.jmx.HealthCheck;
import org.fusesource.fabric.api.jmx.ZooKeeperFacade;
import org.fusesource.fabric.internal.ContainerImpl;
import org.fusesource.fabric.internal.ProfileImpl;
import org.fusesource.fabric.internal.VersionImpl;
import org.fusesource.fabric.utils.Constants;
import org.fusesource.fabric.utils.SystemProperties;
import org.fusesource.fabric.zookeeper.ZkPath;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.MBeanServer;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.google.common.base.Strings;

import static org.fusesource.fabric.zookeeper.utils.ZooKeeperUtils.exists;
import static org.fusesource.fabric.zookeeper.utils.ZooKeeperUtils.generatePassword;
import static org.fusesource.fabric.zookeeper.utils.ZooKeeperUtils.getChildren;
import static org.fusesource.fabric.zookeeper.utils.ZooKeeperUtils.getSubstitutedData;
import static org.fusesource.fabric.zookeeper.utils.ZooKeeperUtils.getSubstitutedPath;


public class FabricServiceImpl implements FabricService {

    public static final String REQUIREMENTS_JSON_PATH = "/fabric/configs/org.fusesource.fabric.requirements.json";
    public static final String JVM_OPTIONS_PATH = "/fabric/configs/org.fusesource.fabric.containers.jvmOptions";

    private static final Logger LOGGER = LoggerFactory.getLogger(FabricServiceImpl.class);

    private CuratorFramework curator;
    private DataStore dataStore;
    private PortService portService;
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

    public CuratorFramework getCurator() {
        return curator;
    }

    public void setCurator(CuratorFramework curator) {
        this.curator = curator;
    }

    public void setDataStore(DataStore dataStore) {
        this.dataStore = dataStore;
    }

    public DataStore getDataStore() {
        return dataStore;
    }

    public PortService getPortService() {
        return portService;
    }

    public void setPortService(PortService portService) {
        this.portService = portService;
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

    @Override
    public void trackConfiguration(Runnable callback) {
        getDataStore().trackConfiguration(callback);
    }

    @Override
    public void unTrackConfiguration(Runnable callback) {
        getDataStore().unTrackConfiguration(callback);
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
            portService.unRegisterPort(container);
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
            options.setZookeeperPassword(generatePassword());
        }

        try {
            ContainerProvider provider = getProvider(options.getProviderType());
            if (provider == null) {
                throw new FabricException("Unable to find a container provider supporting '" + options.getProviderType() + "'");
            }

            Set<? extends CreateContainerMetadata> metadatas = provider.create(options);

            for (CreateContainerMetadata metadata : metadatas) {
                if (metadata.isSuccess()) {
                    Container parent = options.getParent() != null ? getContainer(options.getParent()) : null;
                    //An ensemble server can be created without an existing ensemble.
                    //In this case container config will be created by the newly created container.
                    //TODO: We need to make sure that this entries are somehow added even to ensemble servers.
                    if (!options.isEnsembleServer()) {
                        getDataStore().createContainerConfig(metadata);
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
            if (curator != null && exists(curator, ZkPath.MAVEN_PROXY.getPath("download")) != null) {
                List<String> children = getChildren(curator, ZkPath.MAVEN_PROXY.getPath("download"));
                if (children != null && !children.isEmpty()) {
                    Collections.sort(children);
                }

                String mavenRepo = getSubstitutedPath(curator, ZkPath.MAVEN_PROXY.getPath("download") + "/" + children.get(0));
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
    public List<URI> getMavenRepoURIs() {
        try {
            List<URI> uris = new ArrayList<URI>();
            if (curator != null && exists(curator, ZkPath.MAVEN_PROXY.getPath("download")) != null) {
                List<String> children = getChildren(curator, ZkPath.MAVEN_PROXY.getPath("download"));
                if (children != null && !children.isEmpty()) {
                    Collections.sort(children);
                }
                if (children != null) {
                    for (String child : children) {
                        String mavenRepo = getSubstitutedPath(curator, ZkPath.MAVEN_PROXY.getPath("download") + "/" + child);
                        if (mavenRepo != null && !mavenRepo.endsWith("/")) {
                            mavenRepo += "/";
                        }
                        uris.add(new URI(mavenRepo));
                    }
                }
            }
            return uris;
        } catch (Exception e) {
            throw new FabricException(e);
        }
    }

    @Override
    public URI getMavenRepoUploadURI() {
        URI uri = URI.create(defaultRepo);
        try {
            if (curator != null && exists(curator, ZkPath.MAVEN_PROXY.getPath("upload")) != null) {
                List<String> children = getChildren(curator, ZkPath.MAVEN_PROXY.getPath("upload"));
                if (children != null && !children.isEmpty()) {
                    Collections.sort(children);
                }

                String mavenRepo = getSubstitutedPath(curator, ZkPath.MAVEN_PROXY.getPath("upload") + "/" + children.get(0));
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

    public String containerWebAppURL(String webAppId, String name) {
        String answer = null;
        try {
            String versionsPath = ZkPath.WEBAPPS_CLUSTER.getPath(webAppId);
            if (curator != null && exists(curator, versionsPath) != null) {
                List<String> children = getChildren(curator, versionsPath);
                if (children != null && !children.isEmpty()) {
                    for (String child : children) {
                        if (Strings.isNullOrEmpty(name)) {
                            // lets just use the first container we find
                            String parentPath = versionsPath + "/" + child;
                            List<String> grandChildren = getChildren(curator, parentPath);
                            if (!grandChildren.isEmpty()) {
                                String containerPath = parentPath + "/" + grandChildren.get(0);
                                answer = getWebUrl(containerPath);
                                if (!Strings.isNullOrEmpty(answer)) {
                                    return answer;
                                }
                            }
                        } else {
                            String containerPath = versionsPath + "/" + child + "/" + name;
                            answer = getWebUrl(containerPath);
                            if (!Strings.isNullOrEmpty(answer)) {
                                return answer;
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.error("Failed to find container Jolokia URL " + e, e);
        }
        return answer;


    }

    protected String getWebUrl(String containerPath) throws Exception {
        if (curator.checkExists().forPath(containerPath) != null) {
            byte[] bytes = ZkPath.loadURL(curator, containerPath);
            String text = new String(bytes);
            // NOTE this is a bit naughty, we should probably be doing
            // Jackson parsing here; but we only need 1 String and
            // this avoids the jackson runtime dependency - its just a bit brittle
            // only finding http endpoints and all
            int idx = text.indexOf("\"http");
            String answer = text;
            if (idx > 0) {
                int endIdx = text.indexOf('\"', idx + 5);
                if (endIdx > 0) {
                    answer = text.substring(idx + 1, endIdx);
                    if (answer.length() > 0) {
                        return answer;
                    }
                }
            }
        }
        return null;
    }

    public void registerProvider(String scheme, ContainerProvider provider) {
        providers.put(scheme, provider);
    }

    public void registerProvider(ContainerProvider provider, Map<String, Object> properties) {
        String scheme = (String) properties.get(Constants.PROTOCOL);
        registerProvider(scheme, provider);
    }

    public void unregisterProvider(String scheme) {
        if (providers != null && scheme != null) {
            providers.remove(scheme);
        }
    }

    public void unregisterProvider(ContainerProvider provider, Map<String, Object> properties) {
        String scheme = (String) properties.get(Constants.PROTOCOL);
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
            if (curator != null && curator.getZookeeperClient().isConnected()) {
                Version defaultVersion = getDefaultVersion();
                if (defaultVersion != null) {
                    Profile profile = defaultVersion.getProfile("default");
                    if (profile != null) {
                        Map<String, Map<String, String>> configurations = profile.getConfigurations();
                        if (configurations != null) {
                            Map<String, String> zookeeperConfig = configurations.get("org.fusesource.fabric.zookeeper");
                            if (zookeeperConfig != null) {
                                zooKeeperUrl = getSubstitutedData(curator, zookeeperConfig.get(name));
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
