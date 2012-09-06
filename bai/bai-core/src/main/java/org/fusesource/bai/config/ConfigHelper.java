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
package org.fusesource.bai.config;

import java.io.InputStream;
import java.io.StringWriter;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import org.apache.camel.util.ObjectHelper;

/**
 * Helper methods for working with {@link AuditConfig} objects.
 */
public class ConfigHelper {

    public static final String JAXB_CONTEXT_PACKAGES =
            "org.fusesource.bai.config:org.apache.camel.model.language";

    public static JAXBContext createConfigJaxbContext() throws JAXBException {
        return JAXBContext.newInstance(JAXB_CONTEXT_PACKAGES);
    }

    public static String toXml(AuditConfig config) throws JAXBException {
        JAXBContext context = createConfigJaxbContext();
        Marshaller marshaller = context.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
        StringWriter writer = new StringWriter();
        marshaller.marshal(config, writer);
        return writer.toString();
    }

    public static AuditConfig loadConfig(InputStream stream) throws JAXBException {
        JAXBContext context = createConfigJaxbContext();
        Unmarshaller unmarshaller = context.createUnmarshaller();
        return (AuditConfig) unmarshaller.unmarshal(stream);
    }

    public static AuditConfig loadConfigFromClassPath(String uri) throws JAXBException {
        InputStream stream = ConfigHelper.class.getClassLoader().getResourceAsStream(uri);
        ObjectHelper.notNull(stream, "Could not find '" + uri + "' on ClassLoader");
        return loadConfig(stream);
    }
}
