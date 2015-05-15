/**
 * Copyright 2005-2014 Red Hat, Inc.
 * <p/>
 * Red Hat licenses this file to you under the Apache License, version
 * 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */
package io.fabric8.kubernetes.api;

import io.fabric8.openshift.api.model.OAuthClient;

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
 * Various Kubernetes extensions defined in the OpenShift project which are namespace agnostic
 */
@Path("osapi/v1beta3")
@Produces("application/json")
@Consumes("application/json")
public interface KubernetesGlobalExtensions {

    @POST
    @Path("oauthclients")
    @Consumes("application/json")
    String createOAuthClient(OAuthClient entity) throws Exception;

    @GET
    @Path("oauthclients/{name}")
    OAuthClient getOAuthClient(@PathParam("name") @NotNull String name);

    @PUT
    @Path("oauthclients/{name}")
    @Consumes("application/json")
    String updateOAuthClient(@PathParam("name") @NotNull String name, OAuthClient entity) throws Exception;

    @DELETE
    @Path("oauthclients/{name}")
    String deleteOAuthClient(@PathParam("name") @NotNull String name);

}
