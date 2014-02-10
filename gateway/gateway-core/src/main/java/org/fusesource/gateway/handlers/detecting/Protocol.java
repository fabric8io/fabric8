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
package org.fusesource.gateway.handlers.detecting;

import org.fusesource.gateway.loadbalancer.ConnectionParameters;
import org.vertx.java.core.Handler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.net.NetSocket;

/**
 * An implementation of interface is required for each protocol that you need the DetectingGateway to support.
 */
public interface Protocol {

    public String getProtocolName();
    public int getMaxIdentificationLength();
    public boolean matches(Buffer buffer);
    public void snoopConnectionParameters(NetSocket socket, Buffer received, Handler<ConnectionParameters> handler);

}
