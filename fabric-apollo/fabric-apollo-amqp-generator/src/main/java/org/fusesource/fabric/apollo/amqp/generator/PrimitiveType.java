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

/**
 *
 */
public class PrimitiveType extends AmqpDefinedType {

    private JFieldVar value;

    private JMethod getValue;
    private JMethod setValue;

    public PrimitiveType(Generator generator, String className, Type type) throws JClassAlreadyExistsException {
        super(generator, className, type);
    }

    protected void createInitialFields() {
        value = cls().field(JMod.PRIVATE, getJavaType(), "value");

        getValue = cls().method(JMod.PUBLIC, getJavaType(), "getValue");
        getValue.body()._return(JExpr._this().ref("value"));
        setValue = cls().method(JMod.PUBLIC, cm.VOID, "setValue");
        setValue.param(getJavaType(), "value");
        setValue.body().block().assign(JExpr._this().ref("value"), JExpr.ref("value"));
    }

    protected void createStaticBlock() {

    }

    public Class getJavaType() {
        return generator.getMapping().get(type.getName());
    }
}
