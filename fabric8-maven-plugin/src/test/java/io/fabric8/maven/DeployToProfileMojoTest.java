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
package io.fabric8.maven;

import io.fabric8.deployer.dto.DeployResults;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Server;
import org.jolokia.client.J4pClient;
import org.jolokia.client.exception.J4pException;
import org.jolokia.client.request.J4pExecRequest;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import javax.management.MalformedObjectNameException;
import java.io.File;
import java.io.IOException;

import static io.fabric8.api.FabricConstants.FABRIC_VERSION;
import static io.fabric8.common.util.Base64Encoder.decode;
import static io.fabric8.common.util.Files.writeToFile;
import static io.fabric8.maven.DeployToProfileMojo.PLACEHOLDER_PROJECT_ARTIFACT_ID;
import static io.fabric8.maven.DeployToProfileMojo.PLACEHOLDER_PROJECT_GROUP_ID;
import static io.fabric8.maven.DeployToProfileMojo.PLACEHOLDER_PROJECT_VERSION;
import static java.util.UUID.randomUUID;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class DeployToProfileMojoTest extends Assert {

    // Test subject

    DeployToProfileMojo mojo = new DeployToProfileMojo();

    // Collaborators fixture

    J4pClient jolokiaClient = mock(J4pClient.class, RETURNS_DEEP_STUBS);

    ArgumentCaptor<J4pExecRequest> jolokiaRequest = forClass(J4pExecRequest.class);

    DeployResults deployResults;

    // Data fixtures

    MavenProject project;

    File root = new File("target");

    // Fixtures setup

    @Before
    public void before() {
        mojo.fabricServer = mock(Server.class);

        project = new MavenProject();
        project.setGroupId("io.fabric8");
        project.setArtifactId("artifact");
        project.setVersion(FABRIC_VERSION);
        mojo.project = project;

        deployResults = new DeployResults();
        deployResults.setProfileId("profileId");
        deployResults.setVersionId("versionId");
    }

    // Tests

    @Test
    public void shouldExpandProjectGroupIdPlaceholder() throws IOException, J4pException, MalformedObjectNameException, MojoExecutionException {
        // Given
        File config = new File(root, randomUUID().toString());
        String property = "groupId = " + PLACEHOLDER_PROJECT_GROUP_ID;
        writeToFile(config, property.getBytes());

        // When
        mojo.uploadProfileConfigFile(jolokiaClient, deployResults, root, config);
        String decodedConfig = decodeSentConfig();

        // Then
        String expectedConfig = "groupId = " + project.getGroupId();
        assertEquals(expectedConfig, decodedConfig);
    }

    @Test
    public void shouldExpandProjectArtifactIdPlaceholder() throws IOException, J4pException, MalformedObjectNameException, MojoExecutionException {
        // Given
        File config = new File(root, randomUUID().toString());
        String property = "artifactId = " + PLACEHOLDER_PROJECT_ARTIFACT_ID;
        writeToFile(config, property.getBytes());

        // When
        mojo.uploadProfileConfigFile(jolokiaClient, deployResults, root, config);
        String decodedConfig = decodeSentConfig();

        // Then
        assertEquals("artifactId = artifact", decodedConfig);
    }

    @Test
    public void shouldExpandProjectVersionPlaceholder() throws IOException, J4pException, MalformedObjectNameException, MojoExecutionException {
        // Given
        File config = new File(root, randomUUID().toString());
        String property = "project = " + PLACEHOLDER_PROJECT_VERSION;
        writeToFile(config, property.getBytes());

        // When
        mojo.uploadProfileConfigFile(jolokiaClient, deployResults, root, config);
        String decodedConfig = decodeSentConfig();

        // Then
        assertEquals("project = " + FABRIC_VERSION, decodedConfig);
    }

    @Test
    public void shouldExpandAnyPlaceholder() throws IOException, J4pException, MalformedObjectNameException, MojoExecutionException {
        // Given
        File root = new File("src/test/fabric8");
        File config = new File(root, "pid.properties");

        // When
        mojo.uploadProfileConfigFile(jolokiaClient, deployResults, root, config);
        String decodedConfig = decodeSentConfig();

        // Then
        assertEquals("artifactId = " + "fabric8-maven-plugin", decodedConfig);
    }

    // Helpers

    private String decodeSentConfig() throws J4pException {
        verify(jolokiaClient).execute(jolokiaRequest.capture(), anyString());
        J4pExecRequest capturedRequest = jolokiaRequest.getValue();
        String encodedConfig = (String) capturedRequest.getArguments().get(3);
        return decode(encodedConfig);
    }

}