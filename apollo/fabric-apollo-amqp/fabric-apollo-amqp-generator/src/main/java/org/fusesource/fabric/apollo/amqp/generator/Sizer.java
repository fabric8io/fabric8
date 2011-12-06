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

/**
 *
 */
public class Sizer {

    JDefinedClass definedClass;

    JCodeModel cm;
    Generator generator;

    public Sizer(Generator generator, String className) throws JClassAlreadyExistsException {
        cm = generator.getCm();
        this.generator = generator;

        definedClass = cm._class(className, ClassType.INTERFACE);

        generator.registry().cls().field(JMod.PROTECTED | JMod.FINAL | JMod.STATIC, cls(), "SIZER", JExpr.direct("AMQPSizer.instance()"));
        JMethod singletonAccessor = generator.registry().cls().method(JMod.PUBLIC, cls(), "sizer");
        singletonAccessor.body()._return(JExpr.ref("SIZER"));

    }

    public JDefinedClass cls() {
        return definedClass;
    }
}
