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
package io.fabric8.portable.runtime.tomcat;

import java.io.IOException;
import java.net.URL;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

/**
 * Wait until fabric bootstrap is complete.
 */
public class FabricBootstrapCompleteListener implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent event) {

        ServletContext servletContext = event.getServletContext();
        FabricTomcatActivator.BoostrapLatch latch = (FabricTomcatActivator.BoostrapLatch) servletContext.getAttribute(FabricTomcatActivator.BoostrapLatch.class.getName());
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
            servletContext.removeAttribute(FabricTomcatActivator.BoostrapLatch.class.getName());
        }

        // Print banner message
        printFabricBanner(servletContext);
    }

    @Override
    public void contextDestroyed(ServletContextEvent event) {
    }

    private void printFabricBanner(ServletContext servletContext) {
        Properties pomProperties = new Properties();
        String resname = "/META-INF/maven/io.fabric8.runtime/fabric-runtime-container-tomcat-webapp/pom.properties";
        try {
            URL pomURL = servletContext.getResource(resname);
            pomProperties.load(pomURL.openStream());
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read pom properties from: " + resname);
        }

        Properties brandingProperties = new Properties();
        resname = "/WEB-INF/branding.properties";
        try {
            URL brandingURL = servletContext.getResource(resname);
            brandingProperties.load(brandingURL.openStream());
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read branding properties from: " + resname);
        }
        String welcome = brandingProperties.getProperty("welcome");
        welcome = welcome.replaceAll("\\$\\{project.version\\}", pomProperties.getProperty("version"));

        System.out.println(welcome);
    }
}
