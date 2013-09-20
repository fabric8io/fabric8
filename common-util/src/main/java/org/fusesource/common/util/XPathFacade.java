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

import java.util.ArrayList;
import java.util.List;

/**
 * A facade for working with XPath expressions created via the {@link XPathBuilder} helper class.
 *
 * For example
 * <code>
 *     XPathBuilder builder = new XPathBuilder();
 *     Element firstElement = builder.xpath("//foo[@x='abc']").element(doc);
 * </code>
 *
 * @see org.fusesource.common.util.XPathBuilder#xpath(String) for how to create this facade
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
     * Evaluates the XPath expression on the given item and return a list of nodes
     */
    public List<Node> nodes(Object item) throws XPathExpressionException {
        List<Node> answer = new ArrayList<Node>();
        Object value = expression.evaluate(item, XPathConstants.NODESET);
        if (value instanceof NodeList) {
            NodeList nodeList = (NodeList) value;
            for (int i = 0, size = nodeList.getLength(); i < size; i++) {
                Node node = nodeList.item(i);
                if (node != null) {
                    answer.add(node);
                }
            }
        } else if (value instanceof Node) {
                answer.add((Node) value);
        }
        return answer;
    }

    /**
     * Evaluates the XPath expression on the given item and return a list of nodes
     */
    public List<Element> elements(Object item) throws XPathExpressionException {
        List<Element> answer = new ArrayList<Element>();
        Object value = expression.evaluate(item, XPathConstants.NODESET);
        if (value instanceof NodeList) {
            NodeList nodeList = (NodeList) value;
            for (int i = 0, size = nodeList.getLength(); i < size; i++) {
                Node node = nodeList.item(i);
                if (node instanceof Element) {
                    answer.add((Element) node);
                }
            }
        } else if (value instanceof Element) {
                answer.add((Element) value);
        }
        return answer;
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


    /**
     * Returns the text content of the selected item or null if no element is found
     */
    public String elementTextContent(Object item) throws XPathExpressionException {
        Element element = element(item);
        if (element != null){
            return element.getTextContent();
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
