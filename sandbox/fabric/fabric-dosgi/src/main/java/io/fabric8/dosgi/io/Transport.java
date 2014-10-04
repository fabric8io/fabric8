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
package io.fabric8.dosgi.io;

import io.fabric8.dosgi.api.Dispatched;
import org.fusesource.hawtdispatch.DispatchQueue;

/**
 * Represents an abstract connection.  It can be a client side or server side connection.
 * 
 */
public interface Transport extends Service, Dispatched {


    boolean full();

    /**
     * A one way asynchronous send of a command.  Only sent if the the transport is not full.
     * 
     * @param command
     * @return true if the command was accepted.
     */
    boolean offer(Object command);

    /**
     * Returns the current transport listener
     *
     * @return
     */
    TransportListener getTransportListener();

    /**
     * Registers an inbound command listener
     *
     * @param commandListener
     */
    void setTransportListener(TransportListener commandListener);

    /**
     * Sets the dispatch queue used by the transport
     *
     * @param queue
     */
    void setDispatchQueue(DispatchQueue queue);

    /**
     * suspend delivery of commands.
     */
    void suspendRead();

    /**
     * resume delivery of commands.
     */
    void resumeRead();

    /**
     * @return the remote address for this connection
     */
    String getRemoteAddress();

    /**
     * @return true if the transport is disposed
     */
    boolean isDisposed();
    
    /**
     * @return true if the transport is connected
     */
    boolean isConnected();
    
    /**
     * @return The protocol codec for the transport.
     */
    ProtocolCodec getProtocolCodec();

    /**
     * Sets the protocol codec for the transport
     * @param protocolCodec
     */
    void setProtocolCodec(ProtocolCodec protocolCodec);

}
