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


import java.util.ArrayList;

import static org.fusesource.fabric.apollo.amqp.generator.Utilities.sanitize;
import static org.fusesource.fabric.apollo.amqp.generator.Utilities.toJavaClassName;

/**
 *
 */
public class DescribedType  extends AmqpDefinedType {

    class Attribute {
        public JFieldVar attribute;
        public JMethod getter;
        public JMethod setter;
    }

    private JFieldVar SYMBOLIC_ID;
    private JFieldVar CATEGORY;
    private JFieldVar DESCRIPTOR_ID;
    private JFieldVar NUMERIC_ID;

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

    protected void createInitialFields() {

    }

    protected void createStaticBlock() {
        for ( Object obj : type.getEncodingOrDescriptorOrFieldOrChoiceOrDoc() ) {
            if ( obj instanceof Descriptor ) {
                Descriptor desc = (Descriptor) obj;
                int mods = JMod.PUBLIC | JMod.STATIC | JMod.FINAL;

                SYMBOLIC_ID = cls().field(mods, Buffer.class, "SYMBOLIC_ID", JExpr._new(cm.ref(AsciiBuffer.class)).arg(desc.getName()));

                String code = desc.getCode();
                String category = code.split(":")[0];
                String descriptorId = code.split(":")[1];

                CATEGORY = cls().field(mods, long.class, "CATEGORY", JExpr.lit(Integer.parseInt(category.substring(2), 16)));
                DESCRIPTOR_ID = cls().field(mods, long.class, "DESCRIPTOR_ID", JExpr.lit(Integer.parseInt(descriptorId.substring(2), 16)));
                NUMERIC_ID = cls().field(mods, long.class, "NUMERIC_ID", JExpr.direct("CATEGORY << 32 | DESCRIPTOR_ID"));
            }
        }
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
                    if ( field.getRequires() != null ) {
                        String requiredType = field.getRequires();
                        if (generator.getProvides().contains(requiredType)) {
                            fieldType = generator.getPackagePrefix() + "." + generator.getInterfaces() + "." + toJavaClassName(field.getRequires());
                        }
                    }
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
                        c = cm.ref(clazz.getName());
                    }
                    if ( array ) {
                        c = c.array();
                    }
                    Log.info("%s %s", c.binaryName(), fieldName);
                    Attribute attribute = new Attribute();
                    attribute.attribute = cls().field(JMod.PROTECTED, c, fieldName);

                    String doc = field.getName() + ":" + field.getType();

                    if ( field.getLabel() != null ) {
                        doc += " - " + field.getLabel();
                    }
                    attribute.attribute.javadoc().add(doc);

                    attribute.getter = cls().method(JMod.PUBLIC, attribute.attribute.type(), "get" + toJavaClassName(fieldName));
                    attribute.getter.body()._return(JExpr._this().ref(attribute.attribute));

                    attribute.setter = cls().method(JMod.PUBLIC, cm.VOID, "set" + toJavaClassName(fieldName));
                    JVar param = attribute.setter.param(attribute.attribute.type(), fieldName);
                    attribute.setter.body().assign(JExpr._this().ref(attribute.attribute), param);

                    amqpFields.add(attribute);
                } else {
                    Log.info("Skipping field %s, type not found", field.getName());
                }
            }
        }
        count = cls().method(JMod.PUBLIC, cm.INT, "count");
        count().body()._return(JExpr.lit(amqpFields.size()));
    }

    public void generateSizeMethod() {

    }

    public JMethod size() {
        if (size == null) {
            size = cls().method(JMod.PUBLIC, cm.INT, "size");
        }
        return size();
    }

    public JMethod count() {
        return count;
    }

    public JFieldVar SYMBOLIC_ID() {
        return SYMBOLIC_ID;
    }

    public JFieldVar CATEGORY() {
        return CATEGORY;
    }

    public JFieldVar DESCRIPTOR_ID() {
        return DESCRIPTOR_ID;
    }

    public JFieldVar NUMERIC_ID() {
        return NUMERIC_ID;
    }
}
