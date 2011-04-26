/**
 * Copyright (C) 2011, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.fabric.zookeeper.internal;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.zookeeper.server.NIOServerCnxn;
import org.apache.zookeeper.server.ServerConfig;
import org.apache.zookeeper.server.ServerStats;
import org.apache.zookeeper.server.ZKDatabase;
import org.apache.zookeeper.server.ZooKeeperServer;
import org.apache.zookeeper.server.persistence.FileTxnSnapLog;
import org.apache.zookeeper.server.quorum.QuorumPeer;
import org.apache.zookeeper.server.quorum.QuorumPeerConfig;
import org.apache.zookeeper.server.quorum.QuorumStats;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedServiceFactory;

public class ZKServerFactoryBean implements ManagedServiceFactory {


    private BundleContext bundleContext;
    private Map<String, Object> servers = new HashMap<String, Object>();
    private Map<String, ServiceRegistration> services = new HashMap<String, ServiceRegistration>();

    public BundleContext getBundleContext() {
        return bundleContext;
    }

    public void setBundleContext(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    public String getName() {
        return "ZooKeeper Server";
    }

    public synchronized void updated(String pid, Dictionary properties) throws ConfigurationException {
        try {
            deleted(pid);
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
                if (myId.exists()) {
                    myId.delete();
                }
                myId.getParentFile().mkdirs();
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
                NIOServerCnxn.Factory cnxnFactory =
                        new NIOServerCnxn.Factory(config.getClientPortAddress(),
                                config.getMaxClientCnxns());

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
                    quorumPeer.start();
                } catch (Exception e) {
                    quorumPeer.shutdown();
                    throw e;
                }

                servers.put(pid, quorumPeer);
                services.put(pid, bundleContext.registerService(QuorumStats.Provider.class.getName(), quorumPeer, properties));
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
                NIOServerCnxn.Factory cnxnFactory = new NIOServerCnxn.Factory(cfg.getClientPortAddress(),
                        cfg.getMaxClientCnxns());

                try {
                    cnxnFactory.startup(zkServer);
                } catch (Exception e) {
                    cnxnFactory.shutdown();
                    throw e;
                }

                servers.put(pid, cnxnFactory);
                services.put(pid, bundleContext.registerService(ServerStats.Provider.class.getName(), zkServer, properties));
            }
        } catch (Exception e) {
            throw (ConfigurationException) new ConfigurationException(null, "Unable to parse ZooKeeper configuration: " + e.getMessage()).initCause(e);
        }
    }

    public synchronized void deleted(String pid) {
        Object obj = servers.remove(pid);
        ServiceRegistration reg = services.remove(pid);
        if (reg != null) {
            reg.unregister();
        }
        if (obj instanceof QuorumPeer) {
            ((QuorumPeer) obj).shutdown();
        } else if (obj instanceof NIOServerCnxn.Factory) {
            ((NIOServerCnxn.Factory) obj).shutdown();
        }
    }

    public void destroy() throws Exception {
        while (!servers.isEmpty()) {
            String pid = servers.keySet().iterator().next();
            deleted(pid);
        }
    }

}
