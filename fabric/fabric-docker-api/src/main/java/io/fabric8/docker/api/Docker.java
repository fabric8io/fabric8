/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.fabric8.docker.api;

import io.fabric8.docker.api.container.Change;
import io.fabric8.docker.api.container.ContainerConfig;
import io.fabric8.docker.api.container.ContainerCreateStatus;
import io.fabric8.docker.api.container.ContainerInfo;
import io.fabric8.docker.api.container.CopySource;
import io.fabric8.docker.api.container.HostConfig;
import io.fabric8.docker.api.container.Status;
import io.fabric8.docker.api.container.Top;
import io.fabric8.docker.api.image.DeleteInfo;
import io.fabric8.docker.api.image.ImageHistoryItem;
import io.fabric8.docker.api.image.ImageInfo;
import io.fabric8.docker.api.image.ImageSearchResult;
import io.fabric8.docker.api.image.Progress;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import java.util.List;

@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface Docker {

    String ID = "id";
    String NAME = "name";

    /**
     * Display system-wide information.
     *
     * @return Returns the {@link Info}.
     */
    @GET
    @Path("/info")
    Info info();

    /**
     * Show the docker version information
     *
     * @return
     */
    @GET
    @Path("/version")
    Version version();

    /**
     * Get the list of {@link Container} instances.
     *
     * @param all    1/True/true or 0/False/false, Show all containers. Only running containers are shown by default.
     * @param limit  Show limit last created containers, include non-running ones.
     * @param since  Show only containers created since Id, include non-running ones.
     * @param before Show only containers created before Id, include non-running ones.
     * @param size   1/True/true or 0/False/false, Show the containers sizes.
     * @return
     */
    @GET
    @Path("/containers/json")
    List<Container> containers(@QueryParam("all") Integer all, @QueryParam("limit") Integer limit, @QueryParam("since") String since, @QueryParam("before") String before, @QueryParam("size") Integer size);


    @GET
    @Path("/containers/{id}/json")
    ContainerInfo containerInspect(@PathParam(ID) String id);

    /**
     * Create a {@link Container}.
     *
     * @param config The container’s configuration
     * @return
     */
    @POST
    @Path("/containers/create")
    ContainerCreateStatus containerCreate(ContainerConfig config);

    @GET
    @Path("/containers/{id}/top")
    Top containerTop(@PathParam(ID) String id);

    @GET
    @Path("/containers/{id}/changes")
    List<Change> containerChanges(@PathParam(ID) String id);

    @GET
    @Path("/containers/{id}/export")
    byte[] containerExport(@PathParam(ID) String id);

    @POST
    @Path("/containers/{id}/start")
    void containerStart(@PathParam(ID) String id, HostConfig hostHostConfig);

    @POST
    @Path("/containers/{id}/stop")
    void containerStop(@PathParam(ID) String id, @QueryParam("t") Integer timeToWait);

    @POST
    @Path("/containers/{id}/restart")
    void containerRestart(@PathParam(ID) String id, @QueryParam("t") Integer timeToWait);

    @POST
    @Path("/containers/{id}/kill")
    void containerKill(@PathParam(ID) String id);

    /**
     * logs – 1/True/true or 0/False/false, return logs. Default false
     * stream – 1/True/true or 0/False/false, return stream. Default false
     * stdin – 1/True/true or 0/False/false, if stream=true, attach to stdin. Default false
     * stdout – 1/True/true or 0/False/false, if logs=true, return stdout log, if stream=true, attach to stdout. Default false
     * stderr – 1/True/true or 0/False/false, if logs=true, return stderr log, if stream=true, attach to stderr. Default false
     *
     * @param id
     */
    @POST
    @Path("/containers/{id}/attach")
    byte[] containerRestart(@PathParam(ID) String id, @QueryParam("logs") Integer logs, @QueryParam("stream") Integer stream, @QueryParam("stdin") Integer stdin, @QueryParam("stdout") Integer stdout, @QueryParam("stderr") Integer stderr);


    @POST
    @Path("/containers/{id}/wait")
    Status containerWait(@PathParam(ID) String id);

    /**
     * Remove a container.
     *
     * @param id The container id.
     * @param v  1/True/true or 0/False/false, Remove the volumes associated to the container. Default false
     * @return
     */
    @DELETE
    @Path("/containers/{id}")
    void containerRemove(@PathParam(ID) String id, @QueryParam("v") Integer v);


    @POST
    @Path("/containers/{id}/copy")
    byte[] containerCopy(@PathParam(ID) String id, CopySource resource);

    /**
     * Get the list of {@link Image}s.
     *
     * @param all 1/True/true or 0/False/false, Show all containers. Only running containers are shown by defaul
     * @return
     */
    @GET
    @Path("/images/json")
    List<Image> images(@QueryParam("all") Integer all);


    /**
     * Create an {@link Image}.
     *
     * @param fromImage The source image.
     * @param fromSrc   The source to import, - means stdin.
     * @param repo      The repository.
     * @param tag       The tag.
     * @param registry  The registry.
     */
    @POST
    @Path("/images/create")
    Progress imageCreate(@QueryParam("fromImage") String fromImage, @QueryParam("formSrc") String fromSrc, @QueryParam("repo") String repo, @QueryParam("tag") String tag, @QueryParam("registry") String registry);


    @POST
    @Path("/images/(name)/insert")
    Progress imageInsert(@QueryParam(NAME) String name, @QueryParam("path") String path, @QueryParam("url") String url);

    /**
     * Return low-level information on the image name.
     *
     * @param name The image name.
     * @return The {@link ImageInfo}.
     */
    @POST
    @Path("/images/{name}/json")
    ImageInfo imageInspect(@QueryParam(NAME) String name);

    /**
     * Return the history of the image name.
     *
     * @param name The image name.
     * @return Returns a list of {@link ImageHistoryItem} .
     */
    @GET
    @Path("/images/{name}/history")
    List<ImageHistoryItem> imageHistory(@QueryParam(NAME) String name);

    /**
     * Return the history of the image name.
     *
     * @param name The image name.
     * @return Return the {@link Progress}
     */
    @POST
    @Path("/images/{name}/push")
    Progress imagePush(@QueryParam(NAME) String name, @QueryParam("registry") String registry, Auth authConfig);

    /**
     * Tag an image into a repository
     *
     * @param name  The image name.
     * @param repo  The repository to tag in.
     * @param force 1/True/true or 0/False/false, default false.
     * @return
     */
    @POST
    @Path("/images/{name}/tag")
    void imageTag(@QueryParam(NAME) String name, @QueryParam("repo") String repo, @QueryParam("force") Integer force);

    /**
     * Remove the image id from the filesystem
     *
     * @param name The image name.
     * @return Return a list of {@link DeleteInfo}
     */
    @DELETE
    @Path("/images/{name}")
    List<DeleteInfo> imageDelete(@QueryParam(NAME) String name);

    /**
     * Search for an image in the docker index.
     *
     * @param term The search term to use.
     * @return Returns a list of {@link ImageSearchResult}.
     */
    @GET
    @Path("/images/search")
    List<ImageSearchResult> imageSearch(@QueryParam("term") String term);
}
