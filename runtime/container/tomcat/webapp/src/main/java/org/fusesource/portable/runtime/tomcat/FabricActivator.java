/*
 * #%L
 * Gravia :: Integration Tests :: Common
 * %%
 * Copyright (C) 2010 - 2013 JBoss by Red Hat
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 2.1 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 *
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-2.1.html>.
 * #L%
 */
package org.fusesource.portable.runtime.tomcat;

import io.fabric8.api.FabricService;

import java.util.concurrent.CountDownLatch;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.jboss.gravia.container.tomcat.support.TomcatResourceInstaller;
import org.jboss.gravia.container.tomcat.support.TomcatRuntimeFactory;
import org.jboss.gravia.provision.DefaultProvisioner;
import org.jboss.gravia.provision.Provisioner;
import org.jboss.gravia.provision.spi.RuntimeEnvironment;
import org.jboss.gravia.repository.DefaultRepository;
import org.jboss.gravia.repository.Repository;
import org.jboss.gravia.repository.RepositoryRuntimeRegistration;
import org.jboss.gravia.repository.RepositoryRuntimeRegistration.Registration;
import org.jboss.gravia.resolver.DefaultResolver;
import org.jboss.gravia.resolver.Resolver;
import org.jboss.gravia.runtime.Module;
import org.jboss.gravia.runtime.ModuleContext;
import org.jboss.gravia.runtime.ModuleException;
import org.jboss.gravia.runtime.Runtime;
import org.jboss.gravia.runtime.RuntimeLocator;
import org.jboss.gravia.runtime.ServiceEvent;
import org.jboss.gravia.runtime.ServiceListener;
import org.jboss.gravia.runtime.ServiceRegistration;
import org.jboss.gravia.runtime.WebAppContextListener;
import org.jboss.gravia.runtime.embedded.spi.BundleContextAdaptor;
import org.jboss.gravia.runtime.spi.PropertiesProvider;
import org.jboss.gravia.runtime.util.RuntimePropertiesProvider;
import org.osgi.framework.BundleContext;

/**
 * Activates the {@link Runtime} as part of the web app lifecycle.
 *
 * @author thomas.diesler@jboss.com
 * @since 20-Nov-2013
 */
public class FabricActivator implements ServletContextListener {

    private Registration repositoryRegistration;
    private ServiceRegistration<Provisioner> provisionerRegistration;
    private ServiceRegistration<Resolver> resolverRegistration;

    @Override
    public void contextInitialized(ServletContextEvent event) {

        // Create the runtime
        ServletContext servletContext = event.getServletContext();
        PropertiesProvider propsProvider = new FabricPropertiesProvider(servletContext);
        Runtime runtime = RuntimeLocator.createRuntime(new TomcatRuntimeFactory(servletContext), propsProvider);
        runtime.init();

        // Start listening on the {@link FabricService}
        final BoostrapLatch latch = new BoostrapLatch(1);
        final ModuleContext syscontext = runtime.getModuleContext();
        ServiceListener listener = new ServiceListener() {
            @Override
            public void serviceChanged(ServiceEvent event) {
                if (event.getType() == ServiceEvent.REGISTERED) {
                    syscontext.removeServiceListener(this);
                    latch.countDown();
                }
            }
        };
        syscontext.addServiceListener(listener, "(objectClass=" + FabricService.class.getName() + ")");

        servletContext.setAttribute(BoostrapLatch.class.getName(), latch);

        // Install and start this webapp as a module
        WebAppContextListener webappInstaller = new WebAppContextListener();
        Module module = webappInstaller.installWebappModule(servletContext);
        servletContext.setAttribute(Module.class.getName(), module);
        try {
            module.start();
        } catch (ModuleException ex) {
            throw new IllegalStateException(ex);
        }

        // HttpService integration
        ModuleContext moduleContext = module.getModuleContext();
        BundleContext bundleContext = new BundleContextAdaptor(moduleContext);
        servletContext.setAttribute(BundleContext.class.getName(), bundleContext);

        // Register the {@link Repository}, {@link Resolver}, {@link Provisioner} services
        Repository repository = registerRepositoryService(runtime);
        Resolver resolver = registerResolverService(runtime);
        registerProvisionerService(servletContext, runtime, repository, resolver);
    }

    private Provisioner registerProvisionerService(ServletContext servletContext, Runtime runtime, Repository repository, Resolver resolver) {
        RuntimeEnvironment environment = createEnvironment(servletContext, runtime);
        TomcatResourceInstaller installer = new TomcatResourceInstaller(environment);
        Provisioner provisioner = new DefaultProvisioner(environment, resolver, repository, installer);
        ModuleContext syscontext = runtime.getModuleContext();
        provisionerRegistration = syscontext.registerService(Provisioner.class, provisioner, null);
        return provisioner;
    }

    private RuntimeEnvironment createEnvironment(ServletContext servletContext, Runtime runtime) {
        return new RuntimeEnvironment(runtime).initDefaultContent();
    }

    private Resolver registerResolverService(Runtime runtime) {
        Resolver resolver = new DefaultResolver();
        ModuleContext syscontext = runtime.getModuleContext();
        resolverRegistration = syscontext.registerService(Resolver.class, resolver, null);
        return resolver;
    }

    private Repository registerRepositoryService(final Runtime runtime) {
        PropertiesProvider propertyProvider = new RuntimePropertiesProvider(runtime);
        Repository repository = new DefaultRepository(propertyProvider);
        ModuleContext context = runtime.getModuleContext();
        repositoryRegistration =  RepositoryRuntimeRegistration.registerRepository(context, repository);
        return repository;
    }

    @Override
    public void contextDestroyed(ServletContextEvent event) {
        if (provisionerRegistration != null)
            provisionerRegistration.unregister();
        if (repositoryRegistration != null)
            repositoryRegistration.unregister();
        if (resolverRegistration != null)
            resolverRegistration.unregister();
    }

    static class BoostrapLatch extends CountDownLatch {

        BoostrapLatch(int count) {
            super(count);
        }
    }
}
