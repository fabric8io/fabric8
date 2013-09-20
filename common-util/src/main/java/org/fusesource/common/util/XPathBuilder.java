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

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

/**
 * A helper class for creating XPath expressions
 */
public class XPathBuilder {
    private XPathFactory xpathFactory;
    private XPath xpath;

    /**
     * Creates an XPath expression facade
     */
    public XPathFacade xpath(String xpath) throws XPathExpressionException {
        XPathExpression expression = getXPath().compile(xpath);
        return new XPathFacade(this, xpath, expression);
    }

    public XPath getXPath() {
        if (xpath == null) {
            xpath = getXPathFactory().newXPath();
        }
        return xpath;
    }

    public void setXPath(XPath xpath) {
        this.xpath = xpath;
    }

    public XPathFactory getXPathFactory() {
        if (xpathFactory == null) {
            xpathFactory = XPathFactory.newInstance();
        }
        return xpathFactory;
    }

    public void setXPathFactory(XPathFactory xpathFactory) {
        this.xpathFactory = xpathFactory;
    }
}
