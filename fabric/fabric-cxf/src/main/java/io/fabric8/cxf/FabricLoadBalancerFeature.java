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
package io.fabric8.cxf;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.RetryOneTime;
import org.apache.cxf.Bus;
import org.apache.cxf.buslifecycle.BusLifeCycleListener;
import org.apache.cxf.buslifecycle.BusLifeCycleManager;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.endpoint.ClientLifeCycleManager;
import org.apache.cxf.endpoint.ConduitSelector;
import org.apache.cxf.endpoint.ConduitSelectorHolder;
import org.apache.cxf.endpoint.ServerLifeCycleManager;
import org.apache.cxf.feature.AbstractFeature;
import org.apache.cxf.interceptor.InterceptorProvider;
import io.fabric8.groups.Group;
import io.fabric8.groups.internal.ZooKeeperGroup;


public class FabricLoadBalancerFeature extends AbstractFeature implements BusLifeCycleListener {
    private static final transient Log LOG = LogFactory.getLog(FabricLoadBalancerFeature.class);
    private static final String ZOOKEEPER_URL = "zookeeper.url";
    private static final String ZOOKEEPER_PASSWORD = "zookeeper.password";

    private volatile CuratorFramework curator;

    private String zooKeeperUrl;
    private String zooKeeperPassword;
    private String zkRoot = "/fabric/cxf/endpoints/";
    private String fabricPath;
    private boolean shouldCloseZkClient = false;
    // Default ZooKeeper connection timeout
    private int maximumConnectionTimeout = 10 * 1000;
    private volatile Group group;
    private LoadBalanceStrategy loadBalanceStrategy;

    private ServerAddressResolver addressResolver;

    public void initialize(Client client, Bus bus) {
        setupClientConduitSelector(client);
        // setup the BusLifeCycleListener
        BusLifeCycleManager manager = bus.getExtension(BusLifeCycleManager.class);
        manager.registerLifeCycleListener(this);
    }

    // this method will be used for JAXRS client
    public void initialize(InterceptorProvider interceptorProvider, Bus bus) {
        // try to find if the InterceptorProvider is a ConduitSelectorHolder
        if (interceptorProvider instanceof ConduitSelectorHolder) {
            ConduitSelectorHolder holder = (ConduitSelectorHolder) interceptorProvider;
            // get the endpoint of the original ConduitSelector
            ConduitSelector oldSelector = holder.getConduitSelector();
            LoadBalanceTargetSelector selector = getDefaultLoadBalanceTargetSelector();
            selector.setEndpoint(oldSelector.getEndpoint());
            try {
                selector.setLoadBalanceStrategy(getLoadBalanceStrategy());
                holder.setConduitSelector(selector);
            } catch (Exception e) {
                LOG.error("Cannot setup the LoadBalanceStrategy due to " + e);
            }
            // setup the BusLifeCycleListener
            BusLifeCycleManager manager = bus.getExtension(BusLifeCycleManager.class);
            manager.registerLifeCycleListener(this);
        }
    }

    public void initialize(Bus bus) {
        try {
            FabricServerListener lister = new FabricServerListener(getGroup(), addressResolver);
            // register the server listener itself
            ServerLifeCycleManager serverMgr = bus.getExtension(ServerLifeCycleManager.class);
            if (serverMgr != null) {
                serverMgr.registerListener(lister);
            } else {
                LOG.error("Cannot find the ServerLifeCycleManager, we cannot publish the service through fabric.");
            }
            // register the client listener
            ClientLifeCycleManager clientMgr = bus.getExtension(ClientLifeCycleManager.class);
            FabricClientListener clientListener = new FabricClientListener(this);
            if (clientMgr != null) {
                clientMgr.registerListener(clientListener);
            } else {
                LOG.error("Cannot find the ClientLifeCycleManager, the client cannot access the service through fabric");
            }

        } catch (Exception ex) {
            LOG.error("Cannot initialize the bus with FabricLoadBalancerFeature due to " + ex);
        }
        // setup the BusLifeCycleListener
        BusLifeCycleManager manager = bus.getExtension(BusLifeCycleManager.class);
        manager.registerLifeCycleListener(this);
    }

