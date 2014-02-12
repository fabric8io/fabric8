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

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

import org.fusesource.fabric.api.CreateEnsembleOptions;
import org.fusesource.fabric.api.FabricService;
import org.fusesource.fabric.utils.SystemProperties;
import org.jboss.gravia.runtime.Constants;
import org.jboss.gravia.runtime.Module;
import org.jboss.gravia.runtime.ModuleContext;
import org.jboss.gravia.runtime.ModuleException;
import org.jboss.gravia.runtime.Runtime;
import org.jboss.gravia.runtime.RuntimeLocator;
import org.jboss.gravia.runtime.ServiceEvent;
import org.jboss.gravia.runtime.ServiceListener;
import org.jboss.gravia.runtime.tomcat.TomcatRuntimeFactory;
import org.jboss.gravia.runtime.util.DefaultPropertiesProvider;
import org.jboss.gravia.runtime.util.ManifestHeadersProvider;

/**
 * Activates the {@link Runtime} as part of the web app lifecycle.
 *
 * @author thomas.diesler@jboss.com
 * @since 20-Nov-2013
 */
@WebListener
public class FabricActivator implements ServletContextListener {

    private final static String[] moduleNames = new String[] { "fabric-core", "fabric-git", "fabric-zookeeper" };
    private final static File catalinaHome = new File(SecurityActions.getSystemProperty("catalina.home", null));

    private List<Module> modules;

    @Override
    public void contextInitialized(ServletContextEvent event) {

        // Create the runtime
        Properties sysprops = getRuntimeProperties();
        DefaultPropertiesProvider propsProvider = new DefaultPropertiesProvider(sysprops, true);
        Runtime runtime = RuntimeLocator.createRuntime(new TomcatRuntimeFactory(), propsProvider);
        runtime.init();

        // Start listening on the {@link FabricService}
        final CountDownLatch latch = new CountDownLatch(1);
        ServiceListener listener = new ServiceListener() {
            @Override
            public void serviceChanged(ServiceEvent event) {
                if (event.getType() == ServiceEvent.REGISTERED)
                    latch.countDown();
            }
        };
        ModuleContext syscontext = runtime.getModule(0).getModuleContext();
        syscontext.addServiceListener(listener, "(objectClass=" + FabricService.class.getName() + ")");

        // Install the bootstrap modules
        File catalinaLib = new File(catalinaHome.getPath() + File.separator + "lib");
        modules = new ArrayList<Module>();
        for (final String modname : moduleNames) {
            String[] list = catalinaLib.list(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    return name.startsWith(modname);
                }
            });
            if (list.length != 1)
                throw new IllegalStateException("Cannot find '" + modname + "' at: " + catalinaLib);

            try {
                File modfile = new File(catalinaLib.getPath() + File.separator + list[0]);
                Manifest manifest = new JarFile(modfile).getManifest();
                Dictionary<String, String> headers = new ManifestHeadersProvider(manifest).getHeaders();
                modules.add(runtime.installModule(FabricService.class.getClassLoader(), headers));
            } catch (RuntimeException rte) {
                throw rte;
            } catch (Exception ex) {
                throw new IllegalStateException(ex);
            }
        }

        // Start the bootstrap modules
        for (Module module : modules) {
            try {
                module.start();
            } catch (ModuleException ex) {
                throw new IllegalStateException(ex);
            }
        }

        // Wait for the {@link FabricService} to come up
        try {
            if (!latch.await(5, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Cannot obtain FabricService");
            }
        } catch (InterruptedException ex) {
            // ignore
        }

        // FuseFabric banner message
        Properties brandingProperties = new Properties();
        String resname = "/WEB-INF/branding.properties";
        try {
            ServletContext servletContext = event.getServletContext();
            URL brandingURL = servletContext.getResource(resname);
            brandingProperties.load(brandingURL.openStream());
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read branding properties from: " + resname);
        }
        System.out.println(brandingProperties.getProperty("welcome"));
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

        // Fabric integration properties
        properties.setProperty(CreateEnsembleOptions.ENSEMBLE_AUTOSTART, Boolean.TRUE.toString());
        properties.setProperty(CreateEnsembleOptions.PROFILES_AUTOIMPORT_PATH, profilesImport.getAbsolutePath());

        // Karaf integration properties
        properties.setProperty(SystemProperties.KARAF_HOME, karafHome.getAbsolutePath());
        properties.setProperty(SystemProperties.KARAF_BASE, karafHome.getAbsolutePath());
        properties.setProperty(SystemProperties.KARAF_DATA, karafData.getAbsolutePath());
        properties.setProperty(SystemProperties.KARAF_NAME, "root");

        return properties;
    }
}
