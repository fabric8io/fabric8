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
package io.fabric8.kubernetes.api;

import io.fabric8.kubernetes.api.model.PodListSchema;
import io.fabric8.kubernetes.api.model.PodSchema;

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
@Path("api/v1beta1")
@Produces("application/json")
public interface Kubernetes {

    /**
     * List all pods on this cluster
     */
    @GET
    @Path("pods")
    PodListSchema getPods();


    /**
     * Create a new pod. currentState is ignored if present.
     *
     * @param entity
     *      e.g. {
     *       "kind": "Pod",
     *       "apiVersion": "v1beta1",
     *       "id": "php",
     *       "desiredState": {
     *         "manifest": {
     *           "version": "v1beta1",
     *           "id": "php",
     *           "containers": [{
     *             "name": "nginx",
     *             "image": "dockerfile/nginx",
     *             "ports": [{
     *               "containerPort": 80,
     *               "hostPort": 8080
     *             }],
     *             "livenessProbe": {
     *               "enabled": true,
     *               "type": "http",
     *               "initialDelaySeconds": 30,
     *               "httpGet": {
     *                 "path": "/index.html",
     *                 "port": "8080"
     *               }
     *             }
     *           }]
     *         }
     *       },
     *       "labels": {
     *         "name": "foo"
     *       }
     *     }
     *
     *
     */
    @POST
    @Path("pods")
    @Consumes("application/json")
    void createPod(PodSchema entity) throws Exception;

    /**
     * Get a specific pod
     *
     * @param podId
     *
     */
    @GET
    @Path("pods/{podId}")
    PodSchema getPodsByPodId(@PathParam("podId") @NotNull String podId) throws Exception;

    /**
     * Update a pod
     *
     * @param entity
     *      e.g. {
     *       "kind": "Pod",
     *       "apiVersion": "v1beta1",
     *       "id": "php",
     *       "desiredState": {
     *         "manifest": {
     *           "version": "v1beta1",
     *           "id": "php",
     *           "containers": [{
     *             "name": "nginx",
     *             "image": "dockerfile/nginx",
     *             "ports": [{
     *               "containerPort": 80,
     *               "hostPort": 8080
     *             }],
     *             "livenessProbe": {
     *               "enabled": true,
     *               "type": "http",
     *               "initialDelaySeconds": 30,
     *               "httpGet": {
     *                 "path": "/index.html",
     *                 "port": "8080"
     *               }
     *             }
     *           }]
     *         }
     *       },
     *       "labels": {
     *         "name": "foo"
     *       }
     *     }
     *
     *
     * @param podId
     *
     */
    @PUT
    @Path("pods/{podId}")
    @Consumes("application/json")
    void putPodsByPodId( @PathParam("podId") @NotNull String podId, PodSchema entity) throws Exception;

    /**
     * Delete a specific pod
     *
     * @param podId
     *
     */
    @DELETE
    @Path("pods/{podId}")
    void deletePodsByPodId(@PathParam("podId") @NotNull String podId) throws Exception;

}
