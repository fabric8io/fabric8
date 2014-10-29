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
package io.fabric8.gateway.handlers.detecting.protocol.amqp;

import io.fabric8.gateway.SocketWrapper;
import io.fabric8.gateway.handlers.detecting.Protocol;
import io.fabric8.gateway.handlers.loadbalancer.ConnectionParameters;
import org.apache.qpid.proton.engine.*;
import org.apache.qpid.proton.engine.impl.EngineFactoryImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.Handler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.net.NetSocket;

import java.lang.reflect.Field;

import static io.fabric8.gateway.handlers.detecting.protocol.BufferSupport.*;

/**
 */
public class AmqpProtocol implements Protocol {
    private static final transient Logger LOG = LoggerFactory.getLogger(AmqpProtocol.class);

    static final Buffer PROTOCOL_MAGIC = new Buffer(new byte []{ 'A', 'M', 'Q', 'P' });
    int maxFrameSize = 1024*1024*100;

    @Override
    public String getProtocolName() {
        return "amqp";
    }

    private static final String[] SCHEMES = new String[]{ "amqp", "amqp+nio" };

    @Override
    public String[] getProtocolSchemes() {
        return SCHEMES;
    }

    public int getMaxIdentificationLength() {
        return PROTOCOL_MAGIC.length();
    }

    @Override
    public boolean matches(Buffer header) {
      if (header.length() < PROTOCOL_MAGIC.length()) {
        return false;
      } else {
        return startsWith(header ,PROTOCOL_MAGIC);
      }
    }

    @Override
    public void snoopConnectionParameters(final SocketWrapper socket, final Buffer received, final Handler<ConnectionParameters> handler) {

        // We can't yet snoop the virtual host info from a AMQP connection..
        final AmqpProtocolDecoder h = new AmqpProtocolDecoder(this);
        handler.handle(new ConnectionParameters());

    }

    public void experimentalSnoopConnectionParameters(final NetSocket socket, final Buffer received, final Handler<ConnectionParameters> handler) {

        final AmqpProtocolDecoder h = new AmqpProtocolDecoder(this);
        final ConnectionParameters parameters = new ConnectionParameters();
        h.errorHandler(new Handler<String>() {
            @Override
            public void handle(String error) {
                LOG.info("STOMP protocol decoding error: "+error);
                socket.close();
            }
        });
        h.codecHandler(new Handler<AmqpEvent>() {

            EngineFactory engineFactory = new EngineFactoryImpl();
            Transport protonTransport = engineFactory.createTransport();
            Connection protonConnection = engineFactory.createConnection();
            Sasl sasl;


            @Override
            public void handle(AmqpEvent event) {
                switch( event.type ) {
                    case HEADER:

                        AmqpHeader header = (AmqpHeader) event.decodedFrame;
                        switch (header.getProtocolId()) {
                            case 0:
                                // amqpTransport.sendToAmqp(new AmqpHeader());
                                break; // nothing to do..
                            case 3:
                                // Client will be using SASL for auth..
                                sasl = protonTransport.sasl();
                                // sasl.setMechanisms(new String[] { "ANONYMOUS", "PLAIN" });
                                sasl.server();
                                break;
                            default:
                        }

                        processEvent(event);

                        // Les send back the AMQP response headers so that the client
                        // can send us the SASL init or AMQP open frames.
                        Buffer buffer = toBuffer(protonTransport.getOutputBuffer());
                        protonTransport.outputConsumed();
                        socket.write(buffer);

                        break;

                    default:
                        processEvent(event);
                }
            }

            private void processEvent(AmqpEvent event) {
                byte[] buffer = event.encodedFrame.getBytes();
                int offset = 0;
                int remaining = buffer.length;
                while( remaining>0 ) {

                    try {
                        int count = protonTransport.input(buffer, offset, remaining);
                        offset += count;
                        remaining -= count;
                    } catch (Throwable e) {
                        LOG.info("Could not decode AMQP frame: " + e, e);
                        socket.close();
                        return;
                    }


                    if (sasl != null) {

                        // Connection is using SASL, get the host name from the SASL init frame.
                        // TODO: add timeout in case the client is waiting for SASL negotiation
                        if (sasl.getRemoteMechanisms().length > 0) {
                            parameters.protocolVirtualHost = getHostname(sasl);

                            if ("PLAIN".equals(sasl.getRemoteMechanisms()[0])) {
                                byte[] data = new byte[sasl.pending()];
                                sasl.recv(data, 0, data.length);
                                Buffer[] parts = split(new Buffer(data), (byte)0);
                                if (parts.length > 0) {
                                    parameters.protocolUser = parts[0].toString();
                                }

                                // We are done!
                                handler.handle(parameters);
                            }

                        }
                    }

                    if (protonConnection.getLocalState() == EndpointState.UNINITIALIZED && protonConnection.getRemoteState() != EndpointState.UNINITIALIZED) {

                        // If we get here them the connection was not using SASL.. we can get the hostname
                        // info from the open frame.

                        parameters.protocolVirtualHost = protonConnection.getRemoteHostname();
                        // We are done!
                        handler.handle(parameters);
                    }

                }
            }
        });

        socket.dataHandler(h);
        h.handle(received);
    }

    static private String getHostname(Sasl sasl) {
        try {
            Field hostname = sasl.getClass().getDeclaredField("_hostname");
            hostname.setAccessible(true);
            return (String) hostname.get(sasl);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

}
