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

import com.sun.codemodel.*;
import org.fusesource.fabric.apollo.amqp.jaxb.schema.Definition;
import org.fusesource.fabric.apollo.amqp.jaxb.schema.Type;
import org.fusesource.hawtbuf.Buffer;

import java.io.File;
import java.math.BigDecimal;
import java.util.*;

import static org.fusesource.fabric.apollo.amqp.generator.Utilities.toStaticName;

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
    private HashSet<String> provides = new HashSet<String>();
    private HashSet<String> classes = new HashSet<String>();
    private HashMap<String, String> restrictedMapping = new HashMap<String, String>();
    private HashMap<String, String> compositeMapping = new HashMap<String, String>();
    private HashMap<String, String> sections = new HashMap<String, String>();
    private HashMap<String, Class> mapping = new HashMap<String, Class>();

    JCodeModel cm = new JCodeModel();
    private String interfaces = "interfaces";
    private String types = "types";

    private final XmlDefinitionParser xmlDefinitionParser = new XmlDefinitionParser(this);
    private final CompositeTypeGenerator compositeTypeGenerator = new CompositeTypeGenerator(this);
    private final InterfaceGenerator interfaceGenerator = new InterfaceGenerator(this);

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

        xmlDefinitionParser.parseXML();

        log();

        outputDirectory.mkdirs();

        try {
            interfaceGenerator.generateAbstractBases();

            compositeTypeGenerator.generateDescribedTypes();

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
        Log.info("Provides : %s", provides);
    }

    private void generateDefinitions() throws Exception {
        JDefinedClass defs = cm._class(packagePrefix + ".Definitions", ClassType.INTERFACE);
        Log.info("Creating %s", defs.binaryName());

        for(Definition def : definitions) {
            Log.info("Adding field %s with value %s", def.getName(), def.getValue());
            JFieldVar field = defs.field(JMod.PUBLIC | JMod.STATIC, java.lang.String.class, toStaticName(def.getName()), JExpr.lit(def.getValue()));
            field.javadoc().add(def.getLabel());
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

    public JCodeModel getCm() {
        return cm;
    }

    public String getInterfaces() {
        return interfaces;
    }

    public String getTypes() {
        return types;
    }

    public HashSet<Definition> getDefinitions() {
        return definitions;
    }

    public HashSet<Type> getPrimitives() {
        return primitives;
    }

    public HashSet<Type> getComposites() {
        return composites;
    }

    public HashSet<Type> getRestricted() {
        return restricted;
    }

    public HashSet<String> getProvides() {
        return provides;
    }

    public HashSet<String> getClasses() {
        return classes;
    }

    public HashMap<String, String> getRestrictedMapping() {
        return restrictedMapping;
    }

    public HashMap<String, String> getCompositeMapping() {
        return compositeMapping;
    }

    public HashMap<String, String> getSections() {
        return sections;
    }

    public HashMap<String, Class> getMapping() {
        return mapping;
    }

}
