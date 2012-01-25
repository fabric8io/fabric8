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

import com.sun.codemodel.*;
import org.fusesource.fabric.apollo.amqp.jaxb.schema.Encoding;
import org.fusesource.fabric.apollo.amqp.jaxb.schema.Type;

import java.io.DataInput;
import java.io.DataOutput;
import java.util.ArrayList;
import java.util.List;

import static com.sun.codemodel.JExpr.*;
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
    private JMethod staticReadConstructor;
    private JMethod staticReadBody;
    private JMethod staticWrite;
    private JMethod staticWriteBody;
    private JMethod staticWriteConstructor;
    private JMethod sizer;
    private String highestWidthField;

    public PrimitiveType(Generator generator, String className, Type type) throws JClassAlreadyExistsException {
        super(generator, className, type);
    }

    @Override
    protected void createGetArrayConstructor() {
        getArrayConstructor = cls().method(JMod.PUBLIC, cm.ref("Object"), "getArrayConstructor");
        getArrayConstructor.body()._return(ref(highestWidthField));
    }

    protected void createInitialFields() {
        value = cls().field(JMod.PROTECTED, getJavaType(), "value");

        generateConstructors();

        getValue = cls().method(JMod.PUBLIC, getJavaType(), "getValue");
        getValue.body()._return(_this().ref("value"));
        setValue = cls().method(JMod.PUBLIC, cm.VOID, "setValue");
        setValue.param(getJavaType(), "value");
        setValue.body().block().assign(_this().ref("value"), ref("value"));

        encodingPicker = generator.picker().cls().method(JMod.PUBLIC, cm.BYTE, "choose" + toJavaClassName(type.getName() + "Encoding"));
        encodingPicker.param(getJavaType(), "value");

        generateSize();
        generateEquals();
        generateHashCode();
        generateToString();

        sizeOfConstructor().body()._return(lit(1));
        sizeOfBody().body()._return(invoke("size").minus(invoke(sizeOfConstructor)));
    }

    private void generateHashCode() {
        hashCode = cls().method(JMod.PUBLIC, cm.INT, "hashCode");
        hashCode.body().block()._if(_this().ref("value").eq(_null()))
                ._then()
                ._return(dotclass(cls()).invoke("hashCode"));
        hashCode.body().block()._return(_this().ref("value").invoke("hashCode"));
    }

    private void generateSize() {
        sizer = generator.sizer().cls().method(JMod.PUBLIC, cm.LONG, "sizeOf" + toJavaClassName(type.getName()));
        sizer.param(getJavaType(), "value");

        size().body()._return(generator.registry().cls().staticInvoke("instance")
                .invoke("sizer").invoke("sizeOf" + toJavaClassName(type.getName())).arg(ref("value")));
    }

    private void generateConstructors() {
        noArgConstructor = cls().constructor(JMod.PUBLIC);
        noArgConstructor.body().block().assign(_this().ref("value"), _null());

        valueConstructor = cls().constructor(JMod.PUBLIC);
        valueConstructor.param(getJavaType(), "value");
        valueConstructor.body().block().assign(_this().ref("value"), ref("value"));
    }

    private void generateToString() {
        toString = cls().method(JMod.PUBLIC, cm.ref("java.lang.String"), "toString");
        toString.body()._if(_this().ref("value").eq(_null()))._then().block()._return(lit("null"));
        if ( type.getName().equals("array") ) {
            toString.body()._return(cm.ref("java.util.Arrays").staticInvoke("toString").arg(_this().ref("value")));
        } else {
            toString.body()._return(_this().ref("value").invoke("toString"));
        }
    }

    private void generateEquals() {
        equalsGeneric = cls().method(JMod.PUBLIC, cm.BOOLEAN, "equals");
        equalsGeneric.param(cm.ref("java.lang.Object"), "other");

        equalsGeneric.body().block()._if(_this().eq(ref("other")))
                ._then()
                ._return(TRUE);

        equalsGeneric.body().block()._if(
                ref("other").eq(_null())
                        .cor((ref("other")._instanceof(cls()).not())))
                ._then()
                ._return(FALSE);

        equalsGeneric.body().block()._return(_this().invoke("equals").arg(cast(cls(), ref("other"))));

        equalsTypeSpecific = cls().method(JMod.PUBLIC, cm.BOOLEAN, "equals");
        equalsTypeSpecific.param(cls(), "other");
        equalsTypeSpecific.body().block()._if(ref("other").eq(_null()))._then()._return(FALSE);
        JConditional test = equalsTypeSpecific.body().block()._if(_this().ref("value").eq(_null()).cand(ref("other").invoke("getValue").ne(_null())));
        test._then().block()._return(FALSE);
        JConditional test2 = test._elseif(_this().ref("value").ne(_null()).cand(ref("other").invoke("getValue").eq(_null())));
        test2._then().block()._return(FALSE);
        JConditional test3 = test2._elseif(_this().ref("value").eq(_null()).cand(ref("other").invoke("getValue").eq(_null())));
        test3._then().block()._return(TRUE);
        test3._else().block()._return(_this().ref("value").invoke("equals").arg(ref("other").invoke("getValue")));
    }

    protected void createStaticBlock() {

        initWriteMethods();

        JSwitch writeBodySwitchBlock = staticWriteBody.body().block()._switch(ref("formatCode"));

        staticRead().body().decl(cm.BYTE, "formatCode", ref("in").invoke("readByte"));

        JSwitch staticReadSwitchBlock = staticRead().body().block()._switch(ref("formatCode"));

        staticReadSwitchBlock._case(generator.registry().cls().staticRef("NULL_FORMAT_CODE")).body()._return(cast(cm._ref(getJavaType()), generator.registry().cls()
                .staticInvoke("instance")
                .invoke("encoder")
                .invoke("readNull")
                .arg(ref("in"))));

        JSwitch readSwitchBlock = read().body()._switch(ref("formatCode"));

        writeBodySwitchBlock._case(generator.registry().cls().staticRef("NULL_FORMAT_CODE")).body()._break();

        int highestWidth = 0;

        for ( Object obj : type.getEncodingOrDescriptorOrFieldOrChoiceOrDoc() ) {
            if ( obj instanceof Encoding ) {
                Encoding encoding = (Encoding) obj;

                String fieldName = type.getName();

                if ( encoding.getName() != null ) {
                    fieldName += "_" + encoding.getName();
                }

                Log.info("encoding name %s", fieldName);

                String staticCodeFieldName = toStaticName(fieldName + "_CODE");

                cls().field(JMod.PUBLIC | JMod.STATIC | JMod.FINAL, cm.BYTE, staticCodeFieldName, direct("(byte)" + encoding.getCode()));

                cls().init().add(
                        generator.registry().cls().staticInvoke("instance")
                                .invoke("getPrimitiveFormatCodeMap")
                                .invoke("put")
                                .arg(ref(staticCodeFieldName))
                                .arg(cls().dotclass())
                );

                int width = Integer.parseInt(encoding.getWidth());
                if ( width > highestWidth ) {
                    highestWidth = width;
                    highestWidthField = staticCodeFieldName;
                }

                cls().field(JMod.PUBLIC | JMod.STATIC | JMod.FINAL, cm.INT, toStaticName(fieldName + "_WIDTH"), lit(width));

                readSwitchBlock._case(ref(staticCodeFieldName)).body()
                        .assign(_this().ref("value"),
                                generator.registry().cls()
                                        .staticInvoke("instance")
                                        .invoke("encoder")
                                        .invoke("read" + toJavaClassName(fieldName))
                                        .arg(ref("in")))
                        ._break();

                staticReadSwitchBlock._case(ref(staticCodeFieldName)).body()
                        ._return(generator.registry().cls()
                                .staticInvoke("instance")
                                .invoke("encoder")
                                .invoke("read" + toJavaClassName(fieldName))
                                .arg(ref("in")));

                writeBodySwitchBlock._case(ref(staticCodeFieldName)).body()
                        .add(generator.registry().cls().staticInvoke("instance")
                                .invoke("encoder")
                                .invoke("write" + toJavaClassName(fieldName))
                                .arg(ref("value"))
                                .arg(ref("out")))
                        ._break();
            }
        }

        // TODO - create proper exception type to be thrown here
        readSwitchBlock._default().body()._throw(_new(cm.ref(Exception.class)).arg(lit("Unknown format code for " + type.getName() + " : 0x").plus(cm.ref("java.lang.String").staticInvoke("format").arg("%x").arg(ref("formatCode")))));
        staticReadSwitchBlock._default().body()._throw(_new(cm.ref(Exception.class)).arg(lit("Unknown format code for " + type.getName() + " : 0x").plus(cm.ref("java.lang.String").staticInvoke("format").arg("%x").arg(ref("formatCode")))));
        writeBodySwitchBlock._default().body()._throw(_new(cm.ref(Exception.class)).arg(lit("Unknown format code for " + type.getName() + " : 0x").plus(cm.ref("java.lang.String").staticInvoke("format").arg("%x").arg(ref("formatCode")))));
    }

    private void initWriteMethods() {
        write().body().staticInvoke(cls(), "write").arg(_this().ref("value")).arg(ref("out"));
        writeConstructor().body()._return(cls().staticInvoke("writeConstructor").arg(_this().ref("value")).arg(ref("out")));
        writeBody().body().staticInvoke(cls(), "writeBody").arg(ref("formatCode")).arg(_this().ref("value")).arg(ref("out"));

        staticWrite().body().decl(cm.BYTE, "formatCode", cls().staticInvoke("writeConstructor").arg(ref("value")).arg(ref("out")));
        staticWrite().body().staticInvoke(cls(), "writeBody").arg(ref("formatCode")).arg(ref("value")).arg(ref("out"));

        staticWriteConstructor = cls().method(JMod.PUBLIC | JMod.STATIC, cm.BYTE, "writeConstructor");
        staticWriteConstructor._throws(Exception.class);
        staticWriteConstructor.param(getJavaType(), "value");
        staticWriteConstructor.param(DataOutput.class, "out");

        staticWriteConstructor.body().decl(cm.BYTE, "formatCode", generator.registry().cls()
                .staticInvoke("instance")
                .invoke("picker")
                .invoke("choose" + toJavaClassName(type.getName() + "Encoding"))
                .arg(ref("value")));

        staticWriteConstructor.body().invoke(ref("out"), "writeByte").arg(ref("formatCode"));
        staticWriteConstructor.body()._return(ref("formatCode"));

        staticWriteBody = cls().method(JMod.PUBLIC | JMod.STATIC, cm.VOID, "writeBody");
        staticWriteBody._throws(Exception.class);
        staticWriteBody.param(cm.BYTE, "formatCode");
        staticWriteBody.param(getJavaType(), "value");
        staticWriteBody.param(DataOutput.class, "out");
    }

    private JMethod staticWrite() {
        if ( staticWrite == null ) {
            staticWrite = cls().method(JMod.PUBLIC | JMod.STATIC, cm.VOID, "write");
            staticWrite._throws(Exception.class);
            staticWrite.param(getJavaType(), "value");
            staticWrite.param(DataOutput.class, "out");
        }
        return staticWrite;
    }

    private JMethod staticRead() {
        if ( staticRead == null ) {
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
