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
import io.fabric8.api.Profiles;
import io.fabric8.api.SshHostConfiguration;
import io.fabric8.api.SshScalingRequirements;
import io.fabric8.common.util.Filter;
import io.fabric8.common.util.Filters;
import io.fabric8.common.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

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
        HostProfileCounter hostProfileCounter = new HostProfileCounter();
        Map<String,CountingMap> hostToProfileCounts = createHostToProfileScaleMap(fabricService, containers, request, hostProfileCounter);

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
        SortedSet<LoadSortedSshHostConfiguration> sortedHostConfigurations = filterHosts(requirements, profileRequirements, sshScalingRequirements, hostProfileCounter);
        SshHostConfiguration sshHostConfig = null;
        if (!sortedHostConfigurations.isEmpty()) {
            LoadSortedSshHostConfiguration first = sortedHostConfigurations.first();
            sshHostConfig = first.getHostConfiguration();
        }
        if (sshHostConfig == null) {
            LOG.warn("Could not create version " + request.getVersion() + " profile " + request.getProfile() + " as no matching hosts could be found for " + sshScalingRequirements);
            return null;
        }
        builder.configure(sshHostConfig, requirements, profileRequirements);
        return builder;
    }


    protected Map<String,CountingMap> createHostToProfileScaleMap(FabricService fabricService, Container[] containers, AutoScaleRequest request, HostProfileCounter hostContainerCounts) {
        Map<String, CountingMap> answer = new HashMap<String, CountingMap>();
        FabricRequirements requirements = request.getFabricRequirements();
        Map<String, SshHostConfiguration> sshHostsMap = requirements.getSshHostsMap();
        if (sshHostsMap != null && containers != null) {
            if (containers != null) {
                for (Container container : containers) {
                    String hostAlias = findHostAlias(sshHostsMap, container);
                    if (hostAlias != null) {
                        hostContainerCounts.incrementContainers(hostAlias);
                        List<String> profileIds = Profiles.profileIds(container.getProfiles());
                        hostContainerCounts.incrementProfilesCount(hostAlias, profileIds);
                    }
                }
            }
        }
        return answer;
    }

    /**
     * Tries to find the host alias for the given container by matching on local and public host names and IP addresses etc
     */
    protected String findHostAlias(Map<String, SshHostConfiguration> sshHostsMap, Container container) {
        for (Map.Entry<String, SshHostConfiguration> entry : sshHostsMap.entrySet()) {
            SshHostConfiguration config = entry.getValue();
            String hostName = config.getHostName();
            if (Objects.equal(hostName, container.getLocalHostname()) ||
                    Objects.equal(hostName, container.getLocalIp()) ||
                    Objects.equal(hostName, container.getPublicHostname()) ||
                    Objects.equal(hostName, container.getIp()) ||
                    Objects.equal(hostName, container.getManualIp())) {
                return entry.getKey();
            }
        }
        return null;
    }

    /**
     * Filters the available host configurations
     */
    public static SortedSet<LoadSortedSshHostConfiguration> filterHosts(FabricRequirements requirements, ProfileRequirements profileRequirements, SshScalingRequirements sshScalingRequirements, HostProfileCounter hostProfileCounter) {
        SortedSet<LoadSortedSshHostConfiguration> answer = new TreeSet<>();
        int index = 0;
        Map<String, SshHostConfiguration> hosts = requirements.getSshHostsMap();
        if (hosts != null) {
            Filter<String> filter = sshScalingRequirements != null ? Filters.createStringFilters(sshScalingRequirements.getHostPatterns()) : Filters.<String>trueFilter();
            Set<Map.Entry<String, SshHostConfiguration>> entries = hosts.entrySet();
            for (Map.Entry<String, SshHostConfiguration> entry : entries) {
                String hostAlias = entry.getKey();
                if (filter.matches(hostAlias)) {
                    SshHostConfiguration config = entry.getValue();
                    String profile = profileRequirements.getProfile();
                    boolean valid = true;
                    Integer maximumContainerCount = config.getMaximumContainerCount();
                    if (maximumContainerCount != null) {
                        int count = hostProfileCounter.containerCount(hostAlias);
                        if (count >= maximumContainerCount) {
                            valid = false;
                        }
                    }
                    if (valid) {
                        Integer maximumInstancesPerHost = profileRequirements.getMaximumInstancesPerHost();
                        if (maximumInstancesPerHost != null) {
                            int count = hostProfileCounter.profileCount(hostAlias, profile);
                            if (count >= maximumInstancesPerHost) {
                                valid = false;
                            }
                        }
                    }
                    if (valid) {
                        answer.add(new LoadSortedSshHostConfiguration(hostAlias, config, profile, hostProfileCounter, index++));
                    }
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
