/**
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.fabric8.maven;

import io.fabric8.deployer.dto.DependencyDTO;
import io.fabric8.deployer.dto.ProjectRequirements;
import io.fabric8.utils.Strings;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.dependency.tree.DependencyNode;
import org.apache.maven.shared.dependency.tree.DependencyTreeBuilder;
import org.apache.maven.shared.dependency.tree.DependencyTreeBuilderException;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import io.fabric8.deployer.ProjectDeployer;
import io.fabric8.deployer.dto.DependencyDTO;
import io.fabric8.deployer.dto.DeployResults;
import io.fabric8.deployer.dto.DtoHelper;
import io.fabric8.deployer.dto.ProjectRequirements;
import io.fabric8.utils.Base64Encoder;
import io.fabric8.utils.Files;
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
import org.apache.maven.settings.io.SettingsWriter;
import org.apache.maven.shared.dependency.tree.DependencyNode;
import org.apache.maven.shared.dependency.tree.DependencyTreeBuilder;
import org.apache.maven.shared.dependency.tree.DependencyTreeBuilderException;
import org.jolokia.client.J4pClient;
import org.jolokia.client.exception.J4pException;
import org.jolokia.client.exception.J4pRemoteException;
import org.jolokia.client.request.J4pExecRequest;
import org.jolokia.client.request.J4pReadRequest;
import org.jolokia.client.request.J4pResponse;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.fabric8.deployer.ProjectDeployer;
import io.fabric8.deployer.dto.DependencyDTO;
import io.fabric8.deployer.dto.DeployResults;
import io.fabric8.deployer.dto.DtoHelper;
import io.fabric8.deployer.dto.ProjectRequirements;
import io.fabric8.utils.Base64Encoder;
import io.fabric8.utils.Files;
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
import org.apache.maven.settings.io.SettingsWriter;
import org.apache.maven.shared.dependency.tree.DependencyNode;
import org.apache.maven.shared.dependency.tree.DependencyTreeBuilder;
import org.apache.maven.shared.dependency.tree.DependencyTreeBuilderException;
import org.jolokia.client.J4pClient;
import org.jolokia.client.exception.J4pException;
import org.jolokia.client.exception.J4pRemoteException;
import org.jolokia.client.request.J4pExecRequest;
import org.jolokia.client.request.J4pReadRequest;
import org.jolokia.client.request.J4pResponse;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Abstract base class for Profile based mojos
 */
public abstract class AbstractProfileMojo extends AbstractMojo {
    /**
     * The folder used for defining project specific files
     */
    @Parameter(property = "profileConfigDir", defaultValue = "${basedir}/src/main/fabric8")
    protected File profileConfigDir;

    @Component
    protected MavenProject project;

    @Component
    protected ArtifactCollector artifactCollector;

    @Component
    protected ArtifactFactory artifactFactory;

    @Component
    protected DependencyTreeBuilder dependencyTreeBuilder;

    @Component
    protected ArtifactMetadataSource metadataSource;

    @Parameter(property = "localRepository", readonly = true, required = true)
    protected ArtifactRepository localRepository;

    @Parameter(property = "project.remoteArtifactRepositories")
    protected List<?> remoteRepositories;

    /**
     * The scope to filter by when resolving the dependency tree
     */
    @Parameter(property = "fabric8.scope", defaultValue = "compile")
    private String scope;

    /**
     * The profile ID to deploy to. If not specified then it defaults to the groupId-artifactId of the project
     */
    @Parameter(property = "fabric8.profile")
    private String profile;

    /**
     * The profile version to deploy to. If not specified then the current latest version is used.
     */
    @Parameter(property = "fabric8.version")
    private String version;

    /**
     * The space separated list of parent profile IDs to use for the profile
     */
    @Parameter(property = "fabric8.parentProfiles", defaultValue = "karaf")
    private String parentProfiles;

    /**
     * The space separated list of bundle URLs (in addition to the project artifact) which should be added to the profile
     */
    @Parameter(property = "fabric8.bundles")
    private String bundles;

    /**
     * The space separated list of features to be added to the profile
     */
    @Parameter(property = "fabric8.features")
    private String features;

    /**
     * The space separated list of feature repository URLs to be added to the profile
     */
    @Parameter(property = "fabric8.featureRepos")
    private String featureRepos;

    protected static boolean isFile(File file) {
        return file != null && file.exists() && file.isFile();
    }

    protected static List<String> parameterToStringList(String parameterValue) {
        List<String> answer = new ArrayList<String>();
        if (Strings.isNotBlank(parameterValue)) {
            String[] split = parameterValue.split("\\s");
            if (split != null) {
                for (String text : split) {
                    if (Strings.isNotBlank(text)) {
                        answer.add(text);
                    }
                }
            }
        }
        return answer;
    }

    protected String readInput(String prompt) {
        while (true) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
            System.out.print(prompt);
            try {
                String line = reader.readLine();
                if (line != null && Strings.isNotBlank(line)) {
                    return line;
                }
            } catch (IOException e) {
                getLog().warn("Failed to read input: " + e, e);
            }
        }
    }

    protected void configureRequirements(ProjectRequirements requirements) {
        if (Strings.isNotBlank(profile)) {
            requirements.setProfileId(profile);
        } else {
            requirements.setProfileId(project.getGroupId() + "-" + project.getArtifact());
        }
        if (Strings.isNotBlank(version)) {
            requirements.setVersion(version);
        }
        List<String> bundleList = parameterToStringList(bundles);
        List<String> profileParentList = parameterToStringList(parentProfiles);
        List<String> featureList = parameterToStringList(features);
        List<String> featureReposList = parameterToStringList(featureRepos);
        requirements.setParentProfiles(profileParentList);
        requirements.setBundles(bundleList);
        requirements.setFeatures(featureList);
        requirements.setFeatureRepositories(featureReposList);
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
