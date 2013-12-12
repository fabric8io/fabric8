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
package io.fabric8.zookeeper.commands;

import org.apache.curator.framework.CuratorFramework;
import org.apache.felix.gogo.commands.Command;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@Command(name = "kill", scope = "zk", description = "Kills current session", detailedDescription = "classpath:kill-session.txt")
public class Kill extends ZooKeeperCommandSupport {


    @Override
    protected void doExecute(CuratorFramework curator) throws Exception {
        kill(curator.getZookeeperClient().getZooKeeper(), curator.getZookeeperClient().getCurrentConnectionString(), 60000);
    }


    /**
     * Kill the given ZK session
     *
     * @param client        the client to kill
     * @param connectString server connection string
     * @param maxMs         max time ms to wait for kill
     * @throws Exception errors
     */
    public static void kill(ZooKeeper client, String connectString, int maxMs) throws Exception {
        long startTicks = System.currentTimeMillis();

        final CountDownLatch sessionLostLatch = new CountDownLatch(1);
        Watcher sessionLostWatch = new Watcher() {
            @Override
            public void process(WatchedEvent event) {
                sessionLostLatch.countDown();
            }
        };
        client.exists("/___CURATOR_KILL_SESSION___" + System.nanoTime(), sessionLostWatch);

        final CountDownLatch connectionLatch = new CountDownLatch(1);
        Watcher connectionWatcher = new Watcher() {
            @Override
            public void process(WatchedEvent event) {
                if (event.getState() == Event.KeeperState.SyncConnected) {
                    connectionLatch.countDown();
                }
            }
        };
        ZooKeeper zk = new ZooKeeper(connectString, maxMs, connectionWatcher, client.getSessionId(), client.getSessionPasswd());
        try {
            if (!connectionLatch.await(maxMs, TimeUnit.MILLISECONDS)) {
                throw new Exception("KillSession could not establish duplicate session");
            }
            try {
                zk.close();
            } finally {
                zk = null;
            }

            while (client.getState().isConnected() && !sessionLostLatch.await(100, TimeUnit.MILLISECONDS)) {
                long elapsed = System.currentTimeMillis() - startTicks;
                if (elapsed > maxMs) {
                    throw new Exception("KillSession timed out waiting for session to expire");
                }
            }
        } finally {
            if (zk != null) {
                zk.close();
            }
        }
    }

}
