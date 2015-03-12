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

import io.fabric8.forge.rest.model.ProjectsModel;
import io.fabric8.forge.rest.dto.ProjectDTO;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.util.List;

/**
 */
@Path("/api/forge/projects")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ProjectsResource {
    @Inject
    private ProjectsModel projectsModel;

    @GET
    public List<ProjectDTO> getProjects() {
        return projectsModel.getProjects();
    }

    @GET
    @Path("_ping")
    public String ping() {
        return "true";
    }

    @POST
    public void addProject(ProjectDTO project) throws IOException {
        projectsModel.add(project);
    }

    @DELETE
    @Path("{path: .+}")
    public void removeProject(@PathParam("path") String path) throws IOException {
        projectsModel.remove(path);
    }

    @GET
    @Path("{path: .+}")
    public ProjectDTO getProject(@PathParam("path") String path) throws IOException {
        return projectsModel.findByPath(path);
    }
}
