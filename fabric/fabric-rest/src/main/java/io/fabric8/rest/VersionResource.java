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

import io.fabric8.api.Profile;
import io.fabric8.api.Profiles;
import io.fabric8.api.Version;
import io.fabric8.common.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import java.util.Collections;
import java.util.List;

/**
 * Represents a version resource
 */
public class VersionResource {
    private static final Logger LOG = LoggerFactory.getLogger(FabricResource.class);

    private final FabricResource fabricResource;
    private final Version version;

    public VersionResource(FabricResource fabricResource, Version version) {
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
    @Path("/profiles")
    @Produces("application/json")
    public List<String> profiles() {
        LOG.info("========== asking for profiles");
        if (version != null) {
            return Profiles.profileIds(version.getProfiles());
        }
        return Collections.EMPTY_LIST;
    }

    @GET
    @Path("/profile/{profileId}/")
    @Produces("application/json")
    public ProfileResource version(@PathParam("profileId") String profileId) {
        LOG.info("Looking up a profile for id is: {}", profileId);
        if (Strings.isNotBlank(profileId) && version != null && version.hasProfile(profileId)) {
            Profile profile = version.getProfile(profileId);
            if (profile != null) {
                return new ProfileResource(this, profile);
            }
        }
        return null;


    }
}