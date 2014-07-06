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
public class ProfileResource extends ResourceSupport {
    private final Profile profile;

    public ProfileResource(ResourceSupport parent, Profile profile) {
        super(parent, (profile.isOverlay() ? "/overlay" : "profile/" + profile.getId()));
        this.profile = profile;
    }

    @Override
    public String toString() {
        return "ProfileResource{" +
                "profile=" + profile +
                '}';
    }

    @GET
    public ProfileDTO details() {
        String overlay = profile.isOverlay() ? null : getLink("overlay");
        return new ProfileDTO(profile, overlay, getLink("requirements"), getLink("fileNames"));
    }

    /**
     * Returns the overlay (effective) profile
     */
    @Path("overlay")
    public ProfileResource overlay() {
        if (profile.isOverlay()) {
            return null;
        } else {
            return new ProfileResource(this, profile.getOverlay());
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
    public java.util.Map<String, String> fileNames() {
        List<String> fileNames = profile.getConfigurationFileNames();
        return mapToLinks(fileNames, "/file/");
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
        if (fileName.endsWith(".wadl")) {
            return "application/wadl+xml";
        }
        if (fileName.endsWith(".wsdl")) {
            return "application/wsdl+xml";
        }
        if (fileName.endsWith(".xsd")) {
            return "application/xsd+xml";
        }
        if (fileName.endsWith(".json")) {
            return "application/json";
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
        if (fileName.endsWith(".png")) {
            return "image/png";
        }
        if (fileName.endsWith(".gif")) {
            return "image/gif";
        }
        if (fileName.endsWith(".svg")) {
            return "image/svg+xml";
        }
        return "text/plain";
    }
}
