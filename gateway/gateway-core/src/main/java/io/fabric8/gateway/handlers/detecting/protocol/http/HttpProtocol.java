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
package io.fabric8.gateway.handlers.detecting.protocol.http;

import io.fabric8.gateway.handlers.detecting.Protocol;
import io.fabric8.gateway.SocketWrapper;
import io.fabric8.gateway.handlers.detecting.protocol.Ascii;
import io.fabric8.gateway.handlers.loadbalancer.ConnectionParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.Handler;
import org.vertx.java.core.buffer.Buffer;

import static io.fabric8.gateway.handlers.detecting.protocol.Ascii.ascii;
import static io.fabric8.gateway.handlers.detecting.protocol.BufferSupport.startsWith;

/**
 */
public class HttpProtocol implements Protocol {
    private static final transient Logger LOG = LoggerFactory.getLogger(HttpProtocol.class);

    final Ascii CONNECT = ascii("CONNECT ");
    final Ascii GET = ascii("GET ");
    final Ascii PUT = ascii("PUT ");
    final Ascii POST = ascii("POST ");
    final Ascii DELETE = ascii("DELETE ");
    final Ascii OPTIONS = ascii("OPTIONS ");
    final Ascii HEAD = ascii("HEAD ");
    final Ascii TRACE = ascii("TRACE ");

    @Override
    public String getProtocolName() {
        return "http";
    }

    private static final String[] SCHEMES = new String[]{ "http" };

    @Override
    public String[] getProtocolSchemes() {
        return SCHEMES;
    }


    public int getMaxIdentificationLength() {
        return CONNECT.toBuffer().length();
    }

    @Override
    public boolean matches(Buffer header) {
        return
            startsWith(header, 0, GET.toBuffer()) ||
            startsWith(header, 0, HEAD.toBuffer()) ||
            startsWith(header, 0, POST.toBuffer()) ||
            startsWith(header, 0, PUT.toBuffer()) ||
            startsWith(header, 0, DELETE.toBuffer()) ||
            startsWith(header, 0, OPTIONS.toBuffer()) ||
            startsWith(header, 0, TRACE.toBuffer()) ||
            startsWith(header, 0, CONNECT.toBuffer());
    }

    @Override
    public void snoopConnectionParameters(final SocketWrapper socket, Buffer received, final Handler<ConnectionParameters> handler) {
        handler.handle(new ConnectionParameters());
    }

}
