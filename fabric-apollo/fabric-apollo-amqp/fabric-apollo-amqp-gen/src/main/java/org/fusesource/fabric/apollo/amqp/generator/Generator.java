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

import org.fusesource.fabric.apollo.amqp.jaxb.schema.Amqp;
import org.fusesource.fabric.apollo.amqp.jaxb.schema.Definition;
import org.fusesource.fabric.apollo.amqp.jaxb.schema.Section;
import org.fusesource.fabric.apollo.amqp.jaxb.schema.Type;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.Source;
import javax.xml.transform.sax.SAXSource;
import java.io.*;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.StringTokenizer;
import java.util.TreeSet;

public class Generator {

    private static final String SLASH = File.separator;

    private File[] inputFiles;
    private File outputDirectory;
    private File sourceDirectory;
    private String packagePrefix;

    public static final HashSet<String> CONTROLS = new HashSet<String>();
    public static final HashSet<String> COMMANDS = new HashSet<String>();
    public static final LinkedHashMap<String, AmqpDefinition> DEFINITIONS = new LinkedHashMap<String, AmqpDefinition>();

    public File[] getInputFile() {
        return inputFiles;
    }

    public void setInputFiles(File... inputFiles) {
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

    public void generate() throws Exception {
        TypeRegistry.init(this);
        JAXBContext jc = JAXBContext.newInstance(Amqp.class.getPackage().getName());

        for (File inputFile : inputFiles) {

            // Firstly, scan the file for command and control defs:
            BufferedReader reader = new BufferedReader(new FileReader(inputFile));
            String line = reader.readLine();
            while (line != null) {
                line = line.trim();
                if (line.startsWith("<!-- -")) {
                    StringTokenizer tok = new StringTokenizer(line, "- ");
                    while (tok.hasMoreTokens()) {
                        String token = tok.nextToken();
                        if (token.equals("Control:")) {
                            CONTROLS.add(tok.nextToken());
                            break;
                        } else if (token.equals("Command:")) {
                            COMMANDS.add(tok.nextToken());
                            break;
                        }
                    }
                }
                line = reader.readLine();
            }
            reader.close();

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


            reader = new BufferedReader(new FileReader(inputFile));
            Source er = new SAXSource(xmlreader, new InputSource(reader));

            // Amqp amqp = (Amqp) unmarshaller.unmarshal(new StreamSource(new
            // File(inputFile)), Amqp.class).getValue();
            Amqp amqp = (Amqp) unmarshaller.unmarshal(er);

            // Scan document:
            for (Object docOrSection : amqp.getDocOrSection()) {
                if (docOrSection instanceof Section) {
                    Section section = (Section) docOrSection;

                    for (Object docOrDefinitionOrType : section.getDocOrDefinitionOrType()) {
                        if (docOrDefinitionOrType instanceof Type) {
                            generateClassFromType(amqp, section, (Type) docOrDefinitionOrType);
                        } else if (docOrDefinitionOrType instanceof Definition) {
                            Definition def = (Definition) docOrDefinitionOrType;
                            DEFINITIONS.put(def.getName(), new AmqpDefinition(def));
                        }
                    }
                }
            }
            reader.close();
        }

        // tweak the restricted type to handle cases where the restricted type
        // isn't a primitive type
        for (AmqpClass amqpClass : TypeRegistry.getGeneratedTypes()) {
            if ( amqpClass.isRestricted() ) {
                amqpClass.restrictedType = TypeRegistry.getBasePrimitiveType(amqpClass.restrictedType);
            }
            Utils.LOG.debug(amqpClass.toString());
        }

        Utils.LOG.debug(TypeRegistry.dump());
        // Generate Types:
        for (AmqpClass amqpClass : TypeRegistry.getGeneratedTypes()) {
            amqpClass.generate(this);
        }

        generatePrimitiveEncoderInterface();
        generateCommandHandler();
        generateMarshallerInterface();
        generateMarshaller();
        generateTypeFactory();
        generateDefinitions();
    }

    private void generateCommandHandler() throws IOException {
        String outputPackage = packagePrefix.replace(".", SLASH);
        File out = new File(outputDirectory, outputPackage + SLASH + "AmqpCommandHandler.java");

        BufferedWriter writer = new BufferedWriter(new FileWriter(out));

        Utils.writeJavaCopyWrite(writer);
        writer.write("package " + packagePrefix + ";");
        writer.newLine();
        writer.newLine();

        for (AmqpClass amqpClass : TypeRegistry.getGeneratedTypes()) {
            if (amqpClass.isCommand()) {
                writer.write("import " + amqpClass.getTypeMapping().getImport() + ";");
                writer.newLine();
            }
        }
        Utils.writeAutoGeneratedWarning(writer, 0);
        writer.write("public interface AmqpCommandHandler {");
        writer.newLine();
        // Generate Handler methods:
        for (AmqpClass amqpClass : TypeRegistry.getGeneratedTypes()) {
            if (amqpClass.isCommand()) {
                writer.newLine();
                writer.write(Utils.tab(1) + "public void handle" + Utils.capFirst(Utils.toJavaName(amqpClass.name)) + "(" + amqpClass.getJavaType() + " " + Utils.toJavaName(amqpClass.name) + ") throws Exception;");
                writer.newLine();
            }
        }
        writer.write("}");
        writer.flush();
        writer.close();
    }

    private void generateDefinitions() throws IOException {
        String outputPackage = packagePrefix.replace(".", SLASH);
        File out = new File(outputDirectory, outputPackage + SLASH + "Definitions.java");

        BufferedWriter writer = new BufferedWriter(new FileWriter(out));

        Utils.writeJavaCopyWrite(writer);
        writer.write("package " + packagePrefix + ";");
        writer.newLine();
        writer.newLine();

        Utils.writeAutoGeneratedWarning(writer, 0);
        writer.write("public interface Definitions {");
        writer.newLine();
        // Generate Handler methods:
        for (AmqpDefinition def : DEFINITIONS.values()) {
            writer.newLine();
            def.writeJavaDoc(writer, 1);
            writer.write(Utils.tab(1) + "public static final String " + Utils.capFirst(Utils.toJavaConstant(def.getName())) + " = \"" + def.getValue() + "\";");
            writer.newLine();
        }

        writer.write("}");
        writer.flush();
        writer.close();
    }

    private void generateMarshallerInterface() throws IOException, UnknownTypeException {
        String outputPackage = new String(packagePrefix + ".marshaller");
        File out = new File(outputDirectory, outputPackage.replace(".", SLASH) + SLASH + "AmqpMarshaller.java");

        BufferedWriter writer = new BufferedWriter(new FileWriter(out));

        Utils.writeJavaCopyWrite(writer);
        writer.write("package " + outputPackage + ";");
        writer.newLine();
        writer.newLine();

        TreeSet<String> imports = new TreeSet<String>();
        imports.add(getPackagePrefix() + ".marshaller.AmqpVersion");
        imports.add(getPackagePrefix() + ".marshaller.Encoded");
        writeMarshallerImports(writer, false, imports, outputPackage);

        writer.newLine();
        Utils.writeAutoGeneratedWarning(writer, 0);
        writer.write("public interface AmqpMarshaller {");
        writer.newLine();

        writer.newLine();
        Utils.writeJavaComment(writer, 1, "@return the protocol version of the marshaller");
        writer.write(Utils.tab(1) + "public AmqpVersion getVersion();");
        writer.newLine();

        writer.newLine();
        writer.write(Utils.tab(1) + "public " + TypeRegistry.any().typeMapping + " decodeType(Buffer source) throws AmqpEncodingError;");
        writer.newLine();

        writer.newLine();
        writer.write(Utils.tab(1) + "public " + TypeRegistry.any().typeMapping + " unmarshalType(DataInput in) throws IOException, AmqpEncodingError;");
        writer.newLine();

        // Generate Handler methods:
        for (AmqpClass amqpClass : TypeRegistry.getGeneratedTypes()) {
            if (amqpClass.needsMarshaller()) {

                if (amqpClass.name.equals("*")) {
                    continue;
                }

                writer.newLine();
                writer.write(Utils.tab(1) + "public Encoded<" + amqpClass.getValueMapping() + "> encode(" + amqpClass.getJavaType() + " data) throws AmqpEncodingError;");
                writer.newLine();

                writer.newLine();
                writer.write(Utils.tab(1) + "public Encoded<" + amqpClass.getValueMapping() + "> decode" + amqpClass.getJavaType() + "(Buffer source, int offset) throws AmqpEncodingError;");
                writer.newLine();

                writer.newLine();
                writer.write(Utils.tab(1) + "public Encoded<" + amqpClass.getValueMapping() + "> unmarshal" + amqpClass.getJavaType() + "(DataInput in) throws IOException, AmqpEncodingError;");
                writer.newLine();
            }
        }

        writer.write("}");
        writer.flush();
        writer.close();

    }

    private void generateMarshaller() throws IOException, UnknownTypeException {
        File out = new File(outputDirectory, getMarshallerPackage().replace(".", SLASH) + SLASH + "AmqpMarshaller.java");

        BufferedWriter writer = new BufferedWriter(new FileWriter(out));

        Utils.writeJavaCopyWrite(writer);
        writer.write("package " + getMarshallerPackage() + ";");
        writer.newLine();
        writer.newLine();

        TreeSet<String> imports = new TreeSet<String>();
        imports.add("java.util.HashMap");
        imports.add(getMarshallerPackage() + ".Encoder.*");
        imports.add(getPackagePrefix() + ".marshaller.AmqpVersion");
        imports.add(getPackagePrefix() + ".marshaller.Encoded");
        writeMarshallerImports(writer, false, imports, getMarshallerPackage());

        writer.newLine();
        Utils.writeAutoGeneratedWarning(writer, 0);
        writer.write("public class AmqpMarshaller implements " + getPackagePrefix() + ".marshaller.AmqpMarshaller {");
        writer.newLine();

        writer.newLine();
        writer.write(Utils.tab(1) + "static AmqpMarshaller SINGLETON = new AmqpMarshaller();");
        writer.newLine();

        writer.newLine();
        writer.write(Utils.tab(1) + "public static AmqpVersion VERSION = new AmqpVersion((short)" + DEFINITIONS.get("MAJOR").getValue() + ", (short)" + DEFINITIONS.get("MINOR").getValue()
                + ", (short)" + DEFINITIONS.get("REVISION").getValue() + ");");
        writer.newLine();

        writer.newLine();
        writer.write(Utils.tab(1) + "public static AmqpMarshaller getMarshaller() {");
        writer.newLine();
        writer.write(Utils.tab(2) + "return SINGLETON;");
        writer.newLine();
        writer.write(Utils.tab(1) + "}");
        writer.newLine();

        writer.newLine();
        writer.write(Utils.tab(1) + "private static HashMap<Long, DescribedTypeMarshaller<?>> DESCRIBED_NUMERIC_TYPES = new HashMap<Long, DescribedTypeMarshaller<?>>();");
        writer.newLine();
        writer.write(Utils.tab(1) + "private static HashMap<String, DescribedTypeMarshaller<?>> DESCRIBED_SYMBOLIC_TYPES = new HashMap<String, DescribedTypeMarshaller<?>>();");
        writer.newLine();
        writer.write(Utils.tab(1) + "static {");
        writer.newLine();

        for (AmqpClass amqpClass : TypeRegistry.getGeneratedTypes()) {
            if (amqpClass.isDescribed()) {
                writer.write(Utils.tab(2) + "DESCRIBED_NUMERIC_TYPES.put(" + amqpClass.getTypeMapping() + "Marshaller.NUMERIC_ID, " + amqpClass.getTypeMapping() + "Marshaller.SINGLETON);");
                writer.newLine();
                writer.write(Utils.tab(2) + "DESCRIBED_SYMBOLIC_TYPES.put(" + amqpClass.getTypeMapping() + "Marshaller.SYMBOLIC_ID, " + amqpClass.getTypeMapping() + "Marshaller.SINGLETON);");
                writer.newLine();
            }
        }
        writer.write(Utils.tab(1) + "}");
        writer.newLine();
        writer.newLine();
        Utils.writeJavaComment(writer, 1, "@return the protocol version of the marshaller");
        writer.write(Utils.tab(1) + "public final AmqpVersion getVersion() {");
        writer.newLine();
        writer.write(Utils.tab(2) + "return VERSION;");
        writer.newLine();
        writer.write(Utils.tab(1) + "}");
        writer.newLine();

        // Generate Handler methods:
        writer.newLine();
        writer.write(Utils.tab(1) + "public final " + TypeRegistry.any().typeMapping + " unmarshalType(DataInput in) throws IOException, AmqpEncodingError {");
        writer.newLine();
        writer.write(Utils.tab(2) + "return Encoder.unmarshalType(in);");
        writer.newLine();
        writer.write(Utils.tab(1) + "}");
        writer.newLine();

        writer.newLine();
        writer.write(Utils.tab(1) + "public final " + TypeRegistry.any().typeMapping + " decodeType(Buffer source) throws AmqpEncodingError {");
        writer.newLine();
        writer.write(Utils.tab(2) + "return Encoder.decode(source);");
        writer.newLine();
        writer.write(Utils.tab(1) + "}");
        writer.newLine();

        writer.newLine();
        writer.write(Utils.tab(1) + "final " + TypeRegistry.any().typeMapping + " decodeType(EncodedBuffer encoded) throws AmqpEncodingError {");
        writer.newLine();
        writer.write(Utils.tab(2) + "if(encoded.isDescribed()) {");
        writer.newLine();
        writer.write(Utils.tab(2) + "return decodeType(encoded.asDescribed());");
        writer.newLine();
        writer.write(Utils.tab(2) + "}");
        writer.newLine();

        writer.newLine();
        writer.write(Utils.tab(2) + "switch(encoded.getEncodingFormatCode()) {");
        writer.newLine();
        for (AmqpClass amqpClass : TypeRegistry.getGeneratedTypes()) {
            if (amqpClass.isPrimitive() && !amqpClass.getName().equals("*")) {

                writer.write(Utils.tab(2) + "//" + amqpClass.getTypeMapping() + " Encoded: ");
                writer.newLine();
                if (!amqpClass.hasMultipleEncodings() && !amqpClass.hasNonFixedEncoding()) {
                    writer.write(Utils.tab(2) + "case " + amqpClass.getTypeMapping() + "Marshaller.FORMAT_CODE: ");
                    writer.newLine();
                } else {
                    for (AmqpEncoding encoding : amqpClass.encodings) {

                        writer.write(Utils.tab(2) + "case (byte) " + encoding.getCode() + ":");
                        writer.newLine();
                    }
                }
                writer.write(Utils.tab(2) + "{");
                writer.newLine();
                writer.write(Utils.tab(3) + "return " + amqpClass.bufferMapping + ".create(" + amqpClass.getMarshaller() + ".createEncoded(encoded));");
                writer.newLine();
                writer.write(Utils.tab(2) + "}");
                writer.newLine();
            }
        }
        writer.write(Utils.tab(2) + "default: {");
        writer.newLine();
        writer.write(Utils.tab(3) + "//TODO: Create an unknown or any type");
        writer.newLine();
        writer.write(Utils.tab(3) + "throw new AmqpEncodingError(\"Unrecognized format code:\" + encoded.getEncodingFormatCode());");
        writer.newLine();
        writer.write(Utils.tab(2) + "}");
        writer.newLine();
        writer.write(Utils.tab(2) + "}");
        writer.newLine();
        writer.write(Utils.tab(1) + "}");
        writer.newLine();

        writer.newLine();
        writer.write(Utils.tab(1) + "final " + TypeRegistry.any().typeMapping + " decodeType(DescribedBuffer buffer) throws AmqpEncodingError {");
        writer.newLine();
        writer.write(Utils.tab(2) + TypeRegistry.any().typeMapping + " descriptor = decodeType(buffer.getDescriptorBuffer());");
        writer.newLine();
        writer.write(Utils.tab(2) + "//TODO might want to revisit whether or not the cast is needed here:");
        writer.newLine();
        writer.write(Utils.tab(2) + "DescribedTypeMarshaller<?> dtm = null;");
        writer.newLine();
        writer.write(Utils.tab(2) + "if ( descriptor instanceof AmqpUlong ) {");
        writer.newLine();
        writer.write(Utils.tab(3) + "dtm = DESCRIBED_NUMERIC_TYPES.get(((AmqpUlong)descriptor).getValue().longValue());");
        writer.newLine();
        writer.write(Utils.tab(2) + "} else if ( descriptor instanceof AmqpSymbol ) {");
        writer.newLine();
        writer.write(Utils.tab(3) + "dtm = DESCRIBED_SYMBOLIC_TYPES.get(((AmqpSymbol)descriptor).getValue());");
        writer.newLine();
        writer.write(Utils.tab(2) + "} else if ( descriptor instanceof AmqpBoolean ) {");
        writer.newLine();
        writer.write(Utils.tab(3) + "dtm = MultipleMarshaller.SINGLETON;;");
        writer.newLine();
        writer.write(Utils.tab(2) + "}");
        writer.newLine();
        writer.newLine();
        writer.write(Utils.tab(2) + "if(dtm != null) {");
        writer.newLine();
        writer.write(Utils.tab(3) + "return dtm.decodeDescribedType(descriptor, buffer);");
        writer.newLine();
        writer.write(Utils.tab(2) + "}");
        writer.newLine();
        writer.newLine();
        writer.write(Utils.tab(2) + "//TODO spec actually indicates that we should be able to pass along unknown types. so we should just create");
        writer.newLine();
        writer.write(Utils.tab(2) + "//a placeholder type");
        writer.newLine();
        writer.write(Utils.tab(2) + "throw new AmqpEncodingError(\"Unrecognized described type:\" + descriptor);");
        writer.newLine();
        writer.write(Utils.tab(1) + "}");

        for (AmqpClass amqpClass : TypeRegistry.getGeneratedTypes()) {
            if (amqpClass.needsMarshaller()) {

                if (amqpClass.name.equals("*")) {
                    continue;
                }
                writer.newLine();
                writer.write(Utils.tab(1) + "public final Encoded<" + amqpClass.getValueMapping() + "> encode(" + amqpClass.getJavaType() + " data) throws AmqpEncodingError {");
                writer.newLine();
                writer.write(Utils.tab(2) + "return " + amqpClass.getJavaType() + "Marshaller.encode(data);");
                writer.newLine();
                writer.write(Utils.tab(1) + "}");
                writer.newLine();

                writer.newLine();
                writer.write(Utils.tab(1) + "public Encoded<" + amqpClass.getValueMapping() + "> decode" + amqpClass.getJavaType() + "(Buffer source, int offset) throws AmqpEncodingError {");
                writer.newLine();
                writer.write(Utils.tab(2) + "return " + amqpClass.getMarshaller() + ".createEncoded(source, offset);");
                writer.newLine();
                writer.write(Utils.tab(1) + "}");
                writer.newLine();

                writer.newLine();
                writer.write(Utils.tab(1) + "public Encoded<" + amqpClass.getValueMapping() + "> unmarshal" + amqpClass.getJavaType() + "(DataInput in) throws IOException, AmqpEncodingError {");
                writer.newLine();
                writer.write(Utils.tab(2) + "return " + amqpClass.getMarshaller() + ".createEncoded(in);");
                writer.newLine();
                writer.write(Utils.tab(1) + "}");
                writer.newLine();
            }
        }

        writer.write("}");
        writer.flush();
        writer.close();

    }

    private void generatePrimitiveEncoderInterface() throws IOException, UnknownTypeException {

        String outputPackage = getMarshallerPackage();
        File out = new File(outputDirectory, outputPackage.replace(".", SLASH) + SLASH + "PrimitiveEncoder.java");

        BufferedWriter writer = new BufferedWriter(new FileWriter(out));

        Utils.writeJavaCopyWrite(writer);
        writer.write("package " + outputPackage + ";");
        writer.newLine();
        writer.newLine();

        TreeSet<String> imports = new TreeSet<String>();
        imports.add("java.io.DataOutput");
        writeMarshallerImports(writer, true, imports, getMarshallerPackage(), getPackagePrefix() + ".types");
        writer.newLine();

        writer.newLine();
        Utils.writeAutoGeneratedWarning(writer, 0);
        writer.write("public interface PrimitiveEncoder {");
        writer.newLine();

        HashSet<String> filters = new HashSet<String>();
        filters.add("*");
        filters.add("list");
        filters.add("map");

        // Write out encoding serializers:
        for (AmqpClass amqpClass : TypeRegistry.getGeneratedTypes()) {
            if (!amqpClass.isPrimitive() || filters.contains(amqpClass.getName())) {
                continue;
            }

            String javaType = amqpClass.getValueMapping().getJavaType();

            if (amqpClass.encodings != null) {

                if (amqpClass.hasNonZeroEncoding()) {
                    for (AmqpEncoding encoding : amqpClass.encodings) {
                        String encName = Utils.capFirst(Utils.toJavaName(amqpClass.name));
                        if (amqpClass.hasMultipleEncodings()) {
                            encName += Utils.capFirst(Utils.toJavaName(Utils.option(encoding.getName(), amqpClass.name)));
                        }

                        writer.newLine();
                        Utils.writeJavaComment(writer, 1, "Writes a " + javaType + " encoded as " + encoding.getLabel());
                        writer.write(Utils.tab(1) + "public void write" + encName + "(" + javaType + " val, DataOutput buf) throws IOException, AmqpEncodingError;");
                        writer.newLine();

                        Utils.writeJavaComment(writer, 1, "Encodes a " + javaType + " as " + encoding.getLabel(), "", "The encoded data should be written into the supplied buffer at the given offset.");
                        writer.write(Utils.tab(1) + "public void encode" + encName + "(" + javaType + " val, Buffer buf, int offset) throws AmqpEncodingError;");
                        writer.newLine();

                        writer.newLine();
                        Utils.writeJavaComment(writer, 1, "Reads a " + javaType + " encoded as " + encoding.getLabel());
                        if (amqpClass.hasNonFixedEncoding()) {
                            writer.write(Utils.tab(1) + "public " + javaType + " read" + encName + "(int size, DataInput dis) throws IOException, AmqpEncodingError;");
                        } else {
                            writer.write(Utils.tab(1) + "public " + javaType + " read" + encName + "(DataInput dis) throws IOException, AmqpEncodingError;");
                        }
                        writer.newLine();

                        writer.newLine();
                        Utils.writeJavaComment(writer, 1, "Decodes a " + javaType + " encoded as " + encoding.getLabel());
                        if (amqpClass.hasNonFixedEncoding()) {
                            writer.write(Utils.tab(1) + "public " + javaType + " decode" + encName + "(Buffer encoded, int offset, int length) throws AmqpEncodingError;");
                        } else {
                            writer.write(Utils.tab(1) + "public " + javaType + " decode" + encName + "(Buffer encoded, int offset) throws AmqpEncodingError;");
                        }
                        writer.newLine();
                    }
                }
            }
        }

        writer.write("}");
        writer.newLine();
        writer.flush();
        writer.close();
    }

    private void generateTypeFactory() throws IOException, UnknownTypeException {

        String outputPackage = getPackagePrefix() + ".types";
        File out = new File(outputDirectory, outputPackage.replace(".", SLASH) + SLASH + "TypeFactory.java");

        BufferedWriter writer = new BufferedWriter(new FileWriter(out));

        Utils.writeJavaCopyWrite(writer);
        writer.write("package " + outputPackage + ";");
        writer.newLine();
        writer.newLine();

        TreeSet<String> imports = new TreeSet<String>();
        writeTypeImports(writer, true, imports, getMarshallerPackage(), outputPackage);
        writer.newLine();

        writer.newLine();
        Utils.writeAutoGeneratedWarning(writer, 0);
        writer.write("public class TypeFactory {");
        writer.newLine();

        for (AmqpClass ac : TypeRegistry.getGeneratedTypes()) {
            if (ac.isAny() || ac.isEnumType()) {
                continue;
            }
            if (ac.isDescribed()) {
                writer.newLine();
                Utils.writeJavaComment(writer, 1, "Creates a " + ac.getTypeMapping());
                writer.write(Utils.tab(1) + "public static " + ac.getTypeMapping() + " create" + ac.getTypeMapping() + "() {");
                writer.newLine();
                writer.write(Utils.tab(2) + "return new " + ac.getBeanMapping() + "();");
                writer.newLine();
                writer.write(Utils.tab(1) + "};");
                writer.newLine();
            } else {
                AmqpClass bt = ac.resolveBaseType();
                writer.newLine();
                Utils.writeJavaComment(writer, 1, "Creates a " + ac.getTypeMapping());
                writer.write(Utils.tab(1) + "public static " + ac.getTypeMapping() + " create" + ac.getTypeMapping() + "(" + bt.getValueMapping() + " val) {");
                writer.newLine();
                writer.write(Utils.tab(2) + "return new " + ac.getBeanMapping() + "(val);");
                writer.newLine();
                writer.write(Utils.tab(1) + "}");
                writer.newLine();

                if(bt.getValueMapping().hasPrimitiveType())
                {
                    Utils.writeJavaComment(writer, 1, "Creates a " + ac.getTypeMapping());
                    writer.write(Utils.tab(1) + "public static " + ac.getTypeMapping() + " create" + ac.getTypeMapping() + "(" + bt.getValueMapping().getPrimitiveType() + " val) {");
                    writer.newLine();
                    writer.write(Utils.tab(2) + "return new " + ac.getBeanMapping() + "(val);");
                    writer.newLine();
                    writer.write(Utils.tab(1) + "}");
                    writer.newLine();
                }

                if(bt.isMutable())
                {
                    Utils.writeJavaComment(writer, 1, "Creates an empty " + ac.getTypeMapping());
                    writer.write(Utils.tab(1) + "public static " + ac.getTypeMapping() + " create" + ac.getTypeMapping() + "() {");
                    writer.newLine();
                    writer.write(Utils.tab(2) + "return new " + ac.getBeanMapping() + "();");
                    writer.newLine();
                    writer.write(Utils.tab(1) + "}");
                    writer.newLine();
                }
            }
        }
        writer.write("}");
        writer.newLine();
        writer.flush();
        writer.close();
    }

    private void writeMarshallerImports(BufferedWriter writer, boolean primitiveOnly, TreeSet<String> imports, String... packageFilters) throws IOException, UnknownTypeException {

        imports.add("java.io.DataInput");
        imports.add("java.io.IOException");
        imports.add(getPackagePrefix() + ".marshaller.AmqpEncodingError");

        writeTypeImports(writer, primitiveOnly, imports, packageFilters);
    }

    private void writeTypeImports(BufferedWriter writer, boolean primitiveOnly, TreeSet<String> imports, String... packageFilters) throws IOException, UnknownTypeException {
        HashSet<String> filters = new HashSet<String>();
        filters.add("java.lang");
        for (String filter : packageFilters) {
            filters.add(filter);
        }
        for (AmqpClass amqpClass : TypeRegistry.getGeneratedTypes()) {
            if (primitiveOnly && (!amqpClass.isPrimitive() || amqpClass.isList() || amqpClass.isMap())) {
                continue;
            }
            if (amqpClass.needsMarshaller()) {
                imports.add(amqpClass.getTypeMapping().getImport());
                TypeRegistry.JavaTypeMapping vMap = amqpClass.getValueMapping();
                if (vMap != null && vMap.getImport() != null) {
                    imports.add(vMap.getImport());
                }
            }
        }

        for (String i : imports) {
            if (!filters.contains(Utils.javaPackageOf(i))) {
                writer.write("import " + i + ";");
                writer.newLine();
            }

        }

    }

    private void generateClassFromType(Amqp source, Section section, Type type) throws Exception {
        AmqpClass amqpClass = new AmqpClass();
        amqpClass.parseFromType(this, source, section, type);
        TypeRegistry.addType(amqpClass);
    }

    public String getVersionPackageName() {
        return "v" + DEFINITIONS.get("MAJOR").getValue() + "_" + DEFINITIONS.get("MINOR").getValue() + "_" + DEFINITIONS.get("REVISION").getValue();
    }

    public String getMarshallerPackage() {
        return packagePrefix + ".marshaller." + getVersionPackageName();
    }

}
