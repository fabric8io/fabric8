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
package io.fabric8.gateway.handlers.detecting;

import io.fabric8.gateway.SocketWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.Handler;
import org.vertx.java.core.http.ServerWebSocket;

import java.util.concurrent.atomic.AtomicReference;

/**
 */
public class DetectingGatewayWebSocketHandler implements Handler<ServerWebSocket> {

    private static final transient Logger LOG = LoggerFactory.getLogger(DetectingGatewayWebSocketHandler.class);
    private final AtomicReference<DetectingGateway> gateway = new AtomicReference<DetectingGateway>();
    private String pathPrefix;

    @Override
    public void handle(final ServerWebSocket socket) {
        DetectingGateway handler = this.gateway.get();
        if ( handler==null ) {
            LOG.info("Rejecting web socket: no protocol detecting gateway deployed");
            socket.reject();
            return;
        } else if ( pathPrefix!=null && !socket.path().startsWith(pathPrefix) ) {
            LOG.info("Rejecting web socket: request path does not start with:"+ pathPrefix);
            socket.reject();
            return;
        }
        LOG.info("Processing the web socket '"+socket.remoteAddress()+"' with the protocol detecting gateway");
        handler.handle(SocketWrapper.wrap(socket));
    }

    public DetectingGateway getGateway() {
        return gateway.get();
    }
    public void setGateway(DetectingGateway value) {
        gateway.set(value);
    }

    public String getPathPrefix() {
        return pathPrefix;
    }

    public void setPathPrefix(String pathPrefix) {
        this.pathPrefix = pathPrefix;
    }
}
