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

import org.fusesource.fabric.apollo.amqp.jaxb.schema.*;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.rmi.UnexpectedException;
import java.util.*;

import static org.fusesource.fabric.apollo.amqp.generator.Utils.appendIfNotNull;
import static org.fusesource.fabric.apollo.amqp.generator.Utils.appendIfTrue;

public class AmqpClass {

    protected String name;
    protected String label;
    protected AmqpDoc doc = new AmqpDoc();
    protected AmqpChoice choice;
    protected AmqpDescriptor descriptor;
    protected LinkedList<AmqpEncoding> encodings;
    protected String restrictedType;
    protected String provides;

    protected boolean restricted;
    protected boolean primitive;
    protected boolean isCommand;

    LinkedHashMap<String, AmqpField> fields = new LinkedHashMap<String, AmqpField>();
    public boolean handcoded;

    // Java mapping for this class:
    protected TypeRegistry.JavaTypeMapping typeMapping;
    // Java mapping of the value that this type holds (if any)
    protected TypeRegistry.JavaTypeMapping valueMapping;
    // Java mapping of the bean for this type (if any)
    protected TypeRegistry.JavaTypeMapping beanMapping;
    // Java mapping of the buffer for this type (if any)
    protected TypeRegistry.JavaTypeMapping bufferMapping;

    protected String mapKeyType = "AmqpType<?,?>";
    protected String mapValueType = "AmqpType<?,?>";
    protected String listElementType = "AmqpType<?,?>";

    public TypeRegistry.JavaTypeMapping versionMarshaller;

    public void parseFromType(Generator generator, Amqp source, Section section, Type type) throws UnknownTypeException {
        this.name = type.getName();
        this.restrictedType = type.getSource();
        this.label = type.getLabel();
        this.provides = type.getProvides();
        if ( "frame".equals(provides) || "sasl-frame".equals(provides) ) {
            isCommand = true;
            Generator.COMMANDS.add(this.name);
        }

        for (Object typeAttribute : type.getEncodingOrDescriptorOrFieldOrChoiceOrDoc()) {
            if (typeAttribute instanceof Field) {
                AmqpField field = new AmqpField();
                field.parseFromField((Field) typeAttribute);
                fields.put(field.getName(), field);
            } else if (typeAttribute instanceof Descriptor) {
                descriptor = new AmqpDescriptor();
                descriptor.parseFromDescriptor((Descriptor) typeAttribute);
            } else if (typeAttribute instanceof Choice) {
                if (choice == null) {
                    choice = new AmqpChoice();
                }
                choice.parseFromChoice((Choice) typeAttribute);
            } else if (typeAttribute instanceof Doc) {
                doc.parseFromDoc((Doc) typeAttribute);
            } else if (typeAttribute instanceof Encoding) {
                if (encodings == null) {
                    encodings = new LinkedList<AmqpEncoding>();
                }
                AmqpEncoding encoding = new AmqpEncoding();
                encoding.parseFromEncoding((Encoding) typeAttribute);
                encodings.add(encoding);
            }
        }

        if (label != null) {
            doc.setLabel("Represents a " + label);
        }

        if (type.getClazz().equalsIgnoreCase("primitive")) {
            setPrimitive(true);
        }

        // See if this is a restricting type (used to restrict the type of a
        // field):
        if (type.getClazz().equalsIgnoreCase("restricted")) {
            this.restricted = true;
        }

        typeMapping = new TypeRegistry.JavaTypeMapping(name, generator.getPackagePrefix() + ".types." + "Amqp" + Utils.capFirst(Utils.toJavaName(name)));

        if (isMarshallable()) {
            beanMapping = new TypeRegistry.JavaTypeMapping(name + "-bean", generator.getPackagePrefix() + ".types", typeMapping + "." + typeMapping + "Bean", true);
            bufferMapping = new TypeRegistry.JavaTypeMapping(name + "-bean", generator.getPackagePrefix() + ".types", typeMapping + "." + typeMapping + "Buffer", true);
        }

        if (isPrimitive()) {
            valueMapping = TypeRegistry.getJavaTypeMapping(name);
        } else if (isRestricted()) {
            valueMapping = typeMapping;
        }
    }

    public void generate(Generator generator) throws IOException, UnknownTypeException {
        if (handcoded) {
            return;
        }

        File file = new File(generator.getOutputDirectory() + File.separator + new String(typeMapping.getFullName()).replace(".", File.separator) + ".java");
        file.getParentFile().mkdirs();
        if (file.exists()) {
            file.delete();
        }
        BufferedWriter writer = new BufferedWriter(new FileWriter(file));

        Utils.writeJavaCopyWrite(writer);
        writer.write("package " + typeMapping.getPackageName() + ";\n");
        writer.newLine();
        if (writeImports(writer, generator, false)) {
            writer.newLine();
        }

        if (doc != null) {
            doc.writeJavaDoc(writer, 0);
        } else if (label != null) {
            Utils.writeJavaComment(writer, 0, "Represents a " + label);
        }

        Utils.writeAutoGeneratedWarning(writer, 0);

        // We use enums for restricted types with a choice:
        if (isEnumType()) {
            writer.write("public enum " + typeMapping);
        } else if (isMarshallable()) {

            if (isRestricted()) {
                writer.write("public interface " + typeMapping + " extends " + resolveRestrictedType().getTypeMapping());
            } else if (isDescribed()) {
                writer.write("public interface " + typeMapping + " extends " + descriptor.resolveDescribedType().getTypeMapping());
            } else {
                writer.write("public interface " + typeMapping + " extends AmqpType<" + beanMapping + ", " + bufferMapping + ">");
            }

            if (isList() || isMap()) {
                writer.write(", " + valueMapping.getJavaType());
            }

            if (isCommand()) {
                writer.write(", AmqpCommand");
            }
        }

        writer.write(" {");
        writer.newLine();

        if (isMarshallable()) {
            writer.newLine();
            writeBeanInterface(writer, 1);
            writeBeanImpl(writer, 1);
            writeBufferImpl(writer, 1);
        } else {
            writeEnumType(writer);
        }

        writer.write("}");
        writer.flush();
        writer.close();

        if (!isRestricted()) {
            generateMarshaller(generator);
        }

    }

    private boolean writeImports(BufferedWriter writer, Generator generator, boolean marshaller) throws IOException, UnknownTypeException {

        TreeSet<String> imports = new TreeSet<String>();
        for (AmqpField field : fields.values()) {

            AmqpClass fieldType = field.resolveAmqpFieldType();
            if (!marshaller) {
                filterOrAddImport(imports, fieldType.getValueMapping(), marshaller);
            }

            if (fieldType.isEnumType()) {
                if (!marshaller) {
                    filterOrAddImport(imports, fieldType.getTypeMapping(), marshaller);
                }
                filterOrAddImport(imports, fieldType.resolveBaseType().getTypeMapping(), marshaller);
            } else {
                filterOrAddImport(imports, fieldType.getTypeMapping(), marshaller);
            }
        }

        if (!marshaller && isCommand()) {
            imports.add(generator.getPackagePrefix() + ".AmqpCommandHandler");
            imports.add(generator.getPackagePrefix() + ".AmqpCommand");
        }

        if (marshaller) {
            // Add the marshalled type:
            filterOrAddImport(imports, typeMapping, marshaller);

            if (isMap() || isList()) {
                filterOrAddImport(imports, TypeRegistry.any().getTypeMapping(), marshaller);
            }

            imports.add("java.io.DataInput");
            imports.add("java.io.IOException");
            imports.add(generator.getPackagePrefix() + ".marshaller.AmqpEncodingError");
            imports.add(generator.getPackagePrefix() + ".marshaller.Encoded");
            imports.add(generator.getMarshallerPackage() + ".Encoder");
            imports.add(generator.getMarshallerPackage() + ".Encoder.*");
            imports.add("org.fusesource.hawtbuf.Buffer");

            if (isDescribed()) {

                filterOrAddImport(imports, getValueMapping(), marshaller);
                imports.add(generator.getPackagePrefix() + ".marshaller.UnexpectedTypeException");
                AmqpClass describedType = descriptor.resolveDescribedType();
                if (describedType.getName().equals("list")) {
                    imports.add(TypeRegistry.resolveAmqpClass("list").getValueMapping().getImport());
                    imports.add(generator.getPackagePrefix() + ".types.AmqpType");
                } else if (describedType.getName().equals("map")) {
                    imports.add(TypeRegistry.resolveAmqpClass("map").getValueMapping().getImport());
                    imports.add(generator.getPackagePrefix() + ".types.AmqpType");
                    imports.add("java.util.HashMap");
                    // Import symbol which is used for the keys:
                    imports.add(TypeRegistry.resolveAmqpClass("symbol").getTypeMapping().getImport());
                }

                imports.add(generator.getPackagePrefix() + ".types.AmqpUlong");
                imports.add(generator.getPackagePrefix() + ".types.AmqpSymbol");
            } else {
                imports.add("java.io.DataOutput");
            }

            if (hasMultipleEncodings()) {
                imports.add(generator.getPackagePrefix() + ".marshaller.UnexpectedTypeException");
                imports.add(generator.getPackagePrefix() + ".marshaller.Encoding");
                imports.add(generator.getPackagePrefix() + ".marshaller.AmqpVersion");
            }

            if (isPrimitive()) {
                filterOrAddImport(imports, getValueMapping(), marshaller);
            }

        } else if (isMarshallable()) {

            imports.add(generator.getPackagePrefix() + ".marshaller.AmqpEncodingError");
            imports.add(generator.getPackagePrefix() + ".marshaller.AmqpMarshaller");
            imports.add(generator.getPackagePrefix() + ".marshaller.Encoded");
            imports.add("org.fusesource.hawtbuf.Buffer");
            imports.add("java.io.IOException");
            imports.add("java.io.DataOutput");
            imports.add("java.io.DataInput");

            imports.add(getValueMapping().getImport());

            if (resolveBaseType().isList()) {
                imports.add("java.util.Iterator");
            }

            if (isList()) {
                imports.add("java.util.ArrayList");
            }

            if (resolveBaseType().isMap()) {
                imports.add("java.util.Iterator");
                imports.add("java.util.Map");
                imports.add("java.util.HashMap");
            }

            if (descriptor != null) {

                AmqpClass describedType = descriptor.resolveDescribedType();
                if (describedType.getName().equals("list")) {
                    imports.add(TypeRegistry.resolveAmqpClass("list").getValueMapping().getImport());
                } else if (describedType.getName().equals("map")) {
                    imports.add(TypeRegistry.resolveAmqpClass("map").getValueMapping().getImport());
                }

                filterOrAddImport(imports, describedType.getTypeMapping(), marshaller);

                // filterOrAddImport(imports,
                // describedType.resolveValueMapping());
            }

            if (isCommand()) {
                imports.add(generator.getPackagePrefix() + ".AmqpCommandHandler");
                imports.add(generator.getPackagePrefix() + ".AmqpCommand");
            }
        }

        if (!marshaller && isRestricted()) {
            if (isEnumType()) {
                imports.add(generator.getPackagePrefix() + ".marshaller.AmqpEncodingError");
                imports.add("java.util.HashMap");
            }
            imports.add(TypeRegistry.resolveAmqpClass(restrictedType).getTypeMapping().getImport());
            imports.add(resolveRestrictedType().getValueMapping().getImport());
        }

        boolean ret = false;

        for (String toImport : imports) {
            ret = true;
            writer.write("import " + toImport + ";");
            writer.newLine();
        }
        return ret;
    }

    private void filterOrAddImport(TreeSet<String> imports, TypeRegistry.JavaTypeMapping mapping, boolean marshaller) {
        if (mapping == null) {
            return;
        }
        if (mapping.getImport() == null) {
            return;
        }

        if (!marshaller) {
            if (mapping.getPackageName().equals(typeMapping.getPackageName())) {
                return;
            }
        }
        imports.add(mapping.getImport());
    }

    private void writeEnumType(BufferedWriter writer) throws IOException, UnknownTypeException {

        if (isEnumType()) {
            writer.newLine();
            int i = 0;
            AmqpClass amqpClass = TypeRegistry.resolveAmqpClass(restrictedType);
            TypeRegistry.JavaTypeMapping amqpType = amqpClass.getTypeMapping();
            TypeRegistry.JavaTypeMapping valueType = amqpClass.getValueMapping();

            for (Choice constant : choice.choices) {
                i++;
                if (constant.getDoc() != null) {
                    new AmqpDoc(constant.getDoc()).writeJavaDoc(writer, 1);
                }

                writer.write(Utils.tab(1) + Utils.toJavaConstant(constant.getName()) + "(new " + valueType + "(\"" + constant.getValue() + "\"))");
                if (i < choice.choices.size()) {
                    writer.write(",");
                } else {
                    writer.write(";");
                }
                writer.newLine();
            }

            writer.newLine();
            writer.write(Utils.tab(1) + "private static final HashMap<" + valueType + ", " + getJavaType() + "> LOOKUP = new HashMap<" + valueType + ", " + getJavaType() + ">(2);");
            writer.newLine();
            writer.write(Utils.tab(1) + "static {");
            writer.newLine();
            writer.write(Utils.tab(2) + "for (" + getJavaType() + " " + Utils.toJavaName(getName()) + " : " + getJavaType() + ".values()) {");
            writer.newLine();
            writer.write(Utils.tab(3) + "LOOKUP.put(" + Utils.toJavaName(getName()) + ".value.getValue(), " + Utils.toJavaName(getName()) + ");");
            writer.newLine();
            writer.write(Utils.tab(2) + "}");
            writer.newLine();
            writer.write(Utils.tab(1) + "}");
            writer.newLine();

            writer.newLine();
            writer.write(Utils.tab(1) + "private final " + amqpType + " value;");
            writer.newLine();

            writer.newLine();
            writer.write(Utils.tab(1) + "private " + typeMapping + "(" + valueType + " value) {");
            writer.newLine();
            writer.write(Utils.tab(2) + "this.value = new " + amqpClass.beanMapping + "(value);");

            writer.newLine();
            writer.write(Utils.tab(1) + "}");
            writer.newLine();

            writer.newLine();
            writer.write(Utils.tab(1) + "public final " + amqpType + " getValue() {");
            writer.newLine();
            writer.write(Utils.tab(2) + "return value;");
            writer.newLine();
            writer.write(Utils.tab(1) + "}");
            writer.newLine();

            writer.newLine();
            writer.write(Utils.tab(1) + "public static final " + getJavaType() + " get(" + amqpType + " value) throws AmqpEncodingError{");
            writer.newLine();
            writer.write(Utils.tab(2) + " if ( value == null ) {");
            writer.newLine();
            writer.write(Utils.tab(3) + "return null;");
            writer.newLine();
            writer.write(Utils.tab(2) + "}");
            writer.newLine();
            writer.write(Utils.tab(2) + getJavaType() + " " + Utils.toJavaName(getName()) + "= LOOKUP.get(value.getValue());");
            writer.newLine();
            writer.write(Utils.tab(2) + "if (" + Utils.toJavaName(getName()) + " == null) {");
            writer.newLine();
            writer.write(Utils.tab(3) + "//TODO perhaps this should be an IllegalArgumentException?");
            writer.newLine();
            writer.write(Utils.tab(3) + "throw new AmqpEncodingError(\"Unknown " + Utils.toJavaName(getName()) + ": \" + value + \" expected one of \" + LOOKUP.keySet());");
            writer.newLine();
            writer.write(Utils.tab(2) + "}");
            writer.newLine();
            writer.write(Utils.tab(2) + "return " + Utils.toJavaName(getName()) + ";");
            writer.newLine();
            writer.write(Utils.tab(1) + "}");
            writer.newLine();

            // writer.newLine();
            // writer.write(tab(1) + "public static final  " +
            // getTypeMapping().getJavaType() +
            // " createFromStream(DataInput dis) throws IOException {");
            // writer.newLine();
            // writer.write(tab(2) + " return get(" +
            // amqpType.getTypeMapping().getJavaType() +
            // ".createFromStream(dis)).getValue();");
            // writer.newLine();
            // writer.write(tab(1) + "}");
            // writer.newLine();

        }
    }

