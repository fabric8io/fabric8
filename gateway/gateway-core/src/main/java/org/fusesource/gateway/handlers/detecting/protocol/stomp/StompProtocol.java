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
package org.fusesource.gateway.handlers.detecting.protocol.stomp;

import org.fusesource.gateway.SocketWrapper;
import org.fusesource.gateway.loadbalancer.ConnectionParameters;
import org.fusesource.gateway.handlers.detecting.Protocol;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.Handler;
import org.vertx.java.core.buffer.Buffer;

import static org.fusesource.gateway.handlers.detecting.protocol.BufferSupport.startsWith;
import static org.fusesource.gateway.handlers.detecting.protocol.stomp.Constants.*;

/**
 */
public class StompProtocol implements Protocol {
    private static final transient Logger LOG = LoggerFactory.getLogger(StompProtocol.class);

    public static final int maxCommandLength = 20;
    public int maxHeaderLength = 1024 * 10;
    public int maxHeaders = 1000;
    public int maxDataLength = 1024 * 1024 * 100;

    @Override
    public String getProtocolName() {
        return "stomp";
    }

    public int getMaxIdentificationLength() {
        return 10;
    }

    @Override
    public boolean matches(Buffer header) {
        return startsWith(header, 0, CONNECT.toBuffer()) ||
               startsWith(header, 0, STOMP.toBuffer());
    }

    @Override
    public void snoopConnectionParameters(final SocketWrapper socket, Buffer received, final Handler<ConnectionParameters> handler) {

        StompProtocolDecoder h = new StompProtocolDecoder(this);
        h.errorHandler(new Handler<String>() {
            @Override
            public void handle(String error) {
                LOG.info("STOMP protocol decoding error: "+error);
                socket.close();
            }
        });
        h.codecHandler(new Handler<StompFrame>() {
            @Override
            public void handle(StompFrame event) {
                if( event.action().equals(CONNECT) || event.action().equals(STOMP)) {
                    ConnectionParameters parameters = new ConnectionParameters();
                    parameters.protocol = getProtocolName();
                    parameters.protocolVirtualHost = event.getHeaderAsString(HOST);
                    parameters.protocolUser = event.getHeaderAsString(USERID);
                    parameters.protocolClientId = event.getHeaderAsString(CLIENT_ID);
                    handler.handle(parameters);
                } else {
                    LOG.info("Expected a CONNECT or STOMP frame");
                    socket.close();
                }
            }
        });
        socket.readStream().dataHandler(h);
        h.handle(received);
    }

}
