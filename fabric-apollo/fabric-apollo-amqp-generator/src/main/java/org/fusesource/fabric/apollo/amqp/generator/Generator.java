/**
 * Copyright (C) 2010-2011, FuseSource Corp.  All rights reserved.
 *
 *     http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */

package org.fusesource.fabric.apollo.amqp.generator;

import com.sun.codemodel.internal.*;
import org.fusesource.fabric.apollo.amqp.jaxb.schema.*;
import org.fusesource.hawtbuf.Buffer;
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
import java.math.BigDecimal;
import java.util.*;

import static org.fusesource.fabric.apollo.amqp.generator.Utilities.*;

/**
 *
 */
public class Generator {

    private File[] inputFiles;
    private File outputDirectory;
    private File sourceDirectory;
    private String packagePrefix;

    private HashSet<Definition> definitions = new HashSet<Definition>();
    private HashSet<Type> primitives = new HashSet<Type>();
    private HashSet<Type> composites = new HashSet<Type>();
    private HashSet<Type> restricted = new HashSet<Type>();
    private HashMap<String, HashSet<String>> provides = new HashMap<String, HashSet<String>>();
    private HashSet<String> classes = new HashSet<String>();
    private HashMap<String, String> restrictedMapping = new HashMap<String, String>();
    private HashMap<String, String> compositeMapping = new HashMap<String, String>();
    private HashMap<String, String> sections = new HashMap<String, String>();

    private HashMap<String, Class> mapping = new HashMap<String, Class>();

    JCodeModel cm = new JCodeModel();

    public Generator() {
        mapping.put("null", null);
        mapping.put("*", Object.class);
        mapping.put("boolean", Boolean.class);
        mapping.put("ubyte", Byte.class);
        mapping.put("ushort", Short.class);
        mapping.put("uint", Integer.class);
        mapping.put("ulong", Long.class);
        mapping.put("byte", Byte.class);
        mapping.put("short", Short.class);
        mapping.put("int", Integer.class);
        mapping.put("long", Long.class);
        mapping.put("float", Float.class);
        mapping.put("double", Double.class);
        mapping.put("decimal32", BigDecimal.class);
        mapping.put("decimal64", BigDecimal.class);
        mapping.put("decimal128", BigDecimal.class);
        mapping.put("char", Character.class);
        mapping.put("timestamp", Date.class);
        mapping.put("uuid", UUID.class);
        mapping.put("binary", Buffer.class);
        mapping.put("string", String.class);
        mapping.put("symbol", Buffer.class);
        mapping.put("list", List.class);
        mapping.put("map", Map.class);
        mapping.put("array", Object[].class);
    }

    public void generate() throws Exception {

        parseXML();

        log();

        outputDirectory.mkdirs();

        try {
            generateAbstractBases();
            generateDescribedTypes();
            generateDefinitions();

            cm.build(outputDirectory);

        } catch (Exception e) {
            Log.error("Error generating code : %s", e);
            for (StackTraceElement s : e.getStackTrace()) {
                Log.error("\tat %s.%s(%s:%s)", s.getClassName(), s.getMethodName(), s.getFileName(), s.getLineNumber());
            }
            throw e;
        }
    }

    private void generateDescribedTypes() throws Exception {
        for(Type type : composites) {
            String sectionPackage = sanitize(sections.get(type.getName()));
            String name = toJavaClassName(type.getName());
            name = packagePrefix + "." + sectionPackage + "." + name;

            JDefinedClass cls = cm._getClass(name);
            if (cls == null) {
               cls = cm._class(name);
            }

            if (type.getProvides() != null && !type.getProvides().equals(type.getName())) {
                cls._implements(cm.ref(packagePrefix + "." + sectionPackage + "." + toJavaClassName(type.getProvides())));
            }

            Log.info("");
            Log.info("Generating %s", cls.binaryName());

            for (Object obj : type.getEncodingOrDescriptorOrFieldOrChoiceOrDoc()) {
                if (obj instanceof Field ) {
                    Field field = (Field)obj;
                    Log.info("Field name=%s type=%s", field.getName(), field.getType());
                    String fieldType = field.getType();
                    String fieldName = sanitize(field.getName());

                    if (fieldType.equals("*") && field.getRequires() != null) {
                        fieldType = getPackagePrefix() + "." + sectionPackage + "." + toJavaClassName(field.getRequires());
                        Log.info("Trying required type %s", fieldType);
                    } else {
                        while (!mapping.containsKey(fieldType)) {
                            fieldType = restrictedMapping.get(fieldType);
                            if (fieldType == null) {
                                break;
                            }
                            Log.info("Trying field type %s for field %s", fieldType, fieldName);
                        }
                    }

                    if (fieldType == null) {
                        fieldType = compositeMapping.get(field.getType());
                    }

                    if (fieldType != null) {
                        Class clazz = mapping.get(fieldType);
                        if (clazz == null) {
                            Log.info("%s %s", fieldType, fieldName);
                            JDefinedClass c = cm._getClass(fieldType);
                            if (c == null) {
                                c = cm._class(fieldType);
                            }
                            cls.field(0, c, fieldName);
                        } else {
                            Log.info("%s %s", mapping.get(fieldType).getSimpleName(), fieldName);
                            cls.field(0, mapping.get(fieldType), fieldName);
                        }
                    } else {
                        Log.info("Skipping field %s, type not found", field.getName());
                    }
                }
            }
        }
    }


