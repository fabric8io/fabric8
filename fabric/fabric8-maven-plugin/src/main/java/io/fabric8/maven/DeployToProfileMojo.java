package io.fabric8.maven;

import io.fabric8.deployer.dto.DependencyDTO;
import io.fabric8.deployer.dto.DtoHelper;
import io.fabric8.deployer.dto.ProjectRequirements;
import io.fabric8.utils.Strings;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.deployer.ArtifactDeployer;
import org.apache.maven.artifact.deployer.ArtifactDeploymentException;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.metadata.ArtifactMetadata;
import org.apache.maven.artifact.metadata.ArtifactMetadataSource;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.DefaultArtifactRepository;
import org.apache.maven.artifact.repository.layout.DefaultRepositoryLayout;
import org.apache.maven.artifact.resolver.ArtifactCollector;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.artifact.resolver.filter.ScopeArtifactFilter;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Execute;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.artifact.ProjectArtifactMetadata;
import org.apache.maven.settings.Server;
import org.apache.maven.settings.Settings;
import org.apache.maven.shared.dependency.tree.DependencyNode;
import org.apache.maven.shared.dependency.tree.DependencyTreeBuilder;
import org.apache.maven.shared.dependency.tree.DependencyTreeBuilderException;
import org.jolokia.client.J4pClient;
import org.jolokia.client.exception.J4pException;
import org.jolokia.client.request.J4pExecRequest;
import org.jolokia.client.request.J4pReadRequest;
import org.jolokia.client.request.J4pResponse;

import javax.management.MalformedObjectNameException;
import java.io.File;
import java.util.List;

/**
 * Generates the dependency configuration for the current project so we can HTTP POST the JSON into the fabric8 profile
 */
@Mojo(name = "deploy", defaultPhase = LifecyclePhase.INSTALL, requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME)
@Execute(phase = LifecyclePhase.INSTALL)
public class DeployToProfileMojo extends AbstractMojo {

    @Component
    MavenProject project;

    @Component
    Settings mavenSettings;

    @Component
    ArtifactCollector artifactCollector;

    @Component
    ArtifactResolver resolver;

    @Component
    ArtifactFactory artifactFactory;

    @Component
    DependencyTreeBuilder dependencyTreeBuilder;

    @Component
    ArtifactDeployer deployer;

    /**
     * The scope to filter by when resolving the dependency tree
     */
    @Parameter(property = "scope", defaultValue = "compile")
    private String scope;

    /**
     * The server ID in ~/.m2/settings/xml used for the username and password to login to
     * both the fabric8 maven repository and the jolokia REST API
     */
    @Parameter(property = "fabricServerId", defaultValue = "fabric8.upload.repo")
    private String fabricServerId;

    /**
     * The URL for accessing jolokia on the fabric.
     */
    @Parameter(property = "jolokiaUrl", defaultValue = "http://localhost:8181/jolokia")
    private String jolokiaUrl;

    /**
     * The profile ID to deploy to. If not specified then it defaults to the groupId-artifactId of the project
     */
    @Parameter(property = "profile")
    private String profile;

    /**
     * The profile version to deploy to. If not specified then the current latest version is used.
     */
    @Parameter(property = "version")
    private String version;

    /**
     * Whether or not we should upload the deployment unit to the fabric maven repository.
     */
    @Parameter(property = "upload", defaultValue = "true")
    private boolean upload;

    @Component
    ArtifactMetadataSource metadataSource;

    @Parameter(property = "localRepository", readonly = true, required = true)
    ArtifactRepository localRepository;

    @Parameter(property = "project.remoteArtifactRepositories")
    List<?> remoteRepositories;

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
            DependencyDTO rootDependency = loadRootDependency();

            ProjectRequirements requirements = new ProjectRequirements();
            requirements.setRootDependency(rootDependency);
            configureRequirements(requirements);


            fabricServer = mavenSettings.getServer(fabricServerId);
            if (fabricServer == null) {
                throw new MojoExecutionException("No <server> element can be found in ~/.m2/settings.xml for the server <id>" + fabricServerId + "</id> so we cannot connect to fabric8!\n\n" +
                        "Please add the following to your ~/.m2/settings.xml file (using the correct user/password values):\n\n" +
                        "<servers>\n" +
                        "  <server>\n" +
                        "    <id>" + fabricServerId + "</id>\n" +
                        "    <username>admin</username>\n" +
                        "    <password>admin</password>\n" +
                        "  </server>\n" +
                        "</servers>\n");
            }

            // now lets invoke the mbean
            J4pClient client = createJolokiaClient();

            if (upload) {
                uploadDeploymentUnit(client);
            } else {
                getLog().info("Uploading to the fabric8 maven repository is disabled");
            }

