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
package io.fabric8.gateway.handlers.detecting.protocol.ssl;

import io.fabric8.gateway.SocketWrapper;
import io.fabric8.gateway.handlers.detecting.Protocol;
import io.fabric8.gateway.handlers.loadbalancer.ConnectionParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.Handler;
import org.vertx.java.core.buffer.Buffer;

import static io.fabric8.gateway.handlers.detecting.protocol.Ascii.ascii;
import static io.fabric8.gateway.handlers.detecting.protocol.BufferSupport.startsWith;

/**
 */
public class SslProtocol implements Protocol {
    private static final transient Logger LOG = LoggerFactory.getLogger(SslProtocol.class);

    @Override
    public String getProtocolName() {
        return "ssl";
    }

    private static final String[] SCHEMES = new String[]{ "ssl" };

    @Override
    public String[] getProtocolSchemes() {
        return SCHEMES;
    }


    public int getMaxIdentificationLength() {
        return 6;
    }

    @Override
    public boolean matches(Buffer buffer) {
        if( buffer.length() >= 6 ) {
          if( buffer.getByte(0) == 0x16 ) { // content type
            return (buffer.getByte(5) == 1) && // Client Hello
            ( (buffer.getByte(1) == 2) // SSLv2
              || (
                buffer.getByte(1)== 3 &&
                isSSLVerions(buffer.getByte(2))
              )
            );
          } else {
            // We have variable header offset..
            return ((buffer.getByte(0) & 0xC0) == 0x80) && // The rest of byte 0 and 1 are holds the record length.
              (buffer.getByte(2) == 1) && // Client Hello
              ( (buffer.getByte(3) == 2) // SSLv2
                || (
                  (buffer.getByte(3) == 3) && // SSLv3 or TLS
                  isSSLVerions(buffer.getByte(4))
                )
              );
          }
        } else {
          return false;
        }
    }

    private boolean isSSLVerions(byte ver) {
        switch (ver) {  // Minor version
            case 0: // SSLv3
            case 1: // TLSv1
            case 2: // TLSv2
            case 3: // TLSv3
                return true;
            default:
                return false;
        }
    }

    @Override
    public void snoopConnectionParameters(final SocketWrapper socket, Buffer received, final Handler<ConnectionParameters> handler) {
        handler.handle(new ConnectionParameters());
    }

}
