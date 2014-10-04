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
package io.fabric8.gateway.handlers.detecting;

import io.fabric8.common.util.ShutdownTracker;
import io.fabric8.common.util.Strings;
import io.fabric8.gateway.ServiceDetails;
import io.fabric8.gateway.ServiceMap;
import io.fabric8.gateway.SocketWrapper;
import io.fabric8.gateway.handlers.detecting.protocol.ssl.SslConfig;
import io.fabric8.gateway.handlers.detecting.protocol.ssl.SslSocketWrapper;
import io.fabric8.gateway.handlers.loadbalancer.ClientRequestFacadeFactory;
import io.fabric8.gateway.handlers.loadbalancer.ConnectionParameters;
import io.fabric8.gateway.loadbalancer.ClientRequestFacade;
import io.fabric8.gateway.loadbalancer.LoadBalancer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.net.NetClient;
import org.vertx.java.core.net.NetServer;
import org.vertx.java.core.net.NetSocket;
import org.vertx.java.core.streams.Pump;
import org.vertx.java.core.streams.ReadStream;

import javax.net.ssl.SSLContext;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * A gateway which listens on a port and snoops the initial request bytes from a client
 * to detect the protocol and protocol specific connection parameters such a requested
 * virtual host to handle proxying the connection to an appropriate service.
 */
public class DetectingGateway implements DetectingGatewayMBean {

    private static final transient Logger LOG = LoggerFactory.getLogger(DetectingGateway.class);

    Vertx vertx;
    ServiceMap serviceMap;
    LoadBalancer serviceLoadBalancer;
    String defaultVirtualHost;
    ArrayList<Protocol> protocols;
    int maxProtocolIdentificationLength;
    ClientRequestFacadeFactory clientRequestFacadeFactory = new ClientRequestFacadeFactory("PROTOCOL_SESSION_ID, PROTOCOL_CLIENT_ID, REMOTE_ADDRESS");
    final AtomicReference<InetSocketAddress> httpGateway = new AtomicReference<InetSocketAddress>();
    SslConfig sslConfig;
    long connectionTimeout = 5000;

    final AtomicLong receivedConnectionAttempts = new AtomicLong();
    final AtomicLong successfulConnectionAttempts = new AtomicLong();
    final AtomicLong failedConnectionAttempts = new AtomicLong();
    HashSet<SocketWrapper> socketsConnecting = new HashSet<SocketWrapper>();
    HashSet<ConnectedSocketInfo> socketsConnected = new HashSet<ConnectedSocketInfo>();
    private ShutdownTracker shutdownTacker = new ShutdownTracker();

    private int port;
    private String host;
    private NetServer server;

    private FutureHandler<AsyncResult<NetServer>> listenFuture = new FutureHandler<AsyncResult<NetServer>>() {
        @Override
        public void handle(AsyncResult<NetServer> event) {
            if( event.succeeded() ) {
                LOG.info(String.format("Gateway listening on %s:%d for protocols: %s", server.host(), server.port(), getProtocolNames()));
            }
            super.handle(event);
        }
    };

    @Override
    public String toString() {
        return "DetectingGateway{" +
                ", port=" + port +
                ", host='" + host + '\'' +
                ", protocols='" + getProtocolNames() + '\'' +
                '}';
    }


    public void init() {
        server = vertx.createNetServer().connectHandler(new DetectingGatewayNetSocketHandler(this));
        if (host != null) {
            server = server.listen(port, host, listenFuture);
        } else {
            server = server.listen(port, listenFuture);
        }
    }

