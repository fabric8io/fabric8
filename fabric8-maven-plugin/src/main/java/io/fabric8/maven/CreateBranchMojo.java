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
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

import io.fabric8.common.util.Files;
import io.fabric8.common.util.Strings;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Execute;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.InitCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.gitective.core.RepositoryUtils;

import static io.fabric8.git.internal.GitHelpers.checkoutBranch;
import static io.fabric8.git.internal.GitHelpers.createOrCheckoutBranch;


/**
 * Generates a git branch in the given git repository using the given profile zips as the profile data
 */
@Mojo(name = "branch", defaultPhase = LifecyclePhase.PACKAGE, requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME)
@Execute(phase = LifecyclePhase.PACKAGE)
public class CreateBranchMojo extends AbstractProfileMojo {

    /**
     * Name of the directory used to clone the git repository
     */
    @Parameter(property = "fabric8.branch.buildDir", defaultValue = "${project.build.directory}/git")
    private File buildDir;

    /**
     * Name of the branch to create
     */
    @Parameter(property = "fabric8.branch.branchName", required = true)
    private String branchName;

    /**
     * Name of the old branch to base the new branch off. If not specified then it defaults to the last branch created.
     */
    @Parameter(property = "fabric8.branch.oldBranchName")
    private String oldBranchName;

    /**
     * The URL of the remote git repository to clone. If blank it will just create a local git repository
     */
    @Parameter(property = "fabric8.branch.gitUrl")
    private String gitUrl;

    /**
     * Name of the directory used to create the full profiles configuration zip
     */
    @Parameter(property = "fabric8.branch.cloneAllBranches", defaultValue = "true")
    private boolean cloneAll;

    /**
     * Should we perform a git pull before creating the new branch
     */
    @Parameter(property = "fabric8.branch.pullOnStartup", defaultValue = "true")
    private boolean pullOnStartup;

    /**
     * Name of the remote git repository
     */
    @Parameter(property = "fabric8.branch.remoteName", defaultValue = "origin")
    private String remoteName = "origin";

