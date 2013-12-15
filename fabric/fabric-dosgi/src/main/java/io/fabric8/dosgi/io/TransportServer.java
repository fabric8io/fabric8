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
package io.fabric8.dosgi.io;

import java.net.InetSocketAddress;

import org.fusesource.hawtdispatch.DispatchQueue;

/**
 * A TransportServer asynchronously accepts {@see Transport} objects and then
 * delivers those objects to a {@see TransportAcceptListener}.
 * 
 * @version $Revision: 1.4 $
 */
public interface TransportServer extends Service {

    /**
     * Registers an {@see TransportAcceptListener} which is notified of accepted
     * channels.
     * 
     * @param acceptListener
     */
    void setAcceptListener(TransportAcceptListener acceptListener);

    String getBoundAddress();

    String getConnectAddress();

    /**
     * @return The socket address that this transport is accepting connections
     *         on or null if this does not or is not currently accepting
     *         connections on a socket.
     */
    InetSocketAddress getSocketAddress();

    /**
     * Returns the dispatch queue used by the transport
     *
     * @return
     */
    DispatchQueue getDispatchQueue();

    /**
     * Sets the dispatch queue used by the transport
     *
     * @param queue
     */
    void setDispatchQueue(DispatchQueue queue);

    /**
     * suspend accepting new transports
     */
    void suspend();

    /**
     * resume accepting new transports
     */
    void resume();

}
