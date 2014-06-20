/**
 *  Copyright 2005-2014 Red Hat, Inc.
 *
 *  Red Hat licenses this file to you under the Apache License, version
 *  2.0 (the "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 *  implied.  See the License for the specific language governing
 *  permissions and limitations under the License.
 */
package io.fabric8.portable.runtime.tomcat;


import java.io.File;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import io.fabric8.api.BootstrapComplete;
import org.jboss.gravia.Constants;
import org.jboss.gravia.container.tomcat.support.TomcatResourceInstaller;
import org.jboss.gravia.container.tomcat.support.TomcatRuntimeFactory;
import org.jboss.gravia.provision.ResourceInstaller;
import org.jboss.gravia.provision.spi.RuntimeEnvironment;
import org.jboss.gravia.runtime.Module;
import org.jboss.gravia.runtime.ModuleContext;
import org.jboss.gravia.runtime.ModuleException;
import org.jboss.gravia.runtime.Runtime;
import org.jboss.gravia.runtime.RuntimeLocator;
import org.jboss.gravia.runtime.ServiceEvent;
import org.jboss.gravia.runtime.ServiceListener;
import org.jboss.gravia.runtime.ServiceRegistration;
import org.jboss.gravia.runtime.WebAppContextListener;
import org.jboss.gravia.runtime.spi.PropertiesProvider;

/**
 * Activates the {@link Runtime} as part of the web app lifecycle.
 */
public class FabricTomcatActivator implements ServletContextListener {

    private final Set<ServiceRegistration<?>> registrations = new HashSet<ServiceRegistration<?>>();

    @Override
    public void contextInitialized(ServletContextEvent event) {

        // Create the runtime
        ServletContext servletContext = event.getServletContext();
        PropertiesProvider propsProvider = new FabricPropertiesProvider(servletContext);
        Runtime runtime = RuntimeLocator.createRuntime(new TomcatRuntimeFactory(servletContext), propsProvider);
        runtime.init();

        // Start listening on the {@link BootstrapComplete}
        final ModuleContext syscontext = runtime.getModuleContext();
        final BoostrapLatch latch = new BoostrapLatch(1);
        ServiceListener listener = new ServiceListener() {
            @Override
            public void serviceChanged(ServiceEvent event) {
                if (event.getType() == ServiceEvent.REGISTERED) {
                    syscontext.removeServiceListener(this);
                    latch.countDown();
                }
            }
        };
        servletContext.setAttribute(BoostrapLatch.class.getName(), latch);
        syscontext.addServiceListener(listener, "(objectClass=" + BootstrapComplete.class.getName() + ")");

        // Register the {@link RuntimeEnvironment}, {@link ResourceInstaller} services
        registerServices(servletContext, runtime);

        // Register {@link ContainerCreateHandler} for Karaf, Tomcat, Wildfly
        /*
        Set<ContainerCreateHandler> handlers = new HashSet<ContainerCreateHandler>();
        handlers.add(new KarafContainerCreateHandler());
        handlers.add(new TomcatContainerCreateHandler());
        handlers.add(new WildFlyContainerCreateHandler());
        registerContainerCreateHandlers(syscontext, handlers);
        */

        // Install and start this webapp as a module
        WebAppContextListener webappInstaller = new WebAppContextListener();
        Module module = webappInstaller.installWebappModule(servletContext);
        servletContext.setAttribute(Module.class.getName(), module);
        try {
            module.start();
        } catch (ModuleException ex) {
            throw new IllegalStateException(ex);
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent event) {
        // Unregister system services
        for (ServiceRegistration<?> sreg : registrations) {
            sreg.unregister();
        }
    }

    private void registerServices(ServletContext servletContext, Runtime runtime) {
        RuntimeEnvironment environment = new RuntimeEnvironment(runtime).initDefaultContent();
        TomcatResourceInstaller installer = new TomcatResourceInstaller(environment);
        ModuleContext syscontext = runtime.getModuleContext();
        registrations.add(syscontext.registerService(RuntimeEnvironment.class, environment, null));
        registrations.add(syscontext.registerService(ResourceInstaller.class, installer, null));
    }

    /*
    private void registerContainerCreateHandlers(ModuleContext context, Set<ContainerCreateHandler> handlers) {
        for (ContainerCreateHandler handler : handlers) {
            String[] classes = new String[] { handler.getClass().getName(), ContainerCreateHandler.class.getName() };
            registrations.add(context.registerService(classes, handler, null));
        }
    }
    */

    static class BoostrapLatch extends CountDownLatch {

        BoostrapLatch(int count) {
            super(count);
        }
    }
}
