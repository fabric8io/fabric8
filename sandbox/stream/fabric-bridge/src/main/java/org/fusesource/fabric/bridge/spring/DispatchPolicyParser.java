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

import io.fabric8.bridge.model.DispatchPolicy;
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
