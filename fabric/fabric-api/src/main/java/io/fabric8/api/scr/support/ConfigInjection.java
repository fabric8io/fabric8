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
package io.fabric8.api.scr.support;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ConfigInjection {

    private static final Logger LOG = LoggerFactory.getLogger(ConfigInjection.class);

    public static <T> void applyConfiguration(Map<String, ?> configuration, T target) throws Exception {
        try {
            DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
            Class<?> clazz = target.getClass();
            while (clazz != null && clazz != Object.class) {
                applyConfiguration(target, clazz, configuration, documentBuilder);
                clazz = clazz.getSuperclass();
            }
        } catch (Exception e) {
            LOG.error("Failed to inject configuration " + configuration + " into " + target, e);
        }
    }

    private static <T> void applyConfiguration(T target, Class<?> clazz, Map<String, ?> configuration, DocumentBuilder documentBuilder) throws Exception {
        Set<String> injectedNames = new HashSet<String>();
        ClassLoader classLoader = clazz.getClassLoader();

        // try parse the OSGI MetaType XML file
        String metaTypeXml = "OSGI-INF/metatype/" + clazz.getName() + ".xml";
        Document document = loadClassPathXmlDocument(documentBuilder, classLoader, metaTypeXml);
        if (document != null) {
            NodeList ads = document.getElementsByTagName("AD");
            int length = ads.getLength();
            for (int i = 0; i < length; i++) {
                Node item = ads.item(i);
                if (item instanceof Element) {
                    Element element = (Element) item;
                    String propertyName = element.getAttribute("id");
                    String defaultValue = element.getAttribute("default");
                    if (!Strings.isNullOrBlank(propertyName) && injectedNames.add(propertyName)) {
                        injectMetaTypePropertyValue(clazz, target, propertyName, defaultValue, configuration);
                    }
                }
            }
        }

        // try parse the OSGI Declarative Services XML file
        String dsXml = "OSGI-INF/" + clazz.getName() + ".xml";
        document = loadClassPathXmlDocument(documentBuilder, classLoader, dsXml);
        if (document != null) {
            NodeList ads = document.getElementsByTagName("property");
            int length = ads.getLength();
            for (int i = 0; i < length; i++) {
                Node item = ads.item(i);
                if (item instanceof Element) {
                    Element element = (Element) item;
                    String propertyName = element.getAttribute("name");
                    String defaultValue = element.getAttribute("value");
                    if (!Strings.isNullOrBlank(propertyName) && injectedNames.add(propertyName)) {
                        injectMetaTypePropertyValue(clazz, target, propertyName, defaultValue, configuration);
                    }
                }
            }
        }
    }

    private static Document loadClassPathXmlDocument(DocumentBuilder documentBuilder, ClassLoader classLoader, String path) throws IOException, SAXException {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Looking for " + path);
        }
        URL resource = null;
        try {
            resource = classLoader.getResource(path);
        } catch (Exception e) {
            LOG.warn("Failed to find " + path + " " + e);
        }
        Document document = null;
        if (resource != null) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Found resource " + resource);
            }
            InputStream in = null;
            try {
                in = resource.openStream();
                document = documentBuilder.parse(in, resource.toString());
            } finally {
                if (in != null) {
                    try {
                        in.close();
                    } catch (Exception e) {
                        // ignore
                    }
                }
            }
        }
        return document;
    }

    /**
     * Injects the given AD element configuration value into the instance, trying to find a field or setter method for it
     */
    private static void injectMetaTypePropertyValue(Class<?> clazz, Object instance, String name, String defaultValue, Map<String, ?> configuration) {
        if (Strings.isNullOrBlank(name)) {
            return;
        }
        Object value = configuration.get(name);
        if (value == null) {
            value = defaultValue;
        }
        try {
            Field field = clazz.getDeclaredField(normalizePropertyName(name));
            if (field != null) {
                Object convertedValue = ConverterHelper.convertValue(value, field.getType());
                if (convertedValue != null) {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Injecting value " + convertedValue + " to field " + field);
                    }
                    ReflectionHelper.setField(field, instance, convertedValue);
                }
            }
        } catch (NoSuchFieldException e) {
            // ignore
        }
    }


    /**
     * Utility to transform name containing dots to valid java identifiers.
     * @param name
     * @return
     */
     static String normalizePropertyName(String name) {
        if (Strings.isNullOrBlank(name)) {
            return name;
        } else if (!name.contains(".")) {
            return name;
        } else {
            String[] parts = name.replaceAll(" ", "").split("\\.");
            StringBuilder sb = new StringBuilder();
            if (parts.length > 0) {
                sb.append(parts[0]);
                for (int i = 1; i < parts.length; i++) {
                    String s = parts[i].length() > 0 ? parts[i].substring(0, 1).toUpperCase() + parts[i].substring(1) : "";
                    sb.append(s);
                }
            }
            return sb.toString();
        }
    }
}
