/*
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
package org.fusesource.bai.xml;

import com.sun.xml.internal.bind.marshaller.NamespacePrefixMapper;
import org.apache.camel.util.ObjectHelper;
import org.fusesource.bai.config.PolicySet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import java.io.InputStream;
import java.io.StringWriter;

/**
 * Helper methods for working with {@link org.fusesource.bai.config.PolicySet} objects.
 */
public class ConfigHelper {
    private static final transient Logger LOG = LoggerFactory.getLogger(ConfigHelper.class);

    protected static Schema schema;

    public static final String JAXB_CONTEXT_PACKAGES =
            "org.fusesource.bai.config:org.apache.camel.model.language";

    public static JAXBContext createConfigJaxbContext() throws JAXBException {
        return JAXBContext.newInstance(JAXB_CONTEXT_PACKAGES);
    }

    public static String toXml(PolicySet config) throws JAXBException {
        Marshaller marshaller = createMarshaller();
        StringWriter writer = new StringWriter();
        marshaller.marshal(config, writer);
        return writer.toString();
    }

    public static Marshaller createMarshaller() throws JAXBException {
        JAXBContext context = createConfigJaxbContext();
        Marshaller marshaller = context.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
        NamespacePrefixMapper mapper = new AuditNamespacePrefixMapper();
        marshaller.setProperty("com.sun.xml.internal.bind.namespacePrefixMapper", mapper);
        return marshaller;
    }

    public static PolicySet loadConfig(InputStream stream) throws JAXBException {
        JAXBContext context = createConfigJaxbContext();
        Unmarshaller unmarshaller = context.createUnmarshaller();
        Schema s = getSchema();
        if (s != null) {
            unmarshaller.setSchema(s);
        }
        return (PolicySet) unmarshaller.unmarshal(stream);
    }

    public static PolicySet loadConfigFromClassPath(String uri) throws JAXBException {
        InputStream stream = ConfigHelper.class.getClassLoader().getResourceAsStream(uri);
        ObjectHelper.notNull(stream, "Could not find '" + uri + "' on ClassLoader");
        return loadConfig(stream);
    }

    public static Schema getSchema() {
        if (schema == null) {
            SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            try {
                Source[] sources = {
                        new StreamSource(getSchemaStream("camel-spring.xsd")),
                        new StreamSource(getSchemaStream("bai.xsd"))
                };
                schema = schemaFactory.newSchema(sources);
            } catch (SAXException e) {
                LOG.error("Could not parse BAI schemas. Reason: " + e, e);
            }
        }
        return schema;
    }

    private static InputStream getSchemaStream(String uri) {
        InputStream xsdStream = ConfigHelper.class.getClassLoader().getResourceAsStream(uri);
        ObjectHelper.notNull(xsdStream, "Could not find '" + uri + "' on the classpath");
        return xsdStream;
    }
}

