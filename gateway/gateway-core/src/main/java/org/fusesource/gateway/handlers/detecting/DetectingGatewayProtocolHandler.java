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
import org.fusesource.gateway.SocketWrapper;
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

import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

/**
 * The initial vertx socket handler of a DetectingGateway.
 */
public class DetectingGatewayProtocolHandler implements Handler<SocketWrapper> {
    private static final transient Logger LOG = LoggerFactory.getLogger(DetectingGatewayProtocolHandler.class);

    Vertx vertx;
    ServiceMap serviceMap;
    LoadBalancer<ServiceDetails> serviceLoadBalancer;
    String defaultVirtualHost;
    ArrayList<Protocol> protocols;
    int maxProtocolIdentificationLength;
    ClientRequestFacadeFactory clientRequestFacadeFactory = new ClientRequestFacadeFactory("PROTOCOL_SESSION_ID, PROTOCOL_CLIENT_ID, REMOTE_ADDRESS");
    final AtomicReference<InetSocketAddress> httpGateway = new AtomicReference<InetSocketAddress>();

    public Vertx getVertx() {
        return vertx;
    }

    public void setVertx(Vertx vertx) {
        this.vertx = vertx;
    }

    public void setServiceMap(ServiceMap serviceMap) {
        this.serviceMap = serviceMap;
    }

    public LoadBalancer<ServiceDetails> getServiceLoadBalancer() {
        return serviceLoadBalancer;
    }

    public void setServiceLoadBalancer(LoadBalancer<ServiceDetails> serviceLoadBalancer) {
        this.serviceLoadBalancer = serviceLoadBalancer;
    }

    public String getDefaultVirtualHost() {
        return defaultVirtualHost;
    }

    public void setDefaultVirtualHost(String defaultVirtualHost) {
        this.defaultVirtualHost = defaultVirtualHost;
    }

    public ArrayList<Protocol> getProtocols() {
        return protocols;
    }

    public void setProtocols(ArrayList<Protocol> protocols) {
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
    public void handle(final SocketWrapper socket) {
        socket.readStream().dataHandler(new Handler<Buffer>() {
            Buffer received = new Buffer();

            @Override
            public void handle(Buffer event) {
                received.appendBuffer(event);
                LOG.info("Detecting protocol from: " + received.length() + " request bytes");
                for (final Protocol protocol : protocols) {
                    if (protocol.matches(received)) {
                        if ("http".equals(protocol.getProtocolName())) {
                            InetSocketAddress target = getHttpGateway();
                            if (target != null) {
                                try {
                                    URI url = new URI("http://" + target.getHostString() + ":" + target.getPort());
                                    createClient(socket, url, received);
                                } catch (URISyntaxException e) {
                                    LOG.warn("Could not build valid connect URI.", e);
                                    socket.close();
                                }
                            } else {
                                LOG.info("No http gateway available for the http protocol");
                                socket.close();
                            }
                        } else {
                            protocol.snoopConnectionParameters(socket, received, new Handler<ConnectionParameters>() {
                                @Override
                                public void handle(ConnectionParameters connectionParameters) {
                                    // this will install a new dataHandler on the socket.
                                    if( connectionParameters.protocol == null )
                                        connectionParameters.protocol = protocol.getProtocolName();
                                    if( connectionParameters.protocolSchemes == null )
                                        connectionParameters.protocolSchemes = protocol.getProtocolSchemes();
                                    route(socket, connectionParameters, received);
                                }
                            });
                            return;
                        }
                    }
                }
                if (received.length() >= maxProtocolIdentificationLength) {
                    LOG.info("Connection did not use one of the enabled protocols " + getProtocolNames());
                    socket.close();
                }
            }
        });
    }

    public void route(final SocketWrapper socket, ConnectionParameters params, final Buffer received) {
        NetClient client = null;

        String host = params.protocolVirtualHost;
        if( host==null ) {
            host = defaultVirtualHost;
        }
        HashSet<String> schemes = new HashSet<String>(Arrays.asList(params.protocolSchemes));
        if(host!=null) {
            List<ServiceDetails> services = serviceMap.getServices(host);

            // Lets try again with the defaultVirtualHost
            if( services.isEmpty() && defaultVirtualHost!=null ) {
                host = defaultVirtualHost;
                services = serviceMap.getServices(host);
            }

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
                                if (schemes.contains(urlProtocol)) {
                                    LOG.info(String.format("Connecting '%s' requesting virtual host '%s' with client key '%s' to '%s:%d' using the %s protocol",
                                        socket.remoteAddress(), params.protocolVirtualHost, clientRequestFacade.getClientRequestKey(), uri.getHost(), uri.getPort(), params.protocol
                                      ));

                                    client = createClient(socket, uri, received);
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
            // failed to route
            LOG.info(String.format("No endpoint available for virtual host '%s' and protocol %s", params.protocolVirtualHost, params.protocol));
            socket.close();
        }
    }

    /**
     * Creates a new client for the given URL and handler
     */
    private NetClient createClient(final SocketWrapper socket, URI url, final Buffer received) {
        return vertx.createNetClient().connect(url.getPort(), url.getHost(), new Handler<AsyncResult<NetSocket>>() {
            public void handle(final AsyncResult<NetSocket> asyncSocket) {
                NetSocket clientSocket = asyncSocket.result();
                clientSocket.write(received);
                Pump.createPump(clientSocket, socket.writeStream()).start();
                Pump.createPump(socket.readStream(), clientSocket).start();
            }
        });
    }

    public ServiceMap getServiceMap() {
        return serviceMap;
    }

    public InetSocketAddress getHttpGateway() {
        return httpGateway.get();
    }
    public void setHttpGateway(InetSocketAddress value) {
        httpGateway.set(value);
    }


}
