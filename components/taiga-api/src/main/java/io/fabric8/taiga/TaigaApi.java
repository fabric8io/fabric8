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
package io.fabric8.taiga;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import java.util.List;
import java.util.Map;

/**
 * REST API for working with <a href="http://taiga.io/">Taiga</a> or <a href="http://github.com/">github</a>
 */
@Path("api/v1")
@Produces("application/json")
@Consumes("application/json")
public interface TaigaApi {

    // Projects
    //-------------------------------------------------------------------------

    @GET
    @Path("projects")
    public List<ProjectDTO> getProjects();

    @GET
    @Path("projects/{id}")
    public ProjectDTO getProjectById(@PathParam("id") String id);

    @GET
    @Path("projects/by_slug")
    public ProjectDTO getProjectBySlug(@QueryParam("slug") String slug);

    @POST
    @Path("projects")
    public ProjectDTO createProject(ProjectDTO dto);

    @POST
    @Path("auth")
    public AuthDetailDTO authenticate(AuthDTO dto);


    // Modules
    //-------------------------------------------------------------------------

    @GET
    @Path("projects/{id}/modules")
    public Map<String,ModuleDTO> getModulesForProject(@PathParam("id") Long id);


    // Users
    //-------------------------------------------------------------------------
    @GET
    @Path("users/me")
    public UserDTO getMe();

    @GET
    @Path("users/{userId}")
    public UserDTO getUser(@PathParam("userId") String id);

}
