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
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Modified;
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
import org.osgi.service.cm.ConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ThreadSafe
@Component(name = "org.fusesource.fabric.zookeeper.server", policy = ConfigurationPolicy.REQUIRE, immediate = true)
@Service(ZooKeeperServerFactory.class)
public class ZooKeeperServerFactory extends AbstractComponent {

    private static final Logger LOGGER = LoggerFactory.getLogger(ZooKeeperServerFactory.class);
    private static final long TIMEOUT_BEFORE_INTERRUPT = 30000;

    @Reference(referenceInterface = BootstrapConfiguration.class)
    private final ValidatingReference<BootstrapConfiguration> bootstrapConfiguration = new ValidatingReference<BootstrapConfiguration>();

    private final AtomicBoolean destroyed = new AtomicBoolean(false);
    private final ExecutorService executor = Executors.newSingleThreadExecutor();;
    private final Map<String, Pair<Object, ServiceRegistration<?>>> services = new ConcurrentHashMap<String, Pair<Object, ServiceRegistration<?>>>();;

    private BundleContext context;

    @Activate
    void activate(BundleContext context, Map<String, ?> configuration) throws Exception {
        this.context = context;

        activateInternal(configuration);

        activateComponent();
    }

    private Properties toProperties(Map<String, ?> configuration) {
        Properties properties = new Properties();
        for (Map.Entry<String, ?> entry : configuration.entrySet()) {
            String key = entry.getKey();
            Object val = entry.getValue();
            properties.put(key, val != null ? val.toString() : "");
        }
        return properties;
    }

    private Dictionary<String, ?> toDictionary(Map<String, ?> configuration) {
        Dictionary<String, Object> properties = new Hashtable<String, Object>();
        for (Map.Entry<String, ?> entry : configuration.entrySet()) {
            String key = entry.getKey();
            Object val = entry.getValue();
            properties.put(key, val != null ? val.toString() : "");
        }
        return properties;
    }

    @Modified
    void updated(final Map<String, ?> configuration) throws ConfigurationException {
        final String pid = (String) configuration.get("service.pid");
        LOGGER.info("Configuration {} updated: {}", pid, configuration);
        if (destroyed.get()) {
            return;
        }
        executor.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    internalUpdate(pid, configuration);
                } catch (Throwable t) {
                    LOGGER.warn("Error destroying service for ManagedServiceFactory " + this, t);
                }
            }
        });
    }

    @Deactivate
    void deactivate() {
        deactivateComponent();
        if (destroyed.compareAndSet(false, true)) {
            executor.shutdown();
            try {
                executor.awaitTermination(TIMEOUT_BEFORE_INTERRUPT, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                throw new RuntimeException("Shutdown interrupted");
            }
            if (!executor.isTerminated()) {
                executor.shutdownNow();
                try {
                    executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
                } catch (InterruptedException e) {
                    throw new RuntimeException("Shutdown interrupted");
                }
            }

            while (!services.isEmpty()) {
                String pid = services.keySet().iterator().next();
                internalDelete(pid);
            }
        }
    }

    private Object activateInternal(Map<String, ?> configuration) throws Exception {
        LOGGER.info("Creating zookeeper server with properties: {}", configuration);

        // Create myid file
        Properties properties = toProperties(configuration);
        String serverId = properties.getProperty("server.id");
        if (serverId != null) {
            properties.remove("server.id");
            File myId = new File(properties.getProperty("dataDir"), "myid");
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

        // Load properties
        QuorumPeerConfig config = new QuorumPeerConfig();
        config.parseProperties(properties);
        if (!config.getServers().isEmpty()) {
            NIOServerCnxnFactory cnxnFactory = new NIOServerCnxnFactory();
            cnxnFactory.configure(config.getClientPortAddress(), config.getMaxClientCnxns());

            QuorumPeer quorumPeer = new QuorumPeer();
            quorumPeer.setClientPortAddress(config.getClientPortAddress());
            quorumPeer.setTxnFactory(new FileTxnSnapLog(new File(config.getDataLogDir()), new File(config.getDataDir())));
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
            FileTxnSnapLog ftxn = new FileTxnSnapLog(new File(cfg.getDataLogDir()), new File(cfg.getDataDir()));
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

    private void doDestroy(Object obj) throws Exception {
        LOGGER.info("Destroying zookeeper server");
        ((Destroyable) obj).destroy();
    }

    private String[] getExposedClasses(Object obj) {
        if (obj instanceof ServerStats.Provider) {
            return new String[] { ServerStats.Provider.class.getName() };
        } else if (obj instanceof QuorumStats.Provider) {
            return new String[] { QuorumStats.Provider.class.getName() };
        } else {
            throw new IllegalStateException("Unsupported service type: " + obj.getClass().toString());
        }
    }

    public void deleted(final String pid) {
        LOGGER.info("Configuration {} delete", pid);
        if (destroyed.get()) {
            return;
        }
        executor.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    internalDelete(pid);
                } catch (Throwable throwable) {
                    LOGGER.warn("Error destroying service for ManagedServiceFactory " + this, throwable);
                }
            }
        });
    }

    private Object doUpdate(Object t, Map<String, ?> configuration) throws Exception {
        doDestroy(t);
        return activateInternal(configuration);
    }

    private void internalUpdate(String pid, Map<String, ?> configuration) {
        Dictionary<String, ?> dictionary = toDictionary(configuration);
        Pair<Object, ServiceRegistration<?>> pair = services.get(pid);
        if (pair != null) {
            try {
                Object t = doUpdate(pair.getFirst(), configuration);
                pair.setFirst(t);
                pair.getSecond().setProperties(dictionary);
            } catch (Throwable throwable) {
                internalDelete(pid);
                LOGGER.warn("Error updating service for ManagedServiceFactory " + this, throwable);
            }
        } else {
            if (destroyed.get()) {
                return;
            }
            try {
                Object t = activateInternal(configuration);
                try {
                    if (destroyed.get()) {
                        throw new IllegalStateException("ManagedServiceFactory has been destroyed");
                    }
                    ServiceRegistration<?> registration = context.registerService(getExposedClasses(t), t, dictionary);
                    services.put(pid, new Pair<Object, ServiceRegistration<?>>(t, registration));
                } catch (Throwable throwable1) {
                    try {
                        doDestroy(t);
                    } catch (Throwable throwable2) {
                        // Ignore
                    }
                    throw throwable1;
                }
            } catch (Throwable throwable) {
                if (!destroyed.get()) {
                    LOGGER.warn("Error creating service for ManagedServiceFactory " + this, throwable);
                }
            }
        }
    }

    private void internalDelete(String pid) {
        Pair<Object, ServiceRegistration<?>> pair = services.remove(pid);
        if (pair != null) {
            try {
                pair.getSecond().unregister();
            } catch (Throwable t) {
                LOGGER.info("Error unregistering service", t);
            }
            try {
                doDestroy(pair.getFirst());
            } catch (Throwable t) {
                LOGGER.info("Error destroying service", t);
            }
        }
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

    static class Pair<U, V> {
        private U first;
        private V second;

        public Pair(U first, V second) {
            this.first = first;
            this.second = second;
        }

        public U getFirst() {
            return first;
        }

        public V getSecond() {
            return second;
        }

        public void setFirst(U first) {
            this.first = first;
        }

        public void setSecond(V second) {
            this.second = second;
        }
    }
}
