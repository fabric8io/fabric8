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

import io.fabric8.kubernetes.api.model.Endpoints;
import io.fabric8.kubernetes.api.model.EndpointsList;
import io.fabric8.kubernetes.api.model.Minion;
import io.fabric8.kubernetes.api.model.MinionList;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodList;
import io.fabric8.kubernetes.api.model.ReplicationController;
import io.fabric8.kubernetes.api.model.ReplicationControllerList;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceList;

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
 * Represents the Remote API to working with <a href="http://kubernetes.io/">Kubernetes</a> providing a facade
 * over the generated JAXRS client.
 */
@Path("api/v1beta1")
@Produces("application/json")
@Consumes("application/json")
public interface Kubernetes {

    /**
     * List all pods on this cluster
     */
    @GET
    @Path("pods")
    PodList getPods();


    /**
     * Create a new pod. currentState is ignored if present.
     *
     * @param entity e.g. {
     *               "kind": "Pod",
     *               "apiVersion": "v1beta1",
     *               "id": "php",
     *               "desiredState": {
     *               "manifest": {
     *               "version": "v1beta1",
     *               "id": "php",
     *               "containers": [{
     *               "name": "nginx",
     *               "image": "dockerfile/nginx",
     *               "ports": [{
     *               "containerPort": 80,
     *               "hostPort": 8080
     *               }],
     *               "livenessProbe": {
     *               "enabled": true,
     *               "type": "http",
     *               "initialDelaySeconds": 30,
     *               "httpGet": {
     *               "path": "/index.html",
     *               "port": "8080"
     *               }
     *               }
     *               }]
     *               }
     *               },
     *               "labels": {
     *               "name": "foo"
     *               }
     *               }
     */
    @POST
    @Path("pods")
    @Consumes("application/json")
    String createPod(Pod entity) throws Exception;

    @POST
    @Path("pods")
    @Consumes("application/json")
    String createPod(Pod entity, @QueryParam("namespace") String namespace) throws Exception;

    /**
     * Get a specific pod
     *
     * @param podId
     */
    @GET
    @Path("pods/{podId}")
    Pod getPod(@PathParam("podId") @NotNull String podId);

    /**
     * Update a pod
     *
     * @param entity e.g. {
     *               "kind": "Pod",
     *               "apiVersion": "v1beta1",
     *               "id": "php",
     *               "desiredState": {
     *               "manifest": {
     *               "version": "v1beta1",
     *               "id": "php",
     *               "containers": [{
     *               "name": "nginx",
     *               "image": "dockerfile/nginx",
     *               "ports": [{
     *               "containerPort": 80,
     *               "hostPort": 8080
     *               }],
     *               "livenessProbe": {
     *               "enabled": true,
     *               "type": "http",
     *               "initialDelaySeconds": 30,
     *               "httpGet": {
     *               "path": "/index.html",
     *               "port": "8080"
     *               }
     *               }
     *               }]
     *               }
     *               },
     *               "labels": {
     *               "name": "foo"
     *               }
     *               }
     * @param podId
     */
    @PUT
    @Path("pods/{podId}")
    @Consumes("application/json")
    String updatePod(@PathParam("podId") @NotNull String podId, Pod entity) throws Exception;

    /**
     * Delete a specific pod
     *
     * @param podId
     */
    @DELETE
    @Path("pods/{podId}")
    @Consumes("text/plain")
    String deletePod(@PathParam("podId") @NotNull String podId) throws Exception;


    /**
     * List all services on this cluster
     */
    @Path("services")
    @GET
    @Produces("application/json")
    ServiceList getServices();

    /**
     * Create a new service
     *
     * @param entity e.g. {
     *               "kind": "Service",
     *               "apiVersion": "v1beta1",
     *               "id": "example",
     *               "port": 8000,
     *               "labels": {
     *               "name": "nginx"
     *               },
     *               "selector": {
     *               "name": "nginx"
     *               }
     *               }
     */
    @Path("services")
    @POST
    @Consumes("application/json")
    String createService(Service entity) throws Exception;

    @Path("services")
    @POST
    @Consumes("application/json")
    String createService(Service entity, @QueryParam("namespace") String namespace) throws Exception;

    /**
     * Get a specific service
     *
     * @param serviceId
     */
    @GET
    @Path("services/{serviceId}")
    @Produces("application/json")
    Service getService(@PathParam("serviceId") @NotNull String serviceId);

