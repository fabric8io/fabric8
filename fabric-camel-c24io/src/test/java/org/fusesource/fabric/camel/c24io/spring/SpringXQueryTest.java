/*
 * Copyright (C) 2011, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.fabric.camel.c24io.spring;

import org.apache.camel.CamelContext;
import org.fusesource.fabric.camel.c24io.XPathTest;

/**
 * @version $Revision$
 */
public class SpringXQueryTest extends XPathTest {
    @Override
    protected CamelContext createCamelContext() throws Exception {
        return SpringTestHelper.createSpringCamelContext(this, "org/fusesource/fabric/camel/c24io/spring/xqueryFilter.xml");
    }

}