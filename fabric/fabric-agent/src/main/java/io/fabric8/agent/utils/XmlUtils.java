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
package io.fabric8.agent.utils;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * XML related utilities.
 * TODO merge this clas with ElementHelper
 *
 * @author Alin Dreghiciu
 * @author Niclas Heldman
 */
public class XmlUtils {

    /**
     * Utility class. ment to be used via static methods.
     */
    private XmlUtils() {
        // utility class
    }

    public static Document parseDoc(File xmlFile)
            throws ParserConfigurationException,
            SAXException,
            IOException {
        return parseDoc(new FileInputStream(xmlFile));
    }

    public static Document parseDoc(final InputStream is)
            throws ParserConfigurationException,
            SAXException,
            IOException {
        try {
            BufferedInputStream in = new BufferedInputStream(is);
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            InputSource source = new InputSource(in);
            return builder.parse(source);
        } finally {
            is.close();
        }
    }

    public static Element getElement(final Document doc, final String path) {
        NullArgumentException.validateNotNull(doc, "XML document");
        return getElement(doc.getDocumentElement(), path);
    }

    public static Element getElement(final Element element, final String path) {
        NullArgumentException.validateNotNull(element, "Element ");
        NullArgumentException.validateNotNull(path, "Element path");

        Element current = element;
        StringTokenizer st = new StringTokenizer(path, "/", false);
        while (st.hasMoreTokens() && current != null) {
            final String token = st.nextToken();
            final NodeList childs = current.getChildNodes();
            current = null;
            for (int i = 0; i < childs.getLength(); i++) {
                final Node child = childs.item(i);
                if (child instanceof Element && child.getNodeName().equals(token)) {
                    current = (Element) child;
                }
            }
        }
        return current;
    }

    public static List<Element> getElements(final Document doc, final String path) {
        NullArgumentException.validateNotNull(doc, "Document");
        return getElements(doc.getDocumentElement(), path);
    }

    public static List<Element> getElements(final Element element, final String path) {
        NullArgumentException.validateNotNull(element, "Element");
        NullArgumentException.validateNotNull(path, "Element path");
        String lastElement;
        Element parent;
        if (path.contains("/")) {
            parent = getElement(element, path.substring(0, path.lastIndexOf("/")));
            lastElement = path.substring(path.lastIndexOf("/") + 1);
        } else {
            parent = element;
            lastElement = path;
        }
        List<Element> elements = null;
        if (parent != null) {
            NodeList nodeList = parent.getElementsByTagName(lastElement);
            if (nodeList != null) {
                elements = new ArrayList<Element>();
                for (int i = 0; i < nodeList.getLength(); i++) {
                    elements.add((Element) nodeList.item(i));
                }
            }
        }
        return elements;
    }

    public static List<Element> getChildElements(final Element element) {
        final List<Element> elements = new ArrayList<Element>();
        final NodeList childs = element.getChildNodes();
        for (int i = 0; i < childs.getLength(); i++) {
            Node child = childs.item(i);
            if (child instanceof Element) {
                elements.add((Element) child);
            }
        }
        return elements;
    }

    public static String getTextContentOfElement(final Document doc, final String path) {
        NullArgumentException.validateNotNull(doc, "XML document");
        return getTextContentOfElement(doc.getDocumentElement(), path);
    }

    public static String getTextContentOfElement(final Element element, final String path) {
        NullArgumentException.validateNotNull(element, "Element");
        NullArgumentException.validateNotNull("Element path", path);

        StringTokenizer st = new StringTokenizer(path, "/", false);
        Element currentElement = element;
        while (st.hasMoreTokens()) {
            final String childName = st.nextToken();
            if (childName.endsWith("]")) {
                int startPos = childName.indexOf("[");
                int endPos = childName.indexOf("]");
                NodeList children = currentElement.getElementsByTagName(childName.substring(0, startPos));
                int numChildren = children.getLength();
                int index;
                String numbers = childName.substring(startPos + 1, endPos);
                if ("last".equals(numbers)) {
                    index = numChildren - 1;
                } else {
                    index = Integer.parseInt(numbers);
                }
                if (index > numChildren) {
                    throw new IllegalArgumentException(
                            "index of " + index + " is larger than the number of child nodes (" + numChildren + ")"
                    );
                }
                currentElement = (Element) children.item(index);
            } else {
                final NodeList parent = currentElement.getElementsByTagName(childName);
                if (parent != null) {
                    currentElement = (Element) parent.item(0);
                }
            }
            if (null == currentElement) {
                return null;
            }
        }
        return getTextContent(currentElement);
    }

    public static String getTextContent(final Node node) {
        switch (node.getNodeType()) {
            case Node.ELEMENT_NODE:
            case Node.ATTRIBUTE_NODE:
            case Node.ENTITY_NODE:
            case Node.ENTITY_REFERENCE_NODE:
            case Node.DOCUMENT_FRAGMENT_NODE:
                return mergeTextContent(node.getChildNodes());
            case Node.TEXT_NODE:
            case Node.CDATA_SECTION_NODE:
            case Node.COMMENT_NODE:
            case Node.PROCESSING_INSTRUCTION_NODE:
                return node.getNodeValue();
            case Node.DOCUMENT_NODE:
            case Node.DOCUMENT_TYPE_NODE:
            case Node.NOTATION_NODE:
            default:
                return null;
        }
    }

    private static String mergeTextContent(final NodeList nodes) {
        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < nodes.getLength(); i++) {
            Node n = nodes.item(i);
            final String text;

            switch (n.getNodeType()) {
                case Node.COMMENT_NODE:
                case Node.PROCESSING_INSTRUCTION_NODE:
                    // ignore comments when merging
                    text = null;
                    break;
                default:
                    text = getTextContent(n);
                    break;
            }

            if (text != null) {
                buf.append(text);
            }
        }
        return buf.toString();
    }

}
