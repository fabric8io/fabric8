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

import org.fusesource.common.util.Strings;
import org.fusesource.gateway.ServiceDetails;
import org.fusesource.gateway.ServiceMap;
import org.fusesource.gateway.chooser.Chooser;
import org.fusesource.gateway.chooser.DefaultNetChooser;
import org.fusesource.gateway.chooser.NetChooser;
import org.fusesource.gateway.chooser.RandomChooser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.net.NetClient;
import org.vertx.java.core.net.NetSocket;
import org.vertx.java.core.streams.Pump;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

/**
 */
public class TcpGatewayHandler implements Handler<NetSocket> {
    private static final transient Logger LOG = LoggerFactory.getLogger(TcpGatewayHandler.class);

    private final Vertx vertx;
    private final ServiceMap serviceMap;
    private Chooser<String> pathChooser = new RandomChooser<String>();
    private NetChooser serviceChooser = new DefaultNetChooser();

    public TcpGatewayHandler(Vertx vertx, ServiceMap serviceMap) {
        this.vertx = vertx;
        this.serviceMap = serviceMap;
    }

    @Override
    public void handle(final NetSocket socket) {
        LOG.info("Proxying socket from: " + socket);

        NetClient client = null;
        List<String> paths = serviceMap.getPaths();
        String path = pathChooser.choose(paths);
        if (path != null) {
            List<ServiceDetails> services = serviceMap.getServices(path);
            if (!services.isEmpty()) {
                ServiceDetails serviceDetails = serviceChooser.chooseService(socket, services);
                if (serviceDetails != null) {
                    List<String> urlStrings = serviceDetails.getServices();
                    if (urlStrings.size() > 0) {
                        String urlText = urlStrings.get(0);
                        if (Strings.notEmpty(urlText)) {
                            // lets create a client for this request...
                            try {
                                URL url = new URL(urlText);
                                Handler<AsyncResult<NetSocket>> handler = new Handler<AsyncResult<NetSocket>>() {
                                    public void handle(final AsyncResult<NetSocket> asyncSocket) {
                                        NetSocket clientSocket = asyncSocket.result();
                                        Pump.createPump(clientSocket, socket).start();
                                        Pump.createPump(socket, clientSocket).start();
                                    }
                                };
                                client = createClient(url, handler);
                            } catch (MalformedURLException e) {
                                LOG.warn("Failed to parse URL: " + urlText + ". " + e, e);
                            }
                        }
                    }
                }
            }
        }
        if (client == null) {
            // fail to route
            socket.close();
        }
    }

    /**
     * Creates a new client for the given URL and handler
     */
    protected NetClient createClient(URL url, Handler<AsyncResult<NetSocket>> handler) throws MalformedURLException {
        NetClient client = vertx.createNetClient();
        return client.connect(url.getPort(), url.getHost(), handler);

    }

}
