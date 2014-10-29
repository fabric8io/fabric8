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

import io.fabric8.utils.Objects;
import io.fabric8.vertx.FabricVertexFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.apache.curator.framework.CuratorFramework;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.Vertx;

/**
 * The gateway service which
 */
@ApplicationScoped
public class VertxServiceImpl implements VertxService {
    private static final transient Logger LOG = LoggerFactory.getLogger(VertxServiceImpl.class);

    @Inject
    private CuratorFramework curator;

    @Inject
    private FabricVertexFactory vertxFactory;

    private Vertx vertx;

    @PostConstruct
    public void activate() throws Exception {
        vertx = vertxFactory.createVertx();
        Objects.notNull(vertx, "vertx");
    }

//    @Modified
//    public void updated() throws Exception {
//        // lets reload the configuration and find all the groups to create if they are not already created
//    }

    @PreDestroy
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
