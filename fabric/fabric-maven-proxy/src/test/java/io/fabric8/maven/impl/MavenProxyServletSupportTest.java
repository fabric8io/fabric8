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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.regex.Matcher;

import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

import io.fabric8.api.RuntimeProperties;
import io.fabric8.api.scr.AbstractRuntimeProperties;

import io.fabric8.deployer.ProjectDeployer;
import org.apache.commons.io.FileUtils;
import org.easymock.EasyMock;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.eclipse.aether.repository.RemoteRepository;

import static io.fabric8.maven.impl.MavenProxyServletSupport.*;
import static org.junit.Assert.*;

public class MavenProxyServletSupportTest {

    private RuntimeProperties runtimeProperties;
    private ProjectDeployer projectDeployer;
    private MavenProxyServletSupport servlet;

    @Before
    public void setUp() {
        runtimeProperties = EasyMock.createMock(RuntimeProperties.class);
        projectDeployer = EasyMock.createMock(ProjectDeployer.class);
        servlet = new MavenDownloadProxyServlet(runtimeProperties, null, null, false, null,null,null,null,0,null, null, null, projectDeployer);
    }

    @After
    public void tearDown() {

    }

    @Test
    public void testCreateSimpleRepo() {
        String plainUrl = "http://some.repo.url/somepath";
        RemoteRepository repository = createRemoteRepository(plainUrl).build();
        assertNotNull(repository);
        assertNotNull(repository.getId());
        assertNull(repository.getAuthentication());
    }

