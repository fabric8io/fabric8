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
package io.fabric8.service.child;

import io.fabric8.api.AutoScaleRequest;
import io.fabric8.api.ChildScalingRequirements;
import io.fabric8.api.Container;
import io.fabric8.api.ContainerAutoScaler;
import io.fabric8.api.Containers;
import io.fabric8.api.CreateChildContainerOptions;
import io.fabric8.api.FabricService;
import io.fabric8.api.NameValidator;
import io.fabric8.api.ProfileRequirements;
import io.fabric8.api.scr.support.Strings;
import io.fabric8.common.util.Filter;
import io.fabric8.common.util.Filters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 */
public class ChildAutoScaler implements ContainerAutoScaler {
    private static final transient Logger LOG = LoggerFactory.getLogger(ChildAutoScaler.class);

    private final ChildContainerProvider containerProvider;

    public ChildAutoScaler(ChildContainerProvider containerProvider) {
        this.containerProvider = containerProvider;
    }

    @Override
    public int getWeight() {
        return 0;
    }

    @Override
    public void createContainers(AutoScaleRequest request) throws Exception {
        int count = request.getDelta();
        String profile = request.getProfile();
        String version = request.getVersion();
        FabricService fabricService = request.getFabricService();
        CreateChildContainerOptions.Builder builder = null;
        if (fabricService != null) {
            builder = createAutoScaleOptions(request, fabricService);
        }
        if (builder != null) {
            Set<String> ignoreContainerNames = new HashSet<>();
            for (int i = 0; i < count; i++) {
                final CreateChildContainerOptions.Builder configuredBuilder = builder.number(1).version(version).profiles(profile);

                Container[] containers = fabricService.getContainers();
                NameValidator nameValidator = Containers.createNameValidator(containers);
                String name = Containers.createContainerName(containers, profile, containerProvider.getScheme(), nameValidator);
                ignoreContainerNames.add(name);

                CreateChildContainerOptions options = configuredBuilder.name(name).build();
                LOG.info("Creating container name " + name + " version " + version + " profile " + profile + " " + count + " container(s)");
                fabricService.createContainers(options);
            }
        } else {
            LOG.warn("Could not create version " + version + " profile " + profile + " due to missing autoscale configuration");
        }
    }

    protected CreateChildContainerOptions.Builder createAutoScaleOptions(AutoScaleRequest request, FabricService fabricService) {
        CreateChildContainerOptions.Builder builder = CreateChildContainerOptions.builder();
        Container[] containers = fabricService.getContainers();
        if (containers != null) {
            List<String> containerIds = Containers.rootContainerIds(containers);
            // allow the requirements to customise which root to use...
            if (containerIds.isEmpty()) {
                throw new IllegalStateException("No root containers are available!");
            }
            String rootContainer = null;
            if (containerIds.size() == 1) {
                rootContainer = containerIds.get(0);
            } else {
                rootContainer = chooseRootContainer(request, containerIds);
            }
            if (Strings.isNullOrBlank(rootContainer)) {
                throw new IllegalStateException("Could not choose a root container from the possible IDs: " + containerIds + " with requirements: " +  getChildScalingRequirements(request));
            } else {
                builder = builder.parent(rootContainer);
            }
        }
        String zookeeperUrl = fabricService.getZookeeperUrl();
        String zookeeperPassword = fabricService.getZookeeperPassword();
        return builder.jmxUser("admin").jmxPassword(zookeeperPassword).
                zookeeperUrl(zookeeperUrl).zookeeperPassword(zookeeperPassword);
    }

    protected String chooseRootContainer(AutoScaleRequest request, List<String> containerIds) {
        ChildScalingRequirements scalingRequirements = getChildScalingRequirements(request);
        if (scalingRequirements != null) {
            List<String> rootContainerPatterns = scalingRequirements.getRootContainerPatterns();
            if (rootContainerPatterns != null && !rootContainerPatterns.isEmpty()) {
                Filter<String> filter = Filters.createStringFilters(rootContainerPatterns);
                List<String> matchingRootContainers = Filters.filter(containerIds, filter);
                return Filters.matchRandomElement(matchingRootContainers);
            }
        }
        return Filters.matchRandomElement(containerIds);
    }

    protected ChildScalingRequirements getChildScalingRequirements(AutoScaleRequest request) {
        ChildScalingRequirements scalingRequirements = null;
        ProfileRequirements profileRequirements = request.getProfileRequirements();
        if (profileRequirements != null) {
            scalingRequirements = profileRequirements.getChildScalingRequirements();
        }
        return scalingRequirements;
    }

    @Override
    public void destroyContainers(String profile, int count, List<Container> containers) {
        // TODO

    }
}
