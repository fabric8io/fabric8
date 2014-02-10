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
package org.fusesource.gateway.handlers.tcp;

import org.fusesource.gateway.ServiceMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.net.NetServer;
import org.vertx.java.core.net.NetSocket;

/**
 */
public class TcpGateway {
    private static final transient Logger LOG = LoggerFactory.getLogger(TcpGateway.class);

    private final Vertx vertx;
    private final ServiceMap serviceMap;
    private final int port;
    private final String protocol;
    private final Handler<NetSocket> handler;
    private String host;
    private NetServer server;

    public TcpGateway(Vertx vertx, ServiceMap serviceMap, int port, String protocol, Handler<NetSocket> handler) {
        this.vertx = vertx;
        this.serviceMap = serviceMap;
        this.port = port;
        this.protocol = protocol;
        this.handler = handler;
    }

    @Override
    public String toString() {
        return "TcpGateway{" +
                "protocol='" + protocol + '\'' +
                ", port=" + port +
                ", host='" + host + '\'' +
                '}';
    }

    public void init() {
        server = vertx.createNetServer().connectHandler(handler);
        if (host != null) {
            LOG.info("Listening on port " + port + " and host " + host + " for protocol: " + protocol);
            server = server.listen(port, host);
        } else {
            LOG.info("Listening on port " + port + " for protocol: " + protocol);
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

    public ServiceMap getServiceMap() {
        return serviceMap;
    }

    public String getProtocol() {
        return protocol;
    }
}
