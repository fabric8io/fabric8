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
package io.fabric8.maven.impl;

import java.io.File;
import java.util.regex.Matcher;

import io.fabric8.api.RuntimeProperties;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.sonatype.aether.repository.RemoteRepository;
import static io.fabric8.maven.impl.MavenProxyServletSupport.*;
import static org.junit.Assert.*;

public class MavenProxyServletSupportTest {

    private RuntimeProperties runtimeProperties;
    private MavenProxyServletSupport servlet;

    @Before
    public void setUp() {
        runtimeProperties = EasyMock.createMock(RuntimeProperties.class);
        servlet = new MavenDownloadProxyServlet(runtimeProperties, null, null, false, null,null,null,null,0,null, null, null);
    }

    @After
    public void tearDown() {

    }

    @Test
    public void testCreateSimpleRepo() {
        String plainUrl = "http://some.repo.url/somepath";
        RemoteRepository repository = createRemoteRepository(plainUrl);
        assertNotNull(repository);
        assertNotNull(repository.getId());
        assertNull(repository.getAuthentication());
    }

    @Test
    public void testCreateRepWithCredentials() {
        String plainUrl = "http://user:password@some.repo.url/somepath";
        RemoteRepository repository = createRemoteRepository(plainUrl);
        assertNotNull(repository);
        assertNotNull(repository.getId());
        assertNotNull(repository.getAuthentication());
        assertEquals("user", repository.getAuthentication().getUsername());
        assertEquals("password", repository.getAuthentication().getPassword());
        assertEquals("http://some.repo.url/somepath", repository.getUrl());
    }

    @Test
    public void testMetadataRegex() {
        Matcher m = MavenProxyServletSupport.ARTIFACT_METADATA_URL_REGEX.matcher("groupId/artifactId/version/maven-metadata.xml");
        assertTrue(m.matches());
        assertEquals("maven-metadata.xml", m.group(4));

        m = MavenProxyServletSupport.ARTIFACT_METADATA_URL_REGEX.matcher("groupId/artifactId/version/maven-metadata-local.xml");
        assertTrue(m.matches());
        assertEquals("maven-metadata-local.xml", m.group(4));
        assertEquals("local", m.group(7));

        m = MavenProxyServletSupport.ARTIFACT_METADATA_URL_REGEX.matcher("groupId/artifactId/version/maven-metadata-rep-1234.xml");
        assertTrue(m.matches());
        assertEquals("maven-metadata-rep-1234.xml", m.group(4));
        assertEquals("rep-1234", m.group(7));

        m = MavenProxyServletSupport.ARTIFACT_METADATA_URL_REGEX.matcher("groupId/artifactId/version/maven-metadata.xml.md5");
        assertTrue(m.matches());
        assertEquals("maven-metadata.xml", m.group(4));
    }

    @Test
    public void testRepoRegex() {
        Matcher m = MavenProxyServletSupport.REPOSITORY_ID_REGEX.matcher("repo1.maven.org/maven2@id=central");
        assertTrue(m.matches());
        assertEquals("central", m.group(2));

        m = MavenProxyServletSupport.REPOSITORY_ID_REGEX.matcher("https://repo.fusesource.com/nexus/content/repositories/releases@id=fusereleases");
        assertTrue(m.matches());
        assertEquals("fusereleases", m.group(2));

        m = MavenProxyServletSupport.REPOSITORY_ID_REGEX.matcher("repo1.maven.org/maven2@snapshots@id=central");
        assertTrue(m.matches());
        assertEquals("central", m.group(2));

        m = MavenProxyServletSupport.REPOSITORY_ID_REGEX.matcher("repo1.maven.org/maven2@id=central@snapshots");
        assertTrue(m.matches());
        assertEquals("central", m.group(2));

        m = MavenProxyServletSupport.REPOSITORY_ID_REGEX.matcher("repo1.maven.org/maven2@noreleases@id=central@snapshots");
        assertTrue(m.matches());
        assertEquals("central", m.group(2));
    }

    @Test(expected = InvalidMavenArtifactRequest.class)
    public void testConvertNullPath() throws InvalidMavenArtifactRequest {
        servlet.convertToMavenUrl(null);
    }

