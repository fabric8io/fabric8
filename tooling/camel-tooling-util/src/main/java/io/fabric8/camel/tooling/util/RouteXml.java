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

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerFactory;

import de.pdark.decentxml.Attribute;
import de.pdark.decentxml.Document;
import de.pdark.decentxml.Element;
import de.pdark.decentxml.Namespace;
import de.pdark.decentxml.Node;
import de.pdark.decentxml.NodeWithChildren;
import de.pdark.decentxml.XMLIOSource;
import de.pdark.decentxml.XMLSource;
import de.pdark.decentxml.XMLStringSource;
import de.pdark.decentxml.XMLWriter;
import io.fabric8.camel.tooling.util.parser.PatchedXMLParser;
import org.apache.camel.CamelContext;
import org.apache.camel.model.Constants;
import org.apache.camel.model.ModelCamelContext;
import org.apache.camel.model.RouteDefinition;
import org.apache.camel.model.RoutesDefinition;
import org.apache.camel.spring.CamelContextFactoryBean;
import org.apache.camel.spring.CamelRouteContextFactoryBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.ErrorHandler;

import static io.fabric8.camel.tooling.util.CamelNamespaces.*;

/**
 * Helper class for loading and saving XML for use at design time
 */
public class RouteXml {

    public static Logger LOG = LoggerFactory.getLogger(RouteXml.class);

    private JAXBContext _jaxbContext;
    private ClassLoader classLoader = CamelContextFactoryBean.class.getClassLoader();

    protected TransformerFactory transformerFactory = TransformerFactory.newInstance();
    protected DocumentBuilder documentBuilder = createDocumentBuilder();

    protected DocumentBuilder documentBuilder(ErrorHandler handler) {
        DocumentBuilder db = createDocumentBuilder();
        db.setErrorHandler(handler);
        return db;
    }

    public JAXBContext jaxbContext() throws JAXBException {
        if (_jaxbContext == null) {
            String packageName = Constants.JAXB_CONTEXT_PACKAGES + ":org.apache.camel.spring";
            _jaxbContext = JAXBContext.newInstance(packageName, classLoader);
        }
        return _jaxbContext;
    }

    public void setJaxbContext(JAXBContext jaxbContext) {
        this._jaxbContext = jaxbContext;
    }

    protected Document createExemplarDoc() throws IOException {
        String exemplar = "io/fabric8/camel/tooling/exemplar.xml";
        URL url = findResource(exemplar, null);
        if (url != null) {
            return parse(new XMLIOSource(url));
        } else {
            LOG.warn("Could not find file {} on the class path", exemplar);
            Document d = new Document();
            d.addNode(new Element("beans", springNamespace));
            return d;
        }
    }

    private Document parse(XMLSource source) {
        PatchedXMLParser parser = new PatchedXMLParser();
        return parser.parse(source);
    }

    public XmlModel unmarshal(File file) throws Exception {
        Document doc;
        if (file.exists()) {
            doc = parse(new XMLIOSource(file));
/*
      // lets find the header stuff
      val root = doc.getRootElement
      if (root != null) {
        val name = root.getNodeName()
        println("====== node name is: " + name)
        val text = IOUtil.loadTextFile(file)
        val idx = text.indexOf("<" + name)
        if (idx > 0) {
          header = text.substring(0, idx)
          println("header: " + header)
        }
      }
*/
        } else {
            doc = createExemplarDoc();
        }

        return unmarshal(doc, "XML File " + file);
    }

    public XmlModel unmarshal(String text) throws Exception {
        Document doc;
        if (text != null && text.trim().length() > 0) {
            doc = parse(new XMLStringSource(text));
        } else {
            doc = createExemplarDoc();
        }
        return unmarshal(doc, "Text");
    }

    public XmlModel unmarshal(Document doc) throws Exception {
        return unmarshal(doc, "XML document " + doc);
    }

