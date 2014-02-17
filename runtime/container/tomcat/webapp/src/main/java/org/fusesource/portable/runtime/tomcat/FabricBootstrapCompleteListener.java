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

import java.io.IOException;
import java.net.URL;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.fusesource.portable.runtime.tomcat.FabricTomcatActivator.BoostrapLatch;

/**
 * Wait until fabric bootstrap is complete.
 *
 * @author thomas.diesler@jboss.com
 * @since 17-Dec-2013
 */
public class FabricBootstrapCompleteListener implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent event) {

        ServletContext servletContext = event.getServletContext();
        BoostrapLatch latch = (BoostrapLatch) servletContext.getAttribute(BoostrapLatch.class.getName());
        try {
            // Wait for the {@link ZooKeeperClusterBootstrap} to come up
            try {
                if (!latch.await(60, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("Cannot obtain ZooKeeperClusterBootstrap");
                }
            } catch (InterruptedException ex) {
                // ignore
            }
        } finally {
            servletContext.removeAttribute(BoostrapLatch.class.getName());
        }

        // Print banner message
        printFabricBanner(servletContext);
    }

    @Override
    public void contextDestroyed(ServletContextEvent event) {
    }

    private void printFabricBanner(ServletContext servletContext) {
        Properties brandingProperties = new Properties();
        String resname = "/WEB-INF/branding.properties";
        try {
            URL brandingURL = servletContext.getResource(resname);
            brandingProperties.load(brandingURL.openStream());
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read branding properties from: " + resname);
        }
        System.out.println(brandingProperties.getProperty("welcome"));
    }
}
