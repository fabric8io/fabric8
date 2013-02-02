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
package org.fusesource.fabric.boot.commands;

import java.io.File;
import java.io.IOException;
import java.util.Hashtable;

import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.apache.felix.gogo.commands.Option;
import org.apache.karaf.shell.console.OsgiCommandSupport;
import org.apache.zookeeper.KeeperException;
import org.fusesource.fabric.internal.FabricConstants;
import org.fusesource.fabric.utils.BundleUtils;
import org.fusesource.fabric.utils.Ports;
import org.fusesource.fabric.utils.SystemProperties;
import org.fusesource.fabric.utils.shell.ShellUtils;
import org.fusesource.fabric.zookeeper.IZKClient;
import org.fusesource.fabric.zookeeper.ZkDefs;
import org.fusesource.fabric.zookeeper.ZkPath;
import org.fusesource.fabric.zookeeper.internal.ZKClient;
import org.linkedin.util.clock.Timespan;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.service.cm.ConfigurationAdmin;

@Command(name = "join", scope = "fabric", description = "Join a container to an existing fabric", detailedDescription = "classpath:join.txt")
public class Join extends OsgiCommandSupport implements org.fusesource.fabric.boot.commands.service.Join {

    ConfigurationAdmin configurationAdmin;
    private IZKClient zooKeeper;
    private String version = ZkDefs.DEFAULT_VERSION;
    private BundleContext bundleContext;

    @Option(name = "-n", aliases = "--non-managed", multiValued = false, description = "Flag to keep the container non managed")
    private boolean nonManaged;

    @Option(name = "-f", aliases = "--force", multiValued = false, description = "Forces the use of container name")
    private boolean force;

    @Option(name = "-p", aliases = "--profile", multiValued = false, description = "Chooses the profile of the container")
    private String profile = "fabric";

    @Option(name = "--min-port", multiValued = false, description = "The minimum port of the allowed port range")
    private int minimumPort = Ports.MIN_PORT_NUMBER;

    @Option(name = "--max-port", multiValued = false, description = "The maximum port of the allowed port range")
    private int maximumPort = Ports.MAX_PORT_NUMBER;

    @Argument(required = true, index = 0, multiValued = false, description = "Zookeeper URL")
    private String zookeeperUrl;

    @Option(name = "-r", aliases = {"--resolver"}, description = "The resolver policy. Possible values are: localip, localhostname, publicip, publichostname, manualip. Default is localhostname.")
    String resolver;

    @Option(name = "-m", aliases = {"--manual-ip"}, description = "An address to use, when using the manualip resolver.")
    String manualIp;

    @Option(name = "--zookeeper-password", multiValued = false, description = "The ensemble password to use (one will be generated if not given)")
    private String zookeeperPassword;

    @Argument(required = false, index = 1, multiValued = false, description = "Container name to use in fabric. By default a karaf name will be used")
    private String containerName;


    @Override
    protected Object doExecute() throws Exception {
        String oldName = System.getProperty(SystemProperties.KARAF_NAME);
        if (containerName == null) {
            containerName = oldName;
        }

        if (resolver != null) {
            System.setProperty(ZkDefs.LOCAL_RESOLVER_PROPERTY, resolver);
        }

        if (manualIp != null) {
            System.setProperty(ZkDefs.MANUAL_IP, manualIp);
        }

        zookeeperPassword = zookeeperPassword != null ? zookeeperPassword : ShellUtils.retrieveFabricZookeeperPassword(session);
        System.setProperty(ZkDefs.MINIMUM_PORT, String.valueOf(minimumPort));
        System.setProperty(ZkDefs.MAXIMUM_PORT, String.valueOf(maximumPort));

        if (!containerName.equals(oldName)) {
            if (force || permissionToRenameContainer()) {
                if (!registerContainer(containerName, zookeeperPassword, profile, force)) {
                    System.err.print("A container with the name: " + containerName + " is already member of the cluster. You can specify a different name as an argument.");
                    return null;
                }

                System.setProperty(SystemProperties.KARAF_NAME, containerName);
                System.setProperty("zookeeper.url", zookeeperUrl);
                //Rename the container
                File file = new File(System.getProperty("karaf.base") + "/etc/system.properties");
                org.apache.felix.utils.properties.Properties props = new org.apache.felix.utils.properties.Properties(file);
                props.put(SystemProperties.KARAF_NAME, containerName);
                props.put("zookeeper.url", zookeeperUrl);
                props.put("zookeeper.password", zookeeperPassword);
                props.save();
                //Install required bundles
                if (!nonManaged) {
                    installBundles();
                }
                org.osgi.service.cm.Configuration config = configurationAdmin.getConfiguration("org.fusesource.fabric.zookeeper");
                Hashtable<String, Object> properties = new Hashtable<String, Object>();
                properties.put("zookeeper.url", zookeeperUrl);
                properties.put("zookeeper.password", zookeeperPassword);
                config.setBundleLocation(null);
                config.update(properties);

                //Restart the container
                System.setProperty("karaf.restart", "true");
                System.setProperty("karaf.restart.clean", "false");
                bundleContext.getBundle(0).stop();

                return null;
            } else {
                return null;
            }
        } else {
            if (!registerContainer(containerName, zookeeperPassword, profile, force)) {
                System.err.println("A container with the name: " + containerName + " is already member of the cluster. You can specify a different name as an argument.");
                return null;
            }
            org.osgi.service.cm.Configuration config = configurationAdmin.getConfiguration("org.fusesource.fabric.zookeeper");
            Hashtable<String, Object> properties = new Hashtable<String, Object>();
            properties.put("zookeeper.url", zookeeperUrl);
            properties.put("zookeeper.password", zookeeperPassword);
            config.setBundleLocation(null);
            config.update(properties);

            installBundles();

            return null;
        }
    }

