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

import org.apache.camel.model.language.ConstantExpression;
import org.apache.camel.model.language.ELExpression;
import org.apache.camel.model.language.ExpressionDefinition;
import org.apache.camel.model.language.GroovyExpression;
import org.apache.camel.model.language.HeaderExpression;
import org.apache.camel.model.language.JXPathExpression;
import org.apache.camel.model.language.JavaScriptExpression;
import org.apache.camel.model.language.LanguageExpression;
import org.apache.camel.model.language.MethodCallExpression;
import org.apache.camel.model.language.MvelExpression;
import org.apache.camel.model.language.NamespaceAwareExpression;
import org.apache.camel.model.language.OgnlExpression;
import org.apache.camel.model.language.PhpExpression;
import org.apache.camel.model.language.PropertyExpression;
import org.apache.camel.model.language.PythonExpression;
import org.apache.camel.model.language.RubyExpression;
import org.apache.camel.model.language.SimpleExpression;
import org.apache.camel.model.language.SpELExpression;
import org.apache.camel.model.language.SqlExpression;
import org.apache.camel.model.language.TokenizerExpression;
import org.apache.camel.model.language.XPathExpression;
import org.apache.camel.model.language.XQueryExpression;
import org.apache.camel.util.ObjectHelper;
import org.fusesource.bai.config.BodyExpression;
import org.fusesource.bai.config.ContextFilter;
import org.fusesource.bai.config.ContextsFilter;
import org.fusesource.bai.config.EndpointFilter;
import org.fusesource.bai.config.EndpointsFilter;
import org.fusesource.bai.config.EventFilter;
import org.fusesource.bai.config.EventType;
import org.fusesource.bai.config.EventsFilter;
import org.fusesource.bai.config.ExchangeFilter;
import org.fusesource.bai.config.Policy;
import org.fusesource.bai.config.PolicySet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.PropertyException;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.StringWriter;

/**
 * Helper methods for working with {@link org.fusesource.bai.config.PolicySet} objects.
 */
public class ConfigHelper {
    private static final transient Logger LOG = LoggerFactory.getLogger(ConfigHelper.class);

    protected static Schema schema;
    private static boolean initialised = false;

    public static final Class[] JAXB_CLASSES = {
            // BAI classes
            BodyExpression.class,
            ContextFilter.class,
            ContextsFilter.class,
            EndpointFilter.class,
            EndpointsFilter.class,
            EventFilter.class,
            EventsFilter.class,
            EventType.class,
            ExchangeFilter.class,
            Policy.class,
            PolicySet.class,

            // Camel language classes
            ConstantExpression.class,
            ELExpression.class,
            ExpressionDefinition.class,
            GroovyExpression.class,
            HeaderExpression.class,
            JavaScriptExpression.class,
            JXPathExpression.class,
            LanguageExpression.class,
            MethodCallExpression.class,
            MvelExpression.class,
            NamespaceAwareExpression.class,
            OgnlExpression.class,
            PhpExpression.class,
            PropertyExpression.class,
            PythonExpression.class,
            RubyExpression.class,
            SimpleExpression.class,
            SpELExpression.class,
            SqlExpression.class,
            TokenizerExpression.class,
            XPathExpression.class,
            XQueryExpression.class
    };

    public static JAXBContext createConfigJaxbContext() throws JAXBException {
        return JAXBContext.newInstance(JAXB_CLASSES);
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
        addNamespacePrefixMapper(marshaller);
        return marshaller;
    }

    private static void addNamespacePrefixMapper(Marshaller marshaller) throws PropertyException {
        try {
            AuditNamespacePrefixMapper mapper = new AuditNamespacePrefixMapper();
            marshaller.setProperty("com.sun.xml.bind.namespacePrefixMapper", mapper);
        } catch (Throwable e) {
            // ignore due to class loader issues
        }
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

    public static PolicySet loadConfig(File file) throws FileNotFoundException, JAXBException {
        return loadConfig(new FileInputStream(file));
    }

    public static Schema getSchema() {
        if (schema == null && !initialised) {
            initialised = true;
            SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            try {
                Source[] sources = {
                        new StreamSource(getSchemaStream("camel-spring.xsd")),
                        new StreamSource(getSchemaStream("bai.xsd"))
                };
                schema = schemaFactory.newSchema(sources);
            } catch (Exception e) {
                LOG.error("Could not parse BAI schemas. Reason: " + e, e);
            }
        }
        return schema;
    }

    public static Schema getOrLoadSchema(Source[] sources) {
        if (schema == null) {
            SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            try {
                schema = schemaFactory.newSchema(sources);
            } catch (SAXException e) {
                LOG.error("Could not parse BAI schemas. Reason: " + e, e);
            }
        }
        return schema;
    }

    public static void setSchema(Schema schema) {
        ConfigHelper.schema = schema;
    }

    private static InputStream getSchemaStream(String uri) {
        InputStream xsdStream = ConfigHelper.class.getClassLoader().getResourceAsStream(uri);
        ObjectHelper.notNull(xsdStream, "Could not find '" + uri + "' on the classpath");
        return xsdStream;
    }

}

