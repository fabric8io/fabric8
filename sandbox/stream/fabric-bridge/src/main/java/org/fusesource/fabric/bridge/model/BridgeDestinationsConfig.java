/**
 * Copyright (C) FuseSource, Inc.
 * http://fusesource.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.fabric8.bridge.model;

import javax.xml.bind.annotation.*;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Dhiraj Bokde
 *
 */
@XmlRootElement(name="destinations-config")
@XmlAccessorType(XmlAccessType.NONE)
@XmlType(propOrder = {"dispatchPolicy", "destinations"})
public class BridgeDestinationsConfig extends IdentifiedType {

	public static final String DEFAULT_STAGING_QUEUE_NAME = "io.fabric8.bridge.stagingQueue";

	public static final String DEFAULT_DESTINATION_NAME_HEADER = "io.fabric8.bridge.destinationName";

	public static final String DEFAULT_DESTINATION_TYPE_HEADER = "io.fabric8.bridge.destinationTypePubSub";

    @XmlAttribute
    private boolean useStagingQueue = true;

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
	
	// place holder for bean name reference for dispatch policy
	@XmlAttribute
	private String dispatchPolicyRef;

	// default is empty destination list, to be filled later
	@XmlElement(name="destination")
	private List<BridgedDestination> destinations = new ArrayList<BridgedDestination>();
	
    public boolean isUseStagingQueue() {
        return useStagingQueue;
    }

    public void setUseStagingQueue(boolean useStagingQueue) {
        this.useStagingQueue = useStagingQueue;
    }

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

    public String getDispatchPolicyRef() {
        return dispatchPolicyRef;
    }

    public void setDispatchPolicyRef(String dispatchPolicyRef) {
        this.dispatchPolicyRef = dispatchPolicyRef;
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
			retVal = this.useStagingQueue == config.useStagingQueue &&
                    this.stagingQueueName.equals(config.stagingQueueName) &&
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
