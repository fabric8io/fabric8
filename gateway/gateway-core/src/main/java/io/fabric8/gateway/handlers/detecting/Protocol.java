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
import io.fabric8.gateway.handlers.loadbalancer.ConnectionParameters;
import org.vertx.java.core.Handler;
import org.vertx.java.core.buffer.Buffer;

/**
 * An implementation of interface is required for each protocol that you need the DetectingGateway to support.
 */
public interface Protocol {

    public String[] getProtocolSchemes();
    public String getProtocolName();
    public int getMaxIdentificationLength();
    public boolean matches(Buffer buffer);
    public void snoopConnectionParameters(final SocketWrapper socket, Buffer received, Handler<ConnectionParameters> handler);

}
