/*
 * Copyright (C) 2011, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.fabric.fab.sample.camel;

import org.apache.camel.CamelContext;
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
        // inside OSGi the thread context class loader isn't set to the right class loader so lets try that now
        ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            ClassLoader classLoader = DefaultCamelContext.class.getClassLoader();
            Thread.currentThread().setContextClassLoader(classLoader);
            System.out.println("Setting the context class loader to: " + classLoader);
            startCamel();
        } finally {
            Thread.currentThread().setContextClassLoader(oldClassLoader);
        }
    }

    public void startCamel() throws Exception {
        System.out.println("Starting my Sample CamelContext");
        camelContext = new DefaultCamelContext();

        // TODO package scannning doesn't work in most containers so lets work around it.

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
