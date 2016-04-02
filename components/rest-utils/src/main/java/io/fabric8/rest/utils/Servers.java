/**
 *  Copyright 2005-2016 Red Hat, Inc.
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
package io.fabric8.rest.utils;

import io.fabric8.cxf.endpoint.ManagedApi;
import io.fabric8.utils.Function;
import io.fabric8.utils.Strings;
import io.fabric8.utils.Systems;
import org.apache.cxf.cdi.CXFCdiServlet;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.jboss.weld.environment.servlet.BeanManagerResourceBindingListener;
import org.jboss.weld.environment.servlet.Listener;

import javax.servlet.DispatcherType;
import java.util.EnumSet;

public class Servers {

    // TODO: Move this to fabric8-forge

    protected static final String DEFAULT_PORT = "8588";

    public static Server startServer(String appName) throws Exception {
        return startServer(appName, null, DEFAULT_PORT);
    }

    public static Server startServer(String appName, Function<ServletContextHandler, Void> contextCallback) throws Exception {
        return startServer(appName, contextCallback, DEFAULT_PORT);
    }

    public static Server startServer(String appName, String defaultPort) throws Exception {
        return startServer(appName, null, defaultPort);
    }

    public static Server startServer(String appName, Function<ServletContextHandler, Void> contextCallback, String defaultPort) throws Exception {
        String port = Systems.getEnvVarOrSystemProperty("HTTP_PORT", "HTTP_PORT", defaultPort);
        Integer num = Integer.parseInt(port);
        String service = Systems.getEnvVarOrSystemProperty("WEB_CONTEXT_PATH", "WEB_CONTEXT_PATH", "");

        String servicesPath = "cxf/servicesList";
        String servletContextPath = "/" + service;
        ManagedApi.setSingletonCxfServletContext(servletContextPath);

        String url = "http://localhost:" + port + servletContextPath;
        if (!url.endsWith("/")) {
            url += "/";
        }

        System.out.println();
        System.out.println("-------------------------------------------------------------");
        System.out.println(appName + " is now running at: " + url);
        System.out.println("-------------------------------------------------------------");
        System.out.println();

        final Server server = new Server(num);

        // Register and map the dispatcher servlet
        final ServletHolder servletHolder = new ServletHolder(new CXFCdiServlet());

        // change default service list URI
        servletHolder.setInitParameter("service-list-path", "/" + servicesPath);

        final ServletContextHandler context = new ServletContextHandler();
        context.setContextPath("/");
        context.addEventListener(new Listener());
        context.addEventListener(new BeanManagerResourceBindingListener());
        String servletPath = "/*";
        if (Strings.isNotBlank(service)) {
            servletPath = servletContextPath + "/*";
        }
        context.addServlet(servletHolder, servletPath);
        server.setHandler(context);

        EnumSet<DispatcherType> dispatches = EnumSet.allOf(DispatcherType.class);
        context.addFilter(RestCorsFilter.class, "/*", dispatches);

        if (contextCallback != null) {
            contextCallback.apply(context);
        }
        server.start();
        return server;
    }

}
