/**
 * Copyright (C) 2011, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.fabric.dosgi.io;

import org.fusesource.fabric.dosgi.api.Dispatched;
import org.fusesource.hawtdispatch.DispatchQueue;

/**
 * Represents an abstract connection.  It can be a client side or server side connection.
 * 
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
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
