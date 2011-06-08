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
import org.apache.camel.Service;
import org.apache.camel.spring.SpringCamelContext;
import org.apache.camel.test.CamelTestSupport;

import org.springframework.context.support.AbstractXmlApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * @version 
 */
public final class SpringTestHelper {
    
    private SpringTestHelper() {        
    }
    
    public static CamelContext createSpringCamelContext(CamelTestSupport test, String classpathUri) throws Exception {
        test.setUseRouteBuilder(false);

        final AbstractXmlApplicationContext applicationContext = new ClassPathXmlApplicationContext(classpathUri);
        test.setCamelContextService(new Service() {
            public void start() throws Exception {
                applicationContext.start();
            }

            public void stop() throws Exception {
                applicationContext.stop();
            }
        });

        return SpringCamelContext.springCamelContext(applicationContext);
    }
}
