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
package io.fabric8.kubernetes.api;

import io.fabric8.openshift.api.model.Build;
import io.fabric8.openshift.api.model.BuildConfig;
import io.fabric8.openshift.api.model.BuildConfigList;
import io.fabric8.openshift.api.model.BuildList;
import io.fabric8.openshift.api.model.DeploymentConfig;
import io.fabric8.openshift.api.model.DeploymentConfigList;
import io.fabric8.openshift.api.model.ImageRepository;
import io.fabric8.openshift.api.model.ImageRepositoryList;
import io.fabric8.openshift.api.model.Route;
import io.fabric8.openshift.api.model.RouteList;
import io.fabric8.openshift.api.model.template.Template;

import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

/**
 * Various Kubernetes extensions defined in the OpenShift project
 */
@Path("osapi/v1beta1")
@Produces("application/json")
@Consumes("application/json")
public interface KubernetesExtensions {

    @POST
    @Path("configs")
    @Consumes("application/json")
    String createConfig(Object entity) throws Exception;

    @POST
    @Path("templateConfigs")
    @Consumes("application/json")
    String createTemplate(Template entity) throws Exception;


    // Routes
    //-------------------------------------------------------------------------


    @GET
    @Path("routes")
    RouteList getRoutes(@QueryParam("namespace") String namespace);


    @POST
    @Path("routes")
    String createRoute(Route entity, @QueryParam("namespace") String namespace) throws Exception;

    @GET
    @Path("routes/{name}")
    Route getRoute(@PathParam("name") @NotNull String name, @QueryParam("namespace") String namespace);

    @PUT
    @Path("routes/{name}")
    @Consumes("application/json")
    String updateRoute(@PathParam("name") @NotNull String name, Route entity, @QueryParam("namespace") String namespace) throws Exception;

    @DELETE
    @Path("routes/{name}")
    String deleteRoute(@PathParam("name") @NotNull String name, @QueryParam("namespace") String namespace);


    // Builds
    //-------------------------------------------------------------------------

    @GET
    @Path("builds")
    BuildList getBuilds(@QueryParam("namespace") String namespace);

    @POST
    @Path("builds")
    String createBuild(Build entity) throws Exception;

    @GET
    @Path("builds/{name}")
    Build getBuild(@PathParam("name") @NotNull String name, @QueryParam("namespace") String namespace);

    @PUT
    @Path("builds/{name}")
    @Consumes("application/json")
    String updateBuild(@PathParam("name") @NotNull String name, Build entity, @QueryParam("namespace") String namespace) throws Exception;

    @DELETE
    @Path("builds/{name}")
    String deleteBuild(@PathParam("name") @NotNull String name, @QueryParam("namespace") String namespace);


    // BuildConfigs
    //-------------------------------------------------------------------------

    @GET
    @Path("buildConfigs")
    BuildConfigList getBuildConfigs(@QueryParam("namespace") String namespace);

    @POST
    @Path("buildConfigs")
    String createBuildConfig(BuildConfig entity) throws Exception;

    @GET
    @Path("buildConfigs/{name}")
    BuildConfig getBuildConfig(@PathParam("name") @NotNull String name, @QueryParam("namespace") String namespace);

    @PUT
    @Path("buildConfigs/{name}")
    @Consumes("application/json")
    String updateBuildConfig(@PathParam("name") @NotNull String name, BuildConfig entity, @QueryParam("namespace") String namespace) throws Exception;

    @DELETE
    @Path("buildConfigs/{name}")
    String deleteBuildConfig(@PathParam("name") @NotNull String name, @QueryParam("namespace") String namespace);


    // BuildConfigHooks
    //-------------------------------------------------------------------------
    @POST
    @Path("buildConfigHooks/{name}/{secret}/{type}")
    @Produces("text/plain")
    @Consumes("application/json")
    String triggerBuild(@PathParam("name") @NotNull String name, @QueryParam("namespace") String namespace,
                        @PathParam("secret") @NotNull String secret,
                        @PathParam("type") @NotNull String type,
                        byte[] body);


    // ImageRepositorys
    //-------------------------------------------------------------------------

    @GET
    @Path("imageRepositories")
    ImageRepositoryList getImageRepositories(@QueryParam("namespace") String namespace);

    @POST
    @Path("imageRepositories")
    String createImageRepository(ImageRepository entity) throws Exception;

    @GET
    @Path("imageRepositories/{name}")
    ImageRepository getImageRepository(@PathParam("name") @NotNull String name, @QueryParam("namespace") String namespace);

    @PUT
    @Path("imageRepositories/{name}")
    @Consumes("application/json")
    String updateImageRepository(@PathParam("name") @NotNull String name, ImageRepository entity, @QueryParam("namespace") String namespace) throws Exception;

    @DELETE
    @Path("imageRepositories/{name}")
    String deleteImageRepository(@PathParam("name") @NotNull String name, @QueryParam("namespace") String namespace);


    // DeploymentConfigs
    //-------------------------------------------------------------------------

    @GET
    @Path("deploymentConfigs")
    DeploymentConfigList getDeploymentConfigs(@QueryParam("namespace") String namespace);

    @POST
    @Path("deploymentConfigs")
    String createDeploymentConfig(DeploymentConfig entity) throws Exception;

    @GET
    @Path("deploymentConfigs/{name}")
    DeploymentConfig getDeploymentConfig(@PathParam("name") @NotNull String name, @QueryParam("namespace") String namespace);

    @PUT
    @Path("deploymentConfigs/{name}")
    @Consumes("application/json")
    String updateDeploymentConfig(@PathParam("name") @NotNull String name, DeploymentConfig entity, @QueryParam("namespace") String namespace) throws Exception;

    @DELETE
    @Path("deploymentConfigs/{name}")
    String deleteDeploymentConfig(@PathParam("name") @NotNull String name, @QueryParam("namespace") String namespace);


}
