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
package io.fabric8.fab;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import io.fabric8.common.util.Filter;
import io.fabric8.common.util.Filters;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.junit.Assert;

import static io.fabric8.fab.DependencyTree.newBuilder;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public abstract class DependencyTestSupport {
    protected final transient Logger LOG = Logger.getLogger(getClass().getName());

    protected static SharedClassLoaderRegistry registry = new SharedClassLoaderRegistry();

    DependencyTree clogging11 = newBuilder("commons-logging", "commons-logging-api", "1.1").build();
    DependencyTree clogging104 = newBuilder("commons-logging", "commons-logging-api", "1.04").build();

    DependencyTree commonman = newBuilder("org.fusesource.commonman", "commons-management", "1.0").build();

    DependencyTree camel250_clogging11 = newBuilder("org.apache.camel", "camel-core", "2.5.0", clogging11).build();
    DependencyTree camel250_clogging104 = newBuilder("org.apache.camel", "camel-core", "2.5.0", clogging104).build();

    DependencyTree camel250_clogging_man = newBuilder("org.apache.camel", "camel-core", "2.5.0", clogging11, commonman).build();

    protected MavenResolverImpl mavenResolver = new MavenResolverImpl();

    protected DependencyTreeResult collectDependencies(String pomName) throws Exception {
        URL resource = getClass().getClassLoader().getResource(pomName);
        assertNotNull("Could not find: " + pomName + " on the classpath", resource);
        File rootPom = new File(resource.getPath());

        DependencyTreeResult node = mavenResolver.collectDependencies(rootPom, false);
        if (LOG.isLoggable(Level.FINE)) {
            LOG.fine("File: " + pomName);
            LOG.fine(node.getTreeDescription());
        }

        List<DependencyTree.DuplicateDependency> duplicateDependencies = node.getTree().checkForDuplicateDependencies();
        assertEquals("Should not have duplicates: " + duplicateDependencies, 0, duplicateDependencies.size());
        return node;
    }

    public void assertVersions(DependencyTreeResult node, String groupId, String artifactId, String... expectedVersions) {
        DependencyId id = new DependencyId(groupId, artifactId);
        assertVersions(node, id, expectedVersions);
    }

    public void assertVersions(DependencyTreeResult node, DependencyId id, String[] expectedVersions) {
        List<String> versions = new ArrayList<String>();
        node.addVersions(versions, id);

        List<String> expectedVersionList = Arrays.asList(expectedVersions);

        LOG.info("Found " + id + " has versions: " + versions);

        assertEquals("versions of: " + id, expectedVersionList, versions);
    }


    protected void assertSameClasses(DependencyClassLoader cl1, DependencyClassLoader cl2, String... names) throws ClassNotFoundException {
        for (String name : names) {
            assertSameClass(cl1, cl2, name);
        }
    }

    protected void assertSameClass(DependencyClassLoader cl1, DependencyClassLoader cl2, String name) throws ClassNotFoundException {
        Class<?> class1 = assertLoadClass(cl1, name);
        Class<?> class2 = assertLoadClass(cl2, name);

        LOG.fine("Found class " + class1 + " in " + class1.getClassLoader());
        LOG.fine("Found class " + class2 + " in " + class2.getClassLoader());

        ClassLoader foundClassLoader1 = class1.getClassLoader();
        ClassLoader foundClassLoader2 = class2.getClassLoader();
        assertSame("Should have loaded same class: " + name + " in class loaders " + cl1 + " and " + cl2 + " when found in " + foundClassLoader1 + " and " + foundClassLoader2, class1, class2);
        assertEquals("Should have loaded equal class: " + name + " in class loaders " + cl1 + " and " + cl2, class1, class2);
    }

    protected void assertDifferentClasses(DependencyClassLoader cl1, DependencyClassLoader cl2, String... names) throws ClassNotFoundException {
        for (String name : names) {
            assertDifferentClass(cl1, cl2, name);
        }
    }

    protected void assertDifferentClass(DependencyClassLoader cl1, DependencyClassLoader cl2, String name) throws ClassNotFoundException {
        Class<?> class1 = assertLoadClass(cl1, name);
        Class<?> class2 = assertLoadClass(cl2, name);

        LOG.fine("Found class " + class1 + " in " + class1.getClassLoader());
        LOG.fine("Found class " + class2 + " in " + class2.getClassLoader());

        assertNotSame("Should have loaded different classes for: " + name + " in class loaders " + cl1 + " and " + cl2, class1, class2);
        assertTrue("Should have loaded different classes: " + name + " in class loaders " + cl1 + " and " + cl2, !class1.equals(class2));
    }

    protected DependencyClassLoader getClassLoaderForPom(String pomName) throws Exception {
        Filter<DependencyTree> shareFilter = getShareFilter();
        Filter<DependencyTree> excludeFilter = getExcludeFilter();
        return getClassLoaderForPom(pomName, shareFilter, excludeFilter);
    }

    protected Filter<DependencyTree> getShareFilter() {
        return Filters.<DependencyTree>trueFilter();
    }

    protected Filter<DependencyTree> getExcludeFilter() {
        return DependencyTreeFilters.parseExcludeFilter("", Filters.falseFilter());
    }

    protected DependencyClassLoader getClassLoaderForPom(String pomName, Filter<DependencyTree> shareFilter, Filter<DependencyTree> excludeFilter) throws Exception {
        DependencyTree tree = collectDependencies(pomName).getTree();
        DependencyClassLoader classLoader = registry.getClassLoader(tree, shareFilter, excludeFilter);
        assertNotNull("Could not create a class loader for " + pomName, classLoader);
        return classLoader;
    }

    protected void assertLoadClasses(DependencyClassLoader classLoader, String... names) throws ClassNotFoundException {
        for (String name : names) {
            assertLoadClass(classLoader, name);
        }
    }

    protected Class<?> assertLoadClass(DependencyClassLoader classLoader, String name) throws ClassNotFoundException {
        LOG.fine("Loading class: " + name);
        try {
            return classLoader.loadClass(name);
        } catch (ClassNotFoundException e) {
            LOG.log(Level.SEVERE, "Failed to load class: " + name + " in " + classLoader + ". " + e, e);
            Assert.fail("Could not load: " + name + " in " + classLoader + ". " + e);
            return null;
        }
    }

    protected DependencyTree assertFindDependencyTree(DependencyTreeResult result, String groupId, String artifactId) throws MalformedURLException, ArtifactResolutionException {
        DependencyTree tree = result.getTree();
        DependencyTree answer = tree.findDependency(groupId, artifactId);
        assertNotNull("Should have found a DpendencyTree for " + groupId + ":" + artifactId, answer);
        return answer;
    }
}
