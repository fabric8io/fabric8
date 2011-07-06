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
import org.fusesource.fabric.apollo.amqp.jaxb.schema.Descriptor;
import org.fusesource.fabric.apollo.amqp.jaxb.schema.Field;
import org.fusesource.fabric.apollo.amqp.jaxb.schema.Type;
import org.fusesource.hawtbuf.AsciiBuffer;
import org.fusesource.hawtbuf.Buffer;

import java.math.BigInteger;
import java.util.ArrayList;

import static com.sun.codemodel.JExpr.*;
import static org.fusesource.fabric.apollo.amqp.generator.Utilities.sanitize;
import static org.fusesource.fabric.apollo.amqp.generator.Utilities.toJavaClassName;

/**
 *
 */
public class DescribedType  extends AmqpDefinedType {



    class Attribute {
        public String type;
        public JFieldVar attribute;
        public JMethod getter;
        public JMethod setter;
    }

    private JFieldVar SYMBOLIC_ID;
    private JFieldVar SYMBOLIC_ID_SIZE;
    private JFieldVar NUMERIC_ID;
    private JFieldVar NUMERIC_ID_SIZE;

    private JMethod write;
    private JMethod read;
    private JMethod encodeTo;
    private JMethod decodeFrom;
    private JMethod count;
    private JMethod size;

    private ArrayList<Attribute> amqpFields = new ArrayList<Attribute>();

    public DescribedType(Generator generator, String className, Type type) throws JClassAlreadyExistsException {
        super(generator, className, type);
    }

    @Override
    protected void createGetArrayConstructor() {
        getArrayConstructor = cls().method(JMod.PUBLIC, cm.ref("Object"), "getArrayConstructor");
        getArrayConstructor.body()._return(_new(cm.ref(generator.getMarshaller() + ".DescribedConstructor")).arg(ref("NUMERIC_ID")));
    }

    protected void createInitialFields() {

    }

    protected void createStaticBlock() {
        for ( Object obj : type.getEncodingOrDescriptorOrFieldOrChoiceOrDoc() ) {
            if ( obj instanceof Descriptor ) {
                Descriptor desc = (Descriptor) obj;
                int mods = JMod.PUBLIC | JMod.STATIC | JMod.FINAL;

                SYMBOLIC_ID = cls().field(mods, Buffer.class, "SYMBOLIC_ID", _new(cm.ref(AsciiBuffer.class)).arg(desc.getName()));
                SYMBOLIC_ID_SIZE = cls().field(mods, Long.class, "SYMBOLIC_ID_SIZE", generator.registry().cls().staticInvoke("instance").invoke("sizer").invoke("sizeOfSymbol").arg(ref("SYMBOLIC_ID")));

                String code = desc.getCode();
                String category = code.split(":")[0];
                String descriptorId = code.split(":")[1];
                category = category.substring(2);
                category = category.substring(4);
                descriptorId = descriptorId.substring(2);
                descriptorId = descriptorId.substring(4);

                //CATEGORY = cls().field(mods, long.class, "CATEGORY", JExpr.lit(Integer.parseInt(category.substring(2), 16)));
                //DESCRIPTOR_ID = cls().field(mods, long.class, "DESCRIPTOR_ID", JExpr.lit(Integer.parseInt(descriptorId.substring(2), 16)));
                //NUMERIC_ID = cls().field(mods, cm.LONG, "NUMERIC_ID", JExpr.direct("CATEGORY << 32 | DESCRIPTOR_ID"));
                NUMERIC_ID = cls().field(mods, BigInteger.class, "NUMERIC_ID", _new(cm.ref("java.math.BigInteger")).arg(lit(category + descriptorId)).arg(lit(16)));
                NUMERIC_ID_SIZE = cls().field(mods, Long.class, "NUMERIC_ID_SIZE", generator.registry().cls().staticInvoke("instance").invoke("sizer").invoke("sizeOfULong").arg(ref("NUMERIC_ID")));
                cls().init().add(
                        generator.registry().cls().staticInvoke("instance")
                                .invoke("getFormatCodeMap")
                                .invoke("put")
                                .arg(ref("NUMERIC_ID"))
                                .arg(cls().dotclass())
                );

                cls().init().add(
                        generator.registry().cls().staticInvoke("instance")
                                .invoke("getSymbolicCodeMap")
                                .invoke("put")
                                .arg(ref("SYMBOLIC_ID"))
                                .arg(cls().dotclass())
                );
            }
        }
    }

