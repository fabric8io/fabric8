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
    protected JMethod read;
    protected JMethod encodeTo;
    protected JMethod decodeFrom;

    public AmqpDefinedType(Generator generator, String className, Type type) throws JClassAlreadyExistsException {
        this.cm = generator.getCm();
        this.generator = generator;
        this.type = type;
        definedClass = this.cm._class(className);
        init();
    }

    protected void init() {
        if ( type.getProvides() != null )  {
            cls()._implements(cm.ref(generator.getPackagePrefix() + "." + generator.getInterfaces() + "." + toJavaClassName(type.getProvides())));
        } else {
            cls()._implements(cm.ref(generator.getAmqpBaseType()));
        }

        createInitialFields();
        createStaticBlock();

        generator.registry().cls().init().add(JExpr._new(cls()));

        write();
        read();
        encodeTo();
        decodeFrom();
    }

    protected abstract void createStaticBlock();

    protected abstract void createInitialFields();

    public JMethod write() {
        if (write == null) {
            write = cls().method(JMod.PUBLIC, cm.VOID, "write");
            write._throws(java.lang.Exception.class);
            write.param(java.io.DataOutput.class, "out");
        }
        return write;
    }

    public JMethod read() {
        if (read == null ) {
            read = cls().method(JMod.PUBLIC, cm.VOID, "read");
            read._throws(java.lang.Exception.class);
            read.param(cm.BYTE, "formatCode");
            read.param(java.io.DataInput.class, "in");
        }
        return read;
    }

    public JMethod encodeTo() {
        if (encodeTo == null) {
            encodeTo = cls().method(JMod.PUBLIC, cm.VOID, "encodeTo");
            encodeTo._throws(java.lang.Exception.class);
            encodeTo.param(Buffer.class, "buffer");
            encodeTo.param(cm.INT, "offset");
        }
        return encodeTo;
    }

    public JMethod decodeFrom() {
        if (decodeFrom == null) {
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
