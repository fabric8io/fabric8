/**
 * Copyright (C) FuseSource, Inc.
 * http://fusesource.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.fusesource.fabric.apollo.amqp.generator;

import com.sun.codemodel.*;
import org.fusesource.fabric.apollo.amqp.jaxb.schema.Type;
import org.fusesource.hawtbuf.Buffer;

import static org.fusesource.fabric.apollo.amqp.generator.Utilities.toJavaClassName;

/**
 *
 */
public abstract class AmqpDefinedType {

    protected JCodeModel cm;
    protected Generator generator;
    protected Type type;
    protected JDefinedClass definedClass;

    protected JMethod write;
    protected JMethod writeConstructor;
    protected JMethod writeBody;
    protected JMethod read;
    protected JMethod encodeTo;
    protected JMethod decodeFrom;
    protected JMethod getArrayConstructor;
    protected JMethod size;
    protected JMethod sizeOfConstructor;
    protected JMethod sizeOfBody;

    public AmqpDefinedType(Generator generator, String className, Type type) throws JClassAlreadyExistsException {
        Log.info("Creating new class %s for type %s", className, type.getName());
        this.cm = generator.getCm();
        this.generator = generator;
        this.type = type;
        definedClass = this.cm._class(className);
        init();
    }

    protected void init() {
        if ( type.getProvides() != null ) {
            String types[] = type.getProvides().split(",");
            for ( String t : types ) {
                cls()._implements(cm.ref(generator.getInterfaces() + "." + toJavaClassName(t)));
            }
        } else {
            cls()._implements(cm.ref(generator.getAmqpBaseType()));
        }

        createInitialFields();
        createStaticBlock();
        createGetArrayConstructor();

        generator.registry().cls().init().add(JExpr._new(cls()));

        /*
        sizeOfConstructor();
        sizeOfBody();
        size();
        write();
        writeConstructor();
        writeBody();
        read();
        encodeTo();
        decodeFrom();
        */
    }

    protected abstract void createGetArrayConstructor();

    protected abstract void createStaticBlock();

    protected abstract void createInitialFields();

    public JMethod size() {
        if ( size == null ) {
            size = cls().method(JMod.PUBLIC, cm.LONG, "size");
        }
        return size;
    }

    public JMethod writeConstructor() {
        if ( writeConstructor == null ) {
            writeConstructor = cls().method(JMod.PUBLIC, cm.BYTE, "writeConstructor");
            writeConstructor._throws(java.lang.Exception.class);
            writeConstructor.param(java.io.DataOutput.class, "out");
        }
        return writeConstructor;
    }

    public JMethod writeBody() {
        if ( writeBody == null ) {
            writeBody = cls().method(JMod.PUBLIC, cm.VOID, "writeBody");
            writeBody._throws(java.lang.Exception.class);
            writeBody.param(cm.BYTE, "formatCode");
            writeBody.param(java.io.DataOutput.class, "out");
        }
        return writeBody;
    }

    public JMethod sizeOfConstructor() {
        if ( sizeOfConstructor == null ) {
            sizeOfConstructor = cls().method(JMod.PUBLIC, cm.LONG, "sizeOfConstructor");
        }
        return sizeOfConstructor;
    }

    public JMethod sizeOfBody() {
        if ( sizeOfBody == null ) {
            sizeOfBody = cls().method(JMod.PUBLIC, cm.LONG, "sizeOfBody");
        }
        return sizeOfBody;
    }


    public JMethod write() {
        if ( write == null ) {
            write = cls().method(JMod.PUBLIC, cm.VOID, "write");
            write._throws(java.lang.Exception.class);
            write.param(java.io.DataOutput.class, "out");
        }
        return write;
    }

    public JMethod read() {
        if ( read == null ) {
            read = cls().method(JMod.PUBLIC, cm.VOID, "read");
            read._throws(java.lang.Exception.class);
            read.param(cm.BYTE, "formatCode");
            read.param(java.io.DataInput.class, "in");
        }
        return read;
    }

    public JMethod encodeTo() {
        if ( encodeTo == null ) {
            encodeTo = cls().method(JMod.PUBLIC, cm.VOID, "encodeTo");
            encodeTo._throws(java.lang.Exception.class);
            encodeTo.param(Buffer.class, "buffer");
            encodeTo.param(cm.INT, "offset");
        }
        return encodeTo;
    }

    public JMethod decodeFrom() {
        if ( decodeFrom == null ) {
            decodeFrom = cls().method(JMod.PUBLIC, cm.VOID, "decodeFrom");
            decodeFrom._throws(java.lang.Exception.class);
            decodeFrom.param(cm.BYTE, "formatCode");
            decodeFrom.param(Buffer.class, "buffer");
            decodeFrom.param(cm.INT, "offset");
        }
        return decodeFrom;
    }

    public JDefinedClass cls() {
        return definedClass;
    }

}
