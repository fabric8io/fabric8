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

import io.fabric8.api.CreateEnsembleOptions;
import io.fabric8.api.FabricService;
import io.fabric8.utils.SystemProperties;

import java.io.File;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.jboss.gravia.Constants;
import org.jboss.gravia.container.tomcat.extension.TomcatRuntimeFactory;
import org.jboss.gravia.runtime.Module;
import org.jboss.gravia.runtime.ModuleContext;
import org.jboss.gravia.runtime.ModuleException;
import org.jboss.gravia.runtime.Runtime;
import org.jboss.gravia.runtime.RuntimeLocator;
import org.jboss.gravia.runtime.ServiceEvent;
import org.jboss.gravia.runtime.ServiceListener;
import org.jboss.gravia.runtime.WebAppContextListener;
import org.jboss.gravia.runtime.util.DefaultPropertiesProvider;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

/**
 * Activates the {@link Runtime} as part of the web app lifecycle.
 *
 * @author thomas.diesler@jboss.com
 * @since 20-Nov-2013
 */
public class FabricActivator implements ServletContextListener {

    private final static File catalinaHome = new File(SecurityActions.getSystemProperty("catalina.home", null));

    @Override
    public void contextInitialized(ServletContextEvent event) {

        // Create the runtime
        Properties sysprops = getRuntimeProperties();
        DefaultPropertiesProvider propsProvider = new DefaultPropertiesProvider(sysprops, true);
        Runtime runtime = RuntimeLocator.createRuntime(new TomcatRuntimeFactory(), propsProvider);
        runtime.init();

        // Start listening on the {@link FabricService}
        final BoostrapLatch latch = new BoostrapLatch(1);
        final ModuleContext syscontext = runtime.getModule(0).getModuleContext();
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

        ServletContext servletContext = event.getServletContext();
        servletContext.setAttribute(BoostrapLatch.class.getName(), latch);

        // Install and start this webapp as a module
        WebAppContextListener webappInstaller = new WebAppContextListener();
        Module module = webappInstaller.installWebappModule(runtime, servletContext);
        servletContext.setAttribute(Module.class.getName(), module);
        try {
            module.start();
        } catch (ModuleException ex) {
            throw new IllegalStateException(ex);
        }

        // HttpService integration
        BundleContext bundleContext = module.adapt(Bundle.class).getBundleContext();
        servletContext.setAttribute("org.osgi.framework.BundleContext", bundleContext);
    }

    @Override
    public void contextDestroyed(ServletContextEvent event) {
    }

    private Properties getRuntimeProperties() {

        Properties properties = new Properties();

        // Setup the karaf.home directory
        File catalinaWork = new File(catalinaHome.getPath() + File.separator + "work");
        File karafHome = new File(catalinaWork.getPath() + File.separator + "fabric");
        File karafData = new File(karafHome.getPath() + File.separator + "data");
        File profilesImport = new File(karafHome.getPath() + File.separator + "import");

        // Gravia integration properties
        File storageDir = new File(karafData.getPath() + File.separator + Constants.RUNTIME_STORAGE_DEFAULT);
        properties.setProperty(Constants.RUNTIME_STORAGE_CLEAN, Constants.RUNTIME_STORAGE_CLEAN_ONFIRSTINIT);
        properties.setProperty(Constants.RUNTIME_STORAGE, storageDir.getAbsolutePath());
        properties.setProperty(Constants.RUNTIME_TYPE, "tomcat");

        // Fabric integration properties
        properties.setProperty(CreateEnsembleOptions.ENSEMBLE_AUTOSTART, Boolean.TRUE.toString());
        properties.setProperty(CreateEnsembleOptions.PROFILES_AUTOIMPORT_PATH, profilesImport.getAbsolutePath());

        // [TODO] Derive port from tomcat config
        // https://issues.jboss.org/browse/FABRIC-761
        properties.setProperty("org.osgi.service.http.port", "8080");

        // Karaf integration properties
        properties.setProperty(SystemProperties.KARAF_HOME, karafHome.getAbsolutePath());
        properties.setProperty(SystemProperties.KARAF_BASE, karafHome.getAbsolutePath());
        properties.setProperty(SystemProperties.KARAF_DATA, karafData.getAbsolutePath());
        properties.setProperty(SystemProperties.KARAF_NAME, "root");

        return properties;
    }

    static class BoostrapLatch extends CountDownLatch {

        BoostrapLatch(int count) {
            super(count);
        }
    }
}
