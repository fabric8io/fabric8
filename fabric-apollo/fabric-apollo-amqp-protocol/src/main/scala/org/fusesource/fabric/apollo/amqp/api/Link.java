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

import org.fusesource.fabric.apollo.amqp.codec.types.*;

/**
 *
 * Shared methods for Sender/Receiver
 *
 * @author Stan Lewis
 *
 */
public interface Link {

    /**
     * Returns the session that created this link
     * @return
     */
    public Session getSession();

    /**
     * Sets the name of this link
     * @param name
     */
    public void setName(String name);

    /**
     * Gets the name of this link
     * @return
     */
    public String getName();

    /**
     * Attaches this link to the session
     * @param onAttach task to be executed when the peer link responds
     */
    public void attach(Runnable onAttach);

    /**
     * Detaches this link from the session gracefully
     */
    public void detach();

    /**
     * Detaches this link from the session ungracefully
     * @param t reason for link termination
     */
    public void detach(Throwable t);

    /**
     * Detaches this link from the session ungracefully
     * @param reason reason for link termination
     */
    public void detach(String reason);

    /**
     * Sets the task to be performed when this link is detached
     * @param task
     */
    public void setOnDetach(Runnable task);

    /**
     * Returns whether or not this link is attached to a remote peer
     * @return
     */
    public boolean established();

    /**
     * Creates a new message
     * @param tag the delivery tag to be used when creating the message
     * @return an empty AMQP message
     */
    public Message createMessage(String tag);

    /**
     * Creates a new message
     * @param tag the delivery tag to be used when creating the message
     * @return an empty AMQP message
     */
    public Message createMessage(AmqpDeliveryTag tag);

    /**
     * Creates a new message with a delivery tag generated via UUID.randomUUID()
     * @return an empty AMQP message
     */
    public Message createMessage();

    /**
     * Sets the destination address for this link
     * @param address
     */
    public void setAddress(String address);

    /**
     * Gets the destination address for this link
     * @return
     */
    public String getAddress();

    /**
     * Sets whether or not the target of this link should be durably retained
     * @param durable
     */
    public void setTargetDurable(boolean durable);

    /**
     * Gets whether or not the target of this link should be durably retained
     * @return
     */
    public boolean getTargetDurable();

    /**
     * Sets whether or not the source of this link should be durably retained
     * @param durable
     */
    public void setSourceDurable(boolean durable);

    /**
     * Gets whether or not the source of this link should be durably retained
     * @return
     */
    public boolean getSourceDurable();

    /**
     * Sets the expiry policy of the target of this link
     * @param policy
     */
    public void setTargetExpiryPolicy(AmqpTerminusExpiryPolicy policy);

    /**
     * Gets the expiry policy of the target of this link
     * @return
     */
    public AmqpTerminusExpiryPolicy getTargetExpiryPolicy();

    /**
     * Sets the expiry policy of the source of this link
     * @param policy
     */
    public void setSourceExpiryPolicy(AmqpTerminusExpiryPolicy policy);

    /**
     * Gets the expiry policy of the source of this link
     * @return
     */
    public AmqpTerminusExpiryPolicy getSourceExpiryPolicy();

    /**
     * Sets the timeout for when this link's target expires after being detached
     * @param timeout
     */
    public void setTargetTimeout(AmqpSeconds timeout);

    /**
     * Gets the timeout for when this link's target expires after being detached
     * @return
     */
    public AmqpSeconds getTargetTimeout();

    /**
     * Sets the timeout for when this link's source expires after being detached
     * @param timeout
     */
    public void setSourceTimeout(AmqpSeconds timeout);

    /**
     * Gets the timeout for when this link's source expires after being detached
     * @return
     */
    public AmqpSeconds getSourceTimeout();

    /**
     * Sets the lifetime policy of a dynamically created address
     * @param lifetime policy that controls the lifetime of the dynamically created address
     */
    public void setDynamic(Lifetime lifetime);

    /**
     * Gets the lifetime policy of a dynamically created address
     * @return
     */
    public Lifetime getDynamic();

    /**
     * Sets the distribution mode of this link, defaults to MOVE
     * @param mode
     */
    public void setDistributionMode(DistributionMode mode);

    /**
     * Gets the distribution mode of this link
     * @return
     */
    public DistributionMode getDistributionMode();

    /**
     * Sets a filter on this link
     * @param filter
     */
    public void setFilter(AmqpFilterSet filter);

    /**
     * Gets whatever filter is set on this link
     * @return
     */
    public AmqpFilterSet getFilter();

    /**
     * Sets the default outcome to be used for unsettled transfers
     * @param outcome
     */
    public void setDefaultOutcome(Outcome outcome);

    /**
     * Limits the possible outcomes that can be used for unsettled transfers
     * @param outcomes
     */
    public void setPossibleOutcomes(Outcome outcomes[]);

    /**
     * Sets the capabilities available on this link
     * @param capabilities
     */
    public void setCapabilities(String capabilities[]);

    /**
     * Gets the capabilities available on this link
     * @return
     */
    public String[] getCapabilities();

    /**
     * Sets the capabilities desired by the other end of this link
     * @param capabilities
     */
    public void setDesiredCapabilities(String capabilities[]);

    /**
     * Gets the capabilities desired by the other end of this link
     * @return
     */
    public String[] getDesiredCapabilities();

}
