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

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import io.fabric8.common.util.Files;
import io.fabric8.common.util.Strings;
import io.fabric8.deployer.ProjectDeployer;
import io.fabric8.deployer.dto.DependencyDTO;
import io.fabric8.deployer.dto.DeployResults;
import io.fabric8.deployer.dto.DtoHelper;
import io.fabric8.deployer.dto.ProjectRequirements;
import io.fabric8.utils.Base64Encoder;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.deployer.ArtifactDeployer;
import org.apache.maven.artifact.deployer.ArtifactDeploymentException;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.DefaultArtifactRepository;
import org.apache.maven.artifact.repository.layout.DefaultRepositoryLayout;
import org.apache.maven.artifact.resolver.ArtifactResolutionRequest;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Execute;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.artifact.ProjectArtifactMetadata;
import org.apache.maven.settings.Server;
import org.apache.maven.settings.Settings;
import org.apache.maven.settings.io.SettingsWriter;
import org.jolokia.client.J4pClient;
import org.jolokia.client.exception.J4pConnectException;
import org.jolokia.client.exception.J4pException;
import org.jolokia.client.exception.J4pRemoteException;
import org.jolokia.client.request.J4pExecRequest;
import org.jolokia.client.request.J4pReadRequest;
import org.jolokia.client.request.J4pResponse;
import org.jolokia.client.request.J4pSearchRequest;
import org.jolokia.client.request.J4pSearchResponse;

/**
 * Generates the dependency configuration for the current project so we can HTTP POST the JSON into the fabric8 profile
 */
@Mojo(name = "deploy", defaultPhase = LifecyclePhase.INSTALL, requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME)
@Execute(phase = LifecyclePhase.INSTALL)
public class DeployToProfileMojo extends AbstractProfileMojo {
    public static String FABRIC_MBEAN = "io.fabric8:type=Fabric";

    @Component
    Settings mavenSettings;

    @Parameter(defaultValue = "${user.home}/.m2/settings.xml")
    private File mavenSettingsFile;

    @Component
    SettingsWriter mavenSettingsWriter;

    @Component
    ArtifactResolver resolver;

    @Component
    ArtifactDeployer deployer;

    /**
     * The server ID in ~/.m2/settings/xml used for the username and password to login to
     * both the fabric8 maven repository and the jolokia REST API
     */
    @Parameter(property = "fabric8.serverId", defaultValue = "fabric8.upload.repo")
    private String serverId;

    /**
     * The URL for accessing jolokia on the fabric.
     */
    @Parameter(property = "fabric8.jolokiaUrl", defaultValue = "http://localhost:8181/jolokia")
    private String jolokiaUrl;

    /**
     * Whether or not we should upload the deployment unit to the fabric maven repository.
     */
    @Parameter(property = "fabric8.upload", defaultValue = "true")
    private boolean upload;

    /**
     * Whether or not we should add the maven deployment unit to the fabric profile
     */
    @Parameter(property = "fabric8.includeArtifact", defaultValue = "true")
    private boolean includeArtifact;

    /**
     * Parameter used to control how many times a failed deployment will be retried before giving up and failing. If a
     * value outside the range 1-10 is specified it will be pulled to the nearest value within the range 1-10.
     */
    @Parameter(property = "retryFailedDeploymentCount", defaultValue = "1")
    private int retryFailedDeploymentCount;

