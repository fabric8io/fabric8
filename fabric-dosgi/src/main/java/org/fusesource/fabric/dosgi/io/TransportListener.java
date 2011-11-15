/**
 * Copyright (C) 2011, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.fabric.dosgi.io;

import java.io.IOException;


/**
 * An asynchronous listener of commands
 *
 */
public interface TransportListener {
    
    /**
     * called to process a command
     * @param command
     */
    void onTransportCommand(Transport transport, Object command);

    /**
     * transport can now accept more commands for transmission. 
     */
    void onRefill(Transport transport);

    /**
     * An unrecoverable exception has occured on the transport
     * @param error
     */
    void onTransportFailure(Transport transport, IOException error);
    
    /**
     * The transport has been connected.
     */
    public void onTransportConnected(Transport transport);

    /**
     * The transport has suffered a disconnection from
     * which it hopes to recover
     */
    public void onTransportDisconnected(Transport transport);

}
