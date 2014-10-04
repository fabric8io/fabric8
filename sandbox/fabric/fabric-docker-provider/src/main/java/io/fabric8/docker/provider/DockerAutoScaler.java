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
package io.fabric8.docker.provider;

import io.fabric8.api.AutoScaleRequest;
import io.fabric8.api.Container;
import io.fabric8.api.ContainerAutoScaler;
import io.fabric8.api.Containers;
import io.fabric8.api.DockerHostConfiguration;
import io.fabric8.api.DockerScalingRequirements;
import io.fabric8.api.FabricRequirements;
import io.fabric8.api.FabricService;
import io.fabric8.api.HostConfiguration;
import io.fabric8.api.NameValidator;
import io.fabric8.api.ProfileRequirements;
import io.fabric8.api.SshHostConfiguration;
import io.fabric8.internal.autoscale.AutoScalers;
import io.fabric8.internal.autoscale.HostProfileCounter;
import io.fabric8.internal.autoscale.LoadSortedHostConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.SortedSet;

import static io.fabric8.internal.autoscale.AutoScalers.filterHosts;

/**
 */
public class DockerAutoScaler implements ContainerAutoScaler {
    private static final transient Logger LOG = LoggerFactory.getLogger(DockerAutoScaler.class);

    private final DockerContainerProvider containerProvider;

    public DockerAutoScaler(DockerContainerProvider containerProvider) {
        this.containerProvider = containerProvider;
    }

    @Override
    public int getWeight() {
        return 80;
    }

    @Override
    public void createContainers(AutoScaleRequest request) throws Exception {
        int count = request.getDelta();
        String profile = request.getProfile();
        String version = request.getVersion();
        FabricService fabricService = request.getFabricService();
        if (fabricService != null) {

            Container[] containers = fabricService.getContainers();
            FabricRequirements requirements = request.getFabricRequirements();
            List<? extends HostConfiguration> hostConfigurations = requirements.getDockerHosts();
            HostProfileCounter hostProfileCounter = new HostProfileCounter();
            AutoScalers.createHostToProfileScaleMap(hostProfileCounter, hostConfigurations, containers);


            // TODO this is actually generic to all providers! :)
            for (int i = 0; i < count; i++) {
                CreateDockerContainerOptions.Builder builder = createAutoScaleOptions(request, fabricService, hostProfileCounter);
                if (builder == null) {
                    return;
                }

                NameValidator nameValidator = Containers.createNameValidator(fabricService.getContainers());
                String name = Containers.createContainerName(containers, profile, containerProvider.getScheme(), nameValidator);

                CreateDockerContainerOptions options = builder.name(name).build();
                LOG.info("Creating container name " + name + " version " + version + " profile " + profile + " " + count + " container(s)");
                fabricService.createContainers(options);
            }
        } else {
            LOG.warn("Could not create version " + version + " profile " + profile + " due to missing FabricService");
        }
    }

    protected CreateDockerContainerOptions.Builder createAutoScaleOptions(AutoScaleRequest request, FabricService fabricService, HostProfileCounter hostProfileCounter) {
        String profile = request.getProfile();
        String version = request.getVersion();
        CreateDockerContainerOptions.Builder builder = chooseHostOptions(request, hostProfileCounter);
        if (builder == null) {
            return null;
        }
        String zookeeperUrl = fabricService.getZookeeperUrl();
        String zookeeperPassword = fabricService.getZookeeperPassword();
        return builder.number(1).version(version).profiles(profile).zookeeperUrl(zookeeperUrl).zookeeperPassword(zookeeperPassword);
    }

    /**
     * This method is public for easier testing
     */
    public static CreateDockerContainerOptions.Builder chooseHostOptions(AutoScaleRequest request, HostProfileCounter hostProfileCounter) {
        CreateDockerContainerOptions.Builder builder = CreateDockerContainerOptions.builder();
        FabricRequirements requirements = request.getFabricRequirements();
        ProfileRequirements profileRequirements = request.getProfileRequirements();
        DockerScalingRequirements scalingRequirements = profileRequirements.getDockerScalingRequirements();
        List<DockerHostConfiguration> hosts = requirements.getDockerHosts();
        if (hosts.isEmpty()) {
            // lets default to use the current docker container provider as there are no others configured
            return builder;
        }
        SortedSet<LoadSortedHostConfiguration<DockerHostConfiguration>> sortedHostConfigurations = filterHosts(profileRequirements, scalingRequirements, hostProfileCounter, hosts);
        DockerHostConfiguration hostConfig = null;
        if (!sortedHostConfigurations.isEmpty()) {
            LoadSortedHostConfiguration<DockerHostConfiguration> first = sortedHostConfigurations.first();
            hostConfig = first.getConfiguration();
        }
        if (hostConfig == null) {
            LOG.warn("Could not create version " + request.getVersion() + " profile " + request.getProfile() + " as no matching hosts could be found for " + scalingRequirements);
            request.getProfileAutoScaleStatus().noSuitableHost("" + scalingRequirements);
            return null;
        }
        return builder;
    }

    @Override
    public void destroyContainers(String profile, int count, List<Container> containers) {
        // TODO

    }
}
