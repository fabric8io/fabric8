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
package io.fabric8.gateway.handlers.detecting.protocol.openwire;

import io.fabric8.gateway.handlers.detecting.Protocol;
import io.fabric8.gateway.handlers.detecting.protocol.openwire.command.Command;
import io.fabric8.gateway.handlers.detecting.protocol.openwire.command.WireFormatInfo;
import io.fabric8.gateway.handlers.loadbalancer.ConnectionParameters;
import io.fabric8.gateway.SocketWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.Handler;
import org.vertx.java.core.buffer.Buffer;

import java.io.IOException;

import static io.fabric8.gateway.handlers.detecting.protocol.BufferSupport.indexOf;

/**
 */
public class OpenwireProtocol implements Protocol {
    private static final transient Logger LOG = LoggerFactory.getLogger(OpenwireProtocol.class);

    public static Buffer MAGIC = new Buffer(new byte[]{'A', 'c', 't', 'i', 'v', 'e', 'M', 'Q'});

    public int maxFrameSize = 1024 * 1024 * 100;

    @Override
    public String getProtocolName() {
        return "openwire";
    }

    private static final String[] SCHEMES = new String[]{ "tcp", "nio" };

    @Override
    public String[] getProtocolSchemes() {
        return SCHEMES;
    }


    public int getMaxIdentificationLength() {
        return 5+MAGIC.length();
    }

    @Override
    public boolean matches(Buffer buffer) {
        return buffer.length() >= 4 + MAGIC.length() && indexOf(buffer, 5, MAGIC) >= 0;
    }

    @Override
    public void snoopConnectionParameters(final SocketWrapper socket, Buffer received, final Handler<ConnectionParameters> handler) {

        OpenwireProtocolDecoder h = new OpenwireProtocolDecoder(this);
        h.errorHandler(new Handler<String>() {
            @Override
            public void handle(String error) {
                LOG.info("Openwire protocol decoding error: "+error);
                socket.close();
            }
        });
        h.codecHandler(new Handler<Command>() {
            @Override
            public void handle(Command event) {
                if( event instanceof WireFormatInfo){
                    WireFormatInfo info = (WireFormatInfo) event;
                    ConnectionParameters parameters = new ConnectionParameters();
                    try {
                        parameters.protocolVirtualHost = info.getHost();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    handler.handle(parameters);
                } else {
                    LOG.info("Expected a WireFormatInfo frame");
                    socket.close();
                }
            }
        });
        socket.readStream().dataHandler(h);
        h.handle(received);
    }

}
