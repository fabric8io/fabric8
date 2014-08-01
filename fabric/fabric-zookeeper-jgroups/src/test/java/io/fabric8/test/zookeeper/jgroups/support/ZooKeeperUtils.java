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
package io.fabric8.test.zookeeper.jgroups.support;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.apache.zookeeper.server.NIOServerCnxnFactory;
import org.apache.zookeeper.server.ServerConfig;
import org.apache.zookeeper.server.ZooKeeperServer;
import org.apache.zookeeper.server.persistence.FileTxnSnapLog;
import org.apache.zookeeper.server.quorum.QuorumPeerConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ZooKeeperUtils {
    private static Logger LOGGER = LoggerFactory.getLogger(ZooKeeperUtils.class);

    private static SimpleServer simpleServer = new SimpleServer();

    public static void startServer() throws Exception {
        Properties properties;
        try (InputStream stream = ZooKeeperUtils.class.getClassLoader().getResourceAsStream("zoo.cfg")) {
            properties = new Properties();
            properties.load(stream);
        }
        // set temp data dir
        String tempDir = File.createTempFile("dummy-", ".f8").getParent();
        properties.put("dataDir", tempDir);

        startServer(properties);
    }

    public static void startServer(Properties properties) throws Exception {
        simpleServer.start(getPeerConfig(properties));
    }

    public static void stopServer() throws Exception {
        simpleServer.stop();
    }

    private static QuorumPeerConfig getPeerConfig(Properties props) throws IOException, QuorumPeerConfig.ConfigException {
        QuorumPeerConfig peerConfig = new QuorumPeerConfig();
        peerConfig.parseProperties(props);
        LOGGER.info("Created zookeeper peer configuration: {}", peerConfig);
        return peerConfig;
    }

    private static ServerConfig getServerConfig(QuorumPeerConfig peerConfig) {
        ServerConfig serverConfig = new ServerConfig();
        serverConfig.readFrom(peerConfig);
        LOGGER.info("Created zookeeper server configuration: {}", serverConfig);
        return serverConfig;
    }

    private static class SimpleServer {
        private ZooKeeperServer zkServer;
        private NIOServerCnxnFactory cnxnFactory;

        void start(QuorumPeerConfig peerConfig) throws Exception {
            ServerConfig serverConfig = getServerConfig(peerConfig);

            zkServer = new ZooKeeperServer();
            zkServer.setTxnLogFactory(new FileTxnSnapLog(new File(serverConfig.getDataLogDir()), new File(serverConfig.getDataDir())));
            zkServer.setTickTime(serverConfig.getTickTime());
            zkServer.setMinSessionTimeout(serverConfig.getMinSessionTimeout());
            zkServer.setMaxSessionTimeout(serverConfig.getMaxSessionTimeout());

            cnxnFactory = new NIOServerCnxnFactory() {
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
        }

        void stop() throws Exception {
            if (cnxnFactory != null) {
                cnxnFactory.shutdown();
                cnxnFactory.join();
            }
            if (zkServer != null) {
                if (zkServer.getZKDatabase() != null) {
                    zkServer.getZKDatabase().close();
                }
            }
            cnxnFactory = null;
            zkServer = null;
        }
    }
}
