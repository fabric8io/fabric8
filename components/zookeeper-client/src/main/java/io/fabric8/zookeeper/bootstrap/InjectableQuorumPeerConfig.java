/**
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.fabric8.zookeeper.bootstrap;

import org.apache.deltaspike.core.api.config.ConfigProperty;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.server.ZooKeeperServer;
import org.apache.zookeeper.server.quorum.QuorumPeer;
import org.apache.zookeeper.server.quorum.QuorumPeerConfig;
import org.apache.zookeeper.server.quorum.flexible.QuorumVerifier;

import javax.inject.Inject;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.HashMap;

/**
 * Allows easy injection with CDI for a {@link QuorumPeerConfig}
 */
public class InjectableQuorumPeerConfig extends QuorumPeerConfig {
    @Inject
    public InjectableQuorumPeerConfig() {
    }


    @ConfigProperty(name = "ZOOKEEPER_CLIENT_PORT")
    public void setClientPort(int clientPort) {
        this.clientPortAddress = new InetSocketAddress(clientPort);
    }

    @ConfigProperty(name = "ZOOKEEPER_DATADIR")
    public void setDataDir(String dataDir) {
        this.dataDir = dataDir;
    }

    @ConfigProperty(name = "ZOOKEEPER_DATA_LOG_DIR")
    public void setDataLogDir(String dataLogDir) {
        this.dataLogDir = dataLogDir;
    }

    @ConfigProperty(name = "ZOOKEEPER_TICKTIME", defaultValue = "" + ZooKeeperServer.DEFAULT_TICK_TIME)
    public void setTickTime(int tickTime) {
        this.tickTime = tickTime;
    }

    @ConfigProperty(name = "ZOOKEEPER_INIT_LIMIT")
    public void setInitLimit(int initLimit) {
        this.initLimit = initLimit;
    }

    @ConfigProperty(name = "ZOOKEEPER_SYNC_LIMIT")
    public void setSyncLimit(int syncLimit) {
        this.syncLimit = syncLimit;
    }



    public void setClientPortAddress(InetSocketAddress clientPortAddress) {
        this.clientPortAddress = clientPortAddress;
    }

    public void setMaxClientCnxns(int maxClientCnxns) {
        this.maxClientCnxns = maxClientCnxns;
    }

    public void setMinSessionTimeout(int minSessionTimeout) {
        this.minSessionTimeout = minSessionTimeout;
    }

    public void setMaxSessionTimeout(int maxSessionTimeout) {
        this.maxSessionTimeout = maxSessionTimeout;
    }

    public void setElectionAlg(int electionAlg) {
        this.electionAlg = electionAlg;
    }

    public void setElectionPort(int electionPort) {
        this.electionPort = electionPort;
    }

    public void setQuorumListenOnAllIPs(boolean quorumListenOnAllIPs) {
        this.quorumListenOnAllIPs = quorumListenOnAllIPs;
    }

    public void setServerId(long serverId) {
        this.serverId = serverId;
    }

    public void setServerWeight(HashMap<Long, Long> serverWeight) {
        this.serverWeight = serverWeight;
    }

    public void setServerGroup(HashMap<Long, Long> serverGroup) {
        this.serverGroup = serverGroup;
    }

    public void setNumGroups(int numGroups) {
        this.numGroups = numGroups;
    }

    public void setQuorumVerifier(QuorumVerifier quorumVerifier) {
        this.quorumVerifier = quorumVerifier;
    }

    public void setSnapRetainCount(int snapRetainCount) {
        this.snapRetainCount = snapRetainCount;
    }

    public void setPurgeInterval(int purgeInterval) {
        this.purgeInterval = purgeInterval;
    }

    public void setSyncEnabled(boolean syncEnabled) {
        this.syncEnabled = syncEnabled;
    }

    public void setPeerType(QuorumPeer.LearnerType peerType) {
        this.peerType = peerType;
    }
}