    private void writeEncodings(BufferedWriter writer) throws IOException, UnknownTypeException {
        if (isDescribed()) {

            AmqpClass describedType = descriptor.resolveDescribedType();

            if (describedType.isList()) {
                writer.newLine();
                writer.write(Utils.tab(1) + "private static final ListDecoder<" + TypeRegistry.any().typeMapping + "> DECODER = new ListDecoder<" + TypeRegistry.any().typeMapping + ">() {");
                writer.newLine();
                writer
                        .write(Utils.tab(2) + "public final IAmqpList<" + TypeRegistry.any().typeMapping
                                + "> unmarshalType(int dataCount, int dataSize, DataInput in) throws AmqpEncodingError, IOException {");
                writer.newLine();
                writer.write(Utils.tab(3) + "if (dataCount > " + fields.size() + ") {");
                writer.newLine();
                writer.write(Utils.tab(4) + "throw new AmqpEncodingError(\"Too many fields for \" + SYMBOLIC_ID + \": \" + dataCount);");
                writer.newLine();
                writer.write(Utils.tab(3) + "}");

                writer.newLine();
                writer.write(Utils.tab(3) + "IAmqpList<" + TypeRegistry.any().typeMapping + "> rc = new IAmqpList.ArrayBackedList<" + TypeRegistry.any().typeMapping + ">(new "
                        + TypeRegistry.any().typeMapping + "[" + fields.size() + "]);");
                int f = 0;
                for (AmqpField field : fields.values()) {
                    AmqpClass fieldType = field.resolveAmqpFieldType();
                    writer.newLine();
                    writer.write(Utils.tab(3) + "//" + field.getName() + ":");
                    writer.newLine();
                    writer.write(Utils.tab(3) + "if(dataCount > 0) {");
                    writer.newLine();
                    if (fieldType.isAny()) {
                        writer.write(Utils.tab(4) + "rc.set(" + f + ", AmqpMarshaller.SINGLETON.unmarshalType(in));");
                    } else {
                        writer.write(Utils.tab(4) + "rc.set(" + f + ", " + fieldType.getBufferMapping() + ".create(" + fieldType.getMarshaller() + ".createEncoded(in)));");
                    }
                    writer.newLine();
                    writer.write(Utils.tab(4) + "dataCount--;");
                    writer.newLine();
                    writer.write(Utils.tab(3) + "}");
                    writer.newLine();
                    if (field.isRequired()) {
                        writer.write(Utils.tab(3) + "else {");
                        writer.newLine();
                        writer.write(Utils.tab(4) + "throw new AmqpEncodingError(\"Missing required field for \" + SYMBOLIC_ID + \": " + field.getName() + "\");");
                        writer.newLine();
                        writer.write(Utils.tab(3) + "}");
                        writer.newLine();
                    }
                    f++;
                }

                writer.write(Utils.tab(3) + "return rc;");
                writer.newLine();
                writer.write(Utils.tab(2) + "}");
                writer.newLine();

                writer.newLine();
                writer.write(Utils.tab(2) + "public IAmqpList<" + TypeRegistry.any().typeMapping + "> decode(EncodedBuffer[] constituents) {");
                writer.newLine();
                writer.write(Utils.tab(3) + "if (constituents.length > " + fields.size() + ") {");
                writer.newLine();
                writer.write(Utils.tab(4) + "throw new AmqpEncodingError(\"Too many fields for \" + SYMBOLIC_ID + \":\" + constituents.length);");
                writer.newLine();
                writer.write(Utils.tab(3) + "}");

                writer.newLine();
                writer.write(Utils.tab(3) + "int dataCount = constituents.length;");
                writer.newLine();
                writer.write(Utils.tab(3) + "IAmqpList<" + TypeRegistry.any().typeMapping + "> rc = new IAmqpList.ArrayBackedList<" + TypeRegistry.any().typeMapping + ">(new "
                        + TypeRegistry.any().typeMapping + "[" + fields.size() + "]);");
                f = 0;
                for (AmqpField field : fields.values()) {
                    AmqpClass fieldType = field.resolveAmqpFieldType();
                    writer.newLine();
                    writer.write(Utils.tab(3) + "//" + field.getName() + ":");
                    writer.newLine();
                    writer.write(Utils.tab(3) + "if(dataCount > 0) {");
                    writer.newLine();
                    if (fieldType.isAny()) {
                        writer.write(Utils.tab(4) + "rc.set(" + f + ", AmqpMarshaller.SINGLETON.decodeType(constituents[" + f + "]));");
                    } else {
                        writer.write(Utils.tab(4) + "rc.set(" + f + ", " + fieldType.getBufferMapping() + ".create(" + fieldType.getMarshaller() + ".createEncoded(constituents[" + f + "])));");
                    }
                    writer.newLine();
                    writer.write(Utils.tab(4) + "dataCount--;");
                    writer.newLine();
                    writer.write(Utils.tab(3) + "}");
                    writer.newLine();
                    if (field.isRequired()) {
                        writer.write(Utils.tab(3) + "else {");
                        writer.newLine();
                        writer.write(Utils.tab(4) + "throw new AmqpEncodingError(\"Missing required field for \" + SYMBOLIC_ID + \": " + field.getName() + "\");");
                        writer.newLine();
                        writer.write(Utils.tab(3) + "}");
                        writer.newLine();
                    }
                    f++;
                }
                writer.write(Utils.tab(3) + "return rc;");
                writer.newLine();
                writer.write(Utils.tab(2) + "}");
                writer.newLine();

                writer.write(Utils.tab(1) + "};");
                writer.newLine();
            } else if (describedType.isMap()) {

                writer.newLine();
                writer.write(Utils.tab(1) + "private static final MapDecoder<AmqpSymbol, " + TypeRegistry.any().typeMapping + "> DECODER = new MapDecoder<AmqpSymbol, " + TypeRegistry.any().typeMapping
                        + ">() {");
                writer.newLine();

                writer.newLine();
                writer.write(Utils.tab(2) + "public IAmqpMap<AmqpSymbol, AmqpType<?, ?>> decode(EncodedBuffer[] constituents) throws AmqpEncodingError {");
                writer.newLine();
                writer.write(Utils.tab(3) + "IAmqpMap<AmqpSymbol, AmqpType<?, ?>> rc = new IAmqpMap.AmqpWrapperMap<AmqpSymbol, AmqpType<?,?>>(new HashMap<AmqpSymbol, AmqpType<?,?>>());");
                writer.newLine();
                writer.write(Utils.tab(3) + "if (constituents.length % 2 != 0) {");
                writer.newLine();
                writer.write(Utils.tab(4) + "throw new AmqpEncodingError(\"Invalid number of compound constituents for \" + SYMBOLIC_ID + \": \" + constituents.length);");
                writer.newLine();
                writer.write(Utils.tab(3) + "}");
                writer.newLine();
                for (AmqpField field : fields.values()) {
                    if (field.isRequired()) {
                        writer.newLine();
                        writer.write(Utils.tab(4) + "boolean saw" + Utils.capFirst(field.getName()) + " = false;");
                    }
                }

                writer.write(Utils.tab(3) + "for (int i = 0; i < constituents.length; i += 2) {");
                writer.newLine();
                writer.write(Utils.tab(4) + "AmqpSymbol key = AmqpSymbol.AmqpSymbolBuffer.create(AmqpSymbolMarshaller.createEncoded(constituents[i]));");
                writer.newLine();
                writer.write(Utils.tab(4) + "if (key == null) {");
                writer.newLine();
                writer.write(Utils.tab(5) + "throw new AmqpEncodingError(\"Null Key for \" + SYMBOLIC_ID);");
                writer.newLine();
                writer.write(Utils.tab(4) + "} ");
                int f = fields.size();
                for (AmqpField field : fields.values()) {
                    AmqpClass fieldType = field.resolveAmqpFieldType();
                    writer.write((--f >= 0 ? "else " : "") + "if (key.equals(" + typeMapping + "." + Utils.toJavaConstant(field.getName()) + "_KEY)){");
                    writer.newLine();
                    if (field.isRequired()) {
                        writer.write(Utils.tab(5) + "saw" + Utils.capFirst(field.getName()) + " = true;");
                        writer.newLine();
                    }

                    if (fieldType.isAny()) {
                        writer.write(Utils.tab(5) + "rc.put(" + typeMapping + "." + Utils.toJavaConstant(field.getName()) + "_KEY, AmqpMarshaller.SINGLETON.decodeType(constituents[i + 1]));");
                    } else {
                        writer.write(Utils.tab(5) + "rc.put(" + typeMapping + "." + Utils.toJavaConstant(field.getName()) + "_KEY, " + fieldType.getBufferMapping() + ".create(" + fieldType.getMarshaller()
                                + ".createEncoded(constituents[i + 1])));");
                    }
                    writer.newLine();
                    writer.write(Utils.tab(4) + "} ");
                }
                writer.write("else {");
                writer.newLine();
                writer.write(Utils.tab(5) + "throw new UnexpectedTypeException(\"Invalid field key for \" + SYMBOLIC_ID + \" : \" + key);");
                writer.newLine();
                writer.write(Utils.tab(4) + "}");
                writer.newLine();
                writer.write(Utils.tab(3) + "}");
                for (AmqpField field : fields.values()) {
                    if (field.isRequired()) {
                        writer.newLine();
                        writer.write(Utils.tab(3) + "if(!saw" + field.getName() + ") {");
                        writer.newLine();
                        writer.write(Utils.tab(4) + "throw new AmqpEncodingError(\"Missing required field for \" + SYMBOLIC_ID + \": " + field.getName() + "\");");
                        writer.newLine();
                        writer.write(Utils.tab(3) + "}");
                    }
                }
                writer.newLine();
                writer.write(Utils.tab(3) + "return rc;");
                writer.newLine();
                writer.write(Utils.tab(2) + "}");
                writer.newLine();

                writer.newLine();
                writer.write(Utils.tab(2) + "public IAmqpMap<AmqpSymbol, AmqpType<?, ?>> unmarshalType(int dataCount, int dataSize, DataInput in) throws IOException, AmqpEncodingError {");
                writer.newLine();
                writer.write(Utils.tab(3) + "IAmqpMap<AmqpSymbol, AmqpType<?, ?>> rc = new IAmqpMap.AmqpWrapperMap<AmqpSymbol, AmqpType<?,?>>(new HashMap<AmqpSymbol, AmqpType<?,?>>());");
                writer.newLine();
                writer.write(Utils.tab(3) + "if (dataCount % 2 != 0) {");
                writer.newLine();
                writer.write(Utils.tab(4) + "throw new AmqpEncodingError(\"Invalid number of compound constituents for \" + SYMBOLIC_ID + \": \" + dataCount);");
                writer.newLine();
                writer.write(Utils.tab(3) + "}");
                writer.newLine();

                writer.write(Utils.tab(3) + "for (int i = 0; i < dataCount; i += 2) {");
                writer.newLine();
                writer.write(Utils.tab(4) + "AmqpSymbol key = AmqpSymbol.AmqpSymbolBuffer.create(AmqpSymbolMarshaller.createEncoded(in));");
                writer.newLine();
                writer.write(Utils.tab(4) + "if (key == null) {");
                writer.newLine();
                writer.write(Utils.tab(5) + "throw new AmqpEncodingError(\"Null Key for \" + SYMBOLIC_ID);");
                writer.newLine();
                writer.write(Utils.tab(4) + "}");
                writer.newLine();
                writer.newLine();
                for (AmqpField field : fields.values()) {
                    if (field.isRequired()) {
                        writer.newLine();
                        writer.write(Utils.tab(4) + "boolean saw" + Utils.capFirst(field.getName()) + " = false;");
                    }
                }
                writer.newLine();
                f = 0;
                for (AmqpField field : fields.values()) {
                    AmqpClass fieldType = field.resolveAmqpFieldType();
                    writer.write(Utils.tab(4) + (f > 0 ? "else " : "") + "if (key.equals(" + typeMapping + "." + Utils.toJavaConstant(field.getName()) + "_KEY)){");
                    writer.newLine();
                    if (field.isRequired()) {
                        writer.write(Utils.tab(4) + "saw" + Utils.capFirst(field.getName()) + " = true;");
                        writer.newLine();
                    }
                    if (fieldType.isAny()) {
                        writer.write(Utils.tab(5) + "rc.put(" + typeMapping + "." + Utils.toJavaConstant(field.getName()) + "_KEY, AmqpMarshaller.SINGLETON.unmarshalType(in));");
                    } else {
                        writer.write(Utils.tab(5) + "rc.put(" + typeMapping + "." + Utils.toJavaConstant(field.getName()) + "_KEY, " + fieldType.getBufferMapping() + ".create(" + fieldType.getMarshaller()
                                + ".createEncoded(in)));");
                    }
                    writer.newLine();
                    writer.write(Utils.tab(4) + "}");
                    writer.newLine();
                }
                writer.write(Utils.tab(4) + "else {");
                writer.newLine();
                writer.write(Utils.tab(5) + "throw new UnexpectedTypeException(\"Invalid field key for \" + SYMBOLIC_ID + \" : \" + key);");
                writer.newLine();
                writer.write(Utils.tab(4) + "}");
                writer.newLine();
                writer.write(Utils.tab(3) + "}");
                for (AmqpField field : fields.values()) {
                    if (field.isRequired()) {
                        writer.newLine();
                        writer.write(Utils.tab(3) + "if(!saw" + field.getName() + ") {");
                        writer.newLine();
                        writer.write(Utils.tab(4) + "throw new AmqpEncodingError(\"Missing required field for \" + SYMBOLIC_ID + \": " + field.getName() + "\");");
                        writer.newLine();
                        writer.write(Utils.tab(3) + "}");
                    }
                }
                writer.newLine();
                writer.write(Utils.tab(3) + "return rc;");
                writer.newLine();
                writer.write(Utils.tab(2) + "}");
                writer.newLine();

                writer.write(Utils.tab(1) + "};");
                writer.newLine();
            } else {
                throw new UnexpectedException("Unsupported described type: " + descriptor.getDescribedType());
            }

            writer.newLine();
            writer.write(Utils.tab(1) + "public static class " + getJavaType() + "Encoded extends DescribedEncoded<" + getValueMapping() + "> {");
            writer.newLine();

            writer.newLine();
            writer.write(Utils.tab(2) + "public " + getJavaType() + "Encoded(DescribedBuffer buffer) {");
            writer.newLine();
            writer.write(Utils.tab(3) + "super(buffer);");
            writer.newLine();
            writer.write(Utils.tab(2) + "}");
            writer.newLine();

            writer.newLine();
            writer.write(Utils.tab(2) + "public " + getJavaType() + "Encoded(" + typeMapping + " value) {");
            writer.newLine();
            writer.write(Utils.tab(3) + "super(" + describedType.getMarshaller() + ".encode(value));");
            writer.newLine();
            writer.write(Utils.tab(2) + "}");
            writer.newLine();

            writer.newLine();
            writer.write(Utils.tab(2) + "protected final String getSymbolicId() {");
            writer.newLine();
            writer.write(Utils.tab(3) + "return SYMBOLIC_ID;");
            writer.newLine();
            writer.write(Utils.tab(2) + "}");
            writer.newLine();

            writer.newLine();
            writer.write(Utils.tab(2) + "protected final long getNumericId() {");
            writer.newLine();
            writer.write(Utils.tab(3) + "return NUMERIC_ID;");
            writer.newLine();
            writer.write(Utils.tab(2) + "}");
            writer.newLine();

            writer.newLine();
            writer.write(Utils.tab(2) + "protected final Encoded<" + getValueMapping() + "> decodeDescribed(EncodedBuffer encoded) throws AmqpEncodingError {");
            writer.newLine();
            writer.write(Utils.tab(3) + "return " + describedType.getMarshaller() + ".createEncoded(encoded, DECODER);");
            writer.newLine();
            writer.write(Utils.tab(2) + "}");
            writer.newLine();

            writer.newLine();
            writer.write(Utils.tab(2) + "protected final Encoded<" + getValueMapping() + "> unmarshalDescribed(DataInput in) throws IOException {");
            writer.newLine();
            writer.write(Utils.tab(3) + "return " + describedType.getMarshaller() + ".createEncoded(in, DECODER);");
            writer.newLine();
            writer.write(Utils.tab(2) + "}");
            writer.newLine();

            writer.newLine();
            writer.write(Utils.tab(2) + "protected final EncodedBuffer getDescriptor() {");
            writer.newLine();
            writer.write(Utils.tab(3) + "return DESCRIPTOR;");
            writer.newLine();
            writer.write(Utils.tab(2) + "}");
            writer.newLine();

            writer.write(Utils.tab(1) + "}");
            writer.newLine();

            return;
        }

        if (!hasMultipleEncodings() && !hasNonFixedEncoding()) {
            writer.newLine();
            writer.write(Utils.tab(1) + "public static final byte FORMAT_CODE = (byte) " + encodings.getFirst().getCode() + ";");
            writer.newLine();
            writer.write(Utils.tab(1) + "public static final FormatSubCategory FORMAT_CATEGORY  = FormatSubCategory.getCategory(FORMAT_CODE);");
            writer.newLine();
            // writer.write(tab(1) + "public static final " + getJavaType() +
            // "Encoded ENCODING = new " + getJavaType() + "Encoded();");
            // writer.newLine();

            writer.newLine();
            writer.write(Utils.tab(1) + "public static class " + getJavaType() + "Encoded  extends AbstractEncoded<" + getValueMapping().getJavaType() + "> {");
            writer.newLine();

            writer.newLine();
            writer.write(Utils.tab(2) + "public " + getJavaType() + "Encoded (EncodedBuffer encoded) {");
            writer.newLine();
            writer.write(Utils.tab(3) + "super(encoded);");
            writer.newLine();
            writer.write(Utils.tab(2) + "}");
            writer.newLine();

            writer.newLine();
            writer.write(Utils.tab(2) + "public " + getJavaType() + "Encoded (" + getValueMapping().getJavaType() + " value) throws AmqpEncodingError {");
            writer.newLine();
            writer.write(Utils.tab(3) + "super(FORMAT_CODE, value);");
            writer.newLine();
            writer.write(Utils.tab(2) + "}");
            writer.newLine();

            writer.newLine();
            writer.write(Utils.tab(2) + "public final void encode(" + getValueMapping().getJavaType() + " value, Buffer encoded, int offset) throws AmqpEncodingError{");
            writer.newLine();
            if (hasNonZeroEncoding()) {
                writer.write(Utils.tab(3) + "ENCODER.encode" + Utils.capFirst(Utils.toJavaName(name)) + "(value, encoded, offset);");
            }
            writer.newLine();
            writer.write(Utils.tab(2) + "}");
            writer.newLine();

            writer.newLine();
            writer.write(Utils.tab(2) + "public final " + getValueMapping().getJavaType() + " decode(EncodedBuffer encoded) throws AmqpEncodingError{");
            writer.newLine();
            if (hasNonZeroEncoding()) {
                writer.write(Utils.tab(3) + "return ENCODER.decode" + Utils.capFirst(Utils.toJavaName(name)) + "(encoded.getBuffer(), encoded.getDataOffset());");
            } else {
                writer.write(Utils.tab(3) + "return ENCODER.valueOf" + Utils.capFirst(Utils.toJavaName(name)) + "();");
            }
            writer.newLine();
            writer.write(Utils.tab(2) + "}");
            writer.newLine();

            writer.newLine();
            writer.write(Utils.tab(2) + "public final void marshalData(DataOutput out) throws IOException {");
            writer.newLine();
            if (hasNonZeroEncoding()) {
                writer.write(Utils.tab(3) + "ENCODER.write" + Utils.capFirst(Utils.toJavaName(name)) + "(value, out);");
            }
            writer.newLine();
            writer.write(Utils.tab(2) + "}");
            writer.newLine();

            writer.newLine();
            writer.write(Utils.tab(2) + "public final " + getValueMapping().getJavaType() + " unmarshalData(DataInput in) throws IOException {");
            writer.newLine();
            if (hasNonZeroEncoding()) {
                writer.write(Utils.tab(3) + "return ENCODER.read" + Utils.capFirst(Utils.toJavaName(name)) + "(in);");
            } else {
                writer.write(Utils.tab(3) + "return ENCODER.valueOf" + Utils.capFirst(Utils.toJavaName(name)) + "();");
            }
            writer.newLine();
            writer.write(Utils.tab(2) + "}");
            writer.newLine();

            writer.write(Utils.tab(1) + "}");
        } else {
            String encodingName = getEncodingName(false);

            writer.newLine();
            for (AmqpEncoding encoding : encodings) {
                writer.write(Utils.tab(1) + "public static final byte " + Utils.toJavaConstant(Utils.option(encoding.getName(), name)) + "_FORMAT_CODE = (byte) " + encoding.getCode() + ";");
                writer.newLine();
            }

            // Create an enum that captures the allowed encodings:
            writer.newLine();
            writer.write(Utils.tab(1) + "public static enum " + encodingName + " implements Encoding{");
            writer.newLine();

            int i = 0;
            for (AmqpEncoding encoding : encodings) {
                i++;
                String eName = Utils.option(encoding.getName(), name);
                if (eName == null) {
                    eName = name;
                }
                eName = Utils.toJavaConstant(eName);

                writer.write(Utils.tab(2) + eName + " (" + Utils.toJavaConstant(Utils.option(encoding.getName(), name)) + "_FORMAT_CODE)");
                if (i < encodings.size()) {
                    writer.write(",");
                } else {
                    writer.write(";");
                }

                if (encoding.getLabel() != null) {
                    writer.write(" // " + encoding.getLabel());
                }

                writer.newLine();
            }

            writer.newLine();
            writer.write(Utils.tab(2) + "public final byte FORMAT_CODE;");
            writer.newLine();
            writer.write(Utils.tab(2) + "public final FormatSubCategory CATEGORY;");
            writer.newLine();

            // Write constructor:
            writer.newLine();
            writer.write(Utils.tab(2) + encodingName + "(byte formatCode) {");
            writer.newLine();
            writer.write(Utils.tab(3) + "this.FORMAT_CODE = formatCode;");
            writer.newLine();
            writer.write(Utils.tab(3) + "this.CATEGORY = FormatSubCategory.getCategory(formatCode);");
            writer.newLine();
            writer.write(Utils.tab(2) + "}");
            writer.newLine();

            writer.newLine();
            writer.write(Utils.tab(2) + "public final byte getEncodingFormatCode() {");
            writer.newLine();
            writer.write(Utils.tab(3) + "return FORMAT_CODE;");
            writer.newLine();
            writer.write(Utils.tab(2) + "}");
            writer.newLine();

            writer.newLine();
            writer.write(Utils.tab(2) + "public final AmqpVersion getEncodingVersion() {");
            writer.newLine();
            writer.write(Utils.tab(3) + "return AmqpMarshaller.VERSION;");
            writer.newLine();
            writer.write(Utils.tab(2) + "}");
            writer.newLine();

            writer.newLine();
            writer.write(Utils.tab(2) + "public static " + encodingName + " getEncoding(byte formatCode) throws UnexpectedTypeException {");
            writer.newLine();
            writer.write(Utils.tab(3) + "switch(formatCode) {");
            writer.newLine();
            for (AmqpEncoding encoding : encodings) {
                writer.write(Utils.tab(3) + "case " + Utils.toJavaConstant(Utils.option(encoding.getName(), name)) + "_FORMAT_CODE: {");
                writer.newLine();
                writer.write(Utils.tab(4) + "return " + Utils.toJavaConstant(Utils.option(encoding.getName(), name)) + ";");
                writer.newLine();
                writer.write(Utils.tab(3) + "}");
                writer.newLine();
            }
            writer.write(Utils.tab(3) + "default: {");
            writer.newLine();
            writer.write(Utils.tab(4) + "throw new UnexpectedTypeException(\"Unexpected format code for " + Utils.capFirst(name) + ": \" + formatCode);");
            writer.newLine();
            writer.write(Utils.tab(3) + "}");
            writer.newLine();
            writer.write(Utils.tab(3) + "}");
            writer.newLine();
            writer.write(Utils.tab(2) + "}");
            writer.newLine();

            writer.newLine();
            writer.write(Utils.tab(2) + "static final " + getJavaType() + "Encoded createEncoded(EncodedBuffer buffer) throws AmqpEncodingError {");
            writer.newLine();
            writer.write(Utils.tab(3) + "switch(buffer.getEncodingFormatCode()) {");
            writer.newLine();
            for (AmqpEncoding encoding : encodings) {
                writer.write(Utils.tab(3) + "case " + Utils.toJavaConstant(Utils.option(encoding.getName(), name)) + "_FORMAT_CODE: {");
                writer.newLine();
                writer.write(Utils.tab(4) + "return new " + getJavaType() + Utils.capFirst(Utils.toJavaName(Utils.option(encoding.getName(), name))) + "Encoded(buffer);");
                writer.newLine();
                writer.write(Utils.tab(3) + "}");
                writer.newLine();
            }
            writer.write(Utils.tab(3) + "default: {");
            writer.newLine();
            writer.write(Utils.tab(4) + "throw new UnexpectedTypeException(\"Unexpected format code for " + Utils.capFirst(name) + ": \" + buffer.getEncodingFormatCode());");
            writer.newLine();
            writer.write(Utils.tab(3) + "}");
            writer.newLine();
            writer.write(Utils.tab(3) + "}");
            writer.newLine();
            writer.write(Utils.tab(2) + "}");
            writer.newLine();

            writer.write(Utils.tab(2) + "static final " + getJavaType() + "Encoded createEncoded(byte formatCode, " + getValueMapping().getJavaType() + " value) throws AmqpEncodingError {");
            writer.newLine();
            writer.write(Utils.tab(3) + "switch(formatCode) {");
            writer.newLine();
            for (AmqpEncoding encoding : encodings) {
                writer.write(Utils.tab(3) + "case " + Utils.toJavaConstant(Utils.option(encoding.getName(), name)) + "_FORMAT_CODE: {");
                writer.newLine();
                writer.write(Utils.tab(4) + "return new " + getJavaType() + Utils.capFirst(Utils.toJavaName(Utils.option(encoding.getName(), name))) + "Encoded(value);");
                writer.newLine();
                writer.write(Utils.tab(3) + "}");
                writer.newLine();
            }
            writer.write(Utils.tab(3) + "default: {");
            writer.newLine();
            writer.write(Utils.tab(4) + "throw new UnexpectedTypeException(\"Unexpected format code for " + Utils.capFirst(name) + ": \" + formatCode);");
            writer.newLine();
            writer.write(Utils.tab(3) + "}");
            writer.newLine();
            writer.write(Utils.tab(3) + "}");
            writer.newLine();
            writer.write(Utils.tab(2) + "}");
            writer.newLine();

            writer.write(Utils.tab(1) + "}");
            writer.newLine();

            writer.write(Utils.tab(1) + "public static abstract class " + getJavaType() + "Encoded extends AbstractEncoded <" + getValueMapping().getJavaType() + "> {");
            if (isList()) {
                writer.newLine();
                writer.write(Utils.tab(2) + "ListDecoder decoder = Encoder.DEFAULT_LIST_DECODER;");
                writer.newLine();
            }

            if (isMap()) {
                writer.newLine();
                writer.write(Utils.tab(2) + "MapDecoder decoder = Encoder.DEFAULT_MAP_DECODER;");
                writer.newLine();
            }

            writer.newLine();
            writer.write(Utils.tab(2) + "public " + getJavaType() + "Encoded(EncodedBuffer encoded) {");
            writer.newLine();
            writer.write(Utils.tab(3) + "super(encoded);");
            writer.newLine();
            writer.write(Utils.tab(2) + "}");
            writer.newLine();

            writer.newLine();
            writer.write(Utils.tab(2) + "public " + getJavaType() + "Encoded(byte formatCode, " + getValueMapping().getJavaType() + " value) throws AmqpEncodingError {");
            writer.newLine();
            writer.write(Utils.tab(3) + "super(formatCode, value);");
            writer.newLine();
            writer.write(Utils.tab(2) + "}");
            writer.newLine();

            if (isList()) {
                writer.newLine();
                writer.write(Utils.tab(2) + "final void setDecoder(ListDecoder decoder) {");
                writer.newLine();
                writer.write(Utils.tab(3) + "this.decoder = decoder;");
                writer.newLine();
                writer.write(Utils.tab(2) + "}");
                writer.newLine();
            }

            if (isMap()) {
                writer.newLine();
                writer.write(Utils.tab(2) + "final void setDecoder(MapDecoder decoder) {");
                writer.newLine();
                writer.write(Utils.tab(3) + "this.decoder = decoder;");
                writer.newLine();
                writer.write(Utils.tab(2) + "}");
                writer.newLine();
            }

            writer.write(Utils.tab(1) + "}");
            writer.newLine();

            for (AmqpEncoding encoding : encodings) {
                String eName = Utils.capFirst(Utils.toJavaName(Utils.option(encoding.getName(), name)));

                writer.newLine();
                Utils.writeJavaComment(writer, 1, encoding.getLabel());
                writer.write(Utils.tab(1) + "private static class " + getJavaType() + eName + "Encoded extends " + getJavaType() + "Encoded {");
                writer.newLine();
                writer.newLine();
                writer.write(Utils.tab(2) + "private final " + encodingName + " encoding = " + encodingName + "." + Utils.toJavaConstant(Utils.option(encoding.getName(), name)) + ";");

                writer.newLine();
                writer.write(Utils.tab(2) + "public " + getJavaType() + eName + "Encoded(EncodedBuffer encoded) {");
                writer.newLine();
                writer.write(Utils.tab(3) + "super(encoded);");
                writer.newLine();
                writer.write(Utils.tab(2) + "}");
                writer.newLine();

                writer.newLine();
                writer.write(Utils.tab(2) + "public " + getJavaType() + eName + "Encoded(" + getValueMapping().getJavaType() + " value) throws AmqpEncodingError {");
                writer.newLine();
                writer.write(Utils.tab(3) + "super(" + encodingName + "." + Utils.toJavaConstant(Utils.option(encoding.getName(), name)) + ".FORMAT_CODE, value);");
                writer.newLine();
                writer.write(Utils.tab(2) + "}");
                writer.newLine();

                if (hasNonZeroEncoding()) {
                    writer.newLine();
                    writer.write(Utils.tab(2) + "protected final int computeDataSize() throws AmqpEncodingError {");
                    writer.newLine();
                    writer.write(Utils.tab(3) + "return ENCODER.getEncodedSizeOf" + Utils.capFirst(getName() + "(value, encoding);"));
                    writer.newLine();
                    writer.write(Utils.tab(2) + "}");
                    writer.newLine();
                }

                if (hasCompoundEncoding()) {
                    writer.newLine();
                    writer.write(Utils.tab(2) + "protected final int computeDataCount() throws AmqpEncodingError {");
                    writer.newLine();
                    writer.write(Utils.tab(3) + "return ENCODER.getEncodedCountOf" + Utils.capFirst(getName()) + "(value, encoding);");
                    writer.newLine();
                    writer.write(Utils.tab(2) + "}");
                    writer.newLine();
                }

                writer.newLine();
                writer.write(Utils.tab(2) + "public final void encode(" + getValueMapping().getJavaType() + " value, Buffer encoded, int offset) throws AmqpEncodingError {");
                writer.newLine();
                if (hasNonZeroEncoding()) {
                    writer.write(Utils.tab(3) + "ENCODER.encode" + Utils.capFirst(Utils.toJavaName(name)) + eName + "(value, encoded, offset);");
                }
                writer.newLine();
                writer.write(Utils.tab(2) + "}");
                writer.newLine();

                writer.newLine();
                writer.write(Utils.tab(2) + "public final void marshalData(DataOutput out) throws IOException {");
                writer.newLine();
                if (hasNonZeroEncoding()) {
                    writer.write(Utils.tab(3) + "ENCODER.write" + Utils.capFirst(Utils.toJavaName(name)) + eName + "(value, out);");
                }
                writer.newLine();
                writer.write(Utils.tab(2) + "}");
                writer.newLine();

                writer.newLine();
                writer.write(Utils.tab(2) + "public final " + getValueMapping().getJavaType() + " decode(EncodedBuffer encoded) throws AmqpEncodingError {");
                writer.newLine();
                if (isList() || isMap()) {
                    writer.write(Utils.tab(3) + "return decoder.decode(encoded.asCompound().constituents());");
                } else if (hasCompoundEncoding()) {
                    writer.write(Utils.tab(3) + "return ENCODER.decode" + Utils.capFirst(Utils.toJavaName(name)) + eName
                            + "(encoded.getBuffer(), encoded.getDataOffset(), encoded.getDataCount(), encoded.getDataSize());");
                } else if (hasNonZeroEncoding() && hasVariableEncoding()) {
                    writer.write(Utils.tab(3) + "return ENCODER.decode" + Utils.capFirst(Utils.toJavaName(name)) + eName + "(encoded.getBuffer(), encoded.getDataOffset(), encoded.getDataSize());");
                } else if (hasNonZeroEncoding() && !hasVariableEncoding()) {
                    writer.write(Utils.tab(3) + "return ENCODER.decode" + Utils.capFirst(Utils.toJavaName(name)) + eName + "(encoded.getBuffer(), encoded.getDataOffset());");
                } else {
                    writer.write(Utils.tab(3) + "return ENCODER.valueOf" + Utils.capFirst(Utils.toJavaName(name)) + "(encoding);");
                }
                writer.newLine();
                writer.write(Utils.tab(2) + "}");
                writer.newLine();

                writer.newLine();
                writer.write(Utils.tab(2) + "public final " + getValueMapping().getJavaType() + " unmarshalData(DataInput in) throws IOException {");
                writer.newLine();

                if (isList() || isMap()) {
                    writer.write(Utils.tab(3) + "return decoder.unmarshalType(getDataCount(), getDataSize(), in);");
                } else if (hasCompoundEncoding()) {
                    writer.write(Utils.tab(3) + "return ENCODER.read" + Utils.capFirst(Utils.toJavaName(name)) + eName + "(getDataCount(), getDataSize(), in);");
                } else if (hasNonZeroEncoding() && hasVariableEncoding()) {
                    writer.write(Utils.tab(3) + "return ENCODER.read" + Utils.capFirst(Utils.toJavaName(name)) + eName + "(getDataSize(), in);");
                } else if (hasNonZeroEncoding() && !hasVariableEncoding()) {
                    writer.write(Utils.tab(3) + "return ENCODER.read" + Utils.capFirst(Utils.toJavaName(name)) + eName + "(in);");
                } else {
                    writer.write(Utils.tab(3) + "return ENCODER.valueOf" + Utils.capFirst(Utils.toJavaName(name)) + "(encoding);");
                }
                writer.newLine();
                writer.write(Utils.tab(2) + "}");
                writer.newLine();

                writer.write(Utils.tab(1) + "}");
                writer.newLine();
            }
        }
    }

