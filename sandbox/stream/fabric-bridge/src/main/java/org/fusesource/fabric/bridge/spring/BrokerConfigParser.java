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

import io.fabric8.bridge.model.BrokerConfig;
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