    public XmlModel unmarshal(Document doc, String message) throws Exception {
        Unmarshaller unmarshaller = jaxbContext().createUnmarshaller();

        // ("bean", springNamespace)
        Map<String, String> beans = new HashMap<String, String>();

        // lets pull out the spring beans...
        // TODO: shouldn't we use http://www.springframework.org/schema/beans namespace instead??
        List<Node> beanElems = nodesByNamespace(doc, springNS, "bean");

        for (Node n: beanElems) {
            if (n instanceof Element) {
                String id = ((Element) n).getAttributeValue("id");
                String cn = ((Element) n).getAttributeValue("class");
                if (id != null && cn != null) {
                    beans.put(id, cn);
                }
            }
        }

        // now lets pull out the jaxb routes...
        List<String[]> search = Arrays.asList(
            new String[] { springNS, "routeContext" },
            new String[] { springNS, "camelContext" },
            new String[] { springNS, "routes" },
            new String[] { blueprintNS, "routeContext" },
            new String[] { blueprintNS, "camelContext" },
            new String[] { blueprintNS, "routes" }
        );

        List<Node> found = new LinkedList<Node>();

        for (String[] pair : search) {
            List<Node> nodes = nodesByNamespace(doc, pair[0], pair[1]);
            int n = nodes.size();
            if (n != 0) {
                if (n > 1) {
                    LOG.warn(message + " contains " + n + " <" + pair[1] + "> elements. Only the first one will be used");
                }
                Node node = nodes.get(0);
                found.add(node);
            }
        }

        if (found.size() > 0) {
            Node n = found.get(0);
            if (n != null) {
                String ns = getNamespaceURI(n);
                Node parseNode;
                if (!ns.equals(springNS)) {
                    parseNode = cloneAndReplaceNamespace(n, ns, springNS);
                } else {
                    parseNode = n;
                }

                boolean justRoutes = false;
                boolean routesContext = false;
                String xmlText = nodeWithNamespacesToText(parseNode, (Element) n);
                Object object = unmarshaller.unmarshal(new StringReader(xmlText));
                CamelContextFactoryBean sc;
                if (object instanceof CamelContextFactoryBean) {
                    LOG.debug("Found a valid CamelContextFactoryBean! {}", object);
                    sc = (CamelContextFactoryBean) object;
                } else if (object instanceof RoutesDefinition) {
                    justRoutes = true;
                    sc = new CamelContextFactoryBean();
                    sc.setRoutes(((RoutesDefinition) object).getRoutes());
                } else if (object instanceof CamelRouteContextFactoryBean) {
                    routesContext = true;
                    sc = new CamelContextFactoryBean();
                    sc.setRoutes(((CamelRouteContextFactoryBean) object).getRoutes());
                } else if (object instanceof org.apache.camel.blueprint.CamelRouteContextFactoryBean) {
                    routesContext = true;
                    sc = new CamelContextFactoryBean();
                    sc.setRoutes(((org.apache.camel.blueprint.CamelRouteContextFactoryBean) object).getRoutes());
                } else {
                    LOG.warn("Unmarshalled not a CamelContext: {}", object);
                    sc = new CamelContextFactoryBean();
                }
                return new XmlModel(sc, doc, beans, n, ns, justRoutes, routesContext);
            } else {
                LOG.info(message + " does not contain a CamelContext. Maybe the XML namespace is not spring: '{}' or blueprint: '{}'?", springNS, blueprintNS);
                // lets create a new collection
                return new XmlModel(new CamelContextFactoryBean(), doc, beans, null, CamelNamespaces.springNS, false, false);
            }
        }
        return null; // ?
    }

    protected Node cloneAndReplaceNamespace(Node node, String oldNS, String newNS) {
        Node answer = node.copy();
        return replaceNamespace(answer, oldNS, newNS);
    }

    protected Node replaceNamespace(Node node, String oldNS, String newNS) {
        if (node instanceof Element) {
            String ns = getNamespaceURI(node);
            if (ns != null && ns.equals(oldNS)) {
                Namespace namespace = ((Element) node).getNamespace();
                if (namespace != null) {
                    if (namespace.getURI() != null && namespace.getURI().equals(oldNS)) {
                        ((Element) node).setNamespace(new Namespace(namespace.getPrefix(), newNS));
                    }
                }

                for (Attribute attr : ((Element) node).getAttributes()) {
                    if (attr.getName().startsWith("xmlns")) {
                        String value = attr.getValue();
                        if (value != null && value.equals(oldNS)) {
                            attr.setValue(newNS);
                        }
                    }
                }
            }
        }

        if (node instanceof NodeWithChildren) {
            for (Node n : ((NodeWithChildren) node).getNodes()) {
                replaceNamespace(n, oldNS, newNS);
            }
        }

        return node;
    }

    public void marshal(File file, final CamelContextFactoryBean context) throws Exception {
        marshal(file, new Model2Model() {
            @Override
            public XmlModel transform(XmlModel model) {
                model.update(context);
                return model;
            }
        });
    }

    public void marshal(File file, final CamelContext context) throws Exception {
        marshal(file, new Model2Model() {
            @Override
            public XmlModel transform(XmlModel model) {
                copyRoutesToElement(context, model.getContextElement());
                return model;
            }
        });
    }

    public void copyRoutesToElement(List<RouteDefinition> routeDefinitionList, CamelContextFactoryBean contextElement) {
        List<RouteDefinition> routes = contextElement.getRoutes();
        routes.clear();
        routes.addAll(routeDefinitionList);
    }

    public void copyRoutesToElement(CamelContext context, CamelContextFactoryBean contextElement) {
        if (context instanceof ModelCamelContext) {
            copyRoutesToElement(((ModelCamelContext)context).getRouteDefinitions(), contextElement);
        } else {
            LOG.error("Invalid camel context! ({})", context.getClass().getName());
        }
    }

