/*
 * Copyright (C) 2010-2011, FuseSource Corp.  All rights reserved
 *
 *    http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license, a copy of which has been included with this distribution
 * in the license.txt file
 */

package org.fusesource.fabric.apollo.amqp.protocol.api;

import org.fusesource.hawtdispatch.DispatchQueue;

/**
 * Represents an AMQP Connection.
 *
 * TODO - Add more getters/setters for fields in Open frame
 *
 * @author Stan Lewis
 */
public interface Connection {

    public void setIdleTimeout(long timeout);

    public long getIdleTimeout();

    /**
     * Connects this connection to a peer
     *
     * @param uri the URI of peer
     */
    public void connect(String uri);

    public void onConnected(Runnable task);

    public void onDisconnected(Runnable task);

    /**
     * Creates a new session on this connection.
     *
     * @return a new un-established instance of a {@link Session}
     */
    public Session createSession();

    /**
     * Returns whether or not this Connection is connected
     *
     * @return boolean
     */
    public boolean isConnected();

    /**
     * Gets the last error received on this Connection
     *
     * @return {@link Throwable}
     */
    public Throwable error();

    /**
     * Gets the dispatch queue used by this Connection
     *
     * @return {@link org.fusesource.hawtdispatch.DispatchQueue}
     */
    public DispatchQueue getDispatchQueue();

    /**
     * Sets the container ID to be used by this Connection
     *
     * @param id
     */
    public void setContainerID(String id);

    public String getContainerID();

    /**
     * Closes this connection gracefully.
     */
    public void close();

    /**
     * Closes this connection ungracefully
     *
     * @param t the exception causing the connection to be closed
     */
    public void close(Throwable t);

    /**
     * Closes this connection ungracefully
     *
     * @param reason the reason the connection is being closed
     */
    public void close(String reason);

    public void setSessionHandler(SessionHandler handler);

    /**
     * Gets the container ID of this Connection's peer
     *
     * @return {@link String}}
     */
    public String getPeerContainerID();

}
