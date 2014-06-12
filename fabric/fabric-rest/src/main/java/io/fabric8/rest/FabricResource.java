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
import io.fabric8.api.Profiles;
import io.fabric8.api.Version;
import io.fabric8.common.util.Strings;
import org.apache.cxf.jaxrs.ext.MessageContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Resource;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import java.util.Collections;
import java.util.List;

/**
 * Represents the root fabric resource.
 */
@Path("/")
public class FabricResource {
    private static final Logger LOG = LoggerFactory.getLogger(FabricResource.class);

    private
    @Resource
    MessageContext jaxrsContext;

    private FabricService fabricService;

    public FabricResource() {
    }

    @GET
    @Path("/versions")
    @Produces("application/json")
    public List<String> versions() {
        LOG.info("========== asking for versions");
        if (fabricService != null) {
            return Profiles.versionIds(fabricService.getVersions());
        } else {
            LOG.info("No fabricService!!!");
        }
        return Collections.EMPTY_LIST;
    }

    /**
     * Accesses a version
     */
    @GET
    @Path("/version/{versionId}/")
    @Produces("application/json")
    public VersionResource version(@PathParam("versionId") String versionId) {
        LOG.info("Looking up a version for id is: {}", versionId);
        if (fabricService != null && Strings.isNotBlank(versionId)) {
            Version version = fabricService.getVersion(versionId);
            if (version != null) {
                return new VersionResource(this, version);
            } else {
                LOG.warn("No version found for: {}", version);
            }
        }
        return null;
    }

    public FabricService getFabricService() {
        return fabricService;
    }

    public void setFabricService(FabricService fabricService) {
        this.fabricService = fabricService;
    }
}
