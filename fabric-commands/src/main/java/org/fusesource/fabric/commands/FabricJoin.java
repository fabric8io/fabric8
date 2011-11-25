/**
 * Copyright (C) 2011, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.fabric.commands;

import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.apache.karaf.shell.console.OsgiCommandSupport;
import org.fusesource.fabric.internal.ZooKeeperUtils;
import org.fusesource.fabric.zookeeper.ZkPath;
import org.linkedin.zookeeper.client.IZKClient;
import org.osgi.service.cm.ConfigurationAdmin;

import java.util.Properties;

@Command(name = "join", scope = "fabric", description = "Join fabric cluster", detailedDescription = "classpath:join.txt")
public class FabricJoin extends OsgiCommandSupport {

    ConfigurationAdmin configurationAdmin;
    private IZKClient zooKeeper;
    private String version = "base";

    @Argument(required = true, multiValued = false, description = "Zookeeper URL")
    private String zookeeperUrl;

    @Override
    protected Object doExecute() throws Exception {
        org.osgi.service.cm.Configuration config = configurationAdmin.getConfiguration("org.fusesource.fabric.zookeeper");
        Properties properties = new Properties();
        properties.put("zookeeper.url", zookeeperUrl);
        config.setBundleLocation(null);
        config.update(properties);

        Thread.sleep(2000); //TODO wait for zk client

        String karafName = System.getProperty("karaf.name");

        ZooKeeperUtils.createDefault(zooKeeper, ZkPath.CONFIG_AGENT.getPath(karafName), version);
        ZooKeeperUtils.createDefault(zooKeeper, ZkPath.CONFIG_VERSIONS_AGENT.getPath(version, karafName), "default");

        return null;
    }

    public void setConfigurationAdmin(ConfigurationAdmin configurationAdmin) {
        this.configurationAdmin = configurationAdmin;
    }

    public void setZooKeeper(IZKClient zooKeeper) {
        this.zooKeeper = zooKeeper;
    }
}
