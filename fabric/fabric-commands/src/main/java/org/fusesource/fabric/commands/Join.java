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
package org.fusesource.fabric.commands;

import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.apache.karaf.shell.console.OsgiCommandSupport;
import org.fusesource.fabric.internal.FabricConstants;
import org.fusesource.fabric.zookeeper.utils.ZooKeeperUtils;
import org.fusesource.fabric.zookeeper.IZKClient;
import org.fusesource.fabric.zookeeper.ZkDefs;
import org.fusesource.fabric.zookeeper.ZkPath;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.util.tracker.ServiceTracker;

import static org.fusesource.fabric.utils.BundleUtils.findOrInstallBundle;

import java.util.Properties;

@Command(name = "join", scope = "fabric", description = "Join a container to an existing fabric", detailedDescription = "classpath:join.txt")
public class Join extends OsgiCommandSupport implements org.fusesource.fabric.commands.service.Join {

    ConfigurationAdmin configurationAdmin;
    private IZKClient zooKeeper;
    private String version = ZkDefs.DEFAULT_VERSION;
    private BundleContext bundleContext;

    @Argument(required = true, multiValued = false, description = "Zookeeper URL")
    private String zookeeperUrl;

    @Override
    protected Object doExecute() throws Exception {
        org.osgi.service.cm.Configuration config = configurationAdmin.getConfiguration("org.fusesource.fabric.zookeeper");
        Properties properties = new Properties();
        properties.put("zookeeper.url", zookeeperUrl);
        config.setBundleLocation(null);
        config.update(properties);

            // Wait for the client to be available
            ServiceTracker tracker = new ServiceTracker(bundleContext, org.fusesource.fabric.zookeeper.IZKClient.class.getName(), null);
            tracker.open();
            zooKeeper = (org.fusesource.fabric.zookeeper.IZKClient) tracker.waitForService(5000);
            if (zooKeeper == null) {
                throw new IllegalStateException("Timeout waiting for ZooKeeper client to be registered");
            }
            tracker.close();
            zooKeeper.waitForConnected();

        String karafName = System.getProperty("karaf.name");

        ZooKeeperUtils.createDefault(zooKeeper, ZkPath.CONFIG_CONTAINER.getPath(karafName), version);
        ZooKeeperUtils.createDefault(zooKeeper, ZkPath.CONFIG_VERSIONS_CONTAINER.getPath(version, karafName), "default");
        Bundle bundleFabricJaas = findOrInstallBundle(bundleContext, "org.fusesource.fabric.fabric-jaas",
                "mvn:org.fusesource.fabric/fabric-jaas/" + FabricConstants.FABRIC_VERSION);
        bundleFabricJaas.start();
        return null;
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

    public BundleContext getBundleContext() {
        return bundleContext;
    }

    public void setBundleContext(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }
}
