/*
 * Copyright (C) 2011, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.fabric.fab;

import org.fusesource.fabric.fab.util.Filter;
import org.fusesource.fabric.fab.util.Filters;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonatype.aether.artifact.Artifact;
import org.sonatype.aether.util.artifact.DefaultArtifact;

import java.io.File;
import java.net.MalformedURLException;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class MavenResolverTest extends DependencyTestSupport {
    private static final transient Logger LOG = LoggerFactory.getLogger(MavenResolverTest.class);

    protected boolean testJarFile = true;

    protected String groupId = "org.apache.camel";
    protected String artifactId = "camel-core";
    protected String version = "2.7.0";
    protected String extension = "jar";
    protected String classifier = "";

    @Test
    public void testArchetypeResolve() throws Exception {
        Artifact artifact = new DefaultArtifact(groupId, artifactId, "jar", version);
        File file = mavenResolver.resolveFile(artifact);
        assertNotNull("File null for " + artifact, file);
        assertTrue("File should exist for " + artifact, file.exists());
    }

    @Test
    public void testResolveDependenciesByGroupAndArtifactId() throws Exception {
        LOG.info("Resolving " + groupId + "/" + artifactId + "/" + version);
        DependencyTreeResult result = mavenResolver.collectDependencies(groupId, artifactId, version, extension, classifier, false, DependencyFilters.testScopeOrOptionalFilter);
        assertNotNull("result", result);

        DependencyTree tree = result.getTree();
        assertNotNull("tree", result);

        List<DependencyTree> children = tree.getChildren();
        for (DependencyTree child : children) {
            LOG.info("Dependency: " + child + " optional: " + child.isOptional() + " scope: " + child.getScope());
            if (testJarFile) {
                File jarFile = child.getJarFile();
                assertNotNull("jar file should not be null!", jarFile);
                assertTrue("jar file for " + child + " should exist! " + jarFile, jarFile.exists());
            }
        }
        
        assertTrue("Should have child dependencies!", children.size() > 0);
    }

}
