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

import org.fusesource.fabric.bridge.model.DispatchPolicy;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.AbstractSimpleBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.util.StringUtils;
import org.w3c.dom.Element;

/**
 * @author dbokde
 *
 */
public class DispatchPolicyParser extends AbstractSimpleBeanDefinitionParser {
	
	private boolean generateIdAsFallback = false;

	// ctor used when this parser is used to parse child element
	public DispatchPolicyParser(boolean generateIdAsFallback) {
		this.generateIdAsFallback  = generateIdAsFallback;
	}

	@SuppressWarnings("rawtypes")
	@Override
	protected Class getBeanClass(Element element) {
		return DispatchPolicy.class;
	}

	@Override
	protected boolean shouldGenerateIdAsFallback() {
		return generateIdAsFallback;
	}

	@Override
	protected void doParse(Element element, ParserContext context, BeanDefinitionBuilder builder) {
		super.doParse(element, context, builder);
		
		// look for a message converter attribute, which must a bean name
		String messageConverter = element.getAttribute("messageConverter");
		if (StringUtils.hasText(messageConverter)) {
			builder.addPropertyReference("messageConverterRef", messageConverter);
			builder.addPropertyReference("messageConverter", messageConverter);
		}

	}

}