    private boolean writeFields(BufferedWriter writer, int indent) throws IOException, UnknownTypeException {
        boolean ret = false;

        for (AmqpField field : fields.values()) {
            ret = true;
            AmqpClass fieldClass = field.resolveAmqpFieldType();
            TypeRegistry.JavaTypeMapping valueType = fieldClass.getTypeMapping();

            writer.write(Utils.tab(indent) + "private " + valueType + " " + field.getJavaName());

            if (field.getDefaultValue() != null) {
                String value = field.getDefaultValue();

                if( fieldClass.isPrimitive()) {
                    if (fieldClass.getValueMapping().getJavaType().startsWith("Long")) {
                        writer.write(" = TypeFactory.create" + valueType + "(" + fieldClass.getValueMapping().getJavaType() + ".valueOf(" +  field.getDefaultValue() + "L))");
                    } else {
                        writer.write(" = TypeFactory.create" + valueType + "(" + fieldClass.getValueMapping().getJavaType() + ".valueOf(" +  field.getDefaultValue() + "))");
                    }
                } else if (fieldClass.isEnumType()){
                    writer.write(" = " + valueType + "." +  Utils.toJavaConstant(field.getDefaultValue()));
                }
            }

            writer.write(";");
            writer.newLine();
        }

        if (isRestricted()) {
            resolveRestrictedType().writeFields(writer, indent);
        }

        if (isPrimitive()) {
            writer.write(Utils.tab(indent) + "private " + getValueMapping().getJavaType() + " value;");
            writer.newLine();
        }
        return ret;
    }

