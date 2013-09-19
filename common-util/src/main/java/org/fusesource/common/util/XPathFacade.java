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
package org.fusesource.common.util;

import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;

import org.w3c.dom.*;

/**
 * A facade for working with XPath expressions created via the {@link XPathBuilder} helper class
 */
public class XPathFacade {
    private final XPathBuilder builder;
    private final String xpathText;
    private final XPathExpression expression;

    public XPathFacade(XPathBuilder builder, String xpathText, XPathExpression expression) {
        this.builder = builder;
        this.xpathText = xpathText;
        this.expression = expression;
    }

    @Override
    public String toString() {
        return "XPathFacade(" + xpathText + ")";
    }

    /**
     * Evaluates the XPath expression on the given item and return the first node or null
     */
    public Node node(Object item) throws XPathExpressionException {
        Object answer = expression.evaluate(item, XPathConstants.NODE);
        if (answer instanceof Node) {
            return (Node) answer;
        }
        return null;
    }

    /**
     * Evaluates the XPath expression on the given item and return the first Element or null
     */
    public Element element(Object item) throws XPathExpressionException {
        Node node = node(item);
        if (node instanceof Element) {
            return (Element) node;
        }
        return null;
    }

    public XPathBuilder getBuilder() {
        return builder;
    }

    public String getXpathText() {
        return xpathText;
    }

    public XPathExpression getExpression() {
        return expression;
    }
}
