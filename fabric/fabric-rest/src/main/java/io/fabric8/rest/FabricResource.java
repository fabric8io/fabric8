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
import io.fabric8.api.Profiles;
import io.fabric8.api.Version;
import io.fabric8.api.jmx.FabricDTO;
import io.fabric8.api.jmx.FabricStatusDTO;
import io.fabric8.api.jmx.ProfileDTO;
import io.fabric8.common.util.Strings;
import org.apache.cxf.jaxrs.ext.MessageContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Resource;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Represents the root fabric resource.
 */
@Path("/")
@Produces("application/json")
public class FabricResource extends ResourceSupport {
    private static final Logger LOG = LoggerFactory.getLogger(FabricResource.class);

    public FabricResource() {
    }

    @GET
    public FabricDTO details() {
        FabricService fabricService = getFabricService();
        if (fabricService != null) {
            return new FabricDTO(fabricService, getLink("/containers"), getLink("/versions"), getLink("/status"));
        } else {
            noFabricService();
        }
        return null;
    }

    /**
     * Returns the list of container IDs
     */
    @GET
    @Path("containers")
    public Map<String, String> containers() {
        FabricService fabricService = getFabricService();
        if (fabricService != null) {
            return mapToLinks(Containers.containerIds(fabricService.getContainers()), "/container/");
        } else {
            noFabricService();
        }
        return Collections.EMPTY_MAP;
    }

    /**
     * Accesses a container resource
     */
    @Path("container/{containerId}")
    public ContainerResource container(@PathParam("containerId") String containerId) {
        FabricService fabricService = getFabricService();
        if (fabricService != null && Strings.isNotBlank(containerId)) {
            Container container = fabricService.getContainer(containerId);
            if (container != null) {
                return new ContainerResource(this, container);
            }
            LOG.warn("No container found for: {}", container);
        }
        return null;
    }


    /**
     * Returns the list of version ids
     */
    @GET
    @Path("versions")
    public Map<String,String> versions() {
        FabricService fabricService = getFabricService();
        if (fabricService != null) {
            List<String> versionIds = Profiles.versionIds(fabricService.getVersions());
            return mapToLinks(versionIds, "/version/");
        } else {
            noFabricService();
        }
        return Collections.EMPTY_MAP;
    }

    /**
     * Accesses a version resource
     */
    @Path("version/{versionId}")
    public VersionResource version(@PathParam("versionId") String versionId) {
        FabricService fabricService = getFabricService();
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

    @GET
    @Path("status")
    public FabricStatusDTO status() {
        FabricService fabricService = getFabricService();
        if (fabricService != null) {
            return new FabricStatusDTO(fabricService.getFabricStatus());
        }
        return null;
    }


}
