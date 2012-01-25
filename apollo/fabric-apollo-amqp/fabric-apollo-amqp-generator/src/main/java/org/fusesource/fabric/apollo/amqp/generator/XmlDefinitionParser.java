/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.fusesource.fabric.apollo.amqp.generator;

import org.fusesource.fabric.apollo.amqp.jaxb.schema.*;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.Source;
import javax.xml.transform.sax.SAXSource;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public class XmlDefinitionParser {
    private final Generator generator;

    public XmlDefinitionParser(Generator generator) {
        this.generator = generator;
    }

    void parseXML() throws JAXBException, SAXException, ParserConfigurationException, IOException {
        JAXBContext jc = JAXBContext.newInstance(Amqp.class.getPackage().getName());
        for ( File inputFile : generator.getInputFiles() ) {
            BufferedReader reader = new BufferedReader(new FileReader(inputFile));

            // JAXB has some namespace handling problems:
            Unmarshaller unmarshaller = jc.createUnmarshaller();
            SAXParserFactory parserFactory;
            parserFactory = SAXParserFactory.newInstance();
            parserFactory.setNamespaceAware(false);
            XMLReader xmlreader = parserFactory.newSAXParser().getXMLReader();
            xmlreader.setEntityResolver(new EntityResolver() {
                public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException {
                    InputSource is = null;
                    if ( systemId != null && systemId.endsWith("amqp.dtd") ) {
                        is = new InputSource();
                        is.setPublicId(publicId);
                        is.setSystemId(Generator.class.getResource("amqp.dtd").toExternalForm());
                    }
                    return is;
                }
            });

            Source er = new SAXSource(xmlreader, new InputSource(reader));

            // Amqp amqp = (Amqp) unmarshaller.unmarshal(new StreamSource(new
            // File(inputFile)), Amqp.class).getValue();
            Amqp amqp = (Amqp) unmarshaller.unmarshal(er);

            // Scan document:
            for ( Object docOrSection : amqp.getDocOrSection() ) {
                if ( docOrSection instanceof Section ) {
                    Section section = (Section) docOrSection;

                    for ( Object docOrDefinitionOrType : section.getDocOrDefinitionOrType() ) {
                        if ( docOrDefinitionOrType instanceof Type ) {
                            Type type = (Type) docOrDefinitionOrType;

                            Log.info("Section : %s - Type name=%s class=%s provides=%s source=%s", section.getName(), type.getName(), type.getClazz(), type.getProvides(), type.getSource());

                            generator.getClasses().add(type.getClazz());
                            generator.getSections().put(type.getName(), section.getName());

                            for ( Object obj : type.getEncodingOrDescriptorOrFieldOrChoiceOrDoc() ) {
                                if ( obj instanceof Descriptor ) {
                                    generator.getDescribed().put(type.getName(), type);
                                }
                                if ( obj instanceof Choice ) {
                                    generator.getEnums().put(type.getName(), type);
                                }
                                if ( obj instanceof Field ) {
                                    Field field = (Field) obj;
                                    if ( field.getRequires() != null ) {
                                        generator.getRequires().add(field.getRequires());
                                    }
                                }
                            }

                            if ( type.getProvides() != null ) {
                                Log.info("Adding provides : %s", type.getProvides());
                                String types[] = type.getProvides().split(",");
                                for ( String t : types ) {
                                    generator.getProvides().add(t.trim());
                                }
                            }

                            if ( type.getClazz().startsWith("primitive") ) {
                                generator.getPrimitives().put(type.getName(), type);
                            } else if ( type.getClazz().startsWith("restricted") ) {
                                generator.getRestricted().put(type.getName(), type);
                                //generator.getRestrictedMapping().put(type.getName(), type.getSource());
                            } else if ( type.getClazz().startsWith("composite") ) {
                                //generator.getCompositeMapping().put(type.getName(), generator.getPackagePrefix() + "." + generator.getTypes() + "." + Utilities.toJavaClassName(type.getName()));
                                generator.getComposites().put(type.getName(), type);
                            }

                        } else if ( docOrDefinitionOrType instanceof Definition ) {

                            Definition def = (Definition) docOrDefinitionOrType;
                            generator.getDefinitions().add(def);
                            //DEFINITIONS.put(def.getName(), new AmqpDefinition(def));
                        }
                    }
                }
            }
            reader.close();
        }
    }
}