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

import org.fusesource.fabric.apollo.amqp.codec.types.AmqpFields;
import org.fusesource.fabric.apollo.amqp.codec.types.AmqpFooter;
import org.fusesource.fabric.apollo.amqp.codec.types.AmqpHeader;
import org.fusesource.fabric.apollo.amqp.codec.types.AmqpProperties;

/**
 * The common sections for all message types in an AMQP system
 */
public interface BaseMessage {

    /**
     * Returns the message header
     * @return
     */
    public AmqpHeader getHeader();

    /**
     * Returns a map of delivery annotations
     * @return
     */
    public AmqpFields getDeliveryAnnotations();

    /**
     * Returns a map of message annotations
     * @return
     */
    public AmqpFields getMessageAnnotations();

    /**
     * Returns the message properties
     * @return
     */
    public AmqpProperties getProperties();

    /**
     * Returns the application properties of this message
     * @return
     */
    public AmqpFields getApplicationProperties();


    /**
     * Returns the message footer
     * @return
     */
    public AmqpFooter getFooter();

}
