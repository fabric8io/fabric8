/**
 * Copyright (C) 2011, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.fabric.internal;

import java.io.*;
import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import org.apache.zookeeper.KeeperException;
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
            client.waitForStart(new Timespan(5, Timespan.TimeUnit.SECOND));

            String defaultProfile = ZkPath.CONFIG_VERSIONS_PROFILE.getPath(version, "default");
            setConfigProperty(client, defaultProfile + "/org.fusesource.fabric.zookeeper.properties", "zookeeper.url", "${zk:" + karafName + "/ip}:" + Integer.toString(port));

            String profileNode = ZkPath.CONFIG_VERSIONS_PROFILE.getPath(version, "zk-server-0000") + "/org.fusesource.fabric.zookeeper.server-0000.properties";
            Properties p = new Properties();
            p.put("tickTime", "2000");
            p.put("initLimit", "10");
            p.put("syncLimit", "5");
            p.put("dataDir", "data/zookeeper/0000");

            ZooKeeperUtils.set(client, profileNode, toString(p));

            ZooKeeperUtils.set(client, ZkPath.CONFIG_VERSIONS_PROFILE.getPath(version, "zk-server-0000-1"), "zk-server-0000");
            profileNode = ZkPath.CONFIG_VERSIONS_PROFILE.getPath(version, "zk-server-0000-1") + "/org.fusesource.fabric.zookeeper.server-0000.properties";
            p = new Properties();
            p.put("clientPort", "2181");
            ZooKeeperUtils.set(client, profileNode, toString(p));

            ZooKeeperUtils.set(client, "/fabric/configs/versions/" + version + "/general/zookeeper-cluster", "0000");
            ZooKeeperUtils.set(client, "/fabric/configs/versions/" + version + "/general/zookeeper-cluster/0000", karafName);

            p = getProperties(client, defaultProfile + "/org.fusesource.fabric.agent.properties", new Properties());
            p.put("org.ops4j.pax.url.mvn.defaultRepositories", "file:${karaf.home}/${karaf.default.repository}@snapshots");
            p.put("org.ops4j.pax.url.mvn.repositories", "http://repo1.maven.org/maven2,http://repo.fusesource.com/nexus/content/repositories/releases,http://scala-tools.org/repo-releases");
            p.put("repository.fabric", "mvn:org.fusesource.fabric/fabric-distro/1.1-SNAPSHOT/xml/features");
            p.put("feature.karaf", "karaf");
            p.put("feature.fabric-agent", "fabric-agent");
            p.put("framework", "mvn:org.apache.felix/org.apache.felix.framework/3.0.9-fuse-00-10");
            ZooKeeperUtils.set(client, defaultProfile + "/org.fusesource.fabric.agent.properties", toString(p));

            ZooKeeperUtils.createDefault(client, ZkPath.CONFIG_AGENT.getPath(karafName), version);
            ZooKeeperUtils.createDefault(client, ZkPath.CONFIG_VERSIONS_AGENT.getPath(version, karafName), "default zk-server-0000-1");

            // add auth
            ZooKeeperUtils.createDefault(client, defaultProfile + "/org.fusesource.fabric.jaas/encryption.enabled", "${zk:/fabric/authentication/encryption.enabled}");
            ZooKeeperUtils.createDefault(client, "fabric/authentication/encryption.enabled", "true");
            ZooKeeperUtils.createDefault(client, "fabric/authentication/domain", "zookeeper");            
            ZooKeeperUtils.createDefault(client, "/fabric/authentication/users", "admin={CRYPT}21232f297a57a5a743894a0e4a801fc3{CRYPT},admin\nsystem={CRYPT}1d0258c2440a8d19e716292b231e3190{CRYPT},admin");

            Bundle bundle = bundleContext.installBundle("mvn:org.fusesource.fabric/fabric-configadmin/1.1-SNAPSHOT");
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


    private String getSubstitutedData(String path) throws InterruptedException, KeeperException {
        String data = zooKeeper.getStringData(path);
        Map<String,String> props = new HashMap<String,String>();
        props.put("data", data);
        InterpolationHelper.performSubstitution(props, new InterpolationHelper.SubstitutionCallback() {
                @Override
                public String getValue(String key) {
                    if (key.startsWith("zk:")) {
                        try {
                            new String(ZkPath.loadURL(zooKeeper, key), "UTF-8");
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    return null;
                }
            });
        return props.get("data");
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

            String url = getSubstitutedData( "/fabric/configs/versions/" + version + "/profiles/default/org.fusesource.fabric.zookeeper.properties#zookeeper.url" );
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
            String oldClusterId = ZooKeeperUtils.get(zooKeeper, "/fabric/configs/versions/" + version + "/general/zookeeper-cluster");
            if ( oldClusterId != null ) {
                Properties p = toProperties(zooKeeper.getStringData("/fabric/configs/versions/" + version + "/profiles/zk-server-" + oldClusterId + "/org.fusesource.fabric.zookeeper.server-" + oldClusterId+".properties"));
                for ( Object n : p.keySet() ) {
                    String node = (String) n;
                    if (node.startsWith("server.")) {
                        String data = getSubstitutedData( "/fabric/configs/versions/" + version + "/profiles/zk-server-" + oldClusterId + "/org.fusesource.fabric.zookeeper.server-" + oldClusterId+".properties#" + node );
                        addUsedPorts(usedPorts, data);
                    }
                }
                String datas =  getSubstitutedData( "/fabric/configs/versions/" + version + "/profiles/default/org.fusesource.fabric.zookeeper.properties#zookeeper.url" );
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

            String profileNode = "/fabric/configs/versions/" + version + "/profiles/zk-server-" + newClusterId + "/org.fusesource.fabric.zookeeper.server-" + newClusterId+".properties";
            
            Properties profileNodeProperties = new Properties();
            profileNodeProperties.put("tickTime", "2000");
            profileNodeProperties.put("initLimit", "10");
            profileNodeProperties.put("syncLimit", "5");
            profileNodeProperties.put("dataDir", "data/zookeeper/" + newClusterId);

            int index = 1;
            String connectionUrl = "";
            String realConnectionUrl = "";
            String agentList = "";
            for (String agent : agents) {
                String ip = zooKeeper.getStringData("/fabric/registry/agents/config/" + agent + "/ip");
                String profNode = "/fabric/configs/versions/" + version + "/profiles/zk-server-" + newClusterId + "-" + Integer.toString(index);
                String pidNode = profNode + "/org.fusesource.fabric.zookeeper.server-" + newClusterId+".profile";
                Properties pidNodeProperties = new Properties();
                
                ZooKeeperUtils.add(zooKeeper, profNode, "zk-server-" + newClusterId);
                String port1 = Integer.toString(findPort(usedPorts, ip, 2181));
                if (agents.size() > 1) {
                    String port2 = Integer.toString(findPort(usedPorts, ip, 2888));
                    String port3 = Integer.toString(findPort(usedPorts, ip, 3888));
                    profileNodeProperties.put("server." + Integer.toString(index), "${zk:" + agent + "/ip}:" + port2 + ":" + port3);
                    pidNodeProperties.put("server.id", Integer.toString(index));
                }
                pidNodeProperties.put("clientPort", port1);
                ZooKeeperUtils.set(zooKeeper, pidNode, toString(pidNodeProperties));

                ZooKeeperUtils.add(zooKeeper, "/fabric/configs/versions/" + version + "/agents/" + agent, "zk-server-" + newClusterId + "-" + Integer.toString(index));
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
            
            ZooKeeperUtils.set(zooKeeper, profileNode, toString(profileNodeProperties));

            if (oldClusterId != null) {
                ZKClient src = new ZKClient(zooKeeperUrl, Timespan.ONE_MINUTE, null);
                ZKClient dst = new ZKClient(realConnectionUrl, Timespan.ONE_MINUTE, null);
                try {
                    src.start();
                    dst.start();
                    src.waitForStart(new Timespan(5, Timespan.TimeUnit.SECOND));
                    dst.waitForStart(new Timespan(5, Timespan.TimeUnit.SECOND));

                    ZooKeeperUtils.copy(src, dst, "/fabric/configs");
                    ZooKeeperUtils.set(dst, "/fabric/configs/versions/" + version + "/general/zookeeper-cluster", newClusterId);
                    ZooKeeperUtils.set(dst, "/fabric/configs/versions/" + version + "/general/zookeeper-cluster/" + newClusterId, agentList);

                    for (String agent : dst.getChildren("/fabric/configs/versions/" + version + "/agents")) {
                        ZooKeeperUtils.remove(dst, "/fabric/configs/versions/" + version + "/agents/" + agent, "zk-server-" + oldClusterId + "-.*");
                    }
                    setConfigProperty(dst, "/fabric/configs/versions/" + version + "/profiles/default/org.fusesource.fabric.zookeeper.properties", "zookeeper.url", connectionUrl);
                    setConfigProperty(src, "/fabric/configs/versions/" + version + "/profiles/default/org.fusesource.fabric.zookeeper.properties", "zookeeper.url", connectionUrl);

                } finally {
                    src.destroy();
                    dst.destroy();
                }
            }
        } catch (Exception e) {
            throw new FabricException("Unable to create zookeeper quorum: " + e.getMessage(), e);
        }
    }

    static public String toString(Properties source) throws IOException {
        StringWriter writer = new StringWriter();
        source.store(writer, null);
        return writer.toString();
    }

    static public Properties toProperties(String source) throws IOException {
        Properties rc = new Properties();
        rc.load(new StringReader(source));
        return rc;
    }

    static public Properties getProperties(ZKClient client, String file, Properties defaultValue) throws InterruptedException, KeeperException, IOException {
        try {
            String v = ZooKeeperUtils.get(client, file);
            if( v!=null ) {
                return toProperties(v);
            } else {
                return defaultValue;
            }
        } catch (KeeperException.NoNodeException e) {
            return defaultValue;
        }
    }

    static public void setConfigProperty(ZKClient client, String file, String prop, String value) throws InterruptedException, KeeperException, IOException {
        Properties p = getProperties(client, file, new Properties());
        p.setProperty(prop, value);
        ZooKeeperUtils.set(client, file, toString(p));
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
