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

import org.fusesource.fabric.apollo.amqp.codec.api.BareMessage;

/**
 * A message target on an AMQP link
 *
 * @author Stan Lewis
 *
 */
public interface Receiver extends Link {

    /**
     * Sets the message listener on this receiver
     * @param listener the message listener that will be notified when messages for this link arrive
     */
    public void setListener(MessageListener listener);

    /**
     * Returns whether or not flow control is currently enabled on this receiver
     * @return
     */
    public boolean isFlowControlEnabled();

    /**
     * Enables or disables flow control on this receiver
     * @param enable
     */
    /*
    public void enableFlowControl(boolean enable);
    */

    /**
     * Gets the current amount of available link credit for this receiver
     * @return
     */
    public Long getAvailableLinkCredit();

    /**
     * Add link credit to this receiver for the source
     * @param credit link credit to be added to whatever link credit may already be available
     */
    public void addLinkCredit(long credit);

    /**
     * Drains all link credit from the source, effectively stopping message flow from the source to this receiver
     */
    public void drainLinkCredit();

    /**
     * Gets the available message units for this receiver
     * @return
     */
    public long getAvailable();

    /**
     * Marks a message as settled with the specified outcome
     * @param message
     * @param outcome
     */
    void settle(BareMessage message, Outcome outcome);

}
