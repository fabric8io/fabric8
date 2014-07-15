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
import io.fabric8.api.FabricRequirements;
import io.fabric8.api.FabricService;
import io.fabric8.api.NameValidator;
import io.fabric8.api.ProfileRequirements;
import io.fabric8.api.SshHostConfiguration;
import io.fabric8.api.SshScalingRequirements;
import io.fabric8.common.util.Filter;
import io.fabric8.common.util.Filters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
        return 50;
    }

    @Override
    public void createContainers(AutoScaleRequest request) throws Exception {
        int count = request.getDelta();
        String profile = request.getProfile();
        String version = request.getVersion();
        FabricService fabricService = request.getFabricService();

        Container[] containers = fabricService.getContainers();
        for (int i = 0; i < count; i++) {
            CreateSshContainerOptions.Builder builder = null;
            NameValidator nameValidator = Containers.createNameValidator(containers);
            String name = Containers.createContainerName(containers, profile, containerProvider.getScheme(), nameValidator);

            if (fabricService != null) {
                builder = createAutoScaleOptions(request, fabricService, name);
            }
            if (builder != null) {
                final CreateSshContainerOptions.Builder configuredBuilder = builder.number(1).version(version).profiles(profile);

                CreateSshContainerOptions options = configuredBuilder.name(name).build();
                LOG.info("Creating container name " + name + " version " + version + " profile " + profile + " " + count + " container(s)");
                fabricService.createContainers(options);
            }
        }
    }

    protected CreateSshContainerOptions.Builder createAutoScaleOptions(AutoScaleRequest request, FabricService fabricService, String containerName) {
        CreateSshContainerOptions.Builder builder = CreateSshContainerOptions.builder();
        FabricRequirements requirements = request.getFabricRequirements();
        ProfileRequirements profileRequirements = request.getProfileRequirements();
        SshScalingRequirements sshScalingRequirements = profileRequirements.getSshScalingRequirements();
        List<SshHostConfiguration> sshHostConfigurations = filterHosts(requirements, sshScalingRequirements);
        SshHostConfiguration sshHostConfig = Filters.matchRandomElement(sshHostConfigurations);
        if (sshHostConfig == null ){
            LOG.warn("Could not create version " + request.getVersion() + " profile " + request.getProfile() + " as no matching hosts could be found for " + sshScalingRequirements);
            return null;
        }
        builder.configure(sshHostConfig, requirements, profileRequirements, containerName);
        String zookeeperUrl = fabricService.getZookeeperUrl();
        String zookeeperPassword = fabricService.getZookeeperPassword();
        if (builder.getProxyUri() == null) {
            builder.proxyUri(fabricService.getMavenRepoURI());
        }
        return builder.zookeeperUrl(zookeeperUrl).zookeeperPassword(zookeeperPassword);
    }

    /**
     * Filters the available host configurations
     */
    protected List<SshHostConfiguration> filterHosts(FabricRequirements requirements, SshScalingRequirements sshScalingRequirements) {
        List<SshHostConfiguration> answer = new ArrayList<SshHostConfiguration>();
        Map<String, SshHostConfiguration> hosts = requirements.getSshHostsMap();
        if (hosts != null) {
            Filter<String> filter = Filters.createStringFilters(sshScalingRequirements.getHostPatterns());
            Set<Map.Entry<String, SshHostConfiguration>> entries = hosts.entrySet();
            for (Map.Entry<String, SshHostConfiguration> entry : entries) {
                String hostAlias = entry.getKey();
                if (filter.matches(hostAlias)) {
                    SshHostConfiguration config = entry.getValue();
                    answer.add(config);
                }
            }
        }
        return answer;
    }

    @Override
    public void destroyContainers(String profile, int count, List<Container> containers) {
        // TODO

    }
}
