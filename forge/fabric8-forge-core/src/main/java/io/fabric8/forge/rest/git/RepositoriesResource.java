/**
 * Copyright 2005-2015 Red Hat, Inc.
 * <p/>
 * Red Hat licenses this file to you under the Apache License, version
 * 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */
package io.fabric8.forge.rest.git;

import io.fabric8.forge.rest.main.GitUserHelper;
import io.fabric8.forge.rest.main.ProjectFileSystem;
import io.fabric8.forge.rest.main.RepositoryCache;
import io.fabric8.forge.rest.main.UserDetails;
import io.fabric8.repo.git.GitRepoClient;
import io.fabric8.repo.git.RepositoryDTO;
import io.fabric8.utils.Strings;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 */
@Path("/api/forge/repos")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class RepositoriesResource {
    private static final transient Logger LOG = LoggerFactory.getLogger(RepositoriesResource.class);

    @Inject
    private GitUserHelper gitUserHelper;

    @Inject
    private RepositoryCache repositoryCache;

    @Inject
    private ProjectFileSystem projectFileSystem;

    @Inject
    private GitLockManager lockManager;

    @Context
    private HttpServletRequest request;

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

        String branch = request.getParameter("ref");
        if (Strings.isNullOrBlank(branch)) {
            branch = "master";
        }
        //File projectFolder = projectFileSystem.cloneOrPullProjectFolder(userId, repositoryName, userDetails);
        File projectFolder = projectFileSystem.getUserProjectFolder(userId, repositoryName);
        String cloneUrl = projectFileSystem.getCloneUrl(userId, repositoryName, userDetails);
        File gitFolder = new File(projectFolder, ".git");
        String remoteRepository = userId + "/" + repositoryName;
        RepositoryResource resource = new RepositoryResource(projectFolder, gitFolder, userDetails, origin, branch, remoteRepository, lockManager, projectFileSystem, cloneUrl);
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
