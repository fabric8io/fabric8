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
package io.fabric8.forge.rest;

import io.fabric8.forge.rest.main.GitUserHelper;
import io.fabric8.forge.rest.main.RepositoryCache;
import io.fabric8.forge.rest.main.UserDetails;
import io.fabric8.repo.git.GitRepoClient;
import io.fabric8.repo.git.RepositoryDTO;
import io.fabric8.utils.Strings;
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
        return userDetails.createRepoClient();
    }

}
