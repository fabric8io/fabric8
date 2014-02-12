/*
 * #%L
 * Wildfly Gravia Subsystem
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

package org.wildfly.extension.fabric.service;

import io.fabric8.api.FabricService;

import java.io.IOException;
import java.net.URL;
import java.util.Dictionary;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.gravia.resource.Attachable;
import org.jboss.gravia.runtime.Module;
import org.jboss.gravia.runtime.ModuleContext;
import org.jboss.gravia.runtime.Runtime;
import org.jboss.gravia.runtime.ServiceEvent;
import org.jboss.gravia.runtime.ServiceListener;
import org.jboss.gravia.runtime.ServiceReference;
import org.jboss.gravia.runtime.spi.AbstractModule;
import org.jboss.gravia.runtime.spi.ClassLoaderEntriesProvider;
import org.jboss.gravia.runtime.spi.ManifestHeadersProvider;
import org.jboss.gravia.runtime.spi.ModuleEntriesProvider;
import org.jboss.modules.ModuleClassLoader;
import org.jboss.msc.service.AbstractService;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wildfly.extension.fabric.FabricConstants;
import org.wildfly.extension.gravia.GraviaConstants;

/**
 * Service responsible for creating and managing the life-cycle of the gravia subsystem.
 *
 * @author Thomas.Diesler@jboss.com
 * @since 19-Apr-2013
 */
public class FabricBootstrapService extends AbstractService<FabricService> {

    static final Logger LOGGER = LoggerFactory.getLogger(FabricConstants.class.getPackage().getName());

    private final InjectedValue<ModuleContext> injectedModuleContext = new InjectedValue<ModuleContext>();
    private final InjectedValue<Runtime> injectedRuntime = new InjectedValue<Runtime>();

    private FabricService fabricService;
    private Module module;

    public ServiceController<FabricService> install(ServiceTarget serviceTarget, ServiceVerificationHandler verificationHandler) {
        ServiceBuilder<FabricService> builder = serviceTarget.addService(FabricConstants.FABRIC_SUBSYSTEM_SERVICE_NAME, this);
        builder.addDependency(GraviaConstants.MODULE_CONTEXT_SERVICE_NAME, ModuleContext.class, injectedModuleContext);
        builder.addDependency(GraviaConstants.RUNTIME_SERVICE_NAME, Runtime.class, injectedRuntime);
        builder.addListener(verificationHandler);
        return builder.install();
    }

    @Override
    public void start(StartContext startContext) throws StartException {
        LOGGER.info("Activating Fabric Subsystem");

        // Start listening on the {@link FabricService}
        final CountDownLatch latch = new CountDownLatch(1);
        final ModuleContext syscontext = injectedModuleContext.getValue();
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

        // Install and start this as a {@link Module}
        Runtime runtime = injectedRuntime.getValue();
        ModuleClassLoader classLoader = (ModuleClassLoader) getClass().getClassLoader();
        try {
            URL url = classLoader.getResource(JarFile.MANIFEST_NAME);
            Manifest manifest = new Manifest(url.openStream());
            Dictionary<String, String> headers = new ManifestHeadersProvider(manifest).getHeaders();
            module = runtime.installModule(classLoader, headers);

            // Attach the {@link ModuleEntriesProvider} so
            ModuleEntriesProvider entriesProvider = new ClassLoaderEntriesProvider(module);
            Attachable attachable = AbstractModule.assertAbstractModule(module);
            attachable.putAttachment(AbstractModule.MODULE_ENTRIES_PROVIDER_KEY, entriesProvider);

            // Start the module
            module.start();

        } catch (RuntimeException rte) {
            throw rte;
        } catch (Exception ex) {
            throw new StartException(ex);
        }

        // Wait for the {@link FabricService} to come up
        try {
            if (!latch.await(5, TimeUnit.SECONDS)) {
                throw new StartException("Cannot obtain FabricService");
            }
        } catch (InterruptedException ex) {
            // ignore
        }

        ServiceReference<FabricService> sref = syscontext.getServiceReference(FabricService.class);
        fabricService = syscontext.getService(sref);

        // FuseFabric banner message
        Properties brandingProperties = new Properties();
        String resname = "/META-INF/branding.properties";
        try {
            URL brandingURL = getClass().getResource(resname);
            brandingProperties.load(brandingURL.openStream());
        } catch (IOException e) {
            throw new StartException("Cannot read branding properties from: " + resname);
        }
        System.out.println(brandingProperties.getProperty("welcome"));
    }

    @Override
    public void stop(StopContext context) {
        // Uninstall the bootstrap module
        if (module != null) {
            module.uninstall();
        }
    }

    @Override
    public FabricService getValue() throws IllegalStateException {
        return fabricService;
    }
}