    private void writeBeanImpl(BufferedWriter writer, int indent) throws IOException, UnknownTypeException {

        AmqpClass baseType = resolveBaseType();

        writer.newLine();
        if (isPrimitive() && !isList() && !isMap() && !getJavaType().startsWith("AmqpNull")) {
            writer.write(Utils.tab(indent++) + "public static class " + beanMapping.getShortName() + " implements " + typeMapping + ", Comparable<" + typeMapping + ">, AmqpPrimitive<" +  getValueMapping() + "> {");
        } else {
            writer.write(Utils.tab(indent++) + "public static class " + beanMapping.getShortName() + " implements " + typeMapping + " {");
        }
        writer.newLine();

        writer.newLine();
        writer.write(Utils.tab(indent) + "private " + bufferMapping.getShortName() + " buffer;");
        writer.newLine();
        writer.write(Utils.tab(indent) + "private " + beanMapping.getShortName() + " bean = this;");
        writer.newLine();
        writeFields(writer, indent);

        // CONSTRUCTORS:
        // Allow creation of uninitialized mutable types:
        if (isMutable()) {
            writer.newLine();
            writer.write(Utils.tab(indent) + "" + beanMapping.getShortName() + "() {");
            writer.newLine();
            if (!isDescribed() && baseType.isMap()) {
                writer.write(Utils.tab(indent + 1) + "this.value = new IAmqpMap.AmqpWrapperMap<" + getMapKeyType() + "," + getMapValueType() + ">(new HashMap<" + getMapKeyType() + "," + getMapValueType()
                        + ">());");
                writer.newLine();
            } else if (!isDescribed() && baseType.isList()) {
                writer.write(Utils.tab(indent + 1) + "this.value = new IAmqpList.AmqpWrapperList(new ArrayList<AmqpType<?,?>>());");
                writer.newLine();
            }
            writer.write(Utils.tab(indent) + "}");
            writer.newLine();
        }

        writer.newLine();
        writer.write(Utils.tab(indent) + beanMapping.getShortName() + "(" + baseType.getValueMapping().getJavaType() + " value) {");
        writer.newLine();
        if (isDescribed() && baseType.isList()) {
            writer.write(Utils.tab(++indent) + "for(int i = 0; i < value.getListCount(); i++) {");
            writer.newLine();
            writer.write(Utils.tab(++indent) + "set(i, value.get(i));");
            writer.newLine();
            writer.write(Utils.tab(--indent) + "}");
        } else if (isDescribed() && baseType.isMap()) {
            writer.write(Utils.tab(++indent) + "if (value == null) {");
            writer.newLine();
            writer.write(Utils.tab(++indent) + "return;");
            writer.newLine();
            writer.write(Utils.tab(--indent) + "}");

            for (AmqpField field : fields.values()) {
                AmqpClass fieldClass = field.resolveAmqpFieldType();
                TypeRegistry.JavaTypeMapping valueType = fieldClass.getTypeMapping();
                writer.newLine();
                if (fieldClass.isEnumType()) {
                    writer.write(Utils.tab(indent) + field.getJavaName() + " = " + fieldClass.getJavaType() + ".get((" + fieldClass.resolveRestrictedType().getJavaType() + ")value.get(" + Utils.toJavaConstant(field.getName()) + "_KEY));");

                } else {
                    writer.write(Utils.tab(indent) + field.getJavaName() + " = (" + valueType + ") value.get(" + Utils.toJavaConstant(field.getName()) + "_KEY);");
                }
            }
        } else {
            writer.write(Utils.tab(++indent) + "this.value = value;");
        }
        writer.newLine();
        writer.write(Utils.tab(--indent) + "}");
        writer.newLine();

        writer.newLine();
        writer.write(Utils.tab(indent) + beanMapping.getShortName() + "(" + beanMapping + " other) {");
        writer.newLine();
        writer.write(Utils.tab(indent + 1) + "this.bean = other;");
        writer.newLine();

        writer.write(Utils.tab(indent) + "}");
        writer.newLine();

        // METHODS:
        writer.newLine();
        writer.write(Utils.tab(indent) + "public final " + beanMapping.getShortName() + " copy() {");
        writer.newLine();
        if (isMutable()) {
            writer.write(Utils.tab(++indent) + "return new " + beanMapping + "(bean);");
        } else {
            writer.write(Utils.tab(++indent) + "return bean;");
        }
        writer.newLine();
        writer.write(Utils.tab(--indent) + "}");
        writer.newLine();

        if (isCommand()) {
            writer.newLine();
            writer.write(Utils.tab(indent) + "public final void handle(AmqpCommandHandler handler) throws Exception {");
            writer.newLine();
            writer.write(Utils.tab(++indent) + "handler.handle" + Utils.capFirst(Utils.toJavaName(name)) + "(this);");
            writer.newLine();
            writer.write(Utils.tab(--indent) + "}");
            writer.newLine();
        }

        writer.newLine();
        writer.write(Utils.tab(indent) + "public final " + bufferMapping + " getBuffer(AmqpMarshaller marshaller) throws AmqpEncodingError{");
        writer.newLine();
        writer.write(Utils.tab(++indent) + "if(buffer == null) {");
        writer.newLine();
        writer.write(Utils.tab(++indent) + "buffer = new " + bufferMapping.getShortName() + "(marshaller.encode(this));");
        writer.newLine();
        writer.write(Utils.tab(--indent) + "}");
        writer.newLine();
        writer.write(Utils.tab(indent) + "return buffer;");
        writer.newLine();
        writer.write(Utils.tab(--indent) + "}");
        writer.newLine();

        writer.newLine();
        writer.write(Utils.tab(indent) + "public final void marshal(DataOutput out, AmqpMarshaller marshaller) throws IOException, AmqpEncodingError{");
        writer.newLine();
        writer.write(Utils.tab(++indent) + "getBuffer(marshaller).marshal(out, marshaller);");
        writer.newLine();
        writer.write(Utils.tab(--indent) + "}");
        writer.newLine();

        // Accessors:
        writer.newLine();
        writeFieldAccessors(writer, indent, false);
        writer.newLine();

        if (isMutable()) {
            writer.newLine();
            writer.write(Utils.tab(indent) + "private void copyCheck() {");
            writer.newLine();
            writer.write(Utils.tab(++indent) + "if(buffer != null) {");
            writer.newLine();
            writer.write(Utils.tab(++indent) + "buffer = null;");
            writer.newLine();
            writer.write(Utils.tab(--indent) + "}");
            writer.newLine();
            writer.write(Utils.tab(indent) + "if(bean != this) {");
            writer.newLine();
            writer.write(Utils.tab(++indent) + "copy(bean);");
            writer.newLine();
            writer.write(Utils.tab(--indent) + "}");
            writer.newLine();
            writer.write(Utils.tab(--indent) + "}");
            writer.newLine();

            writer.newLine();
            writer.write(Utils.tab(indent) + "private void copy(" + beanMapping + " other) {");
            writer.newLine();
            indent++;

            if (isDescribed()) {
                for (AmqpField field : baseType.fields.values()) {
                    writer.write(Utils.tab(indent) + "this." + field.getJavaName() + "= other." + field.getJavaName() + ";");
                    writer.newLine();
                }
            } else {
                writer.write(Utils.tab(indent) + "this.value = other.value;");
                writer.newLine();
            }
            writer.write(Utils.tab(indent) + "bean = this;");
            writer.newLine();
            writer.write(Utils.tab(--indent) + "}");
            writer.newLine();
        }
        indent = writeBeanEquals(writer, indent, baseType);


        writer.newLine();
        writer.write(Utils.tab(indent) + "public int hashCode() {");
        writer.newLine();
        if (baseType.isMap()) {
            writer.write(Utils.tab(++indent) + "return AbstractAmqpMap.hashCodeFor(this);");
        } else if (baseType.isList()) {
            writer.write(Utils.tab(++indent) + "return AbstractAmqpList.hashCodeFor(this);");
        } else {
            writer.write(Utils.tab(++indent) + "if(getValue() == null) {");
            writer.newLine();
            writer.write(Utils.tab(++indent) + "return " + beanMapping + ".class.hashCode();");
            writer.newLine();
            writer.write(Utils.tab(--indent) + "}");
            writer.newLine();
            writer.write(Utils.tab(indent) + "return getValue().hashCode();");
        }
        writer.newLine();
        writer.write(Utils.tab(--indent) + "}");
        writer.newLine();

        writeToString(writer, indent, false);

        writer.write(Utils.tab(--indent) + "}");
        writer.newLine();

    }

    private int writeBeanEquals(BufferedWriter writer, int indent, AmqpClass baseType) throws IOException {
        // Equivalency:
        writer.newLine();
        writer.write(Utils.tab(indent) + "public boolean equals(Object o){");
        writer.newLine();
        writer.write(Utils.tab(++indent) + "if(this == o) {");
        writer.newLine();
        writer.write(Utils.tab(++indent) + "return true;");
        writer.newLine();
        writer.write(Utils.tab(--indent) + "}");
        writer.newLine();
        writer.newLine();
        writer.write(Utils.tab(indent) + "if(o == null || !(o instanceof " + typeMapping + ")) {");
        writer.newLine();
        writer.write(Utils.tab(++indent) + "return false;");
        writer.newLine();
        writer.write(Utils.tab(--indent) + "}");
        writer.newLine();
        writer.newLine();
        writer.write(Utils.tab(indent) + "return equals((" + typeMapping + ") o);");
        writer.newLine();
        writer.write(Utils.tab(--indent) + "}");
        writer.newLine();

        writer.newLine();
        writer.write(Utils.tab(indent++) + "public boolean equals(" + typeMapping + " b) {");
        writer.newLine();
        if (isDescribed()) {
            for (AmqpField field : fields.values()) {

                writer.newLine();
                writer.write(Utils.tab(indent) + "if(b.get" + Utils.capFirst(field.getJavaName()) + "() == null ^ get" + Utils.capFirst(field.getJavaName()) + "() == null) {");
                writer.newLine();
                writer.write(Utils.tab(++indent) + "return false;");
                writer.newLine();
                writer.write(Utils.tab(--indent) + "}");
                writer.newLine();
                writer.write(Utils.tab(indent) + "if(b.get" + Utils.capFirst(field.getJavaName()) + "() != null && !b.get" + Utils.capFirst(field.getJavaName()) + "().equals(get" + Utils.capFirst(field.getJavaName())
                        + "())){ ");

                writer.newLine();
                writer.write(Utils.tab(++indent) + "return false;");
                writer.newLine();
                writer.write(Utils.tab(--indent) + "}");
                writer.newLine();
            }
            writer.write(Utils.tab(indent) + "return true;");
        } else if (baseType.isMap()) {
            writer.write(Utils.tab(indent) + "return AbstractAmqpMap.checkEqual(this, b);");
        } else if (baseType.isList()) {
            writer.write(Utils.tab(indent) + "return AbstractAmqpList.checkEqual(this, b);");
        } else {
            writer.write(Utils.tab(indent) + "if(b == null) {");
            writer.newLine();
            writer.write(Utils.tab(++indent) + "return false;");
            writer.newLine();
            writer.write(Utils.tab(--indent) + "}");
            writer.newLine();

            writer.newLine();
            writer.write(Utils.tab(indent) + "if(b.getValue() == null ^ getValue() == null) {");
            writer.newLine();
            writer.write(Utils.tab(++indent) + "return false;");
            writer.newLine();
            writer.write(Utils.tab(--indent) + "}");
            writer.newLine();
            writer.newLine();
            writer.write(Utils.tab(indent) + "return b.getValue() == null || b.getValue().equals(getValue());");
        }
        writer.newLine();
        writer.write(Utils.tab(--indent) + "}");
        writer.newLine();

        if (isPrimitive() && !isList() && !isMap() && !getJavaType().startsWith("AmqpNull")) {
            writer.write(Utils.tab(indent) + "public int compareTo(" + getJavaType() + " o) {"  );
            writer.newLine();
            writer.write(Utils.tab(++indent) + " return getValue().compareTo(o.getValue());");
            writer.newLine();
            writer.write(Utils.tab(--indent) + "}");
            writer.newLine();
        }

        return indent;
    }

    private void writeBufferImpl(BufferedWriter writer, int indent) throws IOException, UnknownTypeException {
        if (isDescribed()) {
            indent = writeDescribedTypeBufferConstructors(writer, indent);
        } else if (isPrimitive()) {
            indent = writePrimitiveTypeBufferConstructors(writer, indent);
        } else if (isRestricted()) {
            indent = writeRestrictedTypeBufferConstructors(writer, indent);
        }

        // METHODS:
        writeFieldAccessors(writer, indent, true);
        indent = writeBufferBufferAccessor(writer, indent);
        indent = writeBufferBeanAccessor(writer, indent);

        if (isCommand()) {
            writer.newLine();
            writer.write(Utils.tab(indent) + "public final void handle(AmqpCommandHandler handler) throws Exception {");
            writer.newLine();
            writer.write(Utils.tab(++indent) + "handler.handle" + Utils.capFirst(Utils.toJavaName(name)) + "(this);");
            writer.newLine();
            writer.write(Utils.tab(--indent) + "}");
            writer.newLine();
        }

        indent = writeBufferEquals(writer, indent);
        indent = writeFactoryMethods(writer, indent);
        writeToString(writer, indent, true);

        writer.write(Utils.tab(--indent) + "}");
        writer.newLine();
    }

    private int writeRestrictedTypeBufferConstructors(BufferedWriter writer, int indent) throws UnknownTypeException, IOException {
        AmqpClass restrictedType = resolveRestrictedType();

        writer.newLine();
        writer.write(Utils.tab(indent++) + "public static class " + bufferMapping.getShortName() + " extends " + restrictedType.bufferMapping + " implements " + typeMapping + "{");
        writer.newLine();

        writer.newLine();
        writer.write(Utils.tab(indent) + "private " + beanMapping.getShortName() + " bean;");
        writer.newLine();

        // CONSTRUCTORS:
        writer.newLine();
        writer.write(Utils.tab(indent) + "protected " + bufferMapping.getShortName() + "() {");
        writer.newLine();
        writer.write(Utils.tab(++indent) + "super();");
        writer.newLine();
        writer.write(Utils.tab(--indent) + "}");
        writer.newLine();

        writer.newLine();
        writer.write(Utils.tab(indent) + "protected " + bufferMapping.getShortName() + "(Encoded<" + restrictedType.getValueMapping() + "> encoded) {");
        writer.newLine();
        writer.write(Utils.tab(++indent) + "super(encoded);");
        writer.newLine();
        writer.write(Utils.tab(--indent) + "}");
        writer.newLine();
        return indent;
    }

