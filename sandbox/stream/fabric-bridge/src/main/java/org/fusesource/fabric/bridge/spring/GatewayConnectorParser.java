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

import io.fabric8.bridge.GatewayConnector;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.beans.factory.xml.AbstractSimpleBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.util.StringUtils;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * @author Dhiraj Bokde
 *
 */
public class GatewayConnectorParser extends AbstractSimpleBeanDefinitionParser {

	private static final String LOCAL_BROKER_CONFIG_PROPERTY = "localBrokerConfig";
	private static final String LOCAL_BROKER_ELEMENT = "local-broker";
	
	private static final String OUTBOUND_DESTINATIONS_PROPERTY = "outboundDestinations";
	private static final String OUTBOUND_DESTINATIONS_ELEMENT = "outbound-destinations";

	private static final String INBOUND_DESTINATIONS_PROPERTY = "inboundDestinations";
	private static final String INBOUND_DESTINATIONS_ELEMENT = "inbound-destinations";
	
	private static final String REMOTE_BRIDGES_PROPERTY = "remoteBridges";
	private static final String REMOTE_BRIDGE_ELEMENT = "remote-bridge";

	protected final BrokerConfigParser brokerConfigParser = new BrokerConfigParser(true);
	private final BridgeDestinationsConfigParser bridgeDestinationsConfigParser = new BridgeDestinationsConfigParser(true);
	private final RemoteBridgeParser remoteBridgeParser = new RemoteBridgeParser(true);
    private static final String INBOUND_DESTINATIONS_ATTRIBUTE = "inboundDestinationsRef";
    private static final String OUTBOUND_DESTINATIONS_ATTRIBUTE = "outboundDestinationsRef";

    @Override
    protected Class getBeanClass(Element element) {
        return GatewayConnector.class;
    }

    @Override
	protected void doParse(Element element,
                           ParserContext parserContext, BeanDefinitionBuilder builder) {
        // parse attributes
		super.doParse(element, parserContext, builder);
		
        // look for destinations references
        String inboundDestinationsRef = element.getAttribute(INBOUND_DESTINATIONS_ATTRIBUTE);
        if (StringUtils.hasText(inboundDestinationsRef)) {
            if (element.getElementsByTagNameNS(BridgeNamespaceHandler.BRIDGE_NS, INBOUND_DESTINATIONS_ELEMENT).getLength() > 0) {
                throw new BeanCreationException("Only one of " +
                    INBOUND_DESTINATIONS_ATTRIBUTE + " or " +
                    INBOUND_DESTINATIONS_ELEMENT + " is allowed");
            }
            builder.addPropertyReference(INBOUND_DESTINATIONS_PROPERTY, inboundDestinationsRef);
        }

        String outboundDestinationsRef = element.getAttribute(OUTBOUND_DESTINATIONS_ATTRIBUTE);
        if (StringUtils.hasText(outboundDestinationsRef)) {
            if (element.getElementsByTagNameNS(BridgeNamespaceHandler.BRIDGE_NS, OUTBOUND_DESTINATIONS_ELEMENT).getLength() > 0) {
                throw new BeanCreationException("Only one of " +
                    OUTBOUND_DESTINATIONS_ATTRIBUTE + " or " +
                    OUTBOUND_DESTINATIONS_ELEMENT + " is allowed");
            }
            builder.addPropertyReference(OUTBOUND_DESTINATIONS_PROPERTY, outboundDestinationsRef);
        }

		// parse remote and local broker and destinations
        ManagedList<BeanDefinition> remoteBridges = new ManagedList<BeanDefinition>();
		NodeList childNodes = element.getChildNodes();
		for (int i = 0; i < childNodes.getLength(); i++) {
			Node node = childNodes.item(i);
			if (node instanceof Element) {
				Element childElement = (Element)node;
				String localName = childElement.getLocalName();

				if (localName.equals(LOCAL_BROKER_ELEMENT)) {
					builder.addPropertyValue(LOCAL_BROKER_CONFIG_PROPERTY, brokerConfigParser.parse(childElement, parserContext));
				} else if (localName.equals(INBOUND_DESTINATIONS_ELEMENT)) {
					builder.addPropertyValue(INBOUND_DESTINATIONS_PROPERTY, bridgeDestinationsConfigParser.parse(childElement, parserContext));
				} else if (localName.equals(OUTBOUND_DESTINATIONS_ELEMENT)) {
					builder.addPropertyValue(OUTBOUND_DESTINATIONS_PROPERTY, bridgeDestinationsConfigParser.parse(childElement, parserContext));
				} else if (localName.equals(REMOTE_BRIDGE_ELEMENT)) {
					remoteBridges.add(remoteBridgeParser.parse(childElement, parserContext));
				}
			}
		}
        if (!remoteBridges.isEmpty()) {
            builder.addPropertyValue(REMOTE_BRIDGES_PROPERTY, remoteBridges);
        }
	}

}
