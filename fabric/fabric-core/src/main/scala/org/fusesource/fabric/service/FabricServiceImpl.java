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

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.karaf.admin.management.AdminServiceMBean;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooDefs;
import org.fusesource.fabric.api.Container;
import org.fusesource.fabric.api.ContainerProvider;
import org.fusesource.fabric.api.CreateContainerArguments;
import org.fusesource.fabric.api.FabricException;
import org.fusesource.fabric.api.FabricService;
import org.fusesource.fabric.api.Profile;
import org.fusesource.fabric.api.Version;
import org.fusesource.fabric.internal.ContainerImpl;
import org.fusesource.fabric.internal.FabricConstants;
import org.fusesource.fabric.internal.ProfileImpl;
import org.fusesource.fabric.internal.VersionImpl;
import org.fusesource.fabric.internal.ZooKeeperUtils;
import org.fusesource.fabric.zookeeper.ZkDefs;
import org.fusesource.fabric.zookeeper.ZkPath;
import org.linkedin.zookeeper.client.IZKClient;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectInstance;
import javax.management.ObjectName;

import static org.fusesource.fabric.zookeeper.ZkPath.CONTAINER_PARENT;

public class FabricServiceImpl implements FabricService, FabricServiceImplMBean {
    private transient Logger logger = LoggerFactory.getLogger(FabricServiceImpl.class);

