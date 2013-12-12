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

import io.fabric8.bridge.model.RemoteBridge;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
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
public class RemoteBridgeParser extends AbstractSimpleBeanDefinitionParser {

    private static final String INBOUND_DESTINATIONS_PROPERTY = "inboundDestinations";
    private static final String INBOUND_DESTINATIONS_ELEMENT = "inbound-destinations";
    private static final String INBOUND_DESTINATIONS_REF = "inboundDestinationsRef";

    private static final String OUTBOUND_DESTINATIONS_PROPERTY = "outboundDestinations";
    private static final String OUTBOUND_DESTINATIONS_ELEMENT = "outbound-destinations";
    private static final String OUTBOUND_DESTINATIONS_REF = "outboundDestinationsRef";

	private static final String REMOTE_BROKER_PROPERTY = "remoteBrokerConfig";
	private static final String REMOTE_BROKER_ELEMENT = "remote-broker";
	private static final String REMOTE_BROKER_REF = "remoteBrokerRef";

	private final BrokerConfigParser brokerConfigParser = new BrokerConfigParser(true);
	private final BridgeDestinationsConfigParser bridgeDestinationsConfigParser = new BridgeDestinationsConfigParser(true);
	private boolean generateIdAsFallback;

    public RemoteBridgeParser(boolean generateIdAsFallback) {
		this.generateIdAsFallback = generateIdAsFallback;
	}

	@Override
	protected boolean shouldGenerateIdAsFallback() {
		return generateIdAsFallback;
	}

	@SuppressWarnings("rawtypes")
	@Override
	protected Class getBeanClass(Element element) {
		return RemoteBridge.class;
	}

	@Override
	protected void doParse(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {

		// parse remote broker and outbound destinations
		String remoteBrokerRef = element.getAttribute(REMOTE_BROKER_REF);
		if (StringUtils.hasText(remoteBrokerRef)) {
			if (element.getElementsByTagNameNS(BridgeNamespaceHandler.BRIDGE_NS, REMOTE_BROKER_ELEMENT).getLength() > 0) {
				throw new BeanCreationException(builder.getRawBeanDefinition().getResourceDescription(),
						element.getAttribute(ID_ATTRIBUTE), "Only one of "
								+ REMOTE_BROKER_REF + " or "
								+ REMOTE_BROKER_ELEMENT + " is allowed");
			}
			builder.addPropertyValue(REMOTE_BROKER_REF, remoteBrokerRef);
			builder.addPropertyReference(REMOTE_BROKER_PROPERTY, remoteBrokerRef);
		}

		String inboundDestinationsRef = element.getAttribute(INBOUND_DESTINATIONS_REF);
		if (StringUtils.hasText(inboundDestinationsRef)) {
			if (element.getElementsByTagNameNS(BridgeNamespaceHandler.BRIDGE_NS, INBOUND_DESTINATIONS_ELEMENT).getLength() > 0) {
				throw new BeanCreationException(builder.getRawBeanDefinition().getResourceDescription(),
						element.getAttribute(ID_ATTRIBUTE), "Only one of "
								+ INBOUND_DESTINATIONS_REF + " or "
								+ INBOUND_DESTINATIONS_ELEMENT + " is allowed");
			}
			builder.addPropertyValue(INBOUND_DESTINATIONS_REF, inboundDestinationsRef);
			builder.addPropertyReference(INBOUND_DESTINATIONS_PROPERTY, inboundDestinationsRef);
		}

        String outboundDestinationsRef = element.getAttribute(OUTBOUND_DESTINATIONS_REF);
        if (StringUtils.hasText(outboundDestinationsRef)) {
            if (element.getElementsByTagNameNS(BridgeNamespaceHandler.BRIDGE_NS, OUTBOUND_DESTINATIONS_ELEMENT).getLength() > 0) {
                throw new BeanCreationException(builder.getRawBeanDefinition().getResourceDescription(),
                        element.getAttribute(ID_ATTRIBUTE), "Only one of "
                                + OUTBOUND_DESTINATIONS_REF + " or "
                                + OUTBOUND_DESTINATIONS_ELEMENT + " is allowed");
            }
            builder.addPropertyValue(OUTBOUND_DESTINATIONS_REF, outboundDestinationsRef);
            builder.addPropertyReference(OUTBOUND_DESTINATIONS_PROPERTY, outboundDestinationsRef);
        }

		NodeList childNodes = element.getChildNodes();
		for (int i = 0; i < childNodes.getLength(); i++) {
			Node node = childNodes.item(i);
			if (node instanceof Element) {
				Element childElement = (Element)node;
				String localName = childElement.getLocalName();
				if (localName.equals(REMOTE_BROKER_ELEMENT)) {
					builder.addPropertyValue(REMOTE_BROKER_PROPERTY, brokerConfigParser.parse(childElement, parserContext));
				} else if (localName.equals(OUTBOUND_DESTINATIONS_ELEMENT)) {
					builder.addPropertyValue(OUTBOUND_DESTINATIONS_PROPERTY, bridgeDestinationsConfigParser.parse(childElement, parserContext));
                } else if (localName.equals(INBOUND_DESTINATIONS_ELEMENT)) {
                    builder.addPropertyValue(INBOUND_DESTINATIONS_PROPERTY, bridgeDestinationsConfigParser.parse(childElement, parserContext));
                }
			}
		}

	}

}
