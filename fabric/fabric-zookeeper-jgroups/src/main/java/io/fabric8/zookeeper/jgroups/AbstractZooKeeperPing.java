/**
 *  Copyright 2005-2014 Red Hat, Inc.
 *
 *  Red Hat licenses this file to you under the Apache License, version
 *  2.0 (the "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 *  implied.  See the License for the specific language governing
 *  permissions and limitations under the License.
 */
package io.fabric8.zookeeper.jgroups;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.utils.ZKPaths;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.jgroups.Address;
import org.jgroups.Event;
import org.jgroups.protocols.FILE_PING;
import org.jgroups.protocols.PingData;
import org.jgroups.util.Util;

public abstract class AbstractZooKeeperPing extends FILE_PING {
    private static final String ROOT_PATH = "/fabric/registry/jgroups/";

    private volatile String discoveryPath;
    private volatile String localNodePath;

    protected CuratorFramework curator;

    protected abstract CuratorFramework createCurator() throws KeeperException;

    protected CreateMode getCreateMode() throws KeeperException {
        return CreateMode.EPHEMERAL;
    }

    @Override
    public void init() throws Exception {
        curator = createCurator();
        super.init();
    }

    @Override
    public void stop() {
        try {
            removeNode(localNodePath);
        } finally {
            super.stop();
        }
    }

    public Object down(Event evt) {
        switch (evt.getType()) {
            case Event.CONNECT:
            case Event.CONNECT_USE_FLUSH:
            case Event.CONNECT_WITH_STATE_TRANSFER:
            case Event.CONNECT_WITH_STATE_TRANSFER_USE_FLUSH:
                discoveryPath = ROOT_PATH + evt.getArg();
                localNodePath = discoveryPath + "/" + addressAsString(local_addr);
                _createRootDir();
                break;
        }
        return super.down(evt);
    }

    /**
     * Creates the root node in ZooKeeper (/fabric/registry/jgroups
     */
    @Override
    protected void createRootDir() {
        // empty on purpose to prevent dir from being created in the local file system
    }

    protected void _createRootDir() {
        try {
            if (curator.checkExists().forPath(localNodePath) == null) {
                curator.create().creatingParentsIfNeeded().withMode(getCreateMode()).forPath(localNodePath);
            }
        } catch (Exception e) {
            throw new RuntimeException(String.format("Failed to create dir %s in ZooKeeper.", localNodePath), e);
        }
    }

    /**
     * Reads all information from the given directory under clustername
     *
     * @return all data
     */
    protected synchronized List<PingData> readAll(String clusterName) {
        List<PingData> retval = new ArrayList<>();
        try {
            for (String node : curator.getChildren().forPath(discoveryPath)) {
                String nodePath = ZKPaths.makePath(discoveryPath, node);
                PingData nodeData = readPingData(nodePath);
                if (nodeData != null) {
                    retval.add(nodeData);
                }
            }

        } catch (Exception e) {
            log.debug(String.format("Failed to read ping data from ZooKeeper for cluster: %s", clusterName), e);
        }
        return retval;
    }

    @Override
    protected synchronized void writeToFile(PingData data, String clustername) {
        writePingData(data);
    }

    protected synchronized PingData readPingData(String path) {
        PingData retval = null;
        DataInputStream in = null;
        try {
            byte[] bytes = curator.getData().forPath(path);
            in = new DataInputStream(new ByteArrayInputStream(bytes));
            PingData tmp = new PingData();
            tmp.readFrom(in);
            return tmp;
        } catch (Exception e) {
            log.debug(String.format("Failed to read ZooKeeper znode: %s", path), e);
        } finally {
            Util.close(in);
        }
        return retval;
    }

    protected synchronized void writePingData(PingData data) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = null;
        try {
            dos = new DataOutputStream(baos);

            data.writeTo(dos);

            if (curator.checkExists().forPath(localNodePath) == null) {
                curator.create().creatingParentsIfNeeded().withMode(getCreateMode()).forPath(localNodePath, baos.toByteArray());
            } else {
                curator.setData().forPath(localNodePath, baos.toByteArray());
            }
        } catch (Exception ex) {
            log.error("Error saving ping data", ex);
        } finally {
            Util.close(dos);
            Util.close(baos);
        }
    }

    protected void removeNode(String path) {
        try {
            curator.delete().forPath(path);
        } catch (KeeperException.NoNodeException e) {
            // just log this, to keep down to 1 call, instead of 2 (exists + delete)
            // coord nodes already remove this in handleView(), hence "expecting" this exception
            if (log.isTraceEnabled()) {
                log.trace(String.format("Node '%s' already removed: %s", path, e));
            }
        } catch (Exception e) {
            log.error(String.format("Failed removing %s", path), e);
        }
    }

    protected void remove(String clustername, Address addr) {
        removeNode(discoveryPath + "/" + addressAsString(addr));
    }
}
