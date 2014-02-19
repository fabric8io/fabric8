package io.fabric8.maven;

import aQute.lib.osgi.About;
import io.fabric8.deployer.ProjectDeployer;
import io.fabric8.deployer.dto.DependencyDTO;
import io.fabric8.deployer.dto.DtoHelper;
import io.fabric8.deployer.dto.ProjectRequirements;
import io.fabric8.utils.Strings;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.metadata.ArtifactMetadataSource;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactCollector;
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
import org.apache.maven.shared.dependency.tree.DependencyNode;
import org.apache.maven.shared.dependency.tree.DependencyTreeBuilder;
import org.apache.maven.shared.dependency.tree.DependencyTreeBuilderException;
import org.jolokia.client.J4pClient;
import org.jolokia.client.request.J4pExecRequest;
import org.jolokia.client.request.J4pResponse;

import javax.management.ObjectName;
import java.util.List;
import java.util.Set;

/**
 * Generates the dependency configuration for the current project so we can HTTP POST the JSON into the fabric8 profile
 */
@Mojo(name = "deploy", defaultPhase = LifecyclePhase.PACKAGE, requiresDependencyResolution = ResolutionScope.RUNTIME)
@Execute(phase = LifecyclePhase.PACKAGE)
public class DeployToProfileMojo extends AbstractMojo {

    @Component
    MavenProject project;

    @Component
    ArtifactCollector artifactCollector;

    @Component
    ArtifactFactory artifactFactory;

    @Component
    DependencyTreeBuilder dependencyTreeBuilder;

    /**
     * The scope to filter by when resolving the dependency tree
     */
    @Parameter(property = "scope", defaultValue = "compile")
    private String scope;

    @Parameter(property = "jolokiaUrl", defaultValue = "http://localhost:8181/jolokia")
    private String jolokiaUrl;

    @Parameter(property = "jolokiaUser", defaultValue = "admin")
    private String jolokiaUser;

    @Parameter(property = "jolokiaPassword", defaultValue = "admin")
    private String jolokiaPassword;

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

    @Component
    ArtifactMetadataSource metadataSource;

    @Parameter(property = "localRepository", readonly = true, required = true)
    ArtifactRepository localRepository;

    @Parameter(property = "project.remoteArtifactRepositories")
    List<?> remoteRepositories;

    @Parameter(readonly = true, property = "plugin.artifacts")
    List<Artifact> pluginDependencies;

    @Parameter(readonly = true, property = "project.dependencyArtifacts")
    Set<Artifact> projectDependencies;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        try {
            DependencyDTO rootDependency = loadRootDependency();

            ProjectRequirements requirements = new ProjectRequirements();
            requirements.setRootDependency(rootDependency);
            configureRequirements(requirements);

            String json = DtoHelper.getMapper().writeValueAsString(requirements);

            // now lets invoke the mbean
            J4pClient client = createJolokiaClient();
            ObjectName mbeanName = ProjectDeployer.OBJECT_NAME;
            getLog().info("About to invoke mbean " + mbeanName + " on jolokia URL: " + jolokiaUrl + " with user: " + jolokiaUrl);
            getLog().debug("JSON: " + json);
            J4pExecRequest request = new J4pExecRequest(mbeanName, "deployProjectJson", json);
            J4pResponse<J4pExecRequest> response = client.execute(request, "POST");
            Object value = response.getValue();
            getLog().info("Got result: " + value);

        } catch (Exception e) {
            throw new MojoExecutionException("Error executing", e);
        }
    }

    protected J4pClient createJolokiaClient() {
        return J4pClient.url(jolokiaUrl).user(jolokiaUser).password(jolokiaPassword).build();
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