    private Git git;


    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        try {
            initGitRepo();
            createAndCheckoutBranch();
            addProfileZips();
            removeSelectedFiles();
            commit("Branch created by fabric8 maven plugin");
        } catch (MojoExecutionException e) {
            throw e;
        } catch (Exception e) {
            throw new MojoExecutionException("Error executing", e);
        }
    }

    /**
     * unzips any dependent zips into the git directory
     */
    protected void addProfileZips() throws MojoExecutionException, IOException, GitAPIException {
        Set<Artifact> dependencyArtifacts = project.getDependencyArtifacts();
        for (Artifact artifact : dependencyArtifacts) {
            if ("zip".equals(artifact.getType())) {
                File file = artifact.getFile();
                if (file != null) {
                    getLog().info("Unzipping file: " + file);
                    addZipFile(file);
                } else {
                    getLog().warn("Could not resolve file for " + artifact);
                }
            }
        }
        git.add().addFilepattern("fabric").call();
    }

    protected void addZipFile(File file) throws MojoExecutionException, IOException, GitAPIException {
        if (file == null || !file.exists() || !file.isFile()) {
            throw new MojoExecutionException("Zip file does not exist: " + file);
        }
        File unzipDir = new File(buildDir, "fabric/profiles");
        unzipDir.mkdirs();
        try {
            Zips.unzip(new FileInputStream(file), unzipDir);
        } catch (IOException e) {
            throw new MojoExecutionException("Failed to unzip file " + file.getCanonicalPath()
                    + " to " + getGitBuildPathDescription() + ". " + e, e);
        }
    }

    /**
     * Removes any unrequired files from the branch before its committed
     */
    protected void removeSelectedFiles() throws MojoExecutionException, IOException, GitAPIException {
    }

    protected void initGitRepo() throws MojoExecutionException, IOException, GitAPIException {
        buildDir.mkdirs();
        File gitDir = new File(buildDir, ".git");
        if (!gitDir.exists()) {
            String repo = gitUrl;
            if (Strings.isNotBlank(repo)) {
                getLog().info("Cloning git repo " + repo + " into directory " + getGitBuildPathDescription() + " cloneAllBranches: " + cloneAll);
                CloneCommand command = Git.cloneRepository().
                        setCloneAllBranches(cloneAll).setURI(repo).setDirectory(buildDir).setRemote(remoteName);
                // .setCredentialsProvider(getCredentials()).
                try {
                    git = command.call();
                    return;
                } catch (Throwable e) {
                    getLog().error("Failed to command remote repo " + repo + " due: " + e.getMessage(), e);
                    // lets just use an empty repo instead
                }
            } else {
                InitCommand initCommand = Git.init();
                initCommand.setDirectory(buildDir);
                git = initCommand.call();
                getLog().info("Initialised an empty git configuration repo at " + getGitBuildPathDescription());

                // lets add a dummy file
                File readMe = new File(buildDir, "ReadMe.md");
                getLog().info("Generating " + readMe);
                Files.writeToFile(readMe, "fabric8 git repository created by fabric8-maven-plugin at " + new Date(), Charset.forName("UTF-8"));
                git.add().addFilepattern("ReadMe.md").call();
                commit("Initial commit");
            }
            String branch = git.getRepository().getBranch();
            configureBranch(branch);
        } else {
            getLog().info("Reusing existing git repository at " + getGitBuildPathDescription());

            FileRepositoryBuilder builder = new FileRepositoryBuilder();
            Repository repository = builder.setGitDir(gitDir)
                    .readEnvironment() // scan environment GIT_* variables
                    .findGitDir() // scan up the file system tree
                    .build();

            git = new Git(repository);
            if (pullOnStartup) {
                doPull();
            } else {
                getLog().info("git pull from remote config repo on startup is disabled");
            }
        }
    }

    protected void configureBranch(String branch) {
        // lets update the merge config
        if (Strings.isNotBlank(branch) && hasRemoteRepo()) {
            StoredConfig config = git.getRepository().getConfig();
            if (Strings.isNullOrBlank(config.getString("branch", branch, "remote")) || Strings.isNullOrBlank(config.getString("branch", branch, "merge"))) {
                config.setString("branch", branch, "remote", remoteName);
                config.setString("branch", branch, "merge", "refs/heads/" + branch);
                try {
                    config.save();
                } catch (IOException e) {
                    getLog().error("Failed to save the git configuration into " + new File(buildDir, ".git")
                            + " with branch " + branch + " on remote repo: " + gitUrl + " due: " + e.getMessage() + ". This exception is ignored.", e);
                }
            }
        }
    }

    /**
     * Returns true if the remote git url is defined or the local git repo has a remote url defined
     */
    protected boolean hasRemoteRepo() {
        if (Strings.isNotBlank(gitUrl)) {
            return true;
        }
        Repository repository = git.getRepository();
        StoredConfig config = repository.getConfig();
        String url = config.getString("remote", remoteName, "url");
        if (Strings.isNotBlank(url)) {
            return true;
        }
        return false;
    }

    protected void doPull() throws MojoExecutionException {
        //CredentialsProvider cp = getCredentials();
        CredentialsProvider cp = null;
        try {
            Repository repository = git.getRepository();
            StoredConfig config = repository.getConfig();
            String url = config.getString("remote", "origin", "url");
            if (Strings.isNullOrBlank(url)) {
                getLog().info("No remote repository defined for the git repository at " + getGitBuildPathDescription() + " so not doing a pull");
                return;
            }
            String branch = repository.getBranch();
            String mergeUrl = config.getString("branch", branch, "merge");
            if (Strings.isNullOrBlank(mergeUrl)) {
                getLog().info("No merge spec for branch." + branch + ".merge in the git repository at " + getGitBuildPathDescription() + " so not doing a pull");
                return;
            }
            getLog().info("Performing a pull in git repository " + getGitBuildPathDescription() + " on remote URL: " + url);

            git.pull().setCredentialsProvider(cp).setRebase(true).call();
        } catch (Throwable e) {
            String credText = "";
            if (cp instanceof UsernamePasswordCredentialsProvider) {
            }
            String message = "Failed to pull from the remote git repo with credentials " + cp + " due: " + e.getMessage() + ". This exception is ignored.";
            getLog().error(message, e);
            throw new MojoExecutionException(message, e);
        }
    }

    protected void createAndCheckoutBranch() throws MojoExecutionException, IOException, GitAPIException {
        if (Strings.isNullOrBlank(oldBranchName)) {
            // lets find the previous branch
            List<String> branches = new ArrayList<String>(RepositoryUtils.getBranches(git.getRepository()));
            int size = branches.size();
            if (size > 0) {
                String last = branches.get(size - 1);
                int idx = last.lastIndexOf('/');
                if (idx > 0) {
                    oldBranchName = last.substring(idx + 1);
                    getLog().info("Using previous branch: " + oldBranchName);
                }
            }
        }
        if (Strings.isNullOrBlank(oldBranchName)) {
            oldBranchName = "master";
            getLog().warn("Could not deduce the old branch so setting it to: " + oldBranchName);
        }
        checkoutBranch(git, oldBranchName);
        getLog().info("Creating branch " + branchName + " in " + getGitBuildPathDescription());

        createOrCheckoutBranch(git, branchName, remoteName);
        checkoutBranch(git, branchName);
    }

    protected void commit(String message) throws GitAPIException {
        git.commit().setMessage(message).call();
    }

    protected String getGitBuildPathDescription() throws IOException {
        return buildDir.getCanonicalPath();
    }
}
