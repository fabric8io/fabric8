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
package io.fabric8.process.spring.boot.registry;

import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.curator.retry.RetryOneTime;
import org.apache.zookeeper.server.NIOServerCnxnFactory;
import org.apache.zookeeper.server.ServerConfig;
import org.apache.zookeeper.server.ZooKeeperServer;
import org.apache.zookeeper.server.persistence.FileTxnSnapLog;

import java.io.File;
import java.io.IOException;

import static java.util.UUID.randomUUID;

public class ZooKeeperProcessRegistries {

    private static final int DEFAULT_ZK_BASE_SLEEP = 1000;
    private static final int DEFAULT_ZK_MAX_RETRIES = 3;

    public static RetryPolicy defaultRetryPolicy() {
        return new RetryOneTime(1);
    }

    public static CuratorFramework newCurator(String hosts) {
        CuratorFramework curator = CuratorFrameworkFactory.builder().
                connectString(hosts).connectionTimeoutMs(50).retryPolicy(defaultRetryPolicy()).
                build();
        curator.start();
        return curator;
    }

    public static NIOServerCnxnFactory zooKeeperServer(int port) {
        try {
            ServerConfig cfg = new ServerConfig();
            String zkData = "target/zk/data/" + randomUUID();
            cfg.parse(new String[]{Integer.toString(port), zkData});

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
        } catch (InterruptedException | IOException e) {
            throw new RuntimeException(e);
        }
    }

}
