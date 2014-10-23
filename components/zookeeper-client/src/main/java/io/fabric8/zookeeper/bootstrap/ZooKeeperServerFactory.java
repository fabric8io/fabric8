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
package io.fabric8.zookeeper.bootstrap;

import org.apache.deltaspike.core.api.config.ConfigProperty;
import org.apache.zookeeper.server.NIOServerCnxnFactory;
import org.apache.zookeeper.server.ServerCnxnFactory;
import org.apache.zookeeper.server.ServerConfig;
import org.apache.zookeeper.server.ServerStats;
import org.apache.zookeeper.server.ZKDatabase;
import org.apache.zookeeper.server.ZooKeeperServer;
import org.apache.zookeeper.server.persistence.FileTxnSnapLog;
import org.apache.zookeeper.server.quorum.QuorumPeer;
import org.apache.zookeeper.server.quorum.QuorumPeerConfig;
import org.apache.zookeeper.server.quorum.QuorumPeerConfig.ConfigException;
import org.apache.zookeeper.server.quorum.QuorumStats;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Properties;

public class ZooKeeperServerFactory  {
    static final Logger LOGGER = LoggerFactory.getLogger(ZooKeeperServerFactory.class);

    private final QuorumPeerConfig peerConfig;
    private final String serverId;

    private SimpleServer simplerServer;
    private ClusteredServer clusteredServer;

/*
    @Reference(referenceInterface = RuntimeProperties.class)
    private final ValidatingReference<RuntimeProperties> runtimeProperties = new ValidatingReference<RuntimeProperties>();
    @Reference(referenceInterface = BootstrapConfiguration.class)
    private final ValidatingReference<BootstrapConfiguration> bootstrapConfiguration = new ValidatingReference<BootstrapConfiguration>();
*/

    private Destroyable destroyable;
    private ServiceRegistration<?> registration;
    private String zooKeeperUrl;

    public ZooKeeperServerFactory(QuorumPeerConfig peerConfig, String serverId) throws IOException, InterruptedException {
        this.peerConfig = peerConfig;
        this.serverId = serverId;

        LOGGER.info("Creating zookeeper server with: {}", peerConfig);

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

            updateZooKeeperURL(cnxnFactory.getLocalAddress(), cnxnFactory.getLocalPort());

            // Register stats provider
            this.clusteredServer = new ClusteredServer(quorumPeer);
/*
            registration = context.registerService(QuorumStats.Provider.class, server, null);

*/
        } else {
            ServerConfig serverConfig = getServerConfig(peerConfig);

            ZooKeeperServer zkServer = new ZooKeeperServer();
            FileTxnSnapLog ftxn = new FileTxnSnapLog(new File(serverConfig.getDataLogDir()), new File(serverConfig.getDataDir()));
            zkServer.setTxnLogFactory(ftxn);
            zkServer.setTickTime(serverConfig.getTickTime());
            zkServer.setMinSessionTimeout(serverConfig.getMinSessionTimeout());
            zkServer.setMaxSessionTimeout(serverConfig.getMaxSessionTimeout());
            NIOServerCnxnFactory cnxnFactory = new NIOServerCnxnFactory() {
                protected void configureSaslLogin() throws IOException {
                }
            };
            InetSocketAddress clientPortAddress = serverConfig.getClientPortAddress();
            cnxnFactory.configure(clientPortAddress, serverConfig.getMaxClientCnxns());
            updateZooKeeperURL(cnxnFactory.getLocalAddress(), cnxnFactory.getLocalPort());

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
            this.simplerServer = new SimpleServer(zkServer, cnxnFactory);
/*
            registration = context.registerService(ServerStats.Provider.class, server, null);
*/
        }
    }

    private void updateZooKeeperURL(InetSocketAddress localAddress, int localPort) {
        System.out.println("localAddress: " + localAddress + " localPort " + localPort);
        if (localAddress != null) {
            InetAddress address = localAddress.getAddress();
            String hostName;
            if (address != null) {
                hostName = address.getHostName();
            } else {
                hostName = localAddress.getHostName();
            }
            //hostName = "localhost";
            zooKeeperUrl = hostName + ":" + localPort;
            LOGGER.info("ZooKeeper URL to local ensemble is: " + zooKeeperUrl);
        } else {
            throw new IllegalStateException("No zookeeper URL can be found for the ensemble server!");
        }
    }

    public String getZooKeeperUrl() {
        return zooKeeperUrl;
    }

    public void destroy() throws Exception {
        LOGGER.info("Destroying zookeeper server: {}", destroyable);
        if (registration != null) {
            registration.unregister();
            registration = null;
        }
        if (destroyable != null) {
            destroyable.destroy();
            destroyable = null;
        }
    }

    private QuorumPeerConfig getPeerConfig(Properties props) throws IOException, ConfigException {
        QuorumPeerConfig peerConfig = new QuorumPeerConfig();
        peerConfig.parseProperties(props);
        LOGGER.info("Created zookeeper peer configuration: {}", peerConfig);
        return peerConfig;
    }

    private ServerConfig getServerConfig(QuorumPeerConfig peerConfig) {
        ServerConfig serverConfig = new ServerConfig();
        serverConfig.readFrom(peerConfig);
        LOGGER.info("Created zookeeper server configuration: {}", serverConfig);
        return serverConfig;
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
            if (server.getZKDatabase() != null) {
                // see https://issues.apache.org/jira/browse/ZOOKEEPER-1459
                server.getZKDatabase().close();
            }
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
