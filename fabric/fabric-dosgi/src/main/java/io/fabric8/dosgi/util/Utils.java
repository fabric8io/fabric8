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
package io.fabric8.dosgi.util;

import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import io.fabric8.dosgi.impl.EndpointDescription;
import org.osgi.framework.BundleContext;

public class Utils {

    public static final String FRAMEWORK_UUID = "org.osgi.framework.uuid";

    public static String getUUID(BundleContext bundleContext) {
        String uuid = bundleContext.getProperty(FRAMEWORK_UUID);
        if (uuid == null) {
            synchronized (FRAMEWORK_UUID) {
                uuid = bundleContext.getProperty(FRAMEWORK_UUID);
                if (uuid == null) {
                    uuid = UuidGenerator.getUUID();
                    System.setProperty(FRAMEWORK_UUID, uuid);
                }
            }
        }
        return uuid;
    }

    public static Set<String> normalize(Object object) {
        Set<String> strings = new HashSet<String>();
        if (object instanceof String) {
            strings.add((String) object);
        } else if (object instanceof String[]) {
            for (String s : (String[]) object) {
                strings.add(s);
            }
        } else if (object instanceof Collection) {
            for (Object o : (Collection) object) {
                if (o instanceof String) {
                    strings.add((String) o);
                }
            }
        }
        return strings;
    }

    private static final String REMOTE_SERVICES_ADMIN_NS = "http://www.osgi.org/xmlns/rsa/v1.0.0";
    private static final String ENDPOINT_DESCRIPTIONS = "endpoint-descriptions";
    private static final String ENDPOINT_DESCRIPTION = "endpoint-description";
    private static final String PROPERTY = "property";
    private static final String ARRAY = "array";
    private static final String SET = "set";
    private static final String LIST = "list";
    private static final String NAME = "name";
    private static final String VALUE = "value";
    private static final String VALUE_TYPE = "value-type";

    public static String getEndpointDescriptionXML(EndpointDescription endpoint) throws XMLStreamException {
        Map<String, Object> properties = endpoint.getProperties();
        StringWriter writer = new StringWriter();
        XMLStreamWriter xml = XMLOutputFactory.newInstance().createXMLStreamWriter(writer);

        xml.writeStartDocument();
        xml.setDefaultNamespace(REMOTE_SERVICES_ADMIN_NS);
        xml.writeStartElement(REMOTE_SERVICES_ADMIN_NS, ENDPOINT_DESCRIPTIONS);
        xml.writeNamespace("", REMOTE_SERVICES_ADMIN_NS);
        xml.writeStartElement(REMOTE_SERVICES_ADMIN_NS, ENDPOINT_DESCRIPTION);

        for (Map.Entry<String, Object> entry : properties.entrySet()) {
            String key = entry.getKey();
            Object val = entry.getValue();
            xml.writeStartElement(REMOTE_SERVICES_ADMIN_NS, PROPERTY);
            xml.writeAttribute(NAME, key);
            if (val.getClass().isArray()) {
                setValueType(xml, val.getClass().getComponentType().getName());
                xml.writeStartElement(REMOTE_SERVICES_ADMIN_NS, ARRAY);
                for (int i = 0, l = Array.getLength(val); i < l; i++) {
                    xml.writeStartElement(REMOTE_SERVICES_ADMIN_NS, VALUE);
                    xml.writeCharacters(Array.get(val, i).toString());
                    xml.writeEndElement();
                }
                xml.writeEndElement();
            } else if (val instanceof List) {
                xml.writeStartElement(REMOTE_SERVICES_ADMIN_NS, LIST);
                handleCollectionValue(xml, (Collection) val);
                xml.writeEndElement();
            } else if (val instanceof Set) {
                xml.writeStartElement(REMOTE_SERVICES_ADMIN_NS, SET);
                handleCollectionValue(xml, (Collection) val);
                xml.writeEndElement();
            } else {
                xml.writeAttribute(VALUE, val.toString());
                setValueType(xml, val.getClass().getName());
            }
            xml.writeEndElement();
        }

        xml.writeEndElement();
        xml.writeEndElement();
        xml.writeEndDocument();
        xml.close();

        return writer.toString();
    }

    private static void handleCollectionValue(XMLStreamWriter xml, Collection val) throws XMLStreamException {
        for (Object o : val) {
            xml.writeStartElement(REMOTE_SERVICES_ADMIN_NS, VALUE);
            setValueType(xml, o.getClass().getName());
            xml.writeCharacters(o.toString());
            xml.writeEndElement();
        }
    }

    private static void setValueType(XMLStreamWriter xml, String dataType) throws XMLStreamException {
        if (dataType.equals(String.class.getName())) {
            return;
        }
        if (dataType.startsWith("java.lang.")) {
            dataType = dataType.substring("java.lang.".length());
        }
        xml.writeAttribute(VALUE_TYPE, dataType);
    }

