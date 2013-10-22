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
package org.fusesource.gateway.fabric;

import org.apache.aries.util.AriesFrameworkUtil;
import org.apache.curator.framework.CuratorFramework;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Modified;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.fusesource.common.util.ClassLoaders;
import org.fusesource.fabric.api.scr.AbstractComponent;
import org.fusesource.fabric.zookeeper.ZkPath;
import org.fusesource.gateway.ServiceMap;
import org.fusesource.gateway.handlers.http.HttpGateway;
import org.fusesource.gateway.handlers.tcp.TcpGateway;
import org.osgi.framework.Bundle;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.VertxFactory;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.concurrent.Callable;

/**
 * The gateway service which
 */
@Service(FabricGateway.class)
@Component(name = "org.fusesource.fabric.gateway", description = "Fabric Gateway Service", immediate = true)
public class FabricGateway extends AbstractComponent {
    private static final transient Logger LOG = LoggerFactory.getLogger(FabricGateway.class);

    @Reference
    private CuratorFramework curator;

    private List<GatewayGroup> groups = new ArrayList<GatewayGroup>();
    private Vertx vertx;

    @Activate
    public void activate(ComponentContext context, final Map<String, String> properties) throws Exception {
        System.out.println("Activating the gateway " + this);

        // TODO support injecting of the ClassLoader without depending on OSGi APIs
        // see https://github.com/jboss-fuse/fuse/issues/104
        Bundle bundle = context.getBundleContext().getBundle();
        final ClassLoader classLoader = AriesFrameworkUtil.getClassLoader(bundle);

        // lets set the thread context class loader for vertx to be able to find services
        ClassLoaders.withContextClassLoader(classLoader, new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                if (vertx == null) {
                    vertx = VertxFactory.newVertx();
                }

                // TODO detect changes in the config and add/remove groups based on that?
                GatewayGroup webGroup = createWebGroup(80);
                addGroup(webGroup);

                // hard code for now until we figure out the configuration
                GatewayGroup messagingGroup = createMessagingGroup("tcp", 61616);
                addGroup(messagingGroup);

                updated(properties);
                return null;
            }
        });
    }

    @Modified
    public void updated(Map<String, String> properties) throws Exception {
        // lets reload the configuration and find all the groups to create if they are not already created

    }

    protected GatewayGroup createMessagingGroup(String protocol, int port) {
        ServiceMap serviceMap = new ServiceMap();
        TcpGateway gateway = new TcpGateway(vertx, serviceMap, port, protocol);
        return new GatewayGroup(curator, ZkPath.MQ_CLUSTERS.getPath(), serviceMap, gateway);
    }

    protected GatewayGroup createWebGroup(int port) {
        ServiceMap serviceMap = new ServiceMap();
        HttpGateway gateway = new HttpGateway(vertx, serviceMap, port);
        return new GatewayGroup(curator, ZkPath.WEBAPPS_CLUSTERS.getPath(), serviceMap, gateway);
    }

    protected void addGroup(GatewayGroup group) throws Exception {
        group.init();
        groups.add(group);
    }

    @Deactivate
    void deactivate() {
        for (GatewayGroup group : groups) {
            group.destroy();
        }
    }
}