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
import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.ObjectMapper;
import org.fusesource.common.util.ClassLoaders;
import io.fabric8.api.scr.AbstractComponent;
import org.fusesource.gateway.fabric.config.ConfigParser;
import org.fusesource.gateway.fabric.config.GatewayConfig;
import org.fusesource.gateway.fabric.config.GatewaysConfig;
import org.fusesource.gateway.fabric.config.ListenConfig;
import org.osgi.framework.Bundle;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.VertxFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * The gateway service which
 */
@Service(FabricGateway.class)
@Component(name = "io.fabric8.gateway", label = "Fabric8 Gateway Service", immediate = true, metatype = false)
public class FabricGateway extends AbstractComponent {
    private static final transient Logger LOG = LoggerFactory.getLogger(FabricGateway.class);

    private String configurationUrl = "profile:io.fabric8.gateway.json";

    @Reference
    private CuratorFramework curator;

    private List<GatewayListener> listeners = new ArrayList<GatewayListener>();
    private Vertx vertx;
    private final ConfigParser configParser = new ConfigParser();

    public FabricGateway() {
    }

    @Activate
    public void activate(ComponentContext context) throws Exception {
        LOG.info("Activating the gateway " + this);

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

                GatewaysConfig config = loadConfig();
                if (config != null) {
                    createListeners(config);
                }
                return null;
            }
        });
    }

    @Modified
    public void updated() throws Exception {
        // lets reload the configuration and find all the groups to create if they are not already created
    }

    @Deactivate
    public void deactivate() {
        for (GatewayListener listener : listeners) {
            listener.destroy();
        }
    }

    public Vertx getVertx() {
        return vertx;
    }

    public CuratorFramework getCurator() {
        return curator;
    }

    protected void createListeners(GatewaysConfig config) {
        List<GatewayConfig> listeners = config.getGateways();
        for (GatewayConfig gatewayConfig : listeners) {
            createListener(gatewayConfig);
        }
    }

    protected void createListener(GatewayConfig config) {
        try {
            GatewayListener listener = config.createListener(this);
            if (listener != null) {
                listener.init();
                LOG.info("Started " + listener + " from " + config);
                listeners.add(listener);
            }
        } catch (Exception e) {
            LOG.info("Failed to create listener " + config + ". Reason: " + e);
        }
    }

    protected GatewaysConfig loadConfig() throws IOException {
        try {
            return configParser.load(configurationUrl);
        } catch (IOException e) {
            LOG.error("Failed to load configuration " + configurationUrl + ". Reason: " + e, e);
            return null;
        }
    }
}