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
package io.fabric8.gateway;

import org.vertx.java.core.http.WebSocketBase;
import org.vertx.java.core.net.NetSocket;
import org.vertx.java.core.streams.ReadStream;
import org.vertx.java.core.streams.WriteStream;

import java.net.InetSocketAddress;

/**
 * A socket wrapper helps abstract a difference between
 * different the different socket 'like' objects that Vert.x
 * supports.
 */
public abstract class SocketWrapper {

    abstract public ReadStream<ReadStream> readStream();
    abstract public WriteStream<WriteStream> writeStream();
    abstract public void close();
    abstract public Object stream();
    abstract public InetSocketAddress localAddress();
    abstract public InetSocketAddress remoteAddress();

    static public SocketWrapper wrap(NetSocket socket) {
        return new NetSocketWrapper(socket);
    }
    static public SocketWrapper wrap(WebSocketBase socket) {
        return new WebSocketWrapper(socket);
    }

    static class NetSocketWrapper extends SocketWrapper {
        private final NetSocket socket;

        public NetSocketWrapper(NetSocket socket) {
            this.socket = socket;
        }

        @Override
        public void close() {
            socket.close();
        }

        @Override
        public ReadStream readStream() {
            return socket;
        }

        @Override
        public WriteStream writeStream() {
            return socket;
        }

        @Override
        public Object stream() {
            return socket;
        }

        @Override
        public InetSocketAddress localAddress() {
            return socket.localAddress();
        }

        @Override
        public InetSocketAddress remoteAddress() {
            return socket.remoteAddress();
        }
    }

    static class WebSocketWrapper extends SocketWrapper {
        private final WebSocketBase socket;

        public WebSocketWrapper(WebSocketBase socket) {
            this.socket = socket;
        }

        @Override
        public void close() {
            socket.close();
        }

        @Override
        public ReadStream readStream() {
            return socket;
        }

        @Override
        public WriteStream writeStream() {
            return socket;
        }

        @Override
        public Object stream() {
            return socket;
        }

        @Override
        public InetSocketAddress localAddress() {
            return socket.localAddress();
        }

        @Override
        public InetSocketAddress remoteAddress() {
            return socket.remoteAddress();
        }

    }
}
