/**
 * Copyright (C) 2011, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.fabric.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.karaf.admin.management.AdminServiceMBean;
import org.fusesource.fabric.api.Agent;
import org.fusesource.fabric.api.AgentService;
import org.fusesource.fabric.api.FabricException;
import org.fusesource.fabric.zookeeper.ZkPath;
import org.linkedin.zookeeper.client.IZKClient;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

public class AgentServiceImpl implements AgentService {

    private IZKClient zooKeeper;
    private ConfigurationAdmin configurationAdmin;
    private String configVersion = "base";
    private String profile = "default";
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

    public Map<String, Agent> getAgents() {
        try {
            Map<String, Agent> agents = new HashMap<String, Agent>();

            List<String> configs = zooKeeper.getChildren(ZkPath.AGENTS.getPath());
            for (String name : configs) {
                String root = zooKeeper.getStringData(ZkPath.AGENT_ROOT.getPath(name)).trim();
                if (root.isEmpty()) {
                    if (!agents.containsKey( name )) {
                        Agent agent = new AgentImpl(null, name, zooKeeper);
                        agents.put( name, agent );
                    }
                } else {
                    Agent parent = agents.get( root );
                    if (parent == null) {
                        parent = new AgentImpl(null, root, zooKeeper);
                        agents.put( root, parent );
                    }
                    Agent agent = new AgentImpl(parent, name, zooKeeper);
                    agents.put( name, agent );
                }
            }

            return agents;
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

    public Agent createChild(final Agent parent, final String name) {
        final String zooKeeperUrl = getZooKeeperUrl();
        createAgentConfig(name);
        return jmxTemplate.execute(parent, new JmxTemplate.AdminServiceCallback<Agent>() {
            public Agent doWithAdminService(AdminServiceMBean adminService) throws Exception {
                String javaOpts = zooKeeperUrl != null ? "-Dzookeeper.url=\"" + zooKeeperUrl + "\" -Xmx512M -server" : "";
                String features = "fabric-agent";
                String featuresUrls = "mvn:org.fusesource.fabric/fabric-distro/1.0-SNAPSHOT/xml/features";
                adminService.createInstance(name, 0, 0, 0, null, javaOpts, features, featuresUrls);
                adminService.startInstance(name, null);
                return new AgentImpl(parent, name, zooKeeper);
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
            ZooKeeperUtils.createDefault(zooKeeper, ZkPath.CONFIG_AGENT.getPath(name), configVersion);
            ZooKeeperUtils.createDefault(zooKeeper, ZkPath.CONFIG_VERSIONS_AGENT.getPath(configVersion, name), profile);
        } catch (Exception e) {
            throw new FabricException(e);
        }
    }


}