    @Test
    public void testConvertNormalPath() throws InvalidMavenArtifactRequest {
        assertEquals("groupId:artifactId:extension:version",servlet.convertToMavenUrl("groupId/artifactId/version/artifactId-version.extension"));
        assertEquals("group.id:artifactId:extension:version",servlet.convertToMavenUrl("group/id/artifactId/version/artifactId-version.extension"));
        assertEquals("group.id:artifact.id:extension:version",servlet.convertToMavenUrl("group/id/artifact.id/version/artifact.id-version.extension"));

        assertEquals("group-id:artifactId:extension:version",servlet.convertToMavenUrl("group-id/artifactId/version/artifactId-version.extension"));
        assertEquals("group-id:artifact-id:extension:version",servlet.convertToMavenUrl("group-id/artifact-id/version/artifact-id-version.extension"));
        assertEquals("group-id:my-artifact-id:extension:version",servlet.convertToMavenUrl("group-id/my-artifact-id/version/my-artifact-id-version.extension"));

        //Some real cases
        assertEquals("org.apache.camel.karaf:apache-camel:jar:LATEST",servlet.convertToMavenUrl("org/apache/camel/karaf/apache-camel/LATEST/apache-camel-LATEST.jar"));
        assertEquals("org.apache.cxf.karaf:apache-cxf:jar:LATEST",servlet.convertToMavenUrl("org/apache/cxf/karaf/apache-cxf/LATEST/apache-cxf-LATEST.jar"));
        assertEquals("io.fabric8:fabric8-karaf:jar:LATEST",servlet.convertToMavenUrl("io/fabric8/fabric8-karaf/LATEST/fabric8-karaf-LATEST.jar"));

        //Try extensions with a dot
        assertEquals("io.fabric8:fabric8-karaf:zip:LATEST",servlet.convertToMavenUrl("io/fabric8/fabric8-karaf/LATEST/fabric8-karaf-LATEST.zip"));
    }

    @Test
    public void testConvertNormalPathWithClassifier() throws InvalidMavenArtifactRequest {
        assertEquals("groupId:artifactId:extension:classifier:version",servlet.convertToMavenUrl("groupId/artifactId/version/artifactId-version-classifier.extension"));
        assertEquals("group.id:artifactId:extension:classifier:version",servlet.convertToMavenUrl("group/id/artifactId/version/artifactId-version-classifier.extension"));
        assertEquals("group.id:artifact.id:extension:classifier:version",servlet.convertToMavenUrl("group/id/artifact.id/version/artifact.id-version-classifier.extension"));

        assertEquals("group.id:artifact.id:extension.sha1:classifier:version",servlet.convertToMavenUrl("group/id/artifact.id/version/artifact.id-version-classifier.extension.sha1"));
        assertEquals("group.id:artifact.id:extension.md5:classifier:version",servlet.convertToMavenUrl("group/id/artifact.id/version/artifact.id-version-classifier.extension.md5"));

        assertEquals("group-id:artifactId:extension:classifier:version",servlet.convertToMavenUrl("group-id/artifactId/version/artifactId-version-classifier.extension"));
        assertEquals("group-id:artifact-id:extension:classifier:version",servlet.convertToMavenUrl("group-id/artifact-id/version/artifact-id-version-classifier.extension"));
        assertEquals("group-id:my-artifact-id:extension:classifier:version",servlet.convertToMavenUrl("group-id/my-artifact-id/version/my-artifact-id-version-classifier.extension"));

        //Some real cases
        assertEquals("org.apache.camel.karaf:apache-camel:xml:features:LATEST",servlet.convertToMavenUrl("org/apache/camel/karaf/apache-camel/LATEST/apache-camel-LATEST-features.xml"));
        assertEquals("org.apache.cxf.karaf:apache-cxf:xml:features:LATEST",servlet.convertToMavenUrl("org/apache/cxf/karaf/apache-cxf/LATEST/apache-cxf-LATEST-features.xml"));
        assertEquals("io.fabric8:fabric8-karaf:xml:features:LATEST",servlet.convertToMavenUrl("io/fabric8/fabric8-karaf/LATEST/fabric8-karaf-LATEST-features.xml"));
        assertEquals("io.fabric8:fabric8-karaf:xml:features:7-1-x-fuse-01",servlet.convertToMavenUrl("io/fabric8/fabric8-karaf/7-1-x-fuse-01/fabric8-karaf-7-1-x-fuse-01-features.xml"));

        //Try extensions with a dot
        assertEquals("io.fabric8:fabric8-karaf:zip:distro:LATEST",servlet.convertToMavenUrl("io/fabric8/fabric8-karaf/LATEST/fabric8-karaf-LATEST-distro.zip"));
    }

    @Test
    public void testStartServlet() throws Exception {
        String old = System.getProperty("karaf.data");
        System.setProperty("karaf.data", new File("target").getCanonicalPath());
        try {
            MavenDownloadProxyServlet servlet = new MavenDownloadProxyServlet(runtimeProperties, System.getProperty("java.io.tmpdir"), null, false, null,null,null,null,0,null, null, null);
            servlet.start();
        } finally {
            if (old != null) {
                System.setProperty("karaf.data", old);
            }
        }
    }

}
