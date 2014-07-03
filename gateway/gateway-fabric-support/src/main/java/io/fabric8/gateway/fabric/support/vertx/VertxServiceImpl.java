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
package io.fabric8.gateway.fabric.support.vertx;

import io.fabric8.api.scr.AbstractComponent;
import io.fabric8.vertx.FabricVertexFactory;
import org.apache.aries.util.AriesFrameworkUtil;
import org.apache.curator.framework.CuratorFramework;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Modified;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import io.fabric8.common.util.ClassLoaders;
import io.fabric8.common.util.Objects;
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

    @Reference(referenceInterface = FabricVertexFactory.class)
    private FabricVertexFactory vertxFactory;

    private Vertx vertx;

    public VertxServiceImpl() {
    }

    @Activate
    public void activate(ComponentContext context) throws Exception {
        vertx = vertxFactory.createVertx();
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

    void bindVertxFactory(FabricVertexFactory service) {
        this.vertxFactory = service;
    }

    void unbindVertxFactory(FabricVertexFactory service) {
        this.vertxFactory = null;
    }
}