    private int writePrimitiveTypeBufferConstructors(BufferedWriter writer, int indent) throws IOException, UnknownTypeException {
        writer.newLine();
        if (!isList() && !isMap() && !getJavaType().startsWith("AmqpNull")) {
            writer.write(Utils.tab(indent++) + "public static class " + bufferMapping.getShortName() + " implements " + typeMapping + ", AmqpBuffer<" + getValueMapping() + ">, Comparable<" + typeMapping + ">, AmqpPrimitive<" + getValueMapping() + "> {");
        } else {
            writer.write(Utils.tab(indent++) + "public static class " + bufferMapping.getShortName() + " implements " + typeMapping + ", AmqpBuffer< " + getValueMapping() + "> {");
        }
        writer.newLine();

        writer.newLine();
        writer.write(Utils.tab(indent) + "private " + beanMapping.getShortName() + " bean;");
        writer.newLine();
        writer.write(Utils.tab(indent) + "protected Encoded<" + valueMapping + "> encoded;");
        writer.newLine();

        if ( isList() || isMap() ) {
            writer.write(Utils.tab(indent) + "protected boolean dirty = false;");
            writer.newLine();
        }

        // CONSTRUCTORS:
        writer.newLine();
        writer.write(Utils.tab(indent) + "protected " + bufferMapping.getShortName() + "() {");
        writer.newLine();
        writer.write(Utils.tab(indent) + "}");
        writer.newLine();

        writer.newLine();
        writer.write(Utils.tab(indent) + "protected " + bufferMapping.getShortName() + "(Encoded<" + getValueMapping() + "> encoded) {");
        writer.newLine();
        writer.write(Utils.tab(++indent) + "this.encoded = encoded;");
        writer.newLine();
        writer.write(Utils.tab(--indent) + "}");
        writer.newLine();

        writer.newLine();
        writer.write(Utils.tab(indent) + "public final Encoded<" + getValueMapping() + "> getEncoded() throws AmqpEncodingError{");
        writer.newLine();
        writer.write(Utils.tab(++indent) + "return encoded;");
        writer.newLine();
        writer.write(Utils.tab(--indent) + "}");
        writer.newLine();

        writer.newLine();
        writer.write(Utils.tab(indent) + "public final void marshal(DataOutput out, AmqpMarshaller marshaller) throws IOException, AmqpEncodingError{");
        writer.newLine();
        if ( isList() || isMap() ) {
            writer.write(Utils.tab(++indent) + "if (!dirty) {");
            writer.newLine();
            writer.write(Utils.tab(++indent) + "encoded.marshal(out);");
            writer.newLine();
            writer.write(Utils.tab(--indent) + "} else {");
            writer.newLine();
            writer.write(Utils.tab(++indent) + "bean().marshal(out, marshaller);");
            writer.newLine();
            writer.write(Utils.tab(indent) + "this.encoded = bean().getBuffer(marshaller).getEncoded();");
            writer.newLine();
            writer.write(Utils.tab(indent) + "dirty = false;");
            writer.newLine();
            writer.write(Utils.tab(--indent) + "}");
            writer.newLine();
        } else {
            writer.write(Utils.tab(++indent) + "encoded.marshal(out);");
            writer.newLine();
        }
        writer.write(Utils.tab(--indent) + "}");
        writer.newLine();
        return indent;
    }

    private int writeDescribedTypeBufferConstructors(BufferedWriter writer, int indent) throws UnknownTypeException, IOException {
        AmqpClass describedType = descriptor.resolveDescribedType();

        writer.newLine();
        writer.write(Utils.tab(indent++) + "public static class " + bufferMapping.getClassName() + " extends " + describedType.bufferMapping + " implements " + typeMapping + "{");
        writer.newLine();

        writer.newLine();
        writer.write(Utils.tab(indent) + "private " + beanMapping.getShortName() + " bean;");
        writer.newLine();

        // CONSTRUCTORS:
        writer.newLine();
        writer.write(Utils.tab(indent) + "protected " + bufferMapping.getShortName() + "(Encoded<" + getValueMapping() + "> encoded) {");
        writer.newLine();
        writer.write(Utils.tab(++indent) + "super(encoded);");
        writer.newLine();
        writer.write(Utils.tab(--indent) + "}");
        writer.newLine();
        return indent;
    }

    private int writeBufferBufferAccessor(BufferedWriter writer, int indent) throws IOException {
        writer.newLine();
        writer.write(Utils.tab(indent) + "public " + bufferMapping + " getBuffer(AmqpMarshaller marshaller) throws AmqpEncodingError{");
        writer.newLine();
        writer.write(Utils.tab(++indent) + "return this;");
        writer.newLine();
        writer.write(Utils.tab(--indent) + "}");
        writer.newLine();
        return indent;
    }

    private int writeBufferBeanAccessor(BufferedWriter writer, int indent) throws IOException {
        writer.newLine();
        writer.write(Utils.tab(indent) + "protected " + typeMapping + " bean() {");
        writer.newLine();
        writer.write(Utils.tab(++indent) + "if(bean == null) {");
        writer.newLine();
        writer.write(Utils.tab(++indent) + "bean = new " + beanMapping + "(encoded.getValue());");
        writer.newLine();
        writer.write(Utils.tab(indent) + "bean.buffer = this;");
        writer.newLine();
        writer.write(Utils.tab(--indent) + "}");
        writer.newLine();
        writer.write(Utils.tab(indent) + "return bean;");
        writer.newLine();
        writer.write(Utils.tab(--indent) + "}");
        writer.newLine();
        return indent;
    }

    private int writeFactoryMethods(BufferedWriter writer, int indent) throws IOException, UnknownTypeException {
        // Factory methods:
        writer.newLine();
        writer.write(Utils.tab(indent) + "public static " + bufferMapping + " create(Encoded<" + getEncodedType().valueMapping + "> encoded) {");
        writer.newLine();
        writer.write(Utils.tab(++indent) + "if(encoded.isNull()) {");
        writer.newLine();
        writer.write(Utils.tab(++indent) + "return null;");
        writer.newLine();
        writer.write(Utils.tab(--indent) + "}");
        writer.newLine();
        writer.write(Utils.tab(indent) + "return new " + bufferMapping + "(encoded);");
        writer.newLine();
        writer.write(Utils.tab(--indent) + "}");
        writer.newLine();

        writer.newLine();
        writer.write(Utils.tab(indent) + "public static " + bufferMapping + " create(DataInput in, AmqpMarshaller marshaller) throws IOException, AmqpEncodingError {");
        writer.newLine();
        writer.write(Utils.tab(++indent) + "return create(marshaller.unmarshal" + getEncodedType().typeMapping + "(in));");
        writer.newLine();
        writer.write(Utils.tab(--indent) + "}");
        writer.newLine();

        writer.newLine();
        writer.write(Utils.tab(indent) + "public static " + bufferMapping + " create(Buffer buffer, int offset, AmqpMarshaller marshaller) throws AmqpEncodingError {");
        writer.newLine();
        writer.write(Utils.tab(++indent) + "return create(marshaller.decode" + getEncodedType().typeMapping + "(buffer, offset));");
        writer.newLine();
        writer.write(Utils.tab(--indent) + "}");
        writer.newLine();
        return indent;
    }

    private int writeBufferEquals(BufferedWriter writer, int indent) throws IOException {
        // Equivalency:
        writer.newLine();
        writer.write(Utils.tab(indent) + "public boolean equals(Object o){");
        writer.newLine();
        writer.write(Utils.tab(++indent) + "return bean().equals(o);");
        writer.newLine();
        writer.write(Utils.tab(--indent) + "}");
        writer.newLine();

        writer.newLine();
        writer.write(Utils.tab(indent) + "public boolean equals(" + typeMapping + " o){");
        writer.newLine();
        writer.write(Utils.tab(++indent) + "return bean().equals(o);");
        writer.newLine();
        writer.write(Utils.tab(--indent) + "}");
        writer.newLine();

        writer.newLine();
        writer.write(Utils.tab(indent) + "public int hashCode() {");
        writer.newLine();
        writer.write(Utils.tab(++indent) + "return bean().hashCode();");
        writer.newLine();
        writer.write(Utils.tab(--indent) + "}");
        writer.newLine();

        if (isPrimitive() && !isList() && !isMap() && !getJavaType().startsWith("AmqpNull")) {
            writer.write(Utils.tab(indent) + "public int compareTo(" + getJavaType() + " o) {"  );
            writer.newLine();
            writer.write(Utils.tab(++indent) + " return getValue().compareTo(o.getValue());");
            writer.newLine();
            writer.write(Utils.tab(--indent) + "}");
            writer.newLine();
        }
        return indent;
    }

    private boolean writeFieldAccessors(BufferedWriter writer, int indent, boolean buffer) throws IOException, UnknownTypeException {
        boolean ret = false;

        for (AmqpField field : fields.values()) {
            ret = true;

            AmqpClass fieldClass = field.resolveAmqpFieldType();
            AmqpClass baseType = fieldClass.resolveBaseType();

            writeFieldAccessor(writer, indent, buffer, field, fieldClass, baseType);
        }
        if (isMap()) {
            writeMapOverrides(writer, indent, buffer);
        }
        if (isList()) {
            writeListOverrides(writer, indent, buffer);
        }
        if (isDescribed()) {
            writeDescribedTypeAccessors(writer, indent, buffer);
        }
        if (isRestricted()) {
            resolveRestrictedType().writeFieldAccessors(writer, indent, buffer);
        }
        if (isPrimitive() && !isMutable()) {
            // Getter:
            writer.newLine();
            writer.write(Utils.tab(indent) + "public " + valueMapping + " getValue() {");
            writer.newLine();
            if (!buffer) {
                if (isList()) {
                    writer.write(Utils.tab(++indent) + "return bean;");
                } else {
                    writer.write(Utils.tab(++indent) + "return bean.value;");
                }
            } else {
                writer.write(Utils.tab(++indent) + "return bean().getValue();");
            }
            writer.newLine();
            writer.write(Utils.tab(--indent) + "}");
            writer.newLine();
        }
        return ret;
    }

    private void writeDescribedTypeAccessors(BufferedWriter writer, int indent, boolean buffer) throws UnknownTypeException, IOException {
        if (descriptor.resolveDescribedType().isList()) {
            writeListDescribedTypeAccessors(writer, indent, buffer);
        } else if (descriptor.resolveDescribedType().isMap()) {
            writeMapDescribedTypeAccessors(writer, indent, buffer);
        }
    }

    private void writeMapDescribedTypeAccessors(BufferedWriter writer, int indent, boolean buffer) throws UnknownTypeException, IOException {
        if ( !buffer ) {
            writer.newLine();
            writeMapDescribedTypeGetter(writer, indent);
            writeMapDescribedTypeSetter(writer, indent);
            writeMapDescribedTypeEntryCount(writer, indent);
            writeMapDescribedTypeIterator(writer, indent);
        } else {
            descriptor.resolveDescribedType().writeFieldAccessors(writer, indent, buffer);
        }
    }

    private void writeMapDescribedTypeEntryCount(BufferedWriter writer, int indent) throws IOException {
        writer.newLine();
        writer.write(Utils.tab(indent) + "public int getEntryCount() {");
        writer.newLine();
        writer.write(Utils.tab(++indent) + "int rc = 0;");
        writer.newLine();
        for (AmqpField field : fields.values()) {
            writer.write(Utils.tab(indent) + "if ( " + field.getJavaName() + " != null ) {");
            writer.newLine();
            writer.write(Utils.tab(++indent) + "rc++;");
            writer.newLine();
            writer.write(Utils.tab(--indent) + "}");
            writer.newLine();
        }
        writer.write(Utils.tab(++indent) + "return rc;");
        writer.newLine();
        writer.write(Utils.tab(--indent) + "}");
        writer.newLine();
    }

    private void writeMapDescribedTypeIterator(BufferedWriter writer, int indent) throws IOException, UnknownTypeException {
        writer.newLine();
        TypeRegistry.JavaTypeMapping any = TypeRegistry.any().typeMapping;
        writer.write(Utils.tab(indent) + "public Iterator<Map.Entry<" + any + ", " + any + ">> iterator() {");
        writer.newLine();
        writer.newLine();
        writer.write(Utils.tab(++indent) + "//TODO - save this instance and just update it");
        writer.newLine();
        writer.write(Utils.tab(indent) + "Map<" + any + ", " + any + "> map = new HashMap<" + any + ", " + any + ">();");
        writer.newLine();
        writer.newLine();
        for (AmqpField field : fields.values()) {
            AmqpClass fieldClass = field.resolveAmqpFieldType();
            TypeRegistry.JavaTypeMapping valueType = fieldClass.getTypeMapping();
            writer.write(Utils.tab(indent) + "if ( " + field.getJavaName() + " != null ) {");
            writer.newLine();
            if (fieldClass.isEnumType()) {
                writer.write(Utils.tab(++indent) + "map.put(" + typeMapping + "." + Utils.toJavaConstant(field.getName()) + "_KEY, " + field.getJavaName() + ".getValue());");
            } else {
                writer.write(Utils.tab(++indent) + "map.put(" + typeMapping + "." + Utils.toJavaConstant(field.getName()) + "_KEY, " + field.getJavaName() + ");");
            }
            writer.newLine();
            writer.write(Utils.tab(--indent) + "}");
            writer.newLine();
        }
        writer.write(Utils.tab(indent) + "return map.entrySet().iterator();");
        writer.newLine();
        writer.write(Utils.tab(--indent) + "}");
        writer.newLine();
    }

    private void writeMapDescribedTypeSetter(BufferedWriter writer, int indent) throws IOException, UnknownTypeException {
        writer.newLine();
        writer.write(Utils.tab(indent) + "public void put(" + getMapKeyType() + " key, " + getMapValueType() + " value) {");
        writer.newLine();
        writer.write(Utils.tab(++indent) + "if ( key == null ) {");
        writer.newLine();
        writer.write(Utils.tab(++indent) + "return;");
        writer.newLine();
        writer.write(Utils.tab(--indent) + "}");
        writer.newLine();
        for (AmqpField field : fields.values()) {
            AmqpClass fieldClass = field.resolveAmqpFieldType();
            TypeRegistry.JavaTypeMapping valueType = fieldClass.getTypeMapping();
            writer.newLine();
            writer.write(Utils.tab(indent) + "if ( key.equals(" + typeMapping + "." + Utils.toJavaConstant(field.getName()) + "_KEY)) {");
            writer.newLine();
            if (fieldClass.isEnumType()) {
                writer.write(Utils.tab(++indent) + field.getJavaName() + " = " + valueType.getJavaType() + ".get((" + fieldClass.resolveRestrictedType().getJavaType() + ")value);");
            } else {
                writer.write(Utils.tab(++indent) + field.getJavaName() + " = (" + valueType + ") value;");
            }
            writer.newLine();
            writer.write(Utils.tab(--indent) + "}");
            writer.newLine();
        }
        writer.write(Utils.tab(--indent) + "}");
        writer.newLine();
    }

    private void writeMapDescribedTypeGetter(BufferedWriter writer, int indent) throws IOException, UnknownTypeException {
        writer.newLine();
        writer.write(Utils.tab(indent) + "public " + getMapValueType() + " get(Object key) {");
        writer.newLine();
        writer.write(Utils.tab(++indent) + "if ( key == null ) {");
        writer.newLine();
        writer.write(Utils.tab(++indent) + "return null;");
        writer.newLine();
        writer.write(Utils.tab(--indent) + "}");

        writer.newLine();
        writer.write(Utils.tab(indent) + "copyCheck();");
        writer.newLine();

        for (AmqpField field : fields.values()) {
            writer.newLine();
            writer.write(Utils.tab(indent) + "if ( key.equals(" + typeMapping + "." + Utils.toJavaConstant(field.getName()) + "_KEY)) {");
            writer.newLine();
            AmqpClass fieldClass = field.resolveAmqpFieldType();
            if (fieldClass.isEnumType()) {
                writer.write(Utils.tab(++indent) + "return " + field.getJavaName() + ".getValue();");
            } else {
                writer.write(Utils.tab(++indent) + "return " + field.getJavaName() + ";");
            }
            writer.newLine();
            writer.write(Utils.tab(--indent) + "}");
            writer.newLine();
        }
        writer.write(Utils.tab(indent) + "return null;");
        writer.newLine();
        writer.write(Utils.tab(--indent) + "}");
        writer.newLine();
    }

    private void writeListDescribedTypeAccessors(BufferedWriter writer, int indent, boolean buffer) throws IOException, UnknownTypeException {
        if ( !buffer ) {
            writer.newLine();
            writeListDescribedTypeSetter(writer, indent);
            writeListDescribedTypeGetter(writer, indent);

            writer.newLine();
            writer.write(Utils.tab(indent) + "public int getListCount() {");
            writer.newLine();
            writer.write(Utils.tab(++indent) + "return " + fields.size() + ";");
            writer.newLine();
            writer.write(Utils.tab(--indent) + "}");
            writer.newLine();

            writer.newLine();
            writer.write(Utils.tab(indent) + "public " + descriptor.resolveDescribedType().getValueMapping() + " getValue() {");
            writer.newLine();
            writer.write(Utils.tab(++indent) + "return bean;");
            writer.newLine();
            writer.write(Utils.tab(--indent) + "}");
            writer.newLine();

            writer.newLine();
            writer.write(Utils.tab(indent) + "public Iterator<AmqpType<?, ?>> iterator() {");
            writer.newLine();
            writer.write(Utils.tab(++indent) + "return new AmqpListIterator<" + TypeRegistry.any().typeMapping + ">(bean);");
            writer.newLine();
            writer.write(Utils.tab(--indent) + "}");
            writer.newLine();

            writer.newLine();
            writer.write(Utils.tab(indent) + "public AmqpType<?, ?>[] toArray() {");
            writer.newLine();

            writer.write(Utils.tab(++indent) + "throw new IllegalArgumentException(\"toArray() cannot be called on described types\");");
            writer.newLine();
            writer.write(Utils.tab(--indent) + "}");
            writer.newLine();
        } else {
            descriptor.resolveDescribedType().writeFieldAccessors(writer, indent, buffer);
        }
    }

