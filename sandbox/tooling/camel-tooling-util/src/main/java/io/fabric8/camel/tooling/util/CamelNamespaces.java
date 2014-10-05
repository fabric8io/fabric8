/**
 *  Copyright 2005-2014 Red Hat, Inc.
 *
 *  Red Hat licenses this file to you under the Apache License, version
 *  2.0 (the "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 *  implied.  See the License for the specific language governing
 *  permissions and limitations under the License.
 */
package io.fabric8.camel.tooling.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.xml.XMLConstants;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import de.pdark.decentxml.Attribute;
import de.pdark.decentxml.Comment;
import de.pdark.decentxml.Document;
import de.pdark.decentxml.Element;
import de.pdark.decentxml.Namespace;
import de.pdark.decentxml.Node;
import de.pdark.decentxml.NodeFilter;
import de.pdark.decentxml.NodeWithChildren;
import de.pdark.decentxml.Parent;
import de.pdark.decentxml.Text;
import de.pdark.decentxml.Token;
import de.pdark.decentxml.XMLWriter;
import org.apache.camel.spring.CamelEndpointFactoryBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.FileCopyUtils;
import org.xml.sax.SAXException;

public class CamelNamespaces {

    public static Logger LOG = LoggerFactory.getLogger(CamelNamespaces.class);

    // All these statics are the sign, that Scala's "object" is not that excellent after all...

    public static final String springNS = "http://camel.apache.org/schema/spring";
    public static final String blueprintNS = "http://camel.apache.org/schema/blueprint";

    public static final String[] camelNamespaces = new String[] { springNS, blueprintNS };

    public static final Namespace springNamespace = new Namespace("", "http://www.springframework.org/schema/beans");
    public static final Namespace droolsNamespace = new Namespace("drools", "http://drools.org/schema/drools-spring");

    private static Schema _schema;

    private static Set<String> elementsWithDescription;

    public static Set<String> elementsWithDescription() {
        if (elementsWithDescription == null) {
            String file = "camelDescriptionElements.txt";
            String text = "";
            URL url = findResource(file, null);
            if (url != null) {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                try {
                    FileCopyUtils.copy(url.openStream(), baos);
                    text = new String(baos.toByteArray(), "UTF-8");
                } catch (IOException e) {
                    throw new RuntimeException(e.getMessage(), e);
                }
            } else {
                LOG.warn("Could not find file {} on the class path", file);
            }
            text += " camelContext";
            String[] arr = text.split(" ");

            elementsWithDescription = new HashSet<String>(Arrays.asList(arr));
        }
        return elementsWithDescription;
    }

    public static List<Node> nodesByNamespace(Document doc, final String namespaceUri, final String localName) {
        NodeFilter<Node> filter = new NodeFilter<Node>() {
            @Override
            public boolean matches(Node n) {
                if (n instanceof Element) {
                    Namespace ns = ((Element) n).getNamespace();
                    // TODO this doesn't work with empty prefixes!
                    if (!((Element) n).getName().equals(localName)) {
                        return false;
                    } else {
                        String uri = getNamespaceURI(n);
                        return ns != null;// && namespaceUri.equals(uri); // (!)
                    }
                }
                return false;
            }
        };
        return findNodes(doc, filter);
    }

    public static List<Node> findNodes(NodeWithChildren node, NodeFilter<Node> filter) {
        List<Node> answer = node.getNodes(filter);
        List<Node> children = node.getNodes();
        for (Node child: children) {
            if (child instanceof Element) {
                List<Node> childMatched = findNodes((Element) child, filter);
                answer.addAll(childMatched);
            }
        }
        return answer;
    }

    public static String nodeWithNamespacesToText(Node parseNode, Element namespacesNode) throws IOException {
        // we need to add any namespace prefixes defined in the root directory
        Node copy = parseNode.copy();
        if (copy instanceof Element && parseNode instanceof Element) {
            moveCommentsIntoDescriptionElements((Element) copy, (Element) parseNode);
            addParentNamespaces((Element) copy, namespacesNode.getParent());
        }
        return xmlToText(copy);
    }

    public static void moveCommentsIntoDescriptionElements(Element e, Element root) {
        // lets iterate through finding all comments which are then added to a description node
        int idx = 0;
        List<Node> nodes = e.getNodes();
        for (Node node: nodes.toArray(new Node[nodes.size()])) {
            if (node instanceof Comment) {
                Comment c = (Comment) node;
                Token token = c.getToken();
                if (token != null) {
                    String text = token.getText().trim().replace("<!--", "").replace("-->", "").trim();
                    Element descr = findOrCreateDescriptionOnNextElement(e, idx, root);
                    if (descr == null) {
                        // lets move the comment node to before the root element...
                        LOG.warn("No description node found");
                        e.removeNode(c);
                        Parent grandParent = root.getParent();
                        if (grandParent != null) {
                            grandParent.addNode(grandParent.nodeIndexOf(root), c);
                        } else {
                            LOG.warn("Cannot save the comment '{}' as there's no parent in the DOM", text);
                        }
                    } else {
                        if (descr.getNodes().size() > 0) {
                            text = "\n" + text;
                        }
                        descr.addNode(new Text(text));
                    }
                }
            } else if (node instanceof Element) {
                moveCommentsIntoDescriptionElements((Element) node, root);
            }
            idx++;
        }
    }

