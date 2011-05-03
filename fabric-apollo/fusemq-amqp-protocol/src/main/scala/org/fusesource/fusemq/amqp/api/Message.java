/**
 * Copyright (C) 2010-2011, FuseSource Corp.  All rights reserved.
 *
 *     http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */

package org.fusesource.fusemq.amqp.api;

import org.fusesource.fusemq.amqp.codec.types.*;
import org.fusesource.hawtbuf.Buffer;

/**
 *
 * Represents an AMQP message
 *
 * @author Stan Lewis
 *
 */
public interface Message {

    /**
     * Gets the delivery tag for this message
     * @return
     */
    public AmqpDeliveryTag getDeliveryTag();

    /**
     * Sets the delivery tag for this message
     * @param tag
     */
    public void setDeliveryTag(AmqpDeliveryTag tag);

    /**
     * Gets whether or not this message has been settled
     * @return
     */
    public boolean getSettled();

    /**
     * Sets whether or not this message has been settled.  Messages that are settled locally (before being sent) will not be acknowledged by the peer
     * @param settled
     */
    public void setSettled(boolean settled);

    /**
     * Add a new DATA message section to this message
     * @param data
     */
    public void addBodyPart(byte[] data);

    /**
     * Add a new DATA message section to this message
     * @param data
     */
    public void addBodyPart(Buffer data);

    /**
     * Add a new AMQP type message section to this message
     * @param data
     * @param <T>
     */
    public <T extends AmqpType<?, ?>> void addBodyPart(T data);

    /**
     * Add a new AmqpMap message section to this message
     * @param map
     */
    public void addBodyPart(AmqpMap map);

    /**
     * Add a new AmqpList message section to this message
     * @param list
     */
    public void addBodyPart(AmqpList list);

    /**
     * Returns the number of message sections in the message body
     * @return
     */
    public int count();

    /**
     * Gets a specific message section from the message body
     * @param index
     * @return
     */
    public Object getBodyPart(int index);

    /**
     * Gets the header of this message
     * @return
     */
    public AmqpHeader getHeader();

    /**
     * Gets the properties of this message
     * @return
     */
    public AmqpProperties getProperties();

    /**
     * Gets the footer of this message
     * @return
     */
    public AmqpFooter getFooter();

    /**
     * Task to be performed when the message is being transmitted
     * @param onSend
     */
    public void onSend(Runnable onSend);

    /**
     * Task to be performed when the message is buffered
     * @param onPut
     */
    public void onPut(Runnable onPut);

    /**
     * Task to be performed when the message has been acknowledged, if the message has been settled locally this task will be performed when the message is transmitted
     * @param onAck
     */
    public void onAck(Runnable onAck);

    /**
     * Gets the status of the message after it's been acknowledged
     * @return
     */
    public Outcome getOutcome();

    /**
     * Gets the error status of the message if the outcome has been REJECTED
     * @return
     */
    public AmqpError getError();

    /**
     * Sets whether or not message acknowledgement should be immediate or can be performed in a batch
     * @param Batchable
     */
    public void setBatchable(boolean Batchable);

    /**
     * Gets whether or not message acknowledgement should be performed in a batch
     * @return
     */
    public boolean getBatchable();

}
