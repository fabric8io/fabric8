/*
 * Copyright (C) 2011, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.fabric.camel.c24io;

import nonamespace.Mx2MtTransform;
import org.apache.camel.builder.RouteBuilder;

/**
 * @version $Revision$
 */
public class TransformUsingBeanClassTest extends TransformUsingBeanTest {

    protected RouteBuilder createRouteBuilder() {
        return new RouteBuilder() {
            public void configure() {
                from("file:src/test/data?noop=true").
                        bean(Mx2MtTransform.class).
                        to("mock:result");
            }
        };
    }
}