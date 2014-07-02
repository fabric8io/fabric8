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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.Handler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.datagram.DatagramPacket;
import org.vertx.java.core.datagram.DatagramSocket;
import org.xbill.DNS.Message;

import java.io.IOException;

public class DnsDatagramPacketHandler implements Handler<DatagramPacket> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DnsDatagramPacketHandler.class);

    private final FabricZoneManager fabricZoneManager;
    private final DatagramSocket socket;

    DnsDatagramPacketHandler(FabricZoneManager fabricZoneManager, DatagramSocket socket) {
        this.fabricZoneManager = fabricZoneManager;
        this.socket = socket;
    }


    @Override
    public void handle(DatagramPacket packet) {
        byte[] data = packet.data().getBytes();
         try {
            Message request = new Message(packet.data().getBytes());
            LOGGER.trace("Received Request {}.", request);
            byte[] response = fabricZoneManager.generateReply(request, data, data.length, null);
            socket.send(new Buffer(response), packet.sender().getHostName(), packet.sender().getPort(), new Handler<AsyncResult<DatagramSocket>>() {
                     @Override
                     public void handle(AsyncResult<DatagramSocket> event) {
                         LOGGER.trace("Event {}", event);
                     }
                 });
        } catch (IOException e) {
            LOGGER.error("Failed to read message.", e);
        }
    }
}
