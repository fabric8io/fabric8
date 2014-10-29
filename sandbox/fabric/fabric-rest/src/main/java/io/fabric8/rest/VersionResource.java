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

import io.fabric8.api.FabricService;
import io.fabric8.api.Profile;
import io.fabric8.api.ProfileBuilder;
import io.fabric8.api.ProfileService;
import io.fabric8.api.Profiles;
import io.fabric8.api.Version;
import io.fabric8.api.jmx.ProfileDTO;
import io.fabric8.utils.Objects;
import io.fabric8.utils.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Represents a version resource
 */
@Produces("application/json")
@Consumes("application/json")
public class VersionResource extends ResourceSupport {
    private static final Logger LOG = LoggerFactory.getLogger(FabricResource.class);

    private final FabricResource fabricResource;
    private final Version version;

    public VersionResource(FabricResource fabricResource, Version version) {
        super(fabricResource, "/version/" + version.getId() + "/");
        this.fabricResource = fabricResource;
        this.version = version;
    }


    @Override
    public String toString() {
        return "VersionResource{" +
                "version=" + version +
                '}';
    }

    @GET
    public VersionDTO details() {
        return new VersionDTO(getLink("profiles"));
    }

    @GET
    @Path("profiles")
    public Map<String, String> profiles() {
        if (version != null) {
            List<String> profileIds = Profiles.profileIds(version.getProfiles());
            return mapToLinks(profileIds, "profile/");
        }
        return Collections.EMPTY_MAP;
    }

    /**
     * Creates a new profile.
     * <br>
     * For example send this JSON to be able to create a new profile:
     *
     * <code>
     * { "id": "myNewProfile", "parents": ["containers-tomcat"] }
     * </code>
     */
    @POST
    public Response createProfile(ProfileDTO profileDTO) throws URISyntaxException {
        Objects.notNull(profileDTO, "profileDTO");
        FabricService fabricService = getFabricService();
        Objects.notNull(fabricService, "fabricService");
        ProfileService profileService = getProfileService();
        Objects.notNull(profileService, "profileService");
        String id = profileDTO.getId();
        if (Strings.isNullOrBlank(id)) {
            return Response.status(Response.Status.BAD_REQUEST).entity("No id specified for the profile to be created").build();
        }

        URI location = new URI(getBaseUri() + "profile/" + id);

        // lets check it doesn't already exist
        String versionId = version.getId();
        if (profileService.hasProfile(versionId, id)) {
            return Response.seeOther(location).entity("Profile already exists for id: " + id).build();
        }

        // lets override whatever the version is set to
        profileDTO.setVersion(versionId);

        // create the profile
        ProfileBuilder builder = ProfileBuilder.Factory.create(versionId, id);
        profileDTO.populateBuilder(fabricService, profileService, builder);
        Profile profile = builder.getProfile();
        profileService.createProfile(profile);
        return Response.created(location).build();
    }


    @Path("profile/{profileId}")
    public ProfileResource version(@PathParam("profileId") String profileId) {
        if (Strings.isNotBlank(profileId) && version != null && version.hasProfile(profileId)) {
            Profile profile = version.getRequiredProfile(profileId);
            if (profile != null) {
                return new ProfileResource(this, profile);
            }
        }
        return null;
    }

    public FabricService getFabricService() {
        return fabricResource.getFabricService();
    }

    public FabricResource getFabricResource() {
        return fabricResource;
    }
}
