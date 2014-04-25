package io.fabric8.common.util;

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
