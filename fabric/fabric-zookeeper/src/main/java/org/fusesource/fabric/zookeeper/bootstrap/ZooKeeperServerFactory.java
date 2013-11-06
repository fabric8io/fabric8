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
package org.fusesource.fabric.zookeeper.bootstrap;

import java.io.File;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.zookeeper.server.NIOServerCnxnFactory;
import org.apache.zookeeper.server.ServerConfig;
import org.apache.zookeeper.server.ServerStats;
import org.apache.zookeeper.server.ZKDatabase;
import org.apache.zookeeper.server.ZooKeeperServer;
import org.apache.zookeeper.server.persistence.FileTxnSnapLog;
import org.apache.zookeeper.server.quorum.QuorumPeer;
import org.apache.zookeeper.server.quorum.QuorumPeerConfig;
import org.apache.zookeeper.server.quorum.QuorumStats;
import org.fusesource.fabric.api.jcip.ThreadSafe;
import org.fusesource.fabric.api.scr.AbstractComponent;
import org.fusesource.fabric.api.scr.ValidatingReference;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ThreadSafe
@Component(name = "org.fusesource.fabric.zookeeper.server.factory", immediate = true)
@Service(ZooKeeperServerFactory.class)
public class ZooKeeperServerFactory extends AbstractComponent {

    final Logger LOGGER = LoggerFactory.getLogger(getClass());

    @Reference(referenceInterface = ZooKeeperServerConfiguration.class)
    private final ValidatingReference<ZooKeeperServerConfiguration> serverConfiguration = new ValidatingReference<ZooKeeperServerConfiguration>();

    private Destroyable destroyable;
    private ServiceRegistration<?> registration;

    @Activate
    void activate(BundleContext context) {
        LOGGER.info("Creating zookeeper server with");
        try {
            destroyable = activateServer(context);
        } catch (RuntimeException rte) {
            throw rte;
        } catch (Exception ex) {
            throw new IllegalStateException("Cannot activate ZooKeeper server", ex);
        }
        activateComponent();
    }

    @Deactivate
    void deactivate() throws Exception {
        deactivateComponent();
        if (registration != null) {
            registration.unregister();
        }
        if (destroyable != null) {
            destroyable.destroy();
        }
    }

    private Destroyable activateServer(BundleContext context) throws Exception {

        ZooKeeperServerConfiguration configuration = serverConfiguration.get();
        QuorumPeerConfig peerConfig = configuration.getPeerConfig();

        if (!peerConfig.getServers().isEmpty()) {
            NIOServerCnxnFactory cnxnFactory = new NIOServerCnxnFactory();
            cnxnFactory.configure(peerConfig.getClientPortAddress(), peerConfig.getMaxClientCnxns());

            QuorumPeer quorumPeer = new QuorumPeer();
            quorumPeer.setClientPortAddress(peerConfig.getClientPortAddress());
            quorumPeer.setTxnFactory(new FileTxnSnapLog(new File(peerConfig.getDataLogDir()), new File(peerConfig.getDataDir())));
            quorumPeer.setQuorumPeers(peerConfig.getServers());
            quorumPeer.setElectionType(peerConfig.getElectionAlg());
            quorumPeer.setMyid(peerConfig.getServerId());
            quorumPeer.setTickTime(peerConfig.getTickTime());
            quorumPeer.setMinSessionTimeout(peerConfig.getMinSessionTimeout());
            quorumPeer.setMaxSessionTimeout(peerConfig.getMaxSessionTimeout());
            quorumPeer.setInitLimit(peerConfig.getInitLimit());
            quorumPeer.setSyncLimit(peerConfig.getSyncLimit());
            quorumPeer.setQuorumVerifier(peerConfig.getQuorumVerifier());
            quorumPeer.setCnxnFactory(cnxnFactory);
            quorumPeer.setZKDatabase(new ZKDatabase(quorumPeer.getTxnFactory()));
            quorumPeer.setLearnerType(peerConfig.getPeerType());

            try {
                LOGGER.debug("Starting quorum peer \"%s\" on address %s", quorumPeer.getMyid(), peerConfig.getClientPortAddress());
                quorumPeer.start();
                LOGGER.debug("Started quorum peer \"%s\"", quorumPeer.getMyid());
            } catch (Exception e) {
                LOGGER.warn(String.format("Failed to start quorum peer \"%s\", reason : %s ", quorumPeer.getMyid(), e.getMessage()));
                quorumPeer.shutdown();
                throw e;
            }

            // Register stats provider
            ClusteredServer server = new ClusteredServer(quorumPeer);
            registration = context.registerService(QuorumStats.Provider.class, server, null);

            return server;
        } else {
            ServerConfig serverConfig = configuration.getServerConfig();

            ZooKeeperServer zkServer = new ZooKeeperServer();
            FileTxnSnapLog ftxn = new FileTxnSnapLog(new File(serverConfig.getDataLogDir()), new File(serverConfig.getDataDir()));
            zkServer.setTxnLogFactory(ftxn);
            zkServer.setTickTime(serverConfig.getTickTime());
            zkServer.setMinSessionTimeout(serverConfig.getMinSessionTimeout());
            zkServer.setMaxSessionTimeout(serverConfig.getMaxSessionTimeout());
            NIOServerCnxnFactory cnxnFactory = new NIOServerCnxnFactory();
            cnxnFactory.configure(serverConfig.getClientPortAddress(), serverConfig.getMaxClientCnxns());

            try {
                LOGGER.debug("Starting ZooKeeper server on address %s", peerConfig.getClientPortAddress());
                cnxnFactory.startup(zkServer);
                LOGGER.debug("Started ZooKeeper server");
            } catch (Exception e) {
                LOGGER.warn(String.format("Failed to start ZooKeeper server, reason : %s", e));
                cnxnFactory.shutdown();
                throw e;
            }

            // Register stats provider
            SimpleServer server = new SimpleServer(zkServer, cnxnFactory);
            registration = context.registerService(ServerStats.Provider.class, server, null);

            return server;
        }
    }

    void bindServerConfiguration(ZooKeeperServerConfiguration service) {
        this.serverConfiguration.bind(service);
    }

    void unbindServerConfiguration(ZooKeeperServerConfiguration service) {
        this.serverConfiguration.unbind(service);
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
