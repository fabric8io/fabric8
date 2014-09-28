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
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import com.google.common.annotations.VisibleForTesting;
import io.fabric8.common.util.Files;
import io.fabric8.common.util.Strings;
import io.fabric8.deployer.ProjectDeployerImpl;
import io.fabric8.deployer.dto.DependencyDTO;
import io.fabric8.deployer.dto.DeployResults;
import io.fabric8.deployer.dto.DtoHelper;
import io.fabric8.deployer.dto.ProjectRequirements;
import io.fabric8.utils.Base64Encoder;
import io.fabric8.utils.FabricValidations;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.deployer.ArtifactDeployer;
import org.apache.maven.artifact.deployer.ArtifactDeploymentException;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.ArtifactRepositoryPolicy;
import org.apache.maven.artifact.repository.Authentication;
import org.apache.maven.artifact.repository.DefaultArtifactRepository;
import org.apache.maven.artifact.repository.MavenArtifactRepository;
import org.apache.maven.artifact.repository.layout.DefaultRepositoryLayout;
import org.apache.maven.artifact.resolver.ArtifactResolutionRequest;
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

    @VisibleForTesting
    static final String PLACEHOLDER_PROJECT_GROUP_ID = "${project.groupId}";

    @VisibleForTesting
    static final String PLACEHOLDER_PROJECT_ARTIFACT_ID = "${project.artifactId}";

    @VisibleForTesting
    static final String PLACEHOLDER_PROJECT_VERSION = "${project.version}";

    public static String FABRIC_MBEAN = "io.fabric8:type=Fabric";

    @Component
    Settings mavenSettings;

    @Parameter(defaultValue = "${user.home}/.m2/settings.xml")
    private File mavenSettingsFile;

    @Component
    SettingsWriter mavenSettingsWriter;

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
     * Parameter used to control how many times a failed deployment will be retried before giving up and failing. If a
     * value outside the range 1-10 is specified it will be pulled to the nearest value within the range 1-10.
     */
    @Parameter(property = "retryFailedDeploymentCount", defaultValue = "1")
    private int retryFailedDeploymentCount;


    /**
     * Type to use for the project artifact bundle reference
     */
    @Parameter(property = "fabric8.artifactBundleType")
    private String artifactBundleType;

    /**
     * Classifier to use for the project artifact bundle reference
     */
    @Parameter(property = "fabric8.artifactBundleClassifier")
    private String artifactBundleClassifier;

    @VisibleForTesting
    Server fabricServer;

    private boolean customUsernameAndPassword;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (isIgnoreProject()) return;

        try {
            ProjectRequirements requirements = new ProjectRequirements();
            if (isIncludeArtifact()) {
                DependencyDTO rootDependency = loadRootDependency();

                if(artifactBundleType != null) {
                    rootDependency.setType(artifactBundleType);
                }

                if(artifactBundleClassifier != null) {
                    if (artifactBundleType != null) {
                        rootDependency.setClassifier(artifactBundleClassifier);
                    } else {
                        throw new MojoFailureException(
                                "The property artifactBundleClassifier was specified as '" + artifactBundleClassifier
                                        +"' without also specifying artifactBundleType");
                    }
                }

                requirements.setRootDependency(rootDependency);
            }
            configureRequirements(requirements);

            // validate requirements
            if (requirements.getProfileId() != null) {
                // make sure the profile id is a valid name
                FabricValidations.validateProfileName(requirements.getProfileId());
            }

            boolean newUserAdded = false;

            fabricServer = mavenSettings.getServer(serverId);

            // we may have username and password from jolokiaUrl
            String jolokiaUsername = null;
            String jolokiaPassword = null;
            try {
                URL url = new URL(jolokiaUrl);
                String s = url.getUserInfo();
                if (Strings.isNotBlank(s) && s.indexOf(':') > 0) {
                    int idx = s.indexOf(':');
                    jolokiaUsername = s.substring(0, idx);
                    jolokiaPassword = s.substring(idx + 1);
                    customUsernameAndPassword = true;
                }
            } catch (MalformedURLException e) {
                throw new IllegalArgumentException("Option jolokiaUrl is invalid due " + e.getMessage());
            }

            // jolokia url overrides username/password configured in maven settings
            if (jolokiaUsername != null) {
                if (fabricServer == null) {
                    fabricServer = new Server();
                }
                getLog().info("Using username: " + jolokiaUsername + " and password from provided jolokiaUrl option");
                fabricServer.setUsername(jolokiaUsername);
                fabricServer.setPassword(jolokiaPassword);
            }

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
                        String userName = readInput("Username: ");
                        String password = readPassword("Password: ");
                        String password2 = readPassword("Repeat Password: ");
                        while (!password.equals(password2)) {
                            System.out.println("Passwords do not match, please try again.");
                            password = readPassword("Password: ");
                            password2 = readPassword("Repeat Password: ");
                        }
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

                        newUserAdded = true;
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

            if (upload && isIncludeArtifact()) {
                uploadDeploymentUnit(client, newUserAdded);
            } else {
                getLog().info("Uploading to the fabric8 maven repository is disabled");
            }

            DeployResults results = uploadRequirements(client, requirements);
            if (results != null) {
                uploadReadMeFile(client, results);
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
    protected void uploadDeploymentUnit(J4pClient client, boolean newUserAdded) throws Exception {
        String uri = getMavenUploadUri(client);

        // lets resolve the artifact to make sure we get a local file
        Artifact artifact = project.getArtifact();
        ArtifactResolutionRequest request = new ArtifactResolutionRequest();
        request.setArtifact(artifact);
        addNeededRemoteRepository();
        request.setRemoteRepositories(remoteRepositories);
        request.setLocalRepository(localRepository);

        resolver.resolve(request);

        String packaging = project.getPackaging();
        File pomFile = project.getFile();

        @SuppressWarnings("unchecked")
        List<Artifact> attachedArtifacts = project.getAttachedArtifacts();

        DefaultRepositoryLayout layout = new DefaultRepositoryLayout();
        ArtifactRepository repo = new DefaultArtifactRepository(serverId, uri, layout);
        if (newUserAdded) {
            // make sure to set authentication if we just added new user
            repo.setAuthentication(new Authentication(fabricServer.getUsername(), fabricServer.getPassword()));
        }

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

    @SuppressWarnings("unchecked")
    private void addNeededRemoteRepository() {
        // TODO: Remove this code when we use releases from Maven Central
        // included jboss-fs repo which is required until we use an Apache version of Karaf
        boolean found = false;
        if (remoteRepositories != null) {
            for (Object obj : remoteRepositories) {
                if (obj instanceof ArtifactRepository) {
                    ArtifactRepository repo = (ArtifactRepository) obj;
                    if (repo.getUrl().contains("repository.jboss.org/nexus/content/groups/fs-public")) {
                        found = true;
                        getLog().debug("Found existing (" + repo.getId() + ") remote repository: " + repo.getUrl());
                        break;
                    }
                }
            }
        }
        if (!found) {
            ArtifactRepository fsPublic = new MavenArtifactRepository();
            fsPublic.setId("jboss-public-fs");
            fsPublic.setUrl("http://repository.jboss.org/nexus/content/groups/fs-public/");
            fsPublic.setLayout(new DefaultRepositoryLayout());
            fsPublic.setReleaseUpdatePolicy(new ArtifactRepositoryPolicy(true, "never", "warn"));
            fsPublic.setSnapshotUpdatePolicy(new ArtifactRepositoryPolicy(false, "never", "ignore"));
            if (remoteRepositories == null) {
                remoteRepositories = new ArrayList();
            }
            remoteRepositories.add(fsPublic);
            getLog().info("Adding needed remote repository: http://repository.jboss.org/nexus/content/groups/fs-public/");
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
                String message = "Status 401: Unauthorized to access: " + jolokiaUrl + " using user: " + fabricServer.getUsername();
                if (!customUsernameAndPassword) {
                    message += ".\nHave you created a Fabric?\nHave you setup your ~/.m2/settings.xml with the correct user and password for server ID: " + serverId + " and do the user/password match the server " + jolokiaUrl + "?";
                }
                getLog().error(message);
                throw new MojoExecutionException(message, e);
            } else if (status == 404) {
                String message = "Status 404: Resource not found: " + jolokiaUrl + ".\nHave you created a Fabric?";
                getLog().error(message);
                throw new MojoExecutionException(message, e);
            } else {
                exception = e;
            }
        } catch (J4pException e) {
            // it may be an empty response which is like a 404
            boolean is404 = "Could not parse answer: Unexpected token END OF FILE at position 0.".equals(e.getMessage());
            if (is404) {
                String message = "Status 404: Resource not found: " + jolokiaUrl + ".\nHave you created a Fabric?";
                getLog().error(message);
                throw new MojoExecutionException(message, e);
            } else {
                exception = e;
            }
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

    protected void uploadReadMeFile(J4pClient client, DeployResults results) throws Exception {
        File profileConfigReadme = null;
        if (profileConfigDir != null) {
            File[] files = profileConfigDir.listFiles(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    return name.toLowerCase(Locale.ENGLISH).startsWith("readme.");
                }
            });
            if (files != null && files.length == 1) {
                profileConfigReadme = files[0];
            }
        }

        // if we already have a readme file or we do not want to include the root readme then we are done
        if (profileConfigReadme != null || !includeReadMe) {
            return;
        }

        File[] files = project.getBasedir().listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.toLowerCase(Locale.ENGLISH).startsWith("readme.");
            }
        });
        if (files != null && files.length == 1) {
            File rootConfigReadme = files[0];
            uploadProfileConfigFile(client, results, project.getBasedir(), rootConfigReadme);
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

    protected void uploadProfileConfigFile(J4pClient client, DeployResults results, File rootDir, File configFile) throws MojoExecutionException, J4pException, IOException, MalformedObjectNameException {
        String profileId = results.getProfileId();
        String versionId = results.getVersionId();
        if (Strings.isNullOrBlank(profileId)) {
            throw new MojoExecutionException("Cannot upload configuration file " + configFile + " to profile as the profileId was not returned");
        }
        if (Strings.isNullOrBlank(versionId)) {
            throw new MojoExecutionException("Cannot upload configuration file " + configFile + " to profile as the versionId was not returned");
        }
        String relativePath = Files.getRelativePath(rootDir, configFile);
        if (relativePath.startsWith("/")) relativePath = relativePath.substring(1);
        // the path should use forward slash only as we use forward slashes in fabric profiles
        relativePath = Files.normalizePath(relativePath, '\\', '/');
        String configFileContents = loadFilteredConfigFile(configFile);
        if (configFileContents == null) {
            getLog().debug(String.format("Filtered copy of the config file %s not found. Using the original file.", configFile));
            configFileContents = Files.toString(configFile);
        }
        String expandedConfig = expandPlaceholders(configFileContents);
        String data = Base64Encoder.encode(expandedConfig);
        String mbeanName = "io.fabric8:type=Fabric";
        getLog().info("Uploading file " + relativePath + " to invoke mbean " + mbeanName + " on jolokia URL: " + jolokiaUrl + " with user: " + fabricServer.getUsername());
        try {
            J4pExecRequest request = new J4pExecRequest(mbeanName, "setConfigurationFile", versionId, profileId, relativePath, data);
            J4pResponse<J4pExecRequest> response = client.execute(request, "POST");
            Object value = response.getValue();
            if (value != null) {
                getLog().info("Upload returned result: " + value);
            }
        } catch (J4pException e) {
            if (e.getMessage().contains(".InstanceNotFoundException")) {
                throw new MojoExecutionException("Could not find the mbean " + mbeanName + " in the JVM for " + jolokiaUrl + ". Are you sure this JVM is running the Fabric8 console?");
            } else {
                throw e;
            }
        }
    }

    protected String loadFilteredConfigFile(File file) {
        File filteredPidFile = new File("target/classes/" + file.getName());
        try {
            if (filteredPidFile.exists()) {
                return Files.toString(filteredPidFile);
            }
        } catch (IOException e) {
            getLog().warn(String.format("Problems while loading filtered PID file %s. Skipping.", filteredPidFile));
        }
        return null;
    }

    /**
     * Expands placeholders in the uploaded configuration file. Currently <code>${project.groupId}</code>,
     * <code>${project.artifactId}</code> and <code>${project.version}</code> placeholders are supported.
     *
     * @param configToExpand configuration containing placeholders to be expanded.
     * @return configuration with expanded placeholders or unaffected configuration if no placeholders are present
     */
    protected String expandPlaceholders(String configToExpand) {
        getLog().debug("Expanding placeholders in the configuration file: " + configToExpand);
        String expandedConfig = configToExpand.replace("${project.groupId}", project.getGroupId()).
                replace("${project.artifactId}", project.getArtifactId()).
                replace("${project.version}", project.getVersion());
        getLog().debug("Expanded configuration file: " + expandedConfig);
        return expandedConfig;
    }

    protected DeployResults uploadRequirements(J4pClient client, ProjectRequirements requirements) throws Exception {
        String json = DtoHelper.getMapper().writeValueAsString(requirements);
        ObjectName mbeanName = ProjectDeployerImpl.OBJECT_NAME;
        getLog().info("Updating " + (requirements.isAbstractProfile() ? "abstract " : "")
                + "profile: " + requirements.getProfileId()
                + " with parent profile(s): " + requirements.getParentProfiles()
                + (requirements.isUseResolver() ? " using OSGi resolver" : "")
                + (requirements.isLocked() ? " locked" : ""));
        getLog().info("About to invoke mbean " + mbeanName + " on jolokia URL: " + jolokiaUrl + " with user: " + fabricServer.getUsername());
        getLog().debug("JSON: " + json);
        try {
            J4pExecRequest request = new J4pExecRequest(mbeanName, "deployProjectJson", json);
            J4pResponse<J4pExecRequest> response = client.execute(request, "POST");
            Object value = response.getValue();
            if (value == null) {
                return null;
            } else {
                DeployResults answer = DtoHelper.getMapper().reader(DeployResults.class).readValue(value.toString());
                if (answer != null) {
                    String profileUrl = answer.getProfileUrl();
                    if (profileUrl != null) {
                        getLog().info("");
                        getLog().info("Profile page: " + profileUrl);
                        getLog().info("");
                    } else {
                        getLog().info("Result: " + answer);
                    }
                } else {
                    getLog().info("Result: " + value);
                }
                return answer;
            }
        } catch (J4pRemoteException e) {
            if (e.getMessage().contains(".InstanceNotFoundException")) {
                throw new MojoExecutionException("Could not find the mbean " + mbeanName + " in the JVM for " + jolokiaUrl + ". Are you sure this JVM is running the Fabric8 console?");
            } else {
                getLog().error("Failed to invoke mbean " + mbeanName + " on jolokia URL: " + jolokiaUrl + " with user: " + fabricServer.getUsername() + ". Error: " + e.getErrorType());
                getLog().error("Stack: " + e.getRemoteStackTrace());
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
            try {
                J4pExecRequest request = new J4pExecRequest(mbeanName, "refreshProfile", versionId, profileId);
                J4pResponse<J4pExecRequest> response = client.execute(request, "POST");
                response.getValue();
            } catch (J4pRemoteException e) {
                getLog().error("Failed to refresh profile " + profileId + " on mbean " + mbeanName + " on jolokia URL: " + jolokiaUrl + " with user: " + fabricServer.getUsername() + ". Error: " + e.getErrorType());
                getLog().error("Stack: " + e.getRemoteStackTrace());
                throw e;
            }
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