    private IZKClient zooKeeper;
    private Map<String, ContainerProvider> providers;
    private ConfigurationAdmin configurationAdmin;
    private String profile = ZkDefs.DEFAULT_PROFILE;
    private ObjectName mbeanName;
    private String userName = "admin";
    private String password = "admin";

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
        if (container.isRoot()) {
            throw new IllegalArgumentException("Cannot start root containers");
        }
        getContainerTemplate(container.getParent()).execute(new ContainerTemplate.AdminServiceCallback<Object>() {
            public Object doWithAdminService(AdminServiceMBean adminService) throws Exception {
                adminService.startInstance(container.getId(), null);
                return null;
            }
        });
    }

    public void stopContainer(final Container container) {
        if (container.isRoot()) {
            throw new IllegalArgumentException("Cannot stop root containers");
        }
        getContainerTemplate(container.getParent()).execute(new ContainerTemplate.AdminServiceCallback<Object>() {
            public Object doWithAdminService(AdminServiceMBean adminService) throws Exception {
                adminService.stopInstance(container.getId());
                return null;
            }
        });
    }


    public Container createContainer(String name) {
        try {
            final String zooKeeperUrl = getZooKeeperUrl();
            createContainerConfig("", name);
            return new ContainerImpl(null, name, FabricServiceImpl.this);
        } catch (FabricException e) {
            throw e;
        } catch (Exception e) {
            throw new FabricException(e);
        }
    }

    public Container createContainer(String url, String name, boolean isEnsembleServer, boolean debugContainer) {
        return createContainers(url, name, isEnsembleServer, debugContainer, 1)[0];
    }

    public Container[] createContainers(String url, String name, boolean isEnsembleServer, boolean debugContainer, int number) {
        Container[] containers = new Container[number];
        try {

            URI uri = URI.create(url);
            ContainerProvider provider = getProvider(uri.getScheme());
            if (provider == null) {
                throw new FabricException("Unable to find an container provider supporting uri '" + url + "'");
            }

            if (!isEnsembleServer) {
                final String zooKeeperUrl = getZooKeeperUrl();

                for (int i = 0; i < number; i++) {
                    String containerName = name;
                    if (number > 1) {
                        containerName += i + 1;
                    }

                    String parent = "";
                    if( provider instanceof ChildContainerProvider) {
                        parent = getParentFromURI(uri);
                    }

                    createContainerConfig(parent, containerName);
                    containers[i] = new ContainerImpl(null, containerName, FabricServiceImpl.this);
                }

                provider.create(getMavenRepoURI(), uri, name, zooKeeperUrl, isEnsembleServer, debugContainer, number);
            } else {
                provider.create(getMavenRepoURI(), uri, name, null, isEnsembleServer, debugContainer, number);
            }
        } catch (FabricException e) {
            throw e;
        } catch (Exception e) {
            throw new FabricException(e);
        }
        return containers;
    }

    public static String getParentFromURI(URI uri) {
        String parent = uri.getHost();
        if (parent == null) {
            parent = uri.getSchemeSpecificPart();
        }
        return parent;
    }

    @Override
    public Container[] createContainer(CreateContainerArguments args, String name, int number) {
        Container[] containers = new Container[number];
        try {
            for (int i = 0; i < number; i++) {
                String containerName = name;
                if (number > 1) {
                    containerName += i + 1;
                }
                containers[i] = createContainer(args, containerName);
            }
        } catch (FabricException e) {
            throw e;
        } catch (Exception e) {
            throw new FabricException(e);
        }
        return containers;
    }

    public Container createContainer(CreateContainerArguments args, String name) {
        try {
            final String zooKeeperUrl = getZooKeeperUrl();
            createContainerConfig("", name);
            Container container = doCreateContainerFromArguments(args, name, zooKeeperUrl);
            if (container == null) {
                throw new IllegalArgumentException("Unknown CreateContainerArguments " + args + " when creating container " + name);
            }
            return container;
        } catch (FabricException e) {
            throw e;
        } catch (Exception e) {
            throw new FabricException(e);
        }
    }

    @Override
    public boolean createRemoteContainer(CreateContainerArguments args, String name) {
        try {
            final String zooKeeperUrl = getZooKeeperUrl();
            Container container = doCreateContainerFromArguments(args, name, zooKeeperUrl);
            return container != null;
        } catch (FabricException e) {
            throw e;
        } catch (Exception e) {
            throw new FabricException(e);
        }
    }

    protected Container doCreateContainerFromArguments(CreateContainerArguments args, String name, String zooKeeperUrl) throws Exception {
        for (ContainerProvider provider : providers.values()) {
            if (provider.create(args, name, zooKeeperUrl)) {
                return new ContainerImpl(null, name, FabricServiceImpl.this);
            }
        }
        return null;
    }

    @Override
    public Container createContainer(final Container parent, final CreateContainerArguments args, final String name) {
        createContainerConfig(parent.getId(), name);
        ContainerTemplate containerTemplate = getContainerTemplate(parent);

        if (containerTemplate.execute(new ContainerTemplate.FabricServiceCallback<Boolean>() {
            public Boolean doWithFabricService(FabricServiceImplMBean fabricService) throws Exception {
                return fabricService.createRemoteContainer(args, name);
            }
        })) {
            return new ContainerImpl(null, name, FabricServiceImpl.this);
        } else {
            return null;
        }
    }

    public Container createContainer(final String url, final String name) {
        return createContainer(url, name, false, false);
    }

    public ContainerProvider getProvider(final String scheme) {
        return providers.get(scheme);
    }

    public Map<String, ContainerProvider> getProviders() {
        return Collections.unmodifiableMap(providers);
    }

    @Override
    public URI getMavenRepoURI() {
        URI uri = null;
        try {
            uri = new URI(DEFAULT_REPO_URI);
            if (zooKeeper.exists(ZkPath.CONFIGS_MAVEN_REPO.getPath()) != null) {
                String mavenRepo = zooKeeper.getStringData(ZkPath.CONFIGS_MAVEN_REPO.getPath());
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

    public Container createContainer(final Container parent, final String name, final boolean debugContainer) {
        final String zooKeeperUrl = getZooKeeperUrl();
        createContainerConfig(parent.getId(), name);
        return getContainerTemplate(parent).execute(new ContainerTemplate.AdminServiceCallback<Container>() {
            public Container doWithAdminService(AdminServiceMBean adminService) throws Exception {
                String javaOpts = zooKeeperUrl != null ? "-Dzookeeper.url=\"" + zooKeeperUrl + "\" -Xmx512M -server" : "";
                if (debugContainer) {
                    javaOpts += ContainerProvider.DEBUG_CONTAINER;
                }
                String features = "fabric-agent";
                String featuresUrls = "mvn:org.fusesource.fabric/fuse-fabric/" + FabricConstants.FABRIC_VERSION + "/xml/features";
                adminService.createInstance(name, 0, 0, 0, null, javaOpts, features, featuresUrls);
                adminService.startInstance(name, null);
                return new ContainerImpl(parent, name, FabricServiceImpl.this);
            }
        });
    }

    public Container createContainer(final Container parent, final String name) {
        return createContainer(parent, name, false);
    }

    public void destroy(Container container) {
        if (container.getParent() != null) {
            destroyChild(container.getParent(), container.getId());
        } else {
            throw new UnsupportedOperationException();
        }
    }

    private void destroyChild(final Container parent, final String name) {
        getContainerTemplate(parent).execute(new ContainerTemplate.AdminServiceCallback<Object>() {
            public Object doWithAdminService(AdminServiceMBean adminService) throws Exception {
                adminService.stopInstance(name);
                adminService.destroyInstance(name);
                zooKeeper.deleteWithChildren(ZkPath.CONFIG_CONTAINER.getPath(name));
                return null;
            }
        });
    }

    private String getZooKeeperUrl() {
        String zooKeeperUrl = null;
        try {
            Configuration config = configurationAdmin.getConfiguration("org.fusesource.fabric.zookeeper", null);
            zooKeeperUrl = (String) config.getProperties().get("zookeeper.url");
            if (zooKeeperUrl == null) {
                throw new IllegalStateException("Unable to find the zookeeper url");
            }

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
