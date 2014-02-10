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
package org.fusesource.gateway.fabric.support.vertx;

import io.fabric8.api.scr.AbstractComponent;
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
import org.fusesource.common.util.Objects;
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
@Service(VertxService.class)
@Component(name = "io.fabric8.gateway.vertx", label = "Fabric8 Gateway Vertx Service", immediate = true, metatype = false)
public class VertxServiceImpl extends AbstractComponent implements VertxService {
    private static final transient Logger LOG = LoggerFactory.getLogger(VertxServiceImpl.class);

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY, bind = "setCurator", unbind = "unsetCurator")
    private CuratorFramework curator;

    private Vertx vertx;

    public VertxServiceImpl() {
    }

    @Activate
    public void activate(ComponentContext context) throws Exception {
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
                    } catch (Throwable e) {
                        LOG.warn("Failed to use META-INF/services to discover vertx: " + e, e);
                    }
                    if (vertx == null) {
                        try {
                            DefaultVertxFactory factory = new DefaultVertxFactory();
                            vertx = factory.createVertx();
                        } catch (Throwable e) {
                            LOG.error("Failed to create Vertx instance: " + e, e);
                        }
                    }
                    LOG.info("Created a vertx implementation: " + vertx);
                }
                return null;
            }
        });
        Objects.notNull(vertx, "vertx");
    }

    @Modified
    public void updated() throws Exception {
        // lets reload the configuration and find all the groups to create if they are not already created
    }

    @Deactivate
    public void deactivate() {
        if (vertx != null) {
            try {
                vertx.stop();
            } catch (Throwable e) {
                LOG.warn("Failed to stop vertx: " + e, e);
            }
            vertx = null;
        }
    }

    @Override
    public Vertx getVertx() {
        return vertx;
    }

    public void setVertx(Vertx vertx) {
        this.vertx = vertx;
    }


    @Override
    public CuratorFramework getCurator() {
        return curator;
    }

    public void setCurator(CuratorFramework curator) {
        this.curator = curator;
    }

    public void unsetCurator(CuratorFramework curator) {
        this.curator = null;
    }
}