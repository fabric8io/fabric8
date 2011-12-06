/*
 * Copyright (C) 2010-2011, FuseSource Corp.  All rights reserved.
 *
 * 	http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license, a copy of which has been included with this distribution
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
import static org.fusesource.fabric.apollo.amqp.generator.Utilities.toStaticName;

/**
 *
 */
public class DescribedType extends AmqpDefinedType {

    class Attribute {
        public String type;
        public Boolean required;
        public String defaultValue;
        public JFieldVar attribute;
        public JMethod getter;
        public JMethod setter;
    }

    private JFieldVar SYMBOLIC_ID;
    private JFieldVar SYMBOLIC_ID_SIZE;
    private JFieldVar NUMERIC_ID;
    private JFieldVar NUMERIC_ID_SIZE;
    private JFieldVar SYMBOLIC_CONSTRUCTOR;
    private JFieldVar NUMERIC_CONSTRUCTOR;
    private JFieldVar CONSTRUCTOR;

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

                SYMBOLIC_CONSTRUCTOR = cls().field(mods, cm.ref(generator.getMarshaller() + ".DescribedConstructor"), "SYMBOLIC_CONSTRUCTOR", _new(cm.ref(generator.getMarshaller() + ".DescribedConstructor")).arg(ref("SYMBOLIC_ID")));

                NUMERIC_CONSTRUCTOR = cls().field(mods, cm.ref(generator.getMarshaller() + ".DescribedConstructor"), "NUMERIC_CONSTRUCTOR", _new(cm.ref(generator.getMarshaller() + ".DescribedConstructor")).arg(ref("NUMERIC_ID")));

                CONSTRUCTOR = cls().field(mods, cm.ref(generator.getMarshaller() + ".DescribedConstructor"), "CONSTRUCTOR");

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

