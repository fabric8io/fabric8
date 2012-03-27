/*
 * Copyright (C) FuseSource, Inc.
 *   http://fusesource.com
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package org.fusesource.fabric.maven;

import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooDefs;
import org.fusesource.fabric.zookeeper.IZKClient;
import org.fusesource.fabric.zookeeper.ZkPath;
import org.fusesource.fabric.zookeeper.internal.OsgiZkClient;
import org.linkedin.zookeeper.client.LifecycleListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MavenProxyRegistration implements LifecycleListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(MavenProxyRegistration.class);

    private int port = 8040;
    private String node = null;
    private IZKClient zookeeper = null;
    private String name = System.getProperty("karaf.name");

    public void destroy() throws InterruptedException, KeeperException {
        if (zookeeper != null && zookeeper.isConnected() && node != null) {
            zookeeper.delete(node);
        }
    }

    @Override
    public void onConnected() {
        String mavenProxyUrl = "http://${zk:" + name + "/ip}:" + port + "/";;
        String parentPath = ZkPath.CONFIGS_MAVEN_PROXY.getPath();
        String path = parentPath + "/p_";
        try {
            if (zookeeper.exists(parentPath) == null) {
                zookeeper.createWithParents(parentPath, null, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
            }

            if (zookeeper.exists(path) == null || (node != null && zookeeper.exists(node) == null)) {
                node = zookeeper.create(path, mavenProxyUrl.getBytes("UTF-8"), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL_SEQUENTIAL);
            }
        } catch (Exception e) {
            LOGGER.error("Failed to register maven proxy.", e);
        }
    }


    @Override
    public void onDisconnected() {
        node = null;
    }

    private String getLocalHostName() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            throw new RuntimeException("Unable to get address", e);
        }
    }

    public IZKClient getZookeeper() {
        return zookeeper;
    }

    public void setZookeeper(IZKClient zookeeper) {
        this.zookeeper = zookeeper;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }
}
