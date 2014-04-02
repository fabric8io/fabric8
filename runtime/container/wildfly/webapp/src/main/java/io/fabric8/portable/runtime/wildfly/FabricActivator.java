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
package io.fabric8.portable.runtime.wildfly;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.jboss.gravia.runtime.Module;
import org.jboss.gravia.runtime.ModuleContext;
import org.jboss.gravia.runtime.Runtime;
import org.jboss.gravia.runtime.RuntimeLocator;
import org.jboss.gravia.runtime.embedded.spi.BundleContextAdaptor;
import org.osgi.framework.BundleContext;

/**
 * Initialize the fabric webapp
 *
 * @since 20-Nov-2013
 */
public class FabricActivator implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent event) {

        // Get the module for this webapp
        Runtime runtime = RuntimeLocator.getRequiredRuntime();
        ServletContext servletContext = event.getServletContext();
        Module module = runtime.getModule(servletContext.getClassLoader());

        // HttpService integration
        ModuleContext moduleContext = module.getModuleContext();
        BundleContext bundleContext = new BundleContextAdaptor(moduleContext);
        servletContext.setAttribute(BundleContext.class.getName(), bundleContext);
    }

    @Override
    public void contextDestroyed(ServletContextEvent event) {
    }
}
