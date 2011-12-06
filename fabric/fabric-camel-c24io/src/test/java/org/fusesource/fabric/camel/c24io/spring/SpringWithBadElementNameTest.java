/*
 * Copyright (C) 2011, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.fabric.camel.c24io.spring;

import junit.framework.TestCase;
import org.apache.camel.RuntimeCamelException;
import org.springframework.context.support.AbstractXmlApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * @version $Revision$
 */
public class SpringWithBadElementNameTest extends TestCase {
    public void testBadElementName() throws Exception {
        try {
            AbstractXmlApplicationContext appContext = new ClassPathXmlApplicationContext("org/fusesource/fabric/camel/c24io/spring/badElementName.xml");
            appContext.start();
            fail("should have failed!");
        } catch (RuntimeCamelException e) {
            System.out.println("Caught expected: " + e);
            e.printStackTrace();
        }
    }
}