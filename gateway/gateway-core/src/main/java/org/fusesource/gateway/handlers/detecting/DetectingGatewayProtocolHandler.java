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
package org.fusesource.gateway.handlers.detecting;

import org.fusesource.common.util.Objects;
import org.fusesource.common.util.Strings;
import org.fusesource.gateway.ServiceDetails;
import org.fusesource.gateway.ServiceMap;
import org.fusesource.gateway.loadbalancer.ClientRequestFacade;
import org.fusesource.gateway.loadbalancer.ClientRequestFacadeFactory;
import org.fusesource.gateway.loadbalancer.ConnectionParameters;
import org.fusesource.gateway.loadbalancer.LoadBalancer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.net.NetClient;
import org.vertx.java.core.net.NetSocket;
import org.vertx.java.core.streams.Pump;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * The initial vertx socket handler of a DetectingGateway.
 */
public class DetectingGatewayProtocolHandler implements Handler<NetSocket> {
    private static final transient Logger LOG = LoggerFactory.getLogger(DetectingGatewayProtocolHandler.class);

    private final Vertx vertx;
    private final ServiceMap serviceMap;
    private final LoadBalancer<ServiceDetails> serviceLoadBalancer;
    private final ArrayList<Protocol> protocols;
    private final int maxProtocolIdentificationLength;
    private ClientRequestFacadeFactory clientRequestFacadeFactory = new ClientRequestFacadeFactory("PROTOCOL_SESSION_ID, PROTOCOL_CLIENT_ID, REMOTE_ADDRESS");

    public DetectingGatewayProtocolHandler(Vertx vertx, ServiceMap serviceMap, List<Protocol> protocols, LoadBalancer<ServiceDetails> serviceLoadBalancer) {
        this.vertx = vertx;
        this.serviceMap = serviceMap;
        this.serviceLoadBalancer = serviceLoadBalancer;
        this.protocols = new ArrayList<Protocol>(protocols);
        int max = 0;
        for (Protocol protocol : protocols) {
            if( protocol.getMaxIdentificationLength() > max ) {
                max = protocol.getMaxIdentificationLength();
            }
        }
        maxProtocolIdentificationLength = max;
    }

    public Collection<String> getProtocolNames() {
        ArrayList<String> rc = new ArrayList<String>(protocols.size());
        for (Protocol protocol : protocols) {
            rc.add(protocol.getProtocolName());
        }
        return rc;
    }

    @Override
    public void handle(final NetSocket socket) {
        socket.dataHandler(new Handler<Buffer>() {
            Buffer received = new Buffer();

            @Override
            public void handle(Buffer event) {
                received.appendBuffer(event);
                LOG.info("Received a buffer, matching protocols against: " + received.length());
                for (final Protocol protocol : protocols) {
                    if( protocol.matches(received) ) {
                        protocol.snoopConnectionParameters(socket, received, new Handler<ConnectionParameters>() {
                            @Override
                            public void handle(ConnectionParameters connectionParameters) {
                                // this will install a new dataHandler on the socket.
                                route(socket, connectionParameters, received);
                            }
                        });
                        return;
                    }
                }
                if( received.length() >= maxProtocolIdentificationLength) {
                    LOG.info("Connection did not use one of the enabled protocols "+getProtocolNames());
                    socket.close();
                }
            }
        });
    }


    public void route(final NetSocket socket, ConnectionParameters params, final Buffer received) {
        NetClient client = null;
        String host = params.protocolVirtualHost;
        if (host != null) {
            List<ServiceDetails> services = serviceMap.getServices(host);
            if (!services.isEmpty()) {
                ClientRequestFacade clientRequestFacade = clientRequestFacadeFactory.create(socket, params);
                ServiceDetails serviceDetails = serviceLoadBalancer.choose(services, clientRequestFacade);
                if (serviceDetails != null) {
                    List<String> urlStrings = serviceDetails.getServices();
                    for (String urlString : urlStrings) {
                        if (Strings.notEmpty(urlString)) {
                            // lets create a client for this request...
                            try {
                                URI uri = new URI(urlString);
                                //URL url = new URL(urlString);
                                String urlProtocol = uri.getScheme();
                                if (Objects.equal(params.protocol, urlProtocol)) {
                                    Handler<AsyncResult<NetSocket>> handler = new Handler<AsyncResult<NetSocket>>() {
                                        public void handle(final AsyncResult<NetSocket> asyncSocket) {
                                            NetSocket clientSocket = asyncSocket.result();
                                            clientSocket.write(received);
                                            Pump.createPump(clientSocket, socket).start();
                                            Pump.createPump(socket, clientSocket).start();
                                        }
                                    };
                                    client = createClient(socket, uri, handler, clientRequestFacade, params);
                                    break;
                                }
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
            LOG.info("No service available for protocol " + params.protocol + " for service " + host);
            socket.close();
        }
    }

    /**
     * Creates a new client for the given URL and handler
     */
    private NetClient createClient(NetSocket socket, URI url, Handler<AsyncResult<NetSocket>> handler, ClientRequestFacade facade, ConnectionParameters params) {
        NetClient client = vertx.createNetClient();
        int port = url.getPort();
        String host = url.getHost();
        LOG.info(String.format("Connecting '%s' requesting host '%s' with client key '%s' to '%s:%d' using the %s protocol",
            socket.remoteAddress(), params.protocolVirtualHost, facade.getClientRequestKey(), host, port, params.protocol
          ));
        return client.connect(port, host, handler);
    }

}