    @Test
    public void testCreateRepWithCredentials() {
        String plainUrl = "http://user:password@some.repo.url/somepath";
        RemoteRepository repository = createRemoteRepository(plainUrl).build();
        assertNotNull(repository);
        assertNotNull(repository.getId());
        assertNotNull(repository.getAuthentication());
        // TODO -- FIXME
        //assertEquals("user", repository.getAuthentication().getUsername());
        //assertEquals("password", repository.getAuthentication().getPassword());
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
            MavenDownloadProxyServlet servlet = new MavenDownloadProxyServlet(runtimeProperties, System.getProperty("java.io.tmpdir"), null, false, null,null,null,null,0,null, null, null, projectDeployer);
            servlet.start();
        } finally {
            if (old != null) {
                System.setProperty("karaf.data", old);
            }
        }
    }

    @Test
    @Ignore("[https://jira.codehaus.org/browse/WAGON-416][FABRIC-171] Wait for wagon-http-lightweight fixes")
    public void testDownloadUsingAuthenticatedProxy() throws Exception {
        testDownload(new AbstractHandler() {
            @Override
            public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
                String proxyAuth = request.getHeader("Proxy-Authorization");
                if (proxyAuth == null || proxyAuth.trim().equals("")) {
                    response.setStatus(HttpServletResponse.SC_PROXY_AUTHENTICATION_REQUIRED);
                    response.addHeader("Proxy-Authenticate", "Basic realm=\"Proxy Server\"");
                    baseRequest.setHandled(true);
                } else {
                    response.setStatus(HttpServletResponse.SC_OK);
                    baseRequest.setHandled(true);
                    response.getOutputStream().write(new byte[] { 0x42 });
                    response.getOutputStream().close();
                }
            }
        });
    }

    @Test
    public void testDownloadUsingNonAuthenticatedProxy() throws Exception {
        testDownload(new AbstractHandler() {
            @Override
            public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
                response.setStatus(HttpServletResponse.SC_OK);
                baseRequest.setHandled(true);
                response.getOutputStream().write(new byte[] { 0x42 });
                response.getOutputStream().close();
            }
        });
    }

    private void testDownload(Handler serverHandler) throws Exception {
        final String old = System.getProperty("karaf.data");
        System.setProperty("karaf.data", new File("target").getCanonicalPath());
        FileUtils.deleteDirectory(new File("target/tmp"));

        Server server = new Server(0);
        server.setHandler(serverHandler);
        server.start();

        try {
            int localPort = server.getConnectors()[0].getLocalPort();
            List<String> remoteRepos = Arrays.asList("http://relevant.not/maven2@id=central");
            RuntimeProperties props = new MockRuntimeProperties();
            MavenDownloadProxyServlet servlet = new MavenDownloadProxyServlet(props, "target/tmp", remoteRepos, false, "always", "warn", "http", "localhost", localPort, "fuse", "fuse", null, projectDeployer);

            HttpServletRequest request = EasyMock.createMock(HttpServletRequest.class);
            EasyMock.expect(request.getPathInfo()).andReturn("org.apache.camel/camel-core/2.13.0/camel-core-2.13.0-sources.jar");

            HttpServletResponse response = EasyMock.createMock(HttpServletResponse.class);
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            EasyMock.expect(response.getOutputStream()).andReturn(new ServletOutputStream() {
                @Override
                public void write(int b) throws IOException {
                    baos.write(b);
                }

                @Override
                public void write(byte[] b, int off, int len) throws IOException {
                    baos.write(b, off, len);
                }
            }).anyTimes();
            response.setStatus(EasyMock.anyInt());
            EasyMock.expectLastCall().anyTimes();
            response.setContentLength(EasyMock.anyInt());
            EasyMock.expectLastCall().anyTimes();
            response.setContentType((String) EasyMock.anyObject());
            EasyMock.expectLastCall().anyTimes();
            response.setDateHeader((String) EasyMock.anyObject(), EasyMock.anyLong());
            EasyMock.expectLastCall().anyTimes();
            response.setHeader((String) EasyMock.anyObject(), (String) EasyMock.anyObject());
            EasyMock.expectLastCall().anyTimes();

            EasyMock.replay(request, response);

            servlet.start();
            servlet.doGet(request, response);
            Assert.assertArrayEquals(new byte[] { 0x42 }, baos.toByteArray());

            EasyMock.verify(request, response);
        } finally {
            server.stop();
            if (old != null) {
                System.setProperty("karaf.data", old);
            }
        }
    }

    @Test
    public void testJarUploadFullMvnPath() throws Exception {
        String jarPath = "org.acme/acme-core/1.0/acme-core-1.0.jar";

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        JarOutputStream jas = new JarOutputStream(baos);
        addEntry(jas, "hello.txt", "Hello!".getBytes());
        jas.close();

        byte[] contents = baos.toByteArray();

        testUpload(jarPath, contents, false);
    }

    @Test
    public void testJarUploadWithMvnPom() throws Exception {
        String jarPath = "org.acme/acme-core/1.0/acme-core-1.0.jar";

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        JarOutputStream jas = new JarOutputStream(baos);
        addEntry(jas, "hello.txt", "Hello!".getBytes());
        addPom(jas, "org.acme", "acme-core", "1.0");
        jas.close();

        byte[] contents = baos.toByteArray();

        testUpload(jarPath, contents, false);
    }

    @Test
    public void testJarUploadNoMvnPath() throws Exception {
        String jarPath = "acme-core-1.0.jar";

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        JarOutputStream jas = new JarOutputStream(baos);
        addEntry(jas, "hello.txt", "Hello!".getBytes());
        jas.close();

        byte[] contents = baos.toByteArray();
        testUpload(jarPath, contents, true);
    }

    @Test
    public void testWarUploadFullMvnPath() throws Exception {
        String warPath = "org.acme/acme-ui/1.0/acme-ui-1.0.war";

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        JarOutputStream jas = new JarOutputStream(baos);
        addEntry(jas, "WEB-INF/web.xml", "<web/>".getBytes());
        jas.close();

        byte[] contents = baos.toByteArray();

        testUpload(warPath, contents, false);
    }

    @Test
    public void testWarUploadWithMvnPom() throws Exception {
        String warPath = "org.acme/acme-ui/1.0/acme-ui-1.0.war";

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        JarOutputStream jas = new JarOutputStream(baos);
        addEntry(jas, "WEB-INF/web.xml", "<web/>".getBytes());
        addPom(jas, "org.acme", "acme-ui", "1.0");
        jas.close();

        byte[] contents = baos.toByteArray();

        testUpload(warPath, contents, false);
    }

    @Test
    public void testWarUploadNoMvnPath() throws Exception {
        String warPath = "acme-ui-1.0.war";

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        JarOutputStream jas = new JarOutputStream(baos);
        addEntry(jas, "WEB-INF/web.xml", "<web/>".getBytes());
        jas.close();

        byte[] contents = baos.toByteArray();

        testUpload(warPath, contents, true);
    }

    private static void addEntry(JarOutputStream jas, String name, byte[] content) throws Exception {
        JarEntry entry = new JarEntry(name);
        jas.putNextEntry(entry);
        if (content != null) {
            jas.write(content);
        }
        jas.closeEntry();
    }

    private static void addPom(JarOutputStream jas, String groupId, String artifactId, String version) throws Exception {
        Properties properties = new Properties();
        properties.setProperty("groupId", groupId);
        properties.setProperty("artifactId", artifactId);
        properties.setProperty("version", version);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        properties.store(baos, null);
        addEntry(jas, String.format("META-INF/maven/%s/%s/%s/pom.properties", groupId, artifactId, version), baos.toByteArray());
    }

    private Map<String, String> testUpload(String path, final byte[] contents, boolean hasLocationHeader) throws Exception {
        return testUpload(path, contents, null, hasLocationHeader);
    }

    private Map<String, String> testUpload(String path, final byte[] contents, String location, boolean hasLocationHeader) throws Exception {
        return testUpload(path, contents, location, null, null, hasLocationHeader);
    }

    private Map<String, String> testUpload(String path, final byte[] contents, String location, String profile, String version, boolean hasLocationHeader) throws Exception {
        final String old = System.getProperty("karaf.data");
        System.setProperty("karaf.data", new File("target").getCanonicalPath());
        FileUtils.deleteDirectory(new File("target/tmp"));

        Server server = new Server(0);
        server.setHandler(new AbstractHandler() {
            @Override
            public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
                response.setStatus(HttpServletResponse.SC_NO_CONTENT);
            }
        });
        server.start();

        try {
            int localPort = server.getConnectors()[0].getLocalPort();
            List<String> remoteRepos = Arrays.asList("http://relevant.not/maven2@id=central");
            RuntimeProperties props = new MockRuntimeProperties();
            MavenUploadProxyServlet servlet = new MavenUploadProxyServlet(props, "target/tmp", remoteRepos, false, "always", "warn", "http", "localhost", localPort, "fuse", "fuse", null, projectDeployer);

            HttpServletRequest request = EasyMock.createMock(HttpServletRequest.class);
            EasyMock.expect(request.getPathInfo()).andReturn(path);
            EasyMock.expect(request.getInputStream()).andReturn(new ServletInputStream() {
                private int i;

                @Override
                public int read() throws IOException {
                    if (i >= contents.length) {
                        return -1;
                    }
                    return (contents[i++] & 0xFF);
                }
            });
            EasyMock.expect(request.getHeader("X-Location")).andReturn(location);
            EasyMock.expect(request.getParameter("profile")).andReturn(profile);
            EasyMock.expect(request.getParameter("version")).andReturn(version);

            final Map<String, String> headers = new HashMap<>();

            HttpServletResponse rm = EasyMock.createMock(HttpServletResponse.class);
            HttpServletResponse response = new HttpServletResponseWrapper(rm) {
                @Override
                public void addHeader(String name, String value) {
                    headers.put(name, value);
                }
            };
            response.setStatus(EasyMock.anyInt());
            EasyMock.expectLastCall().anyTimes();
            response.setContentLength(EasyMock.anyInt());
            EasyMock.expectLastCall().anyTimes();
            response.setContentType((String) EasyMock.anyObject());
            EasyMock.expectLastCall().anyTimes();
            response.setDateHeader((String) EasyMock.anyObject(), EasyMock.anyLong());
            EasyMock.expectLastCall().anyTimes();
            response.setHeader((String) EasyMock.anyObject(), (String) EasyMock.anyObject());
            EasyMock.expectLastCall().anyTimes();

            EasyMock.replay(request, rm);

            servlet.start();
            servlet.doPut(request, response);

            EasyMock.verify(request, rm);

            Assert.assertEquals(hasLocationHeader, headers.containsKey("X-Location"));

            return headers;
        } finally {
            server.stop();
            if (old != null) {
                System.setProperty("karaf.data", old);
            }
        }
    }

    /**
     * To satisfy new container-independent source of properties
     */
    private static class MockRuntimeProperties extends AbstractRuntimeProperties {
        
        @Override
        public Path getDataPath() {
            return Paths.get("target/tmp");
        }

        @Override
        protected String getPropertyInternal(String key, String defaultValue) {
            return null;
        }
    }
}
