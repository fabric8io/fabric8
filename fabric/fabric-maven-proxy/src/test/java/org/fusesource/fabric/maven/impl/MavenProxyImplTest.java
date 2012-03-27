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

package org.fusesource.fabric.maven.impl;

import java.io.File;
import java.io.IOException;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class MavenProxyImplTest {

    private static final int PORT = Integer.parseInt(System.getProperty("proxyPort"));
    private static final String REMOTE_REPOS = "http://repo1.maven.org/maven2,http://repo.fusesource.com/nexus/content/groups/public,http://repo.fusesource.com/nexus/content/groups/ea";
    private static final String LOCAL_REPO = "target/testrepo";

    private MavenProxyImpl proxy = new MavenProxyImpl();

    private static final String KARAF_GROUP_ID = "org.apache.karaf";
    private static final String KARAF_ARTIFACT_ID = "apache-karaf";
    private static final String KARAF_VERSION = System.getProperty("karafVersion").trim();
    //private static final String KARAF_VERSION = "2.2.4";
    private static final String KARAF_TYPE = "tar.gz";

    @Before
    public void setUp() throws IOException {
        proxy.setPort(PORT);
        proxy.setRemoteRepositories(REMOTE_REPOS);
        proxy.setLocalRepository(LOCAL_REPO);
        proxy.start();
    }

    @After
    public void tearDown() {
        proxy.stop();
    }

    @Test
    public void testCopyFileToLocalRepo() throws IOException {
        getArtifact(KARAF_GROUP_ID, KARAF_ARTIFACT_ID, KARAF_VERSION, KARAF_TYPE);
        File file = new File(LOCAL_REPO + File.separatorChar + getArtifactPath(KARAF_GROUP_ID, KARAF_ARTIFACT_ID, KARAF_VERSION, KARAF_TYPE));
        assertTrue(file.exists());
    }


    @Test
    public void testLocalRepoFirst() throws IOException, InterruptedException {
        int status;
        status = getArtifact(KARAF_GROUP_ID, KARAF_ARTIFACT_ID, KARAF_VERSION, KARAF_TYPE);
        assertEquals("Expected http status OK", HttpStatus.SC_OK, status);
        File file = new File(LOCAL_REPO + File.separatorChar + getArtifactPath(KARAF_GROUP_ID, KARAF_ARTIFACT_ID, KARAF_VERSION, KARAF_TYPE));
        assertTrue(file.exists());

        proxy.stop();
        proxy.setRemoteRepositories("fake-1,fake-2");

        //Wait for port to close.
        Thread.sleep(2000);

        proxy.start();
        status = getArtifact(KARAF_GROUP_ID, KARAF_ARTIFACT_ID, KARAF_VERSION, KARAF_TYPE);
        assertEquals("Expected http status OK", HttpStatus.SC_OK, status);
        // In order to prove that the file DOES get retrieved from the local repo, let's delete it and see what
        // happens.  This can fail though on windows due to open file handles so lets not treat as an error in
        // that case.
        if (file.delete()) {
        	status = getArtifact(KARAF_GROUP_ID, KARAF_ARTIFACT_ID, KARAF_VERSION, KARAF_TYPE);
        	assertEquals("Expected http status OK", HttpStatus.SC_NOT_FOUND, status);
        }
    }

    protected String getArtifactPath(String groupId, String artifactId, String version, String type) {
        return groupId.replaceAll("\\.", "/") + "/" + artifactId + "/" + version + "/" + artifactId + "-" + version + "." + type;
    }

    protected int getArtifact(String groupId, String artifactId, String version, String type) throws IOException {
        HttpClient httpClient = new HttpClient();
        String path = getArtifactPath(groupId, artifactId, version, type);
        GetMethod getMethod = new GetMethod("http://localhost:" + PORT + "/" + path);
        httpClient.executeMethod(getMethod);
        return getMethod.getStatusCode();
    }
}
