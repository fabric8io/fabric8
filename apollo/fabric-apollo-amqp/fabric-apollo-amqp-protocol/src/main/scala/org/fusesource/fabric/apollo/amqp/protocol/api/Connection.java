/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
