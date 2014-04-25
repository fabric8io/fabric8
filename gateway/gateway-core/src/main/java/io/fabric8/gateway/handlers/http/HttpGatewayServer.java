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
package io.fabric8.gateway.handlers.http;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.http.HttpServer;
import org.vertx.java.core.http.ServerWebSocket;

/**
 */
public class HttpGatewayServer {
    private static final transient Logger LOG = LoggerFactory.getLogger(HttpGatewayServer.class);

    private final Vertx vertx;
    private final HttpGatewayHandler handler;
    private final int port;
    private String host;
    private HttpServer server;
    private Handler<ServerWebSocket> websocketHandler;

    public HttpGatewayServer(Vertx vertx, HttpGatewayHandler handler, Handler<ServerWebSocket> websocketHandler, int port) {
        this.vertx = vertx;
        this.handler = handler;
        this.websocketHandler = websocketHandler;
        this.port = port;
    }

    @Override
    public String toString() {
        return "HttpGatewayServer{" +
                "port=" + port +
                ", host='" + host + '\'' +
                '}';
    }

    public void init() {
        server = vertx.createHttpServer();
        server.requestHandler(handler);
        if( websocketHandler!=null ) {
            server.websocketHandler(websocketHandler);
        }
        if (host != null) {
            LOG.info("Listening on port " + port + " and host " + host);
            server = server.listen(port, host);
        } else {
            LOG.info("Listening on port " + port);
            server = server.listen(port);
        }
    }

    public void destroy() {
        server.close();
    }

    public int getPort() {
        return port;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public Vertx getVertx() {
        return vertx;
    }


}

