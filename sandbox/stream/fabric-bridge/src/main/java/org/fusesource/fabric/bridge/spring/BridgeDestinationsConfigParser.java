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

import io.fabric8.bridge.model.BridgeDestinationsConfig;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.beans.factory.xml.AbstractSimpleBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.util.StringUtils;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * @author Dhiraj Bokde
 *
 */
public class BridgeDestinationsConfigParser extends AbstractSimpleBeanDefinitionParser {

	private static final String DISPATCH_POLICY_ELEMENT = "dispatch-policy";
	private static final String DISPATCH_POLICY_PROPERTY = "dispatchPolicy";
    private static final String DISPATCH_POLICY_REF_ATTRIBUTE = "dispatchPolicyRef";

    private static final String DESTINATION_ELEMENT = "destination";
    private static final String DESTINATIONS_PROPERTY = "destinations";

    private final DispatchPolicyParser dispatchPolicyParser = new DispatchPolicyParser(true);
    private final BridgedDestinationParser destinationParser = new BridgedDestinationParser(true);

    private final boolean generateIdAsFallback;

    public BridgeDestinationsConfigParser(boolean generateIdAsFallback) {
		this.generateIdAsFallback = generateIdAsFallback;
	}

	@SuppressWarnings("rawtypes")
	@Override
	protected Class getBeanClass(Element element) {
		return BridgeDestinationsConfig.class;
	}
	
	@Override
	protected boolean shouldGenerateIdAsFallback() {
		return generateIdAsFallback;
	}
	
	@Override
	protected void doParse(Element element, ParserContext parserContext,
			BeanDefinitionBuilder builder) {
		
		super.doParse(element, parserContext, builder);
		
		// parse dispatch-policy
		String dispatchPolicyReference = element.getAttribute(DISPATCH_POLICY_REF_ATTRIBUTE);
		if (StringUtils.hasText(dispatchPolicyReference)) {
			// make sure no nested dispatch policy is provided
			if (element.getElementsByTagNameNS(BridgeNamespaceHandler.BRIDGE_NS, DISPATCH_POLICY_ELEMENT).getLength() > 0) {
				throw new BeanCreationException(builder.getBeanDefinition().getResourceDescription(),
						element.getAttribute(ID_ATTRIBUTE), "Only one of "
								+ DISPATCH_POLICY_REF_ATTRIBUTE + " or "
								+ DISPATCH_POLICY_ELEMENT + " is allowed");
			} else {
				builder.addPropertyValue(DISPATCH_POLICY_REF_ATTRIBUTE, dispatchPolicyReference);
				builder.addPropertyReference(DISPATCH_POLICY_PROPERTY, dispatchPolicyReference);
			}
		} else {
			NodeList elements = element.getElementsByTagNameNS(BridgeNamespaceHandler.BRIDGE_NS, DISPATCH_POLICY_ELEMENT);
			if (elements != null && elements.getLength() == 1) {
				builder.addPropertyValue(DISPATCH_POLICY_PROPERTY, dispatchPolicyParser.parse((Element) elements.item(0), parserContext));
			}
		}
		
		// parse destinations
        NodeList elements = element.getElementsByTagNameNS(BridgeNamespaceHandler.BRIDGE_NS, DESTINATION_ELEMENT);
		ManagedList<BeanDefinition> destinations = new ManagedList<BeanDefinition>();
		for (int i = 0; i < elements.getLength(); i++) {
			Element destinationElement = (Element) elements.item(i);
			destinations.add(destinationParser.parse(destinationElement, parserContext));
		}
		if (!destinations.isEmpty()) {
			builder.addPropertyValue(DESTINATIONS_PROPERTY, destinations);
		}
	}

}
