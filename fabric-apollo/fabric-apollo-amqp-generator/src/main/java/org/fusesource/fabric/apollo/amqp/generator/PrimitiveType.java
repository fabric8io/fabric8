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
import org.fusesource.fabric.apollo.amqp.jaxb.schema.Encoding;
import org.fusesource.fabric.apollo.amqp.jaxb.schema.Type;

import java.io.DataInput;
import java.io.DataOutput;
import java.util.ArrayList;
import java.util.List;

import static org.fusesource.fabric.apollo.amqp.generator.Utilities.toJavaClassName;
import static org.fusesource.fabric.apollo.amqp.generator.Utilities.toStaticName;

/**
 *
 */
public class PrimitiveType extends AmqpDefinedType {

    private JFieldVar value;
    private List<JFieldVar> formatCodes = new ArrayList<JFieldVar>();

    private JMethod getValue;
    private JMethod setValue;
    private JMethod noArgConstructor;
    private JMethod valueConstructor;
    private JMethod encodingPicker;

    private JMethod staticRead;
    private JMethod staticWrite;

    public PrimitiveType(Generator generator, String className, Type type) throws JClassAlreadyExistsException {
        super(generator, className, type);
    }

    protected void createInitialFields() {

        value = cls().field(JMod.PRIVATE, getJavaType(), "value");

        noArgConstructor = cls().constructor(JMod.PUBLIC);
        noArgConstructor.body().block().assign(JExpr._this().ref("value"), JExpr._null());

        valueConstructor = cls().constructor(JMod.PUBLIC);
        valueConstructor.param(getJavaType(), "value");
        valueConstructor.body().block().assign(JExpr._this().ref("value"), JExpr.ref("value"));

        getValue = cls().method(JMod.PUBLIC, getJavaType(), "getValue");
        getValue.body()._return(JExpr._this().ref("value"));
        setValue = cls().method(JMod.PUBLIC, cm.VOID, "setValue");
        setValue.param(getJavaType(), "value");
        setValue.body().block().assign(JExpr._this().ref("value"), JExpr.ref("value"));

        encodingPicker = generator.picker().cls().method(JMod.PUBLIC, cm.BYTE, "choose" + toJavaClassName(type.getName() + "Encoding"));
        encodingPicker.param(getJavaType(), "value");

    }

