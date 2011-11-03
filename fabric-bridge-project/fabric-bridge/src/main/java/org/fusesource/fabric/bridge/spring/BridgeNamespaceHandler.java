/**
 * Copyright (C) 2010-2011, FuseSource Corp.  All rights reserved.
 *
 *     http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.fabric.bridge.spring;

import org.springframework.beans.factory.xml.NamespaceHandlerSupport;

/**
 * @author dbokde
 *
 */
public class BridgeNamespaceHandler extends NamespaceHandlerSupport {

	public static final String BRIDGE_NS = "http://fusesource.org/fabric/bridge";

	/* (non-Javadoc)
	 * @see org.springframework.beans.factory.xml.NamespaceHandler#init()
	 */
	@Override
	public void init() {
		
		// model parsers
		registerBeanDefinitionParser("destination", new BridgedDestinationParser(false));
		registerBeanDefinitionParser("destinations-config", new BridgeDestinationsConfigParser(false));
		registerBeanDefinitionParser("broker-config", new BrokerConfigParser(false));
		registerBeanDefinitionParser("dispatch-policy", new DispatchPolicyParser(false));		
		registerBeanDefinitionParser("remote-bridge", new RemoteBridgeParser(false));

		// connector parsers
		registerBeanDefinitionParser("bridge-connector", new BridgeConnectorParser());
		registerBeanDefinitionParser("gateway-connector", new GatewayConnectorParser());

	}

}
