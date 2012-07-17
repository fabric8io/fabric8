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
package org.fusesource.fabric.camel;

import org.apache.camel.*;
import org.apache.camel.builder.ErrorHandlerBuilder;
import org.apache.camel.spi.LifecycleStrategy;
import org.apache.camel.spi.RouteContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.data.ACL;
import org.codehaus.jackson.annotate.JsonProperty;
import org.fusesource.fabric.groups.*;
import org.linkedin.util.clock.Timespan;
import org.linkedin.zookeeper.client.IZKClient;
import org.linkedin.zookeeper.client.ZKClient;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 *
 */
public class ClusteredSingletonLifecycleStrategy implements LifecycleStrategy {

    // TODO: Work in progress

    public static Log LOG = LogFactory.getLog(ClusteredSingletonLifecycleStrategy.class);

    Group group;
    String groupName;
    String id;

    volatile CamelContext camelContext;
    final AtomicBoolean started = new AtomicBoolean();

    IZKClient zkClient;
    boolean managedZkClient;
    ClusteredSingleton<CamelNode> singleton = new ClusteredSingleton<CamelNode>(CamelNode.class);
    public List<ACL> acl = ZooDefs.Ids.OPEN_ACL_UNSAFE;

    static void info(String msg, Object... args) {
        if(LOG.isInfoEnabled()) {
            LOG.info(String.format(msg, args));
        }
    }

    static class CamelNode implements NodeState {
        @JsonProperty
        String id;
        // We could advertise information about our camel context.
        //        @JsonProperty
        //        String services[];
        @JsonProperty
        String container;
        @JsonProperty
        Boolean started;

        public String id() {
            return id;
        }
    }

    CamelNode createState() {
        CamelNode state = new CamelNode();
        state.id = id;
        state.container = System.getProperty("karaf.name");
        state.started = started.get();
//        state.services = services.toArray(new String[services.size()]);
        return state;
    }


    public void start() throws Exception {

        if (zkClient == null) {
            managedZkClient = true;
            ZKClient client = new ZKClient(System.getProperty("zookeeper.url", "localhost:2181"), Timespan.parse("10s"), null);
            client.start();
            client.waitForStart();
            zkClient = client;
        } else {
            managedZkClient = false;
        }

        group = ZooKeeperGroupFactory.create(zkClient, "/fabric/camel-clusters/" + groupName, acl);
        singleton.start(group);
        singleton.join(createState());

        info("Camel context %s is waiting to become the master", id);

        singleton.add(new ChangeListener() {
            @Override
            public void changed() {
                if (singleton.isMaster()) {
                    if (started.compareAndSet(false, true)) {
                        info("Camel context %s is now the master, starting the context.", id);
                        try {
                            camelContext.start();
                            // Update the state of the master since he is now running.
                            singleton.update(createState());
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                } else {
                    if (started.compareAndSet(true, false)) {
                        info("Camel context %s is now a slave, stopping the context.", id);
                        try {
                            camelContext.stop();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }

            public void connected() {
                this.changed();
            }

            public void disconnected() {
                this.changed();
            }
        });
    }

    public void stop() {
        try {
            group.close();
        } catch (Throwable ignore) {
            // Most likely a ServiceUnavailableException: The Blueprint container is being or has been destroyed
        }
        if (managedZkClient) {
            try {
                zkClient.close();
            } catch (Throwable ignore) {
                // Most likely a ServiceUnavailableException: The Blueprint container is being or has been destroyed
            }
            zkClient = null;
        }
    }


    ///////////////////////////////////////////////////////////////////
    // LifecycleStrategy interface impl.
    ///////////////////////////////////////////////////////////////////

    public void onContextStart(final CamelContext camelContext) throws VetoCamelContextStartException {
        this.camelContext = camelContext;
        if (!started.get()) {
            throw new VetoCamelContextStartException("Do not start him! and don't rethrow", camelContext);
//            throw new VetoCamelContextStartException("Do not start him! and dont rethrow", camelContext, false);
        }
    }

    public void onContextStop(CamelContext camelContext) {
    }
    public void onComponentAdd(String s, Component component) {
    }
    public void onComponentRemove(String s, Component component) {
    }
    public void onEndpointAdd(Endpoint endpoint) {
    }
    public void onEndpointRemove(Endpoint endpoint) {
    }
    public void onServiceAdd(CamelContext camelContext, Service service, Route route) {
    }
    public void onServiceRemove(CamelContext camelContext, Service service, Route route) {
    }
    public void onRoutesAdd(Collection<Route> routes) {
    }
    public void onRoutesRemove(Collection<Route> routes) {
    }
    public void onRouteContextCreate(RouteContext routeContext) {
    }

    @Override
    public void onErrorHandlerAdd(RouteContext routeContext, Processor processor, ErrorHandlerFactory errorHandlerFactory) {
    }

    public void onErrorHandlerAdd(RouteContext routeContext, Processor processor, ErrorHandlerBuilder errorHandlerBuilder) {
    }
    
    public void onErrorHandlerRemove(RouteContext routeContext, Processor errorHandler, ErrorHandlerFactory errorHandlerBuilder) {
    }
    
    public void onThreadPoolAdd(CamelContext camelContext, ThreadPoolExecutor threadPoolExecutor, String s, String s1, String s2, String s3) {
    }

    public void onThreadPoolRemove(CamelContext camelContext, ThreadPoolExecutor threadPoolExecutor) {
    }

    ///////////////////////////////////////////////////////////////////
    // Getters/Setters for IOC injection.
    ///////////////////////////////////////////////////////////////////
    public List<ACL> getAcl() {
        return acl;
    }
    public void setAcl(List<ACL> acl) {
        this.acl = acl;
    }
    public String getGroupName() {
        return groupName;
    }
    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }
    public String getId() {
        return id;
    }
    public void setId(String id) {
        this.id = id;
    }
    public IZKClient getZkClient() {
        return zkClient;
    }
    public void setZkClient(IZKClient zkClient) {
        this.zkClient = zkClient;
    }
}