    public void generateToString() {
        JMethod toString = cls().method(JMod.PUBLIC, cm.ref("java.lang.String"), "toString");

        toString.body().decl(cm.ref("java.lang.String"), "rc", lit(toJavaClassName(type.getName()) + "{"));

        for (Attribute attr : amqpFields) {
            if (attr.attribute.type().isArray()) {
                toString.body()._if(_this().ref(attr.attribute).ne(_null()))._then().assignPlus(ref("rc"), lit(attr.attribute.name() + "=").plus(cm.ref("java.util.Arrays").staticInvoke("toString").arg(_this().ref(attr.attribute))).plus(lit(" ")));
            } else {
                toString.body()._if(_this().ref(attr.attribute).ne(_null()))._then().assignPlus(ref("rc"), lit(attr.attribute.name() + "=").plus(_this().ref(attr.attribute)).plus(lit(" ")));
            }
        }
        toString.body().assign(ref("rc"), ref("rc").invoke("trim"));
        toString.body().assignPlus(ref("rc"), lit("}"));
        toString.body()._return(ref("rc"));
    }

    public void generateDescribedFields() {
        Log.info("");
        Log.info("Generating %s", cls().binaryName());

        for ( Object obj : type.getEncodingOrDescriptorOrFieldOrChoiceOrDoc() ) {
            if ( obj instanceof Field ) {
                Field field = (Field) obj;
                String fieldType = field.getType();
                String fieldName = sanitize(field.getName());

                Log.info("Field type for field %s : %s", fieldName, fieldType);

                if ( fieldType.equals("*") ) {
                    fieldType = generator.getAmqpBaseType();
                    /*
                    if ( field.getRequires() != null ) {
                        String requiredType = field.getRequires();
                        if (generator.getProvides().contains(requiredType)) {
                            fieldType = generator.getInterfaces() + "." + toJavaClassName(field.getRequires());
                        }
                    }
                    */
                } else if (generator.getDescribed().containsKey(fieldType)) {
                    fieldType = generator.getDescribedJavaClass().get(field.getType());
                } else if (generator.getRestricted().containsKey(fieldType)) {
                    fieldType = generator.getRestrictedMapping().get(field.getType());
                }

                if ( fieldType != null ) {
                    boolean array = false;
                    if ( field.getMultiple() != null && field.getMultiple().equals("true") ) {
                        array = true;
                    }

                    Log.info("Using field type %s", fieldType);

                    Class clazz = generator.getMapping().get(fieldType);
                    JClass c = null;
                    if (fieldType.equals(generator.getAmqpBaseType())) {
                        c = cm.ref(fieldType);
                    } else if ( clazz == null ) {
                        c = cm._getClass(fieldType);
                    } else {
                        if (array) {
                            c = cm.ref(generator.getPrimitiveJavaClass().get(fieldType));
                        } else {
                            c = cm.ref(clazz.getName());
                        }
                    }
                    if ( array ) {
                        c = c.array();
                    }
                    Log.info("%s %s", c.binaryName(), fieldName);
                    Attribute attribute = new Attribute();
                    attribute.attribute = cls().field(JMod.PROTECTED, c, fieldName);

                    attribute.type = fieldType;

                    String doc = field.getName() + ":" + field.getType();

                    if ( field.getLabel() != null ) {
                        doc += " - " + field.getLabel();
                    }
                    attribute.attribute.javadoc().add(doc);

                    attribute.getter = cls().method(JMod.PUBLIC, attribute.attribute.type(), "get" + toJavaClassName(fieldName));
                    attribute.getter.body()._return(_this().ref(attribute.attribute));

                    attribute.setter = cls().method(JMod.PUBLIC, cm.VOID, "set" + toJavaClassName(fieldName));
                    JVar param = attribute.setter.param(attribute.attribute.type(), fieldName);
                    attribute.setter.body().assign(_this().ref(attribute.attribute), param);

                    amqpFields.add(attribute);
                } else {
                    Log.info("Skipping field %s, type not found", field.getName());
                }
            }
        }

        fillInReadMethod();
        fillInWriteMethod();
        fillInSizeMethod();
        generateToString();

        count = cls().method(JMod.PUBLIC, cm.INT, "count");
        count().body()._return(lit(amqpFields.size()));
    }

    private void fillInReadMethod() {
        read().body().decl(cm.LONG, "count", cm.ref(generator.getMarshaller() + ".DescribedTypeSupport").staticInvoke("readListHeader").arg(ref("in")));

        Log.info("Filling in read method for %s", type.getName());

        for (Attribute attribute : amqpFields) {
            Log.info("%s %s", attribute.type, attribute.attribute.name());
            read().body().assign(ref("count"), ref("count").minus(lit(1)));
            JBlock ifBody = read().body()._if(ref("count").gte(lit(0)))._then();
            if (attribute.attribute.type().isArray()) {
                ifBody.assign(attribute.attribute, cast(attribute.attribute.type(), cm.ref("AMQPArray").staticInvoke("read").arg(ref("in"))));
            } else if (generator.getMapping().get(attribute.type) != null) {
                ifBody.assign(attribute.attribute, cm.ref(generator.getPrimitiveJavaClass().get(attribute.type)).staticInvoke("read").arg(ref("in")));
            } else if (generator.getProvides().contains(attribute.type)) {
            } else {
                //ifBody.assign(attribute.attribute, cast(attribute.attribute.type(), cm.ref(generator.getMarshaller() + ".TypeReader").staticInvoke("read").arg(ref("in"))));
                ifBody.assign(attribute.attribute, cast(attribute.attribute.type(), cm.ref(generator.getMarshaller() + ".TypeReader").staticInvoke("read").arg(ref("in"))));
            }
        }
    }

