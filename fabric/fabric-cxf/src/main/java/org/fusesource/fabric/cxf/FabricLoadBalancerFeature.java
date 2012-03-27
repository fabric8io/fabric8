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
package org.fusesource.fabric.cxf;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.cxf.Bus;
import org.apache.cxf.buslifecycle.BusLifeCycleListener;
import org.apache.cxf.buslifecycle.BusLifeCycleManager;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.endpoint.ServerLifeCycleManager;
import org.apache.cxf.feature.AbstractFeature;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.data.ACL;
import org.fusesource.fabric.groups.Group;
import org.fusesource.fabric.groups.ZooKeeperGroupFactory;
import org.linkedin.util.clock.Timespan;
import org.linkedin.zookeeper.client.IZKClient;
import org.linkedin.zookeeper.client.ZKClient;

import java.util.List;


public class FabricLoadBalancerFeature extends AbstractFeature implements BusLifeCycleListener {
    private static final transient Log LOG = LogFactory.getLog(FabricLoadBalancerFeature.class);
    private volatile IZKClient zkClient;
    private String zkRoot = "/fabric/cxf/endpoints/";
    private String fabricPath;
    private boolean shouldCloseZkClient = false;
    private long maximumConnectionTimeout = 10 * 1000L;
    private long connectionRetryTime = 100L;
    private volatile Group group;
    private LoadBalanceStrategy loadBalanceStrategy;
    private List<ACL> accessControlList = ZooDefs.Ids.OPEN_ACL_UNSAFE;

    public void initialize(Client client, Bus bus) {
        LoadBalanceTargetSelector selector = getDefaultLoadBalanceTargetSelector();
        selector.setEndpoint(client.getEndpoint());
        try {
            selector.setLoadBalanceStrategy(getLoadBalanceStrategy());
            client.setConduitSelector(selector);
        } catch (Exception e) {
            LOG.error("Cannot setup the LoadBalanceStrategy due to " + e);
        }
        // setup the BusLifeCycleListener
        BusLifeCycleManager manager = bus.getExtension(BusLifeCycleManager.class);
        manager.registerLifeCycleListener(this);
    }

    public void initialize(Bus bus) {
        try {
            FabricServerListener lister = new FabricServerListener(getGroup());
            // register the listener itself
            ServerLifeCycleManager mgr = bus.getExtension(ServerLifeCycleManager.class);
            if (mgr != null) {
                mgr.registerListener(lister);
            } else {
                LOG.error("Cannot find the ServerLifeCycleManager, we cannot publish the service through fabric.");
            }
        } catch (Exception ex) {
            LOG.error("Cannot initialize the bus with FabricLoadBalancerFeature due to " + ex);
        }
        // setup the BusLifeCycleListener
        BusLifeCycleManager manager = bus.getExtension(BusLifeCycleManager.class);
        manager.registerLifeCycleListener(this);
    }

    protected LoadBalanceStrategy getDefaultLoadBalanceStrategy() {
        return new RandomLoadBalanceStrategy();
    }

    protected LoadBalanceTargetSelector getDefaultLoadBalanceTargetSelector() {
        return new LoadBalanceTargetSelector();
    }
    
    public synchronized Group getGroup() throws Exception {
         if (group == null) {
             group = ZooKeeperGroupFactory.create(getZkClient(), zkRoot + fabricPath, accessControlList);
         }
        return group;
    }

    public void destroy() throws Exception {
        if (zkClient != null && isShouldCloseZkClient()) {
            zkClient.close();
        }
    }

    public String getFabricPath() {
        return fabricPath;
    }

    public void setFabricPath(String fabricPath) {
        this.fabricPath = fabricPath;
    }

    public List<ACL> getAccessControlList() {
        return accessControlList;
    }

    public void setAccessControlList(List<ACL> accessControlList) {
        this.accessControlList = accessControlList;
    }

    public synchronized IZKClient getZkClient() throws Exception {
        if (zkClient == null) {
            String connectString = System.getProperty("zookeeper.url", "localhost:2181");
            ZKClient client = new ZKClient(connectString, Timespan.parse("10s"), null);
            LOG.debug("IZKClient not be found in registry, creating new with connection " + connectString);
            zkClient = client;
            setShouldCloseZkClient(true);
        }

        // ensure we are started
        if (zkClient instanceof ZKClient) {
            if (!zkClient.isConnected()) {
                LOG.debug("Staring IZKClient " + zkClient);
                ((ZKClient) zkClient).start();
            }
        }
        checkZkConnected();
        return zkClient;
    }

    public void setZkClient(IZKClient zkClient) {
        this.zkClient = zkClient;
    }

    public LoadBalanceStrategy getLoadBalanceStrategy() throws Exception {
        if (loadBalanceStrategy == null) {
            loadBalanceStrategy = getDefaultLoadBalanceStrategy();
        }
        if (loadBalanceStrategy.getGroup() == null) {
            loadBalanceStrategy.setGroup(getGroup());
        }
        return loadBalanceStrategy;
    }

    public void setLoadBalanceStrategy(LoadBalanceStrategy strategy) {
        this.loadBalanceStrategy = strategy;
    }
    
    public void setShouldCloseZkClient(boolean closeZkClient) {
         this.shouldCloseZkClient = closeZkClient;
    }

    public boolean isShouldCloseZkClient() {
        return shouldCloseZkClient;
    }

    public long getMaximumConnectionTimeout() {
        return maximumConnectionTimeout;
    }

    public void setMaximumConnectionTimeout(long maximumConnectionTimeout) {
        this.maximumConnectionTimeout = maximumConnectionTimeout;
    }

    public long getConnectionRetryTime() {
        return connectionRetryTime;
    }

    public void setConnectionRetryTime(long connectionRetryTime) {
        this.connectionRetryTime = connectionRetryTime;
    }

    /**
     * Lets check if we are connected and throw an exception if we are not.
     * Note that if start() has just been called on IZKClient then it will take a little
     * while for the connection to be established, so we keep checking up to the {@link #getMaximumConnectionTimeout()}
     * until we throw the exception
     */
    protected void checkZkConnected() throws Exception {
        long start = System.currentTimeMillis();
        do {
            if (zkClient.isConnected()) {
                return;
            }
            try {
                Thread.sleep(getConnectionRetryTime());
            } catch (InterruptedException e) {
                // ignore
            }
        } while (System.currentTimeMillis() < start + getMaximumConnectionTimeout());

        if (!zkClient.isConnected()) {
            throw new Exception("Could not connect to ZooKeeper " + zkClient + " at " + zkClient.getConnectString());
        }
    }

    @Override
    public void initComplete() {
        // Do nothing here
    }

    @Override
    public void preShutdown() {
        // Do nothing here
    }

    @Override
    public void postShutdown() {
        // just try to close the zkClient
        try {
            destroy();
        } catch (Exception e) {
            LOG.error("Cannot shut down the zkClient due to " + e);
        }
    }
}
