/**
 * Copyright (C) 2011, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.fabric.service;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.karaf.admin.management.AdminServiceMBean;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooDefs;
import org.fusesource.fabric.api.Agent;
import org.fusesource.fabric.api.AgentService;
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

    public Map<String, Agent> getAgents() throws Exception {
        Map<String, Agent> agents = new HashMap<String, Agent>();

        List<String> configs = zooKeeper.getChildren("/fabric/registry/agents/config");
        for (String name : configs) {
            String root = zooKeeper.getStringData("/fabric/registry/agents/config/" + name + "/root").trim();
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
    }

    public void startAgent(final Agent agent) throws Exception {
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

    public void stopAgent(final Agent agent) throws Exception {
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

    public Agent createChild(final Agent parent, final String name) throws Exception {
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

    public void destroy(Agent agent) throws Exception {
        if (agent.getParent() != null) {
            destroyChild(agent.getParent(), agent.getId());
        } else {
            throw new UnsupportedOperationException();
        }
    }

    private void destroyChild(final Agent parent, final String name) throws Exception {
        jmxTemplate.execute(parent, new JmxTemplate.AdminServiceCallback<Object>() {
            public Object doWithAdminService(AdminServiceMBean adminService) throws Exception {
                adminService.stopInstance(name);
                adminService.destroyInstance(name);
                zooKeeper.deleteWithChildren("/fabric/configs/agents/" + name);
                return null;
            }
        });
    }

    private String getZooKeeperUrl() throws IOException {
        Configuration config = configurationAdmin.getConfiguration("org.fusesource.fabric.zookeeper", null);
        final String zooKeeperUrl = (String) config.getProperties().get("zookeeper.url");
        if (zooKeeperUrl == null) {
            throw new IllegalStateException("Unable to find the zookeeper url");
        }
        return zooKeeperUrl;
    }

    private void createAgentConfig(String name) throws InterruptedException, KeeperException {
        if (zooKeeper.exists("/fabric/configs/agents/" + name) == null) {
            zooKeeper.createWithParents( "/fabric/configs/agents/" + name, configVersion, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT );
        }
        if (zooKeeper.exists("/fabric/configs/versions/" + configVersion + "/agents/" + name) == null) {
            zooKeeper.createWithParents( "/fabric/configs/versions/" + configVersion + "/agents/" + name, profile, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT );
        }
    }


}
