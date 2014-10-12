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
package io.fabric8.rest;

import io.fabric8.api.Container;
import io.fabric8.api.Containers;
import io.fabric8.api.FabricService;
import io.fabric8.api.Profile;
import io.fabric8.api.Profiles;
import io.fabric8.api.jmx.ContainerDTO;
import io.fabric8.common.util.Objects;
import io.fabric8.common.util.Strings;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import java.util.Collections;
import java.util.List;
import java.util.Map;


/**
 * A resource for a container inside fabric8
 */
@Produces("application/json")
public class ContainerResource extends ResourceSupport {
    private static final Logger LOG = LoggerFactory.getLogger(ContainerResource.class);
    private final Container container;

    public ContainerResource(ResourceSupport parent, Container container){
        super(parent);
        this.container = container;
    }

    /**
     * Returns the container details
     */
    @GET
    public ContainerDTO details() {
        return FabricDTO.createContainerDTO(container, getParentBaseUri());
    }

    /**
     * Returns links to the profiles implemented by this container
     */
    @GET
    @Path("profiles")
    public Map<String, String> profiles() {
        if (container != null) {
            List<String> profileIds = Profiles.profileIds(container.getProfiles());
            return mapToLinks(profileIds, "profile/");
        }
        return Collections.EMPTY_MAP;
    }

    /**
     * Returns the profile for the given id on this container
     */
    @Path("profile/{profileId}")
    public ProfileResource version(@PathParam("profileId") String profileId) {
        if (Strings.isNotBlank(profileId) && container != null) {
            Profile profile = Profiles.profile(container.getProfiles(), profileId);
            if (profile != null) {
                return new ProfileResource(this, profile);
            }
        }
        return null;
    }

    /**
     * Returns the current status
     */
    @GET
    @Path("status")
    public Response getStatus() {
        if (Containers.isAliveAndProvisionSuccess(container)) {
            return Response.status(Response.Status.OK).entity("Started").build();
        } else {
            boolean provisioningPending = container.isProvisioningPending();
            String answer = "Stopped";
            if (container.isAlive()) {
                answer = container.getProvisionResult();
                if (Container.PROVISION_SUCCESS.equals(answer) && provisioningPending) {
                    answer = "provisioning";
                }
            }
            return Response.status(Response.Status.NOT_FOUND).entity(answer).build();
        }
    }

    /**
     * Start the container
     */
    @POST
    @Path("start")
    public void start() {
        FabricService fabricService = getFabricService();
        Objects.notNull(fabricService, "fabricService");
        fabricService.startContainer(container);
    }

    /**
     * Stops the container
     */
    @POST
    @Path("stop")
    public void stop() {
        FabricService fabricService = getFabricService();
        Objects.notNull(fabricService, "fabricService");
        fabricService.stopContainer(container);
    }

    /**
     * Deletes this container
     */
    @DELETE
    public void delete() {
        FabricService fabricService = getFabricService();
        Objects.notNull(fabricService, "fabricService");
        fabricService.destroyContainer(container);
    }
}
