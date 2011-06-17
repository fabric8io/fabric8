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
    private JMethod equalsTypeSpecific;
    private JMethod equalsGeneric;
    private JMethod toString;
    private JMethod hashCode;

    private JMethod staticRead;
    private JMethod staticWrite;
    private JMethod sizer;

    public PrimitiveType(Generator generator, String className, Type type) throws JClassAlreadyExistsException {
        super(generator, className, type);
    }

    protected void createInitialFields() {
        value = cls().field(JMod.PRIVATE, getJavaType(), "value");

        generateConstructors();

        getValue = cls().method(JMod.PUBLIC, getJavaType(), "getValue");
        getValue.body()._return(JExpr._this().ref("value"));
        setValue = cls().method(JMod.PUBLIC, cm.VOID, "setValue");
        setValue.param(getJavaType(), "value");
        setValue.body().block().assign(JExpr._this().ref("value"), JExpr.ref("value"));

        encodingPicker = generator.picker().cls().method(JMod.PUBLIC, cm.BYTE, "choose" + toJavaClassName(type.getName() + "Encoding"));
        encodingPicker.param(getJavaType(), "value");

        generateSize();
        generateEquals();
        generateHashCode();
        generateToString();
    }

    private void generateHashCode() {
        hashCode = cls().method(JMod.PUBLIC, cm.INT, "hashCode");
        hashCode.body().block()._if(JExpr._this().ref("value").eq(JExpr._null()))
                ._then()
                ._return(JExpr.dotclass(cls()).invoke("hashCode"));
        hashCode.body().block()._return(JExpr._this().ref("value").invoke("hashCode"));
    }

    private void generateSize() {
        sizer = generator.sizer().cls().method(JMod.PUBLIC, cm.LONG, "sizeOf" + toJavaClassName(type.getName()));
        sizer.param(getJavaType(), "value");

        size().body()._return(generator.registry().cls().staticInvoke("instance")
        .invoke("sizer").invoke("sizeOf" + toJavaClassName(type.getName())).arg(JExpr.ref("value")));
    }

    private void generateConstructors() {
        noArgConstructor = cls().constructor(JMod.PUBLIC);
        noArgConstructor.body().block().assign(JExpr._this().ref("value"), JExpr._null());

        valueConstructor = cls().constructor(JMod.PUBLIC);
        valueConstructor.param(getJavaType(), "value");
        valueConstructor.body().block().assign(JExpr._this().ref("value"), JExpr.ref("value"));
    }

    private void generateToString() {
        toString = cls().method(JMod.PUBLIC, cm.ref("java.lang.String"), "toString");
        toString.body().block()._if(JExpr._this().ref("value").eq(JExpr._null()))._then().block()._return(JExpr.lit("null"));
        toString.body().block()._return(JExpr._this().ref("value").invoke("toString"));
    }

    private void generateEquals() {
        equalsGeneric = cls().method(JMod.PUBLIC, cm.BOOLEAN, "equals");
        equalsGeneric.param(cm.ref("java.lang.Object"), "other");

        equalsGeneric.body().block()._if(JExpr._this().eq(JExpr.ref("other")))
                ._then()
                ._return(JExpr.TRUE);

        equalsGeneric.body().block()._if(
                JExpr.ref("other").eq(JExpr._null())
                        .cor( (JExpr.ref("other")._instanceof(cls()).not() )))
                ._then()
                ._return(JExpr.FALSE);

        equalsGeneric.body().block()._return(JExpr._this().invoke("equals").arg(JExpr.cast(cls(), JExpr.ref("other"))));

        equalsTypeSpecific = cls().method(JMod.PUBLIC, cm.BOOLEAN, "equals");
        equalsTypeSpecific.param(cls(), "other");
        equalsTypeSpecific.body().block()._if(JExpr.ref("other").eq(JExpr._null()))._then()._return(JExpr.FALSE);
        JConditional test = equalsTypeSpecific.body().block()._if(JExpr._this().ref("value").eq(JExpr._null()).cand(JExpr.ref("other").invoke("getValue").ne(JExpr._null())));
        test._then().block()._return(JExpr.FALSE);
        JConditional test2 = test._elseif(JExpr._this().ref("value").ne(JExpr._null()).cand(JExpr.ref("other").invoke("getValue").eq(JExpr._null())));
        test2._then().block()._return(JExpr.FALSE);
        JConditional test3 = test2._elseif(JExpr._this().ref("value").eq(JExpr._null()).cand(JExpr.ref("other").invoke("getValue").eq(JExpr._null())));
        test3._then().block()._return(JExpr.TRUE);
        test3._else().block()._return(JExpr._this().ref("value").invoke("equals").arg(JExpr.ref("other").invoke("getValue")));
    }

    protected void createStaticBlock() {

        staticRead().body().decl(cm.BYTE, "formatCode", JExpr.ref("in").invoke("readByte"));

        JSwitch staticReadSwitchBlock = staticRead().body().block()._switch(JExpr.ref("formatCode"));

        staticReadSwitchBlock._case(generator.registry().cls().staticRef("NULL_FORMAT_CODE")).body()._return(JExpr.cast(cm._ref(getJavaType()), generator.registry().cls()
                .staticInvoke("instance")
                .invoke("encoder")
                .invoke("readNull")
                .arg(JExpr.ref("in"))));

        JSwitch readSwitchBlock = read().body()._switch(JExpr.ref("formatCode"));

        staticWrite().body().decl(cm.BYTE, "formatCode", generator.registry().cls()
                .staticInvoke("instance")
                .invoke("picker")
                .invoke("choose" + toJavaClassName(type.getName() + "Encoding"))
                .arg(JExpr.ref("value")));

        write().body().decl(cm.BYTE, "formatCode", generator.registry().cls()
                .staticInvoke("instance")
                .invoke("picker")
                .invoke("choose" + toJavaClassName(type.getName() + "Encoding"))
                .arg(JExpr.ref("value")));

        JSwitch staticWriteSwitchBlock = staticWrite().body()._switch(JExpr.ref("formatCode"));
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

    private JMethod staticWrite() {
        if (staticWrite == null) {
            staticWrite = cls().method(JMod.PUBLIC | JMod.STATIC, cm.VOID, "write");
            staticWrite._throws(Exception.class);
            staticWrite.param(getJavaType(), "value");
            staticWrite.param(DataOutput.class, "out");
        }
        return staticWrite;
    }

    private JMethod staticRead() {
        if (staticRead == null) {
            staticRead = cls().method(JMod.PUBLIC | JMod.STATIC, getJavaType(), "read");
            staticRead._throws(Exception.class);
            staticRead.param(DataInput.class, "in");
        }
        return staticRead;
    }

    public Class getJavaType() {
        return generator.getMapping().get(type.getName());
    }
}
