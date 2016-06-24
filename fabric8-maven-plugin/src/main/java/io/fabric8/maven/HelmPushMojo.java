/*
 * Copyright 2005-2016 Red Hat, Inc.
 *
 * Red Hat licenses this file to you under the Apache License, version
 * 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */
package io.fabric8.maven;

import io.fabric8.utils.Files;
import io.fabric8.utils.Objects;
import io.fabric8.utils.Strings;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.eclipse.jgit.api.CommitCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.PushCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;

import java.io.File;
import java.util.List;

/**
 * Adds any changes to the Helm charts, commits them to the charts git repository and pushes changes
 */
@Mojo(name = "helm-push", defaultPhase = LifecyclePhase.PACKAGE)
public class HelmPushMojo extends HelmMojo {

    /**
     * The commit message for changes to the helm chart repository
     */
    @Parameter(property = "fabric8.helm.commitMessage", defaultValue = "Adding charts for ${project.groupId}/${project.artifactId} version ${project.version}")
    private String commitMessage;

    /**
     * Should we push the git commits to the helm chart repository
     */
    @Parameter(property = "fabric8.helm.push", defaultValue = "true")
    private boolean pushChanges;

    /**
     * The user name used for git commits to the helm chart repository
     */
    @Parameter(property = "fabric8.helm.username")
    private String userName;

    /**
     * The email address used for git commits to the helm chart repository
     */
    @Parameter(property = "fabric8.helm.email")
    private String emailAddress;

    @Parameter(defaultValue = "${reactorProjects}", readonly = true, required = true)
    private List<MavenProject> reactorProjects;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (!isRootReactorBuild()) {
            getLog().info("Not the root reactor build so not committing changes");
            return;
        }

        File outputDir = getHelmRepoFolder();
        if (!Files.isDirectory(outputDir)) {
            throw new MojoExecutionException("No helm repository exists for " + outputDir + ". Did you run `mvn fabric8:helm` yet?");
        }
        File gitFolder = new File(outputDir, ".git");
        if (!Files.isDirectory(gitFolder)) {
            throw new MojoExecutionException("No helm git repository exists for " + gitFolder + ". Did you run `mvn fabric8:helm` yet?");
        }
        FileRepositoryBuilder builder = new FileRepositoryBuilder();
        Git git = null;

        try {
            Repository repository = builder.setGitDir(gitFolder)
                    .readEnvironment() // scan environment GIT_* variables
                    .findGitDir() // scan up the file system tree
                    .build();

            git = new Git(repository);

            git.add().addFilepattern(".").call();
        } catch (Exception e) {
            throw new MojoExecutionException("Failed to add files to the helm git repository: " + e, e);
        }
        CommitCommand commit = git.commit().setAll(true).setMessage(commitMessage);
        PersonIdent author = null;
        if (Strings.isNotBlank(userName) && Strings.isNotBlank(emailAddress)) {
            author = new PersonIdent(userName, emailAddress);
        }
        if (author != null) {
            commit = commit.setAuthor(author);
        }

        try {
            RevCommit answer = commit.call();
            getLog().info("Committed " + answer.getId() + " " + answer.getFullMessage());
        } catch (GitAPIException e) {
            throw new MojoExecutionException("Failed to commit changes to help repository: " + e, e);
        }

        if (pushChanges) {
            PushCommand push = git.push();
            try {
                push.setRemote(remoteRepoName).call();

                getLog().info("Pushed commits upstream to " + getHelmGitUrl());
            } catch (GitAPIException e) {
                throw new MojoExecutionException("Failed to push helm git changes to remote repository " + remoteRepoName + ": " + e, e);
            }
        }

    }

    protected boolean isRootReactorBuild() {
        if (reactorProjects == null || reactorProjects.size() <= 1) {
            return true;
        }
        MavenProject project = getProject();
        if (project != null) {
            MavenProject parent = project.getParent();
            if (parent != null) {
                if (!containsProject(reactorProjects, parent)) {
                    return true;
                }
            }
        }
        return false;
    }

    protected static boolean containsProject(List<MavenProject> projects, MavenProject actualProject) {
        for (MavenProject project : projects) {
            if (projectsEqual(project, actualProject)) {
                return true;
            }
        }
        return false;
    }

    protected static boolean projectsEqual(MavenProject project1, MavenProject project2) {
        return Objects.equal(project1.getGroupId(), project2.getGroupId()) &&
                Objects.equal(project1.getArtifactId(), project2.getArtifactId()) &&
                Objects.equal(project1.getVersion(), project2.getVersion());
    }


}
