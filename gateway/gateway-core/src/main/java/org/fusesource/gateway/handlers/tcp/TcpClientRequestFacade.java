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
package org.fusesource.gateway.handlers.tcp;

import org.fusesource.gateway.loadbalancer.ClientRequestFacade;
import org.vertx.java.core.net.NetSocket;

/**
 */
public class TcpClientRequestFacade implements ClientRequestFacade {
    private final NetSocket socket;

    public TcpClientRequestFacade(NetSocket socket) {
        this.socket = socket;
    }

    @Override
    public String getClientRequestKey() {
        return socket.localAddress().toString();
    }
}