    private void writeListDescribedTypeGetter(BufferedWriter writer, int indent) throws IOException, UnknownTypeException {
        writer.newLine();
        writer.write(Utils.tab(indent) + "public " + TypeRegistry.any().typeMapping + " get(int index) {");
        writer.newLine();
        writer.write(Utils.tab(++indent) + "switch(index) {");
        writer.newLine();
        int f = 0;
        for (AmqpField field : fields.values()) {
            writer.write(Utils.tab(indent) + "case " + f + ": {");
            writer.newLine();
            AmqpClass fieldClass = field.resolveAmqpFieldType();
            if (fieldClass.isEnumType()) {
                writer.write(Utils.tab(++indent) + "if(" + field.getJavaName() + " == null) {");
                writer.newLine();
                writer.write(Utils.tab(++indent) + "return null;");
                writer.newLine();
                writer.write(Utils.tab(--indent) + "}");
                writer.newLine();
                writer.write(Utils.tab(indent) + "return " + field.getJavaName() + ".getValue();");
            } else {
                writer.write(Utils.tab(++indent) + "return bean." + field.getJavaName() + ";");
            }

            writer.newLine();
            writer.write(Utils.tab(--indent) + "}");
            writer.newLine();
            f++;
        }
        writer.write(Utils.tab(indent) + "default : {");
        writer.newLine();
        writer.write(Utils.tab(++indent) + "throw new IndexOutOfBoundsException(String.valueOf(index));");
        writer.newLine();
        writer.write(Utils.tab(--indent) + "}");
        writer.newLine();
        writer.write(Utils.tab(indent) + "}");
        writer.newLine();
        writer.write(Utils.tab(--indent) + "}");
        writer.newLine();
    }

    private void writeListDescribedTypeSetter(BufferedWriter writer, int indent) throws IOException, UnknownTypeException {
        writer.write(Utils.tab(indent) + "public void set(int index, " + TypeRegistry.any().typeMapping + " value) {");
        writer.newLine();
        writer.write(Utils.tab(++indent) + "switch(index) {");
        writer.newLine();
        int f = 0;
        for (AmqpField field : fields.values()) {
            writer.write(Utils.tab(indent) + "case " + f + ": {");
            writer.newLine();
            AmqpClass fieldClass = field.resolveAmqpFieldType();
            if (fieldClass.isEnumType()) {
                writer.write(Utils.tab(++indent) + "set" + Utils.capFirst(field.getJavaName()) + "(" + fieldClass.typeMapping + ".get((" + fieldClass.resolveRestrictedType().typeMapping + ")value));");
            } else {
                writer.write(Utils.tab(++indent) + "set" + Utils.capFirst(field.getJavaName()) + "((" + fieldClass.typeMapping + ") value);");
            }
            writer.newLine();
            writer.write(Utils.tab(indent) + "break;");
            writer.newLine();
            writer.write(Utils.tab(--indent) + "}");
            writer.newLine();
            f++;
        }
        writer.write(Utils.tab(indent) + "default : {");
        writer.newLine();
        writer.write(Utils.tab(++indent) + "throw new IndexOutOfBoundsException(String.valueOf(index));");
        writer.newLine();
        writer.write(Utils.tab(--indent) + "}");
        writer.newLine();
        writer.write(Utils.tab(indent) + "}");
        writer.newLine();
        writer.write(Utils.tab(--indent) + "}");
        writer.newLine();
    }

    private void writeListOverrides(BufferedWriter writer, int indent, boolean buffer) throws IOException {
        if (!buffer) {
            writeListBeanOverrides(writer, indent);
        } else {
            writeListBufferOverrides(writer, indent);
        }
    }

    private void writeListBufferOverrides(BufferedWriter writer, int indent) throws IOException {
        writer.newLine();
        writer.write(Utils.tab(indent) + "public void set(int index, " + TypeRegistry.any().typeMapping + " value) {");
        writer.newLine();
        writer.write(Utils.tab(++indent) + "bean().set(index, value);");
        writer.newLine();
        writer.write(Utils.tab(--indent) + "}");
        writer.newLine();

        writer.newLine();
        writer.write(Utils.tab(indent) + "public " + TypeRegistry.any().typeMapping + " get(int index) {");
        writer.newLine();
        writer.write(Utils.tab(++indent) + "return bean().get(index);");
        writer.newLine();
        writer.write(Utils.tab(--indent) + "}");
        writer.newLine();

        writer.newLine();
        writer.write(Utils.tab(indent) + "public int getListCount() {");
        writer.newLine();
        writer.write(Utils.tab(++indent) + "return bean().getListCount();");
        writer.newLine();
        writer.write(Utils.tab(--indent) + "}");
        writer.newLine();

        writer.newLine();
        writer.write(Utils.tab(indent) + "public Iterator<" + TypeRegistry.any().typeMapping + "> iterator() {");
        writer.newLine();
        writer.write(Utils.tab(++indent) + "return bean().iterator();");
        writer.newLine();
        writer.write(Utils.tab(--indent) + "}");
        writer.newLine();

        writer.newLine();
        writer.write(Utils.tab(indent) + "public " + TypeRegistry.any().typeMapping + "[] toArray() {");
        writer.newLine();
        writer.write(Utils.tab(++indent) + "return bean().toArray();");
        writer.newLine();
        writer.write(Utils.tab(--indent) + "}");
        writer.newLine();
    }

    private void writeListBeanOverrides(BufferedWriter writer, int indent) throws IOException {
        writer.newLine();
        writer.write(Utils.tab(indent) + "public void set(int index, " + TypeRegistry.any().typeMapping + " value) {");
        writer.newLine();
        writer.write(Utils.tab(++indent) + "copyCheck();");
        writer.newLine();
        writer.write(Utils.tab(indent) + "bean.value.set(index, value);");
        writer.newLine();
        writer.write(Utils.tab(--indent) + "}");
        writer.newLine();

        writer.newLine();
        writer.write(Utils.tab(indent) + "public " + TypeRegistry.any().typeMapping + " get(int index) {");
        writer.newLine();
        writer.write(Utils.tab(++indent) + "return bean.value.get(index);");
        writer.newLine();
        writer.write(Utils.tab(--indent) + "}");
        writer.newLine();

        writer.newLine();
        writer.write(Utils.tab(indent) + "public int getListCount() {");
        writer.newLine();
        writer.write(Utils.tab(++indent) + "return bean.value.getListCount();");
        writer.newLine();
        writer.write(Utils.tab(--indent) + "}");
        writer.newLine();

        writer.newLine();
        writer.write(Utils.tab(indent) + "public Iterator<" + TypeRegistry.any().typeMapping + "> iterator() {");
        writer.newLine();
        writer.write(Utils.tab(++indent) + "return new AmqpListIterator<" + TypeRegistry.any().typeMapping + ">(bean.value);");
        writer.newLine();
        writer.write(Utils.tab(--indent) + "}");
        writer.newLine();

        writer.newLine();
        writer.write(Utils.tab(indent) + "public " + TypeRegistry.any().typeMapping + "[] toArray() {");
        writer.newLine();
        writer.write(Utils.tab(++indent) + "return bean.value.toArray();");
        writer.newLine();
        writer.write(Utils.tab(--indent) + "}");
        writer.newLine();
    }

    private void writeMapOverrides(BufferedWriter writer, int indent, boolean buffer) throws IOException {
        if (!buffer) {
            writeMapBeanOverrides(writer, indent);
        } else {
            writeMapBufferOverrides(writer, indent);
        }
    }

    private void writeMapBufferOverrides(BufferedWriter writer, int indent) throws IOException {
        writer.write(Utils.tab(indent) + "public void put(" + getMapKeyType() + " key, " + getMapValueType() + " value) {");
        writer.newLine();
        writer.write(Utils.tab(++indent) + "bean().put(key, value);");
        writer.newLine();
        writer.write(Utils.tab(--indent) + "}");
        writer.newLine();

        writer.newLine();
        writer.write(Utils.tab(indent) + "public " + getMapValueType() + " get(Object key) {");
        writer.newLine();
        writer.write(Utils.tab(++indent) + "return bean().get(key);");
        writer.newLine();
        writer.write(Utils.tab(--indent) + "}");
        writer.newLine();

        writer.newLine();
        writer.write(Utils.tab(indent) + "public int getEntryCount() {");
        writer.newLine();
        writer.write(Utils.tab(++indent) + "return bean().getEntryCount();");
        writer.newLine();
        writer.write(Utils.tab(--indent) + "}");
        writer.newLine();

        writer.newLine();
        writer.write(Utils.tab(indent) + "public Iterator<Map.Entry<" + getMapKeyType() + ", " + getMapValueType() + ">> iterator() {");
        writer.newLine();
        writer.write(Utils.tab(++indent) + "return bean().iterator();");
        writer.newLine();
        writer.write(Utils.tab(--indent) + "}");
        writer.newLine();
    }

    private void writeMapBeanOverrides(BufferedWriter writer, int indent) throws IOException {
        writer.write(Utils.tab(indent) + "public void put(" + getMapKeyType() + " key," + getMapValueType() + " value) {");
        writer.newLine();
        writer.write(Utils.tab(++indent) + "copyCheck();");
        writer.newLine();
        writer.write(Utils.tab(indent) + "bean.value.put(key, value);");
        writer.newLine();
        writer.write(Utils.tab(--indent) + "}");
        writer.newLine();

        writer.newLine();
        writer.write(Utils.tab(indent) + "public " + getMapValueType() + " get(Object key) {");
        writer.newLine();
        writer.write(Utils.tab(++indent) + "return bean.value.get(key);");
        writer.newLine();
        writer.write(Utils.tab(--indent) + "}");
        writer.newLine();

        writer.newLine();
        writer.write(Utils.tab(indent) + "public int getEntryCount() {");
        writer.newLine();
        writer.write(Utils.tab(++indent) + "return bean.value.getEntryCount();");
        writer.newLine();
        writer.write(Utils.tab(--indent) + "}");
        writer.newLine();

        writer.newLine();
        writer.write(Utils.tab(indent) + "public Iterator<Map.Entry<" + getMapKeyType() + ", " + getMapValueType() + ">> iterator() {");
        writer.newLine();
        writer.write(Utils.tab(++indent) + "return bean.value.iterator();");
        writer.newLine();
        writer.write(Utils.tab(--indent) + "}");
        writer.newLine();
    }

    private void writeFieldAccessor(BufferedWriter writer, int indent, boolean buffer, AmqpField field, AmqpClass fieldClass, AmqpClass baseType) throws UnknownTypeException, IOException {
        if (!buffer) {
            writeBeanFieldAccessor(writer, indent, field, fieldClass, baseType);
        } else {
            writeBufferFieldAccessor(writer, indent, field, fieldClass, baseType);
        }
    }

    private void writeBufferFieldAccessor(BufferedWriter writer, int indent, AmqpField field, AmqpClass fieldClass, AmqpClass baseType) throws UnknownTypeException, IOException {
        writeBufferFieldSetter(writer, indent, field, fieldClass, baseType);
        writeBufferFieldGetter(writer, indent, field, fieldClass);
    }

    private void writeBufferFieldGetter(BufferedWriter writer, int indent, AmqpField field, AmqpClass fieldClass) throws UnknownTypeException, IOException {
        // Getter:
        TypeRegistry.JavaTypeMapping returnType = fieldClass.isPrimitive() ? fieldClass.getValueMapping() : fieldClass.typeMapping;
        writer.newLine();
        writer.write(Utils.tab(indent) + "public final " + returnType + " get" + Utils.capFirst(field.getJavaName()) + "() {");
        writer.newLine();
        writer.write(Utils.tab(++indent) + "return bean().get" + Utils.capFirst(field.getJavaName()) + "();");
        writer.newLine();
        writer.write(Utils.tab(--indent) + "}");
        writer.newLine();
    }

    private void writeBufferFieldSetter(BufferedWriter writer, int indent, AmqpField field, AmqpClass fieldClass, AmqpClass baseType) throws UnknownTypeException, IOException {
        // Setters:
        if (baseType.isPrimitive() && !baseType.isAny() && !fieldClass.isEnumType() && !baseType.isDescribed() && !baseType.isMutable()) {
        writer.newLine();
        writer.write(Utils.tab(indent) + "public void set" + Utils.capFirst(field.getJavaName()) + "(" + baseType.getValueMapping() + " " + Utils.toJavaName(field.getName()) + ") {");
        writer.newLine();
        writer.write(Utils.tab(++indent) + "dirty = true;");
        writer.newLine();
        writer.write(Utils.tab(indent) + "bean().set" + Utils.capFirst(field.getJavaName()) + "(" + Utils.toJavaName(field.getName()) + ");");
        writer.newLine();
        writer.write(Utils.tab(--indent) + "}");
        writer.newLine();

        if (baseType.getValueMapping().hasPrimitiveType()) {
            writer.newLine();
            writer.write(Utils.tab(indent) + "public void set" + Utils.capFirst(field.getJavaName()) + "(" + baseType.getValueMapping().getPrimitiveType() + " " + Utils.toJavaName(field.getName()) + ") {");
            writer.newLine();
            writer.write(Utils.tab(++indent) + "dirty = true;");
            writer.newLine();
            writer.write(Utils.tab(indent) + "bean().set" + Utils.capFirst(field.getJavaName()) + "(" + Utils.toJavaName(field.getName()) + ");");
            writer.newLine();
            writer.write(Utils.tab(--indent) + "}");
            writer.newLine();
            writer.newLine();
        }
    }
        writer.newLine();
        writer.write(Utils.tab(indent) + "public final void set" + Utils.capFirst(field.getJavaName()) + "(" + fieldClass.getTypeMapping() + " " + Utils.toJavaName(field.getName()) + ") {");
        writer.newLine();
        writer.write(Utils.tab(++indent) + "dirty = true;");
        writer.newLine();
        writer.write(Utils.tab(indent) + "bean().set" + Utils.capFirst(field.getJavaName()) + "(" + Utils.toJavaName(field.getName()) + ");");
        writer.newLine();
        writer.write(Utils.tab(--indent) + "}");
        writer.newLine();
    }

    private void writeBeanFieldAccessor(BufferedWriter writer, int indent, AmqpField field, AmqpClass fieldClass, AmqpClass baseType) throws UnknownTypeException, IOException {
        writeBeanFieldSetter(writer, indent, field, fieldClass, baseType);
        writeBeanFieldGetter(writer, indent, field, fieldClass);
    }

    private void writeBeanFieldGetter(BufferedWriter writer, int indent, AmqpField field, AmqpClass fieldClass) throws UnknownTypeException, IOException {
        // Getter:
        TypeRegistry.JavaTypeMapping returnType = fieldClass.isPrimitive() ? fieldClass.getValueMapping() : fieldClass.typeMapping;
        writer.write(Utils.tab(indent) + "public final " + returnType + " get" + Utils.capFirst(field.getJavaName()) + "() {");
        writer.newLine();
        if (!fieldClass.isAny() && fieldClass.isPrimitive() && !fieldClass.isMutable()) {
        writer.write(Utils.tab(++indent) + "if ( bean." + field.getJavaName() + " == null ) {");
        writer.newLine();
        writer.write(Utils.tab(++indent) + "return null;");
        writer.newLine();
        writer.write(Utils.tab(--indent) + "}");
        writer.newLine();
        writer.write(Utils.tab(indent) + "return bean." + field.getJavaName() + ".getValue();");
    } else {
        writer.write(Utils.tab(++indent) + "return bean." + field.getJavaName() + ";");
    }
        writer.newLine();
        writer.write(Utils.tab(--indent) + "}");
        writer.newLine();
    }

