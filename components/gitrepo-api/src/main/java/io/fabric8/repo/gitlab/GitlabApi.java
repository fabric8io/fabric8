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
package io.fabric8.repo.gitlab;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import java.util.List;

/**
 * REST API for working with gitlab
 */
@Path("/api/v3")
@Produces("application/json")
@Consumes("application/json")
public interface GitlabApi {

    // Namespaces
    //-------------------------------------------------------------------------

    @GET
    @Path("namespaces")
    public List<NamespaceDTO> getNamespaces();


    // Namespaces
    //-------------------------------------------------------------------------

    @GET
    @Path("groups")
    public List<GroupDTO> getGroups();


    @POST
    @Path("group")
    public GroupDTO createGroup(CreateGroupDTO dto);


    // Projects
    //-------------------------------------------------------------------------

    @GET
    @Path("groups/{groupId}/projects")
    public List<ProjectDTO> getProjects(@PathParam("groupId") Long groupId);

    @GET
    @Path("projects")
    public List<ProjectDTO> getProjects();

    @GET
    @Path("projects/{projectId}")
    public List<ProjectDTO> getProject(@PathParam("projectId") Long projectId);


    // Issues
    //-------------------------------------------------------------------------

    @GET
    @Path("issues")
    public List<IssueDTO> getIssues();

    @GET
    @Path("groups/{groupId}/issues")
    public List<IssueDTO> getGroupIssues(@PathParam("groupId") Long groupId);

    @GET
    @Path("projects/{projectId}/issues")
    public List<IssueDTO> getProjectIssue(@PathParam("projectId") Long projectId);



    // System Hooks
    //-------------------------------------------------------------------------


/*
    @GET
    @Path("repos/{owner}/{repo}/hooks")
    public List<WebHookDTO> getWebhooks(@PathParam("owner") String owner, @PathParam("repo") String repo);

    @POST
    @Path("repos/{owner}/{repo}/hooks")
    public WebHookDTO createWebhook(@PathParam("owner") String owner, @PathParam("repo") String repo, CreateWebhookDTO dto);
*/
}
