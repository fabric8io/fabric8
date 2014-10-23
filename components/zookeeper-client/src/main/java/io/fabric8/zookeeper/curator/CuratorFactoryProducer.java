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
package io.fabric8.zookeeper.curator;

import io.fabric8.common.util.Objects;
import io.fabric8.common.util.Strings;
import io.fabric8.zookeeper.bootstrap.ZooKeeperServerFactory;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.api.ACLProvider;
import org.apache.curator.framework.state.ConnectionStateListener;
import org.apache.deltaspike.core.api.config.ConfigProperty;
import org.apache.zookeeper.server.quorum.QuorumPeerConfig;

import javax.enterprise.inject.Produces;
import javax.inject.Singleton;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 */
public class CuratorFactoryProducer {

    private ZooKeeperServerFactory serverFactory;

    @Produces
    @Singleton
    public CuratorFramework createCuratorFramework(CuratorConfig curatorConfig,
                                                   ACLProvider aclProvider,
                                                   QuorumPeerConfig peerConfig,
                                                   @ConfigProperty(name = "ZOOKEEPER_SERVER_ID", defaultValue = "singleRootZkNode") String serverId
                                                   ) throws IOException, InterruptedException {
        String zookeeperUrl = curatorConfig.getZookeeperUrl();
        System.out.println("ZK URL: " + zookeeperUrl);
        if (Strings.isNullOrBlank(zookeeperUrl)) {
            System.out.println("No ZooKeeper URL has been configured so creating a local ensemble server");
            serverFactory = new ZooKeeperServerFactory(peerConfig, "rootEnsembleNode");
            zookeeperUrl = serverFactory.getZooKeeperUrl();
            Objects.notNull(zookeeperUrl, "zookeeperUrl");
            curatorConfig.setZookeeperUrl(zookeeperUrl);
        }
        List<ConnectionStateListener> connectionListenerList = new ArrayList<>();
        CuratorFramework curatorFramework = ManagedCuratorFramework.createCuratorFramework(curatorConfig, aclProvider, connectionListenerList);
        curatorFramework.start();
        return curatorFramework;
    }
}
