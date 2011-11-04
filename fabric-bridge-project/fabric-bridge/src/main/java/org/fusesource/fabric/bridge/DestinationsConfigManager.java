/**
 * Copyright (C) 2010-2011, FuseSource Corp.  All rights reserved.
 *
 *     http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.fabric.bridge;

import java.util.List;

import org.fusesource.fabric.bridge.model.BridgeDestinationsConfig;
import org.fusesource.fabric.bridge.model.BridgedDestination;
import org.springframework.jms.JmsException;

/**
 * 
 * Manages destinations config in a connector
 * 
 * @author Dhiraj Bokde
 *
 */
public interface DestinationsConfigManager {
	
	BridgeDestinationsConfig getDestinationsConfig() throws JmsException;
	
	void setDestinationsConfig(BridgeDestinationsConfig destinationsConfig) throws JmsException;
	
	void addDestinations(List<BridgedDestination> destinations) throws JmsException;
	
	void removeDestinations(List<BridgedDestination> destinations) throws JmsException;

}
