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
package io.fabric8.vertx.internal;

import io.fabric8.vertx.FabricVertexFactory;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.VertxFactory;
import org.vertx.java.core.impl.DefaultVertxFactory;

@Component(immediate = true)
@Service(FabricVertexFactory.class)
public class FabricVertxFactoryImpl extends VertxFactory implements FabricVertexFactory {

    private final DefaultVertxFactory defaultVertxFactory = new DefaultVertxFactory();

    @Override
    public Vertx createVertx() {
        return defaultVertxFactory.createVertx();
    }

    @Override
    public Vertx createVertx(String hostname) {
        return defaultVertxFactory.createVertx(hostname);
    }

    @Override
    public Vertx createVertx(int port, String hostname) {
        return defaultVertxFactory.createVertx(port, hostname);
    }

    @Override
    public void createVertx(int port, String hostname, Handler<AsyncResult<Vertx>> resultHandler) {
        defaultVertxFactory.createVertx(port, hostname, resultHandler);
    }
}