    private void writeBeanFieldSetter(BufferedWriter writer, int indent, AmqpField field, AmqpClass fieldClass, AmqpClass baseType) throws UnknownTypeException, IOException {
        // Setters:
        if (baseType.isPrimitive() && !baseType.isAny() && !fieldClass.isEnumType() && !baseType.isDescribed() && !baseType.isMutable()) {
            writer.newLine();
            writer.write(Utils.tab(indent) + "public void set" + Utils.capFirst(field.getJavaName()) + "(" + baseType.valueMapping + " " + Utils.toJavaName(field.getName()) + ") {");
            writer.newLine();
            writer.write(Utils.tab(++indent) + "set" + Utils.capFirst(field.getJavaName()) + "(TypeFactory.create" + fieldClass.getJavaType() + "(" + Utils.toJavaName(field.getName()) + "));");
            writer.newLine();
            writer.write(Utils.tab(--indent) + "}");
            writer.newLine();
            writer.newLine();

            if (baseType.getValueMapping().hasPrimitiveType()) {
                writer.newLine();
                writer.write(Utils.tab(indent) + "public void set" + Utils.capFirst(field.getJavaName()) + "(" + baseType.getValueMapping().getPrimitiveType() + " " + Utils.toJavaName(field.getName()) + ") {");
                writer.newLine();
                writer.write(Utils.tab(++indent) + "set" + Utils.capFirst(field.getJavaName()) + "(TypeFactory.create" + fieldClass.getJavaType() + "(" + Utils.toJavaName(field.getName()) + "));");
                writer.newLine();
                writer.write(Utils.tab(--indent) + "}");
                writer.newLine();
                writer.newLine();
            }
        }

        writer.newLine();
        writer.write(Utils.tab(indent) + "public final void set" + Utils.capFirst(field.getJavaName()) + "(" + fieldClass.typeMapping + " " + Utils.toJavaName(field.getName()) + ") {");
        writer.newLine();
        writer.write(Utils.tab(++indent) + "copyCheck();");
        writer.newLine();
        writer.write(Utils.tab(indent) + "bean." + field.getJavaName() + " = " + Utils.toJavaName(field.getName()) + ";");
        writer.newLine();
        writer.write(Utils.tab(--indent) + "}");
        writer.newLine();
        writer.newLine();
    }

    private void writeToString(BufferedWriter writer, int indent, boolean buffer) throws IOException, UnknownTypeException {
        // toString()
        if ( isDescribed() ) {
            if (!buffer) {
                writer.newLine();
                writer.write(Utils.tab(indent) + "public String toString() {");
                writer.newLine();
                writer.write(Utils.tab(++indent) + "String ret = \"" + this.getJavaType() + "{\";");
                writer.newLine();
                writer.write(Utils.tab(indent) + "String fields = new String();");
                writer.newLine();
                for ( AmqpField field : fields.values() ) {
                    writer.write(Utils.tab(indent) + "if ( " + field.getJavaName() + " != null ) {");
                    writer.newLine();
                    writer.write(Utils.tab(++indent) + "fields += \"" + field.getJavaName() + "=\" + " + field.getJavaName() + " + \" \";");
                    writer.newLine();
                    writer.write(Utils.tab(--indent) + "}");
                    writer.newLine();
                }
                writer.write(Utils.tab(indent) + "ret += fields.trim();");
                writer.newLine();
                writer.write(Utils.tab(indent) + "ret += \"}\";");
                writer.newLine();
                writer.write(Utils.tab(indent) + "return ret;");
                writer.newLine();
                writer.write(Utils.tab(--indent) + "}");
                writer.newLine();
            } else {
                writer.newLine();
                writer.write(Utils.tab(indent) + "public String toString() {");
                writer.newLine();
                writer.write(Utils.tab(++indent) + "return bean().toString();");
                writer.newLine();
                writer.write(Utils.tab(--indent) + "}");
                writer.newLine();
            }
        } else if ( isList() ) {
            if ( !buffer ) {
                writer.newLine();
                writer.write(Utils.tab(indent) + "public String toString() {");
                writer.newLine();
                writer.write(Utils.tab(++indent) + "String rc = \"AmqpList{\";");
                writer.newLine();
                writer.write(Utils.tab(indent) + "String contents = new String();");
                writer.newLine();
                writer.write(Utils.tab(indent) + "for (int i=0; i < getListCount(); i++) {");
                writer.newLine();
                writer.write(Utils.tab(++indent) + "contents += \"[\" + i + \"]=\" + bean.value.get(i);");
                writer.newLine();
                writer.write(Utils.tab(--indent) + "}");
                writer.newLine();
                writer.write(Utils.tab(indent) + "rc += contents.trim();");
                writer.newLine();
                writer.write(Utils.tab(indent) + "rc += \"}\";");
                writer.newLine();
                writer.write(Utils.tab(indent) + "return rc;");
                writer.newLine();
                writer.write(Utils.tab(--indent) + "}");
                writer.newLine();
            } else {
                writer.newLine();
                writer.write(Utils.tab(indent) + "public String toString() {");
                writer.newLine();
                writer.write(Utils.tab(++indent) + "return bean().toString();");
                writer.newLine();
                writer.write(Utils.tab(--indent) + "}");
                writer.newLine();
            }
        } else if (isPrimitive() || isRestricted()) {
            writer.newLine();
            writer.write(Utils.tab(indent) + "public String toString() {");
            writer.newLine();
            String returnStatement = "return \"\"";
            boolean mapFilter = isMap();
            if ( !mapFilter && isRestricted() ) {
                mapFilter = resolveRestrictedType().isMap();
            }
            if ( !mapFilter ) {
                if (buffer) {
                    returnStatement += " + bean().getValue()";
                } else {
                    returnStatement += " + bean.value";
                }
            }
            returnStatement += ";";
            writer.write(Utils.tab(++indent) + returnStatement);
            writer.newLine();
            writer.write(Utils.tab(--indent) + "}");
            writer.newLine();
        }
    }

    private void writeBeanInterface(BufferedWriter writer, int indent) throws IOException, UnknownTypeException {

        if (isDescribed()) {

            // Write out symbol constants
            if (resolveBaseType().isMap()) {
                writer.newLine();
                for (AmqpField field : fields.values()) {

                    AmqpClass symbolClass = TypeRegistry.resolveAmqpClass("symbol");
                    Utils.writeJavaComment(writer, indent, "Key for: " + field.getLabel());
                    writer.write(Utils.tab(1) + "public static final " + symbolClass.typeMapping + " " + Utils.toJavaConstant(field.getName()) + "_KEY = TypeFactory.create" + symbolClass.typeMapping + "(\""
                            + field.getName() + "\");");

                    writer.newLine();
                }
                writer.newLine();
            }

            writer.newLine();
            for (AmqpField field : fields.values()) {

                AmqpClass fieldClass = field.resolveAmqpFieldType();
                AmqpClass baseType = fieldClass.resolveBaseType();

                if (baseType.isPrimitive() && !baseType.isAny() && !fieldClass.isEnumType() && !baseType.isDescribed() && !baseType.isMutable()) {
                    // Setter:
                    writer.newLine();
                    field.getDoc().parseFromDoc(fieldClass.doc.docs);
                    field.writeJavaDoc(writer, indent);
                    writer.write(Utils.tab(indent) + "public void set" + Utils.capFirst(field.getJavaName()) + "(" + baseType.valueMapping + " " + Utils.toJavaName(field.getName()) + ");");
                    writer.newLine();

                    if (baseType.getValueMapping().hasPrimitiveType()) {
                        writer.newLine();
                        field.writeJavaDoc(writer, indent);
                        writer.write(Utils.tab(indent) + "public void set" + Utils.capFirst(field.getJavaName()) + "(" + baseType.getValueMapping().getPrimitiveType() + " " + Utils.toJavaName(field.getName()) + ");");
                        writer.newLine();
                    }

                }

                // Setters:
                writer.newLine();
                field.writeJavaDoc(writer, indent);
                writer.write(Utils.tab(1) + "public void set" + Utils.capFirst(field.getJavaName()) + "(" + fieldClass.typeMapping + " " + Utils.toJavaName(field.getName()) + ");");
                writer.newLine();

                // Getter:
                TypeRegistry.JavaTypeMapping returnType = fieldClass.isPrimitive() ? fieldClass.getValueMapping() : fieldClass.typeMapping;
                writer.newLine();
                field.writeJavaDoc(writer, indent);
                writer.write(Utils.tab(indent) + "public " + returnType + " get" + Utils.capFirst(field.getJavaName()) + "();");
                writer.newLine();
            }
        }

        if (isMap()) {
            doc.writeJavaDoc(writer, indent);
            writer.write(Utils.tab(indent) + "public void put(" + getMapKeyType() + " key, " + getMapValueType() + " value);");
            writer.newLine();
            writer.write(Utils.tab(indent) + "public " + getMapValueType() + " get(Object key);");
            writer.newLine();

        } else if (isList()) {
            doc.writeJavaDoc(writer, indent);
            writer.write(Utils.tab(indent) + "public void set(int index, " + TypeRegistry.any().typeMapping + " value);");
            writer.newLine();
            writer.write(Utils.tab(indent) + "public " + TypeRegistry.any().typeMapping + " get(int index);");
            writer.newLine();
            writer.write(Utils.tab(indent) + "public int getListCount();");
            writer.newLine();
            writer.write(Utils.tab(indent) + "public " + TypeRegistry.any().typeMapping + "[] toArray();");
            writer.newLine();
        }

        if (isPrimitive() && !isMutable()) {
            // Getter:
            writer.newLine();
            writer.write(Utils.tab(1) + "public " + valueMapping + " getValue();");
            writer.newLine();
        }
    }

