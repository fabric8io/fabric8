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

import io.fabric8.api.FabricService;
import io.fabric8.api.scr.AbstractComponent;
import io.fabric8.api.scr.ValidatingReference;
import org.apache.aries.util.AriesFrameworkUtil;
import org.apache.curator.framework.CuratorFramework;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Modified;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.fusesource.common.util.ClassLoaders;
import org.osgi.framework.Bundle;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.VertxFactory;
import org.vertx.java.core.impl.DefaultVertxFactory;

import java.util.concurrent.Callable;

/**
 * The gateway service which
 */
@Service(FabricGateway.class)
@Component(name = "io.fabric8.gateway", label = "Fabric8 Gateway Service", immediate = true, metatype = false)
public class FabricGateway extends AbstractComponent {
    private static final transient Logger LOG = LoggerFactory.getLogger(FabricGateway.class);

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY, bind = "setCurator", unbind = "unsetCurator")
    private CuratorFramework curator;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY, bind = "setFabricService", unbind = "unsetFabricService")
    private FabricService fabricService;

    private Vertx vertx;

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
                    try {
                        vertx = VertxFactory.newVertx();
                    } catch (Exception e) {
                        LOG.warn("Failed to use META-INF/services to discover vertx: " + e, e);
                    }
                    if (vertx == null) {
                        DefaultVertxFactory factory = new DefaultVertxFactory();
                        vertx = factory.createVertx();
                    }
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
    }

    public Vertx getVertx() {
        return vertx;
    }

    public CuratorFramework getCurator() {
        return curator;
    }

    public void setCurator(CuratorFramework curator) {
        this.curator = curator;
    }

    public void unsetCurator(CuratorFramework curator) {
        this.curator = null;
    }

    public void setVertx(Vertx vertx) {
        this.vertx = vertx;
    }

    public FabricService getFabricService() {
        return fabricService;
    }

    public void setFabricService(FabricService fabricService) {
        this.fabricService = fabricService;
    }

    public void unsetFabricService(FabricService fabricService) {
        this.fabricService = null;
    }
}