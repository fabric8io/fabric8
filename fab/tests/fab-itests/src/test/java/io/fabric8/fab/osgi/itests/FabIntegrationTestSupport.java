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
package io.fabric8.fab.osgi.itests;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.MavenUtils;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.osgi.service.url.URLStreamHandlerService;
import org.osgi.util.tracker.ServiceTracker;

import static org.ops4j.pax.exam.CoreOptions.*;

@RunWith(PaxExam.class)
public abstract class FabIntegrationTestSupport {

    public static final String CAMEL_VERSION;

    public static final String KARAF_VERSION;

    public static final String VERSION = System.getProperty("project.version");

    public static final String LOCAL_REPOSITORY = System.getProperty("org.ops4j.pax.url.mvn.localRepository");

    public static final String REPOSITORIES = "https://repository.jboss.org/nexus/content/groups/fs-public,"
        + "https://repo.fusesource.com/nexus/content/groups/public,"
        + "http://repo1.maven.org/maven2/,"
        + "https://repo.fusesource.com/nexus/content/repositories/public,"
        + "https://repo.fusesource.com/nexus/content/groups/ea,"
        + "http://repo.fusesource.com/nexus/groups/m2-proxy";

    static {
        String camelVersion;
        String karafVersion;
        try {
            camelVersion = MavenUtils.getArtifactVersion("org.apache.camel", "camel-core");
        } catch (RuntimeException e) {
            camelVersion = System.getProperty("camel.version");
        }
        CAMEL_VERSION = camelVersion;
        try {
            karafVersion = MavenUtils.getArtifactVersion("org.apache.karaf.features", "org.apache.karaf.features.core");
        } catch (RuntimeException e) {
            karafVersion = System.getProperty("karaf.version");
        }
        KARAF_VERSION = karafVersion;
    }

    /**
     * Get the fab: url for a given example
     *
     * @param groupId    the artifact's group id
     * @param artifactId the artifact id
     * @return a fab: url
     */
    public String fab(String groupId, String artifactId) {
        return String.format("fab:mvn:%s/%s/%s", groupId, artifactId, VERSION);
    }

    @Configuration
    public Option[] config() {
        return new Option[] {
            junitBundles(),

            systemProperty("project.version").value(VERSION),
            systemProperty("org.ops4j.pax.url.mvn.localRepository").value(LOCAL_REPOSITORY),
            systemProperty("karaf.etc").value("src/test/resources"),

            // we need the boot delegation to allow the Spring/Blueprint XML parsing with JAXP to succeed
            bootDelegationPackage("com.sun.*"),

            mavenBundle("org.ops4j.pax.logging", "pax-logging-api").versionAsInProject(),
            mavenBundle("org.ops4j.pax.url", "pax-url-aether").versionAsInProject(),

            mavenBundle("org.apache.felix", "org.apache.felix.fileinstall").versionAsInProject(),
            mavenBundle("org.apache.felix", "org.apache.felix.configadmin").versionAsInProject(),
            mavenBundle("org.apache.felix", "org.apache.felix.gogo.runtime").versionAsInProject(),

            mavenBundle("org.apache.karaf.jaas", "org.apache.karaf.jaas.boot").versionAsInProject(),
            mavenBundle("org.apache.karaf.jaas", "org.apache.karaf.jaas.config").versionAsInProject(),
            mavenBundle("org.apache.karaf.jaas", "org.apache.karaf.jaas.modules").versionAsInProject(),

            mavenBundle("org.apache.servicemix.bundles", "org.apache.servicemix.bundles.asm").versionAsInProject(),
            mavenBundle("org.apache.aries", "org.apache.aries.util").versionAsInProject(),
            mavenBundle("org.apache.aries.proxy", "org.apache.aries.proxy").versionAsInProject(),
            mavenBundle("org.apache.aries.blueprint", "org.apache.aries.blueprint.api").versionAsInProject(),
            mavenBundle("org.apache.aries.blueprint", "org.apache.aries.blueprint.core").versionAsInProject(),
            mavenBundle("org.apache.karaf.features", "org.apache.karaf.features.core").versionAsInProject(),
            mavenBundle("org.apache.mina", "mina-core").versionAsInProject(),
            mavenBundle("org.apache.sshd", "sshd-core").versionAsInProject(),
            mavenBundle("org.apache.karaf.shell", "org.apache.karaf.shell.console").versionAsInProject(),
            mavenBundle("org.apache.karaf.shell", "org.apache.karaf.shell.osgi").versionAsInProject(),

            mavenBundle("org.ow2.asm", "asm-all").versionAsInProject(),
            mavenBundle("commons-collections", "commons-collections").versionAsInProject(),
            mavenBundle("org.ops4j.base", "ops4j-base-lang").versionAsInProject(),
            mavenBundle("org.ops4j.base", "ops4j-base-net").versionAsInProject(),
            mavenBundle("org.ops4j.base", "ops4j-base-util-property").versionAsInProject(),
            mavenBundle("org.ops4j.pax.swissbox", "pax-swissbox-optional-jcl").versionAsInProject(),
            mavenBundle("org.ops4j.pax.swissbox", "pax-swissbox-property").versionAsInProject(),

            mavenBundle("commons-io", "commons-io").versionAsInProject(),
            mavenBundle("commons-lang", "commons-lang").versionAsInProject(),

            mavenBundle("org.apache.servicemix.bundles", "org.apache.servicemix.bundles.aopalliance").versionAsInProject(),
            mavenBundle("com.google.inject", "guice").versionAsInProject(),

            mavenBundle("io.fabric8", "common-util").versionAsInProject(),
            mavenBundle("io.fabric8.fab", "fab-osgi").versionAsInProject()
        };
    }

    protected void await(final BundleContext bundleContext, String filter) throws Exception {
        final Filter srvfilter = FrameworkUtil.createFilter(filter);

        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicReference<URLStreamHandlerService> serviceRef = new AtomicReference<URLStreamHandlerService>();
        ServiceTracker<URLStreamHandlerService, URLStreamHandlerService> tracker = new ServiceTracker<URLStreamHandlerService, URLStreamHandlerService>(bundleContext, URLStreamHandlerService.class, null) {
            @Override
            public URLStreamHandlerService addingService(ServiceReference<URLStreamHandlerService> sref) {
                URLStreamHandlerService service = super.addingService(sref);
                if (srvfilter == null || srvfilter.match(sref)) {
                    serviceRef.set(bundleContext.getService(sref));
                    latch.countDown();
                }
                return service;
            }
        };
        tracker.open();
        try {
            if (!latch.await(60000L, TimeUnit.MILLISECONDS)) {
                throw new RuntimeException("Cannot obtain service: " + srvfilter);
            }
            //return serviceRef.get();
        } catch (InterruptedException ex) {
            throw new IllegalStateException();
        } finally {
            tracker.close();
        }
    }

}