    private void fillInWriteMethod() {
        writeConstructor().body().block().invoke(ref("out"), "writeByte").arg(generator.registry().cls().staticRef("DESCRIBED_FORMAT_CODE"));
        writeConstructor().body().block().staticInvoke(cm.ref(generator.getPrimitiveJavaClass().get("ulong")), "write").arg(ref("NUMERIC_ID")).arg(ref("out"));
        writeConstructor().body()._return(cast(cm.BYTE, lit(0)));

        write().body().invoke("writeConstructor").arg(ref("out"));
        write().body().invoke("writeBody").arg(cast(cm.BYTE, lit((byte) 0))).arg(ref("out"));

        writeBody().body().decl(cm.LONG, "fieldSize", _this().invoke("sizeOfFields"));

        writeBody().body().staticInvoke(cm.ref(generator.getMarshaller() + ".DescribedTypeSupport"), "writeListHeader").arg(ref("fieldSize")).arg(_this().invoke("count")).arg(ref("out"));

        for (Attribute attribute : amqpFields) {
            if (attribute.attribute.type().isArray()) {
                writeBody().body().block().staticInvoke(cm.ref("AMQPArray"), "write").arg(_this().ref(attribute.attribute.name())).arg(ref("out"));
            } else if (generator.getMapping().get(attribute.type) != null) {
                writeBody().body().block().staticInvoke(cm.ref(generator.getPrimitiveJavaClass().get(attribute.type)), "write").arg(_this().ref(attribute.attribute.name())).arg(ref("out"));
            } else {
                JConditional conditional = writeBody.body()
                        ._if(ref(attribute.attribute.name()).ne(_null()));
                conditional._then()
                        .invoke(ref(attribute.attribute.name()), "write").arg(ref("out"));
                conditional._else().invoke(ref("out"), "writeByte").arg(generator.registry().cls().staticRef("NULL_FORMAT_CODE"));
            }
        }
    }

    private void fillInSizeMethod() {
        size().body()._return(invoke("sizeOfConstructor").plus(invoke("sizeOfBody")));
        sizeOfConstructor().body()._return(ref("NUMERIC_ID_SIZE"));
        JMethod sizeOfFields = cls().method(JMod.PRIVATE, cm.LONG, "sizeOfFields");

        sizeOfFields.body().decl(cm.LONG, "fieldSize", lit(0L));

        for (Attribute attribute : amqpFields) {
            if (generator.getMapping().get(attribute.type) != null) {
                if (attribute.attribute.type().isArray()) {
                    sizeOfFields.body().assign(ref("fieldSize"), ref("fieldSize").plus(
                            generator.registry().cls().staticInvoke("instance")
                            .invoke("sizer")
                            .invoke("sizeOfArray")
                                .arg(ref(attribute.attribute.name()))));

                } else {
                    sizeOfFields.body().assign(ref("fieldSize"), ref("fieldSize").plus(
                            generator.registry().cls().staticInvoke("instance")
                                    .invoke("sizer")
                                    .invoke("sizeOf" + toJavaClassName(attribute.type))
                                        .arg(ref(attribute.attribute.name()))));
                }
            } else {
                if (attribute.attribute.type().isArray()) {
                    sizeOfFields.body().assign(ref("fieldSize"), ref("fieldSize").plus(
                            generator.registry().cls().staticInvoke("instance")
                                    .invoke("sizer")
                                    .invoke("sizeOfArray")
                                    .arg(ref(attribute.attribute.name()))));
                } else {

                    JConditional conditional = sizeOfFields.body()
                            ._if(ref(attribute.attribute.name()).ne(_null()));

                    conditional._then()
                            .assign(
                                    ref("fieldSize"), ref("fieldSize").plus(
                                    ref(attribute.attribute.name()).invoke("size")));

                    conditional._else()
                            .assign(ref("fieldSize"), ref("fieldSize").plus(lit(1L)));
                }
            }
        }

        sizeOfFields.body()._return(ref("fieldSize"));
        sizeOfBody().body()._return(cm.ref(generator.getMarshaller()+ ".DescribedTypeSupport").staticInvoke("fullSizeOfList").arg(_this().invoke("sizeOfFields")).arg(_this().invoke("count")));
    }

    public JMethod count() {
        return count;
    }
}
