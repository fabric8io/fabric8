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
 * Base generic type of all AMQP message types
 */
public interface GenericMessage<E> extends BaseMessage {

    /**
     * Returns the message body
     * @return
     */
    public E getBody();

    /**
     * Sets the message body
     * @param body
     */
    public void setBody(E body);


}
