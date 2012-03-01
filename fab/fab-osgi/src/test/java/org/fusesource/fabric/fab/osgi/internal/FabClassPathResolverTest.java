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
package org.fusesource.fabric.fab.osgi.internal;

import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.fusesource.fabric.fab.DependencyTree;
import org.fusesource.fabric.fab.VersionedDependencyId;
import org.fusesource.fabric.fab.osgi.ServiceConstants;
import org.fusesource.fabric.fab.util.Filter;
import org.junit.Test;
import org.sonatype.aether.RepositoryException;
import org.sonatype.aether.graph.Dependency;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

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
            
            private Map<String, String> properties = new HashMap<String, String>();

            {
                properties.put(ServiceConstants.INSTR_FAB_INSTALL_PROVIDED_BUNDLE_DEPENDENCIES, "true");
            }

            @Override
            public String getManifestProperty(String name) {
                return properties.get(name);
            }
        };
        assertTrue(resolver.isInstallProvidedBundleDependencies());
    }

    /*
     * A mock {@link FabFacade} installation
     */
    private static final class MockFabFacade extends FabFacadeSupport {

        @Override
        public File getJarFile() throws IOException {
            return null;
        }

        @Override
        public Configuration getConfiguration() {
            return null;
        }

        @Override
        public VersionedDependencyId getVersionedDependencyId() throws IOException, XmlPullParserException {
            return null;
        }

        @Override
        public String getProjectDescription() {
            return null;
        }

        @Override
        public DependencyTree collectDependencyTree(boolean offline, Filter<Dependency> excludeDependencyFilter) throws RepositoryException, IOException, XmlPullParserException {
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
