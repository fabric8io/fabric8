/**
 * Copyright (C) 2011, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.fabric.service;

import java.io.File;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.fusesource.fabric.api.FabricException;
import org.fusesource.fabric.api.ZooKeeperClusterService;
import org.fusesource.fabric.zookeeper.ZkPath;
import org.linkedin.util.clock.Timespan;
import org.linkedin.zookeeper.client.IZKClient;
import org.linkedin.zookeeper.client.ZKClient;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

import static org.fusesource.fabric.service.ZooKeeperUtils.*;

public class ZooKeeperClusterServiceImpl implements ZooKeeperClusterService {

    private BundleContext bundleContext;
    private ConfigurationAdmin configurationAdmin;
    private IZKClient zooKeeper;
    private String version = "base";

    public BundleContext getBundleContext() {
        return bundleContext;
    }

    public void setBundleContext(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    public ConfigurationAdmin getConfigurationAdmin() {
        return configurationAdmin;
    }

    public void setConfigurationAdmin(ConfigurationAdmin configurationAdmin) {
        this.configurationAdmin = configurationAdmin;
    }

    public IZKClient getZooKeeper() {
        return zooKeeper;
    }

    public void setZooKeeper(IZKClient zooKeeper) {
        this.zooKeeper = zooKeeper;
    }

    public void createLocalServer() {
        createLocalServer(2181);
    }

    public void createLocalServer(int port) {
        ZKClient client = null;
        try {
            String karafName = System.getProperty("karaf.name");
            Configuration config = configurationAdmin.createFactoryConfiguration("org.fusesource.fabric.zookeeper.server");
            Properties properties = new Properties();
            properties.put("tickTime",  "2000");
            properties.put("initLimit", "10");
            properties.put("syncLimit", "5");
            properties.put("dataDir", "data/zookeeper/0000");
            properties.put("clientPort", Integer.toString(port));
            properties.put("fabric.zookeeper.pid", "org.fusesource.fabric.zookeeper.server-0000");
            config.setBundleLocation(null);
            config.update(properties);
            config = configurationAdmin.getConfiguration("org.fusesource.fabric.zookeeper");
            properties = new Properties();
            String connectionUrl = getLocalHostAddress() + ":" + Integer.toString(port);
            properties.put("zookeeper.url", connectionUrl);
            config.setBundleLocation(null);
            config.update(properties);

            client = new ZKClient(connectionUrl, Timespan.ONE_MINUTE, null);
            client.start();
            client.waitForStart();

            String defaultProfile = ZkPath.CONFIG_VERSIONS_PROFILE.getPath(version, "default");
            set(client, defaultProfile + "/org.fusesource.fabric.zookeeper/zookeeper.url", "${zk:" + karafName + "/ip}:" + Integer.toString(port));

            String profileNode = ZkPath.CONFIG_VERSIONS_PROFILE.getPath(version, "zk-server-0000") + "/org.fusesource.fabric.zookeeper.server-0000";
            set( client, profileNode + "/tickTime", "2000" );
            set( client, profileNode + "/initLimit", "10" );
            set( client, profileNode + "/syncLimit", "5" );
            set( client, profileNode + "/dataDir", "data/zookeeper/0000" );

            set( client, ZkPath.CONFIG_VERSIONS_PROFILE.getPath(version, "zk-server-0000-1"), "zk-server-0000" );
            profileNode = ZkPath.CONFIG_VERSIONS_PROFILE.getPath(version, "zk-server-0000-1") + "/org.fusesource.fabric.zookeeper.server-0000";
            set( client, profileNode + "/clientPort", "2181" );
            set( client, "/fabric/configs/versions/" + version + "/general/zookeeper-cluster", "0000" );
            set( client, "/fabric/configs/versions/" + version + "/general/zookeeper-cluster/0000", karafName );

            createDefault( client, defaultProfile + "/org.fusesource.fabric.agent/org.ops4j.pax.url.mvn.useFallbackRepositories", "false" );
            createDefault( client, defaultProfile + "/org.fusesource.fabric.agent/org.ops4j.pax.url.mvn.disableAether", "true" );
            createDefault( client, defaultProfile + "/org.fusesource.fabric.agent/org.ops4j.pax.url.mvn.defaultRepositories", "file:${karaf.home}/${karaf.default.repository}@snapshots" );
            createDefault( client, defaultProfile + "/org.fusesource.fabric.agent/org.ops4j.pax.url.mvn.repositories", "http://repo1.maven.org/maven2,http://repo.fusesource.com/nexus/content/repositories/releases" );

            createDefault( client, defaultProfile + "/org.fusesource.fabric.agent/repository.fabric", "mvn:org.fusesource.fabric/fabric-distro/1.0-SNAPSHOT/xml/features" );
            createDefault( client, defaultProfile + "/org.fusesource.fabric.agent/feature.karaf", "karaf" );
            createDefault( client, defaultProfile + "/org.fusesource.fabric.agent/feature.fabric-agent", "fabric-agent" );
            createDefault( client, defaultProfile + "/org.fusesource.fabric.agent/framework", "mvn:org.apache.felix/org.apache.felix.framework/3.0.9-fuse-00-10" );

            createDefault( client, ZkPath.CONFIG_AGENT.getPath(karafName), version);
            createDefault( client, ZkPath.CONFIG_VERSIONS_AGENT.getPath(version,  karafName), "default zk-server-0000-1");

            Bundle bundle = bundleContext.installBundle("mvn:org.fusesource.fabric/fabric-configadmin/1.0-SNAPSHOT");
            if (bundle.getState() == Bundle.ACTIVE) {
                bundle.stop();
            }
            bundle.start();
        } catch (Exception e) {
            throw new FabricException("Unable to create zookeeper server configuration", e);
        } finally {
            if (client != null) {
                client.destroy();
            }
        }
    }

    public void clean() {
        try {
            Configuration[] configs = configurationAdmin.listConfigurations("(|(service.factoryPid=org.fusesource.fabric.zookeeper.server)(service.pid=org.fusesource.fabric.zookeeper))");
            if (configs != null) {
                for (Configuration config : configs) {
                    config.delete();
                }
            }
            File zkDir = new File("data/zookeeper");
            if (zkDir.isDirectory()) {
                File newZkDir = new File("data/zookeeper." + System.currentTimeMillis());
                if (!zkDir.renameTo(newZkDir)) {
                    newZkDir = zkDir;
                }
                delete(newZkDir);
            }
        } catch (Exception e) {
            throw new FabricException("Unable to delete zookeeper configuration", e);
        }
    }

    private static void delete(File dir) {
        if (dir.isDirectory()) {
            for (File child : dir.listFiles()) {
                delete(child);
            }
        }
        if (dir.exists()) {
            dir.delete();
        }
    }

    public List<String> getClusterAgents() {
        try {
            Configuration[] configs = configurationAdmin.listConfigurations("(service.pid=org.fusesource.fabric.zookeeper)");
            if (configs == null || configs.length == 0) {
                return Collections.emptyList();
            }
            List<String> list = new ArrayList<String>();
            if (zooKeeper.exists("/fabric/configs/versions/" + version + "/general/zookeeper-cluster") != null) {
                String clusterId = zooKeeper.getStringData("/fabric/configs/versions/" + version + "/general/zookeeper-cluster");
                String agents = zooKeeper.getStringData( "/fabric/configs/versions/" + version + "/general/zookeeper-cluster/" + clusterId );
                Collections.addAll( list, agents.split( "," ) );
            }
            return list;
        } catch (Exception e) {
            throw new FabricException("Unable to load zookeeper quorum agents", e);
        }
    }

    public String getZooKeeperUrl() {
        try {
            Configuration config = configurationAdmin.getConfiguration("org.fusesource.fabric.zookeeper", null);
            final String zooKeeperUrl = (String) config.getProperties().get("zookeeper.url");
            if (zooKeeperUrl == null) {
                throw new IllegalStateException("Unable to find the zookeeper url");
            }
            return zooKeeperUrl;
        } catch (Exception e) {
            throw new FabricException("Unable to load zookeeper current url", e);
        }
    }

    public void createCluster(List<String> agents) {
        try {
            if (agents == null || agents.size() == 2) {
                throw new IllegalArgumentException("One or at least 3 agents must be used to create a zookeeper cluster");
            }
            Configuration config = configurationAdmin.getConfiguration("org.fusesource.fabric.zookeeper", null);
            String zooKeeperUrl = config != null && config.getProperties() != null ? (String) config.getProperties().get("zookeeper.url") : null;
            if (zooKeeperUrl == null) {
                if (agents.size() != 1 || !agents.get(0).equals(System.getProperty("karaf.name"))) {
                    throw new FabricException("The first zookeeper cluster must be configured on this agent only.");
                }
                createLocalServer();
                return;
            }

            String url = zooKeeper.getStringData( "/fabric/configs/versions/" + version + "/profiles/default/org.fusesource.fabric.zookeeper/zookeeper.url" );
            if (!url.equals(zooKeeperUrl)) {
                throw new IllegalStateException("The zookeeper configuration is not properly backed in the zookeeper tree.");
            }
            for (String agent : agents) {
                if (zooKeeper.exists("/fabric/registry/agents/alive/" + agent) == null) {
                    throw new FabricException("The agent " + agent + " is not alive");
                }
            }

            // Find used zookeeper ports
            Map<String, List<Integer>> usedPorts = new HashMap<String, List<Integer>>();
            String oldClusterId = get( zooKeeper, "/fabric/configs/versions/" + version + "/general/zookeeper-cluster" );
            if ( oldClusterId != null ) {
                for ( String node : zooKeeper.getAllChildren( "/fabric/configs/versions/" + version + "/profiles/zk-server-" + oldClusterId + "/org.fusesource.fabric.zookeeper.server-" + oldClusterId ) ) {
                    if (node.startsWith("server.")) {
                        String data = zooKeeper.getStringData( "/fabric/configs/versions/" + version + "/profiles/zk-server-" + oldClusterId + "/org.fusesource.fabric.zookeeper.server-" + oldClusterId + "/" + node );
                        addUsedPorts(usedPorts, data);
                    }
                }
                String datas =  zooKeeper.getStringData( "/fabric/configs/versions/" + version + "/profiles/default/org.fusesource.fabric.zookeeper/zookeeper.url" );
                for (String data : datas.split(",")) {
                    addUsedPorts(usedPorts, data);
                }
            }

            String newClusterId;
            if (oldClusterId == null) {
                newClusterId = "0000";
            } else {
                newClusterId = new DecimalFormat("0000").format(Integer.parseInt(oldClusterId) + 1);
            }

            String profileNode = "/fabric/configs/versions/" + version + "/profiles/zk-server-" + newClusterId + "/org.fusesource.fabric.zookeeper.server-" + newClusterId;

            set(zooKeeper, profileNode + "/tickTime", "2000");
            set(zooKeeper, profileNode + "/initLimit", "10");
            set(zooKeeper, profileNode + "/syncLimit", "5");
            set(zooKeeper, profileNode + "/dataDir", "data/zookeeper/" + newClusterId);

            int index = 1;
            String connectionUrl = "";
            String realConnectionUrl = "";
            String agentList = "";
            for (String agent : agents) {
                String ip = zooKeeper.getStringData("/fabric/registry/agents/config/" + agent + "/ip");
                String profNode = "/fabric/configs/versions/" + version + "/profiles/zk-server-" + newClusterId + "-" + Integer.toString(index);
                String pidNode = profNode + "/org.fusesource.fabric.zookeeper.server-" + newClusterId;
                add( zooKeeper, profNode, "zk-server-" + newClusterId );
                String port1 = Integer.toString(findPort(usedPorts, ip, 2181));
                if (agents.size() > 1) {
                    String port2 = Integer.toString(findPort(usedPorts, ip, 2888));
                    String port3 = Integer.toString(findPort(usedPorts, ip, 3888));
                    set(zooKeeper, profileNode + "/server." + Integer.toString(index), "${zk:" + agent + "/ip}:" + port2 + ":" + port3);
                    set(zooKeeper, pidNode + "/server.id", Integer.toString(index));
                }
                set(zooKeeper, pidNode + "/clientPort", port1);
                add(zooKeeper, "/fabric/configs/versions/" + version + "/agents/" + agent, "zk-server-" + newClusterId + "-" + Integer.toString(index));
                if (connectionUrl.length() > 0) {
                    connectionUrl += ",";
                    realConnectionUrl += ",";
                }
                connectionUrl += "${zk:" + agent + "/ip}:" + port1;
                realConnectionUrl += ip + ":" + port1;
                if (agentList.length() > 0) {
                    agentList += ",";
                }
                agentList += agent;
                index++;
            }

            if (oldClusterId != null) {
                ZKClient src = new ZKClient(zooKeeperUrl, Timespan.ONE_MINUTE, null);
                ZKClient dst = new ZKClient(realConnectionUrl, Timespan.ONE_MINUTE, null);
                try {
                    src.start();
                    dst.start();
                    src.waitForStart();
                    dst.waitForStart();

                    copy(src, dst, "/fabric/configs");
                    set( dst, "/fabric/configs/versions/" + version + "/general/zookeeper-cluster", newClusterId );
                    set( dst, "/fabric/configs/versions/" + version + "/general/zookeeper-cluster/" + newClusterId, agentList );

                    for (String agent : dst.getChildren("/fabric/configs/versions/" + version + "/agents")) {
                        remove(dst, "/fabric/configs/versions/" + version + "/agents/" + agent, "zk-server-" + oldClusterId + "-.*");
                    }

                    set( dst, "/fabric/configs/versions/" + version + "/profiles/default/org.fusesource.fabric.zookeeper/zookeeper.url", connectionUrl );
                    set( src, "/fabric/configs/versions/" + version + "/profiles/default/org.fusesource.fabric.zookeeper/zookeeper.url", connectionUrl );


                } finally {
                    src.destroy();
                    dst.destroy();
                }
            }
        } catch (Exception e) {
            throw new FabricException("Unable to create zookeeper quorum: " + e.getMessage(), e);
        }
    }

    private int findPort(Map<String, List<Integer>> usedPorts, String ip, int port) {
        List<Integer> ports = usedPorts.get(ip);
        if (ports == null) {
            ports = new ArrayList<Integer>();
            usedPorts.put(ip, ports);
        }
        for (;;) {
            if (!ports.contains(port)) {
                ports.add(port);
                return port;
            }
            port++;
        }
    }

    private void addUsedPorts(Map<String, List<Integer>> usedPorts, String data) {
        String[] parts = data.split(":");
        List<Integer> ports = usedPorts.get(parts[0]);
        if (ports == null) {
            ports = new ArrayList<Integer>();
            usedPorts.put(parts[0], ports);
        }
        for (int i = 1; i < parts.length; i++) {
            ports.add(Integer.parseInt(parts[i]));
        }
    }

    public void addToCluster(List<String> agents) {
        try {
            List<String> current = getClusterAgents();
            current.addAll(agents);
            createCluster(current);
        } catch (Exception e) {
            throw new FabricException("Unable to add agents to zookeeper quorum: " + e.getMessage(), e);
        }
    }

    public void removeFromCluster(List<String> agents) {
        try {
            List<String> current = getClusterAgents();
            current.removeAll(agents);
            createCluster(current);
        } catch (Exception e) {
            throw new FabricException("Unable to add agents to zookeeper quorum: " + e.getMessage(), e);
        }
    }

    private static String getLocalHostAddress() throws UnknownHostException {
        return InetAddress.getByName(InetAddress.getLocalHost().getCanonicalHostName()).getHostAddress();
    }
}
