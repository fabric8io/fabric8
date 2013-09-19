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

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.Text;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;

/**
 * A collection of helper methods for working with the DOM API
 */
public class DomHelper {

    private static TransformerFactory transformerFactory;
    private static Transformer transformer;

    public static void save(Document document, File file) throws FileNotFoundException, TransformerException {
        Transformer transformer = getTransformer();
        transformer.transform(new DOMSource(document), new StreamResult(new FileOutputStream(file)));
    }

    public static Transformer getTransformer() throws TransformerConfigurationException {
        if (transformer == null) {
            transformer = getTransformerFactory().newTransformer();
        }
        return transformer;
    }

    public static void setTransformer(Transformer transformer) {
        DomHelper.transformer = transformer;
    }

    public static TransformerFactory getTransformerFactory() {
        if (transformerFactory == null){
            transformerFactory = TransformerFactory.newInstance();
        }
        return transformerFactory;
    }

    public static void setTransformerFactory(TransformerFactory transformerFactory) {
        DomHelper.transformerFactory = transformerFactory;
    }

    /**
     * If the node is attached to a parent then detach it
     */
    public static void detach(Node node) {
        if (node != null) {
            Node parentNode = node.getParentNode();
            if (parentNode != null) {
                parentNode.removeChild(node);
            }
        }
    }

    /**
     * Replaces the old node with the new node
     */
    public static void replaceWith(Node oldNode, Node newNode) {
        Node parentNode = oldNode.getParentNode();
        if (parentNode != null) {
            parentNode.replaceChild(newNode, oldNode);
        }
    }

    /**
     * Removes any previous siblings text nodes
     */
    public static void removePreviousSiblingText(Element element) {
        while (true) {
            Node sibling = element.getPreviousSibling();
            if (sibling instanceof Text) {
                detach(sibling);
            } else {
                break;
            }
        }
    }

    /**
     * Removes any next siblings text nodes
     */
    public static void removeNextSiblingText(Element element) {
        while (true) {
            Node sibling = element.getNextSibling();
            if (sibling instanceof Text) {
                detach(sibling);
            } else {
                break;
            }
        }
    }
}
