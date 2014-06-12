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
package io.fabric8.rest;

import io.fabric8.api.FabricRequirements;
import io.fabric8.api.FabricService;
import io.fabric8.api.Profile;
import io.fabric8.api.ProfileRequirements;
import io.fabric8.api.jmx.ProfileDTO;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import java.util.List;

/**
 * Represents a Profile resource
 */
@Produces("application/json")
public class ProfileResource {
    private final FabricResource fabricResource;
    private final Profile profile;

    public ProfileResource(FabricResource fabricResource, Profile profile) {
        this.fabricResource = fabricResource;
        this.profile = profile;
    }

    public ProfileResource(VersionResource versionResource, Profile profile) {
        this(versionResource.getFabricResource(), profile);
    }

    public ProfileResource(ContainerResource containerResource, Profile profile) {
        this(containerResource.getFabricResource(), profile);
    }

    @Override
    public String toString() {
        return "ProfileResource{" +
                "profile=" + profile +
                '}';
    }

    @GET
    public ProfileDTO details() {
        return new ProfileDTO(profile);
    }

    /**
     * Returns the overlay (effective) profile
     */
    @Path("overlay")
    public ProfileResource overlay() {
        if (profile.isOverlay()) {
            return null;
        } else {
            return new ProfileResource(fabricResource, profile.getOverlay());
        }
    }

    @GET
    @Path("requirements")
    public ProfileRequirements requirements() {
        FabricRequirements requirements = getFabricService().getRequirements();
        if (requirements != null) {
            return requirements.getOrCreateProfileRequirement(profile.getId());
        }
        return null;
    }

    @GET
    @Path("fileNames")
    public List<String> fileNames() {
        return profile.getConfigurationFileNames();
    }


    @GET
    @Path("file/{fileName: .*}")
    public Response file(@PathParam("fileName") String fileName) {
        byte[] bytes = profile.getFileConfiguration(fileName);
        if (bytes == null) {
            return Response.status(Response.Status.NOT_FOUND).
                    entity("No file: " + fileName +
                            " for profile: " + profile.getId() +
                            " version: " + profile.getVersion()).build();
        }
        String mediaType = guessMediaType(fileName);
        return Response.ok(bytes, mediaType).build();
    }

    protected String guessMediaType(String fileName) {
        // TODO isn't there a helper method in jaxrs/cxf/somewhere to do this?
        if (fileName.endsWith(".xml")) {
            return "application/xml";
        }
        if (fileName.endsWith(".json")) {
            return "application/json";
        }
        if (fileName.endsWith(".xml") || fileName.endsWith(".xsd")) {
            return "text/html";
        }
        if (fileName.endsWith(".html") || fileName.endsWith(".htm")) {
            return "application/html";
        }
        if (fileName.endsWith(".properties")) {
            return "text/x-java-properties";
        }
        if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) {
            return "image/jpeg";
        }
        return "text/plain";
    }

    protected FabricService getFabricService() {
        return fabricResource.getFabricService();
    }
}
