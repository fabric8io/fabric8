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

import org.fusesource.fabric.apollo.amqp.codec.api.AnnotatedMessage;
import org.fusesource.fabric.apollo.amqp.codec.api.BareMessage;

/**
 *
 * A message source on an AMQP link
 *
 * @author Stan Lewis
 *
 */
public interface Sender extends Link {

    /**
     * Sends a message on the link to the target.  If sufficient link credit isn't available the message will be buffered locally and the target will be notified that a message is available to be sent.
     * @param message
     * @return
     */
    public boolean put(BareMessage message);

    public boolean put(AnnotatedMessage message);

    /**
     * Returns whether or not this sender has sufficient link credit to send a message
     * @return
     */
    public boolean sufficientLinkCredit();

    /**
     * Sets a task to be performed when the sender has sufficient link or session credit to send and no outstanding messages in it's internal buffer.
     * @param refiller
     */
    public void setRefiller(Runnable refiller);

    /**
     * Returns whether or not this sender has messages buffered
     * @return
     */
    public boolean empty();
}
