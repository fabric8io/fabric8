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
package io.fabric8.service.ssh;

import io.fabric8.api.AutoScaleRequest;
import io.fabric8.api.Container;
import io.fabric8.api.ContainerAutoScaler;
import io.fabric8.api.Containers;
import io.fabric8.api.FabricService;
import io.fabric8.api.NameValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 */
public class SshAutoScaler implements ContainerAutoScaler {
    private static final transient Logger LOG = LoggerFactory.getLogger(SshAutoScaler.class);

    private final SshContainerProvider containerProvider;

    public SshAutoScaler(SshContainerProvider containerProvider) {
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
        String command = request.getVersion();
        FabricService fabricService = request.getFabricService();

        CreateSshContainerOptions.Builder builder = null;
        if (fabricService != null) {
            builder = createAuthoScaleOptions(fabricService);
        }
        if (builder != null) {
            Container[] containers = fabricService.getContainers();
            for (int i = 0; i < count; i++) {
                final CreateSshContainerOptions.Builder configuredBuilder = builder.number(1).version(command).profiles(profile);

                NameValidator nameValidator = Containers.createNameValidator(containers);
                String name = Containers.createContainerName(containers, profile, containerProvider.getScheme(), nameValidator);

                CreateSshContainerOptions options = configuredBuilder.name(name).build();
                LOG.info("Creating container name " + name + " version " + command + " profile " + profile + " " + count + " container(s)");
                fabricService.createContainers(options);
            }
        } else {
            LOG.warn("Could not create version " + command + " profile " + profile + " due to missing autoscale configuration");
        }
    }

    protected CreateSshContainerOptions.Builder createAuthoScaleOptions(FabricService fabricService) {
        CreateSshContainerOptions.Builder builder = CreateSshContainerOptions.builder();
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
        return builder.zookeeperUrl(zookeeperUrl).zookeeperPassword(zookeeperPassword);
    }

    @Override
    public void destroyContainers(String profile, int count, List<Container> containers) {
        // TODO

    }
}
