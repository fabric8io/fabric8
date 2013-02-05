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
import org.fusesource.fabric.groups.Group;
import org.fusesource.fabric.groups.ZooKeeperGroupFactory;
import org.fusesource.fabric.zookeeper.IZKClient;
import org.fusesource.fabric.zookeeper.internal.ZKClient;
import org.linkedin.util.clock.Timespan;


public class FabricLoadBalancerFeature extends AbstractFeature implements BusLifeCycleListener {
    private static final transient Log LOG = LogFactory.getLog(FabricLoadBalancerFeature.class);
    private static final String ZOOKEEPER_URL = "zookeeper.url";
    private static final String ZOOKEEPER_PASSWORD = "zookeeper.password";

    private volatile IZKClient zkClient;
    private String zkRoot = "/fabric/cxf/endpoints/";
    private String fabricPath;
    private boolean shouldCloseZkClient = false;
    private long maximumConnectionTimeout = 10 * 1000L;
    private volatile Group group;
    private LoadBalanceStrategy loadBalanceStrategy;

    private ServerAddressResolver addressResolver;

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
            FabricServerListener lister = new FabricServerListener(getGroup(), addressResolver);
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
             group = ZooKeeperGroupFactory.create(getZkClient(), zkRoot + fabricPath);
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

    public synchronized IZKClient getZkClient() throws Exception {
        if (zkClient == null) {
            String connectString = System.getProperty(ZOOKEEPER_URL, "localhost:2181");
            String password = System.getProperty(ZOOKEEPER_PASSWORD);
            LOG.debug("IZKClient not find in camel registry, creating new with connection " + connectString);
            ZKClient client = new ZKClient(connectString, Timespan.parse("10s"), null);
            if (password != null && !password.isEmpty()) {
                client.setPassword(password);
            }
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
        zkClient.waitForConnected(new Timespan(getMaximumConnectionTimeout()));
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

    public ServerAddressResolver getAddressResolver() {
        return addressResolver;
    }

    public void setAddressResolver(ServerAddressResolver addressResolver) {
        this.addressResolver = addressResolver;
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
