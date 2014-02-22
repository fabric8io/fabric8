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
package io.fabric8.zookeeper.bootstrap;

import io.fabric8.api.Constants;
import io.fabric8.api.CreateEnsembleOptions;
import io.fabric8.api.RuntimeProperties;
import io.fabric8.api.jcip.ThreadSafe;
import io.fabric8.api.scr.AbstractComponent;
import io.fabric8.api.scr.ValidatingReference;
import io.fabric8.utils.SystemProperties;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Modified;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.zookeeper.server.NIOServerCnxnFactory;
import org.apache.zookeeper.server.ServerConfig;
import org.apache.zookeeper.server.ServerStats;
import org.apache.zookeeper.server.ZKDatabase;
import org.apache.zookeeper.server.ZooKeeperServer;
import org.apache.zookeeper.server.persistence.FileTxnSnapLog;
import org.apache.zookeeper.server.quorum.QuorumPeer;
import org.apache.zookeeper.server.quorum.QuorumPeerConfig;
import org.apache.zookeeper.server.quorum.QuorumPeerConfig.ConfigException;
import org.apache.zookeeper.server.quorum.QuorumStats;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ThreadSafe
@Component(name = Constants.ZOOKEEPER_SERVER_PID, label = "Fabric8 ZooKeeper Server Factory", policy = ConfigurationPolicy.REQUIRE, immediate = true, metatype = true)
@org.apache.felix.scr.annotations.Properties({
        @Property(name = "tickTime", label = "Tick Time", description = "The basic time unit in milliseconds used by ZooKeeper. It is used to do heartbeats and the minimum session timeout will be twice the tickTime"),
        @Property(name = "dataDir", label = "Data Directory", description = "The location to store the in-memory database snapshots and, unless specified otherwise, the transaction log of updates to the database"),
        @Property(name = "clientPort", label = "Client Port", description = "The port to listen for client connections"),
        @Property(name = "initLimit", label = "Init Limit", description = "The amount of time in ticks (see tickTime), to allow followers to connect and sync to a leader. Increased this value as needed, if the amount of data managed by ZooKeeper is large"),
        @Property(name = "syncLimit", label = "Sync Limit", description = "The amount of time, in ticks (see tickTime), to allow followers to sync with ZooKeeper. If followers fall too far behind a leader, they will be dropped"),
        @Property(name = "dataLogDir", label = "Data Log Directory", description = "This option will direct the machine to write the transaction log to the dataLogDir rather than the dataDir. This allows a dedicated log device to be used, and helps avoid competition between logging and snaphots")
}
)
public class ZooKeeperServerFactory extends AbstractComponent {

    static final Logger LOGGER = LoggerFactory.getLogger(ZooKeeperServerFactory.class);

    @Reference(referenceInterface = RuntimeProperties.class)
    private final ValidatingReference<RuntimeProperties> runtimeProperties = new ValidatingReference<RuntimeProperties>();
    @Reference(referenceInterface = BootstrapConfiguration.class)
    private final ValidatingReference<BootstrapConfiguration> bootstrapConfiguration = new ValidatingReference<BootstrapConfiguration>();

    private Destroyable destroyable;
    private ServiceRegistration<?> registration;

    @Activate
    void activate(BundleContext context, Map<String, ?> configuration) throws Exception {
        // A valid configuration must contain a dataDir entry
        if (configuration.containsKey("dataDir")) {
            destroyable = activateInternal(context, configuration);
        }
        activateComponent();
    }

    @Modified
    void modified(BundleContext context, Map<String, ?> configuration) throws Exception {
        deactivateInternal();
        destroyable = activateInternal(context, configuration);
    }

    @Deactivate
    void deactivate() throws Exception {
        deactivateComponent();
        deactivateInternal();
    }

    private Destroyable activateInternal(BundleContext context, Map<String, ?> configuration) throws Exception {
        LOGGER.info("Creating zookeeper server with: {}", configuration);

        Properties props = new Properties();
        for (Entry<String, ?> entry : configuration.entrySet()) {
            props.put(entry.getKey(), entry.getValue());
        }

        // Remove the dependency on the current dir from dataDir
        String dataDir = props.getProperty("dataDir");
        if (dataDir != null && dataDir.startsWith(CreateEnsembleOptions.DEFAULT_DATA_DIR)) {
            RuntimeProperties sysprops = runtimeProperties.get();
            dataDir = dataDir.substring(dataDir.indexOf('/'));
            dataDir = sysprops.getProperty(SystemProperties.KARAF_DATA) + dataDir;
            props.setProperty("dataDir", dataDir);
        }

        // Create myid file
        String serverId = (String) props.get("server.id");
        if (serverId != null) {
            props.remove("server.id");
            File myId = new File(dataDir, "myid");
            if (myId.exists() && !myId.delete()) {
                throw new IOException("Failed to delete " + myId);
            }
            if (myId.getParentFile() == null || (!myId.getParentFile().exists() && !myId.getParentFile().mkdirs())) {
                throw new IOException("Failed to create " + myId.getParent());
            }
            FileOutputStream fos = new FileOutputStream(myId);
            try {
                fos.write((serverId + "\n").getBytes());
            } finally {
                fos.close();
            }
        }

        QuorumPeerConfig peerConfig = getPeerConfig(props);

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

    private void deactivateInternal() throws Exception {
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

    void bindRuntimeProperties(RuntimeProperties service) {
        this.runtimeProperties.bind(service);
    }

    void unbindRuntimeProperties(RuntimeProperties service) {
        this.runtimeProperties.unbind(service);
    }

    void bindBootstrapConfiguration(BootstrapConfiguration service) {
        this.bootstrapConfiguration.bind(service);
    }

    void unbindBootstrapConfiguration(BootstrapConfiguration service) {
        this.bootstrapConfiguration.unbind(service);
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
