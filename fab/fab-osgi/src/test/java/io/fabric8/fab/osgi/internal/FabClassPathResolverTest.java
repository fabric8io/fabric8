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
package io.fabric8.fab.osgi.internal;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

import io.fabric8.fab.DependencyTree;
import io.fabric8.fab.VersionedDependencyId;
import io.fabric8.fab.osgi.util.FeatureCollector;
import io.fabric8.fab.osgi.util.Services;
import org.fusesource.common.util.Filter;
import org.junit.Test;
import org.sonatype.aether.RepositoryException;
import org.sonatype.aether.graph.Dependency;

import static io.fabric8.fab.osgi.ServiceConstants.INSTR_FAB_INSTALL_PROVIDED_BUNDLE_DEPENDENCIES;
import static io.fabric8.fab.osgi.ServiceConstants.INSTR_FAB_REQUIRE_FEATURE;
import static io.fabric8.fab.osgi.ServiceConstants.INSTR_FAB_REQUIRE_FEATURE_URL;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Test cases for {@link FabClassPathResolver}
 */
public class FabClassPathResolverTest {

    @Test
    public void testMissingHeaderParsing() {
        FabClassPathResolver resolver = new FabClassPathResolver(new MockFabFacade(), null, null) {
            @Override
            public String getManifestProperty(String name) {
                // always returning blank here because that's what the real implementation does for a missing header
                return "";
            }
        };
        assertFalse(resolver.isInstallProvidedBundleDependencies());
    }

    @Test
    public void testAvailableHeaderParsing() {
        FabClassPathResolver resolver = new FabClassPathResolver(new MockFabFacade(), null, null) {

            Map<String, String> properties = Services.createProperties(INSTR_FAB_INSTALL_PROVIDED_BUNDLE_DEPENDENCIES, "true");

            @Override
            public String getManifestProperty(String name) {
                return properties.get(name);
            }
        };
        assertTrue(resolver.isInstallProvidedBundleDependencies());
    }

    @Test
    public void testDefaultValueForProvidedDependency() {
        FabClassPathResolver resolver = new FabClassPathResolver(new MockFabFacade(), null, null) {

            @Override
            public String getManifestProperty(String name) {
                // no value set for the header
                return "";
            }
        };
        resolver.processFabInstructions();
        assertTrue("Apache ActiveMQ dependencies are considered shared by default",
                   resolver.sharedFilterPatterns.contains("org.apache.activemq:*"));
        assertTrue("Apache Camel dependencies are considered shared by default",
                   resolver.sharedFilterPatterns.contains("org.apache.camel:*"));
        assertTrue("Apache CXF dependencies are considered shared by default",
                   resolver.sharedFilterPatterns.contains("org.apache.cxf:*"));
    }

    @Test
    public void testConfigureRequiredFeaturesAndURLS() throws URISyntaxException {
        FabClassPathResolver resolver = new FabClassPathResolver(new FabClassPathResolverTest.MockFabFacade(), null, null) {

            Map<String, String> properties =
                    Services.createProperties(INSTR_FAB_REQUIRE_FEATURE, "karaf-framework camel-blueprint/2.9.0",
                                              INSTR_FAB_REQUIRE_FEATURE_URL, "mvn:com.mycompany/features/1.0/xml/features");

            @Override
            public String getManifestProperty(String name) {
                return properties.get(name);
            }
        };

        resolver.processFabInstructions();

        Collection<String> features = resolver.getInstallFeatures();
        assertEquals(2, features.size());
        assertTrue(features.contains("karaf-framework"));
        assertTrue(features.contains("camel-blueprint/2.9.0"));

        Collection<URI> uris = resolver.getInstallFeatureURLs();
        assertEquals(1, uris.size());
        assertTrue(uris.contains(new URI("mvn:com.mycompany/features/1.0/xml/features")));
    }

    @Test
    public void testAddFeatureCollectorThroughPruningFilters() {
        FabClassPathResolver resolver = new FabClassPathResolver(new MockFabFacade(), null, null);

        MockFeatureCollectorFilter filter = new MockFeatureCollectorFilter();
        resolver.addPruningFilter(filter);

        // now ensure that every feature collected by the filter is listed in the install features
        assertEquals(filter.getCollection().size(), resolver.getInstallFeatures().size());
        for (String element : filter.getCollection()) {
            resolver.getInstallFeatures().contains(element);
        }
    }

    /*
     * Mock Filter for DependencyTree instances that also implements FeatureCollector,
     * similar to e.g. {@link CamelFeaturesFilter}
     */
    private static final class MockFeatureCollectorFilter implements Filter<DependencyTree>, FeatureCollector {

        @Override
        public Collection<String> getCollection() {
            return Arrays.asList(new String[]{"mock_feature_1", "mock_feature_2"});
        }

        @Override
        public boolean matches(DependencyTree dependencyTree) {
            return false;
        }

    }

    /*
     * A mock {@link FabFacade} installation
     */
    public static final class MockFabFacade extends FabFacadeSupport {

        @Override
        public File getJarFile() throws IOException {
            return null;
        }

        @Override
        public ConfigurationImpl getConfiguration() {
            return null;
        }

        @Override
        public VersionedDependencyId getVersionedDependencyId() throws IOException {
            return null;
        }

        @Override
        public String getProjectDescription() {
            return null;
        }

        @Override
        public DependencyTree collectDependencyTree(boolean offline, Filter<Dependency> excludeDependencyFilter) throws RepositoryException, IOException {
            return null;
        }

        @Override
        public String toVersionRange(String version) {
            return null;
        }

        @Override
        public boolean isInstalled(DependencyTree tree) {
            return false;
        }
    }
}
