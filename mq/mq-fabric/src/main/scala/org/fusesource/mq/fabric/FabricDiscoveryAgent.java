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
package org.fusesource.mq.fabric;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import io.fabric8.groups.internal.ManagedGroupFactory;
import io.fabric8.groups.internal.ManagedGroupFactoryBuilder;
import io.fabric8.groups.internal.OsgiManagedGroupFactory;
import org.apache.activemq.command.DiscoveryEvent;
import org.apache.activemq.transport.discovery.DiscoveryAgent;
import org.apache.activemq.transport.discovery.DiscoveryListener;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.RetryOneTime;
import org.codehaus.jackson.annotate.JsonProperty;
import io.fabric8.groups.GroupListener;
import io.fabric8.groups.Group;
import io.fabric8.groups.MultiGroup;
import io.fabric8.groups.NodeState;
import io.fabric8.groups.internal.ZooKeeperMultiGroup;
import io.fabric8.zookeeper.curator.CuratorACLManager;
import io.fabric8.zookeeper.utils.ZooKeeperUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FabricDiscoveryAgent implements DiscoveryAgent, Callable {
    
    private static final Logger LOG = LoggerFactory.getLogger(FabricDiscoveryAgent.class);

    protected CuratorFramework curator;
    private boolean managedZkClient;

    private String groupName = "default";

    private AtomicBoolean running=new AtomicBoolean();
    private final AtomicReference<DiscoveryListener> discoveryListener = new AtomicReference<DiscoveryListener>();

    private final HashMap<String, SimpleDiscoveryEvent> discoveredServices = new HashMap<String, SimpleDiscoveryEvent>();
    private final AtomicInteger startCounter = new AtomicInteger(0);

    private long initialReconnectDelay = 1000;
    private long maxReconnectDelay = 1000 * 30;
    private long backOffMultiplier = 2;
    private boolean useExponentialBackOff=true;    
    private int maxReconnectAttempts = 0;
    private final Object sleepMutex = new Object();
    private long minConnectTime = 5000;
    private String id;
    private String agent;

    MultiGroup<ActiveMQNode> group;
    ManagedGroupFactory factory;

    List<String> services = new ArrayList<String>();

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public static class ActiveMQNode extends NodeState {

        public ActiveMQNode() {
            super();
        }

        public ActiveMQNode(String id, String container) {
            super(id, container);
        }

        @JsonProperty
        String[] services;
    }
    
    ActiveMQNode createState() {
        ActiveMQNode state = new ActiveMQNode(id, agent);
        state.id = id;
        state.services = services.toArray(new String[services.size()]);
        return state;
    }

    class SimpleDiscoveryEvent extends DiscoveryEvent {

        private int connectFailures;
        private long reconnectDelay = initialReconnectDelay;
        private long connectTime = System.currentTimeMillis();
        private AtomicBoolean failed = new AtomicBoolean(false);
        private AtomicBoolean removed = new AtomicBoolean(false);

        public SimpleDiscoveryEvent(String service) {
            super(service);
        }

    }

    synchronized public void registerService(String service) throws IOException {
        services.add(service);
        updateClusterState();
    }

    public void updateClusterState() {
        if (startCounter.get() > 0 ) {
            if( id==null )
                throw new IllegalStateException("You must configure the id of the fabric discovery if you want to register services");
            group.update(createState());
        }
    }

    public void serviceFailed(DiscoveryEvent devent) throws IOException {

        final SimpleDiscoveryEvent event = (SimpleDiscoveryEvent)devent;
        if (event.failed.compareAndSet(false, true)) {
        	discoveryListener.get().onServiceRemove(event);
        	if(!event.removed.get()) {
	        	// Setup a thread to re-raise the event...
	            Thread thread = new Thread() {
	                public void run() {
	
	                    // We detect a failed connection attempt because the service
	                    // fails right away.
	                    if (event.connectTime + minConnectTime > System.currentTimeMillis()) {
	                        LOG.debug("Failure occurred soon after the discovery event was generated.  It will be classified as a connection failure: "+event);
	
	                        event.connectFailures++;
	
	                        if (maxReconnectAttempts > 0 && event.connectFailures >= maxReconnectAttempts) {
	                            LOG.debug("Reconnect attempts exceeded "+maxReconnectAttempts+" tries.  Reconnecting has been disabled.");
	                            return;
	                        }
	
	                        synchronized (sleepMutex) {
	                            try {
	                                if (!running.get() || event.removed.get()) {
	                                    return;
	                                }
	                                LOG.debug("Waiting "+event.reconnectDelay+" ms before attempting to reconnect.");
	                                sleepMutex.wait(event.reconnectDelay);
	                            } catch (InterruptedException ie) {
	                                Thread.currentThread().interrupt();
	                                return;
	                            }
	                        }
	
	                        if (!useExponentialBackOff) {
	                            event.reconnectDelay = initialReconnectDelay;
	                        } else {
	                            // Exponential increment of reconnect delay.
	                            event.reconnectDelay *= backOffMultiplier;
	                            if (event.reconnectDelay > maxReconnectDelay) {
	                                event.reconnectDelay = maxReconnectDelay;
	                            }
	                        }
	
	                    } else {
	                        event.connectFailures = 0;
	                        event.reconnectDelay = initialReconnectDelay;
	                    }
	
	                    if (!running.get() || event.removed.get()) {
	                        return;
	                    }
	
	                    event.connectTime = System.currentTimeMillis();
	                    event.failed.set(false);
	                    discoveryListener.get().onServiceAdd(event);
	                }
	            };
	            thread.setDaemon(true);
	            thread.start();
        	}
        }
    }

    public void setDiscoveryListener(DiscoveryListener discoveryListener) {
        this.discoveryListener.set(discoveryListener);
    }

    synchronized public void start() throws Exception {
        if( startCounter.addAndGet(1)==1 ) {
            running.set(true);

            if (curator != null) {
                managedZkClient = false;
            }

            getGroup().add(new GroupListener<ActiveMQNode>() {
                @Override
                public void groupEvent(Group<ActiveMQNode> group, GroupEvent event) {
                    Map<String, ActiveMQNode> masters = new HashMap<String, ActiveMQNode>();
                    for (ActiveMQNode node : group.members().values()) {
                        if (!masters.containsKey(node.id)) {
                            masters.put(node.id, node);
                        }
                    }
                    update(masters.values());
                }
            });
            if( id!=null ) {
                group.update(createState());
            }
            group.start();
        }
    }

    synchronized public void stop() throws Exception {
        if( startCounter.decrementAndGet()==0 ) {
            running.set(false);
            try {
                if (group != null) {
                    group.close();
                }
            } catch (Throwable ignore) {
                // Most likely a ServiceUnavailableException: The Blueprint container is being or has been destroyed
            }
            if( managedZkClient ) {
                try {
                    curator.close();
                } catch (Throwable ignore) {
                    // Most likely a ServiceUnavailableException: The Blueprint container is being or has been destroyed
                }
                curator = null;
            }
        }
    }

    private void update(Collection<ActiveMQNode> members) {

        // Find new registered services...
        DiscoveryListener discoveryListener = this.discoveryListener.get();
        if(discoveryListener!=null) {
            HashSet<String> activeServices = new HashSet<String>();
            for(ActiveMQNode m : members) {
                for(String service: m.services) {

                    String resolved = service;
                    try {
                        resolved = ZooKeeperUtils.getSubstitutedData(curator, service);
                    } catch (Exception e) {
                        // ignore, we'll use unresolved value
                    }
                    activeServices.add(resolved);
                }
            }
            // If there is error talking the the central server, then activeServices == null
            if( members !=null ) {
                synchronized(discoveredServices) {
                    
                    HashSet<String> removedServices = new HashSet<String>(discoveredServices.keySet());
                    removedServices.removeAll(activeServices);
                    
                    HashSet<String> addedServices = new HashSet<String>(activeServices);
                    addedServices.removeAll(discoveredServices.keySet());
                    addedServices.removeAll(removedServices);
                    
                    for (String service : addedServices) {
                        SimpleDiscoveryEvent e = new SimpleDiscoveryEvent(service);
                        discoveredServices.put(service, e);
                        discoveryListener.onServiceAdd(e);
                    }
                    
                    for (String service : removedServices) {
                    	SimpleDiscoveryEvent e = discoveredServices.remove(service);
                    	if( e !=null ) {
                    		e.removed.set(true);
                    	}
                        discoveryListener.onServiceRemove(e);
                    }
                }
            }
        }
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public List<String> getServices() {
        return services;
    }

    public void setServices(String[] services) {
        this.services.clear();
        for(String s:services) {
            this.services.add(s);
        }
        updateClusterState();
    }

    public MultiGroup<ActiveMQNode> getGroup() throws Exception {
        if (group == null) {
            factory = ManagedGroupFactoryBuilder.create(curator, getClass().getClassLoader(), this);
            group = (MultiGroup)factory.createMultiGroup("/fabric/registry/clusters/fusemq/" + groupName, ActiveMQNode.class);
            curator = factory.getCurator();
        }

        return group;
    }

    @Override
    public Object call() throws Exception {
        LOG.info("Using local ZKClient");
        managedZkClient = true;
        CuratorFrameworkFactory.Builder builder = CuratorFrameworkFactory.builder()
                .connectString(System.getProperty("zookeeper.url", "localhost:2181"))
                .retryPolicy(new RetryOneTime(1000))
                .connectionTimeoutMs(10000);

        String password = System.getProperty("zookeeper.password", "admin");
        if (password != null && !password.isEmpty()) {
            builder.aclProvider(new CuratorACLManager());
            builder.authorization("digest", ("fabric:"+password).getBytes());
        }

        CuratorFramework client = builder.build();
        client.start();
        client.getZookeeperClient().blockUntilConnectedOrTimedOut();
        return client;
    }

    public CuratorFramework getCurator() {
        return curator;
    }

    public void setCurator(CuratorFramework curator) {
        this.curator = curator;
    }

    public String getAgent() {
        return agent;
    }

    public void setAgent(String agent) {
        this.agent = agent;
    }
}
