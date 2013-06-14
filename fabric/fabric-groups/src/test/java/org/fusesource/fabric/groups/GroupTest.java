/*
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
package org.fusesource.fabric.groups;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.RetryNTimes;
import org.apache.zookeeper.server.NIOServerCnxnFactory;
import org.apache.zookeeper.server.ServerConfig;
import org.apache.zookeeper.server.ZooKeeperServer;
import org.apache.zookeeper.server.persistence.FileTxnSnapLog;
import org.fusesource.fabric.groups.internal.ZooKeeperGroup;
import org.junit.Test;

import java.io.File;
import java.net.ServerSocket;
import java.util.Collection;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class GroupTest {

    private GroupListener listener = new GroupListener<NodeState>() {
        @Override
        public void groupEvent(Group<NodeState> group, GroupListener.GroupEvent event) {
            boolean connected = group.isConnected();
            boolean master = group.isMaster();
            Collection<NodeState> members = group.members().values();
            System.err.println("GroupEvent: " + event + " (connected=" + connected + ", master=" + master + ", members=" + members + ")");
        }
    };
    ;

    private int findFreePort() throws Exception {
        ServerSocket ss = new ServerSocket(0);
        int port = ss.getLocalPort();
        ss.close();
        return port;
    }

    private NIOServerCnxnFactory startZooKeeper(int port) throws Exception {
        ServerConfig cfg = new ServerConfig();
        cfg.parse(new String[] { Integer.toString(port), "target/zk/data" });

        ZooKeeperServer zkServer = new ZooKeeperServer();
        FileTxnSnapLog ftxn = new FileTxnSnapLog(new File(cfg.getDataLogDir()), new File(cfg.getDataDir()));
        zkServer.setTxnLogFactory(ftxn);
        zkServer.setTickTime(cfg.getTickTime());
        zkServer.setMinSessionTimeout(cfg.getMinSessionTimeout());
        zkServer.setMaxSessionTimeout(cfg.getMaxSessionTimeout());
        NIOServerCnxnFactory cnxnFactory = new NIOServerCnxnFactory();
        cnxnFactory.configure(cfg.getClientPortAddress(), cfg.getMaxClientCnxns());
        cnxnFactory.startup(zkServer);
        return cnxnFactory;
    }

    @Test
    public void testJoinAfterConnect() throws Exception {
        int port = findFreePort();

        CuratorFramework curator = CuratorFrameworkFactory.builder()
                .connectString("localhost:" + port)
                .retryPolicy(new RetryNTimes(10, 100))
                .build();
        curator.start();

        final Group<NodeState> group = new ZooKeeperGroup<NodeState>(curator, "/singletons/test" + System.currentTimeMillis(), NodeState.class);
        group.add(listener);
        group.start();

        assertFalse(group.isConnected());
        assertFalse(group.isMaster());

        NIOServerCnxnFactory cnxnFactory = startZooKeeper(port);

        curator.getZookeeperClient().blockUntilConnectedOrTimedOut();

        assertTrue(group.isConnected());
        assertFalse(group.isMaster());

        group.update(new NodeState("foo"));
        Thread.sleep(1000);
        assertTrue(group.isMaster());


        group.close();
        curator.close();
        cnxnFactory.shutdown();
        cnxnFactory.join();
    }

    @Test
    public void testJoinBeforeConnect() throws Exception {
        int port = findFreePort();

        CuratorFramework curator = CuratorFrameworkFactory.builder()
                .connectString("localhost:" + port)
                .retryPolicy(new RetryNTimes(10, 100))
                .build();
        curator.start();

        Group<NodeState> group = new ZooKeeperGroup<NodeState>(curator, "/singletons/test" + System.currentTimeMillis(), NodeState.class);
        group.add(listener);
        group.start();

        assertFalse(group.isConnected());
        assertFalse(group.isMaster());
        group.update(new NodeState("foo"));

        NIOServerCnxnFactory cnxnFactory = startZooKeeper(port);

        curator.getZookeeperClient().blockUntilConnectedOrTimedOut();

        assertTrue(group.isConnected());
        Thread.sleep(1000);
        assertTrue(group.isMaster());


        group.close();
        curator.close();
        cnxnFactory.shutdown();
        cnxnFactory.join();
    }

    @Test
    public void testRejoinAfterDisconnect() throws Exception {
        int port = findFreePort();

        CuratorFramework curator = CuratorFrameworkFactory.builder()
                .connectString("localhost:" + port)
                .retryPolicy(new RetryNTimes(10, 100))
                .build();
        curator.start();

        NIOServerCnxnFactory cnxnFactory = startZooKeeper(port);
        Group<NodeState> group = new ZooKeeperGroup<NodeState>(curator, "/singletons/test" + System.currentTimeMillis(), NodeState.class);
        group.add(listener);
        group.update(new NodeState("foo"));
        group.start();

        curator.getZookeeperClient().blockUntilConnectedOrTimedOut();
        Thread.sleep(1000);

        assertTrue(group.isConnected());
        assertTrue(group.isMaster());

        cnxnFactory.shutdown();
        cnxnFactory.join();

        Thread.sleep(1000);

        assertFalse(group.isConnected());
        assertFalse(group.isMaster());

        cnxnFactory = startZooKeeper(port);

        curator.getZookeeperClient().blockUntilConnectedOrTimedOut();
        Thread.sleep(1000);

        assertTrue(group.isConnected());
        assertTrue(group.isMaster());

        group.close();
        curator.close();
        cnxnFactory.shutdown();
        cnxnFactory.join();
    }

}
