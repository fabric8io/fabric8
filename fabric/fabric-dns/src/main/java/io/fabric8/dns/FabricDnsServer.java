/*
 * Copyright 2005-2014 Red Hat, Inc.
 *
 * Red Hat licenses this file to you under the Apache License, version
 * 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */

package io.fabric8.dns;

import io.fabric8.api.FabricService;
import io.fabric8.api.scr.AbstractComponent;
import io.fabric8.api.scr.ValidatingReference;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.AsyncResultHandler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.datagram.DatagramSocket;
import org.vertx.java.core.datagram.InternetProtocolFamily;
import org.vertx.java.core.impl.DefaultVertx;

@Component(configurationPid = "io.fabric8.dns.server", policy = ConfigurationPolicy.OPTIONAL, immediate = true, metatype = true)
public class FabricDnsServer extends AbstractComponent {

    private static final Logger LOGGER = LoggerFactory.getLogger(FabricDnsServer.class);
    private static final int DEFAULT_DNS_SERVER_PORT = 8053;

    @Property(name = "bindAddress", label = "DNS Server bind address", description = "The address that the dns server will bind on", value = "${bind.address}")
    private String bindAddress = "0.0.0.0";

    @Property(name = "port", label = "DNS Server port", description = "The port that the dns server will bind on", value = "${dns.server.port}")
    private int port = DEFAULT_DNS_SERVER_PORT;

    @Reference(referenceInterface = FabricService.class)
    private ValidatingReference<FabricService> fabricService = new ValidatingReference<>();

    @Reference(referenceInterface = FabricZoneManager.class)
    private ValidatingReference<FabricZoneManager> fabricZoneManager = new ValidatingReference<>();

    private final Vertx vertx = new DefaultVertx();
    private DatagramSocket socket;

    @Activate
    void activate() {
        socket = vertx.createDatagramSocket(InternetProtocolFamily.IPv4);
        socket.listen(bindAddress, port, new AsyncResultHandler<DatagramSocket>() {
                    @Override
                    public void handle(AsyncResult<DatagramSocket> event) {
                       socket.dataHandler(new DnsDatagramPacketHandler(fabricZoneManager.get(), socket));
                    }
                }
        );
    }

    @Deactivate
    void deactivate() {
        socket.close();
    }

    void bindFabricService(FabricService service) {
        this.fabricService.bind(service);
    }

    void unbindFabricService(FabricService service) {
        this.fabricService.unbind(service);
    }

    void bindFabricZoneManager(FabricZoneManager service) {
        this.fabricZoneManager.bind(service);
    }

    void unbindFabricZoneManager(FabricZoneManager service) {
        this.fabricZoneManager.unbind(service);
    }
}
