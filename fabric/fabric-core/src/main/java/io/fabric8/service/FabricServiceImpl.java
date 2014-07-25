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
package io.fabric8.service;

import static io.fabric8.api.Profiles.assertValidProfileId;
import static io.fabric8.internal.PlaceholderResolverHelpers.getSchemesForProfileConfigurations;
import static io.fabric8.utils.DataStoreUtils.substituteBundleProperty;
import static io.fabric8.zookeeper.utils.ZooKeeperUtils.exists;
import static io.fabric8.zookeeper.utils.ZooKeeperUtils.getChildren;
import static io.fabric8.zookeeper.utils.ZooKeeperUtils.getChildrenSafe;
import static io.fabric8.zookeeper.utils.ZooKeeperUtils.getSubstitutedData;
import static io.fabric8.zookeeper.utils.ZooKeeperUtils.getSubstitutedPath;
import static org.apache.felix.scr.annotations.ReferenceCardinality.OPTIONAL_MULTIPLE;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.fabric8.api.AutoScaleStatus;
import io.fabric8.api.Constants;
import io.fabric8.api.Container;
import io.fabric8.api.ContainerAutoScaler;
import io.fabric8.api.ContainerAutoScalerFactory;
import io.fabric8.api.ContainerProvider;
import io.fabric8.api.Containers;
import io.fabric8.api.CreateContainerBasicMetadata;
import io.fabric8.api.CreateContainerBasicOptions;
import io.fabric8.api.CreateContainerMetadata;
import io.fabric8.api.CreateContainerOptions;
import io.fabric8.api.CreationStateListener;
import io.fabric8.api.DataStore;
import io.fabric8.api.FabricException;
import io.fabric8.api.FabricRequirements;
import io.fabric8.api.FabricService;
import io.fabric8.api.FabricStatus;
import io.fabric8.api.NameValidator;
import io.fabric8.api.NullCreationStateListener;
import io.fabric8.api.PatchService;
import io.fabric8.api.PlaceholderResolver;
import io.fabric8.api.PortService;
import io.fabric8.api.Profile;
import io.fabric8.api.ProfileBuilder;
import io.fabric8.api.ProfileRegistry;
import io.fabric8.api.ProfileRequirements;
import io.fabric8.api.ProfileService;
import io.fabric8.api.Profiles;
import io.fabric8.api.RuntimeProperties;
import io.fabric8.api.Version;
import io.fabric8.api.VersionBuilder;
import io.fabric8.api.jcip.ThreadSafe;
import io.fabric8.api.scr.AbstractComponent;
import io.fabric8.api.scr.ValidatingReference;
import io.fabric8.api.visibility.VisibleForTesting;
import io.fabric8.internal.ContainerImpl;
import io.fabric8.utils.DataStoreUtils;
import io.fabric8.utils.PasswordEncoder;
import io.fabric8.utils.SystemProperties;
import io.fabric8.zookeeper.ZkPath;
import io.fabric8.zookeeper.utils.InterpolationHelper;
import io.fabric8.zookeeper.utils.ZooKeeperUtils;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.curator.framework.CuratorFramework;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.Service;
import org.osgi.framework.BundleContext;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;

/**
 * FabricService
 * |_ ConfigurationAdmin
 * |_ PlaceholderResolver (optional,multiple)
 * |_ CuratorFramework (@see ManagedCuratorFramework)
 * |  |_ ACLProvider (@see CuratorACLManager)
 * |_ DataStore (@see GitDataStore)
 *    |_ CuratorFramework  --^
 *    |_ GitService (@see FabricGitServiceImpl)
 *    |_ ContainerProvider (optional,multiple) (@see ChildContainerProvider)
 *    |  |_ FabricService --^
 *    |_ PortService (@see ZookeeperPortService)
 *       |_ CuratorFramework --^
 */
@ThreadSafe
@Component(name = "io.fabric8.service", label = "Fabric8 Service", metatype = false)
@Service(FabricService.class)
public final class FabricServiceImpl extends AbstractComponent implements FabricService {

    public static final String REQUIREMENTS_JSON_PATH = "/fabric/configs/io.fabric8.requirements.json";
    public static final String JVM_OPTIONS_PATH = "/fabric/configs/io.fabric8.containers.jvmOptions";

    private static final Logger LOGGER = LoggerFactory.getLogger(FabricServiceImpl.class);

    // Logical Dependencies
    @Reference
    private ChecksumPlaceholderResolver checksumPlaceholderResolver;
    @Reference
    private ContainerPlaceholderResolver containerPlaceholderResolver;
    @Reference
    private EncryptedPropertyResolver encryptedPropertyResolver;
    @Reference
    private EnvPlaceholderResolver envPlaceholderResolver;
    @Reference
    private PortPlaceholderResolver portPlaceholderResolver;
    @Reference
    private ProfilePropertyPointerResolver profilePropertyPointerResolver;
    @Reference
    private VersionPropertyPointerResolver versionPropertyPointerResolver;
    @Reference
    private ZookeeperPlaceholderResolver zookeeperPlaceholderResolver;

