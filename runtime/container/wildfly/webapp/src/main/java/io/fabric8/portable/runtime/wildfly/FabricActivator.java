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
