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
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.MalformedURLException;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class MavenResolverTest extends DependencyTestSupport {
    private static final transient Logger LOG = LoggerFactory.getLogger(MavenResolverTest.class);
    
    protected String groupId = "org.apache.camel";
    protected String artifactId = "camel-core";
    protected String version = "2.7.0";
    protected String extension = "jar";
    protected String classifier = "";

    @Test
    public void testResolveDependenciesByGroupAndArtifactId() throws Exception {
        LOG.info("Resolving " + groupId + "/" + artifactId + "/" + version);
        DependencyTreeResult result = mavenResolver.collectDependencies(groupId, artifactId, version, extension, classifier);
        assertNotNull("result", result);

        DependencyTree tree = result.getTree();
        assertNotNull("tree", result);

        List<DependencyTree> children = tree.getChildren();
        for (DependencyTree child : children) {
            LOG.info("Dependency: " + child + " optional: " + child.isOptional() + " scope: " + child.getScope());
            File jarFile = child.getJarFile();
            assertNotNull("jar file should not be null!", jarFile);
            assertTrue("jar file for " + child + " should exist! " + jarFile, jarFile.exists());
        }
        
        assertTrue("Should have child dependencies!", children.size() > 0);
    }

}