    private void parseXML() throws JAXBException, SAXException, ParserConfigurationException, IOException {
        JAXBContext jc = JAXBContext.newInstance(Amqp.class.getPackage().getName());
        for (File inputFile : inputFiles) {
            BufferedReader reader = new BufferedReader(new FileReader(inputFile));

            // JAXB has some namespace handling problems:
            Unmarshaller unmarshaller = jc.createUnmarshaller();
            SAXParserFactory parserFactory;
            parserFactory = SAXParserFactory.newInstance();
            parserFactory.setNamespaceAware(false);
            XMLReader xmlreader = parserFactory.newSAXParser().getXMLReader();
            xmlreader.setEntityResolver(new EntityResolver(){
                public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException {
                    InputSource is = null;
                    if( systemId!=null && systemId.endsWith("amqp.dtd") ) {
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
            for (Object docOrSection : amqp.getDocOrSection()) {
                if (docOrSection instanceof Section ) {
                    Section section = (Section) docOrSection;

                    for (Object docOrDefinitionOrType : section.getDocOrDefinitionOrType()) {
                        if (docOrDefinitionOrType instanceof Type ) {
                            Type type = (Type)docOrDefinitionOrType;

                            Log.info("Section : %s - Type name=%s class=%s provides=%s source=%s", section.getName(),  type.getName(), type.getClazz(), type.getProvides(), type.getSource());

                            classes.add(type.getClazz());
                            sections.put(type.getName(), section.getName());

                            if (type.getProvides() != null) {
                                if (!provides.containsKey(section.getName())) {
                                    provides.put(section.getName(), new HashSet<String>());
                                }
                                provides.get(section.getName()).add(type.getProvides());
                            }

                            if (type.getClazz().startsWith("primitive")) {
                                primitives.add(type);
                            } else if (type.getClazz().startsWith("restricted")) {
                                restricted.add(type);
                                restrictedMapping.put(type.getName(), type.getSource());
                            } else if (type.getClazz().startsWith("composite")) {
                                compositeMapping.put(type.getName(), packagePrefix + "." + sanitize(section.getName() + "." + toJavaClassName(type.getName())));
                                composites.add(type);
                            }

                        } else if (docOrDefinitionOrType instanceof Definition ) {

                            Definition def = (Definition) docOrDefinitionOrType;
                            definitions.add(def);
                            //DEFINITIONS.put(def.getName(), new AmqpDefinition(def));
                        }
                    }
                }
            }
            reader.close();
        }
    }

    private void log() {
        Log.info("");
        Log.info("Type classes : ");
        for(String type : classes) {
            Log.info("Type : %s", type);
        }

        Log.info("");
        Log.info("Primitive types : ");
        for(Type type : primitives) {
            Log.info("Type : %s", type.getName());
        }

        Log.info("");
        Log.info("Restricted types : ");
        for(Type type : restricted) {
            Log.info("Type : %s, source : %s", type.getName(), type.getSource());
        }

        Log.info("");
        Log.info("Composite types : ");
        for(Type type : composites) {
            Log.info("Type : %s", type.getName());
            if (type.getProvides() != null) {
                Log.info("\tProvides : %s", type.getProvides());
            }
        }

        Log.info("");
        Log.info("Provides : ");
        for (String section : provides.keySet()) {
            Log.info("Section : %s has provides : %s", section, provides.get(section));
        }
    }

    private void generateAbstractBases() throws JClassAlreadyExistsException, IOException {
        for (String section : provides.keySet()) {
            for (String base : provides.get(section)) {
                boolean found = false;
                for (Type type : composites) {
                    if (sections.get(type.getName()).equals(section)) {
                        if (type.getName().equals(base)) {
                            found = true;
                            break;
                        }
                    }
                }
                if (found) {
                    continue;
                }
                String sectionPackage = sanitize(section);
                String name = toJavaClassName(base);
                JDefinedClass cls = cm._class(packagePrefix + "." + sectionPackage + "." + name, ClassType.INTERFACE);
            }
        }
    }

    private void generateDefinitions() throws Exception {
        JDefinedClass defs = cm._class(packagePrefix + ".Definitions", ClassType.INTERFACE);
        Log.info("Creating %s", defs.binaryName());

        for(Definition def : definitions) {
            Log.info("Adding field %s with value %s", def.getName(), def.getValue());
            JFieldVar field = defs.field(JMod.PUBLIC | JMod.STATIC, java.lang.String.class, toStaticName(def.getName()), JExpr.lit(def.getValue()));
            field.javadoc().addXdoclet(def.getLabel());
        }
    }

    public File[] getInputFiles() {

        return inputFiles;
    }

    public void setInputFiles(File ... inputFiles) {
        this.inputFiles = inputFiles;
    }

    public File getOutputDirectory() {
        return outputDirectory;
    }

    public void setOutputDirectory(File outputDirectory) {
        this.outputDirectory = outputDirectory;
    }

    public void setSourceDirectory(File sourceDirectory) {
        this.sourceDirectory = sourceDirectory;
    }

    public String getPackagePrefix() {
        return packagePrefix;
    }

    public void setPackagePrefix(String packagePrefix) {
        this.packagePrefix = packagePrefix;
    }

}