    private Server fabricServer;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        try {
            ProjectRequirements requirements = new ProjectRequirements();
            if (includeArtifact) {
                DependencyDTO rootDependency = loadRootDependency();
                requirements.setRootDependency(rootDependency);
            }
            configureRequirements(requirements);


            fabricServer = mavenSettings.getServer(serverId);
            if (fabricServer == null) {
                boolean create = false;
                if (mavenSettings.isInteractiveMode() && mavenSettingsWriter != null) {
                    System.out.println("Maven settings file: " + mavenSettingsFile.getAbsolutePath());
                    System.out.println();
                    System.out.println();
                    System.out.println("There is no <server> section in your ~/.m2/settings.xml file for the server id: " + serverId);
                    System.out.println();
                    System.out.println("You can enter the username/password now and have the settings.xml updated or you can do this by hand if you prefer.");
                    System.out.println();
                    while (true) {
                        String value = readInput("Would you like to update the settings.xml file now? (y/n): ").toLowerCase();
                        if (value.startsWith("n")) {
                            System.out.println();
                            System.out.println();
                            break;
                        } else if (value.startsWith("y")) {
                            create = true;
                            break;
                        }
                    }
                    if (create) {
                        System.out.println("Please let us know the login details for this server: " + serverId);
                        System.out.println();
                        String userName = readInput("User name: ");
                        String password = readInput("Password: ");
                        System.out.println();
                        fabricServer = new Server();
                        fabricServer.setId(serverId);
                        fabricServer.setUsername(userName);
                        fabricServer.setPassword(password);
                        mavenSettings.addServer(fabricServer);
                        if (mavenSettingsFile.exists()) {
                            int counter = 1;
                            while (true) {
                                File backupFile = new File(mavenSettingsFile.getAbsolutePath() + ".backup-" + counter++ + ".xml");
                                if (!backupFile.exists()) {
                                    System.out.println("Copied original: " + mavenSettingsFile.getAbsolutePath() + " to: " + backupFile.getAbsolutePath());
                                    Files.copy(mavenSettingsFile, backupFile);
                                    break;
                                }
                            }
                        }
                        Map<String, Object> config = new HashMap<String, Object>();
                        mavenSettingsWriter.write(mavenSettingsFile, config, mavenSettings);
                        System.out.println("Updated settings file: " + mavenSettingsFile.getAbsolutePath());
                        System.out.println();
                    }
                }
            }
            if (fabricServer == null) {
                    String message = "No <server> element can be found in ~/.m2/settings.xml for the server <id>" + serverId + "</id> so we cannot connect to fabric8!\n\n" +
                            "Please add the following to your ~/.m2/settings.xml file (using the correct user/password values):\n\n" +
                            "<servers>\n" +
                            "  <server>\n" +
                            "    <id>" + serverId + "</id>\n" +
                            "    <username>admin</username>\n" +
                            "    <password>admin</password>\n" +
                            "  </server>\n" +
                            "</servers>\n";
                    getLog().error(message);
                    throw new MojoExecutionException(message);
            }

            // now lets invoke the mbean
            J4pClient client = createJolokiaClient();

            if (upload && includeArtifact) {
                uploadDeploymentUnit(client);
            } else {
                getLog().info("Uploading to the fabric8 maven repository is disabled");
            }

            DeployResults results = uploadRequirements(client, requirements);
            if (results != null) {
                uploadProfileConfigurations(client, results);
                refreshProfile(client, results);
            }
        } catch (MojoExecutionException e) {
            throw e;
        } catch (Exception e) {
            throw new MojoExecutionException("Error executing", e);
        }
    }

    @SuppressWarnings("unchecked")
    protected void uploadDeploymentUnit(J4pClient client) throws Exception {
        String uri = getMavenUploadUri(client);

        // lets resolve the artifact to make sure we get a local file
        Artifact artifact = project.getArtifact();
        ArtifactResolutionRequest request = new ArtifactResolutionRequest();
        request.setArtifact(artifact);
        request.setRemoteRepositories(remoteRepositories);
        request.setLocalRepository(localRepository);

        resolver.resolve(request);

        String packaging = project.getPackaging();
        File pomFile = project.getFile();

        @SuppressWarnings("unchecked")
        List<Artifact> attachedArtifacts = project.getAttachedArtifacts();

        DefaultRepositoryLayout layout = new DefaultRepositoryLayout();
        ArtifactRepository repo = new DefaultArtifactRepository(serverId, uri, layout);

        // Deploy the POM
        boolean isPomArtifact = "pom".equals(packaging);
        if (!isPomArtifact) {
            ProjectArtifactMetadata metadata = new ProjectArtifactMetadata(artifact, pomFile);
            artifact.addMetadata(metadata);
        }

        try {
            if (isPomArtifact) {
                deploy(pomFile, artifact, repo, localRepository, retryFailedDeploymentCount);
            } else {
                File file = artifact.getFile();

                // lets deploy the pom for the artifact first
                if (isFile(pomFile)) {
                    deploy(pomFile, artifact, repo, localRepository, retryFailedDeploymentCount);
                }
                if (isFile(file)) {
                    deploy(file, artifact, repo, localRepository, retryFailedDeploymentCount);
                } else if (!attachedArtifacts.isEmpty()) {
                    getLog().info("No primary artifact to deploy, deploying attached artifacts instead.");

                    Artifact pomArtifact =
                            artifactFactory.createProjectArtifact(artifact.getGroupId(), artifact.getArtifactId(),
                                    artifact.getBaseVersion());
                    pomArtifact.setFile(pomFile);

                    deploy(pomFile, pomArtifact, repo, localRepository, retryFailedDeploymentCount);

                    // propagate the timestamped version to the main artifact for the attached artifacts to pick it up
                    artifact.setResolvedVersion(pomArtifact.getVersion());
                } else {
                    String message = "The packaging for this project did not assign a file to the build artifact";
                    throw new MojoExecutionException(message);
                }
            }

            for (Artifact attached : attachedArtifacts) {
                deploy(attached.getFile(), attached, repo, localRepository, retryFailedDeploymentCount);
            }
        } catch (ArtifactDeploymentException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }
    }

    protected void deploy(File source, Artifact artifact, ArtifactRepository deploymentRepository,
                          ArtifactRepository localRepository, int retryFailedDeploymentCount)
            throws ArtifactDeploymentException {
        getLog().info("Uploading file " + source);

        int retryFailedDeploymentCounter = Math.max(1, Math.min(10, retryFailedDeploymentCount));
        ArtifactDeploymentException exception = null;
        for (int count = 0; count < retryFailedDeploymentCounter; count++) {
            try {
                if (count > 0) {
                    getLog().info("Retrying deployment attempt " + (count + 1) + " of " + retryFailedDeploymentCounter);
                }
                deployer.deploy(source, artifact, deploymentRepository, localRepository);
                exception = null;
                break;
            } catch (ArtifactDeploymentException e) {
                if (count + 1 < retryFailedDeploymentCounter) {
                    getLog().warn("Encountered issue during deployment: " + e.getLocalizedMessage());
                    getLog().debug(e);
                }
                if (exception == null) {
                    exception = e;
                }
            }
        }
        if (exception != null) {
            throw exception;
        }
    }

    protected String getMavenUploadUri(J4pClient client) throws MalformedObjectNameException, J4pException, MojoExecutionException {
        Exception exception = null;
        try {
            J4pSearchResponse searchResponse = client.execute(new J4pSearchRequest(FABRIC_MBEAN));
            List<String> mbeanNames = searchResponse.getMBeanNames();
            if (mbeanNames == null || mbeanNames.isEmpty()) {
                getLog().warn("No MBean " + FABRIC_MBEAN + " found, are you sure you have created a fabric in this JVM?");
                return null;
            }
            J4pResponse<J4pReadRequest> request = client.execute(new J4pReadRequest(FABRIC_MBEAN, "MavenRepoUploadURI"));
            Object value = request.getValue();
            if (value != null) {
                String uri = value.toString();
                if (uri.startsWith("http")) {
                    return uri;
                } else {
                    getLog().warn("Could not find the Maven upload URI. Got: " + value);
                }
            } else {
                getLog().warn("Could not find the Maven upload URI");
            }
        } catch (J4pConnectException e) {
            String message = "Could not connect to jolokia on " + jolokiaUrl + " using user: " + fabricServer.getUsername() + ".\nAre you sure you are running a fabric8 container?";
            getLog().error(message);
            throw new MojoExecutionException(message, e);
        } catch (J4pRemoteException e) {
            int status = e.getStatus();
            if (status == 401) {
                String message = "Unauthorized to access to: " + jolokiaUrl + " using user: " + fabricServer.getUsername() + ".\nHave you created a Fabric?\nHave you setup your ~/.m2/settings.xml with the correct user and password for server ID: " + serverId + " and do the user/password match the server " + jolokiaUrl + "?";
                getLog().error(message);
                throw new MojoExecutionException(message, e);
            } else {
                exception = e;
            }
        } catch (Exception e) {
            exception = e;
        }
        if (exception != null) {
            getLog().error("Failed to get maven repository URI from " + jolokiaUrl + ". " + exception, exception);
            throw new MojoExecutionException("Could not find the Maven Upload Repository URI");
        } else {
            throw new MojoExecutionException("Could not find the Maven Upload Repository URI");
        }
    }

    protected void uploadProfileConfigurations(J4pClient client, DeployResults results) throws Exception {
        if (profileConfigDir != null && profileConfigDir.exists()) {
            uploadProfileConfigDir(client, results, profileConfigDir, profileConfigDir);
        } else {
            getLog().info("No profile configuration file directory " + profileConfigDir + " is defined in this project; so not importing any other configuration files into the profile.");
        }
    }

    protected void uploadProfileConfigDir(J4pClient client, DeployResults results, File rootDir, File file) throws MojoExecutionException, J4pException, IOException, MalformedObjectNameException {
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files != null) {
                for (File child : files) {
                    uploadProfileConfigDir(client, results, rootDir, child);
                }
            }
        } else if (file.isFile()) {
            uploadProfileConfigFile(client, results, rootDir, file);
        }
    }

    protected void uploadProfileConfigFile(J4pClient client, DeployResults results, File rootDir, File file) throws MojoExecutionException, J4pException, IOException, MalformedObjectNameException {
        String profileId = results.getProfileId();
        String versionId = results.getVersionId();
        if (Strings.isNullOrBlank(profileId)) {
            throw new MojoExecutionException("Cannot upload configuration file " + file + " to profile as the profileId was not returned");
        }
        if (Strings.isNullOrBlank(versionId)) {
            throw new MojoExecutionException("Cannot upload configuration file " + file + " to profile as the versionId was not returned");
        }
        String relativePath = Files.getRelativePath(rootDir, file);
        // the path should use forward slash only as we use forward slashes in fabric profiles
        relativePath = Files.normalizePath(relativePath, '\\', '/');
        String text = Files.toString(file);
        String data = Base64Encoder.encode(text);
        String mbeanName = "io.fabric8:type=Fabric";
        getLog().info("Uploading file " + relativePath + " to invoke mbean " + mbeanName + " on jolokia URL: " + jolokiaUrl + " with user: " + fabricServer.getUsername());
        try {
            J4pExecRequest request = new J4pExecRequest(mbeanName, "setConfigurationFile", versionId, profileId, relativePath, data);
            J4pResponse<J4pExecRequest> response = client.execute(request, "POST");
            Object value = response.getValue();
            getLog().info("Got result: " + value);
        } catch (J4pException e) {
            if (e.getMessage().contains(".InstanceNotFoundException")) {
                throw new MojoExecutionException("Could not find the mbean " + mbeanName + " in the JVM for " + jolokiaUrl + ". Are you sure this JVM is running the Fabric8 console?");
            } else {
                throw e;
            }
        }
    }

    protected DeployResults uploadRequirements(J4pClient client, ProjectRequirements requirements) throws Exception {
        String json = DtoHelper.getMapper().writeValueAsString(requirements);
        ObjectName mbeanName = ProjectDeployer.OBJECT_NAME;
        getLog().info("Updating profile: " + requirements.getProfileId() + " with parent profile(s): " + requirements.getParentProfiles());
        getLog().info("About to invoke mbean " + mbeanName + " on jolokia URL: " + jolokiaUrl + " with user: " + fabricServer.getUsername());
        getLog().debug("JSON: " + json);
        try {
            J4pExecRequest request = new J4pExecRequest(mbeanName, "deployProjectJson", json);
            J4pResponse<J4pExecRequest> response = client.execute(request, "POST");
            Object value = response.getValue();
            getLog().info("Got result: " + value);
            if (value == null) {
                return null;
            } else {
                DeployResults results = DtoHelper.getMapper().reader(DeployResults.class).readValue(value.toString());
                return results;
            }
        } catch (J4pException e) {
            if (e.getMessage().contains(".InstanceNotFoundException")) {
                throw new MojoExecutionException("Could not find the mbean " + mbeanName + " in the JVM for " + jolokiaUrl + ". Are you sure this JVM is running the Fabric8 console?");
            } else {
                throw e;
            }
        }
    }

    protected void refreshProfile(J4pClient client, DeployResults results) throws Exception {
        String profileId = results.getProfileId();
        String versionId = results.getVersionId();
        ObjectName mbeanName = new ObjectName(FABRIC_MBEAN);
        if (!Strings.isNullOrBlank(profileId) && !Strings.isNullOrBlank(versionId)) {
            getLog().info("Performing profile refresh on mbean: " + mbeanName + " version: " + versionId + " profile: " + profileId);
            J4pExecRequest request = new J4pExecRequest(mbeanName, "refreshProfile", versionId, profileId);
            J4pResponse<J4pExecRequest> response = client.execute(request, "POST");
            response.getValue();
        }
    }


    protected J4pClient createJolokiaClient() throws MojoExecutionException {
        String user = fabricServer.getUsername();
        String password = fabricServer.getPassword();
        if (Strings.isNullOrBlank(user)) {
            throw new MojoExecutionException("No <username> value defined for the server " + serverId + " in your ~/.m2/settings.xml. Please add a value!");
        }
        if (Strings.isNullOrBlank(password)) {
            throw new MojoExecutionException("No <password> value defined for the server " + serverId + " in your ~/.m2/settings.xml. Please add a value!");
        }
        return J4pClient.url(jolokiaUrl).user(user).password(password).build();
    }


}
