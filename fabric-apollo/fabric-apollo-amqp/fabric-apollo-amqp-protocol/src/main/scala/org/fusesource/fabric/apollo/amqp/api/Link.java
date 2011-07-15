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

import org.fusesource.fabric.apollo.amqp.codec.interfaces.AMQPType;
import org.fusesource.fabric.apollo.amqp.codec.types.AMQPSymbol;
import org.fusesource.hawtbuf.Buffer;

import java.util.Map;

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
    public void setTargetDurable(long durable);

    /**
     * Gets whether or not the target of this link should be durably retained
     * @return
     */
    public long getTargetDurable();

    /**
     * Sets whether or not the source of this link should be durably retained
     * @param durable
     */
    public void setSourceDurable(long durable);

    /**
     * Gets whether or not the source of this link should be durably retained
     * @return
     */
    public long getSourceDurable();

    /**
     * Sets the expiry policy of the target of this link
     * @param policy
     */
    public void setTargetExpiryPolicy(Buffer policy);

    /**
     * Gets the expiry policy of the target of this link
     * @return
     */
    public Buffer getTargetExpiryPolicy();

    /**
     * Sets the expiry policy of the source of this link
     * @param policy
     */
    public void setSourceExpiryPolicy(Buffer policy);

    /**
     * Gets the expiry policy of the source of this link
     * @return
     */
    public Buffer getSourceExpiryPolicy();

    /**
     * Sets the timeout for when this link's target expires after being detached
     * @param timeout
     */
    public void setTargetTimeout(long timeout);

    /**
     * Gets the timeout for when this link's target expires after being detached
     * @return
     */
    public long getTargetTimeout();

    /**
     * Sets the timeout for when this link's source expires after being detached
     * @param timeout
     */
    public void setSourceTimeout(long timeout);

    /**
     * Gets the timeout for when this link's source expires after being detached
     * @return
     */
    public long getSourceTimeout();

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
    public void setDistributionMode(Buffer mode);

    /**
     * Gets the distribution mode of this link
     * @return
     */
    public Buffer getDistributionMode();

    /**
     * Sets a filter on this link
     * @param filter
     */
    public void setFilter(Map<?, ?> filter);

    /**
     * Gets whatever filter is set on this link
     * @return
     */
    public Map<?, ?> getFilter();

    /**
     * Sets the default outcome to be used for unsettled transfers
     * @param outcome
     */
    public void setDefaultOutcome(AMQPType outcome);

    /**
     * Limits the possible outcomes that can be used for unsettled transfers
     * @param outcomes
     */
    public void setPossibleOutcomes(AMQPSymbol outcomes[]);

    /**
     * Sets the capabilities available on this link
     * @param capabilities
     */
    public void setCapabilities(AMQPSymbol capabilities[]);

    /**
     * Gets the capabilities available on this link
     * @return
     */
    public AMQPSymbol[] getCapabilities();

    /**
     * Sets the capabilities desired by the other end of this link
     * @param capabilities
     */
    public void setDesiredCapabilities(AMQPSymbol capabilities[]);

    /**
     * Gets the capabilities desired by the other end of this link
     * @return
     */
    public AMQPSymbol[] getDesiredCapabilities();

}
