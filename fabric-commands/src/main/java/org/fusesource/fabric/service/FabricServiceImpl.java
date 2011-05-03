/**
 * Copyright (C) 2011, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.fabric.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.karaf.admin.management.AdminServiceMBean;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.ZooDefs;
import org.fusesource.fabric.api.Agent;
import org.fusesource.fabric.api.FabricException;
import org.fusesource.fabric.api.FabricService;
import org.fusesource.fabric.api.Profile;
import org.fusesource.fabric.api.Version;
import org.fusesource.fabric.zookeeper.ZkPath;
import org.linkedin.zookeeper.client.IZKClient;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

public class FabricServiceImpl implements FabricService {

    public static final String DEFAULT_VERSION = "base";
    private static final String DEFAULT_PROFILE = "default";

    private IZKClient zooKeeper;
    private ConfigurationAdmin configurationAdmin;
    private String profile = DEFAULT_PROFILE;
    private JmxTemplate jmxTemplate = new JmxTemplate();

    public IZKClient getZooKeeper() {
        return zooKeeper;
    }

    public void setZooKeeper(IZKClient zooKeeper) {
        this.zooKeeper = zooKeeper;
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

            List<String> configs = zooKeeper.getChildren(ZkPath.AGENTS.getPath());
            for (String name : configs) {
                String root = zooKeeper.getStringData(ZkPath.AGENT_ROOT.getPath(name)).trim();
                if (root.isEmpty()) {
                    if (!agents.containsKey( name )) {
                        Agent agent = new AgentImpl(null, name, this);
                        agents.put( name, agent );
                    }
                } else {
                    Agent parent = agents.get( root );
                    if (parent == null) {
                        parent = new AgentImpl(null, root, this);
                        agents.put( root, parent );
                    }
                    Agent agent = new AgentImpl(parent, name, this);
                    agents.put( name, agent );
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
            return new AgentImpl( root.isEmpty() ? null : getAgent( root ), name, this );
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
        jmxTemplate.execute(agent.getParent(), new JmxTemplate.AdminServiceCallback<Object>() {
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
        jmxTemplate.execute(agent.getParent(), new JmxTemplate.AdminServiceCallback<Object>() {
            public Object doWithAdminService(AdminServiceMBean adminService) throws Exception {
                adminService.stopInstance(agent.getId());
                return null;
            }
        });
    }

    public Agent createAgent(final String name) {
        throw new UnsupportedOperationException("Creation of root agents is not supported");
    }

    public Agent createAgent(final Agent parent, final String name) {
        final String zooKeeperUrl = getZooKeeperUrl();
        createAgentConfig(name);
        return jmxTemplate.execute(parent, new JmxTemplate.AdminServiceCallback<Agent>() {
            public Agent doWithAdminService(AdminServiceMBean adminService) throws Exception {
                String javaOpts = zooKeeperUrl != null ? "-Dzookeeper.url=\"" + zooKeeperUrl + "\" -Xmx512M -server" : "";
                String features = "fabric-agent";
                String featuresUrls = "mvn:org.fusesource.fabric/fabric-distro/1.0-SNAPSHOT/xml/features";
                adminService.createInstance(name, 0, 0, 0, null, javaOpts, features, featuresUrls);
                adminService.startInstance(name, null);
                return new AgentImpl(parent, name, FabricServiceImpl.this);
            }
        });
    }

    public void destroy(Agent agent) {
        if (agent.getParent() != null) {
            destroyChild(agent.getParent(), agent.getId());
        } else {
            throw new UnsupportedOperationException();
        }
    }

    private void destroyChild(final Agent parent, final String name) {
        jmxTemplate.execute(parent, new JmxTemplate.AdminServiceCallback<Object>() {
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

    private void createAgentConfig(String name) {
        try {
            String configVersion = getDefaultVersion().getName();
            ZooKeeperUtils.createDefault( zooKeeper, ZkPath.CONFIG_AGENT.getPath(name), configVersion );
            ZooKeeperUtils.createDefault( zooKeeper, ZkPath.CONFIG_VERSIONS_AGENT.getPath(configVersion, name), profile );
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
                ZooKeeperUtils.createDefault( zooKeeper, ZkPath.CONFIG_DEFAULT_VERSION.getPath(), version );
                ZooKeeperUtils.createDefault( zooKeeper, ZkPath.CONFIG_VERSION.getPath(version), null );
            }
            return new VersionImpl( version, this );
        } catch (Exception e) {
            throw new FabricException(e);
        }
    }

    @Override
    public void setDefaultVersion( Version version ) {
        try {
            ZooKeeperUtils.set( zooKeeper, ZkPath.CONFIG_DEFAULT_VERSION.getPath(), version.getName() );
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
            ZooKeeperUtils.create( zooKeeper, ZkPath.CONFIG_VERSIONS_PROFILE.getPath(version, name) );
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
}
