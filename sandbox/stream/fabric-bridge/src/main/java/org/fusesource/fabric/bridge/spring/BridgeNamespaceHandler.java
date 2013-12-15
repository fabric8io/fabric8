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
package io.fabric8.bridge.spring;

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
