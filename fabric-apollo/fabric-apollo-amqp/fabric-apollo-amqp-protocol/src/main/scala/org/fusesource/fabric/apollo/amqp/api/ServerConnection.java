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

/**
 * Represents an AMQP server that listens for incoming client connections
 *
 * @author Stan Lewis
 *
 */
public interface ServerConnection {

    /**
     * Sets the container ID for this ServerConnection and all connections created by it
     * @param id
     */
    public void setContainerId(String id);

    /**
     * Binds this ServerConnection to the specified URI
     * @param uri
     */
    public void bind(String uri);

    /**
     * Gets the port this ServerConnection is bound to
     * @return
     */
    public int getListenPort();

    /**
     * Gets the hostname this ServerConnection is bound to
     * @return
     */
    public String getListenHost();
}