    protected static Element findOrCreateDescriptionOnNextElement(Element element, int commentIndex, Parent root) {
        // lets find the next peer element node and if it can contain a description lets use that
        List<Node> nodes = element.getNodes();
        Node[] array = nodes.toArray(new Node[nodes.size()]);
        for (int i=commentIndex + 1; i<array.length; i++) {
            if (array[i] instanceof Element) {
                if (elementsWithDescription().contains(element.getName())) {
                    return findOrCreateDescrptionElement((Element) array[i], root);
                }
            }
        }

        return findOrCreateDescrptionElement(element, root);
    }

    protected static Element findOrCreateDescrptionElement(Element element, Parent root) {
        for (Node node : element.getNodes()) {
            if (node instanceof Element) {
                if ("description".equals(((Element) node).getName())) {
                    return (Element) node;
                }
            }
        }

        Element parent = element.getParentElement();
        if (element == root || parent == null || parent == root) {
            return null;
        } else if (elementsWithDescription.contains(element.getName())) {
            // lets check for a namespace prefix
            String ebn = element.getBeginName();
            int idx = ebn.indexOf(":");
            String name = idx > 0 ? ebn.substring(0, idx + 1) + "description" : "description";
            Element description = new Element(name, element.getNamespace());

            element.addNode(0, description);
            return description;
        } else {
            return findOrCreateDescrptionElement(parent, root);
        }
    }

    public static String xmlToText(Node node) throws IOException {
        StringWriter buffer = new StringWriter();
        XMLWriter writer = new XMLWriter(buffer);
        node.toXML(writer);
        writer.close();

        return buffer.toString();
    }

    public static String getNamespaceURI(Node node) {
        if (node instanceof Element) {
            Namespace ns = ((Element) node).getNamespace();
            if (ns != null) {
                String uri = ns.getURI();
                if (uri == null || uri.length() == 0) {
                    String uriAttr = ns.getPrefix().equals("") ? ((Element) node).getAttributeValue("xmlns") : null;
                    if (uriAttr != null) {
                        return uriAttr;
                    } else {
                        return getNamespaceURI(((Element) node).getParent());
                    }
                } else {
                    return uri;
                }
            }
        }
        return null;
    }

    public static void addParentNamespaces(Element element, Parent parent) {
        if (parent instanceof Element) {
            for (Attribute attr : ((Element) parent).getAttributes()) {
                String name = attr.getName();
                if (name.startsWith("xmlns") && element.getAttributeValue(name) == null) {
                    element.setAttribute(name, attr.getValue());
                }
            }
            addParentNamespaces(element, ((Element) parent).getParent());
        }
    }

    public static Document getOwnerDocument(Node node) {
        if (node instanceof Element) {
            return ((Element) node).getDocument();
        } else if (node instanceof Document) {
            return (Document) node;
        }
        return null;
    }

    public static void replaceChild(Parent parent, Node newChild, Node oldNode) {
        int idx = parent.nodeIndexOf(oldNode);
        if (idx < 0) {
            parent.addNode(newChild);
        } else {
            parent.removeNode(idx);
            parent.addNode(idx, newChild);
        }
    }

    public static URL findResource(String name, Iterable<ClassLoader> classLoaders) {
        if (classLoaders == null) {
            classLoaders = Arrays.asList(
                Thread.currentThread().getContextClassLoader(),
                CamelNamespaces.class.getClassLoader()
            );
        }

        for (ClassLoader cl : classLoaders) {
            URL resource = tryLoadClass(cl, name);
            if (resource != null) {
                return resource;
            }
        }
        return null;
    }

    private static URL tryLoadClass(ClassLoader classLoader, String name) {
        try {
           return classLoader.getResource(name);
        } catch (Exception e) {
            return null;
        }
    }

    public void loadSchemasWith(final SchemaFinder finder) throws IOException, SAXException {
        loadSchemas(new SchemaFinder() {
            @Override
            public URL findSchema(XsdDetails xsd) {
                return finder.findSchema(xsd);
            }
        });
    }

    private static void loadSchemas(SchemaFinder loader) throws SAXException, IOException {
        SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);

        XsdDetails[] xsds = new XsdDetails[] {
            new XsdDetails("camel-spring.xsd", "http://camel.apache.org/schema/spring/camel-spring.xsd", CamelEndpointFactoryBean.class),
            new XsdDetails("camel-blueprint.xsd", "http://camel.apache.org/schema/blueprint/camel-blueprint.xsd", org.apache.camel.blueprint.CamelEndpointFactoryBean.class)
        };

        List<Source> sources = new ArrayList<Source>(xsds.length);

        for (XsdDetails xsdd : xsds) {
            URL url = loader.findSchema(xsdd);
            if (url != null) {
                sources.add(new StreamSource(url.openStream(), xsdd.getUri()));
            } else {
                System.out.println("Warning could not find local resource " + xsdd.getPath() + " on classpath");
                sources.add(new StreamSource(xsdd.getUri()));
            }
        }

        _schema = factory.newSchema(sources.toArray(new Source[sources.size()]));
    }

    public static Schema camelSchemas() throws IOException, SAXException {
        if (_schema == null) {
            loadSchemas(new SchemaFinder() {
                @Override
                public URL findSchema(XsdDetails details) {
                    return details.getClassLoader().getResource(details.getPath());
                }
            });
        }
        return _schema;
    }

}
