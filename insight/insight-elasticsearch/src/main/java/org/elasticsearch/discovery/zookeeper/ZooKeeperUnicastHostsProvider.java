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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.elasticsearch.cluster.node.DiscoveryNode;
import org.elasticsearch.common.component.AbstractComponent;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.discovery.zen.ping.unicast.UnicastHostsProvider;
import org.linkedin.zookeeper.client.IZKClient;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.util.tracker.ServiceTracker;

public class ZooKeeperUnicastHostsProvider extends AbstractComponent implements UnicastHostsProvider {

    private BundleContext context;
    private ServiceTracker tracker;

    public ZooKeeperUnicastHostsProvider(Settings settings) {
        super(settings);
        context = FrameworkUtil.getBundle(getClass()).getBundleContext();
        tracker = new ServiceTracker(context, IZKClient.class.getName(), null);
        tracker.open();
    }

    public List<DiscoveryNode> buildDynamicNodes() {
        try {
            List<DiscoveryNode> dn = new ArrayList<DiscoveryNode>();
            IZKClient zooKeeper = (IZKClient) tracker.getService();
            if (zooKeeper != null && zooKeeper.isConnected()) {
                String path = componentSettings.get("node");
                List<String> nodes = zooKeeper.getChildren(path);
                for (String node : nodes) {
                    String data = zooKeeper.getStringData(path + "/" + node);
                    String[] datas = data.split(":");
                    InetSocketTransportAddress addr = new InetSocketTransportAddress(datas[0], Integer.parseInt(datas[1]));
                    dn.add(new DiscoveryNode(node, addr));
                }
            }
            return dn;
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }
}
