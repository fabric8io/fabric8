/**
 * Copyright (C) 2011, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
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
import org.apache.zookeeper.ZooDefs;
import org.fusesource.fabric.api.Agent;
import org.fusesource.fabric.api.AgentProvider;
import org.fusesource.fabric.api.CreateAgentArguments;
import org.fusesource.fabric.api.FabricException;
import org.fusesource.fabric.api.FabricService;
import org.fusesource.fabric.api.Profile;
import org.fusesource.fabric.api.Version;
import org.fusesource.fabric.internal.AgentImpl;
import org.fusesource.fabric.internal.ProfileImpl;
import org.fusesource.fabric.internal.VersionImpl;
import org.fusesource.fabric.internal.ZooKeeperUtils;
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

import static org.fusesource.fabric.zookeeper.ZkPath.AGENT_ROOT;

public class FabricServiceImpl implements FabricService, FabricServiceImplMBean {
    private transient Logger logger = LoggerFactory.getLogger(FabricServiceImpl.class);

    public static final String DEFAULT_VERSION = "base";
    private static final String DEFAULT_PROFILE = "default";

    private IZKClient zooKeeper;
    private Map<String, AgentProvider> providers;
    private ConfigurationAdmin configurationAdmin;
    private String profile = DEFAULT_PROFILE;
    private ObjectName mbeanName;
    private String userName = "admin";
    private String password = "admin";

    public FabricServiceImpl() {
        providers = new ConcurrentHashMap<String, AgentProvider>();
        providers.put("child", new ChildAgentProvider(this));
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

    public Agent[] getAgents() {
        try {
            Map<String, Agent> agents = new HashMap<String, Agent>();
            List<String> configs = zooKeeper.getChildren(ZkPath.CONFIGS_AGENTS.getPath());
            for (String name : configs) {
                String root = zooKeeper.getStringData(ZkPath.AGENT_ROOT.getPath(name)).trim();
                if (root.isEmpty()) {
                    if (!agents.containsKey(name)) {
                        Agent agent = new AgentImpl(null, name, this);
                        agents.put(name, agent);
                    }
                } else {
                    Agent parent = agents.get(root);
                    if (parent == null) {
                        parent = new AgentImpl(null, root, this);
                        agents.put(root, parent);
                    }
                    Agent agent = new AgentImpl(parent, name, this);
                    agents.put(name, agent);
                }
            }

            return agents.values().toArray(new Agent[agents.size()]);
        } catch (Exception e) {
            throw new FabricException(e);
        }
    }

    public Agent getAgent(String name) {
        try {
            if (zooKeeper.exists(ZkPath.AGENT_ROOT.getPath(name)) == null) {
                throw new FabricException("Agent '" + name + "' does not exist!");
            }
            String root = zooKeeper.getStringData(ZkPath.AGENT_ROOT.getPath(name)).trim();
            return new AgentImpl(root.isEmpty() ? null : getAgent(root), name, this);
        } catch (FabricException e) {
            throw e;
        } catch (Exception e) {
            throw new FabricException(e);
        }
    }

    public void startAgent(final Agent agent) {
        if (agent.isRoot()) {
            throw new IllegalArgumentException("Can not stop root agents");
        }
        getAgentTemplate(agent.getParent()).execute(new AgentTemplate.AdminServiceCallback<Object>() {
            public Object doWithAdminService(AdminServiceMBean adminService) throws Exception {
                adminService.startInstance(agent.getId(), null);
                return null;
            }
        });
    }

    public void stopAgent(final Agent agent) {
        if (agent.isRoot()) {
            throw new IllegalArgumentException("Can not stop root agents");
        }
        getAgentTemplate(agent.getParent()).execute(new AgentTemplate.AdminServiceCallback<Object>() {
            public Object doWithAdminService(AdminServiceMBean adminService) throws Exception {
                adminService.stopInstance(agent.getId());
                return null;
            }
        });
    }


    public Agent createAgent(String name) {
        try {
            final String zooKeeperUrl = getZooKeeperUrl();
            createAgentConfig("",name);
            return new AgentImpl(null, name, FabricServiceImpl.this);
        } catch (FabricException e) {
            throw e;
        } catch (Exception e) {
            throw new FabricException(e);
        }
    }

    public Agent createAgent(String url, String name, boolean debugAgent) {
        try {
            final String zooKeeperUrl = getZooKeeperUrl();
            URI uri = URI.create(url);
            AgentProvider provider = getProvider(uri.getScheme());
            if (provider == null) {
                throw new FabricException("Unable to find an agent provider supporting uri '" + url + "'");
            }
            createAgentConfig("", name);
            provider.create(this, uri, name, zooKeeperUrl,debugAgent);
            return new AgentImpl(null, name, FabricServiceImpl.this);
        } catch (FabricException e) {
            throw e;
        } catch (Exception e) {
            throw new FabricException(e);
        }
    }

    public Agent createAgent(CreateAgentArguments args, String name) {
        try {
            final String zooKeeperUrl = getZooKeeperUrl();
            createAgentConfig("", name);
            Agent agent = doCreateAgentFromArguments(args, name, zooKeeperUrl);
            if (agent == null) {
                throw new IllegalArgumentException("Unknown CreateAgentArguments " + args + " when creating agent " + name);
            }
            return agent;
        } catch (FabricException e) {
            throw e;
        } catch (Exception e) {
            throw new FabricException(e);
        }
    }

    @Override
    public boolean createRemoteAgent(CreateAgentArguments args, String name) {
        try {
            final String zooKeeperUrl = getZooKeeperUrl();
            Agent agent = doCreateAgentFromArguments(args, name, zooKeeperUrl);
            return agent != null;
        } catch (FabricException e) {
            throw e;
        } catch (Exception e) {
            throw new FabricException(e);
        }
    }

    protected Agent doCreateAgentFromArguments(CreateAgentArguments args, String name, String zooKeeperUrl) throws Exception {
        for (AgentProvider provider : providers.values()) {
            if (provider.create(this, args, name, zooKeeperUrl)) {
                return new AgentImpl(null, name, FabricServiceImpl.this);
            }
        }
        return null;
    }

    @Override
    public Agent createAgent(final Agent parent, final CreateAgentArguments args, final String name) {
        createAgentConfig(parent.getId(), name);
        AgentTemplate agentTemplate = getAgentTemplate(parent);

        if (agentTemplate.execute(new AgentTemplate.FabricServiceCallback<Boolean>() {
            public Boolean doWithFabricService(FabricServiceImplMBean fabricService) throws Exception {
                return fabricService.createRemoteAgent(args, name);
            }
        })) {
            return new AgentImpl(null, name, FabricServiceImpl.this);
        } else {
            return null;
        }
    }

    public Agent createAgent(final String url, final String name) {
        return createAgent(url,name,false);
    }

    public AgentProvider getProvider(final String scheme) {
        return providers.get(scheme);
    }

    public Map<String, AgentProvider> getProviders() {
        return Collections.unmodifiableMap(providers);
    }

    @Override
    public URI getMavenRepoURI() {
        // TODO load from ZK!
        return null;
    }

    public void registerProvider(String scheme, AgentProvider provider) {
        providers.put(scheme, provider);
    }

    public void registerProvider(AgentProvider provider, Map<String, Object> properties) {
        String scheme = (String) properties.get(AgentProvider.PROTOCOL);
        registerProvider(scheme, provider);
    }

    public void unregisterProvider(String scheme) {
        if (providers != null && scheme != null) {
            providers.remove(scheme);
        }
    }

    public void unregisterProvider(AgentProvider provider, Map<String, Object> properties) {
        String scheme = (String) properties.get(AgentProvider.PROTOCOL);
        unregisterProvider(scheme);
    }

    public void registerMBeanServer(MBeanServer mbeanServer) {
        try {
            ObjectName name = getMbeanName();
            ObjectInstance objectInstance = mbeanServer.registerMBean(this, name);
        } catch (Exception e) {
            logger.warn("An error occured during mbean server registration: " + e, e);
        }
    }

    public void unregisterMBeanServer(MBeanServer mbeanServer) {
        if (mbeanServer != null) {
            try {
                mbeanServer.unregisterMBean(getMbeanName());
            } catch (Exception e) {
                logger.warn("An error occured during mbean server registration: " + e, e);
            }
        }
    }

    public Agent createAgent(final Agent parent, final String name, final boolean debugAgent) {
        final String zooKeeperUrl = getZooKeeperUrl();
        createAgentConfig(parent.getId(), name);
        return getAgentTemplate(parent).execute(new AgentTemplate.AdminServiceCallback<Agent>() {
            public Agent doWithAdminService(AdminServiceMBean adminService) throws Exception {
                String javaOpts = zooKeeperUrl != null ? "-Dzookeeper.url=\"" + zooKeeperUrl + "\" -Xmx512M -server" : "";
                if(debugAgent) {
                    javaOpts += AgentProvider.DEBUG_AGNET;
                }
                String features = "fabric-agent";
                String featuresUrls = "mvn:org.fusesource.fabric/fabric-distro/1.1-SNAPSHOT/xml/features";
                adminService.createInstance(name, 0, 0, 0, null, javaOpts, features, featuresUrls);
                adminService.startInstance(name, null);
                return new AgentImpl(parent, name, FabricServiceImpl.this);
            }
        });
    }

    public Agent createAgent(final Agent parent, final String name) {
        return createAgent(parent,name,false);
    }

    public void destroy(Agent agent) {
        if (agent.getParent() != null) {
            destroyChild(agent.getParent(), agent.getId());
        } else {
            throw new UnsupportedOperationException();
        }
    }

    private void destroyChild(final Agent parent, final String name) {
        getAgentTemplate(parent).execute(new AgentTemplate.AdminServiceCallback<Object>() {
            public Object doWithAdminService(AdminServiceMBean adminService) throws Exception {
                adminService.stopInstance(name);
                adminService.destroyInstance(name);
                zooKeeper.deleteWithChildren(ZkPath.CONFIG_AGENT.getPath(name));
                return null;
            }
        });
    }

    private String getZooKeeperUrl() {
        try {
            Configuration config = configurationAdmin.getConfiguration("org.fusesource.fabric.zookeeper", null);
            final String zooKeeperUrl = (String) config.getProperties().get("zookeeper.url");
            if (zooKeeperUrl == null) {
                throw new IllegalStateException("Unable to find the zookeeper url");
            }
            return zooKeeperUrl;
        } catch (Exception e) {
            throw new FabricException(e);
        }
    }

    private void createAgentConfig(String parent, String name) {
        try {
            String configVersion = getDefaultVersion().getName();
            ZooKeeperUtils.createDefault(zooKeeper, ZkPath.CONFIG_AGENT.getPath(name), configVersion);
            ZooKeeperUtils.createDefault(zooKeeper, ZkPath.CONFIG_VERSIONS_AGENT.getPath(configVersion, name), profile);
            zooKeeper.createOrSetWithParents(AGENT_ROOT.getPath(name), parent, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
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
                version = DEFAULT_VERSION;
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
            return versions.toArray(new Version[versions.size()]);
        } catch (Exception e) {
            throw new FabricException(e);
        }
    }

    public Version getVersion(String name) {
        try {
            if (zooKeeper.exists(ZkPath.CONFIG_VERSION.getPath(name)) == null) {
                throw new FabricException("Version '" + name + "' does not exist!");
            }
            return new VersionImpl(name, this);
        } catch (FabricException e) {
            throw e;
        } catch (Exception e) {
            throw new FabricException(e);
        }
    }

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

    public Profile createProfile(String version, String name) {
        try {
            ZooKeeperUtils.create(zooKeeper, ZkPath.CONFIG_VERSIONS_PROFILE.getPath(version, name));
            return new ProfileImpl(name, version, this);
        } catch (Exception e) {
            throw new FabricException(e);
        }
    }

    public void deleteProfile(Profile profile) {
        try {
            zooKeeper.deleteWithChildren(ZkPath.CONFIG_VERSIONS_PROFILE.getPath(profile.getVersion(), profile.getId()));
        } catch (Exception e) {
            throw new FabricException(e);
        }
    }

    protected AgentTemplate getAgentTemplate(Agent agent) {
        // there's no point caching the JMX Connector as we are unsure if we'll communicate again with the same agent any time soon
        // though in the future we could possibly pool them
        boolean cacheJmx = false;
        return new AgentTemplate(agent, cacheJmx, userName, password);
    }

}
