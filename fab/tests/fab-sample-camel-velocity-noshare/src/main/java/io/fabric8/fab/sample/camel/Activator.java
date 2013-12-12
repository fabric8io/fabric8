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
package io.fabric8.fab.sample.camel;

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
                from("timer://foo?fixedRate=trueperiod=5000").to("velocity:transform.vm").to("log:myFooTimer");
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
