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
import org.fusesource.gateway.handlers.Gateway;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.net.NetServer;
import org.vertx.java.core.net.NetSocket;

/**
 */
public class TcpGateway implements Gateway {
    private final Vertx vertx;
    private final ServiceMap serviceMap;
    private final int port;
    private String host;
    private NetServer server;
    private Handler<NetSocket> handler;

    public TcpGateway(Vertx vertx, ServiceMap serviceMap, int port, String protocol) {
        this.vertx = vertx;
        this.serviceMap = serviceMap;
        this.port = port;
    }

    @Override
    public void init() {
        if (handler == null) {
            handler = new TcpGatewayHandler(vertx, serviceMap);
        }
        server = vertx.createNetServer().connectHandler(handler);
        if (host != null) {
            server = server.listen(port, host);
        } else {
            server = server.listen(port);
        }
    }

    @Override
    public void destroy() {
        server.close();
    }

    @Override
    public int getPort() {
        return port;
    }

    @Override
    public String getHost() {
        return host;
    }

    @Override
    public void setHost(String host) {
        this.host = host;
    }
}
