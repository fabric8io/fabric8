/**
 * Copyright (C) FuseSource, Inc.
 * http://fusesource.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.fabric8.fab.osgi.itests;

import io.fabric8.fab.DependencyTree;
import io.fabric8.fab.osgi.FabURLHandler;
import io.fabric8.fab.osgi.internal.Configuration;
import io.fabric8.fab.osgi.internal.FabClassPathResolver;
import io.fabric8.fab.osgi.internal.FabConnection;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 */
public class IntegrationTestSupport {
    private static final transient Logger LOG = LoggerFactory.getLogger(IntegrationTestSupport.class);
    public static final String JAVA_PROTOCOL_HANDLER_PKGS = "java.protocol.handler.pkgs";

    protected DependencyTree doTestFabricBundle(String artifactId) throws Exception {
        String groupId = "io.fabric8.fab.tests";
        return doTestFabricBundle(groupId, artifactId);
    }

    protected DependencyTree doTestFabricBundle(String groupId, String artifactId) throws Exception {
        installMavenUrlHandler();

        Configuration config = Configuration.newInstance();
        BundleContext bundleContext = new StubBundleContext();

        FabURLHandler handler = new FabURLHandler();
        handler.setBundleContext(bundleContext);
        FabConnection connection = handler.openConnection(new URL("fab:mvn:" + groupId + "/" + artifactId));
        FabClassPathResolver resolve = connection.resolve();
        DependencyTree rootTree = resolve.getRootTree();
        return rootTree;
    }

    public static void installMavenUrlHandler() {
        // lets add pax-maven-url...
        String separator = "|";
        String value = System.getProperty(JAVA_PROTOCOL_HANDLER_PKGS, "");
        String newPackage = "org.ops4j.pax.url" + separator + "io.fabric8.fab.osgi.url";
        if (value.length() > 0) {
            newPackage += separator;
        }
        value = newPackage + value;
        System.setProperty(JAVA_PROTOCOL_HANDLER_PKGS, value);

        LOG.info("System property " + JAVA_PROTOCOL_HANDLER_PKGS + " =  " + value);
    }

    protected void println(Object value, Throwable e) {
        println(value + ". " + e);
        e.printStackTrace();
    }

    protected void println(Object value) {
        System.out.println("======================== " + value);
    }

    protected DependencyTree assertDependencyMatching(DependencyTree tree, String filterText) {
        DependencyTree dependency = tree.findDependency(filterText);
        assertNotNull("Should have found dependency matching: " + filterText, dependency);
        return dependency;
    }

    protected DependencyTree assertNoDependencyMatching(DependencyTree tree, String filterText) {
        DependencyTree dependency = tree.findDependency(filterText);
        assertTrue("Should not have found dependency matching: " + filterText + " but found: " + dependency, dependency == null);
        return dependency;
    }
}