    /**
     * Checks if there is an existing container using the same name.
     *
     * @param name
     * @return
     * @throws InterruptedException
     * @throws KeeperException
     */
    private boolean registerContainer(String name, String registryPassword, String profile, boolean force) throws Exception {
        boolean exists = false;
        ZKClient zkClient = null;
        try {
            zkClient = new ZKClient(zookeeperUrl, Timespan.ONE_MINUTE, null);
            if (registryPassword != null && !registryPassword.isEmpty()) {
                zkClient.setPassword(registryPassword);
            }
            zkClient.start();
            zkClient.waitForConnected();
            exists = zkClient.exists(ZkPath.CONTAINER.getPath(name)) != null;
            if (!exists || force) {
                ZkPath.createContainerPaths(zkClient, containerName, version, profile);
            }
        } finally {
            if (zkClient != null) {
                zkClient.close();
            }
        }
        return !exists || force;
    }

    /**
     * Asks the users permission to restart the container.
     *
     * @return
     * @throws IOException
     */
    private boolean permissionToRenameContainer() throws IOException {
        for (; ; ) {
            StringBuffer sb = new StringBuffer();
            System.err.println("You are about to change the container name. This action will restart the container.");
            System.err.println("The local shell will automatically restart, but ssh connections will be terminated.");
            System.err.println("The container will automatically join: " + zookeeperUrl + " the cluster after it restarts.");
            System.err.print("Do you wish to proceed (yes/no):");
            System.err.flush();
            for (; ; ) {
                int c = session.getKeyboard().read();
                if (c < 0) {
                    return false;
                }
                System.err.print((char) c);
                if (c == '\r' || c == '\n') {
                    break;
                }
                sb.append((char) c);
            }
            String str = sb.toString();
            if ("yes".equals(str)) {
                return true;
            }
            if ("no".equals(str)) {
                return false;
            }
        }
    }

    public void installBundles() throws BundleException {
        Bundle bundleFabricJaas = BundleUtils.findOrInstallBundle(bundleContext, "org.fusesource.fabric.fabric-jaas",
                "mvn:org.fusesource.fabric/fabric-jaas/" + FabricConstants.FABRIC_VERSION);
        Bundle bundleFabricCommands = BundleUtils.findOrInstallBundle(bundleContext, "org.fusesource.fabric.fabric-commands",
                "mvn:org.fusesource.fabric/fabric-commands/" + FabricConstants.FABRIC_VERSION);
        bundleFabricJaas.start();
        bundleFabricCommands.start();

        if (!nonManaged) {
            Bundle bundleFabricConfigAdmin = BundleUtils.findOrInstallBundle(bundleContext, "org.fusesource.fabric.fabric-configadmin",
                    "mvn:org.fusesource.fabric/fabric-configadmin/" + FabricConstants.FABRIC_VERSION);
            Bundle bundleFabricAgent = BundleUtils.findOrInstallBundle(bundleContext, "org.fusesource.fabric.fabric-agent",
                    "mvn:org.fusesource.fabric/fabric-agent/" + FabricConstants.FABRIC_VERSION);
            bundleFabricConfigAdmin.start();
            bundleFabricAgent.start();
        }
    }


    @Override
    public Object run() throws Exception {
        return doExecute();
    }

    @Override
    public void setConfigurationAdmin(ConfigurationAdmin configurationAdmin) {
        this.configurationAdmin = configurationAdmin;
    }

    @Override
    public void setZooKeeper(IZKClient zooKeeper) {
        this.zooKeeper = zooKeeper;
    }

    @Override
    public String getVersion() {
        return version;
    }

    @Override
    public void setVersion(String version) {
        this.version = version;
    }

    @Override
    public String getZookeeperUrl() {
        return zookeeperUrl;
    }

    @Override
    public void setZookeeperUrl(String zookeeperUrl) {
        this.zookeeperUrl = zookeeperUrl;
    }

    public boolean isNonManaged() {
        return nonManaged;
    }

    public void setNonManaged(boolean nonManaged) {
        this.nonManaged = nonManaged;
    }

    public boolean isForce() {
        return force;
    }

    public void setForce(boolean force) {
        this.force = force;
    }

    public String getProfile() {
        return profile;
    }

    public void setProfile(String profile) {
        this.profile = profile;
    }

    public String getContainerName() {
        return containerName;
    }

    public void setContainerName(String containerName) {
        this.containerName = containerName;
    }

    public BundleContext getBundleContext() {
        return bundleContext;
    }

    public void setBundleContext(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }


    public String getResolver() {
        return resolver;
    }

    public void setResolver(String resolver) {
        this.resolver = resolver;
    }

    public String getManualIp() {
        return manualIp;
    }

    public void setManualIp(String manualIp) {
        this.manualIp = manualIp;
    }

    public String getZookeeperPassword() {
        return zookeeperPassword;
    }

    public void setZookeeperPassword(String zookeeperPassword) {
        this.zookeeperPassword = zookeeperPassword;
    }
}
