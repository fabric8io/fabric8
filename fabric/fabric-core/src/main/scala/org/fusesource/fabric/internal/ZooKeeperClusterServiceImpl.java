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
package org.fusesource.fabric.internal;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.apache.zookeeper.KeeperException;
import org.fusesource.fabric.api.FabricException;
import org.fusesource.fabric.api.ZooKeeperClusterService;
import org.fusesource.fabric.utils.HostUtils;
import org.fusesource.fabric.zookeeper.IZKClient;
import org.fusesource.fabric.zookeeper.ZkDefs;
import org.fusesource.fabric.zookeeper.ZkPath;
import org.fusesource.fabric.zookeeper.utils.ZooKeeperUtils;
import org.fusesource.fabric.zookeeper.utils.ZookeeperImportUtils;
import org.linkedin.util.clock.Timespan;
import org.linkedin.zookeeper.client.ZKClient;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.util.tracker.ServiceTracker;
import static org.fusesource.fabric.utils.PortUtils.findPort;


import static org.fusesource.fabric.utils.BundleUtils.installOrStopBundle;
import static org.fusesource.fabric.utils.PortUtils.mapPortToRange;

public class ZooKeeperClusterServiceImpl implements ZooKeeperClusterService {

    private static final String FRAMEWORK_VERSION = "mvn:org.apache.felix/org.apache.felix.framework/" + FabricConstants.FRAMEWORK_VERSION;

    private BundleContext bundleContext;
    private ConfigurationAdmin configurationAdmin;
    private IZKClient zooKeeper;
    private String version = ZkDefs.DEFAULT_VERSION;
    private boolean ensembleAutoStart = Boolean.parseBoolean(System.getProperty(ENSEMBLE_AUTOSTART));

