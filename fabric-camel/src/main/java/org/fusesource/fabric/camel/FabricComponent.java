/*
 * Copyright (C) 2011, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.fabric.camel;

import java.util.List;
import java.util.Map;

import org.apache.camel.Endpoint;
import org.apache.camel.impl.DefaultComponent;
import org.apache.camel.impl.ProducerCache;
import org.apache.camel.util.ObjectHelper;
import org.apache.camel.util.ServiceHelper;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.data.ACL;
import org.fusesource.fabric.groups.Group;
import org.fusesource.fabric.groups.ZooKeeperGroupFactory;
import org.linkedin.util.clock.Timespan;
import org.linkedin.zookeeper.client.IZKClient;
import org.linkedin.zookeeper.client.ZKClient;
import org.springframework.beans.factory.annotation.Autowired;

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
            zkClient = (IZKClient) getCamelContext().getRegistry().lookup(IZKClient.class.getName());
            LOG.debug("IZKClient find in camel registry.");
        }
        if (zkClient == null) {
            ZKClient client = new ZKClient(System.getProperty("zookeeper.url", "localhost:2181"), Timespan.parse("10s"), null);
            LOG.debug("IZKClient not find in camel registry and created.");
            client.start();
            zkClient = client;
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
            String fabricPath = getFabricPath(name);
            String childUri = remaining.substring(idx + 1);

            Group group = ZooKeeperGroupFactory.create(getZkClient(), fabricPath, accessControlList);
            return new FabricPublisherEndpoint(uri, this, group, childUri);

        } else {
            String fabricPath = getFabricPath(remaining);
            Group group = ZooKeeperGroupFactory.create(getZkClient(), fabricPath, accessControlList);
            return new FabricLocatorEndpoint(uri, this, group);
        }
    }

    protected String getFabricPath(String name) {
        String path = name;
        if (ObjectHelper.isNotEmpty(zkRoot)) {
            path = zkRoot + "/" + name;
        }
        return path;
    }

}