                JConditional block = cls().init()._if(cm.ref("java.lang.Boolean").staticInvoke("parseBoolean").arg(cm.ref("java.lang.System").staticInvoke("getProperty").arg(lit(generator.getPackagePrefix() + ".UseSymbolicID"))));
                block._then().assign(ref("CONSTRUCTOR"), ref("SYMBOLIC_CONSTRUCTOR"));
                block._else().assign(ref("CONSTRUCTOR"), ref("NUMERIC_CONSTRUCTOR"));
            }
        }
    }

    public void generateToString() {
        JMethod toString = cls().method(JMod.PUBLIC, cm.ref("java.lang.String"), "toString");

        toString.body().decl(cm.ref("java.lang.String"), "rc", lit(toJavaClassName(type.getName()) + "{"));

        for ( Attribute attr : amqpFields ) {
            if ( attr.attribute.type().isArray() ) {
                toString.body()._if(_this().ref(attr.attribute).ne(_null()))._then().assignPlus(ref("rc"), lit(attr.attribute.name() + "=").plus(cm.ref("java.util.Arrays").staticInvoke("toString").arg(_this().ref(attr.attribute))).plus(lit(" ")));
            } else {
                toString.body()._if(_this().ref(attr.attribute).ne(_null()))._then().assignPlus(ref("rc"), lit(attr.attribute.name() + "=").plus(_this().ref(attr.attribute)).plus(lit(" ")));
            }
        }
        toString.body().assign(ref("rc"), ref("rc").invoke("trim"));
        toString.body().assignPlus(ref("rc"), lit("}"));
        toString.body()._return(ref("rc"));
    }

    public boolean isComposite() {
        return type.getClazz().equals("composite");
    }

    public boolean isRestricted() {
        return type.getClazz().equals("restricted");
    }

    public void generateDescribedFields() {
        Log.info("");
        Log.info("Generating %s", cls().binaryName());

        if ( isComposite() ) {
            for ( Object obj : type.getEncodingOrDescriptorOrFieldOrChoiceOrDoc() ) {
                if ( obj instanceof Field ) {
                    Field field = (Field) obj;
                    processField(field);
                }
            }
        } else if ( isRestricted() ) {
            Field field = new Field();
            field.setName("value");
            field.setType(type.getSource());
            processField(field);
        }

        generateConstructors();


        if ( isComposite() ) {
            addDefaults();
            generateCount();
        }
        fillInReadMethod();
        fillInWriteMethod();
        fillInSizeMethod();
        generateToString();
    }

    private void addDefaults() {
        int mods = JMod.PUBLIC | JMod.STATIC | JMod.FINAL;
        for (Attribute attr : amqpFields) {
            if (attr.defaultValue != null && !attr.defaultValue.equals("none")) {
                JExpression init = null;


                String d = attr.defaultValue;
                String t = attr.type;

                try {
                    if (t.equals("boolean")) {
                        init = lit(Boolean.parseBoolean(d));
                    } else if (t.equals("long") || t.equals("uint")) {
                        init = lit(Long.parseLong(d));
                    } else if (t.equals("symbol")) {
                        init = cm.ref(Buffer.class).staticInvoke("ascii").arg(lit(d));
                    } else if (t.equals("int") || t.equals("ushort")) {
                        init = lit(Integer.parseInt(d));
                    } else {
                        Log.warn("\n\nDefault value type for %s : %s, Java type : %s not set, value is %s\n\n\n", attr.attribute.name(), attr.type, attr.attribute.type().name(), attr.defaultValue);

                    }
                } catch (Exception e) {
                    Log.warn("\n\nDefault value type for %s : %s, Java type : %s not set, value is %s\n\n\n", attr.attribute.name(), attr.type, attr.attribute.type().name(), attr.defaultValue);
                    init = null;
                }

                if (init != null) {
                    cls().field(mods, attr.attribute.type(), toStaticName(attr.attribute.name() + "_DEFAULT"), init);
                }
            }
        }
    }

    private void generateConstructors() {
        int numFields = amqpFields.size();

        for ( int i = 0; i <= numFields; i++ ) {
            JMethod constructor = cls().constructor(JMod.PUBLIC);
            String log_message = "Adding constructor for : ";
            for ( int j = 0; j < i; j++ ) {
                log_message += amqpFields.get(j).attribute.name() + " ";
                constructor.param(amqpFields.get(j).attribute.type(), amqpFields.get(j).attribute.name());
                constructor.body().assign(_this().ref(amqpFields.get(j).attribute.name()), ref(amqpFields.get(j).attribute.name()));
            }
            Log.info(log_message.trim());
        }
    }

    private void processField(Field field) {
        String fieldType = field.getType();
        String fieldName = sanitize(field.getName());

        Log.info("Field type for field %s : %s", fieldName, fieldType);

        if ( fieldType.equals("*") ) {
            fieldType = generator.getAmqpBaseType();
            /*
            if ( field.getRequires() != null ) {
                String requiredType = field.getRequires();
                if (generator.getProvides().contains(requiredType)) {
                    fieldType = generator.getInterfaces() + "." + toJavaClassName(requiredType);
                }
            }
            */
        } else if ( generator.getDescribed().containsKey(fieldType) ) {
            fieldType = generator.getDescribedJavaClass().get(field.getType());
        } else if ( generator.getRestricted().containsKey(fieldType) ) {
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
            if ( fieldType.equals(generator.getAmqpBaseType()) ) {
                c = cm.ref(fieldType);
            } else if ( clazz == null ) {
                c = cm._getClass(fieldType);
            } else {
                if ( array ) {
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
            attribute.defaultValue = field.getDefault();
            if ( field.getMandatory() != null ) {
                attribute.required = Boolean.parseBoolean(field.getMandatory());
            } else {
                attribute.required = Boolean.FALSE;
            }

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

    private void generateCount() {
        count = cls().method(JMod.PUBLIC, cm.INT, "count");
        count().body().decl(cm.INT, "rc", lit(amqpFields.size()));
        for ( int i = amqpFields.size(); i > 0; i-- ) {
            JConditional _if = count().body()._if(amqpFields.get(i - 1).attribute.eq(_null()));
            _if._then().assign(ref("rc"), ref("rc").minus(lit(1)));
            _if._else()._return(ref("rc"));
        }
        count().body()._return(ref("rc"));
    }

    private void fillInReadMethod() {
        if ( isComposite() ) {
            read().body().decl(cm.LONG, "count", cm.ref(generator.getMarshaller() + ".DescribedTypeSupport").staticInvoke("readListHeader").arg(ref("in")));
        }

        Log.info("Filling in read method for %s", type.getName());

        for ( Attribute attribute : amqpFields ) {
            Log.info("%s %s", attribute.type, attribute.attribute.name());
            if ( isComposite() ) {
                read().body().assign(ref("count"), ref("count").minus(lit(1)));
                JBlock ifBody = read().body()._if(ref("count").gte(lit(0)))._then();
                addFieldRead(attribute, ifBody);
            } else {
                addFieldRead(attribute, read().body());
            }

        }

        if ( isComposite() ) {
            for ( Attribute attribute : amqpFields ) {
                if ( attribute.required ) {
                    read().body()._if(attribute.attribute.eq(_null()))._then()._throw(_new(cm.ref(RuntimeException.class)).arg("No value specified for mandatory attribute " + attribute.attribute.name()));
                }
            }
        }
    }

    private void addFieldRead(Attribute attribute, JBlock body) {
        if ( attribute.attribute.type().isArray() ) {
            body.assign(attribute.attribute, cast(attribute.attribute.type(), cm.ref("AMQPArray").staticInvoke("read").arg(ref("in"))));
        } else if ( generator.getMapping().get(attribute.type) != null ) {
            body.assign(attribute.attribute, cm.ref(generator.getPrimitiveJavaClass().get(attribute.type)).staticInvoke("read").arg(ref("in")));
        } else if ( generator.getProvides().contains(attribute.type) ) {
        } else {
            //body.assign(attribute.attribute, cast(attribute.attribute.type(), cm.ref(generator.getMarshaller() + ".TypeReader").staticInvoke("read").arg(ref("in"))));
            body.assign(attribute.attribute, cast(attribute.attribute.type(), cm.ref(generator.getMarshaller() + ".TypeReader").staticInvoke("read").arg(ref("in"))));
        }
    }

    private void fillInWriteMethod() {
        writeConstructor().body().block().invoke(ref("CONSTRUCTOR"), "write").arg(ref("out"));
        writeConstructor().body()._return(cast(cm.BYTE, lit(0)));

        write().body().invoke("writeConstructor").arg(ref("out"));
        write().body().invoke("writeBody").arg(cast(cm.BYTE, lit((byte) 0))).arg(ref("out"));

        if ( isComposite() ) {
            writeBody().body().decl(cm.LONG, "fieldSize", _this().invoke("sizeOfFields"));
            writeBody().body().decl(cm.INT, "count", _this().invoke("count"));

            writeBody().body().staticInvoke(cm.ref(generator.getMarshaller() + ".DescribedTypeSupport"), "writeListHeader").arg(ref("fieldSize")).arg(ref("count")).arg(ref("out"));
        }

        for ( Attribute attribute : amqpFields ) {
            if ( isComposite() ) {
                writeBody().body().assign(ref("count"), ref("count").minus(lit(1)));
                JBlock ifBody = writeBody().body()._if(ref("count").gte(lit(0)))._then();
                addFieldWrite(attribute, ifBody);
            } else {
                addFieldWrite(attribute, writeBody().body());
            }
        }
    }

    private void addFieldWrite(Attribute attribute, JBlock body) {
        if ( attribute.attribute.type().isArray() ) {
            body.staticInvoke(cm.ref("AMQPArray"), "write").arg(_this().ref(attribute.attribute.name())).arg(ref("out"));
        } else if ( generator.getMapping().get(attribute.type) != null ) {
            body.staticInvoke(cm.ref(generator.getPrimitiveJavaClass().get(attribute.type)), "write").arg(_this().ref(attribute.attribute.name())).arg(ref("out"));
        } else {
            JConditional conditional = body
                    ._if(ref(attribute.attribute.name()).ne(_null()));
            conditional._then()
                    .invoke(ref(attribute.attribute.name()), "write").arg(ref("out"));
            conditional._else().invoke(ref("out"), "writeByte").arg(generator.registry().cls().staticRef("NULL_FORMAT_CODE"));
        }
    }

    private void fillInSizeMethod() {
        size().body()._return(invoke("sizeOfConstructor").plus(invoke("sizeOfBody")));
        sizeOfConstructor().body()._return(ref("CONSTRUCTOR").invoke("size"));
        JMethod sizeOfFields = cls().method(JMod.PRIVATE, cm.LONG, "sizeOfFields");

        sizeOfFields.body().decl(cm.LONG, "fieldSize", lit(0L));

        if ( isComposite() ) {
            sizeOfFields.body().decl(cm.INT, "count", _this().invoke(count));
        }

        for ( Attribute attribute : amqpFields ) {
            if ( isComposite() ) {
                sizeOfFields.body().assign(ref("count"), ref("count").minus(lit(1)));
                JBlock ifBody = sizeOfFields.body()._if(ref("count").gte(lit(0)))._then();
                addFieldSize(attribute, ifBody);
            } else {
                addFieldSize(attribute, sizeOfFields.body());
            }
        }

        sizeOfFields.body()._return(ref("fieldSize"));
        if ( isComposite() ) {
            sizeOfBody().body()._return(cm.ref(generator.getMarshaller() + ".DescribedTypeSupport").staticInvoke("fullSizeOfList").arg(_this().invoke("sizeOfFields")).arg(_this().invoke("count")));
        } else {
            sizeOfBody().body()._return(_this().invoke("sizeOfFields"));
        }
    }

    private void addFieldSize(Attribute attribute, JBlock body) {
        if ( generator.getMapping().get(attribute.type) != null ) {
            if ( attribute.attribute.type().isArray() ) {
                body.assign(ref("fieldSize"), ref("fieldSize").plus(
                        generator.registry().cls().staticInvoke("instance")
                                .invoke("sizer")
                                .invoke("sizeOfArray")
                                .arg(ref(attribute.attribute.name()))));

            } else {
                body.assign(ref("fieldSize"), ref("fieldSize").plus(
                        generator.registry().cls().staticInvoke("instance")
                                .invoke("sizer")
                                .invoke("sizeOf" + toJavaClassName(attribute.type))
                                .arg(ref(attribute.attribute.name()))));
            }
        } else {
            if ( attribute.attribute.type().isArray() ) {
                body.assign(ref("fieldSize"), ref("fieldSize").plus(
                        generator.registry().cls().staticInvoke("instance")
                                .invoke("sizer")
                                .invoke("sizeOfArray")
                                .arg(ref(attribute.attribute.name()))));
            } else {

                JConditional conditional = body
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

    public JMethod count() {
        return count;
    }
}
