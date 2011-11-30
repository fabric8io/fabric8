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

import org.fusesource.fabric.bridge.model.BridgeDestinationsConfig;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.beans.factory.xml.AbstractSimpleBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * @author Dhiraj Bokde
 *
 */
public class BridgeDestinationsConfigParser extends AbstractSimpleBeanDefinitionParser {

	private static final String DISPATCH_POLICY_ELEMENT = "dispatch-policy";
	private static final String DISPATCH_POLICY_PROPERTY = "dispatchPolicy";
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
		NodeList elements = element.getElementsByTagNameNS(BridgeNamespaceHandler.BRIDGE_NS, DISPATCH_POLICY_ELEMENT);
		if (elements != null && elements.getLength() == 1) {
			Element dispatchPolicyElement = (Element) elements.item(0);
			builder.addPropertyValue(DISPATCH_POLICY_PROPERTY, dispatchPolicyParser.parse(dispatchPolicyElement, parserContext));
		}
		
		// parse destinations
		elements = element.getElementsByTagNameNS(BridgeNamespaceHandler.BRIDGE_NS, DESTINATION_ELEMENT);
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
