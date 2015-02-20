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

import io.fabric8.kubernetes.api.model.*;

import javax.validation.constraints.NotNull;
import javax.ws.rs.*;

/**
 * Represents the Remote API to working with <a href="http://kubernetes.io/">Kubernetes</a> providing a facade
 * over the generated JAXRS client.
 */
@Path("api/v1beta2")
@Produces("application/json")
@Consumes("application/json")
public interface Kubernetes {

    static final String NAMESPACE_ALL = "";
    static final String NAMESPACE_DEFAULT = "";

    /**
     * List all pods on this cluster
     * @param namespace
     */
    @GET
    @Path("pods")
    PodList getPods(@QueryParam("namespace") String namespace);

    @POST
    @Path("pods")
    @Consumes("application/json")
    String createPod(Pod entity, @QueryParam("namespace") String namespace) throws Exception;

    /**
     * Get a specific pod
     *
     * @param podId
     * @param namespace
     */
    @GET
    @Path("pods/{podId}")
    Pod getPod(@PathParam("podId") @NotNull String podId, @QueryParam("namespace") String namespace);

    /**
     * Update a pod
     * @param podId
     * @param entity
     * @param namespace
     */
    @PUT
    @Path("pods/{podId}")
    @Consumes("application/json")
    String updatePod(@PathParam("podId") @NotNull String podId, Pod entity, @QueryParam("namespace") String namespace) throws Exception;

    @DELETE
    @Path("pods/{podId}")
    @Consumes("text/plain")
    String deletePod(@PathParam("podId") @NotNull String podId, @QueryParam("namespace") String namespace) throws Exception;

    /**
     * List all services on this cluster
     * @param namespace 
     */
    @Path("services")
    @GET
    @Produces("application/json")
    ServiceList getServices(@QueryParam("namespace") String namespace);

    @Path("services")
    @POST
    @Consumes("application/json")
    String createService(Service entity, @QueryParam("namespace") String namespace) throws Exception;

    /**
     * Get a specific service
     *
     * @param serviceId
     * @param namespace
     */
    @GET
    @Path("services/{serviceId}")
    @Produces("application/json")
    Service getService(@PathParam("serviceId") @NotNull String serviceId, @QueryParam("namespace") String namespace);

    /**
     * Update a service
     */
    @PUT
    @Path("services/{serviceId}")
    @Consumes("application/json")
    String updateService(@PathParam("serviceId") @NotNull String serviceId, Service entity, @QueryParam("namespace") String namespace) throws Exception;

    @DELETE
    @Path("services/{serviceId}")
    @Produces("application/json")
    @Consumes("text/plain")
    String deleteService(@PathParam("serviceId") @NotNull String serviceId, @QueryParam("namespace") String namespace) throws Exception;

    /**
     * List all replicationControllers on this cluster
     * @param namespace
     */
    @Path("replicationControllers")
    @GET
    @Produces("application/json")
    ReplicationControllerList getReplicationControllers(@QueryParam("namespace") String namespace);

    @Path("replicationControllers")
    @POST
    @Consumes("application/json")
    String createReplicationController(ReplicationController entity, @QueryParam("namespace") String namespace) throws Exception;

    @PUT
    @Path("replicationControllers/{controllerId}")
    @Consumes("application/json")
    String updateReplicationController(@PathParam("controllerId") @NotNull String controllerId, ReplicationController entity, @QueryParam("namespace") String namespace) throws Exception;

    /**
     * Get a specific controller
     *
     * @param controllerId
     * @param namespace 
     */
    @GET
    @Path("replicationControllers/{controllerId}")
    @Produces("application/json")
    ReplicationController getReplicationController(@PathParam("controllerId") @NotNull String controllerId, @QueryParam("namespace") String namespace);

    /**
     * Delete a specific controller
     *
     * @param controllerId
     */
    @DELETE
    @Path("replicationControllers/{controllerId}")
    @Produces("application/json")
    @Consumes("text/plain")
    String deleteReplicationController(@PathParam("controllerId") @NotNull String controllerId, @QueryParam("namespace") String namespace) throws Exception;

    /**
     * List all service endpoints on this cluster
     */
    @GET
    @Path("endpoints")
    EndpointsList getEndpoints(@QueryParam("namespace") String namespace);

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