    protected void setupClientConduitSelector(Client client) {
        //TODO do we need to check if the ConduitSelector is replaced
        LoadBalanceTargetSelector selector = getDefaultLoadBalanceTargetSelector();
        selector.setEndpoint(client.getEndpoint());
        try {
            selector.setLoadBalanceStrategy(getLoadBalanceStrategy());
            client.setConduitSelector(selector);
        } catch (Exception e) {
            LOG.error("Cannot setup the LoadBalanceStrategy due to " + e);
        }
    }

    protected LoadBalanceStrategy getDefaultLoadBalanceStrategy() {
        return new RandomLoadBalanceStrategy();
    }

    protected LoadBalanceTargetSelector getDefaultLoadBalanceTargetSelector() {
        return new LoadBalanceTargetSelector();
    }
    
    public synchronized Group getGroup() throws Exception {
         if (group == null) {
             group = new ZooKeeperGroup<CxfNodeState>(getCurator(), zkRoot + fabricPath, CxfNodeState.class);
             group.start();
         }
        return group;
    }

    public void destroy() throws Exception {
        if (group != null) {
            group.close();
        }
        if (curator != null && isShouldCloseZkClient()) {
            curator.close();
        }
    }

    public String getFabricPath() {
        return fabricPath;
    }

    public void setFabricPath(String fabricPath) {
        this.fabricPath = fabricPath;
    }

    public synchronized CuratorFramework getCurator() throws Exception {
        if (curator == null) {
            String connectString = getZooKeeperUrl();
            if (connectString == null) {
                connectString = System.getProperty(ZOOKEEPER_URL, "localhost:2181");
            }
            String password = getZooKeeperPassword();
            if (password == null) {
                System.getProperty(ZOOKEEPER_PASSWORD);
            }
            LOG.debug("Zookeeper client not find in camel registry, creating new with connection " + connectString);
            CuratorFrameworkFactory.Builder builder = CuratorFrameworkFactory.builder()
                    .connectString(connectString)
                    .retryPolicy(new RetryOneTime(1000))
                    .connectionTimeoutMs(maximumConnectionTimeout);

            if (password != null && !password.isEmpty()) {
                builder.authorization("digest", ("fabric:" + password).getBytes());
            }

            CuratorFramework client = builder.build();
            LOG.debug("Starting curator " + curator);
            client.start();
            curator = client;
            setShouldCloseZkClient(true);
        }

        curator.getZookeeperClient().blockUntilConnectedOrTimedOut();
        return curator;
    }

    public void setCurator(CuratorFramework curator) {
        this.curator = curator;
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

    public int getMaximumConnectionTimeout() {
        return maximumConnectionTimeout;
    }

    public void setMaximumConnectionTimeout(int maximumConnectionTimeout) {
        this.maximumConnectionTimeout = maximumConnectionTimeout;
    }

    public ServerAddressResolver getAddressResolver() {
        return addressResolver;
    }

    public void setAddressResolver(ServerAddressResolver addressResolver) {
        this.addressResolver = addressResolver;
    }

    public String getZooKeeperUrl() {
        return zooKeeperUrl;
    }

    public void setZooKeeperUrl(String zooKeeperUrl) {
        this.zooKeeperUrl = zooKeeperUrl;
    }

    public String getZooKeeperPassword() {
        return zooKeeperPassword;
    }

    public void setZooKeeperPassword(String zooKeeperPassword) {
        this.zooKeeperPassword = zooKeeperPassword;
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
        // just try to close the curator
        try {
            destroy();
        } catch (Exception e) {
            LOG.error("Cannot shut down the curator due to " + e);
        }
    }
}
