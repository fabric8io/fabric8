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

import org.fusesource.fabric.bridge.model.BridgedDestination;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.AbstractSimpleBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.util.StringUtils;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * @author Dhiraj Bokde
 *
 */
public class BridgedDestinationParser extends AbstractSimpleBeanDefinitionParser {

	private static final String DISPATCH_POLICY_ELEMENT = "dispatch-policy";
	private static final String DISPATCH_POLICY_PROPERTY = "dispatchPolicy";
	private static final String DISPATCH_POLICY_REF_ATTRIBUTE = "dispatchPolicyRef";
	private final DispatchPolicyParser dispatchPolicyParser = new DispatchPolicyParser(true);
	private boolean generateIdAsFallback;

	public BridgedDestinationParser(boolean generateIdAsFallback) {
		this.generateIdAsFallback  = generateIdAsFallback;
	}

	@SuppressWarnings("rawtypes")
	@Override
	protected Class getBeanClass(Element element) {
		return BridgedDestination.class;
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
	}

}
