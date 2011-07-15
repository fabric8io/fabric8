/*
 * Copyright (C) 2011, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.fabric.fab.sample.camel;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.impl.converter.CorePackageScanClassResolver;
import org.apache.camel.impl.converter.DefaultTypeConverter;
import org.apache.camel.spi.TypeConverterRegistry;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class Activator implements BundleActivator {
    private DefaultCamelContext camelContext;

    public void start(BundleContext context) throws Exception {
        startCamel();
    }

    public void startCamel() throws Exception {
        System.out.println("Starting my Sample CamelContext");
        camelContext = new DefaultCamelContext();

        // TODO package scannning doesn't work in most containers so lets work around it.
        // until Camel supports non-package scanning by default...

        CorePackageScanClassResolver corePackageScanClassResolver = new CorePackageScanClassResolver();
        TypeConverterRegistry typeConverterRegistry = new DefaultTypeConverter(new CorePackageScanClassResolver(),
                camelContext.getInjector(), camelContext.getFactoryFinder(""));
        camelContext.setTypeConverterRegistry(typeConverterRegistry);

        camelContext.addRoutes(new RouteBuilder() {
            public void configure() throws Exception {
                from("timer://foo?fixedRate=trueperiod=5000").to("log:myFooTimer");
            }
        });
        camelContext.start();
    }

    public void stop(BundleContext context) throws Exception {
        stopCamel();
    }

    public void stopCamel() throws Exception {
        if (camelContext != null) {
            System.out.println("Shutting down my Sample CamelContext");
            camelContext.stop();
        }
    }
}