    public void destroy() {
        server.close();
        for (SocketWrapper socket : new ArrayList<>(socketsConnecting)) {
            handleConnectFailure(socket, null);
        }
        for (ConnectedSocketInfo socket : new ArrayList<>(socketsConnected)) {
            handleShutdown(socket);
        }
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

    public Vertx getVertx() {
        return vertx;
    }

    public void setVertx(Vertx vertx) {
        this.vertx = vertx;
    }

    public void setServiceMap(ServiceMap serviceMap) {
        this.serviceMap = serviceMap;
    }

    public LoadBalancer getServiceLoadBalancer() {
        return serviceLoadBalancer;
    }

    public void setServiceLoadBalancer(LoadBalancer serviceLoadBalancer) {
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

    SSLContext sslContext;
    SslSocketWrapper.ClientAuth clientAuth = SslSocketWrapper.ClientAuth.WANT;

    public void setShutdownTacker(ShutdownTracker shutdownTacker) {

        this.shutdownTacker = shutdownTacker;
    }

    static class ConnectedSocketInfo {

        private final ConnectionParameters params;
        private final URI url;
        private final SocketWrapper from;
        private final NetSocket to;

        public ConnectedSocketInfo(ConnectionParameters params, URI url, SocketWrapper from, NetSocket to) {
            this.params = params;
            this.url = url;
            this.from = from;
            this.to = to;
        }
    }

    public void handle(final SocketWrapper socket) {
        shutdownTacker.retain();
        receivedConnectionAttempts.incrementAndGet();
        socketsConnecting.add(socket);

        if( connectionTimeout > 0 ) {
            vertx.setTimer(connectionTimeout, new Handler<Long>() {
                public void handle(Long timerID) {
                    if( socketsConnecting.contains(socket) ) {
                        handleConnectFailure(socket, String.format("Gateway client '%s' protocol detection timeout.", socket.remoteAddress()));
                    }
                }
            });
        }

        ReadStream<ReadStream> readStream = socket.readStream();
        readStream.exceptionHandler(new Handler<Throwable>() {
            @Override
            public void handle(Throwable e) {
                handleConnectFailure(socket, String.format("Failed to route gateway client '%s' due to: %s", socket.remoteAddress(), e));
            }
        });
        readStream.endHandler(new Handler<Void>() {
            @Override
            public void handle(Void event) {
                handleConnectFailure(socket, String.format("Gateway client '%s' closed the connection before it could be routed.", socket.remoteAddress()));
            }
        });
        readStream.dataHandler(new Handler<Buffer>() {
            Buffer received = new Buffer();

            @Override
            public void handle(Buffer event) {
                received.appendBuffer(event);
                for (final Protocol protocol : protocols) {
                    if (protocol.matches(received)) {
                        if ("ssl".equals(protocol.getProtocolName())) {

                            LOG.info(String.format("SSL Connection from '%s'", socket.remoteAddress()));
                            String disabledCypherSuites=null;
                            String enabledCipherSuites=null;
                            if (sslConfig != null) {
                                disabledCypherSuites = sslConfig.getDisabledCypherSuites();
                                enabledCipherSuites = sslConfig.getEnabledCipherSuites();
                            }
                            if (sslContext == null) {
                                try {
                                    if (sslConfig != null) {
                                        sslContext = SSLContext.getInstance(sslConfig.getProtocol());
                                        sslContext.init(sslConfig.getKeyManagers(), sslConfig.getTrustManagers(), null);
                                    } else {
                                        sslContext = SSLContext.getDefault();
                                    }
                                } catch (Exception e) {
                                    handleConnectFailure(socket, "Could initialize SSL: " + e);
                                    return;
                                }
                            }

                            // lets wrap it up in a SslSocketWrapper.
                            SslSocketWrapper sslSocketWrapper = new SslSocketWrapper(socket);
                            sslSocketWrapper.putBackHeader(received);
                            sslSocketWrapper.initServer(sslContext, clientAuth, disabledCypherSuites, enabledCipherSuites);
                            DetectingGateway.this.handle(sslSocketWrapper);
                            return;

                        } else if ("http".equals(protocol.getProtocolName())) {
                            InetSocketAddress target = getHttpGateway();
                            if (target != null) {
                                try {
                                    URI url = new URI("http://" + target.getHostString() + ":" + target.getPort());
                                    LOG.info(String.format("Connecting '%s' to '%s:%d' using the http protocol",
                                            socket.remoteAddress(), url.getHost(), url.getPort()));
                                    ConnectionParameters params = new ConnectionParameters();
                                    params.protocol = "http";
                                    createClient(params, socket, url, received);
                                    return;
                                } catch (URISyntaxException e) {
                                    handleConnectFailure(socket, "Could not build valid connect URI: "+e);
                                    return;
                                }
                            } else {
                                handleConnectFailure(socket, "No http gateway available for the http protocol");
                                return;
                            }
                        } else {
                            protocol.snoopConnectionParameters(socket, received, new Handler<ConnectionParameters>() {
                                @Override
                                public void handle(ConnectionParameters connectionParameters) {
                                    // this will install a new dataHandler on the socket.
                                    if (connectionParameters.protocol == null)
                                        connectionParameters.protocol = protocol.getProtocolName();
                                    if (connectionParameters.protocolSchemes == null)
                                        connectionParameters.protocolSchemes = protocol.getProtocolSchemes();
                                    route(socket, connectionParameters, received);
                                }
                            });
                            return;
                        }
                    }
                }
                if (received.length() >= maxProtocolIdentificationLength) {
                    handleConnectFailure(socket, "Connection did not use one of the enabled protocols " + getProtocolNames());
                }
            }
        });
    }

    private void handleConnectFailure(SocketWrapper socket, String reason) {
        if( socketsConnecting.remove(socket) ) {
            if( reason!=null ) {
                LOG.info(reason);
            }
            failedConnectionAttempts.incrementAndGet();
            socket.close();
            shutdownTacker.release();
        }
    }

    public void route(final SocketWrapper socket, ConnectionParameters params, final Buffer received) {
        NetClient client = null;

        if( params.protocolVirtualHost==null ) {
            params.protocolVirtualHost = defaultVirtualHost;
        }
        HashSet<String> schemes = new HashSet<String>(Arrays.asList(params.protocolSchemes));
        if(params.protocolVirtualHost!=null) {
            List<ServiceDetails> services = serviceMap.getServices(params.protocolVirtualHost);

            // Lets try again with the defaultVirtualHost
            if( services.isEmpty() && !params.protocolVirtualHost.equals(defaultVirtualHost) ) {
                params.protocolVirtualHost = defaultVirtualHost;
                services = serviceMap.getServices(params.protocolVirtualHost);
            }

            LOG.debug(String.format("%d services match the virtual host", services.size()));
            if (!services.isEmpty()) {
                ClientRequestFacade clientRequestFacade = clientRequestFacadeFactory.create(socket, params);
                ServiceDetails serviceDetails = serviceLoadBalancer.choose(services, clientRequestFacade);
                if (serviceDetails != null) {
                    List<String> urlStrings = serviceDetails.getServices();
                    LOG.debug("Selected service exposes the following URLS: {}", urlStrings);
                    for (String urlString : urlStrings) {
                        if (Strings.notEmpty(urlString)) {
                            // lets create a client for this request...
                            try {
                                URI uri = new URI(urlString);
                                //URL url = new URL(urlString);
                                String urlProtocol = uri.getScheme();
                                if (schemes.contains(urlProtocol)) {
                                    if( !socket.remoteAddress().toString().equals(clientRequestFacade.getClientRequestKey())  ) {
                                        LOG.info(String.format("Connecting client from '%s' (with key '%s') requesting virtual host '%s' to '%s:%d' using the %s protocol",
                                            socket.remoteAddress(), clientRequestFacade.getClientRequestKey(), params.protocolVirtualHost, uri.getHost(), uri.getPort(), params.protocol
                                          ));
                                    } else {
                                        LOG.info(String.format("Connecting client from '%s' requesting virtual host '%s' to '%s:%d' using the %s protocol",
                                            socket.remoteAddress(), params.protocolVirtualHost, uri.getHost(), uri.getPort(), params.protocol
                                          ));
                                    }

                                    client = createClient(params, socket, uri, received);
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
            handleConnectFailure(socket, String.format("No endpoint available for virtual host '%s' and protocol %s", params.protocolVirtualHost, params.protocol));
        }
    }

    /**
     * Creates a new client for the given URL and handler
     */
    private NetClient createClient(final ConnectionParameters params, final SocketWrapper socketFromClient, final URI url, final Buffer received) {
        NetClient netClient = vertx.createNetClient();
        return netClient.connect(url.getPort(), url.getHost(), new Handler<AsyncResult<NetSocket>>() {
            public void handle(final AsyncResult<NetSocket> asyncSocket) {

                if( !asyncSocket.succeeded() ) {
                    handleConnectFailure(socketFromClient, String.format("Could not connect to '%s'", url));
                } else {
                    final NetSocket socketToServer = asyncSocket.result();

                    successfulConnectionAttempts.incrementAndGet();
                    socketsConnecting.remove(socketFromClient);
                    final ConnectedSocketInfo connectedInfo = new ConnectedSocketInfo(params, url, socketFromClient, socketToServer);
                    socketsConnected.add(connectedInfo);

                    Handler<Void> endHandler = new Handler<Void>() {
                        @Override
                        public void handle(Void event) {
                            handleShutdown(connectedInfo);
                        }
                    };
                    Handler<Throwable> exceptionHandler = new Handler<Throwable>() {
                        @Override
                        public void handle(Throwable event) {
                            handleShutdown(connectedInfo);
                        }
                    };
                    socketFromClient.readStream().endHandler(endHandler);
                    socketFromClient.readStream().exceptionHandler(exceptionHandler);
                    socketToServer.endHandler(endHandler);
                    socketToServer.exceptionHandler(exceptionHandler);

                    socketToServer.write(received);
                    Pump.createPump(socketToServer, socketFromClient.writeStream()).start();
                    Pump.createPump(socketFromClient.readStream(), socketToServer).start();
                }
            }
        });
    }

    private void handleShutdown(ConnectedSocketInfo connectedInfo) {
        if( socketsConnected.remove(connectedInfo) ) {
            connectedInfo.from.close();
            connectedInfo.to.close();
            shutdownTacker.release();
        }
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

    public SslConfig getSslConfig() {
        return sslConfig;
    }

    public void setSslConfig(SslConfig sslConfig) {
        this.sslConfig = sslConfig;
    }


    public long getReceivedConnectionAttempts() {
        return receivedConnectionAttempts.get();
    }
    public long getSuccessfulConnectionAttempts() {
        return successfulConnectionAttempts.get();
    }
    public long getFailedConnectionAttempts() {
        return failedConnectionAttempts.get();
    }

    public String[] getConnectingClients() {
        ArrayList<String> rc = new ArrayList<>();
        for (SocketWrapper socket : socketsConnecting) {
            rc.add(socket.remoteAddress().toString());
        }
        return rc.toArray(new String[rc.size()]);
    }

    public String[] getConnectedClients() {
        ArrayList<String> rc = new ArrayList<>();
        for (ConnectedSocketInfo info : socketsConnected) {
            rc.add(info.from.remoteAddress().toString());
        }
        return rc.toArray(new String[rc.size()]);
    }

    public long getConnectionTimeout() {
        return connectionTimeout;
    }

    public void setConnectionTimeout(long connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }
}
