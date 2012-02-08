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

package org.elasticsearch.discovery.zookeeper;

import org.elasticsearch.cluster.ClusterName;
import org.elasticsearch.cluster.ClusterService;
import org.elasticsearch.common.collect.ImmutableList;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.discovery.zen.ZenDiscovery;
import org.elasticsearch.discovery.zen.ping.ZenPing;
import org.elasticsearch.discovery.zen.ping.ZenPingService;
import org.elasticsearch.discovery.zen.ping.unicast.UnicastZenPing;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.transport.TransportService;

public class ZooKeeperDiscovery extends ZenDiscovery {

    @Inject
    public ZooKeeperDiscovery(Settings settings, ClusterName clusterName, ThreadPool threadPool, TransportService transportService,
                              ClusterService clusterService, ZenPingService pingService) {
        super(settings, clusterName, threadPool, transportService, clusterService, pingService);
        ImmutableList<? extends ZenPing> zenPings = pingService.zenPings();
        UnicastZenPing unicastZenPing = null;
        for (ZenPing zenPing : zenPings) {
            if (zenPing instanceof UnicastZenPing) {
                unicastZenPing = (UnicastZenPing) zenPing;
                break;
            }
        }
        // update the unicast zen ping to add cloud hosts provider
        // and, while we are at it, use only it and not the multicast for example
        unicastZenPing.addHostsProvider(new ZooKeeperUnicastHostsProvider(settings));
        pingService.zenPings(ImmutableList.of(unicastZenPing));
    }
}
