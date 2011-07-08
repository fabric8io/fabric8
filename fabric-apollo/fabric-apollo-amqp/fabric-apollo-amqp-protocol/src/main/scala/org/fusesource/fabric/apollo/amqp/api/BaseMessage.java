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

import org.fusesource.fabric.apollo.amqp.codec.types.Footer;
import org.fusesource.fabric.apollo.amqp.codec.types.Header;
import org.fusesource.fabric.apollo.amqp.codec.types.Properties;

import java.util.LinkedList;
import java.util.Map;

/**
 * The common sections for all message types in an AMQP system
 */
public interface BaseMessage {

    /**
     * Returns the message header
     * @return
     */
    public Header getHeader();

    /**
     * Returns a map of delivery annotations
     * @return
     */
    public Map getDeliveryAnnotations();

    /**
     * Returns a map of message annotations
     * @return
     */
    public Map getMessageAnnotations();

    /**
     * Returns the message properties
     * @return
     */
    public Properties getProperties();

    /**
     * Returns the application properties of this message
     * @return
     */
    public Map getApplicationProperties();

    /**
     * Returns the message footer
     * @return
     */
    public Footer getFooter();

    /**
     * Returns the list of tasks to be performed when this message is settled by the peer link
     * @return
     */
    public LinkedList<Runnable> getOnAckTasks();

    /**
     * Returns the list of tasks to be performed when this message is sent by the local session
     * @return
     */
    public LinkedList<Runnable> getOnSendTasks();
}
