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

import org.fusesource.common.util.Objects;
import org.fusesource.common.util.Strings;
import org.fusesource.gateway.ServiceDetails;
import org.fusesource.gateway.ServiceMap;
import org.fusesource.gateway.loadbalancer.LoadBalancer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.net.NetClient;
import org.vertx.java.core.net.NetSocket;
import org.vertx.java.core.streams.Pump;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

/**
 * A TCP gateway implementation
 */
public class TcpGatewayHandler implements Handler<NetSocket> {
    private static final transient Logger LOG = LoggerFactory.getLogger(TcpGatewayHandler.class);

    private final Vertx vertx;
    private final ServiceMap serviceMap;
    private final String protocol;
    private final LoadBalancer<String> pathLoadBalancer;
    private final LoadBalancer<ServiceDetails> serviceLoadBalancer;

    public TcpGatewayHandler(Vertx vertx, ServiceMap serviceMap, String protocol, LoadBalancer<String> pathLoadBalancer, LoadBalancer<ServiceDetails> serviceLoadBalancer) {
        this.vertx = vertx;
        this.serviceMap = serviceMap;
        this.protocol = protocol;
        this.pathLoadBalancer = pathLoadBalancer;
        this.serviceLoadBalancer = serviceLoadBalancer;
    }

    @Override
    public void handle(final NetSocket socket) {
        NetClient client = null;
        List<String> paths = serviceMap.getPaths();
        TcpClientRequestFacade requestFacade = new TcpClientRequestFacade(socket);
        String path = pathLoadBalancer.choose(paths, requestFacade);
        if (path != null) {
            List<ServiceDetails> services = serviceMap.getServices(path);
            if (!services.isEmpty()) {
                ServiceDetails serviceDetails = serviceLoadBalancer.choose(services, requestFacade);
                if (serviceDetails != null) {
                    List<String> urlStrings = serviceDetails.getServices();
                    for (String urlString : urlStrings) {
                        if (Strings.notEmpty(urlString)) {
                            // lets create a client for this request...
                            try {
                                URI uri = new URI(urlString);
                                //URL url = new URL(urlString);
                                String urlProtocol = uri.getScheme();
                                if (Objects.equal(protocol, urlProtocol)) {
                                    Handler<AsyncResult<NetSocket>> handler = new Handler<AsyncResult<NetSocket>>() {
                                        public void handle(final AsyncResult<NetSocket> asyncSocket) {
                                            NetSocket clientSocket = asyncSocket.result();
                                            Pump.createPump(clientSocket, socket).start();
                                            Pump.createPump(socket, clientSocket).start();
                                        }
                                    };
                                    client = createClient(socket, uri, handler);
                                    break;
                                }
                            } catch (MalformedURLException e) {
                                LOG.warn("Failed to parse URL: " + urlString + ". " + e, e);
                            } catch (URISyntaxException e) {
                                LOG.warn("Failed to parse URI: " + urlString + ". " + e, e);
                            }
                        }
                    }
                }
            }
        }
        if (client == null) {
            // fail to route
            LOG.info("No service available for protocol " + protocol + " for paths " + paths);
            socket.close();
        }
    }

    /**
     * Creates a new client for the given URL and handler
     */
    protected NetClient createClient(NetSocket socket, URI url, Handler<AsyncResult<NetSocket>> handler) throws MalformedURLException {
        NetClient client = vertx.createNetClient();
        int port = url.getPort();
        String host = url.getHost();
        LOG.info("Connecting " + socket.remoteAddress() + " to host " + host + " port " + port + " protocol " + protocol);
        return client.connect(port, host, handler);
    }
}
