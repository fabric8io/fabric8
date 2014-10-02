/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.fabric8.agent.model;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import io.fabric8.agent.internal.XmlUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLFilter;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLFilterImpl;

public final class JaxbUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(JaxbUtil.class);
    private static final JAXBContext FEATURES_CONTEXT;
    private static final Map<String, Schema> SCHEMAS = new ConcurrentHashMap<>();

    static {
        try {
            FEATURES_CONTEXT = JAXBContext.newInstance(Features.class);
        } catch (JAXBException e) {
            throw new RuntimeException(e);
        }
    }

    private JaxbUtil() {
    }

    public static void marshal(Features features, OutputStream out) throws JAXBException {
        Marshaller marshaller = FEATURES_CONTEXT.createMarshaller();

        marshaller.setProperty("jaxb.formatted.output", true);

        marshaller.marshal(features, out);
    }

    public static void marshal(Features features, Writer out) throws JAXBException {
        Marshaller marshaller = FEATURES_CONTEXT.createMarshaller();

        marshaller.setProperty("jaxb.formatted.output", true);

        marshaller.marshal(features, out);
    }


    /**
     * Read in a Features from the input stream.
     *
     * @param uri      uri to read
     * @param validate whether to validate the input.
     * @return a Features read from the input stream
     */
    public static Features unmarshal(String uri, boolean validate) {
        if (validate) {
            return unmarshalValidate(uri, null);
        } else {
            return unmarshalNoValidate(uri, null);
        }
    }

    public static Features unmarshal(String uri, InputStream stream, boolean validate) {
        if (validate) {
            return unmarshalValidate(uri, stream);
        } else {
            return unmarshalNoValidate(uri, stream);
        }
    }

    private static Features unmarshalValidate(String uri, InputStream stream) {
        try {
            Document doc;
            if (stream != null) {
                doc = XmlUtils.parse(stream);
                doc.setDocumentURI(uri);
            } else {
                doc = XmlUtils.parse(uri);
            }

            String nsuri = doc.getDocumentElement().getNamespaceURI();
            if (nsuri == null) {
                LOGGER.warn("Old style feature file without namespace found (URI: {}). This format is deprecated and support for it will soon be removed", uri);
            } else {
                Schema schema = getSchema(nsuri);
                try {
                    schema.newValidator().validate(new DOMSource(doc));
                } catch (SAXException e) {
                    throw new IllegalArgumentException("Unable to validate " + uri, e);
                }
            }

            fixDom(doc, doc.getDocumentElement());
            Unmarshaller unmarshaller = FEATURES_CONTEXT.createUnmarshaller();
            return (Features) unmarshaller.unmarshal(new DOMSource(doc));


        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Unable to load " + uri, e);
        }
    }

    private static Schema getSchema(String namespace) throws SAXException {
        Schema schema = SCHEMAS.get(namespace);
        if (schema == null) {
            String schemaLocation;
            switch (namespace) {
            case FeaturesNamespaces.URI_1_0_0:
                schemaLocation = "/org/apache/karaf/features/karaf-features-1.0.0.xsd";
                break;
            case FeaturesNamespaces.URI_1_1_0:
                schemaLocation = "/org/apache/karaf/features/karaf-features-1.1.0.xsd";
                break;
            case FeaturesNamespaces.URI_1_2_0:
                schemaLocation = "/org/apache/karaf/features/karaf-features-1.2.0.xsd";
                break;
            case FeaturesNamespaces.URI_1_3_0:
                schemaLocation = "/org/apache/karaf/features/karaf-features-1.3.0.xsd";
                break;
            default:
                throw new IllegalArgumentException("Unsupported namespace: " + namespace);
            }

            SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            // root element has namespace - we can use schema validation
            URL url = JaxbUtil.class.getResource(schemaLocation);
            if (url == null) {
                throw new IllegalStateException("Could not find resource: " + schemaLocation);
            }
            schema = factory.newSchema(new StreamSource(url.toExternalForm()));
            SCHEMAS.put(namespace, schema);
        }
        return schema;
    }


    private static void fixDom(Document doc, Node node) {
        if (node.getNodeType() == Node.ELEMENT_NODE) {
            if (!FeaturesNamespaces.URI_CURRENT.equals(node.getNamespaceURI())) {
                doc.renameNode(node, FeaturesNamespaces.URI_CURRENT, node.getLocalName());
            }
            NodeList children = node.getChildNodes();
            for (int i = 0; i < children.getLength(); i++) {
                fixDom(doc, children.item(i));
            }
        }
    }

    private static Features unmarshalNoValidate(String uri, InputStream stream) {
        try {
            Unmarshaller unmarshaller = FEATURES_CONTEXT.createUnmarshaller();
            XMLFilter xmlFilter = new NoSourceAndNamespaceFilter(XmlUtils.xmlReader());
            xmlFilter.setContentHandler(unmarshaller.getUnmarshallerHandler());

            InputSource is = new InputSource(uri);
            if (stream != null) {
                is.setByteStream(stream);
            }
            SAXSource source = new SAXSource(xmlFilter, new InputSource(uri));
            return (Features) unmarshaller.unmarshal(source);

        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Unable to load " + uri, e);
        }
    }

    /**
     * Provides an empty inputsource for the entity resolver.
     * Converts all elements to the features namespace to make old feature files
     * compatible to the new format
     */
    public static class NoSourceAndNamespaceFilter extends XMLFilterImpl {
        private static final InputSource EMPTY_INPUT_SOURCE = new InputSource(new ByteArrayInputStream(new byte[0]));

        public NoSourceAndNamespaceFilter(XMLReader xmlReader) {
            super(xmlReader);
        }

        @Override
        public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException {
            return EMPTY_INPUT_SOURCE;
        }

        @Override
        public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
            super.startElement(FeaturesNamespaces.URI_CURRENT, localName, qName, atts);
        }

        @Override
        public void endElement(String uri, String localName, String qName) throws SAXException {
            super.endElement(FeaturesNamespaces.URI_CURRENT, localName, qName);
        }
    }

}
