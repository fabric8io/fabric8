/**
 * Copyright (C) 2010-2011, FuseSource Corp.  All rights reserved.
 *
 *     http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */

package org.fusesource.fabric.apollo.amqp.api;

import org.fusesource.hawtdispatch.DispatchQueue;

/**
 *
 * Represents an AMQP Connection.
 *
 * @author Stan Lewis
 *
 */
public interface Connection {

    /**
     * Connects this connection to a peer
     * @param uri the URI of peer
     * @param onConnect The task to perform when the AMQP connection has been established or if an error occurs while establishing a connection
     */
    public void connect(String uri, Runnable onConnect);


    /**
     * Creates a new session on this connection.
     * @return a new un-established instance of a {@link Session}
     */
    public Session createSession();

    /**
     * Returns whether or not this Connection is connected
     * @return boolean
     */
    public boolean connected();

    /**
     * Gets the last error received on this Connection
     * @return {@link java.lang.Throwable}
     */
    public Throwable error();

    /**
     * Gets the dispatch queue used by this Connection
     * @return {@link org.fusesource.hawtdispatch.DispatchQueue}
     */
    public DispatchQueue getDispatchQueue();

    /**
     * Sets the container ID to be used by this Connection
     * @param id
     */
    public void setContainerId(String id);

    /**
     * Closes this connection gracefully.
     */
    public void close();

    /**
     * Closes this connection ungracefully
     * @param t the exception causing the connection to be closed
     */
    public void close(Throwable t);

    /**
     * Closes this connection ungracefully
     * @param reason the reason the connection is being closed
     */
    public void close(String reason);

    /**
     * Sets the task to run when this connection is closed
     * @param onClose
     */
    public void setOnClose(Runnable onClose);

    /**
     * Sets a {@link SessionListener} on this connection
     * @param listener the listener to be called when a session is created or destroyed on this connection
     */
    public void setSessionListener(SessionListener listener);

    /**
     * Gets the container ID of this Connection's peer
     * @return {@link java.lang.String}}
     */
    public String getPeerContainerId();

}
