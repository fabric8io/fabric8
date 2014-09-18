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
package io.fabric8.gateway.servlet.support;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;

import org.apache.commons.httpclient.HttpConnection;
import org.apache.commons.httpclient.params.HttpConnectionParams;
import org.apache.commons.httpclient.protocol.DefaultProtocolSocketFactory;
import org.apache.commons.httpclient.protocol.ProtocolSocketFactory;

/**
 * A @{link ProtocolSocketFactory} implementation that does not bind a socket.
 * <p>
 *
 * The reason for this {@link ProtocolSocketFactory} implementation is that for ProxyServlet we don't need to
 * bind to a local address and port. This binding can cause issue in PaaS environments where restrictions apply.
 * <p>
 *
 * When {@link HttpConnection#open()} calls
 * {@link ProtocolSocketFactory#createSocket(String, int, InetAddress, int, HttpConnectionParams)}
 * which by default will use {@link DefaultProtocolSocketFactory#createSocket(String, int, InetAddress, int, HttpConnectionParams)}.
 * The method that will be invoke is {@link DefaultProtocolSocketFactory#createSocket(String, int, InetAddress, int, HttpConnectionParams)}
 * which will call {@code return new Socket(host, port, localAddress, localPort);}.
 * The {@link Socket#Socket(InetAddress, int, InetAddress, int)} constructor will create a new Socket and connect to the
 * remote address and port (the first two arguments). It will also bind() to the local address and port. This is
 * the part that can cause issue in PaaS environment where there are restrictions.
 *
 */
public class NonBindingSocketFactory implements ProtocolSocketFactory {

    @Override
    public Socket createSocket(String host, int port, InetAddress localAddress, int localPort) throws IOException {
        return createNonBindingSocket(host, port);
    }

    @Override
    public Socket createSocket(String host, int port, InetAddress localAddress, int localPort, HttpConnectionParams params) throws IOException {
        return createNonBindingSocket(host, port);
    }

    @Override
    public Socket createSocket(String host, int port) throws IOException {
        return createNonBindingSocket(host, port);
    }

    private static Socket createNonBindingSocket(String host, int port) throws IOException {
        return new Socket(host, port);
    }

}
