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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.xml.bind.annotation.*;

/**
 * @author Dhiraj Bokde
 *
 */
@XmlRootElement(name="destinations-config")
@XmlAccessorType(XmlAccessType.NONE)
@XmlType(propOrder = {"dispatchPolicy", "destinations"})
public class BridgeDestinationsConfig extends IdentifiedType {

	public static final String DEFAULT_STAGING_QUEUE_NAME = "org.fusesource.fabric.bridge.stagingQueue";

	public static final String DEFAULT_DESTINATION_NAME_HEADER = "org.fusesource.fabric.bridge.destinationName";

	public static final String DEFAULT_DESTINATION_TYPE_HEADER = "org.fusesource.fabric.bridge.destinationTypePubSub";

	@XmlAttribute
	private String stagingQueueName = DEFAULT_STAGING_QUEUE_NAME;

	@XmlAttribute
	private boolean defaultStagingLocation = true;

	@XmlAttribute
	private String destinationNameHeader = DEFAULT_DESTINATION_NAME_HEADER;
	
	@XmlAttribute
	private String destinationTypeHeader = DEFAULT_DESTINATION_TYPE_HEADER;

	// use default DipatchPolicy
	@XmlElement(name="dispatch-policy")
	private DispatchPolicy dispatchPolicy = new DispatchPolicy();
	
	// default is empty destination list, to be filled later
	@XmlElement(name="destination")
	private List<BridgedDestination> destinations = new ArrayList<BridgedDestination>();
	
	public final void setStagingQueueName(String stagingQueueName) {
		this.stagingQueueName = stagingQueueName;
	}

	public final String getStagingQueueName() {
		return stagingQueueName;
	}

	public final boolean isDefaultStagingLocation() {
		return defaultStagingLocation;
	}

	public final void setDefaultStagingLocation(boolean defaultStagingLocation) {
		this.defaultStagingLocation = defaultStagingLocation;
	}

	public final void setDestinationNameHeader(String destinationNameHeader) {
		this.destinationNameHeader = destinationNameHeader;
	}

	public final String getDestinationNameHeader() {
		return destinationNameHeader;
	}

	public final String getDestinationTypeHeader() {
		return destinationTypeHeader;
	}

	public final void setDestinationTypeHeader(String destinationTypeHeader) {
		this.destinationTypeHeader = destinationTypeHeader;
	}

	public final void setDispatchPolicy(DispatchPolicy dispatchPolicy) {
		this.dispatchPolicy = dispatchPolicy;
	}

	public final DispatchPolicy getDispatchPolicy() {
		return dispatchPolicy;
	}

	public final List<BridgedDestination> getDestinations() {
		return destinations;
	}

	public final void setDestinations(List<BridgedDestination> destinations) {
		this.destinations = destinations;
	}

	@Override
	public boolean equals(Object other) {
		boolean retVal = false;
		if (other != null && other instanceof BridgeDestinationsConfig) {
			BridgeDestinationsConfig config = (BridgeDestinationsConfig) other;
			retVal = this.stagingQueueName.equals(config.stagingQueueName) &&
					defaultStagingLocation == config.defaultStagingLocation &&
					destinationNameHeader.equals(config.destinationNameHeader) &&
					destinationTypeHeader.equals(config.destinationTypeHeader) &&
					dispatchPolicy.equals(config.dispatchPolicy);
			if (retVal) {
				// compare destinations
				if (destinations == null) {
					retVal = (config.destinations == null);
				} else if (config.destinations != null &&
                    (destinations.size() == config.destinations.size())) {
					for (BridgedDestination destination : destinations) {
						if (!config.destinations.contains(destination)) {
							retVal = false;
							break;
						}
					}
				} else {
					retVal = false;
				}
			}
		}
		return retVal;
	}

}
