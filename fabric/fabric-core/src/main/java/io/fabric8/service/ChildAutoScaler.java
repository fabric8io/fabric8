/**
 * Copyright (C) FuseSource, Inc.
 * http://fusesource.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.fabric8.service;

import io.fabric8.api.Container;
import io.fabric8.api.ContainerAutoScaler;
import io.fabric8.api.Containers;
import io.fabric8.api.CreateChildContainerOptions;
import io.fabric8.api.FabricService;
import io.fabric8.api.NameValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

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
    public void createContainers(String version, String profile, int count) throws Exception {
        CreateChildContainerOptions.Builder builder = null;
        FabricService fabricService = containerProvider.getFabricService();
        if (fabricService != null) {
            builder = createAuthoScaleOptions(fabricService);
        }
        if (builder != null) {
            Container[] containers = fabricService.getContainers();
            for (int i = 0; i < count; i++) {
                final CreateChildContainerOptions.Builder configuredBuilder = builder.number(1).version(version).profiles(profile);

                NameValidator nameValidator = Containers.createNameValidator(containers);
                String name = Containers.createContainerName(containers, profile, containerProvider.getScheme(), nameValidator);

                CreateChildContainerOptions options = configuredBuilder.name(name).build();
                LOG.info("Creating container name " + name + " version " + version + " profile " + profile + " " + count + " container(s)");
                fabricService.createContainers(options);
            }
        } else {
            LOG.warn("Could not create version " + version + " profile " + profile + " due to missing autoscale configuration");
        }
    }

    protected CreateChildContainerOptions.Builder createAuthoScaleOptions(FabricService fabricService) {
        CreateChildContainerOptions.Builder builder = CreateChildContainerOptions.builder();
        Container[] containers = fabricService.getContainers();
        if (containers != null) {
            String parent = null;
            for (Container container : containers) {
                if (container.isRoot()) {
                    parent = container.getId();
                    builder = builder.parent(parent);
                    break;
                }
            }
        }
        String zookeeperUrl = fabricService.getZookeeperUrl();
        String zookeeperPassword = fabricService.getZookeeperPassword();
        return builder.jmxUser("admin").jmxPassword(zookeeperPassword).
                zookeeperUrl(zookeeperUrl).zookeeperPassword(zookeeperPassword);
    }

    @Override
    public void destroyContainers(String profile, int count, List<Container> containers) {
        // TODO

    }
}
