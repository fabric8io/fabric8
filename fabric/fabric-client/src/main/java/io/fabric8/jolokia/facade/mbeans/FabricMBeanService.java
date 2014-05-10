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
package io.fabric8.jolokia.facade.mbeans;

import com.fasterxml.jackson.databind.type.TypeFactory;
import io.fabric8.jolokia.facade.JolokiaFabricConnector;
import io.fabric8.jolokia.facade.dto.*;
import io.fabric8.jolokia.facade.utils.Helpers;

import java.util.Collection;
import java.util.List;

/**
 * Author: lhein
 */
public class FabricMBeanService {
    private JolokiaFabricConnector connector;
    private FabricMBean mbean;

    public FabricMBeanService(JolokiaFabricConnector connector, FabricMBean mbean) {
        this.connector = connector;
        this.mbean = mbean;
    }

    public FabricMBean getMbean() {
        return this.mbean;
    }

    public void dispose() {
        this.mbean = null;
    }

    // ########################## API methods ####################################

    public FabricDTO getFabricDetails() {
        try {
            return Helpers.getObjectMapper().readValue(mbean.getFabricFields(), FabricDTO.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public FabricRequirementsDTO getRequirements() {
        try {
            return Helpers.getObjectMapper().readValue(mbean.requirements(), FabricRequirementsDTO.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public String setRequirements(FabricRequirementsDTO requirements) {
        try {
            return mbean.requirements(requirements);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public Collection<VersionDTO> getVersions() {
        try {
            return Helpers.getObjectMapper().readValue(mbean.versions(), TypeFactory.defaultInstance().constructParametricType(Collection.class, VersionDTO.class));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public Collection<VersionDTO> getVersionsFields(List versionsFields) {
        if (versionsFields == null || versionsFields.size() < 1) return getVersions();

        try {
            return Helpers.getObjectMapper().readValue(mbean.versions(versionsFields), TypeFactory.defaultInstance().constructParametricType(Collection.class, VersionDTO.class));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * returns the fabric status
     *
     * @return the fabric status or a runtime exception if unable to gather
     */
    public FabricStatusDTO getFabricStatus() {
        try {
            return Helpers.getObjectMapper().readValue(mbean.fabricStatus(), FabricStatusDTO.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public Collection<ContainerDTO> getContainers() {
        try {
            return Helpers.getObjectMapper().readValue(mbean.containers(), TypeFactory.defaultInstance().constructParametricType(Collection.class, ContainerDTO.class));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public Collection<ContainerDTO> getContainersFields(List containersFields) {
        if (containersFields == null || containersFields.size() < 1) return getContainers();

        try {
            return Helpers.getObjectMapper().readValue(mbean.containers(containersFields), TypeFactory.defaultInstance().constructParametricType(Collection.class, ContainerDTO.class));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public Collection<ProfileDTO> getProfiles(String version) {
        try {
            return Helpers.getObjectMapper().readValue(mbean.getProfiles(version), TypeFactory.defaultInstance().constructParametricType(Collection.class, ProfileDTO.class));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public Collection<ProfileDTO> getProfilesFields(String version, List profilesFields) {
        if (profilesFields == null || profilesFields.size() < 1) return getProfiles(version);

        try {
            return Helpers.getObjectMapper().readValue(mbean.getProfiles(version, profilesFields), TypeFactory.defaultInstance().constructParametricType(Collection.class, ProfileDTO.class));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public Collection<String> getProfileIds(String version) {
        try {
            return Helpers.getObjectMapper().readValue(mbean.getProfileIds(version), TypeFactory.defaultInstance().constructParametricType(Collection.class, String.class));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