    public void init() {
        if (ensembleAutoStart) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    createLocalServer();
                }
            }).start();

        }
    }

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

    @Override
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
        try {
            IZKClient client;
            Properties properties;
            String karafName = System.getProperty("karaf.name");
            String minimumPort = System.getProperty(ZkDefs.MINIMUM_PORT);
            String maximumPort = System.getProperty(ZkDefs.MAXIMUM_PORT);
            int mappedPort = mapPortToRange(port, minimumPort, maximumPort);

            // Install or stop the fabric-configadmin bridge
            Bundle bundleFabricConfigAdmin = installOrStopBundle(bundleContext, "org.fusesource.fabric.fabric-configadmin",
                    "mvn:org.fusesource.fabric/fabric-configadmin/" + FabricConstants.FABRIC_VERSION);
            Bundle bundleFabricZooKeeper = installOrStopBundle(bundleContext, "org.fusesource.fabric.fabric-zookeeper",
                    "mvn:org.fusesource.fabric/fabric-zookeeper/" + FabricConstants.FABRIC_VERSION);
            Bundle bundleFabricJaas = installOrStopBundle(bundleContext, "org.fusesource.fabric.fabric-jaas  ",
                    "mvn:org.fusesource.fabric/fabric-jaas/" + FabricConstants.FABRIC_VERSION);
            Bundle bundleFabricCommands = installOrStopBundle(bundleContext, "org.fusesource.fabric.fabric-commands  ",
                    "mvn:org.fusesource.fabric/fabric-commands/" + FabricConstants.FABRIC_VERSION);
            Bundle bundleFabricMavenProxy = installOrStopBundle(bundleContext, "org.fusesource.fabric.fabric-commands  ",
                    "mvn:org.fusesource.fabric/fabric-maven-proxy/" + FabricConstants.FABRIC_VERSION);

            // Create configuration
            String connectionUrl = HostUtils.getLocalHostName() + ":" + Integer.toString(mappedPort);

            Configuration config = configurationAdmin.createFactoryConfiguration("org.fusesource.fabric.zookeeper.server");
            properties = new Properties();
            properties.put("tickTime", "2000");
            properties.put("initLimit", "10");
            properties.put("syncLimit", "5");
            properties.put("dataDir", "data/zookeeper/0000");
            properties.put("clientPort", Integer.toString(mappedPort));
            properties.put("fabric.zookeeper.pid", "org.fusesource.fabric.zookeeper.server-0000");
            config.setBundleLocation(null);
            config.update(properties);

            // Update the client configuration
            config = configurationAdmin.getConfiguration("org.fusesource.fabric.zookeeper");
            properties = new Properties();
            properties.put("zookeeper.url", connectionUrl);
            properties.put("fabric.zookeeper.pid", "org.fusesource.fabric.zookeeper");
            config.setBundleLocation(null);
            config.update(properties);

            // Start fabric-zookeeper bundle
            bundleFabricZooKeeper.start();

            // Wait for the client to be available
            ServiceTracker tracker = new ServiceTracker(bundleContext, IZKClient.class.getName(), null);
            tracker.open();
            client = (IZKClient) tracker.waitForService(5000);
            if (client == null) {
                throw new IllegalStateException("Timeout waiting for ZooKeeper client to be registered");
            }
            tracker.close();
            client.waitForConnected();

            // Import data into zookeeper
            String autoImportFrom = System.getProperty(PROFILES_AUTOIMPORT_PATH);
            if (autoImportFrom != null) {
                ZookeeperImportUtils.importFromFileSystem(client, autoImportFrom, "/", null, null, false, false, false);
            }

            String defaultProfile = ZkPath.CONFIG_VERSIONS_PROFILE.getPath(version, "default");
            setConfigProperty(client, defaultProfile + "/org.fusesource.fabric.zookeeper.properties", "zookeeper.url", "${zk:" + karafName + "/ip}:" + Integer.toString(mappedPort));

            ZooKeeperUtils.set(client, ZkPath.CONFIG_VERSIONS_PROFILE.getPath(version, "fabric-ensemble-0000"), "abstract=true\nhidden=true");

            String profileNode = ZkPath.CONFIG_VERSIONS_PROFILE.getPath(version, "fabric-ensemble-0000") + "/org.fusesource.fabric.zookeeper.server-0000.properties";
            Properties p = new Properties();
            p.put("tickTime", "2000");
            p.put("initLimit", "10");
            p.put("syncLimit", "5");
            p.put("dataDir", "data/zookeeper/0000");
            ZooKeeperUtils.set(client, profileNode, toString(p));

            ZooKeeperUtils.set(client, ZkPath.CONFIG_VERSIONS_PROFILE.getPath(version, "fabric-ensemble-0000-1"), "parents=fabric-ensemble-0000\nhidden=true");
            profileNode = ZkPath.CONFIG_VERSIONS_PROFILE.getPath(version, "fabric-ensemble-0000-1") + "/org.fusesource.fabric.zookeeper.server-0000.properties";
            p = new Properties();
            p.put("clientPort", String.valueOf(mappedPort));
            ZooKeeperUtils.set(client, profileNode, toString(p));

            ZooKeeperUtils.set(client, "/fabric/configs/versions/" + version + "/general/fabric-ensemble", "0000");
            ZooKeeperUtils.set(client, "/fabric/configs/versions/" + version + "/general/fabric-ensemble/0000", karafName);

            String fabricProfile = ZkPath.CONFIG_VERSIONS_PROFILE.getPath(version, "fabric");
            ZooKeeperUtils.createDefault(client, fabricProfile, "default");
            p = getProperties(client, fabricProfile + "/org.fusesource.fabric.agent.properties", new Properties());
            p.put("feature.fabric-commands", "fabric-commands");
            ZooKeeperUtils.set(client, fabricProfile + "/org.fusesource.fabric.agent.properties", toString(p));

            ZooKeeperUtils.createDefault(client, ZkPath.CONFIG_CONTAINER.getPath(karafName), version);
            String assignedProfile = System.getProperty(PROFILE);
            if (assignedProfile != null && !assignedProfile.isEmpty() && !"fabric".equals(assignedProfile)) {
                ZooKeeperUtils.createDefault(client, ZkPath.CONFIG_VERSIONS_CONTAINER.getPath(version, karafName), "fabric fabric-ensemble-0000-1 " + assignedProfile);
            } else {
                ZooKeeperUtils.createDefault(client, ZkPath.CONFIG_VERSIONS_CONTAINER.getPath(version, karafName), "fabric fabric-ensemble-0000-1");
            }

            // add auth
            ZooKeeperUtils.createDefault(client, defaultProfile + "/org.fusesource.fabric.jaas/encryption.enabled", "${zk:/fabric/authentication/encryption.enabled}");
            ZooKeeperUtils.createDefault(client, "/fabric/authentication/encryption.enabled", "true");
            ZooKeeperUtils.createDefault(client, "/fabric/authentication/domain", "karaf");
            ZooKeeperUtils.createDefault(client, "/fabric/authentication/users", "admin={CRYPT}21232f297a57a5a743894a0e4a801fc3{CRYPT},admin\nsystem={CRYPT}1d0258c2440a8d19e716292b231e3190{CRYPT},admin");

            // Reset the autostart flag
            if (ensembleAutoStart) {
                System.setProperty(ENSEMBLE_AUTOSTART, Boolean.FALSE.toString());
                File file = new File(System.getProperty("karaf.base") + "/etc/system.properties");
                org.apache.felix.utils.properties.Properties props = new org.apache.felix.utils.properties.Properties(file);
                props.put(ENSEMBLE_AUTOSTART, Boolean.FALSE.toString());
                props.save();
            }

            // Restart fabric-configadmin bridge
            bundleFabricConfigAdmin.start();
            bundleFabricJaas.start();
            bundleFabricCommands.start();

            //Check if the agent is configured to auto start.
            if (!System.getProperties().containsKey(AGENT_AUTOSTART) || Boolean.parseBoolean(System.getProperty(AGENT_AUTOSTART))) {
                Bundle bundleFabricAgent = installOrStopBundle(bundleContext, "org.fusesource.fabric.fabric-agent  ",
                        "mvn:org.fusesource.fabric/fabric-agent/" + FabricConstants.FABRIC_VERSION);
                bundleFabricAgent.start();
            }
            bundleFabricMavenProxy.start();
        } catch (Exception e) {
            throw new FabricException("Unable to create zookeeper server configuration", e);
        }
    }


    public void clean() {
        try {
            for (; ; ) {
                Configuration[] configs = configurationAdmin.listConfigurations("(|(service.factoryPid=org.fusesource.fabric.zookeeper.server)(service.pid=org.fusesource.fabric.zookeeper))");
                if (configs != null && configs.length > 0) {
                    for (Configuration config : configs) {
                        config.delete();
                    }
                    Thread.sleep(100);
                } else {
                    break;
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

    public List<String> getClusterContainers() {
        try {
            Configuration[] configs = configurationAdmin.listConfigurations("(service.pid=org.fusesource.fabric.zookeeper)");
            if (configs == null || configs.length == 0) {
                return Collections.emptyList();
            }
            List<String> list = new ArrayList<String>();
            if (zooKeeper.exists("/fabric/configs/versions/" + version + "/general/fabric-ensemble") != null) {
                String clusterId = zooKeeper.getStringData("/fabric/configs/versions/" + version + "/general/fabric-ensemble");
                String containers = zooKeeper.getStringData("/fabric/configs/versions/" + version + "/general/fabric-ensemble/" + clusterId);
                Collections.addAll(list, containers.split(","));
            }
            return list;
        } catch (Exception e) {
            throw new FabricException("Unable to load zookeeper quorum containers", e);
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

    public void createCluster(List<String> containers) {
        try {
            if (containers == null || containers.size() == 2) {
                throw new IllegalArgumentException("One or at least 3 containers must be used to create a zookeeper ensemble");
            }
            Configuration config = configurationAdmin.getConfiguration("org.fusesource.fabric.zookeeper", null);
            String zooKeeperUrl = config != null && config.getProperties() != null ? (String) config.getProperties().get("zookeeper.url") : null;
            if (zooKeeperUrl == null) {
                if (containers.size() != 1 || !containers.get(0).equals(System.getProperty("karaf.name"))) {
                    throw new FabricException("The first zookeeper cluster must be configured on this container only.");
                }
                createLocalServer();
                return;
            }

            String url = ZooKeeperUtils.getSubstitutedPath(zooKeeper, "/fabric/configs/versions/" + version + "/profiles/default/org.fusesource.fabric.zookeeper.properties#zookeeper.url");
            if (!url.equals(zooKeeperUrl)) {
                throw new IllegalStateException("The zookeeper configuration is not properly backed in the zookeeper tree.");
            }
            for (String container : containers) {
                if (zooKeeper.exists(ZkPath.CONTAINER_ALIVE.getPath(container)) == null) {
                    throw new FabricException("The container " + container + " is not alive");
                }
            }

            // Find used zookeeper ports
            Map<String, List<Integer>> usedPorts = new HashMap<String, List<Integer>>();
            String oldClusterId = ZooKeeperUtils.get(zooKeeper, "/fabric/configs/versions/" + version + "/general/fabric-ensemble");
            if (oldClusterId != null) {
                Properties p = toProperties(zooKeeper.getStringData("/fabric/configs/versions/" + version + "/profiles/fabric-ensemble-" + oldClusterId + "/org.fusesource.fabric.zookeeper.server-" + oldClusterId + ".properties"));
                for (Object n : p.keySet()) {
                    String node = (String) n;
                    if (node.startsWith("server.")) {
                        String data = ZooKeeperUtils.getSubstitutedPath(zooKeeper, "/fabric/configs/versions/" + version + "/profiles/fabric-ensemble-" + oldClusterId + "/org.fusesource.fabric.zookeeper.server-" + oldClusterId + ".properties#" + node);
                        addUsedPorts(usedPorts, data);
                    }
                }
                String datas = ZooKeeperUtils.getSubstitutedPath(zooKeeper, "/fabric/configs/versions/" + version + "/profiles/default/org.fusesource.fabric.zookeeper.properties#zookeeper.url");
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

            ZooKeeperUtils.set(zooKeeper, ZkPath.CONFIG_VERSIONS_PROFILE.getPath(version, "fabric-ensemble-" + newClusterId), "abstract=true\nhidden=true");
            String profileNode = "/fabric/configs/versions/" + version + "/profiles/fabric-ensemble-" + newClusterId + "/org.fusesource.fabric.zookeeper.server-" + newClusterId + ".properties";

            Properties profileNodeProperties = new Properties();
            profileNodeProperties.put("tickTime", "2000");
            profileNodeProperties.put("initLimit", "10");
            profileNodeProperties.put("syncLimit", "5");
            profileNodeProperties.put("dataDir", "data/zookeeper/" + newClusterId);

            int index = 1;
            String connectionUrl = "";
            String realConnectionUrl = "";
            String containerList = "";
            for (String container : containers) {
                String ip = ZooKeeperUtils.getSubstitutedPath(zooKeeper, ZkPath.CONTAINER_IP.getPath(container));

                String minimumPort = ZooKeeperUtils.getSubstitutedPath(zooKeeper, ZkPath.CONTAINER_PORT_MIN.getPath(container));
                String maximumPort = ZooKeeperUtils.getSubstitutedPath(zooKeeper, ZkPath.CONTAINER_PORT_MAX.getPath(container));

                String profNode = "/fabric/configs/versions/" + version + "/profiles/fabric-ensemble-" + newClusterId + "-" + Integer.toString(index);
                String pidNode = profNode + "/org.fusesource.fabric.zookeeper.server-" + newClusterId + ".properties";
                Properties pidNodeProperties = new Properties();

                ZooKeeperUtils.set(zooKeeper, profNode, "parents=fabric-ensemble-" + newClusterId + "\nhidden=true");
                String port1 = Integer.toString(findPort(usedPorts, ip, mapPortToRange(2181, minimumPort, maximumPort)));
                if (containers.size() > 1) {
                    String port2 = Integer.toString(findPort(usedPorts, ip, mapPortToRange(2888, minimumPort, maximumPort)));
                    String port3 = Integer.toString(findPort(usedPorts, ip, mapPortToRange(3888, minimumPort, maximumPort)));
                    profileNodeProperties.put("server." + Integer.toString(index), "${zk:" + container + "/ip}:" + port2 + ":" + port3);
                    pidNodeProperties.put("server.id", Integer.toString(index));
                }
                pidNodeProperties.put("clientPort", port1);
                ZooKeeperUtils.set(zooKeeper, pidNode, toString(pidNodeProperties));

                ZooKeeperUtils.add(zooKeeper, "/fabric/configs/versions/" + version + "/containers/" + container, "fabric-ensemble-" + newClusterId + "-" + Integer.toString(index));
                if (connectionUrl.length() > 0) {
                    connectionUrl += ",";
                    realConnectionUrl += ",";
                }
                connectionUrl += "${zk:" + container + "/ip}:" + port1;
                realConnectionUrl += ip + ":" + port1;
                if (containerList.length() > 0) {
                    containerList += ",";
                }
                containerList += container;
                index++;
            }

            ZooKeeperUtils.set(zooKeeper, profileNode, toString(profileNodeProperties));

            if (oldClusterId != null) {
                ZKClient dst = new ZKClient(realConnectionUrl, Timespan.ONE_MINUTE, null);
                try {
                    dst.start();
                    dst.waitForStart(new Timespan(30, Timespan.TimeUnit.SECOND));

                    ZooKeeperUtils.copy(zooKeeper, dst, "/fabric/registry");
                    ZooKeeperUtils.copy(zooKeeper, dst, "/fabric/authentication");
                    ZooKeeperUtils.copy(zooKeeper, dst, "/fabric/configs");

                    //Make sure that the alive zndoe is deleted for each container.
                    for (String container : containers) {
                        String alivePath = "/fabric/registry/containers/alive/" + container;
                        if (dst.exists(alivePath) != null) {
                            dst.deleteWithChildren(alivePath);
                        }
                    }

                    ZooKeeperUtils.set(dst, "/fabric/configs/versions/" + version + "/general/fabric-ensemble", newClusterId);
                    ZooKeeperUtils.set(dst, "/fabric/configs/versions/" + version + "/general/fabric-ensemble/" + newClusterId, containerList);
                    for (String container : dst.getChildren("/fabric/configs/versions/" + version + "/containers")) {
                        ZooKeeperUtils.remove(dst, "/fabric/configs/versions/" + version + "/containers/" + container, "fabric-ensemble-" + oldClusterId + "-.*");
                    }
                    setConfigProperty(dst, "/fabric/configs/versions/" + version + "/profiles/default/org.fusesource.fabric.zookeeper.properties", "zookeeper.url", connectionUrl);
                    setConfigProperty(zooKeeper, "/fabric/configs/versions/" + version + "/profiles/default/org.fusesource.fabric.zookeeper.properties", "zookeeper.url", connectionUrl);

                } finally {
                    dst.destroy();
                }
            } else {
                setConfigProperty(zooKeeper, "/fabric/configs/versions/" + version + "/profiles/default/org.fusesource.fabric.zookeeper.properties", "zookeeper.url", connectionUrl);
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

    static public Properties getProperties(org.linkedin.zookeeper.client.IZKClient client, String file, Properties defaultValue) throws InterruptedException, KeeperException, IOException {
        try {
            String v = ZooKeeperUtils.get(client, file);
            if (v != null) {
                return toProperties(v);
            } else {
                return defaultValue;
            }
        } catch (KeeperException.NoNodeException e) {
            return defaultValue;
        }
    }

    static public void setConfigProperty(org.linkedin.zookeeper.client.IZKClient client, String file, String prop, String value) throws InterruptedException, KeeperException, IOException {
        Properties p = getProperties(client, file, new Properties());
        p.setProperty(prop, value);
        ZooKeeperUtils.set(client, file, toString(p));
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

    public void addToCluster(List<String> containers) {
        try {
            List<String> current = getClusterContainers();
            current.addAll(containers);
            createCluster(current);
        } catch (Exception e) {
            throw new FabricException("Unable to add containers to fabric ensemble: " + e.getMessage(), e);
        }
    }

    public void removeFromCluster(List<String> containers) {
        try {
            List<String> current = getClusterContainers();
            current.removeAll(containers);
            createCluster(current);
        } catch (Exception e) {
            throw new FabricException("Unable to remove containers to fabric ensemble: " + e.getMessage(), e);
        }
    }
}
