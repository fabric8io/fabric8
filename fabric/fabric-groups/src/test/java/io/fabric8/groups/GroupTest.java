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
package io.fabric8.groups;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.RetryNTimes;
import org.apache.zookeeper.server.NIOServerCnxnFactory;
import org.apache.zookeeper.server.ServerConfig;
import org.apache.zookeeper.server.ZooKeeperServer;
import org.apache.zookeeper.server.persistence.FileTxnSnapLog;
import io.fabric8.groups.internal.ZooKeeperGroup;
import org.junit.Test;

import java.io.File;
import java.net.ServerSocket;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class GroupTest {

    private GroupListener listener = new GroupListener<NodeState>() {
        @Override
        public void groupEvent(Group<NodeState> group, GroupListener.GroupEvent event) {
            boolean connected = group.isConnected();
            boolean master = group.isMaster();
            if (connected) {
                Collection<NodeState> members = group.members().values();
                System.err.println("GroupEvent: " + event + " (connected=" + connected + ", master=" + master + ", members=" + members + ")");
            } else {
                System.err.println("GroupEvent: " + event + " (connected=" + connected + ", master=false)");
            }
        }
    };

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

        GroupCondition groupCondition = new GroupCondition();
        group.add(groupCondition);

        NIOServerCnxnFactory cnxnFactory = startZooKeeper(port);

        curator.getZookeeperClient().blockUntilConnectedOrTimedOut();

        assertTrue(groupCondition.waitForConnected(5, TimeUnit.SECONDS));
        assertFalse(group.isMaster());

        group.update(new NodeState("foo"));
        assertTrue(groupCondition.waitForMaster(5, TimeUnit.SECONDS));


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

        GroupCondition groupCondition = new GroupCondition();
        group.add(groupCondition);

        assertFalse(group.isConnected());
        assertFalse(group.isMaster());
        group.update(new NodeState("foo"));

        NIOServerCnxnFactory cnxnFactory = startZooKeeper(port);

        curator.getZookeeperClient().blockUntilConnectedOrTimedOut();

        assertTrue(groupCondition.waitForConnected(5, TimeUnit.SECONDS));
        assertTrue(groupCondition.waitForMaster(5, TimeUnit.SECONDS));


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

        GroupCondition groupCondition = new GroupCondition();
        group.add(groupCondition);

        curator.getZookeeperClient().blockUntilConnectedOrTimedOut();
        assertTrue(groupCondition.waitForConnected(5, TimeUnit.SECONDS));
        assertTrue(groupCondition.waitForMaster(5, TimeUnit.SECONDS));

        cnxnFactory.shutdown();
        cnxnFactory.join();

        groupCondition.waitForDisconnected(5, TimeUnit.SECONDS);
        group.remove(groupCondition);

        assertFalse(group.isConnected());
        assertFalse(group.isMaster());

        groupCondition = new GroupCondition();
        group.add(groupCondition);

        cnxnFactory = startZooKeeper(port);

        curator.getZookeeperClient().blockUntilConnectedOrTimedOut();
        curator.getZookeeperClient().blockUntilConnectedOrTimedOut();
        assertTrue(groupCondition.waitForConnected(5, TimeUnit.SECONDS));
        assertTrue(groupCondition.waitForMaster(5, TimeUnit.SECONDS));


        group.close();
        curator.close();
        cnxnFactory.shutdown();
        cnxnFactory.join();
    }

    //Tests that if close() is executed right after start(), there are no left over entries.
    //(see  https://github.com/jboss-fuse/fuse/issues/133)
    @Test
    public void testGroupClose() throws Exception {
        int port = findFreePort();
        NIOServerCnxnFactory cnxnFactory = startZooKeeper(port);

        CuratorFramework curator = CuratorFrameworkFactory.builder()
                .connectString("localhost:" + port)
                .retryPolicy(new RetryNTimes(10, 100))
                .build();
        curator.start();
        curator.getZookeeperClient().blockUntilConnectedOrTimedOut();
        String groupNode =  "/singletons/test" + System.currentTimeMillis();
        curator.create().creatingParentsIfNeeded().forPath(groupNode);

        for (int i = 0; i < 10; i++) {
            Group<NodeState> group = new ZooKeeperGroup<NodeState>(curator, groupNode, NodeState.class);
            group.add(listener);
            group.update(new NodeState("foo"));
            group.start();
            group.close();
            List<String> entries = curator.getChildren().forPath(groupNode);
            assertTrue(entries.isEmpty());
        }

        curator.close();
        cnxnFactory.shutdown();
        cnxnFactory.join();
    }

    private class GroupCondition implements GroupListener<NodeState> {
        private CountDownLatch connected = new CountDownLatch(1);
        private CountDownLatch master = new CountDownLatch(1);
        private CountDownLatch disconnected = new CountDownLatch(1);

        @Override
        public void groupEvent(Group<NodeState> group, GroupEvent event) {
            switch (event) {
                case CONNECTED:
                case CHANGED:
                    connected.countDown();
                    if (group.isMaster()) {
                        master.countDown();
                    }
                    break;
                case DISCONNECTED:
                    disconnected.countDown();
            }
        }

        public boolean waitForConnected(long time, TimeUnit timeUnit) throws InterruptedException {
            return connected.await(time, timeUnit);
        }

        public boolean waitForDisconnected(long time, TimeUnit timeUnit) throws InterruptedException {
            return disconnected.await(time, timeUnit);
        }

        public boolean waitForMaster(long time, TimeUnit timeUnit) throws InterruptedException {
            return master.await(time, timeUnit);
        }
    }
}