    private void generateMarshaller(Generator generator) throws IOException, UnknownTypeException {
        if (!(isPrimitive() || descriptor != null)) {
            return;
        }

        String packageName = generator.getMarshallerPackage();

        File file = new File(generator.getOutputDirectory() + File.separator + new String(packageName).replace(".", File.separator) + File.separator + typeMapping.getClassName() + "Marshaller.java");
        file.getParentFile().mkdirs();
        if (file.exists()) {
            file.delete();
        }
        BufferedWriter writer = new BufferedWriter(new FileWriter(file));

        Utils.writeJavaCopyWrite(writer);
        writer.write("package " + packageName + ";\n");
        writer.newLine();
        if (writeImports(writer, generator, true)) {
            writer.newLine();
        }
        Utils.writeAutoGeneratedWarning(writer, 0);

        // Write out the descriptor (for compound types):
        if (descriptor != null) {
            writer.write("public class " + typeMapping.getShortName() + "Marshaller implements DescribedTypeMarshaller<" + typeMapping + ">{");
            writer.newLine();

            writer.newLine();
            writer.write(Utils.tab(1) + "static final " + typeMapping + "Marshaller SINGLETON = new " + typeMapping + "Marshaller();");
            writer.newLine();
            writer.write(Utils.tab(1) + "private static final Encoded<" + getValueMapping() + "> NULL_ENCODED = new NullEncoded<" + getValueMapping() + ">();");
            writer.newLine();

            writer.newLine();
            writer.write(Utils.tab(1) + "public static final String SYMBOLIC_ID = \"" + descriptor.getSymbolicName() + "\";");
            writer.newLine();
            writer.write(Utils.tab(1) + "//Format code: " + descriptor.getFormatCode() + ":");
            writer.newLine();
            writer.write(Utils.tab(1) + "public static final long CATEGORY = " + descriptor.getCategory() + ";");
            writer.newLine();
            writer.write(Utils.tab(1) + "public static final long DESCRIPTOR_ID = " + descriptor.getDescriptorId() + ";");
            writer.newLine();
            writer.write(Utils.tab(1) + "public static final long NUMERIC_ID = CATEGORY << 32 | DESCRIPTOR_ID; //(" + (descriptor.getCategory() << 32 | descriptor.getDescriptorId()) + "L)");
            writer.newLine();
            /*
            writer.write(Utils.tab(1) + "//Hard coded descriptor:");
            writer.newLine();
            writer.write(Utils.tab(1) + "public static final EncodedBuffer DESCRIPTOR = FormatCategory.createBuffer(AmqpSymbolMarshaller.createEncoded(SYMBOLIC_ID).getBuffer(), 0);");
            */
            writer.write(Utils.tab(1) + "//Hard coded descriptor:");
            writer.newLine();
            writer.write(Utils.tab(1) + "public static final EncodedBuffer DESCRIPTOR = FormatCategory.createBuffer(new Buffer(new byte [] {");
            writer.newLine();
            // TODO retrieve ulong encoding from the ulong itself:
            writer.write(Utils.tab(2) + "(byte) 0x80,                                         // ulong descriptor encoding)");
            writer.newLine();
            // Add the category code:
            writer.write(Utils.tab(2));
            String categoryHex = Utils.padHex(descriptor.getFormatCode().substring(2, descriptor.getFormatCode().indexOf(":")), 8);
            for (int i = 0; i < 8; i += 2) {
                writer.write("(byte) 0x" + categoryHex.substring(i, i + 2) + ", ");
            }
            writer.write(" // CATEGORY CODE");
            writer.newLine();
            writer.write(Utils.tab(2));
            // Add the descriptor id code:
            String descriptorIdHex = Utils.padHex(descriptor.getFormatCode().substring(descriptor.getFormatCode().indexOf(":") + 3), 8);

            for (int i = 0; i < 8; i += 2) {
                writer.write("(byte) 0x" + descriptorIdHex.substring(i, i + 2));
                if (i < 6) {
                    writer.write(", ");
                }
            }
            writer.write("   // DESCRIPTOR ID CODE");
            writer.newLine();
            writer.write(Utils.tab(1) + "}), 0);");
            writer.newLine();


            AmqpClass describedType = descriptor.resolveDescribedType();

            if (!(describedType.isMap() || describedType.isList())) {
                throw new UnknownTypeException("Support for " + descriptor.getDescribedType() + " as a described type isn't yet implemented");
            }

            writeEncodings(writer);

            writer.newLine();
            writer.write(Utils.tab(1) + "public static final Encoded<" + getValueMapping() + "> encode(" + typeMapping + " value) throws AmqpEncodingError {");
            writer.newLine();
            writer.write(Utils.tab(2) + "return new " + typeMapping.getJavaType() + "Encoded(value);");
            writer.newLine();
            writer.write(Utils.tab(1) + "}");
            writer.newLine();

            writer.newLine();
            writer.write(Utils.tab(1) + "static final Encoded<" + getValueMapping() + "> createEncoded(Buffer source, int offset) throws AmqpEncodingError {");
            writer.newLine();
            writer.write(Utils.tab(2) + "return createEncoded(FormatCategory.createBuffer(source, offset));");
            writer.newLine();
            writer.write(Utils.tab(1) + "}");
            writer.newLine();

            writer.newLine();
            writer.write(Utils.tab(1) + "static final Encoded<" + getValueMapping() + "> createEncoded(DataInput in) throws IOException, AmqpEncodingError {");
            writer.newLine();
            writer.write(Utils.tab(2) + "return createEncoded(FormatCategory.createBuffer(in.readByte(), in));");
            writer.newLine();
            writer.write(Utils.tab(1) + "}");
            writer.newLine();

            writer.newLine();
            writer.write(Utils.tab(1) + "static final Encoded<" + getValueMapping() + "> createEncoded(EncodedBuffer buffer) throws AmqpEncodingError {");
            writer.newLine();
            writer.write(Utils.tab(2) + "byte fc = buffer.getEncodingFormatCode();");
            writer.newLine();
            writer.write(Utils.tab(2) + "if (fc == Encoder.NULL_FORMAT_CODE) {");
            writer.newLine();
            writer.write(Utils.tab(3) + "return NULL_ENCODED;");
            writer.newLine();
            writer.write(Utils.tab(2) + "}");
            writer.newLine();
            writer.newLine();
            writer.write(Utils.tab(2) + "DescribedBuffer db = buffer.asDescribed();");
            writer.newLine();
            writer.write(Utils.tab(2) + TypeRegistry.any().typeMapping + " descriptor = AmqpMarshaller.SINGLETON.decodeType(db.getDescriptorBuffer());");
            writer.newLine();
            writer.write(Utils.tab(2) + "if(!(descriptor instanceof AmqpUlong && ((AmqpUlong)descriptor).getValue().longValue() == NUMERIC_ID ||");
            writer.newLine();
            writer.write(Utils.tab(3) + "   descriptor instanceof AmqpSymbol && ((AmqpSymbol)descriptor).getValue().equals(SYMBOLIC_ID))) {");
            writer.newLine();
            writer.write(Utils.tab(3) + "throw new UnexpectedTypeException(\"descriptor mismatch: \" + descriptor);");
            writer.newLine();
            writer.write(Utils.tab(2) + "}");
            writer.newLine();
            writer.write(Utils.tab(2) + "return new " + getJavaType() + "Encoded(db);");
            writer.newLine();
            writer.write(Utils.tab(1) + "}");
            writer.newLine();

            writer.newLine();
            writer.write(Utils.tab(1) + "public final " + bufferMapping + " decodeDescribedType(" + TypeRegistry.any().typeMapping + " descriptor, DescribedBuffer encoded) throws AmqpEncodingError {");
            writer.newLine();
            writer.write(Utils.tab(2) + "return " + bufferMapping + ".create(new " + getJavaType() + "Encoded(encoded));");
            writer.newLine();
            writer.write(Utils.tab(1) + "}");
            writer.newLine();

        }
        // Add accessors for primitive encoded sizes:
        else if (isPrimitive()) {

            writer.write("public class " + typeMapping.getShortName() + "Marshaller {");
            writer.newLine();

            writer.newLine();
            writer.write(Utils.tab(1) + "private static final Encoder ENCODER = Encoder.SINGLETON;");
            writer.newLine();
            writer.write(Utils.tab(1) + "private static final Encoded<" + getValueMapping() + "> NULL_ENCODED = new NullEncoded<" + getValueMapping() + ">();");

            writer.newLine();

            writeEncodings(writer);

            writer.newLine();
            // Handle fixed width encodings:
            if (!hasMultipleEncodings() && !hasNonFixedEncoding()) {
                writer.newLine();
                writer.write(Utils.tab(1) + "public static final Encoded<" + getValueMapping() + "> encode(" + getJavaType() + " data) throws AmqpEncodingError {");
                writer.newLine();
                writer.write(Utils.tab(2) + "if(data == null) {");
                writer.newLine();
                writer.write(Utils.tab(3) + "return NULL_ENCODED;");
                writer.newLine();
                writer.write(Utils.tab(2) + "}");
                writer.newLine();
                writer.write(Utils.tab(2) + "return new " + getJavaType() + "Encoded(data.getValue());");
                writer.newLine();
                writer.write(Utils.tab(1) + "}");
                writer.newLine();

                writer.newLine();
                writer.write(Utils.tab(1) + "static final Encoded<" + getValueMapping() + "> createEncoded(Buffer source, int offset) throws AmqpEncodingError {");
                writer.newLine();
                writer.write(Utils.tab(2) + "return createEncoded(FormatCategory.createBuffer(source, offset));");
                writer.newLine();
                writer.write(Utils.tab(1) + "}");
                writer.newLine();

                writer.newLine();
                writer.write(Utils.tab(1) + "static final Encoded<" + getValueMapping() + "> createEncoded(DataInput in) throws IOException, AmqpEncodingError {");
                writer.newLine();
                writer.write(Utils.tab(2) + "return createEncoded(FormatCategory.createBuffer(in.readByte(), in));");
                writer.newLine();
                writer.write(Utils.tab(1) + "}");
                writer.newLine();

                writer.newLine();
                writer.write(Utils.tab(1) + "static final Encoded<" + getValueMapping() + "> createEncoded(EncodedBuffer buffer) throws AmqpEncodingError {");
                writer.newLine();
                writer.write(Utils.tab(2) + "if(buffer.getEncodingFormatCode() == AmqpNullMarshaller.FORMAT_CODE) {");
                writer.newLine();
                writer.write(Utils.tab(3) + "return new NullEncoded<" + getValueMapping() + ">();");
                writer.newLine();
                writer.write(Utils.tab(2) + "}");
                writer.newLine();
                writer.write(Utils.tab(2) + "if(buffer.getEncodingFormatCode() != FORMAT_CODE) {");
                writer.newLine();
                writer.write(Utils.tab(3) + "throw new AmqpEncodingError(\"Unexpected format for " + typeMapping.getShortName() + " expected: \" + FORMAT_CODE);");
                writer.newLine();
                writer.write(Utils.tab(2) + "}");
                writer.newLine();
                writer.write(Utils.tab(2) + "return new " + typeMapping.getJavaType() + "Encoded(buffer);");
                writer.newLine();
                writer.write(Utils.tab(1) + "}");
                writer.newLine();

            } else {

                writer.newLine();
                writer.write(Utils.tab(1) + "private static final " + getEncodingName(false) + " chooseEncoding(" + getJavaType() + " val) throws AmqpEncodingError {");
                writer.newLine();
                if (isMutable()) {
                    writer.write(Utils.tab(2) + "return Encoder.choose" + Utils.capFirst(name) + "Encoding(val);");
                } else {
                    writer.write(Utils.tab(2) + "return Encoder.choose" + Utils.capFirst(name) + "Encoding(val.getValue());");
                }
                writer.newLine();
                writer.write(Utils.tab(1) + "}");
                writer.newLine();

                writer.newLine();
                writer.write(Utils.tab(1) + "private static final " + getEncodingName(false) + " chooseEncoding(" + valueMapping + " val) throws AmqpEncodingError {");
                writer.newLine();
                writer.write(Utils.tab(2) + "return Encoder.choose" + Utils.capFirst(name) + "Encoding(val);");
                writer.newLine();
                writer.write(Utils.tab(1) + "}");
                writer.newLine();

                writer.newLine();
                writer.write(Utils.tab(1) + "static final Encoded<" + getValueMapping() + "> encode(" + getJavaType() + " data) throws AmqpEncodingError {");
                writer.newLine();
                writer.write(Utils.tab(2) + "if(data == null) {");
                writer.newLine();
                writer.write(Utils.tab(3) + "return NULL_ENCODED;");
                writer.newLine();
                writer.write(Utils.tab(2) + "}");
                writer.newLine();
                if (isMutable()) {
                    writer.write(Utils.tab(2) + "return " + getEncodingName(false) + ".createEncoded(chooseEncoding(data).FORMAT_CODE, data);");
                } else {
                    writer.write(Utils.tab(2) + "return " + getEncodingName(false) + ".createEncoded(chooseEncoding(data).FORMAT_CODE, data.getValue());");
                }
                writer.newLine();
                writer.write(Utils.tab(1) + "}");
                writer.newLine();

                writer.newLine();
                writer.write(Utils.tab(1) + "static final Encoded<" + getValueMapping() + "> createEncoded(Buffer source, int offset) throws AmqpEncodingError {");
                writer.newLine();
                writer.write(Utils.tab(2) + "return createEncoded(FormatCategory.createBuffer(source, offset));");
                writer.newLine();
                writer.write(Utils.tab(1) + "}");
                writer.newLine();

                writer.newLine();
                writer.write(Utils.tab(1) + "static final Encoded<" + getValueMapping() + "> createEncoded(" + getValueMapping() + " val) throws AmqpEncodingError {");
                writer.newLine();
                writer.write(Utils.tab(2) + "return " + getEncodingName(false) + ".createEncoded(chooseEncoding(val).FORMAT_CODE, val);");
                writer.newLine();
                writer.write(Utils.tab(1) + "}");
                writer.newLine();

                writer.newLine();
                writer.write(Utils.tab(1) + "static final Encoded<" + getValueMapping() + "> createEncoded(DataInput in) throws IOException, AmqpEncodingError {");
                writer.newLine();
                writer.write(Utils.tab(2) + "return createEncoded(FormatCategory.createBuffer(in.readByte(), in));");
                writer.newLine();
                writer.write(Utils.tab(1) + "}");
                writer.newLine();

                writer.newLine();
                writer.write(Utils.tab(1) + "static final Encoded<" + getValueMapping() + "> createEncoded(EncodedBuffer buffer) throws AmqpEncodingError {");
                writer.newLine();
                writer.write(Utils.tab(2) + "if(buffer.getEncodingFormatCode() == AmqpNullMarshaller.FORMAT_CODE) {");
                writer.newLine();
                writer.write(Utils.tab(3) + "return NULL_ENCODED;");
                writer.newLine();
                writer.write(Utils.tab(2) + "}");
                writer.newLine();
                writer.write(Utils.tab(2) + "return " + getEncodingName(false) + ".createEncoded(buffer);");
                writer.newLine();
                writer.write(Utils.tab(1) + "}");
                writer.newLine();

                if (isList()) {
                    writer.newLine();
                    writer.write(Utils.tab(1) + "static final Encoded<" + getValueMapping() + "> createEncoded(DataInput in, ListDecoder decoder) throws IOException, AmqpEncodingError {");
                    writer.newLine();
                    writer.write(Utils.tab(2) + "return createEncoded(FormatCategory.createBuffer(in.readByte(), in), decoder);");
                    writer.newLine();
                    writer.write(Utils.tab(1) + "}");
                    writer.newLine();

                    writer.newLine();
                    writer.write(Utils.tab(1) + "static final Encoded<" + getValueMapping() + "> createEncoded(EncodedBuffer buffer, ListDecoder decoder) throws AmqpEncodingError {");
                    writer.newLine();
                    writer.write(Utils.tab(2) + "if(buffer.getEncodingFormatCode() == AmqpNullMarshaller.FORMAT_CODE) {");
                    writer.newLine();
                    writer.write(Utils.tab(3) + "return NULL_ENCODED;");
                    writer.newLine();
                    writer.write(Utils.tab(2) + "}");
                    writer.newLine();
                    writer.write(Utils.tab(2) + getJavaType() + "Encoded rc = " + getEncodingName(false) + ".createEncoded(buffer);");
                    writer.newLine();
                    writer.write(Utils.tab(2) + "rc.setDecoder(decoder);");
                    writer.newLine();
                    writer.write(Utils.tab(2) + "return rc;");
                    writer.newLine();
                    writer.write(Utils.tab(1) + "}");
                    writer.newLine();
                }

                if (isMap()) {
                    writer.newLine();
                    writer.write(Utils.tab(1) + "static final Encoded<" + getValueMapping() + "> createEncoded(DataInput in, MapDecoder decoder) throws IOException, AmqpEncodingError {");
                    writer.newLine();
                    writer.write(Utils.tab(2) + "return createEncoded(FormatCategory.createBuffer(in.readByte(), in), decoder);");
                    writer.newLine();
                    writer.write(Utils.tab(1) + "}");
                    writer.newLine();

                    writer.newLine();
                    writer.write(Utils.tab(1) + "static final Encoded<" + getValueMapping() + "> createEncoded(EncodedBuffer buffer, MapDecoder decoder) throws AmqpEncodingError {");
                    writer.newLine();
                    writer.write(Utils.tab(2) + "if(buffer.getEncodingFormatCode() == AmqpNullMarshaller.FORMAT_CODE) {");
                    writer.newLine();
                    writer.write(Utils.tab(3) + "return NULL_ENCODED;");
                    writer.newLine();
                    writer.write(Utils.tab(2) + "}");
                    writer.newLine();
                    writer.write(Utils.tab(2) + getJavaType() + "Encoded rc = " + getEncodingName(false) + ".createEncoded(buffer);");
                    writer.newLine();
                    writer.write(Utils.tab(2) + "rc.setDecoder(decoder);");
                    writer.newLine();
                    writer.write(Utils.tab(2) + "return rc;");
                    writer.newLine();
                    writer.write(Utils.tab(1) + "}");
                    writer.newLine();
                }
            }
        }

        writer.write("}");
        writer.newLine();
        writer.flush();
    }

    public String getJavaType() {
        return typeMapping.getJavaType();
    }

    public String getJavaPackage() {
        return typeMapping.getPackageName();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isAny() {
        return name.equals("*");
    }

    public boolean isMutable() throws UnknownTypeException {
        AmqpClass baseType = resolveBaseType();
        if (baseType.isPrimitive() && !(baseType.isList() || baseType.isMap())) {
            return false;
        } else {
            return true;
        }
    }

    public boolean isPrimitive() {
        return primitive;
    }

    public boolean isRestricted() {
        return restricted;
    }

    public boolean isCommand() {
        return isCommand;
    }

    public String getRestrictedType() {
        return restrictedType;
    }

    public boolean isDescribed() {
        return descriptor != null;
    }

    public boolean isEnumType() {
        return isRestricted() && choice != null;
    }

    public String getMarshaller() throws UnknownTypeException {
        if (isAny()) {
            return "AmqpMarshaller.SINGLETON";
        } else if (isRestricted()) {
            return resolveRestrictedType().getTypeMapping() + "Marshaller";
        } else {
            return getTypeMapping() + "Marshaller";
        }
    }

    public AmqpClass getEncodedType() throws UnknownTypeException {
        if (isAny()) {
            return TypeRegistry.any();
        } else if (isRestricted()) {
            return resolveRestrictedType();
        } else {
            return this;
        }
    }

    public AmqpClass getDescribedType() throws UnknownTypeException {
        return descriptor.resolveDescribedType();
    }

    public boolean isMap() {
        return name.equals("map");
    }

    public final String getMapKeyType() {
        return mapKeyType;
    }

    public final String getMapValueType() {
        return mapValueType;
    }

    public boolean isList() {
        return name.equals("list");
    }

    public final String getListElementType() {
        return listElementType;
    }

    public boolean isMarshallable() {
        return isDescribed() || isPrimitive() || (isRestricted() && !isEnumType());
    }

    public boolean needsMarshaller() {
        return isDescribed() || isPrimitive();
    }

    public AmqpClass resolveRestrictedType() throws UnknownTypeException {
        return TypeRegistry.resolveAmqpClass(restrictedType);
    }

    public void setPrimitive(boolean primitive) {
        this.primitive = primitive;
    }

    public boolean hasMultipleEncodings() {
        return encodings != null && encodings.size() > 1;
    }

    public boolean hasNonFixedEncoding() {
        if (encodings == null) {
            return false;
        }

        for (AmqpEncoding encoding : encodings) {
            if (!encoding.isFixed()) {
                return true;
            }
        }

        return false;
    }

    public boolean hasNonZeroEncoding() {
        if (encodings == null) {
            return false;
        }

        for (AmqpEncoding encoding : encodings) {
            if (Integer.parseInt(encoding.getWidth()) > 0) {
                return true;
            }
        }

        return false;
    }

    public boolean hasVariableEncoding() {
        if (encodings == null) {
            return false;
        }

        for (AmqpEncoding encoding : encodings) {
            if (encoding.isVariable()) {
                return true;
            }
        }

        return false;
    }

    public boolean hasCompoundEncoding() {
        if (encodings == null) {
            return false;
        }

        for (AmqpEncoding encoding : encodings) {
            if (encoding.isCompound()) {
                return true;
            }
        }

        return false;
    }

    public boolean hasArrayEncoding() {
        if (encodings == null) {
            return false;
        }

        for (AmqpEncoding encoding : encodings) {
            if (encoding.isArray()) {
                return true;
            }
        }

        return false;
    }

    public String getEncodingName(boolean full) {
        if (full) {
            return getJavaType() + "Marshaller." + getEncodingName(false);
        } else {
            return Utils.toJavaConstant(name) + "_ENCODING";
        }
    }

    public TypeRegistry.JavaTypeMapping getBeanMapping() throws UnknownTypeException {
        if (beanMapping == null) {
            if (isEnumType()) {
                beanMapping = resolveRestrictedType().beanMapping;
            }
        }
        return beanMapping;
    }

    public TypeRegistry.JavaTypeMapping getBufferMapping() throws UnknownTypeException {
        if (bufferMapping == null) {
            if (isEnumType()) {
                bufferMapping = resolveRestrictedType().bufferMapping;
            }
        }
        return bufferMapping;
    }

    /**
     * Resolves the JavaTypeMapping that will be exposed via the class' api.
     *
     * @return
     * @throws UnknownTypeException
     */
    public TypeRegistry.JavaTypeMapping getValueMapping() throws UnknownTypeException {
        if (valueMapping == null) {
            if (isDescribed()) {
                valueMapping = descriptor.resolveDescribedType().getValueMapping();
            }
        }

        return valueMapping;
    }

    /**
     * Resolves the TypeMapping of this class' base class
     *
     * @return
     * @throws UnknownTypeException
     */
    public AmqpClass resolveBaseType() throws UnknownTypeException {
        if (isRestricted()) {
            return TypeRegistry.resolveAmqpClass(restrictedType);
        } else if (isDescribed()) {
            return getDescribedType();
        } else {
            return this;
        }
    }

    public TypeRegistry.JavaTypeMapping getTypeMapping() {
        return typeMapping;
    }

    public String toString() {
        String tableFormat = "|%2s |%25s |%25s |%6s |%6s |%10s |";
        String header = String.format(tableFormat, "#", "Name", "Type", "Req", "Mult", "Default");
        String ret = "\n" + Utils.getTitle(header.length(), getJavaType() + ":" + name);

        try {
            ret = String.format("%s\nBase Type: %s:%s", ret, resolveBaseType().getJavaType(), resolveBaseType().name);
        } catch ( UnknownTypeException ut ) {
            ret = String.format("%s\nBase Type: Unknown", ret);
        }

        ret += appendIfNotNull("\nEncoding(s) : %s", encodings);
        ret += appendIfNotNull("\nDescriptor {\n%s\n}", descriptor);
        ret += appendIfTrue("\nRestricted Type: %s", restrictedType, isRestricted());

        ret += appendIfNotNull("\nType Mapping: %s", typeMapping);
        ret += appendIfNotNull("\nValue Mapping: %s", valueMapping);
        ret += appendIfNotNull("\nBean Mapping: %s", beanMapping);
        ret += appendIfNotNull("\nBuffer Mapping: %s", bufferMapping);

        if ( !fields.isEmpty() ) {
            int num = 0;

            ret += "\n" + Utils.getTitle(header.length(), "Fields");
            ret += "\n" + header + "\n";
            ret += Utils.getBar(header.length() - 1);
            for (AmqpField f : fields.values()) {
                ret += String.format("\n" + tableFormat, ++num, f.getName(), f.getType(), f.isRequired(), f.isMultiple(), f.getDefaultValue());
            }
        }
        ret += "\n" + Utils.getBar(header.length()) + "\n\n";

        return ret;
    }
}
