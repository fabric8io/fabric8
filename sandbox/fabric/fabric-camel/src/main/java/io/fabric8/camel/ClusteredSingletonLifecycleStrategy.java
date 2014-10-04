/**
 *  Copyright 2005-2014 Red Hat, Inc.
 *
 *  Red Hat licenses this file to you under the Apache License, version
 *  2.0 (the "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 *  implied.  See the License for the specific language governing
 *  permissions and limitations under the License.
 */
package io.fabric8.camel;

import org.apache.camel.CamelContext;
import org.apache.camel.Component;
import org.apache.camel.Endpoint;
import org.apache.camel.ErrorHandlerFactory;
import org.apache.camel.Processor;
import org.apache.camel.Route;
import org.apache.camel.Service;
import org.apache.camel.VetoCamelContextStartException;
import org.apache.camel.builder.ErrorHandlerBuilder;
import org.apache.camel.spi.LifecycleStrategy;
import org.apache.camel.spi.RouteContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.RetryOneTime;
import io.fabric8.groups.Group;
import io.fabric8.groups.GroupListener;
import io.fabric8.groups.internal.ManagedGroupFactory;
import io.fabric8.groups.internal.ManagedGroupFactoryBuilder;

import java.util.Collection;
import java.util.concurrent.Callable;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 *
 */
public class ClusteredSingletonLifecycleStrategy implements LifecycleStrategy, GroupListener<CamelNodeState>, Callable<CuratorFramework> {

    // TODO: Work in progress

    public static Log LOG = LogFactory.getLog(ClusteredSingletonLifecycleStrategy.class);

    Group<CamelNodeState> group;
    String groupName;

    volatile CamelContext camelContext;
    final AtomicBoolean started = new AtomicBoolean();

    CuratorFramework curator;
    ManagedGroupFactory factory;

    static void info(String msg, Object... args) {
        if(LOG.isInfoEnabled()) {
            LOG.info(String.format(msg, args));
        }
    }

    CamelNodeState createState() {
        CamelNodeState state = new CamelNodeState(groupName);
        state.started = started.get();
        return state;
    }


    public void start() throws Exception {
        factory = ManagedGroupFactoryBuilder.create(curator, getClass().getClassLoader(), this);
        group = factory.createGroup("/fabric/camel-clusters/" + groupName, CamelNodeState.class);
        group.update(createState());
        group.add(this);
        group.start();
        info("Camel context %s is waiting to become the master", groupName);
    }

    public CuratorFramework call() throws Exception {
        String connectString = System.getProperty("zookeeper.url", "localhost:2181");
        String password = System.getProperty("zookeeper.password");

        CuratorFrameworkFactory.Builder builder = CuratorFrameworkFactory.builder()
                .connectString(connectString)
                .retryPolicy(new RetryOneTime(1000))
                .connectionTimeoutMs(10000);

        if (password != null && !password.isEmpty()) {
            builder.authorization("digest", ("fabric:"+password).getBytes());
        }

        CuratorFramework client = builder.build();
        LOG.debug("Starting curator " + curator);
        client.start();
        return client;
    }

    public void stop() {
        try {
            factory.close();
        } catch (Throwable ignore) {
        }
    }


    @Override
    public void groupEvent(Group<CamelNodeState> group, GroupEvent event) {
        if (group.isMaster()) {
            if (started.compareAndSet(false, true)) {
                info("Camel context %s is now the master, starting the context.", groupName);
                try {
                    camelContext.start();
                    // Update the state of the master since he is now running.
                    group.update(createState());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } else {
            if (started.compareAndSet(true, false)) {
                info("Camel context %s is now a slave, stopping the context.", groupName);
                try {
                    camelContext.stop();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
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
    public String getGroupName() {
        return groupName;
    }
    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }
    public CuratorFramework getCurator() {
        return curator;
    }
    public void setCurator(CuratorFramework curator) {
        this.curator = curator;
    }
}
