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

/**
 * A representation of an AMQP Session
 *
 * @author Stan Lewis
 */
public interface Session {

    /**
     * Sets the outgoing window size for this session
     *
     * @param window
     */
    public void setOutgoingWindow(long window);

    /**
     * Sets the incoming window size for this session
     *
     * @param window
     */
    public void setIncomingWindow(long window);

    /**
     * Gets the outgoing window size for this session
     *
     * @return
     */
    public long getOutgoingWindow();

    /**
     * Gets the incoming window size for this session
     *
     * @return
     */
    public long getIncomingWindow();

    public void attach(Link link);

    public void detach(Link link);

    public void detach(Link link, String reason);

    public void detach(Link link, Throwable t);

    /**
     * Returns whether or not there is sufficient session credit to send based on the remote peer's incoming window
     *
     * @return
     */
    public boolean sufficientSessionCredit();

    /**
     * Starts this session
     *
     * @param onBegin task to be performed when the peer ackowledges the session has been started
     */
    public void begin(Runnable onBegin);

    /**
     * Returns the connection that created this session
     *
     * @return
     */
    public Connection getConnection();

    /**
     * Returns whether or not this session is associated with a connection and attached to a peer session
     *
     * @return
     */
    public boolean established();

    /**
     * Sets a LinkListener on this session that will be notified when links are attached or detached
     *
     * @param handler the listener to be called
     */
    public void setLinkHandler(LinkHandler handler);

    public void setOnEnd(Runnable task);

    /**
     * Ends the session gracefully
     */
    public void end();

    /**
     * Ends the session ungracefully
     *
     * @param t exception indicating the reason for session termination
     */
    public void end(Throwable t);

    /**
     * Ends the session ungracefully
     *
     * @param reason the reason for session termination
     */
    public void end(String reason);

}