            uploadRequirements(client, requirements);
        } catch (Exception e) {
            throw new MojoExecutionException("Error executing", e);
        }
    }

    protected void uploadDeploymentUnit(J4pClient client) throws Exception {
        String uri = getMavenUploadUri(client);

        // lets resolve the artifact to make sure we get a local file
        Artifact artifact = project.getArtifact();
        resolver.resolve(artifact, remoteRepositories, localRepository);

        String packaging = project.getPackaging();
        File pomFile = project.getFile();

        @SuppressWarnings("unchecked")
        List<Artifact> attachedArtifacts = project.getAttachedArtifacts();

        DefaultRepositoryLayout layout = new DefaultRepositoryLayout();
        ArtifactRepository repo = new DefaultArtifactRepository(fabricServerId, uri, layout);

        // Deploy the POM
        boolean isPomArtifact = "pom".equals(packaging);
        if (!isPomArtifact) {
            ArtifactMetadata metadata = new ProjectArtifactMetadata(artifact, pomFile);
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

    protected static boolean isFile(File file) {
        return file != null && file.exists() && file.isFile();
    }

    protected String getMavenUploadUri(J4pClient client) throws MalformedObjectNameException, J4pException, MojoExecutionException {
        J4pResponse<J4pReadRequest> request = client.execute(new J4pReadRequest("io.fabric8:type=Fabric", "MavenRepoUploadURI"));
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
        throw new MojoExecutionException("Could not find the Maven Upload Repository URI");
    }

    protected void uploadRequirements(J4pClient client, ProjectRequirements requirements) throws Exception {
        String json = DtoHelper.getMapper().writeValueAsString(requirements);
        //ObjectName mbeanName = ProjectDeployer.OBJECT_NAME;
        String mbeanName = "io.fabric8:type=ProjectDeployer";
        getLog().info("About to invoke mbean " + mbeanName + " on jolokia URL: " + jolokiaUrl + " with user: " + jolokiaUrl);
        getLog().debug("JSON: " + json);
        J4pExecRequest request = new J4pExecRequest(mbeanName, "deployProjectJson", json);
        J4pResponse<J4pExecRequest> response = client.execute(request, "POST");
        Object value = response.getValue();
        getLog().info("Got result: " + value);
    }

    protected J4pClient createJolokiaClient() throws MojoExecutionException {
        String user = fabricServer.getUsername();
        String password = fabricServer.getPassword();
        if (Strings.isNullOrBlank(user)) {
            throw new MojoExecutionException("No <username> value defined for the server " + fabricServerId + " in your ~/.m2/settings.xml. Please add a value!");
        }
        if (Strings.isNullOrBlank(password)) {
            throw new MojoExecutionException("No <password> value defined for the server " + fabricServerId + " in your ~/.m2/settings.xml. Please add a value!");
        }
        return J4pClient.url(jolokiaUrl).user(user).password(password).build();
    }

    protected void configureRequirements(ProjectRequirements requirements) {
        if (Strings.isNotBlank(profile)) {
            requirements.setProfileId(profile);
        }
        if (Strings.isNotBlank(version)) {
            requirements.setVersion(version);
        }
    }

    protected DependencyDTO loadRootDependency() throws DependencyTreeBuilderException {
        ArtifactFilter artifactFilter = createResolvingArtifactFilter();
        DependencyNode dependencyNode = dependencyTreeBuilder.buildDependencyTree(project, localRepository, artifactFactory, metadataSource, artifactFilter, artifactCollector);
        return buildFrom(dependencyNode);
    }

    private DependencyDTO buildFrom(DependencyNode node) {
        Artifact artifact = node.getArtifact();
        if (artifact != null) {
            DependencyDTO answer = new DependencyDTO();
            answer.setGroupId(artifact.getGroupId());
            answer.setArtifactId(artifact.getArtifactId());
            answer.setVersion(artifact.getVersion());
            answer.setClassifier(artifact.getClassifier());
            answer.setScope(artifact.getScope());
            answer.setType(artifact.getType());
            answer.setOptional(artifact.isOptional());

            List children = node.getChildren();
            for (Object child : children) {
                if (child instanceof DependencyNode) {
                    DependencyNode childNode = (DependencyNode) child;
                    DependencyDTO childDTO = buildFrom(childNode);
                    answer.addChild(childDTO);
                }
            }
            return answer;
        }
        return null;
    }

    protected void walkTree(DependencyNode node, int level) {
        if (node == null) {
            getLog().warn("Null node!");
            return;
        }
        getLog().info(indent(level) + node.getArtifact());
        List children = node.getChildren();
        for (Object child : children) {
            if (child instanceof DependencyNode) {
                walkTree((DependencyNode) child, level + 1);
            } else {
                getLog().warn("Unknown class " + child.getClass());
            }
        }
    }

    protected String indent(int level) {
        StringBuilder builder = new StringBuilder();
        while (level-- > 0) {
            builder.append("    ");
        }
        return builder.toString();
    }


    /**
     * Gets the artifact filter to use when resolving the dependency tree.
     *
     * @return the artifact filter
     */
    private ArtifactFilter createResolvingArtifactFilter() {
        ArtifactFilter filter;
        if (scope != null) {
            getLog().debug("+ Resolving dependency tree for scope '" + scope + "'");
            filter = new ScopeArtifactFilter(scope);
        } else {
            filter = null;
        }
        return filter;
    }

}
