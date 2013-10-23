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
package org.fusesource.gateway.handlers.http;

import org.fusesource.gateway.ServiceMap;
import org.fusesource.gateway.handlers.Gateway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.http.HttpServer;

/**
 */
public class HttpGateway implements Gateway {
    private static final transient Logger LOG = LoggerFactory.getLogger(HttpGateway.class);

    private final Vertx vertx;
    private final ServiceMap serviceMap;
    private final int port;
    private final String protocol = "http";
    private String host;
    private HttpServer server;
    private HttpGatewayHandler handler;

    public HttpGateway(Vertx vertx, ServiceMap serviceMap, int port) {
        this.vertx = vertx;
        this.serviceMap = serviceMap;
        this.port = port;
    }

    @Override
    public String toString() {
        return "HttpGateway{" +
                "protocol='" + protocol + '\'' +
                ", port=" + port +
                ", host='" + host + '\'' +
                '}';
    }

    @Override
    public void init() {
        if (handler == null) {
            handler = new HttpGatewayHandler(vertx, serviceMap);
        }
        server = vertx.createHttpServer().requestHandler(handler);
        if (host != null) {
            LOG.info("Listening on port " + port + " and host " + host);
            server = server.listen(port, host);
        } else {
            LOG.info("Listening on port " + port);
            server = server.listen(port);
        }
    }

    @Override
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

    public ServiceMap getServiceMap() {
        return serviceMap;
    }

    @Override
    public String getProtocol() {
        return protocol;
    }
}

