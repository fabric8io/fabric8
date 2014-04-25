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
