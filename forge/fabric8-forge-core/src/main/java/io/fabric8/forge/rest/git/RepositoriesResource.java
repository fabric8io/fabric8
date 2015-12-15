/**
 *  Copyright 2005-2015 Red Hat, Inc.
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
package io.fabric8.forge.rest.git;

import io.fabric8.forge.rest.main.GitUserHelper;
import io.fabric8.forge.rest.main.ProjectFileSystem;
import io.fabric8.forge.rest.main.RepositoryCache;
import io.fabric8.forge.rest.main.UserDetails;
import io.fabric8.kubernetes.api.model.LocalObjectReference;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.openshift.api.model.BuildConfig;
import io.fabric8.openshift.api.model.BuildConfigSpec;
import io.fabric8.openshift.api.model.BuildSource;
import io.fabric8.openshift.api.model.GitBuildSource;
import io.fabric8.openshift.client.OpenShiftClient;
import io.fabric8.repo.git.GitRepoClient;
import io.fabric8.repo.git.RepositoryDTO;
import io.fabric8.utils.Base64Encoder;
import io.fabric8.utils.Files;
import io.fabric8.utils.Strings;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 */
@Path("/api/forge/repos")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class RepositoriesResource {
    private static final transient Logger LOG = LoggerFactory.getLogger(RepositoriesResource.class);
    public static final String SSH_PRIVATE_KEY_DATA_KEY = "ssh-privatekey";
    public static final String SSH_PUBLIC_KEY_DATA_KEY = "ssh-publickey";
    public static final String SSH_PRIVATE_KEY_DATA_KEY2 = "ssh-key";
    public static final String SSH_PUBLIC_KEY_DATA_KEY2 = "ssh-key.pub";
    public static final String USERNAME_DATA_KEY = "username";
    public static final String PASSWORD_DATA_KEY = "password";

    private final GitUserHelper gitUserHelper;
    private final RepositoryCache repositoryCache;
    private final ProjectFileSystem projectFileSystem;
    private final GitLockManager lockManager;
    private final KubernetesClient kubernetes;

    @Context
    private HttpServletRequest request;

    @Inject
    public RepositoriesResource(GitUserHelper gitUserHelper, RepositoryCache repositoryCache, ProjectFileSystem projectFileSystem, GitLockManager lockManager, KubernetesClient kubernetes) {
        this.gitUserHelper = gitUserHelper;
        this.repositoryCache = repositoryCache;
        this.projectFileSystem = projectFileSystem;
        this.lockManager = lockManager;
        this.kubernetes = kubernetes;
    }

    @GET
    @Path("_ping")
    public String ping() {
        return "true";
    }

    @GET
    public List<RepositoryDTO> getUserRepositories() {
        GitRepoClient repoClient = createGitRepoClient();
        List<RepositoryDTO> repositoryDTOs = repoClient.listRepositories();
        repositoryCache.updateUserRepositories(repositoryDTOs);

        for (RepositoryDTO repositoryDTO : repositoryDTOs) {
            enrichRepository(repositoryDTO);
        }
        return repositoryDTOs;
    }

    @GET
    @Path("user/{name}")
    public RepositoryDTO getUserRepository(@PathParam("name") String name) {
        UserDetails userDetails = gitUserHelper.createUserDetails(request);
        String user = userDetails.getUser();
        GitRepoClient repoClient = userDetails.createRepoClient();
        return repositoryCache.getOrFindUserRepository(user, name, repoClient);
    }

    @Path("user/{owner}/{repo}")
    public RepositoryResource repositoryResource(@PathParam("owner") String userId, @PathParam("repo") String repositoryName) throws IOException, GitAPIException {
        UserDetails userDetails = gitUserHelper.createUserDetails(request);
        String origin = projectFileSystem.getRemote();

        String branch = request.getParameter("branch");
        if (Strings.isNullOrBlank(branch)) {
            branch = "master";
        }
        String objectId = request.getParameter("ref");

        //File projectFolder = projectFileSystem.cloneOrPullProjectFolder(userId, repositoryName, userDetails);
        File projectFolder = projectFileSystem.getUserProjectFolder(userId, repositoryName);
        String cloneUrl = projectFileSystem.getCloneUrl(userId, repositoryName, userDetails);
        File gitFolder = new File(projectFolder, ".git");
        String remoteRepository = userId + "/" + repositoryName;
        RepositoryResource resource = new RepositoryResource(projectFolder, gitFolder, userDetails, origin, branch, remoteRepository, lockManager, projectFileSystem, cloneUrl, objectId);
        try {
            String message = request.getParameter("message");
            if (Strings.isNotBlank(message)) {
                resource.setMessage(message);
            }
        } catch (Exception e) {
            LOG.warn("failed to load message parameter: " + e, e);
        }
        return resource;
    }

    @Path("project/{namespace}/{projectId}")
    public RepositoryResource projectRepositoryResource(@PathParam("namespace") String namespace, @PathParam("projectId") String projectId) throws IOException, GitAPIException {
        UserDetails userDetails = gitUserHelper.createUserDetails(request);
        String origin = projectFileSystem.getRemote();

        String remoteRepository = namespace + "/" + projectId;

        String branch = request.getParameter("branch");
        if (Strings.isNullOrBlank(branch)) {
            branch = "master";
        }
        String objectId = request.getParameter("ref");

        // lets get the BuildConfig
        OpenShiftClient osClient = kubernetes.adapt(OpenShiftClient.class).inNamespace(namespace);
        BuildConfig buildConfig = osClient.buildConfigs().withName(projectId).get();
        if (buildConfig == null) {
            throw new NotFoundException("No BuildConfig for " + remoteRepository);
        }
        BuildConfigSpec spec = buildConfig.getSpec();
        if (spec == null) {
            throw new NotFoundException("No BuildConfig spec for " + remoteRepository);
        }
        BuildSource source = spec.getSource();
        if (source == null) {
            throw new NotFoundException("No BuildConfig source for " + remoteRepository);
        }
        GitBuildSource gitSource = source.getGit();
        if (gitSource == null) {
            throw new NotFoundException("No BuildConfig git source for " + remoteRepository);
        }
        String uri = gitSource.getUri();
        if (Strings.isNullOrBlank(uri)) {
            throw new NotFoundException("No BuildConfig git URI for " + remoteRepository);
        }

        String sourceSecretName = request.getParameter("secret");
        String secretNamespace = request.getParameter("secretNamespace");
        if (Strings.isNullOrBlank(secretNamespace)) {
            secretNamespace = namespace;
        }
        if (Strings.isNullOrBlank(sourceSecretName)) {
            LocalObjectReference sourceSecret = source.getSourceSecret();
            if (sourceSecret != null) {
                sourceSecretName = sourceSecret.getName();
            }
        }
        File projectFolder = projectFileSystem.getNamespaceProjectFolder(namespace, projectId, secretNamespace, sourceSecretName);


        String cloneUrl = uri;
        File gitFolder = new File(projectFolder, ".git");
        LOG.debug("Cloning " + cloneUrl);
        RepositoryResource resource = new RepositoryResource(projectFolder, gitFolder, userDetails, origin, branch, remoteRepository, lockManager, projectFileSystem, cloneUrl, objectId);
        if (sourceSecretName != null) {
            try {
                Secret secret = osClient.secrets().inNamespace(secretNamespace).withName(sourceSecretName).get();
                if (secret != null) {
                    Map<String, String> data = secret.getData();
                    File privateKeyFile = createSshKeyFile(namespace, sourceSecretName, SSH_PRIVATE_KEY_DATA_KEY, data.get(SSH_PRIVATE_KEY_DATA_KEY));
                    if (privateKeyFile == null) {
                        privateKeyFile = createSshKeyFile(namespace, sourceSecretName, SSH_PRIVATE_KEY_DATA_KEY2, data.get(SSH_PRIVATE_KEY_DATA_KEY2));
                    }
                    userDetails.setSshPrivateKey(privateKeyFile);
                    if (privateKeyFile != null) {
                        privateKeyFile.setReadable(false, true);
                    }
                    File publicKeyFile = createSshKeyFile(namespace, sourceSecretName, SSH_PUBLIC_KEY_DATA_KEY, data.get(SSH_PUBLIC_KEY_DATA_KEY));
                    if (publicKeyFile == null) {
                        publicKeyFile = createSshKeyFile(namespace, sourceSecretName, SSH_PUBLIC_KEY_DATA_KEY2, data.get(SSH_PUBLIC_KEY_DATA_KEY2));
                    }
                    userDetails.setSshPublicKey(publicKeyFile);
                    String username = decodeSecretData(data.get(USERNAME_DATA_KEY));
                    String password = decodeSecretData(data.get(PASSWORD_DATA_KEY));
                    if (Strings.isNotBlank(username)) {
                        userDetails.setUser(username);
                        if (LOG.isDebugEnabled()) {
                            LOG.debug("Using user: " + username);
                        }
                    }
                    if (Strings.isNotBlank(password)) {
                        userDetails.setPassword(password);
                    }
                }
            } catch (IOException e) {
                LOG.error("Failed to load secret key " + sourceSecretName + ". " + e, e);
                throw new RuntimeException("Failed to load secret key " + sourceSecretName + ". " + e, e);
            }
        }
        try {
            String message = request.getParameter("message");
            if (Strings.isNotBlank(message)) {
                resource.setMessage(message);
            }
        } catch (Exception e) {
            LOG.warn("failed to load message parameter: " + e, e);
        }
        return resource;
    }

    protected String decodeSecretData(String text) {
        if (Strings.isNotBlank(text)) {
            return Base64Encoder.decode(text);
        } else {
            return text;
        }
    }

    public HttpServletRequest getRequest() {
        return request;
    }

    public void setRequest(HttpServletRequest request) {
        this.request = request;
    }

    protected File createSshKeyFile(@PathParam("namespace") String namespace, String sourceSecretName, String privateKeyName, String privateKey) throws IOException {
        File keyFile = null;
        if (privateKey != null) {
            String text = Base64Encoder.decode(privateKey);
            keyFile = projectFileSystem.getSecretsFolder(namespace, sourceSecretName, privateKeyName);
            Files.writeToFile(keyFile, text.getBytes());
        }
        return keyFile;
    }

    protected void enrichRepository(RepositoryDTO repositoryDTO) {
        String repoName = repositoryDTO.getName();
        if (Strings.isNullOrBlank(repoName)) {
            String fullName = repositoryDTO.getFullName();
            if (Strings.isNotBlank(fullName)) {
                String[] split = fullName.split("/", 2);
                if (split != null && split.length > 1) {
                    String user = split[0];
                    String name = split[1];
                    //repositoryDTO.setUser(user);
                    repositoryDTO.setName(name);
                }
            }
        }
    }

    protected GitRepoClient createGitRepoClient() {
        UserDetails userDetails = gitUserHelper.createUserDetails(request);
        LOG.debug("Using user " + userDetails.getUser() + " at " + userDetails.getAddress());
        return userDetails.createRepoClient();
    }

}
