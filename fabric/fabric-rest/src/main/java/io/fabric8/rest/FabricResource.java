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
import java.util.Collections;
import java.util.List;

/**
 * Represents the root fabric resource.
 */
@Path("/")
@Produces("application/json")
public class FabricResource {
    private static final Logger LOG = LoggerFactory.getLogger(FabricResource.class);

    @Resource
    private MessageContext messageContext;

    private FabricService fabricService;

    public FabricResource() {
    }

    @GET
    public FabricDTO details() {
        if (fabricService != null) {
            return new FabricDTO(fabricService);
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
    public List<String> containers() {
        if (fabricService != null) {
            return Containers.containerIds(fabricService.getContainers());
        } else {
            noFabricService();
        }
        return Collections.EMPTY_LIST;
    }

    /**
     * Accesses a container resource
     */
    @Path("container/{containerId}")
    public ContainerResource container(@PathParam("containerId") String containerId) {
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
    public List<String> versions() {
        if (fabricService != null) {
            return Profiles.versionIds(fabricService.getVersions());
        } else {
            noFabricService();
        }
        return Collections.EMPTY_LIST;
    }

    /**
     * Accesses a version resource
     */
    @Path("version/{versionId}")
    public VersionResource version(@PathParam("versionId") String versionId) {
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
        if (fabricService != null) {
            return new FabricStatusDTO(fabricService.getFabricStatus());
        }
        return null;
    }


    public FabricService getFabricService() {
        return fabricService;
    }

    public void setFabricService(FabricService fabricService) {
        this.fabricService = fabricService;
    }


    protected void noFabricService() {
        LOG.warn("No fabricService available!");
    }
}