    /**
     * Update a service
     *
     * @param serviceId
     * @param entity    e.g. {
     *                  "kind": "Service",
     *                  "apiVersion": "v1beta1",
     *                  "id": "example",
     *                  "port": 8000,
     *                  "labels": {
     *                  "name": "nginx"
     *                  },
     *                  "selector": {
     *                  "name": "nginx"
     *                  }
     *                  }
     */
    @PUT
    @Path("services/{serviceId}")
    @Consumes("application/json")
    String updateService(@PathParam("serviceId") @NotNull String serviceId, Service entity) throws Exception;

    /**
     * Delete a specific service
     *
     * @param serviceId
     */
    @DELETE
    @Path("services/{serviceId}")
    @Produces("application/json")
    @Consumes("text/plain")
    String deleteService(@PathParam("serviceId") @NotNull String serviceId) throws Exception;


    /**
     * List all replicationControllers on this cluster
     */
    @Path("replicationControllers")
    @GET
    @Produces("application/json")
    ReplicationControllerList getReplicationControllers();

    /**
     * Create a new controller. currentState is ignored if present.
     *
     * @param entity e.g.   {
     *               "id": "nginxController",
     *               "apiVersion": "v1beta1",
     *               "kind": "ReplicationController",
     *               "desiredState": {
     *               "replicas": 2,
     *               "replicaSelector": {"name": "nginx"},
     *               "podTemplate": {
     *               "desiredState": {
     *               "manifest": {
     *               "version": "v1beta1",
     *               "id": "nginxController",
     *               "containers": [{
     *               "name": "nginx",
     *               "image": "dockerfile/nginx",
     *               "ports": [{"containerPort": 80, "hostPort": 8080}]
     *               }]
     *               }
     *               },
     *               "labels": {"name": "nginx"}
     *               }},
     *               "labels": {"name": "nginx"}
     *               }
     */
    @Path("replicationControllers")
    @POST
    @Consumes("application/json")
    String createReplicationController(ReplicationController entity) throws Exception;

    @Path("replicationControllers")
    @POST
    @Consumes("application/json")
    String createReplicationController(ReplicationController entity, @QueryParam("namespace") String namespace) throws Exception;

    /**
     * Get a specific controller
     *
     * @param controllerId
     */
    @GET
    @Path("replicationControllers/{controllerId}")
    @Produces("application/json")
    ReplicationController getReplicationController(@PathParam("controllerId") @NotNull String controllerId);

    /**
     * Update a controller
     *
     * @param controllerId
     * @param entity       e.g.   {
     *                     "id": "nginxController",
     *                     "apiVersion": "v1beta1",
     *                     "kind": "ReplicationController",
     *                     "desiredState": {
     *                     "replicas": 2,
     *                     "replicaSelector": {"name": "nginx"},
     *                     "podTemplate": {
     *                     "desiredState": {
     *                     "manifest": {
     *                     "version": "v1beta1",
     *                     "id": "nginxController",
     *                     "containers": [{
     *                     "name": "nginx",
     *                     "image": "dockerfile/nginx",
     *                     "ports": [{"containerPort": 80, "hostPort": 8080}]
     *                     }]
     *                     }
     *                     },
     *                     "labels": {"name": "nginx"}
     *                     }},
     *                     "labels": {"name": "nginx"}
     *                     }
     */
    @PUT
    @Path("replicationControllers/{controllerId}")
    @Consumes("application/json")
    String updateReplicationController(@PathParam("controllerId") @NotNull String controllerId, ReplicationController entity) throws Exception;

    /**
     * Delete a specific controller
     *
     * @param controllerId
     */
    @DELETE
    @Path("replicationControllers/{controllerId}")
    @Produces("application/json")
    @Consumes("text/plain")
    String deleteReplicationController(@PathParam("controllerId") @NotNull String controllerId) throws Exception;

    /**
     * List all service endpoints on this cluster
     */
    @GET
    @Path("endpoints")
    EndpointsList getEndpoints();

    /**
     * List all endpoints for a service
     */
    @GET
    @Path("endpoints/{serviceId}")
    Endpoints endpointsForService(@PathParam("serviceId") @NotNull String serviceId, @QueryParam("namespace") String namespace);


    /**
     * List all the minions on this cluster
     */
    @GET
    @Path("minions")
    MinionList getMinions();

    /**
     * List all endpoints for a service
     */
    @GET
    @Path("minions/{minionId}")
    Minion minion(@PathParam("minionId") @NotNull String minionId);
}