    protected void createStaticBlock() {

        staticRead = cls().method(JMod.PUBLIC | JMod.STATIC, getJavaType(), "read");
        staticRead._throws(Exception.class);
        staticRead.param(DataInput.class, "in");

        JVar fmc = staticRead.body().decl(cm.BYTE, "formatCode", JExpr.ref("in").invoke("readByte"));

        JSwitch staticReadSwitchBlock = staticRead.body().block()._switch(JExpr.ref("formatCode"));

        staticReadSwitchBlock._case(generator.registry().cls().staticRef("NULL_FORMAT_CODE")).body()._return(JExpr.cast(cm._ref(getJavaType()), generator.registry().cls()
                .staticInvoke("instance")
                .invoke("encoder")
                .invoke("readNull")
                .arg(JExpr.ref("in"))));

        JSwitch readSwitchBlock = read().body()._switch(JExpr.ref("formatCode"));

        staticWrite = cls().method(JMod.PUBLIC | JMod.STATIC, cm.VOID, "write");
        staticWrite._throws(Exception.class);
        staticWrite.param(getJavaType(), "value");
        staticWrite.param(DataOutput.class, "out");

        staticWrite.body().decl(cm.BYTE, "formatCode", generator.registry().cls()
                .staticInvoke("instance")
                .invoke("picker")
                .invoke("choose" + toJavaClassName(type.getName() + "Encoding"))
                .arg(JExpr.ref("value")));

        write().body().decl(cm.BYTE, "formatCode", generator.registry().cls()
                .staticInvoke("instance")
                .invoke("picker")
                .invoke("choose" + toJavaClassName(type.getName() + "Encoding"))
                .arg(JExpr.ref("value")));

        JSwitch staticWriteSwitchBlock = staticWrite.body()._switch(JExpr.ref("formatCode"));
        JSwitch writeSwitchBlock = write().body()._switch(JExpr.ref("formatCode"));

        writeSwitchBlock._case(generator.registry().cls().staticRef("NULL_FORMAT_CODE")).body().add(generator.registry().cls()
                .staticInvoke("instance")
                .invoke("encoder")
                .invoke("writeNull")
                .arg(JExpr.ref("out")))
                    ._break();

        staticWriteSwitchBlock._case(generator.registry().cls().staticRef("NULL_FORMAT_CODE")).body().add(generator.registry().cls()
                .staticInvoke("instance")
                .invoke("encoder")
                .invoke("writeNull")
                .arg(JExpr.ref("out")))
                ._break();

        for (Object obj : type.getEncodingOrDescriptorOrFieldOrChoiceOrDoc()) {
            if (obj instanceof Encoding ) {
                Encoding encoding = (Encoding)obj;

                String fieldName = type.getName();

                if (encoding.getName() != null) {
                    fieldName += "_" + encoding.getName();
                }

                Log.info("encoding name %s", fieldName);

                String staticCodeFieldName = toStaticName(fieldName + "_CODE");

                cls().field(JMod.PUBLIC | JMod.STATIC | JMod.FINAL, cm.BYTE, staticCodeFieldName, JExpr.direct("(byte)" + encoding.getCode()));

                cls().init().add(
                        generator.registry().cls().staticInvoke("instance")
                                .invoke("getPrimitiveFormatCodeMap")
                                .invoke("put")
                                    .arg(JExpr.ref(staticCodeFieldName))
                                    .arg(cls().dotclass())
                );

                cls().field(JMod.PUBLIC | JMod.STATIC | JMod.FINAL, cm.INT, toStaticName(fieldName + "_WIDTH"), JExpr.lit(Integer.parseInt(encoding.getWidth())));

                readSwitchBlock._case(JExpr.ref(staticCodeFieldName)).body()
                        .assign(JExpr._this().ref("value"),
                                generator.registry().cls()
                                .staticInvoke("instance")
                                .invoke("encoder")
                                .invoke("read" + toJavaClassName(fieldName))
                                    .arg(JExpr.ref("in")))
                            ._break();

                staticReadSwitchBlock._case(JExpr.ref(staticCodeFieldName)).body()
                        ._return(generator.registry().cls()
                                .staticInvoke("instance")
                                .invoke("encoder")
                                .invoke("read" + toJavaClassName(fieldName))
                                .arg(JExpr.ref("in")));

                writeSwitchBlock._case(JExpr.ref(staticCodeFieldName)).body()
                        .add(generator.registry().cls().staticInvoke("instance")
                        .invoke("encoder")
                        .invoke("write" + toJavaClassName(fieldName))
                            .arg(JExpr._this().ref("value"))
                            .arg(JExpr.ref("out")))
                        ._break();

                staticWriteSwitchBlock._case(JExpr.ref(staticCodeFieldName)).body()
                        .add(generator.registry().cls().staticInvoke("instance")
                                .invoke("encoder")
                                .invoke("write" + toJavaClassName(fieldName))
                                .arg(JExpr.ref("value"))
                                .arg(JExpr.ref("out")))
                        ._break();
            }
        }

        // TODO - create proper exception type to be thrown here
        readSwitchBlock._default().body()._throw(JExpr._new(cm.ref(Exception.class)).arg(JExpr.lit("Unknown format code for " + type.getName() + " : 0x").plus(cm.ref("java.lang.String").staticInvoke("format").arg("%x").arg(JExpr.ref("formatCode")))));
        staticReadSwitchBlock._default().body()._throw(JExpr._new(cm.ref(Exception.class)).arg(JExpr.lit("Unknown format code for " + type.getName() + " : 0x").plus(cm.ref("java.lang.String").staticInvoke("format").arg("%x").arg(JExpr.ref("formatCode")))));
        writeSwitchBlock._default().body()._throw(JExpr._new(cm.ref(Exception.class)).arg(JExpr.lit("Unknown format code for " + type.getName() + " : 0x").plus(cm.ref("java.lang.String").staticInvoke("format").arg("%x").arg(JExpr.ref("formatCode")))));
        staticWriteSwitchBlock._default().body()._throw(JExpr._new(cm.ref(Exception.class)).arg(JExpr.lit("Unknown format code for " + type.getName() + " : 0x").plus(cm.ref("java.lang.String").staticInvoke("format").arg("%x").arg(JExpr.ref("formatCode")))));
    }

    public Class getJavaType() {
        return generator.getMapping().get(type.getName());
    }
}
