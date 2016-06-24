/**
 *  Copyright 2005-2016 Red Hat, Inc.
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
package io.fabric8.repo.git;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import java.io.InputStream;
import java.util.List;

/**
 * REST API for working with git hosted repositories using back ends like
 * <a href="http://gogs.io/">gogs</a> or <a href="http://github.com/">github</a>
 */
@Path("api/v1")
@Produces("application/json")
@Consumes("application/json")
public interface GitApi {

    @GET
    @Path("user/repos")
    public List<RepositoryDTO> listRepositories();

    @GET
    @Path("user/orgs")
    public List<OrganisationDTO> listUserOrganisations();

    @GET
    @Path("orgs/{org}/repos")
    public List<RepositoryDTO> listOrganisationRepositories(@PathParam("org") String organisation);

    @GET
    @Path("orgs/{org}/repos/{repo}")
    public RepositoryDTO getOrganisationRepository(@PathParam("org") String organisation, @PathParam("repo") String repo);

/*
    TODO not implemented yet

    @GET
    @Path("orgs/{org}/repos/{repo}/raw/{path:.*}")
    public InputStream getOrganisationRawFile(@PathParam("org") String organisation, @PathParam("repo") String repo, @PathParam("path") String path);
*/

    @POST
    @Path("user/repos")
    public RepositoryDTO createRepository(CreateRepositoryDTO dto);


    @GET
    @Path("repos/{owner}/{repo}")
    public RepositoryDTO getRepository(@PathParam("owner") String owner, @PathParam("repo") String repo);

    /**
     * Returns the raw file for the given username, repo, branch/ref and file path
     */
    @GET
    @Path("repos/{username}/{repo}/raw/{branch}/{path:.*}")
    public InputStream getRawFile(@PathParam("username") String username, @PathParam("repo") String repo, @PathParam("branch") String branch, @PathParam("path") String path);


    @GET
    @Path("repos/{owner}/{repo}/hooks")
    public List<WebHookDTO> getWebhooks(@PathParam("owner") String owner, @PathParam("repo") String repo);

    @POST
    @Path("repos/{owner}/{repo}/hooks")
    public WebHookDTO createWebhook(@PathParam("owner") String owner, @PathParam("repo") String repo, CreateWebhookDTO dto);
}
