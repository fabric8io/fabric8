/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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
import org.fusesource.fabric.internal.ZooKeeperUtils;
import org.fusesource.fabric.zookeeper.ZkPath;
import org.linkedin.zookeeper.client.IZKClient;
import org.osgi.service.cm.ConfigurationAdmin;

import java.util.Properties;

@Command(name = "join", scope = "fabric", description = "Join fabric cluster", detailedDescription = "classpath:join.txt")
public class Join extends OsgiCommandSupport {

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
