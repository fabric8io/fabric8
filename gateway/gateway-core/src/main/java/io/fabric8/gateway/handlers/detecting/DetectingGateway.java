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
package io.fabric8.gateway.handlers.detecting;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.net.NetServer;

import java.net.InetSocketAddress;

/**
 * A gateway which listens on a port and snoops the initial request bytes from a client
 * to detect the protocol and protocol specific connection parameters such a requested
 * virtual host to handle proxying the connection to an appropriate service.
 */
public class DetectingGateway {

    private static final transient Logger LOG = LoggerFactory.getLogger(DetectingGateway.class);

    private final Vertx vertx;
    private final int port;
    private final DetectingGatewayProtocolHandler handler;
    private String host;
    private NetServer server;

    private FutureHandler<AsyncResult<NetServer>> listenFuture = new FutureHandler<AsyncResult<NetServer>>() {
        @Override
        public void handle(AsyncResult<NetServer> event) {
            if( event.succeeded() ) {
                LOG.info(String.format("Gateway listening on %s:%d for protocols: %s", server.host(), server.port(), handler.getProtocolNames()));
            }
            super.handle(event);
        }
    };

    public DetectingGateway(Vertx vertx, int port, DetectingGatewayProtocolHandler handler) {
        this.vertx = vertx;
        this.port = port;
        this.handler = handler;
    }

    @Override
    public String toString() {
        return "DetectingGateway{" +
                ", port=" + port +
                ", host='" + host + '\'' +
                ", protocols='" + handler.getProtocolNames() + '\'' +
                '}';
    }


    public void init() {
        server = vertx.createNetServer().connectHandler(new DetectingGatewayNetSocketHandler(handler));
        if (host != null) {
            server = server.listen(port, host, listenFuture);
        } else {
            server = server.listen(port, listenFuture);
        }
    }

    public void destroy() {
        server.close();
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getBoundPort() throws Exception {
        return FutureHandler.result(listenFuture).port();
    }

    public DetectingGatewayProtocolHandler getHandler() {
        return handler;
    }

    public void setHttpGateway(InetSocketAddress value) {
        handler.setHttpGateway(value);
    }

    public InetSocketAddress getHttpGateway() {
        return handler.getHttpGateway();
    }
}