    @Reference(referenceInterface = ConfigurationAdmin.class)
    private final ValidatingReference<ConfigurationAdmin> configAdmin = new ValidatingReference<ConfigurationAdmin>();
    @Reference(referenceInterface = RuntimeProperties.class)
    private final ValidatingReference<RuntimeProperties> runtimeProperties = new ValidatingReference<RuntimeProperties>();
    @Reference(referenceInterface = CuratorFramework.class)
    private final ValidatingReference<CuratorFramework> curator = new ValidatingReference<CuratorFramework>();
    @Reference(referenceInterface = DataStore.class)
    private final ValidatingReference<DataStore> dataStore = new ValidatingReference<DataStore>();
    @Reference(referenceInterface = PortService.class)
    private final ValidatingReference<PortService> portService = new ValidatingReference<PortService>();
    @Reference(referenceInterface = ProfileService.class)
    private final ValidatingReference<ProfileService> profileService = new ValidatingReference<>();
    @Reference(referenceInterface = ProfileRegistry.class)
    private final ValidatingReference<ProfileRegistry> profileRegistry = new ValidatingReference<>();
    @Reference(referenceInterface = ContainerProvider.class, bind = "bindProvider", unbind = "unbindProvider", cardinality = OPTIONAL_MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    private final Map<String, ContainerProvider> providers = new ConcurrentHashMap<String, ContainerProvider>();
    @Reference(referenceInterface = PlaceholderResolver.class, bind = "bindPlaceholderResolver", unbind = "unbindPlaceholderResolver", cardinality = OPTIONAL_MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    private final Map<String, PlaceholderResolver> placeholderResolvers = new ConcurrentHashMap<String, PlaceholderResolver>();

    private String defaultRepo = FabricService.DEFAULT_REPO_URI;
    private BundleContext bundleContext;

    @Activate
    void activate(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
        activateComponent();
    }

    @Deactivate
    void deactivate() {
        deactivateComponent();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T adapt(Class<T> type) {
        assertValid();
        if (type.isAssignableFrom(CuratorFramework.class)) {
            return (T) curator.get();
        } else if (type.isAssignableFrom(DataStore.class)) {
            return (T) dataStore.get();
        } else if (type.isAssignableFrom(ProfileService.class)) {
            return (T) profileService.get();
        } else if (type.isAssignableFrom(ProfileRegistry.class)) {
            return (T) profileRegistry.get();
        }
        return null;
    }

    public String getDefaultRepo() {
        synchronized (this) {
            return defaultRepo;
        }
    }

    public void setDefaultRepo(String defaultRepo) {
        synchronized (this) {
            this.defaultRepo = defaultRepo;
        }
    }

    @Override
    public PortService getPortService() {
        assertValid();
        return portService.get();
    }

    @Override
    public Container getCurrentContainer() {
        assertValid();
        String name = getCurrentContainerName();
        return getContainer(name);
    }

    @Override
    public String getEnvironment() {
        assertValid();
        return runtimeProperties.get().getProperty(SystemProperties.FABRIC_ENVIRONMENT);
    }

    @Override
    public String getCurrentContainerName() {
        assertValid();
        return runtimeProperties.get().getRuntimeIdentity();
    }

    @Override
    public void trackConfiguration(Runnable callback) {
        assertValid();
        dataStore.get().trackConfiguration(callback);
    }

    @Override
    public void untrackConfiguration(Runnable callback) {
        assertValid();
        dataStore.get().untrackConfiguration(callback);
    }

    @Override
    public Container[] getContainers() {
        assertValid();
        Map<String, Container> containers = new HashMap<String, Container>();
        List<String> containerIds = dataStore.get().getContainers();
        for (String containerId : containerIds) {
            String parentId = dataStore.get().getContainerParent(containerId);
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

	@Override
	public Container[] getAssociatedContainers(String versionId, String profileId) {
		assertValid();
        List<Container> containers = new ArrayList<>();
        for (Container container : getContainers()) {
        	for (Profile profile : Arrays.asList(container.getProfiles())) {
            	if (profile.getId().equals(profileId)) {
            		containers.add(container);
            	}
        	}
        }
        return containers.toArray(new Container[containers.size()]);
	}

    @Override
    public Container getContainer(String name) {
        assertValid();
        if (dataStore.get().hasContainer(name)) {
            Container parent = null;
            String parentId = dataStore.get().getContainerParent(name);
            if (parentId != null && !parentId.isEmpty()) {
                parent = getContainer(parentId);
            }
            return new ContainerImpl(parent, name, this);
        }
        throw new FabricException("Container '" + name + "' does not exist");
    }

    @Override
    public void startContainer(String containerId) {
        startContainer(containerId, false);
    }

    public void startContainer(String containerId, boolean force) {
        assertValid();
        Container container = getContainer(containerId);
        if (container != null) {
            startContainer(container, force);
        }
    }

    public void startContainer(Container container) {
        startContainer(container, true);
    }

    public void startContainer(Container container, boolean force) {
        assertValid();
        LOGGER.info("Starting container {}", container.getId());
        ContainerProvider provider = getProvider(container);
        provider.start(container);
    }

    @Override
    public void stopContainer(String containerId) {
        stopContainer(containerId, false);
    }

    public void stopContainer(String containerId, boolean force) {
        assertValid();
        Container container = getContainer(containerId);
        if (container != null) {
            stopContainer(container, force);
        }
    }

    public void stopContainer(Container container) {
        stopContainer(container, false);
    }

    public void stopContainer(Container container, boolean force) {
        assertValid();
        LOGGER.info("Stopping container {}", container.getId());
        ContainerProvider provider = getProvider(container);
        provider.stop(container);
    }

    @Override
    public void destroyContainer(String containerId) {
        destroyContainer(containerId, false);
    }

    public void destroyContainer(String containerId, boolean force) {
        assertValid();
        Container container = getContainer(containerId);
        if (container != null) {
            destroyContainer(container, force);
        }
    }

    @Override
    public void destroyContainer(Container container) {
        destroyContainer(container, false);
    }

    public void destroyContainer(Container container, boolean force) {
        assertValid();
        String containerId = container.getId();
        LOGGER.info("Destroying container {}", containerId);
        boolean destroyed = false;
        try {
            ContainerProvider provider = getProvider(container, true);
            if (provider != null) {
                try {
                    provider.stop(container);
                } catch (Exception ex) {
                    //Ignore error while stopping and try to destroy.
                }
                provider.destroy(container);
                destroyed = true;
            }

        } finally {
            try {
                if (destroyed || force) {
                    try {
                        portService.get().unregisterPort(container);
                    } catch (Exception e) {
                        LOGGER.warn("Failed to cleanup container {} entries due to: {}. This will be ignored.", containerId, e.getMessage());
                    }
                    dataStore.get().deleteContainer(this, container.getId());
                }
            } catch (Exception e) {
                LOGGER.warn("Failed to cleanup container {} entries due to: {}. This will be ignored.", containerId, e.getMessage());
            }
        }
    }

    private ContainerProvider getProvider(Container container) {
        return getProvider(container, false);
    }

    private ContainerProvider getProvider(Container container, boolean returnNull) {
        CreateContainerMetadata metadata = container.getMetadata();
        String type = metadata != null ? metadata.getCreateOptions().getProviderType() : null;
        if (type == null) {
            if (returnNull) {
                return null;
            }
            throw new UnsupportedOperationException("Container " + container.getId() + " has not been created using Fabric");
        }
        ContainerProvider provider = getProvider(type);
        if (provider == null) {
            if (returnNull) {
                return null;
            }
            throw new UnsupportedOperationException("Container provider " + type + " not supported");
        }
        return provider;
    }

    @Override
    public CreateContainerMetadata[] createContainers(CreateContainerOptions options) {
        return createContainers(options, null);
    }

    @Override
    public CreateContainerMetadata[] createContainers(CreateContainerOptions options, CreationStateListener listener) {
        assertValid();
        try {
            final ContainerProvider provider = getProvider(options.getProviderType());
            if (provider == null) {
                throw new FabricException("Unable to find a container provider supporting '" + options.getProviderType() + "'");
            }

            String originalName = options.getName();
            if (originalName == null || originalName.length() == 0) {
                throw new FabricException("A name must be specified when creating containers");
            }

            if (listener == null) {
                listener = new NullCreationStateListener();
            }

            ObjectMapper mapper = new ObjectMapper();
            mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            Map optionsMap = mapper.readValue(mapper.writeValueAsString(options), Map.class);
            String versionId = options.getVersion() != null ? options.getVersion() : dataStore.get().getDefaultVersion();
            Set<String> profileIds = options.getProfiles();
            if (profileIds == null || profileIds.isEmpty()) {
                profileIds = new LinkedHashSet<String>();
                profileIds.add("default");
            }
            optionsMap.put("version", versionId);
            optionsMap.put("profiles", profileIds);
            optionsMap.put("number", 0);

            final List<CreateContainerMetadata> metadatas = new CopyOnWriteArrayList<CreateContainerMetadata>();
            int orgNumber = options.getNumber();
            int number = Math.max(orgNumber, 1);
            final CountDownLatch latch = new CountDownLatch(number);
            Set<String> ignoreContainerNames = new HashSet<>();
            Container[] containers = getContainers();
            for (int i = 1; i <= number; i++) {
                NameValidator validator = Containers.createNameValidator(containers, ignoreContainerNames);
                String containerName = Containers.createUniqueContainerName(containers, originalName, validator);
                ignoreContainerNames.add(containerName);

                optionsMap.put("name", containerName);

                //Check if datastore configuration has been specified and fallback to current container settings.
                if (!hasValidDataStoreProperties(optionsMap)) {
                    optionsMap.put("dataStoreProperties", profileRegistry.get().getDataStoreProperties());
                }
                Class cl = options.getClass().getClassLoader().loadClass(options.getClass().getName() + "$Builder");
                CreateContainerBasicOptions.Builder builder = (CreateContainerBasicOptions.Builder) mapper.readValue(mapper.writeValueAsString(optionsMap), cl);
                //We always want to pass the obfuscated version of the password to the container provider.
                builder = (CreateContainerBasicOptions.Builder) builder.zookeeperPassword(PasswordEncoder.encode(getZookeeperPassword()));
                final CreateContainerOptions containerOptions = builder.build();
                final CreationStateListener containerListener = listener;
                final FabricService fabricService = this;
                new Thread("Creating container " + containerName) {
                    public void run() {
                        try {
                            dataStore.get().createContainerConfig(containerOptions);
                            CreateContainerMetadata metadata = provider.create(containerOptions, containerListener);
                            if (metadata.isSuccess()) {
                                Container parent = containerOptions.getParent() != null ? getContainer(containerOptions.getParent()) : null;
                                //An ensemble server can be created without an existing ensemble.
                                //In this case container config will be created by the newly created container.
                                //TODO: We need to make sure that this entries are somehow added even to ensemble servers.
                                if (!containerOptions.isEnsembleServer()) {
                                    dataStore.get().createContainerConfig(metadata);
                                }
                                ContainerImpl container = new ContainerImpl(parent, metadata.getContainerName(), FabricServiceImpl.this);
                                metadata.setContainer(container);
                                LOGGER.info("The container " + metadata.getContainerName() + " has been successfully created");
                            } else {
                                LOGGER.info("The creation of the container " + metadata.getContainerName() + " has failed", metadata.getFailure());
                            }
                            metadatas.add(metadata);
                        } catch (Throwable t) {
                            CreateContainerBasicMetadata metadata = new CreateContainerBasicMetadata();
                            metadata.setCreateOptions(containerOptions);
                            metadata.setFailure(t);
                            metadatas.add(metadata);
                            dataStore.get().deleteContainer(fabricService, containerOptions.getName());
                        } finally {
                            latch.countDown();
                        }
                    }
                }.start();
            }
            if (!latch.await(30, TimeUnit.MINUTES)) {
                throw new FabricException("Timeout waiting for container creation");
            }
            return metadatas.toArray(new CreateContainerMetadata[metadatas.size()]);
        } catch (Exception e) {
            LOGGER.error("Failed to create containers " + e, e);
            throw FabricException.launderThrowable(e);
        }
    }

    @Override
    public Set<Class<? extends CreateContainerBasicOptions>> getSupportedCreateContainerOptionTypes() {
        assertValid();
        Set<Class<? extends CreateContainerBasicOptions>> optionTypes = new HashSet<Class<? extends CreateContainerBasicOptions>>();
        for (Map.Entry<String, ContainerProvider> entry : providers.entrySet()) {
            optionTypes.add(entry.getValue().getOptionsType());
        }
        return optionTypes;
    }

    @Override
    public Set<Class<? extends CreateContainerBasicMetadata>> getSupportedCreateContainerMetadataTypes() {
        assertValid();
        Set<Class<? extends CreateContainerBasicMetadata>> metadataTypes = new HashSet<Class<? extends CreateContainerBasicMetadata>>();
        for (Map.Entry<String, ContainerProvider> entry : providers.entrySet()) {
            metadataTypes.add(entry.getValue().getMetadataType());
        }
        return metadataTypes;
    }

    @Override
    public ContainerProvider getProvider(final String scheme) {
        return providers.get(scheme);
    }

    // FIXME public access on the impl
    public Map<String, ContainerProvider> getProviders() {
        assertValid();
        return Collections.unmodifiableMap(providers);
    }

    @Override
    public String getRestAPI() {
        assertValid();
        String restApiFolder = ZkPath.REST_API_CLUSTERS.getPath("FabricResource/fabric8");
        try {
            CuratorFramework curatorFramework = curator.get();
            if (curatorFramework != null) {
                List<String> versions = getChildrenSafe(curatorFramework, restApiFolder);
                for (String version : versions) {
                    String versionPath = restApiFolder + "/" + version;
                    List<String> containers = getChildrenSafe(curatorFramework, versionPath);
                    for (String container : containers) {
                        String containerPath = versionPath + "/" + container;
                        String answer = getFirstService(containerPath);
                        if (!Strings.isNullOrEmpty(answer)) {
                            return answer;
                        }
                    }
                }
            }
        } catch (Exception e) {
            //On exception just return uri.
            LOGGER.warn("Failed to find API " + restApiFolder + ". " + e, e);
        }
        return null;
    }

    protected String getFirstService(String containerPath) throws Exception {
    CuratorFramework curatorFramework = curator.get();
        if (curatorFramework != null) {
            byte[] data = curatorFramework.getData().forPath(containerPath);
            if (data != null && data.length > 0) {
                String text = new String(data).trim();
                if (!text.isEmpty()) {
                    ObjectMapper mapper = new ObjectMapper();
                    Map<String, Object> map = mapper.readValue(data, HashMap.class);
                    Object serviceValue = map.get("services");
                    if (serviceValue instanceof List) {
                        List services = (List) serviceValue;
                        if (services != null) {
                            if (!services.isEmpty()) {
                                List<String> serviceTexts = new ArrayList<String>();
                                for (Object service : services) {
                                    String serviceText = getSubstitutedData(curatorFramework, service.toString());
                                    if (io.fabric8.common.util.Strings.isNotBlank(serviceText)) {
                                        return serviceText;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    @Override
    public String getGitUrl() {
        assertValid();
        String restApiFolder = ZkPath.GIT.getPath();
        try {
            CuratorFramework curatorFramework = curator.get();
            if (curatorFramework != null) {
                List<String> versions = getChildrenSafe(curatorFramework, restApiFolder);
                for (String version : versions) {
                    String versionPath = restApiFolder + "/" + version;
                    String answer = getFirstService(versionPath);
                    if (!Strings.isNullOrEmpty(answer)) {
                        return answer;
                    }
                }
            }
        } catch (Exception e) {
            //On exception just return uri.
            LOGGER.warn("Failed to find API " + restApiFolder + ". " + e, e);
        }
        return null;
    }

    @Override
    public String getWebConsoleUrl() {
        Container[] containers = null;
        try {
            containers = getContainers();
        } catch (Exception e) {
            LOGGER.debug("Ignored exception trying to find containers: " + e, e);
            return null;
        }
        for (Container aContainer : containers) {
            Profile[] profiles = aContainer.getProfiles();
            for (Profile aProfile : profiles) {
                String id = aProfile.getId();
                if (id.equals("fabric")) {
                    return profileWebAppURL("io.hawt.hawtio-web", id, aProfile.getVersion());
                }
            }
        }
        return null;
    }

    @Override
    public URI getMavenRepoURI() {
        assertValid();
        URI uri = URI.create(getDefaultRepo());
        try {
            if (exists(curator.get(), ZkPath.MAVEN_PROXY.getPath("download")) != null) {
                List<String> children = getChildren(curator.get(), ZkPath.MAVEN_PROXY.getPath("download"));
                if (children != null && !children.isEmpty()) {
                    Collections.sort(children);

                    String mavenRepo = getSubstitutedPath(curator.get(), ZkPath.MAVEN_PROXY.getPath("download") + "/" + children.get(0));
                    if (mavenRepo != null && !mavenRepo.endsWith("/")) {
                        mavenRepo += "/";
                    }
                    uri = new URI(mavenRepo);
                }
            }
        } catch (Exception e) {
            //On exception just return uri.
        }
        return uri;
    }

    @Override
    public List<URI> getMavenRepoURIs() {
        assertValid();
        try {
            List<URI> uris = new ArrayList<URI>();
            if (exists(curator.get(), ZkPath.MAVEN_PROXY.getPath("download")) != null) {
                List<String> children = getChildren(curator.get(), ZkPath.MAVEN_PROXY.getPath("download"));
                if (children != null && !children.isEmpty()) {
                    Collections.sort(children);
                }
                if (children != null) {
                    for (String child : children) {
                        String mavenRepo = getSubstitutedPath(curator.get(), ZkPath.MAVEN_PROXY.getPath("download") + "/" + child);
                        if (mavenRepo != null && !mavenRepo.endsWith("/")) {
                            mavenRepo += "/";
                        }
                        uris.add(new URI(mavenRepo));
                    }
                }
            }
            return uris;
        } catch (Exception e) {
            throw FabricException.launderThrowable(e);
        }
    }

    @Override
    public URI getMavenRepoUploadURI() {
        assertValid();
        URI uri = URI.create(getDefaultRepo());
        try {
            if (exists(curator.get(), ZkPath.MAVEN_PROXY.getPath("upload")) != null) {
                List<String> children = getChildren(curator.get(), ZkPath.MAVEN_PROXY.getPath("upload"));
                if (children != null && !children.isEmpty()) {
                    Collections.sort(children);
    
                    String mavenRepo = getSubstitutedPath(curator.get(), ZkPath.MAVEN_PROXY.getPath("upload") + "/" + children.get(0));
                    if (mavenRepo != null && !mavenRepo.endsWith("/")) {
                        mavenRepo += "/";
                    }
                    uri = new URI(mavenRepo);
                }
            }
        } catch (Exception e) {
            //On exception just return uri.
        }
        return uri;
    }


    @Override
    public String profileWebAppURL(String webAppId, String profileId, String versionId) {
        if (versionId == null || versionId.length() == 0) {
            Version version = getDefaultVersion();
            if (version != null) {
                versionId = version.getId();
            }
        }
        List<Container> containers = Containers.containersForProfile(getContainers(), profileId, versionId);
        for (Container container : containers) {
            String url = containerWebAppURL(webAppId, container.getId());
            if (url != null && url.length() > 0) {
                return url;
            }
        }
        return null;
    }


    public String containerWebAppURL(String webAppId, String name) {
        assertValid();
        // lets try both the webapps and servlets area
        String answer = containerWebAppUrl(ZkPath.WEBAPPS_CLUSTER.getPath(webAppId), name);
        if (answer == null) {
            answer = containerWebAppUrl(ZkPath.SERVLETS_CLUSTER.getPath(webAppId), name);
        }
        return answer;

    }

    private String containerWebAppUrl(String versionsPath, String name) {
        try {
            if (exists(curator.get(), versionsPath) != null) {
                List<String> children = getChildren(curator.get(), versionsPath);
                if (children != null && !children.isEmpty()) {
                    for (String child : children) {
                        if (Strings.isNullOrEmpty(name)) {
                            // lets just use the first container we find
                            String parentPath = versionsPath + "/" + child;
                            List<String> grandChildren = getChildren(curator.get(), parentPath);
                            if (!grandChildren.isEmpty()) {
                                String containerPath = parentPath + "/" + grandChildren.get(0);
                                String answer = getWebUrl(containerPath);
                                if (!Strings.isNullOrEmpty(answer)) {
                                    return answer;
                                }
                            }
                        } else {
                            String childPath = versionsPath + "/" + child;
                            String containerPath = childPath + "/" + name;
                            String answer = getWebUrl(containerPath);
                            if (Strings.isNullOrEmpty(answer)) {
                                // lets recurse into a child folder just in case
                                // or in the case of servlet paths where there may be extra levels of depth
                                answer = containerWebAppUrl(childPath, name);
                            }
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
        return null;
    }

    private String getWebUrl(String containerPath) throws Exception {
        if (curator.get().checkExists().forPath(containerPath) != null) {
            byte[] bytes = ZkPath.loadURL(curator.get(), containerPath);
            String text = new String(bytes);
            // NOTE this is a bit naughty, we should probably be doing
            // Jackson parsing here; but we only need 1 String and
            // this avoids the jackson runtime dependency - its just a bit brittle
            // only finding http endpoints and all
            String prefix = "\"services\":[\"";
            int idx = text.indexOf(prefix);
            String answer = text;
            if (idx > 0) {
                int startIndex = idx + prefix.length();
                int endIdx = text.indexOf("\"]", startIndex);
                if (endIdx > 0) {
                    answer = text.substring(startIndex, endIdx);
                    if (answer.length() > 0) {
                        // lets expand any variables
                        answer = ZooKeeperUtils.getSubstitutedData(curator.get(), answer);
                        return answer;
                    }
                }
            }
        }
        return null;
    }

    private static boolean hasValidDataStoreProperties(Map options) {
        if (!options.containsKey("dataStoreProperties")) {
            return false;
        }

        Object props = options.get("dataStoreProperties");
        if (props instanceof Map) {
            return !((Map) props).isEmpty();
        } else {
            return false;
        }
    }

    // FIXME public access on the impl
    public void registerProvider(String scheme, ContainerProvider provider) {
        assertValid();
        providers.put(scheme, provider);
    }

    // FIXME public access on the impl
    public void registerProvider(ContainerProvider provider, Map<String, Object> properties) {
        assertValid();
        String scheme = (String) properties.get(io.fabric8.utils.Constants.PROTOCOL);
        registerProvider(scheme, provider);
    }

    // FIXME public access on the impl
    public void unregisterProvider(String scheme) {
        assertValid();
        providers.remove(scheme);
    }

    // FIXME public access on the impl
    public void unregisterProvider(ContainerProvider provider, Map<String, Object> properties) {
        assertValid();
        String scheme = (String) properties.get(io.fabric8.utils.Constants.PROTOCOL);
        unregisterProvider(scheme);
    }

    @Override
    public String getZookeeperUrl() {
        assertValid();
        return getZookeeperInfo("zookeeper.url");
    }

    @Override
    public String getZooKeeperUser() {
        assertValid();
        String answer = null;
        try {
            answer = getZookeeperInfo("zookeeper.user");
        } catch (Exception e) {
            LOGGER.warn("could not find zookeeper.user: " + e, e);
        }
        if (Strings.isNullOrEmpty(answer)) {
            answer = "admin";
        }
        return answer;
    }

    @Override
    public String getZookeeperPassword() {
        assertValid();
        String rawZookeeperPassword = getZookeeperInfo("zookeeper.password");
        if (rawZookeeperPassword != null) {
            return PasswordEncoder.decode(rawZookeeperPassword);
        } else {
            return null;
        }
    }

    // FIXME public access on the impl
    public String getZookeeperInfo(String name) {
        assertValid();
        String zooKeeperUrl = null;
        //We are looking directly for at the zookeeper for the url, since container might not even be mananaged.
        //Also this is required for the integration with the IDE.
        try {
            if (curator.get().getZookeeperClient().isConnected()) {
                Version defaultVersion = getDefaultVersion();
                if (defaultVersion != null) {
                    Profile profile = defaultVersion.getRequiredProfile("default");
                    if (profile != null) {
                        Map<String, String> zookeeperConfig = profile.getConfiguration(Constants.ZOOKEEPER_CLIENT_PID);
                        if (zookeeperConfig != null) {
                            zooKeeperUrl = getSubstitutedData(curator.get(), zookeeperConfig.get(name));
                        }
                    }
                }
            }
        } catch (Exception e) {
            //Ignore it.
        }

        if (zooKeeperUrl == null) {
            try {
                Configuration config = configAdmin.get().getConfiguration(Constants.ZOOKEEPER_CLIENT_PID, null);
                zooKeeperUrl = (String) config.getProperties().get(name);
            } catch (Exception e) {
                //Ignore it.
            }
        }
        return zooKeeperUrl;
    }

    @Override
    public String getDefaultVersionId() {
        assertValid();
        return dataStore.get().getDefaultVersion();
    }

    @Override
    public Version getDefaultVersion() {
        assertValid();
        String versionId = dataStore.get().getDefaultVersion();
        return profileService.get().getVersion(versionId);
    }

    @Override
    public Version getRequiredDefaultVersion() {
        assertValid();
        String versionId = dataStore.get().getDefaultVersion();
        return profileService.get().getRequiredVersion(versionId);
    }

	@Override
	public void setDefaultVersionId(String versionId) {
        assertValid();
        dataStore.get().setDefaultVersion(versionId);
	}

    @Override
    public void setRequirements(FabricRequirements requirements) throws IOException {
        assertValid();
        validateRequirements(this, requirements);
        dataStore.get().setRequirements(requirements);
    }

    /**
     * Validates that the requirements are valid; to ensure the profiles exist etc
     */
    public static void validateRequirements(FabricService fabricService, FabricRequirements requirements) {
        ProfileService profileService = fabricService.adapt(ProfileService.class);
        String versionId = requirements.getVersion();
        Version version;
        if (!Strings.isNullOrEmpty(versionId)) {
            version = profileService.getRequiredVersion(versionId);
        } else {
            version = fabricService.getDefaultVersion();
        }
        Set<String> profileIds = new HashSet<String>(Profiles.profileIds(version.getProfiles()));
        List<ProfileRequirements> profileRequirements = requirements.getProfileRequirements();
        for (ProfileRequirements profileRequirement : profileRequirements) {
            validateProfileRequirements(fabricService, requirements, profileRequirement, profileIds);
        }
    }

    protected static void validateProfileRequirements(FabricService fabricService, FabricRequirements requirements, ProfileRequirements profileRequirement, Set<String> profileIds) {
        profileRequirement.validate();
        assertValidProfileId(profileIds, profileRequirement.getProfile());
        List<String> dependentProfiles = profileRequirement.getDependentProfiles();
        if (dependentProfiles != null) {
            for (String dependentProfile : dependentProfiles) {
                assertValidProfileId(profileIds, dependentProfile);
            }
        }
    }

    @Override
    public FabricRequirements getRequirements() {
        assertValid();
        FabricRequirements requirements = dataStore.get().getRequirements();
        requirements.setVersion(getDefaultVersionId());
        return requirements;
    }

    @Override
    public AutoScaleStatus getAutoScaleStatus() {
        assertValid();
        return dataStore.get().getAutoScaleStatus();
    }

    @Override
    public FabricStatus getFabricStatus() {
        assertValid();
        return new FabricStatus(this);
    }

    @Override
    public PatchService getPatchService() {
        assertValid();
        return new PatchServiceImpl(this);
    }

    @Override
    public String getDefaultJvmOptions() {
        assertValid();
        return dataStore.get().getDefaultJvmOptions();
    }

    @Override
    public void setDefaultJvmOptions(String jvmOptions) {
        assertValid();
        dataStore.get().setDefaultJvmOptions(jvmOptions);
    }

    @Override
    public String getConfigurationValue(String versionId, String profileId, String pid, String key) {
        assertValid();
        Profile pr = profileService.get().getRequiredProfile(versionId, profileId);
        Map<String, byte[]> configs = pr.getFileConfigurations();

        byte[] b = configs.get(pid);

        Properties p = null;
        try {
            if (b != null) {
                p = DataStoreUtils.toProperties(b);
            } else {
                p = new Properties();
            }
        } catch (Throwable t) {
            throw new FabricException(t);
        }

        return p.getProperty(key);
    }

    @Override
    public void setConfigurationValue(String versionId, String profileId, String pid, String key, String value) {
        assertValid();
        Version version = profileService.get().getRequiredVersion(versionId);
        Profile profile = version.getRequiredProfile(profileId);
        
        Map<String, byte[]> configs = profile.getFileConfigurations();
        byte[] bytes = configs.get(pid);
        Properties properties;
        if (bytes != null) {
            properties = DataStoreUtils.toProperties(bytes);
        } else {
            properties = new Properties();
        }
        properties.setProperty(key, value);
        bytes = DataStoreUtils.toBytes(properties);
        configs.put(pid, bytes);
        
        ProfileBuilder builder = ProfileBuilder.Factory.createFrom(profile);
        builder.setFileConfigurations(configs);
        profileService.get().updateProfile(builder.getProfile());
    }

    @Override
    public boolean scaleProfile(String profile, int numberOfInstances) throws IOException {
        if (numberOfInstances == 0) {
            throw new IllegalArgumentException("numberOfInstances should be greater or less than zero");
        }
        FabricRequirements requirements = getRequirements();
        ProfileRequirements profileRequirements = requirements.getOrCreateProfileRequirement(profile);
        Integer minimumInstances = profileRequirements.getMinimumInstances();
        List<Container> containers = Containers.containersForProfile(getContainers(), profile);
        int containerCount = containers.size();
        int newCount = containerCount + numberOfInstances;
        if (newCount < 0) {
            newCount = 0;
        }
        boolean update = minimumInstances == null || newCount != minimumInstances;
        if (update) {
            profileRequirements.setMinimumInstances(newCount);
            setRequirements(requirements);
        }
        return update;
    }

    @Override
    public ContainerAutoScaler createContainerAutoScaler(FabricRequirements requirements, ProfileRequirements profileRequirements) {
        Collection<ContainerProvider> providerCollection = getProviders().values();
        for (ContainerProvider containerProvider : providerCollection) {
            // lets pick the highest weighted autoscaler (e.g. to prefer openshift to docker to child
            SortedMap<Integer, ContainerAutoScaler> sortedAutoScalers = new TreeMap<Integer, ContainerAutoScaler>();
            if (containerProvider instanceof ContainerAutoScalerFactory) {
                ContainerAutoScalerFactory provider = (ContainerAutoScalerFactory) containerProvider;
                ContainerAutoScaler autoScaler = provider.createAutoScaler(requirements, profileRequirements);
                if (autoScaler != null) {
                    int weight = autoScaler.getWeight();
                    sortedAutoScalers.put(weight, autoScaler);
                }
            }
            if (!sortedAutoScalers.isEmpty()) {
                Integer key = sortedAutoScalers.lastKey();
                if (key != null) {
                    return sortedAutoScalers.get(key);
                }
            }
        }
        return null;
    }

    /**
     * Performs substitution to configuration based on the registered {@link PlaceholderResolver} instances.
     */
    public Map<String, Map<String, String>> substituteConfigurations(final Map<String, Map<String, String>> configurations) {

        final Map<String, PlaceholderResolver> resolversSnapshot = new HashMap<String, PlaceholderResolver>(placeholderResolvers);

        // Check that all resolvers are available
        Set<String> requiredSchemes = getSchemesForProfileConfigurations(configurations);
        Set<String> availableSchemes = resolversSnapshot.keySet();
        if (!availableSchemes.containsAll(requiredSchemes)) {
            StringBuilder sb = new StringBuilder();
            sb.append("Missing Placeholder Resolvers:");
            for (String scheme : requiredSchemes) {
                if (!availableSchemes.contains(scheme)) {
                    sb.append(" ").append(scheme);
                }
            }
            throw new FabricException(sb.toString());
        }

        final Map<String, Map<String, String>> mutableConfigurations = new HashMap<>();
        for (Entry<String, Map<String, String>> entry : configurations.entrySet()) {
            String key = entry.getKey();
            Map<String, String> value = new HashMap<>(entry.getValue());
            mutableConfigurations.put(key, value);
        }
        
        final FabricService fabricService = this;
        for (Map.Entry<String, Map<String, String>> entry : mutableConfigurations.entrySet()) {
            final String pid = entry.getKey();
            Map<String, String> props = entry.getValue();
            for (Map.Entry<String, String> e : props.entrySet()) {
                final String key = e.getKey();
                final String value = e.getValue();
                props.put(key, InterpolationHelper.substVars(value, key, null, props, new InterpolationHelper.SubstitutionCallback() {
                    public String getValue(String toSubstitute) {
                        if (toSubstitute != null && toSubstitute.contains(":")) {
                            String scheme = toSubstitute.substring(0, toSubstitute.indexOf(":"));
                            return resolversSnapshot.get(scheme).resolve(fabricService, mutableConfigurations, pid, key, toSubstitute);
                        }
                        return substituteBundleProperty(toSubstitute, bundleContext);
                    }
                }));
            }
        }
        
        return mutableConfigurations;
    }

    void bindConfigAdmin(ConfigurationAdmin service) {
        this.configAdmin.bind(service);
    }
    void unbindConfigAdmin(ConfigurationAdmin service) {
        this.configAdmin.unbind(service);
    }

    @VisibleForTesting
    public void bindRuntimeProperties(RuntimeProperties service) {
        this.runtimeProperties.bind(service);
    }
    void unbindRuntimeProperties(RuntimeProperties service) {
        this.runtimeProperties.unbind(service);
    }

    @VisibleForTesting
    public void bindCurator(CuratorFramework service) {
        this.curator.bind(service);
    }
    void unbindCurator(CuratorFramework service) {
        this.curator.unbind(service);
    }

    @VisibleForTesting
    public void bindDataStore(DataStore service) {
        this.dataStore.bind(service);
    }
    void unbindDataStore(DataStore service) {
        this.dataStore.unbind(service);
    }

    void bindPortService(PortService service) {
        this.portService.bind(service);
    }
    void unbindPortService(PortService service) {
        this.portService.unbind(service);
    }

    void bindProfileService(ProfileService service) {
        profileService.bind(service);
    }
    void unbindProfileService(ProfileService service) {
        profileService.unbind(service);
    }
    
    void bindProfileRegistry(ProfileRegistry service) {
        this.profileRegistry.bind(service);
    }
    void unbindProfileRegistry(ProfileRegistry service) {
        this.profileRegistry.unbind(service);
    }
    
    void bindProvider(ContainerProvider provider) {
        providers.put(provider.getScheme(), provider);
    }
    void unbindProvider(ContainerProvider provider) {
        providers.remove(provider.getScheme());
    }

    @VisibleForTesting
    public void bindPlaceholderResolver(PlaceholderResolver resolver) {
        String resolverScheme = resolver.getScheme();
        placeholderResolvers.put(resolverScheme, resolver);
    }
    void unbindPlaceholderResolver(PlaceholderResolver resolver) {
        String resolverScheme = resolver.getScheme();
        placeholderResolvers.remove(resolverScheme);
    }
}
