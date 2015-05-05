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
@Path("api/v1beta3")
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
    @Path("namespaces/{namespace}/namespaces/{namespace}/pods")
    PodList getPods(@PathParam("namespace") String namespace);

    @POST
    @Path("namespaces/{namespace}/namespaces/{namespace}/pods")
    @Consumes("application/json")
    String createPod(Pod entity, @PathParam("namespace") String namespace) throws Exception;

    /**
     * Get a specific pod
     *
     * @param podId
     * @param namespace
     */
    @GET
    @Path("namespaces/{namespace}/pods/{podId}")
    Pod getPod(@PathParam("podId") @NotNull String podId, @PathParam("namespace") String namespace);

    /**
     * Update a pod
     * @param podId
     * @param entity
     * @param namespace
     */
    @PUT
    @Path("namespaces/{namespace}/pods/{podId}")
    @Consumes("application/json")
    String updatePod(@PathParam("podId") @NotNull String podId, Pod entity, @PathParam("namespace") String namespace) throws Exception;

    @DELETE
    @Path("namespaces/{namespace}/pods/{podId}")
    @Consumes("text/plain")
    String deletePod(@PathParam("podId") @NotNull String podId, @PathParam("namespace") String namespace) throws Exception;

    /**
     * List all services on this cluster
     * @param namespace 
     */
    @Path("namespaces/{namespace}/services")
    @GET
    @Produces("application/json")
    ServiceList getServices(@PathParam("namespace") String namespace);

    @Path("namespaces/{namespace}/services")
    @POST
    @Consumes("application/json")
    String createService(Service entity, @PathParam("namespace") String namespace) throws Exception;

    /**
     * Get a specific service
     *
     * @param serviceId
     * @param namespace
     */
    @GET
    @Path("namespaces/{namespace}/services/{serviceId}")
    @Produces("application/json")
    Service getService(@PathParam("serviceId") @NotNull String serviceId, @PathParam("namespace") String namespace);

    /**
     * Update a service
     */
    @PUT
    @Path("namespaces/{namespace}/services/{serviceId}")
    @Consumes("application/json")
    String updateService(@PathParam("serviceId") @NotNull String serviceId, Service entity, @PathParam("namespace") String namespace) throws Exception;

    @DELETE
    @Path("namespaces/{namespace}/services/{serviceId}")
    @Produces("application/json")
    @Consumes("text/plain")
    String deleteService(@PathParam("serviceId") @NotNull String serviceId, @PathParam("namespace") String namespace) throws Exception;

    /**
     * List all replicationControllers on this cluster
     * @param namespace
     */
    @Path("namespaces/{namespace}/replicationcontrollers")
    @GET
    @Produces("application/json")
    ReplicationControllerList getReplicationControllers(@PathParam("namespace") String namespace);

    @Path("namespaces/{namespace}/replicationcontrollers")
    @POST
    @Consumes("application/json")
    String createReplicationController(ReplicationController entity, @PathParam("namespace") String namespace) throws Exception;

    @PUT
    @Path("namespaces/{namespace}/replicationcontrollers/{controllerId}")
    @Consumes("application/json")
    String updateReplicationController(@PathParam("controllerId") @NotNull String controllerId, ReplicationController entity, @PathParam("namespace") String namespace) throws Exception;

    /**
     * Get a specific controller
     *
     * @param controllerId
     * @param namespace 
     */
    @GET
    @Path("namespaces/{namespace}/replicationcontrollers/{controllerId}")
    @Produces("application/json")
    ReplicationController getReplicationController(@PathParam("controllerId") @NotNull String controllerId, @PathParam("namespace") String namespace);

    /**
     * Delete a specific controller
     *
     * @param controllerId
     */
    @DELETE
    @Path("namespaces/{namespace}/replicationcontrollers/{controllerId}")
    @Produces("application/json")
    @Consumes("text/plain")
    String deleteReplicationController(@PathParam("controllerId") @NotNull String controllerId, @PathParam("namespace") String namespace) throws Exception;

    /**
     * List all service endpoints on this cluster
     */
    @GET
    @Path("namespaces/{namespace}/endpoints")
    EndpointsList getEndpoints(@PathParam("namespace") String namespace);

    /**
     * List all endpoints for a service
     */
    @GET
    @Path("namespaces/{namespace}/endpoints/{serviceId}")
    Endpoints endpointsForService(@PathParam("serviceId") @NotNull String serviceId, @PathParam("namespace") String namespace);

    /**
     * List all the minions on this cluster
     */
    @GET
    @Path("nodes")
    NodeList getNodes();

    /**
     * List all endpoints for a service
     */
    @GET
    @Path("nodes/{nodeId}")
    Node node(@PathParam("nodeId") @NotNull String nodeId);
}