    public static EndpointDescription getEndpointDescription(String data) throws XMLStreamException {
        List<EndpointDescription> endpoints = getEndpointDescriptions(data);
        if (endpoints == null || endpoints.size() != 1) {
            throw new IllegalArgumentException();
        }
        return endpoints.get(0);
    }

    public static List<EndpointDescription> getEndpointDescriptions(String data) throws XMLStreamException {
        List<EndpointDescription> endpoints = new ArrayList<EndpointDescription>();
        Map<String, Object> properties = null;
        String key = null;
        String type = null;
        String value = null;
        Object val = null;
        String txt = null;
        XMLStreamReader reader = XMLInputFactory.newInstance().createXMLStreamReader(new StringReader(data));
        for (;;) {
            int code = reader.next();
            switch (code) {
                case XMLStreamConstants.START_DOCUMENT:
                    break;
                case XMLStreamConstants.START_ELEMENT:
                    if (ENDPOINT_DESCRIPTION.equals(reader.getLocalName())) {
                        properties = new HashMap<String, Object>();
                    } else if (PROPERTY.equals(reader.getLocalName())) {
                        key = reader.getAttributeValue(null, NAME);
                        type = reader.getAttributeValue(null, VALUE_TYPE);
                        value = reader.getAttributeValue(null, VALUE);
                        val = null;
                    } else if (ARRAY.equals(reader.getLocalName())) {
                        val = Array.newInstance(TYPES.get(type == null ? "String" : type), 0);
                    } else if (SET.equals(reader.getLocalName())) {
                        val = new HashSet<Object>();
                    } else if (LIST.equals(reader.getLocalName())) {
                        val = new ArrayList<Object>();
                    } else if (VALUE.equals(reader.getLocalName())) {
                        txt = null;
                    }
                    break;
                case XMLStreamConstants.CHARACTERS:
                    if (txt == null) {
                        txt = reader.getText();
                    } else {
                        txt += reader.getText();
                    }
                    break;
                case XMLStreamConstants.END_ELEMENT:
                    if (ENDPOINT_DESCRIPTION.equals(reader.getLocalName())) {
                        endpoints.add(new EndpointDescription(properties));
                        properties = null;
                    } else if (PROPERTY.equals(reader.getLocalName())) {
                        if (key == null || (val == null && value == null) || (val != null && value != null)) {
                            throw new IllegalArgumentException();
                        }
                        if (value != null) {
                            val = instantiate(type == null ? "String" : type, value);
                        }
                        properties.put(key, val);
                    } else if (VALUE.equals(reader.getLocalName())) {
                        if (val.getClass().isArray()) {
                            int len = Array.getLength(val);
                            Object a = Array.newInstance(TYPES.get(type == null ? "String" : type), len + 1);
                            System.arraycopy(val, 0, a, 0, len);
                            Array.set(a, len, instantiate(type == null ? "String" : type, txt));
                            val = a;
                        } else {
                            ((Collection) val).add(instantiate(type == null ? "String" : type, txt));
                        }
                    }
                    break;
                case XMLStreamConstants.END_DOCUMENT:
                    return endpoints;
            }
        }
    }

    private static final Map<String, Class> TYPES;
    static {
        Map<String, Class> types = new HashMap<String, Class>();
        types.put("long", long.class);
        types.put("Long", Long.class);
        types.put("double", double.class);
        types.put("Double", Double.class);
        types.put("float", float.class);
        types.put("Float", Float.class);
        types.put("int", int.class);
        types.put("Integer", Integer.class);
        types.put("byte", byte.class);
        types.put("Byte", Byte.class);
        types.put("char", char.class);
        types.put("Character", Character.class);
        types.put("boolean", boolean.class);
        types.put("Double", Double.class);
        types.put("short", short.class);
        types.put("Short", Short.class);
        types.put("String", String.class);
        TYPES = types;
    }

    private static Object instantiate(String type, String value) {
        if ("String".equals(type)) {
            return value;
        }
        value = value.trim();
        String boxedType = null;
        if ("long".equals(type)) {
            boxedType = "Long";
        } else if ("double".equals(type)) {
            boxedType = "Double";
        } else if ("float".equals(type)) {
            boxedType = "Float";
        } else if ("int".equals(type)) {
            boxedType = "Integer";
        } else if ("byte".equals(type)) {
            boxedType = "Byte";
        } else if ("char".equals(type)) {
            boxedType = "Character";
        } else if ("boolean".equals(type)) {
            boxedType = "Boolean";
        } else if ("short".equals(type)) {
            boxedType = "Short";
        }
        if (boxedType == null) {
            boxedType = type;
        }
        String javaType = "java.lang." + boxedType;
        if (boxedType.equals("Character")) {
            return value.charAt(0);
        }
        try {
            Class<?> cls = ClassLoader.getSystemClassLoader().loadClass(javaType);
            Constructor<?> ctor = cls.getConstructor(String.class);
            return ctor.newInstance(value);
        } catch (Exception e) {
            return null;
        }
    }
}
