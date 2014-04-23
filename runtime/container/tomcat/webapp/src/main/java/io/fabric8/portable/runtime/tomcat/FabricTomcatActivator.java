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

import io.fabric8.api.ZooKeeperClusterBootstrap;

import java.io.File;
import java.util.concurrent.CountDownLatch;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.jboss.gravia.Constants;
import org.jboss.gravia.container.common.ActivationSupport;
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
import org.jboss.gravia.runtime.spi.RuntimePropertiesProvider;
import org.osgi.framework.BundleContext;

/**
 * Activates the {@link Runtime} as part of the web app lifecycle.
 */
public class  FabricTomcatActivator implements ServletContextListener {

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

        // Initialize ConfigurationAdmin content
        Object configsDir = propsProvider.getProperty(Constants.PROPERTY_CONFIGURATIONS_DIR);
        ActivationSupport.initConfigurationAdmin(new File((String) configsDir));

        // Start listening on the {@link ZooKeeperClusterBootstrap}
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
        syscontext.addServiceListener(listener, "(objectClass=" + ZooKeeperClusterBootstrap.class.getName() + ")");

        servletContext.setAttribute(BoostrapLatch.class.getName(), latch);
        servletContext.setAttribute(BundleContext.class.getName(), new BundleContextAdaptor(syscontext));

        // Install and start this webapp as a module
        WebAppContextListener webappInstaller = new WebAppContextListener();
        Module module = webappInstaller.installWebappModule(servletContext);
        servletContext.setAttribute(Module.class.getName(), module);
        try {
            module.start();
        } catch (ModuleException ex) {
            throw new IllegalStateException(ex);
        }

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
        repositoryRegistration = RepositoryRuntimeRegistration.registerRepository(context, repository);
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
