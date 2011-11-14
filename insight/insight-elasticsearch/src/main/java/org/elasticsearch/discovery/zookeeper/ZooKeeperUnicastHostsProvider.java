/**
 * Copyright (C) 2010, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
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
