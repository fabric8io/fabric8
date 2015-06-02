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
import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.NamespaceList;
import io.fabric8.kubernetes.api.model.Node;
import io.fabric8.kubernetes.api.model.NodeList;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodList;
import io.fabric8.kubernetes.api.model.ReplicationController;
import io.fabric8.kubernetes.api.model.ReplicationControllerList;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretList;
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

/**
 * Represents the Remote API to working with <a href="http://kubernetes.io/">Kubernetes</a> providing a facade
 * over the generated JAXRS client.
 */
@Path(Kubernetes.ROOT_API_PATH)
@Produces("application/json")
@Consumes("application/json")
public interface Kubernetes {

    static final String ROOT_API_PATH = "api/v1beta3";

    static final String NAMESPACE_ALL = "";
    static final String NAMESPACE_DEFAULT = "default";

    static final String SERVICE_ACCOUNT_TOKEN_FILE = "/var/run/secrets/kubernetes.io/servicaccount/token";

    /**
     * List all namespaces on this cluster
     */
    @GET
    @Path("namespaces")
    NamespaceList getNamespaces();


    @POST
    @Path("namespaces")
    @Consumes("application/json")
    String createNamespace(Namespace entity) throws Exception;

    /**
     * Get a specific Namespace
     */
    @GET
    @Path("namespaces/{name}")
    Namespace getNamespace(@PathParam("name") @NotNull String name);

    /**
     * Update a namespace
     * @param namespaceId
     * @param entity
     */
    @PUT
    @Path("namespaces/{name}")
    @Consumes("application/json")
    String updateNamespace(@PathParam("name") @NotNull String namespaceId, Namespace entity) throws Exception;

    @DELETE
    @Path("namespaces/{name}")
    @Consumes("text/plain")
    String deleteNamespace(@PathParam("name") @NotNull String name) throws Exception;

    /**
     * List all pods on this cluster
     * @param namespace
     */
    @GET
    @Path("namespaces/{namespace}/pods")
    PodList getPods(@PathParam("namespace") String namespace);

    @POST
    @Path("namespaces/{namespace}/pods")
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
     * List all secrets on this cluster
     * @param namespace
     */
    @Path("namespaces/{namespace}/secrets")
    @GET
    @Produces("application/json")
    SecretList getSecrets(@PathParam("namespace") String namespace);

    @Path("namespaces/{namespace}/secrets")
    @POST
    @Consumes("application/json")
    String createSecret(Secret entity, @PathParam("namespace") String namespace) throws Exception;

    /**
     * Get a specific secret
     *
     * @param secretId
     * @param namespace
     */
    @GET
    @Path("namespaces/{namespace}/secrets/{secretId}")
    @Produces("application/json")
    Secret getSecret(@PathParam("secretId") @NotNull String secretId, @PathParam("namespace") String namespace);

    /**
     * Update a secret
     */
    @PUT
    @Path("namespaces/{namespace}/secrets/{secretId}")
    @Consumes("application/json")
    String updateSecret(@PathParam("secretId") @NotNull String secretId, Secret entity, @PathParam("namespace") String namespace) throws Exception;

    @DELETE
    @Path("namespaces/{namespace}/secrets/{secretId}")
    @Produces("application/json")
    @Consumes("text/plain")
    String deleteSecret(@PathParam("secretId") @NotNull String secretId, @PathParam("namespace") String namespace) throws Exception;



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
