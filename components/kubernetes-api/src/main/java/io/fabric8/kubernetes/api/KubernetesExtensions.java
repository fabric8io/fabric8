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
import io.fabric8.openshift.api.model.ImageStream;
import io.fabric8.openshift.api.model.ImageStreamList;
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

/**
 * Various Kubernetes extensions defined in the OpenShift project
 */
@Path(KubernetesExtensions.OSAPI_ROOT_PATH + "/namespaces/{namespace}")
@Produces("application/json")
@Consumes("application/json")
public interface KubernetesExtensions {

    String OSAPI_ROOT_PATH = "osapi/v1beta3";

    @POST
    @Path("processedtemplates")
    @Consumes("application/json")
    String processTemplate(Template entity, @PathParam("namespace") String namespace) throws Exception;


/*
    TODO uncomment when TemplateList is in the schema

    @Path("templates")
    @GET
    @Produces("application/json")
    TemplateList getTemplates(@PathParam("namespace") String namespace);
*/

    @Path("templates")
    @POST
    @Consumes("application/json")
    String createTemplate(Template entity, @PathParam("namespace") String namespace) throws Exception;

    @GET
    @Path("templates/{name}")
    @Produces("application/json")
    Template getTemplate(@PathParam("name") @NotNull String name, @PathParam("namespace") String namespace);

    @PUT
    @Path("templates/{name}")
    @Consumes("application/json")
    String updateTemplate(@PathParam("name") @NotNull String name, Template entity, @PathParam("namespace") String namespace) throws Exception;

    @DELETE
    @Path("templates/{name}")
    @Produces("application/json")
    @Consumes("text/plain")
    String deleteTemplate(@PathParam("name") @NotNull String name, @PathParam("namespace") String namespace) throws Exception;


    // Routes
    //-------------------------------------------------------------------------


    @GET
    @Path("routes")
    RouteList getRoutes(@PathParam("namespace") String namespace);


    @POST
    @Path("routes")
    String createRoute(Route entity, @PathParam("namespace") String namespace) throws Exception;

    @GET
    @Path("routes/{name}")
    Route getRoute(@PathParam("name") @NotNull String name, @PathParam("namespace") String namespace);

    @PUT
    @Path("routes/{name}")
    @Consumes("application/json")
    String updateRoute(@PathParam("name") @NotNull String name, Route entity, @PathParam("namespace") String namespace) throws Exception;

    @DELETE
    @Path("routes/{name}")
    String deleteRoute(@PathParam("name") @NotNull String name, @PathParam("namespace") String namespace);


    // Builds
    //-------------------------------------------------------------------------

    @GET
    @Path("builds")
    BuildList getBuilds(@PathParam("namespace") String namespace);

    @POST
    @Path("builds")
    String createBuild(Build entity, @PathParam("namespace") String namespace) throws Exception;

    @GET
    @Path("builds/{name}")
    Build getBuild(@PathParam("name") @NotNull String name, @PathParam("namespace") String namespace);

    @PUT
    @Path("builds/{name}")
    @Consumes("application/json")
    String updateBuild(@PathParam("name") @NotNull String name, Build entity, @PathParam("namespace") String namespace) throws Exception;

    @DELETE
    @Path("builds/{name}")
    String deleteBuild(@PathParam("name") @NotNull String name, @PathParam("namespace") String namespace);


    // BuildConfigs
    //-------------------------------------------------------------------------

    @GET
    @Path("buildconfigs")
    BuildConfigList getBuildConfigs(@PathParam("namespace") String namespace);

    @POST
    @Path("buildconfigs")
    String createBuildConfig(BuildConfig entity, @PathParam("namespace") String namespace) throws Exception;

    @GET
    @Path("buildconfigs/{name}")
    BuildConfig getBuildConfig(@PathParam("name") @NotNull String name, @PathParam("namespace") String namespace);

    @PUT
    @Path("buildconfigs/{name}")
    @Consumes("application/json")
    String updateBuildConfig(@PathParam("name") @NotNull String name, BuildConfig entity, @PathParam("namespace") String namespace) throws Exception;

    @DELETE
    @Path("buildconfigs/{name}")
    String deleteBuildConfig(@PathParam("name") @NotNull String name, @PathParam("namespace") String namespace);


    // BuildConfigHooks
    //-------------------------------------------------------------------------
    @POST
    @Path("buildconfigHooks/{name}/{secret}/{type}")
    @Produces("text/plain")
    @Consumes("application/json")
    String triggerBuild(@PathParam("name") @NotNull String name, @PathParam("namespace") String namespace,
                        @PathParam("secret") @NotNull String secret,
                        @PathParam("type") @NotNull String type,
                        byte[] body);


    // ImageRepositorys
    //-------------------------------------------------------------------------

    @GET
    @Path("imagestreams")
    ImageStreamList getImageStreams(@PathParam("namespace") String namespace);

    @POST
    @Path("imagestreams")
    String createImageStream(ImageStream entity, @PathParam("namespace") String namespace) throws Exception;

    @GET
    @Path("imagestreams/{name}")
    ImageStream getImageStream(@PathParam("name") @NotNull String name, @PathParam("namespace") String namespace);

    @PUT
    @Path("imagestreams/{name}")
    @Consumes("application/json")
    String updateImageStream(@PathParam("name") @NotNull String name, ImageStream entity, @PathParam("namespace") String namespace) throws Exception;

    @DELETE
    @Path("imagestreams/{name}")
    String deleteImageStream(@PathParam("name") @NotNull String name, @PathParam("namespace") String namespace);


    // DeploymentConfigs
    //-------------------------------------------------------------------------

    @GET
    @Path("deploymentconfigs")
    DeploymentConfigList getDeploymentConfigs(@PathParam("namespace") String namespace);

    @POST
    @Path("deploymentconfigs")
    String createDeploymentConfig(DeploymentConfig entity, @PathParam("namespace") String namespace) throws Exception;

    @GET
    @Path("deploymentconfigs/{name}")
    DeploymentConfig getDeploymentConfig(@PathParam("name") @NotNull String name, @PathParam("namespace") String namespace);

    @PUT
    @Path("deploymentconfigs/{name}")
    @Consumes("application/json")
    String updateDeploymentConfig(@PathParam("name") @NotNull String name, DeploymentConfig entity, @PathParam("namespace") String namespace) throws Exception;

    @DELETE
    @Path("deploymentconfigs/{name}")
    String deleteDeploymentConfig(@PathParam("name") @NotNull String name, @PathParam("namespace") String namespace);


}
