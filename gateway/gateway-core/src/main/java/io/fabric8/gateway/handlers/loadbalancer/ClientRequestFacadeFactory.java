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
package io.fabric8.gateway.handlers.loadbalancer;

import io.fabric8.gateway.SocketWrapper;
import io.fabric8.gateway.loadbalancer.ClientRequestFacade;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

/**
 * A factory that creates ClientRequestFacade instances which extract
 * values from a NetSocket and ConnectionParameters based on
 * the factory is configured.
 */
public class ClientRequestFacadeFactory {

    private final ArrayList<KeyExtractor> keyExtractors;

    interface KeyExtractor {
        String extract(SocketWrapper socket, ConnectionParameters connectionParameters);
    }

    static enum KeyExtractors implements KeyExtractor {
        REMOTE_ADDRESS {
            @Override
            public String extract(SocketWrapper socket, ConnectionParameters connectionParameters) {
                if( socket!=null ) {
                    InetSocketAddress address = socket.remoteAddress();
                    if( address!=null ) {
                        return address.toString();
                    }
                }
                return null;
            }
        },

        LOCAL_ADDRESS {
            @Override
            public String extract(SocketWrapper socket, ConnectionParameters connectionParameters) {
                if( socket!=null ) {
                    InetSocketAddress address = socket.localAddress();
                    if( address!=null ) {
                        return address.toString();
                    }
                }
                return null;
            }
        },

        PROTOCOL_VIRTUAL_HOST {
            @Override
            public String extract(SocketWrapper socket, ConnectionParameters connectionParameters) {
                if( connectionParameters!=null ) {
                    return connectionParameters.protocolVirtualHost;
                }
                return null;
            }
        },

        PROTOCOL_USER {
            @Override
            public String extract(SocketWrapper socket, ConnectionParameters connectionParameters) {
                if( connectionParameters!=null ) {
                    return connectionParameters.protocolUser;
                }
                return null;
            }
        },

        PROTOCOL_CLIENT_ID {
            @Override
            public String extract(SocketWrapper socket, ConnectionParameters connectionParameters) {
                if( connectionParameters!=null ) {
                    return connectionParameters.protocolClientId;
                }
                return null;
            }
        },

        PROTOCOL_SESSION_ID {
            @Override
            public String extract(SocketWrapper socket, ConnectionParameters connectionParameters) {
                if( connectionParameters!=null ) {
                    return connectionParameters.protocolSessionId;
                }
                return null;
            }
        };
    }


    public ClientRequestFacadeFactory(String extractors) {
        this(toExtractors(extractors));
    }

    private static ArrayList<KeyExtractor> toExtractors(String extractors) {
        String[] parts = extractors.split("[\\s,]+");
        ArrayList<KeyExtractor> rc  = new ArrayList<KeyExtractor>(parts.length);
        for (String part : parts) {
            rc.add(KeyExtractors.valueOf(part.toUpperCase()));
        }
        return rc;
    }

    public ClientRequestFacadeFactory(KeyExtractor...extractors) {
        this(Arrays.asList(extractors));
    }
    public ClientRequestFacadeFactory(Collection<KeyExtractor> extractors) {
        this.keyExtractors =  new ArrayList<KeyExtractor>(extractors);
    }

    public ClientRequestFacade create(final SocketWrapper socket, final ConnectionParameters params) {
        return new ClientRequestFacade() {
            @Override
            public String getClientRequestKey() {
                for (KeyExtractor extractor : keyExtractors) {
                    String value = extractor.extract(socket, params);
                    if( value!=null ) {
                        return value;
                    }
                }
                return null;
            }
        };
    }
}
