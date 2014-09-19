/*
 * #%L
 * Gravia :: Integration Tests :: Common
 * %%
 * Copyright (C) 2010 - 2014 JBoss by Red Hat
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package io.fabric8.itests.common;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.servlet.Servlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.osgi.StartLevelAware;
import org.jboss.gravia.container.tomcat.WebAppContextListener;
import org.jboss.gravia.itests.support.AnnotatedContextListener;
import org.jboss.gravia.itests.support.ArchiveBuilder;
import org.jboss.gravia.itests.support.HttpRequest;
import org.jboss.gravia.resource.ManifestBuilder;
import org.jboss.gravia.runtime.Module;
import org.jboss.gravia.runtime.ModuleContext;
import org.jboss.gravia.runtime.Runtime;
import org.jboss.gravia.runtime.RuntimeLocator;
import org.jboss.gravia.runtime.RuntimeType;
import org.jboss.gravia.runtime.ServiceLocator;
import org.jboss.gravia.runtime.ServiceReference;
import org.jboss.osgi.metadata.OSGiManifestBuilder;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.osgi.service.http.HttpService;

/**
 * Test the {@link HttpService}
 *
 * @author thomas.diesler@jboss.com
 * @since 28-Nov-2013
 */
@RunWith(Arquillian.class)
public class HttpServiceTest {

    @Deployment
    @StartLevelAware(autostart = true)
    public static Archive<?> deployment() {
        final RuntimeType targetContainer = ArchiveBuilder.getTargetContainer();
        final ArchiveBuilder archive = new ArchiveBuilder("http-service");
        archive.addClasses(AnnotatedContextListener.class, WebAppContextListener.class);
        archive.addClasses(HttpRequest.class);
        archive.setManifest(new Asset() {
            @Override
            public InputStream openStream() {
                if (targetContainer == RuntimeType.KARAF) {
                    OSGiManifestBuilder builder = OSGiManifestBuilder.newInstance();
                    builder.addBundleManifestVersion(2);
                    builder.addBundleSymbolicName("http-service");
                    builder.addBundleVersion("1.0.0");
                    builder.addImportPackages(RuntimeLocator.class, Servlet.class, HttpServlet.class, HttpService.class);
                    builder.addBundleClasspath("WEB-INF/classes");
                    return builder.openStream();
                } else {
                    ManifestBuilder builder = new ManifestBuilder();
                    builder.addIdentityCapability("http-service", "1.0.0");
                    return builder.openStream();
                }
            }
        });
        return archive.getArchive();
    }

    @Test
    public void testServletThroughSystemContext() throws Exception {

        Runtime runtime = RuntimeLocator.getRequiredRuntime();
        Module module = runtime.getModule(getClass().getClassLoader());

        HttpService httpService = ServiceLocator.getRequiredService(HttpService.class);
        String reqspec = "/fabric/httpsrv/servlet?test=module";

        // Verify that the alias is not yet available
        assertNotAvailable(reqspec);

        // Register the test servlet and make a call
        String servletAlias = getRuntimeAwareAlias("/httpsrv/servlet");
        httpService.registerServlet(servletAlias, new HttpServiceServlet(module), null, null);
        Assert.assertEquals("http-service:1.0.0", performCall(reqspec));

        // Unregister the servlet alias
        httpService.unregister(servletAlias);
        assertNotAvailable(reqspec);

        // Verify that the alias is not available any more
        assertNotAvailable(reqspec);
    }

    @Test
    public void testServletThroughModuleContext() throws Exception {

        Runtime runtime = RuntimeLocator.getRequiredRuntime();
        Module module = runtime.getModule(getClass().getClassLoader());

        ModuleContext context = module.getModuleContext();
        ServiceReference<HttpService> sref = context.getServiceReference(HttpService.class);
        HttpService httpService = context.getService(sref);

        String reqspec = "/fabric/httpsrv/servlet?test=module";
        try {
            // Verify that the alias is not yet available
            assertNotAvailable(reqspec);

            // Register the test servlet and make a call
            String servletAlias = getRuntimeAwareAlias("/httpsrv/servlet");
            httpService.registerServlet(servletAlias, new HttpServiceServlet(module), null, null);
            Assert.assertEquals("http-service:1.0.0", performCall(reqspec));

            // Unregister the servlet alias
            httpService.unregister(servletAlias);
            assertNotAvailable(reqspec);

            // Verify that the alias is not available any more
            assertNotAvailable(reqspec);
        } finally {
            context.ungetService(sref);
        }
    }

    private void assertNotAvailable(String reqspec) throws Exception {
        try {
            performCall(reqspec, null, 500, TimeUnit.MILLISECONDS);
            Assert.fail("IOException expected");
        } catch (IOException ex) {
            // expected
        }
    }

    private String getRuntimeAwareAlias(String alias) {
        String context = RuntimeType.getRuntimeType() == RuntimeType.KARAF ? "/fabric" : "";
        return context + alias;
    }

    private String performCall(String path) throws Exception {
        return performCall(path, null, 2, TimeUnit.SECONDS);
    }

    private String performCall(String path, Map<String, String> headers, long timeout, TimeUnit unit) throws Exception {
        String port = RuntimeType.getRuntimeType() == RuntimeType.KARAF ? "8181" : "8080";
        return HttpRequest.get("http://localhost:" + port + path, headers, timeout, unit);
    }

    @SuppressWarnings("serial")
    static final class HttpServiceServlet extends HttpServlet {

        private final Module module;

        // This hides the default ctor and verifies that this instance is used
        HttpServiceServlet(Module module) {
            this.module = module;
        }

        protected void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException {
            PrintWriter out = res.getWriter();
            String type = req.getParameter("test");
            if ("param".equals(type)) {
                String value = req.getParameter("param");
                out.print("Hello: " + value);
            } else if ("init".equals(type)) {
                String key = req.getParameter("init");
                String value = getInitParameter(key);
                out.print(key + "=" + value);
            } else if ("module".equals(type)) {
                out.print(module.getIdentity());
            } else {
                throw new IllegalArgumentException("Invalid 'test' parameter: " + type);
            }
            out.close();
        }
    }
}