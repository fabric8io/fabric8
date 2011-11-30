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

import org.fusesource.fabric.bridge.model.BrokerConfig;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.AbstractSimpleBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.util.StringUtils;
import org.w3c.dom.Element;

/**
 * @author Dhiraj Bokde
 *
 */
public class BrokerConfigParser extends AbstractSimpleBeanDefinitionParser {

	private static final String DESTINATION_RESOLVER_PROPERTY = "destinationResolver";
	private static final String CONNECTION_FACTORY_PROPERTY = "connectionFactory";
	private static final String CONNECTION_FACTORY_REFERENCE = "connectionFactoryRef";
	private static final String DESTINATION_RESOLVER_REFERENCE = "destinationResolverRef";
	private final boolean generateIdAsFallback;

	public BrokerConfigParser(boolean generateIdAsFallback) {
		this.generateIdAsFallback = generateIdAsFallback;
	}

	@SuppressWarnings("rawtypes")
	@Override
	protected Class getBeanClass(Element element) {
		return BrokerConfig.class;
	}
	
	@Override
	protected boolean shouldGenerateIdAsFallback() {
		return generateIdAsFallback;
	}
	
	@Override
	protected void doParse(Element element, ParserContext parserContext,
			BeanDefinitionBuilder builder) {
		super.doParse(element, parserContext, builder);
		
		// set beans for ConnectionFactory and DestinationResolver
		String connectionFactoryRef = element.getAttribute(CONNECTION_FACTORY_REFERENCE);
		if (StringUtils.hasText(connectionFactoryRef)) {
			builder.addPropertyValue(CONNECTION_FACTORY_REFERENCE, connectionFactoryRef);
			builder.addPropertyReference(CONNECTION_FACTORY_PROPERTY, connectionFactoryRef);
		}
		String destinationResolverRef = element.getAttribute(DESTINATION_RESOLVER_REFERENCE);
		if (StringUtils.hasText(destinationResolverRef)) {
			builder.addPropertyValue(DESTINATION_RESOLVER_REFERENCE, destinationResolverRef);
			builder.addPropertyReference(DESTINATION_RESOLVER_PROPERTY, destinationResolverRef);
		}
	}

}
