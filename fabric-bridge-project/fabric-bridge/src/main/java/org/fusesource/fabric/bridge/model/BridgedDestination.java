/**
 * Copyright (C) 2010-2011, FuseSource Corp.  All rights reserved.
 *
 *     http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.fabric.bridge.model;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * Destination for fan-in/fan-out connector, also supports various policies for controlling how messages are sent/received. 
 * 
 * @author Dhiraj Bokde
 *
 */
@XmlRootElement(name="destination")
@XmlAccessorType(XmlAccessType.FIELD)
public class BridgedDestination extends IdentifiedType {

	@XmlAttribute(required=true)
	private String name;

	@XmlAttribute
	private boolean pubSubDomain;
	
	@XmlAttribute
	private String durableSubscriptionName;
	
	@XmlAttribute
	private boolean subscriptionDurable;
	
	@XmlAttribute
	private String targetName;
	
	@XmlElement(name="dispatch-policy")
	private DispatchPolicy dispatchPolicy;

	// place holder for bean name reference for dispatch policy
	@XmlAttribute
	private String dispatchPolicyRef;

	public void setName(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public void setPubSubDomain(boolean pubSubDomain) {
		this.pubSubDomain = pubSubDomain;
	}

	public boolean isPubSubDomain() {
		return pubSubDomain;
	}

	public final String getDurableSubscriptionName() {
		return durableSubscriptionName;
	}

	public final void setDurableSubscriptionName(String durableSubscriptionName) {
		this.durableSubscriptionName = durableSubscriptionName;
	}

	public final boolean isSubscriptionDurable() {
		return subscriptionDurable;
	}

	public final void setSubscriptionDurable(boolean subscriptionDurable) {
		this.subscriptionDurable = subscriptionDurable;
	}

	public void setTargetName(String targetName) {
		this.targetName = targetName;
	}

	public String getTargetName() {
		return targetName;
	}

	public void setDispatchPolicy(DispatchPolicy dispatchPolicy) {
		this.dispatchPolicy = dispatchPolicy;
	}

	public DispatchPolicy getDispatchPolicy() {
		return dispatchPolicy;
	}

	public String getDispatchPolicyRef() {
		return dispatchPolicyRef;
	}

	public void setDispatchPolicyRef(String dispatchPolicyRef) {
		this.dispatchPolicyRef = dispatchPolicyRef;
	}

	@Override
	public String toString() {
		return ReflectionToStringBuilder.toString(this, ToStringStyle.SHORT_PREFIX_STYLE);
	}

	@Override
	public int hashCode() {
		return (name != null ? name.hashCode() : getClass().toString().hashCode());
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj != null && obj instanceof BridgedDestination) {
			BridgedDestination dest = (BridgedDestination) obj;
			return (this.name != null ? this.name.equals(dest.name) : dest.name == null) &&
				(this.pubSubDomain == dest.pubSubDomain) &&
				(this.durableSubscriptionName != null ? this.durableSubscriptionName.equals(dest.durableSubscriptionName) : dest.durableSubscriptionName == null) &&
				(this.subscriptionDurable == dest.subscriptionDurable) &&
				(this.targetName != null ? this.targetName.equals(dest.targetName) : dest.targetName == null) &&
				(this.dispatchPolicy != null ? this.dispatchPolicy.equals(dest.dispatchPolicy) : dest.dispatchPolicy == null) &&
				(this.dispatchPolicyRef != null ? this.dispatchPolicyRef.equals(dest.dispatchPolicyRef) : dest.dispatchPolicyRef == null);
			}

		return false;
	}

}
