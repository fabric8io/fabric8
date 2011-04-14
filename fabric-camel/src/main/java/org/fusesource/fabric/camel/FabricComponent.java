/*
 * Copyright (C) 2011, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.fabric.camel;

import org.apache.camel.Endpoint;
import org.apache.camel.impl.DefaultComponent;
import org.apache.camel.impl.ProducerCache;
import org.apache.camel.util.ObjectHelper;
import org.apache.camel.util.ServiceHelper;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.data.ACL;
import org.apache.zookeeper.data.Stat;
import org.fusesource.fabric.zookeeper.ZKClientFactoryBean;
import org.linkedin.zookeeper.client.IZKClient;
import org.linkedin.zookeeper.tracker.NodeEvent;
import org.linkedin.zookeeper.tracker.NodeEventType;
import org.linkedin.zookeeper.tracker.NodeEventsListener;
import org.linkedin.zookeeper.tracker.ZKDataReader;
import org.linkedin.zookeeper.tracker.ZooKeeperTreeTracker;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * The FABRIC camel component for providing endpoint discovery, clustering and load balancing.
 */
public class FabricComponent extends DefaultComponent {
    private static final transient Log LOG = LogFactory.getLog(FabricComponent.class);

    @Autowired
    private IZKClient zkClient;
    private String zkRoot = "/fabric/camel/endpoints";
    private List<ACL> accessControlList = ZooDefs.Ids.OPEN_ACL_UNSAFE;
    private LoadBalancerFactory loadBalancerFactory = new DefaultLoadBalancerFactory();
    private ProducerCache producerCache;
    private int cacheSize = 1000;
    protected ZKDataReader<EndpointLocator> reader = new EndpointLocatorReader();


    public IZKClient getZkClient() {
        return zkClient;
    }

    public void setZkClient(IZKClient zkClient) {
        this.zkClient = zkClient;
    }

    public String getZkRoot() {
        return zkRoot;
    }

    public void setZkRoot(String zkRoot) {
        this.zkRoot = zkRoot;
    }

    public List<ACL> getAccessControlList() {
        return accessControlList;
    }

    public void setAccessControlList(List<ACL> accessControlList) {
        this.accessControlList = accessControlList;
    }

    public int getCacheSize() {
        return cacheSize;
    }

    public void setCacheSize(int cacheSize) {
        this.cacheSize = cacheSize;
    }

    public ProducerCache getProducerCache() {
        return producerCache;
    }

    public void setProducerCache(ProducerCache producerCache) {
        this.producerCache = producerCache;
    }

    public LoadBalancerFactory getLoadBalancerFactory() {
        return loadBalancerFactory;
    }

    public void setLoadBalancerFactory(LoadBalancerFactory loadBalancerFactory) {
        this.loadBalancerFactory = loadBalancerFactory;
    }

    //  Implementation methods
    //-------------------------------------------------------------------------


    @Override
    protected void doStart() throws Exception {
        super.doStart();
        if (zkClient == null) {
            zkClient = new ZKClientFactoryBean().getObject();
        }
        checkZkConnected();
        if (producerCache == null) {
            producerCache = new ProducerCache(this, getCamelContext(), cacheSize);
        }
        ServiceHelper.startService(producerCache);
    }

    protected void checkZkConnected() throws Exception {
        if (!zkClient.isConnected()) {
            throw new Exception("Could not connect to ZooKeeper " + zkClient);
        }
    }

    @Override
    protected void doStop() throws Exception {
        super.doStop();
        if (zkClient != null) {
            zkClient.close();
        }
        ServiceHelper.stopService(producerCache);
    }

    @Override
    protected Endpoint createEndpoint(String uri, String remaining, Map<String, Object> params) throws Exception {
        int idx = remaining.indexOf(':');
        if (idx > 0) {
            // we are registering a regular endpoint
            String name = remaining.substring(0, idx);
            String path = getFabricPath(name);

            String childUri = remaining.substring(idx + 1);
            Endpoint childEndpoint = getCamelContext().getEndpoint(childUri);
            registerEndpoint(path, childUri, childEndpoint);
            return childEndpoint;
        } else {
            String fabricPath = getFabricPath(remaining);
            ZooKeeperTreeTracker<EndpointLocator> treeTracker = createTreeTracker(fabricPath);
            treeTracker.registerListener(new NodeEventsListener<EndpointLocator>() {
                public void onEvents(Collection<NodeEvent<EndpointLocator>> nodeEvents) {
                    for (NodeEvent<EndpointLocator> event : nodeEvents) {
                        NodeEventType eventType = event.getEventType();
                        if (eventType == NodeEventType.DELETED) {
                            // lets close the locator
                            try {
                                event.getData().stop();
                            } catch (Exception e) {
                                LOG.error("Failed to stop EndpointLocator: " + e, e);
                            }
                        }
                    }

                }
            });
            treeTracker.track();

            return new FabricEndpoint(uri, this, fabricPath, remaining, treeTracker);
        }
    }

    protected String getFabricPath(String name) {
        String path = name;
        if (ObjectHelper.isNotEmpty(zkRoot)) {
            path = zkRoot + "/" + name;
        }
        return path;
    }

    protected void registerEndpoint(String path, String uri, Endpoint endpoint) throws Exception {
        checkZkConnected();
        LOG.debug("Registering ZooKeeper entry at " + path + " for endpoint: " + uri);
        Stat stats = zkClient.exists(path);
        if (stats == null) {
            zkClient.createWithParents(path, "", accessControlList, CreateMode.PERSISTENT);
        }
        zkClient.create(path + "/e", uri, accessControlList, CreateMode.EPHEMERAL_SEQUENTIAL);
    }

    public ZooKeeperTreeTracker<EndpointLocator> createTreeTracker(String path) {
        IZKClient client = getZkClient();
        ObjectHelper.notNull(client, "zkClient");
        return new ZooKeeperTreeTracker<EndpointLocator>(client, reader, path);
    }

}
