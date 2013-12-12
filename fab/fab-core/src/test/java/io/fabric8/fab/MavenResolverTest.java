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

package io.fabric8.fab;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonatype.aether.artifact.Artifact;
import org.sonatype.aether.repository.RemoteRepository;
import org.sonatype.aether.util.artifact.DefaultArtifact;

import java.io.File;
import java.util.List;

import static org.junit.Assert.*;

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

    @Test
    public void testGetRemoteRepositories() {
        MavenResolverImpl resolver = new MavenResolverImpl();
        resolver.setRepositories(new String[] { "http://user:password@host1/path/to/repo" , "http://host2/path/to/repo@snapshots"});
        
        List<RemoteRepository> repositories = resolver.getRemoteRepositories();        
        assertEquals(2, repositories.size());
        
        // first repository is http://user:password@somehost/path/to/repo 
        RemoteRepository repo = repositories.get(0);
        assertEquals("host1", repo.getHost());
        assertEquals("user", repo.getAuthentication().getUsername());
        assertEquals("password", repo.getAuthentication().getPassword());

        // second repository is http://host2/path/to/repo@snapshots
        repo = repositories.get(1);
        assertEquals("host2", repo.getHost());
        assertNull(repo.getAuthentication());
        assertTrue("@snapshots enables snapshots", repo.getPolicy(true).isEnabled());
    }

}
