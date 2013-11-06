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
package org.fusesource.fabric.zookeeper.internal;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Properties;

import org.apache.zookeeper.server.NIOServerCnxnFactory;
import org.apache.zookeeper.server.ServerConfig;
import org.apache.zookeeper.server.ServerStats;
import org.apache.zookeeper.server.ZKDatabase;
import org.apache.zookeeper.server.ZooKeeperServer;
import org.apache.zookeeper.server.persistence.FileTxnSnapLog;
import org.apache.zookeeper.server.quorum.QuorumPeer;
import org.apache.zookeeper.server.quorum.QuorumPeerConfig;
import org.apache.zookeeper.server.quorum.QuorumStats;
import org.osgi.framework.BundleContext;

public class ZKServerFactoryBean extends BaseManagedServiceFactory<Object> {

    public ZKServerFactoryBean(BundleContext context) {
        super(context, "ZooKeeper Server Factory");
    }

    @Override
    protected Object doCreate(Dictionary properties) throws Exception {
        LOGGER.info("Creating zookeeper server with properties: {}", properties);
        Properties props = new Properties();
        for (Enumeration ek = properties.keys(); ek.hasMoreElements();) {
            Object key = ek.nextElement();
            Object val = properties.get(key);
            props.put(key.toString(), val != null ? val.toString() : "");
        }

        // Create myid file
        String serverId = props.getProperty("server.id");
        if (serverId != null) {
            props.remove("server.id");
            File myId = new File(props.getProperty("dataDir"), "myid");
            if (myId.exists() && !myId.delete()) {
                throw new IOException("Failed to delete " + myId);
            }
            if (myId.getParentFile() == null || (!myId.getParentFile().exists() && !myId.getParentFile().mkdirs()))  {
                throw new IOException("Failed to create " + myId.getParent());
            }
            FileOutputStream fos = new FileOutputStream(myId);
            try {
                fos.write((serverId + "\n").getBytes());
            } finally {
                fos.close();
            }
        }

        // Load properties
        QuorumPeerConfig config = new QuorumPeerConfig();
        config.parseProperties(props);
        if (!config.getServers().isEmpty()) {
            NIOServerCnxnFactory cnxnFactory = new NIOServerCnxnFactory();
            cnxnFactory.configure(config.getClientPortAddress(), config.getMaxClientCnxns());

            QuorumPeer quorumPeer = new QuorumPeer();
            quorumPeer.setClientPortAddress(config.getClientPortAddress());
            quorumPeer.setTxnFactory(new FileTxnSnapLog(
                    new File(config.getDataLogDir()),
                    new File(config.getDataDir())));
            quorumPeer.setQuorumPeers(config.getServers());
            quorumPeer.setElectionType(config.getElectionAlg());
            quorumPeer.setMyid(config.getServerId());
            quorumPeer.setTickTime(config.getTickTime());
            quorumPeer.setMinSessionTimeout(config.getMinSessionTimeout());
            quorumPeer.setMaxSessionTimeout(config.getMaxSessionTimeout());
            quorumPeer.setInitLimit(config.getInitLimit());
            quorumPeer.setSyncLimit(config.getSyncLimit());
            quorumPeer.setQuorumVerifier(config.getQuorumVerifier());
            quorumPeer.setCnxnFactory(cnxnFactory);
            quorumPeer.setZKDatabase(new ZKDatabase(quorumPeer.getTxnFactory()));
            quorumPeer.setLearnerType(config.getPeerType());

            try {
                LOGGER.debug("Starting quorum peer \"%s\" on address %s", quorumPeer.getMyid(), config.getClientPortAddress());
                quorumPeer.start();
                LOGGER.debug("Started quorum peer \"%s\"", quorumPeer.getMyid());
            } catch (Exception e) {
                LOGGER.warn(String.format("Failed to start quorum peer \"%s\", reason : %s ", quorumPeer.getMyid(), e.getMessage()));
                quorumPeer.shutdown();
                throw e;
            }
            return new ClusteredServer(quorumPeer);
        } else {
            ServerConfig cfg = new ServerConfig();
            cfg.readFrom(config);

            ZooKeeperServer zkServer = new ZooKeeperServer();
            FileTxnSnapLog ftxn = new FileTxnSnapLog(new
                    File(cfg.getDataLogDir()), new File(cfg.getDataDir()));
            zkServer.setTxnLogFactory(ftxn);
            zkServer.setTickTime(cfg.getTickTime());
            zkServer.setMinSessionTimeout(cfg.getMinSessionTimeout());
            zkServer.setMaxSessionTimeout(cfg.getMaxSessionTimeout());
            NIOServerCnxnFactory cnxnFactory = new NIOServerCnxnFactory();
            cnxnFactory.configure(cfg.getClientPortAddress(), cfg.getMaxClientCnxns());

            try {
                LOGGER.debug("Starting ZooKeeper server on address %s", config.getClientPortAddress());
                cnxnFactory.startup(zkServer);
                LOGGER.debug("Started ZooKeeper server");
            } catch (Exception e) {
                LOGGER.warn(String.format("Failed to start ZooKeeper server, reason : %s", e));
                cnxnFactory.shutdown();
                throw e;
            }
            return new SimpleServer(zkServer, cnxnFactory);
        }
    }

    @Override
    protected void doDestroy(Object obj) throws Exception {
        LOGGER.info("Destroying zookeeper server");
        ((Destroyable) obj).destroy();
    }

    @Override
    protected String[] getExposedClasses(Object obj) {
        if (obj instanceof  ServerStats.Provider) {
            return new String[] { ServerStats.Provider.class.getName() };
        } else if (obj instanceof QuorumStats.Provider) {
            return new String[] { QuorumStats.Provider.class.getName() };
        } else {
            throw new IllegalStateException("Unsupported service type: " + obj.getClass().toString());
        }
    }

    interface Destroyable {
        void destroy() throws Exception;
    }

    static class SimpleServer implements Destroyable, ServerStats.Provider {
        private final ZooKeeperServer server;
        private final NIOServerCnxnFactory cnxnFactory;

        SimpleServer(ZooKeeperServer server, NIOServerCnxnFactory cnxnFactory) {
            this.server = server;
            this.cnxnFactory = cnxnFactory;
        }

        @Override
        public void destroy() throws Exception {
            cnxnFactory.shutdown();
            cnxnFactory.join();
        }

        @Override
        public long getOutstandingRequests() {
            return server.getOutstandingRequests();
        }

        @Override
        public long getLastProcessedZxid() {
            return server.getLastProcessedZxid();
        }

        @Override
        public String getState() {
            return server.getState();
        }

        @Override
        public int getNumAliveConnections() {
            return server.getNumAliveConnections();
        }
    }

    static class ClusteredServer implements Destroyable, QuorumStats.Provider {
        private final QuorumPeer peer;

        ClusteredServer(QuorumPeer peer) {
            this.peer = peer;
        }

        @Override
        public void destroy() throws Exception {
            peer.shutdown();
            peer.join();
        }

        @Override
        public String[] getQuorumPeers() {
            return peer.getQuorumPeers();
        }

        @Override
        public String getServerState() {
            return peer.getServerState();
        }
    }

}
