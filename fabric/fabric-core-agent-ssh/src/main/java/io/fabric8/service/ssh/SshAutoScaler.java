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
import io.fabric8.api.HostConfiguration;
import io.fabric8.api.NameValidator;
import io.fabric8.api.ProfileRequirements;
import io.fabric8.api.SshHostConfiguration;
import io.fabric8.api.SshScalingRequirements;
import io.fabric8.internal.autoscale.AutoScalers;
import io.fabric8.internal.autoscale.HostProfileCounter;
import io.fabric8.internal.autoscale.LoadSortedHostConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.SortedSet;

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
        FabricRequirements requirements = request.getFabricRequirements();
        List<? extends HostConfiguration> hostConfigurations = requirements.getSshHosts();
        HostProfileCounter hostProfileCounter = new HostProfileCounter();
        AutoScalers.createHostToProfileScaleMap(hostProfileCounter, hostConfigurations, containers);

        for (int i = 0; i < count; i++) {
            CreateSshContainerOptions.Builder builder = null;
            NameValidator nameValidator = Containers.createNameValidator(containers);
            String name = Containers.createContainerName(containers, profile, containerProvider.getScheme(), nameValidator);

            if (fabricService != null) {
                builder = createAutoScaleOptions(request, fabricService, hostProfileCounter);
            }
            if (builder != null) {
                final CreateSshContainerOptions.Builder configuredBuilder = builder.number(1).version(version).profiles(profile);

                CreateSshContainerOptions options = configuredBuilder.name(name).build();
                LOG.info("Creating container name " + name + " version " + version + " profile " + profile + " " + count + " container(s)");
                fabricService.createContainers(options);
            }
        }
    }

    protected CreateSshContainerOptions.Builder createAutoScaleOptions(AutoScaleRequest request, FabricService fabricService, HostProfileCounter hostProfileCounter) {
        CreateSshContainerOptions.Builder builder = chooseHostContainerOptions(request, hostProfileCounter);
        if (builder == null) return null;
        String zookeeperUrl = fabricService.getZookeeperUrl();
        String zookeeperPassword = fabricService.getZookeeperPassword();
        if (builder.getProxyUri() == null) {
            builder.proxyUri(fabricService.getMavenRepoURI());
        }
        return builder.zookeeperUrl(zookeeperUrl).zookeeperPassword(zookeeperPassword);
    }

    /**
     * This method is public for easier testing
     */
    public static CreateSshContainerOptions.Builder chooseHostContainerOptions(AutoScaleRequest request, HostProfileCounter hostProfileCounter) {
        CreateSshContainerOptions.Builder builder = CreateSshContainerOptions.builder();
        FabricRequirements requirements = request.getFabricRequirements();
        ProfileRequirements profileRequirements = request.getProfileRequirements();
        SshScalingRequirements sshScalingRequirements = profileRequirements.getSshScalingRequirements();
        List<SshHostConfiguration> hosts = requirements.getSshHosts();
        SortedSet<LoadSortedHostConfiguration<SshHostConfiguration>> sortedHostConfigurations = AutoScalers.filterHosts(profileRequirements, sshScalingRequirements, hostProfileCounter, hosts);
        SshHostConfiguration sshHostConfig = null;
        if (!sortedHostConfigurations.isEmpty()) {
            LoadSortedHostConfiguration<SshHostConfiguration> first = sortedHostConfigurations.first();
            sshHostConfig = first.getConfiguration();
        }
        if (sshHostConfig == null) {
            LOG.warn("Could not create version " + request.getVersion() + " profile " + request.getProfile() + " as no matching hosts could be found for " + sshScalingRequirements);
            request.getProfileAutoScaleStatus().noSuitableHost("" + sshScalingRequirements);
            return null;
        }
        builder.configure(sshHostConfig, requirements, profileRequirements);
        return builder;
    }

    @Override
    public void destroyContainers(String profile, int count, List<Container> containers) {
        // TODO

    }
}