    /**
     * Loads the given file then updates the route definitions from the given list then stores the file again
     */
    public void marshal(File file, final List<RouteDefinition> routeDefinitionList) throws Exception {
        marshal(file, new Model2Model() {
            @Override
            public XmlModel transform(XmlModel model) {
                copyRoutesToElement(routeDefinitionList, model.getContextElement());
                return model;
            }
        });
    }

    public void marshal(File file, Model2Model transformer) throws Exception {
        // lets load the file first in case its been edited since we last loaded it
        XmlModel model = unmarshal(file);
        marshal(file, transformer.transform(model));
    }

    public String marshalToText(String text, final List<RouteDefinition> routeDefinitionList) throws Exception {
        return marshalToText(text, new Model2Model() {
            @Override
            public XmlModel transform(XmlModel model) {
                copyRoutesToElement(routeDefinitionList, model.getContextElement());
                return model;
            }
        });
    }

    public String marshalToText(String text, Model2Model transformer) throws Exception {
        XmlModel model = unmarshal(text);
        return marshalToText(transformer.transform(model));
    }

    public void marshal(File file, XmlModel model) throws JAXBException, IOException {
        marshalToDoc(model);
        writeXml(model.getDoc(), file);
    }

    public String marshalToText(XmlModel model) throws JAXBException, IOException {
        marshalToDoc(model);
        return xmlToText(model.getDoc());
    }

    protected void replaceCamelElement(Element docElem, Node camelElem, Node oldNode) {
        replaceChild(docElem, camelElem, oldNode);

        // lets replace the camel namespace, copying any namespace from the old node as well
        if (camelElem instanceof Element && oldNode instanceof Element) {
            for (Attribute attr : ((Element) oldNode).getAttributes()) {
                if (attr.getName().startsWith("xmlns")) {
                    ((Element) camelElem).setAttribute(attr.getName(), attr.getValue());
                }
            }
        }
    }

    /**
     * Marshals the model to XML and updates the model's doc property to contain the
     * new marshalled model
     *
     * @param model
     */
    public void marshalToDoc(XmlModel model) throws JAXBException {
        Marshaller marshaller = jaxbContext().createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, java.lang.Boolean.TRUE);
        try {
            marshaller.setProperty("com.sun.xml.bind.indentString", "  ");
        } catch (Exception e) {
            LOG.debug("Property not supported: {}", e);
        }

        Object value = model.marshalRootElement();
        Document doc = model.getDoc();
        Element docElem = doc.getRootElement();

        // JAXB only seems to do nice whitespace/namespace stuff when writing to stream
        // rather than DOM directly
        // marshaller.marshal(value, docElem);

        StringWriter buffer = new StringWriter();
        marshaller.marshal(value, buffer);

        // now lets parse the XML and insert the root element into the doc
        String xml = buffer.toString();
        if (!model.getNs().equals(springNS)) {
            // !!!
            xml = xml.replaceAll(springNS, model.getNs());
        }
        Document camelDoc = parse(new XMLStringSource(xml));
        Node camelElem = camelDoc.getRootElement();

        // TODO
        //val camelElem = doc.importNode(element, true)

        if (model.isRoutesContext() && camelDoc.getRootElement().getName().equals("camelContext")) {
            camelDoc.getRootElement().setName("routeContext");
        }
        if (model.isJustRoutes()) {
            replaceChild(doc, camelElem, docElem);
        } else {
            if (model.getNode() != null) {
                replaceCamelElement(docElem, camelElem, model.getNode());
            } else {
                docElem.addNode(camelElem);
            }
        }
    }

    public void writeXml(Document doc, File file) throws IOException {
        File parentDir = file.getParentFile();
        if (parentDir != null) {
            parentDir.mkdirs();
        }

        XMLWriter writer = new XMLWriter(new FileWriter(file));
        doc.toXML(writer);
        writer.close();
    }

    protected DocumentBuilder createDocumentBuilder() {
//        String JAXP_SCHEMA_LANGUAGE = "http://java.sun.com/xml/jaxp/properties/schemaLanguage";
//        String JAXP_SCHEMA_SOURCE = "http://java.sun.com/xml/jaxp/properties/schemaSource";
//        String W3C_XML_SCHEMA = "http://www.w3.org/2001/XMLSchema";

        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();

//        boolean validating = false;
//        if (validating) {
//            try {
//                dbf.setAttribute(JAXP_SCHEMA_LANGUAGE, W3C_XML_SCHEMA);
//                dbf.setAttribute(JAXP_SCHEMA_SOURCE, CamelNamespaces.camelSchemas());
//                dbf.setValidating(validating);
//            } catch (Exception e) {
//                // ignore
//            }
//        }

        dbf.setExpandEntityReferences(false);
        dbf.setIgnoringComments(false);
        dbf.setIgnoringElementContentWhitespace(false);
        dbf.setCoalescing(false);
        dbf.setNamespaceAware(true);
        try {
            return dbf.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }


    /**
     * XmlModel => XmlModel
     */
    private static interface Model2Model {

        public XmlModel transform(XmlModel model);

    }

}
