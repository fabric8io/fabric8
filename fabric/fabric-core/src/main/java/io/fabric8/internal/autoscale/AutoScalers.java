/**
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.fabric8.internal.autoscale;

import io.fabric8.api.Container;
import io.fabric8.api.FabricRequirements;
import io.fabric8.api.HostConfiguration;
import io.fabric8.api.HostScalingRequirements;
import io.fabric8.api.ProfileRequirements;
import io.fabric8.api.Profiles;
import io.fabric8.api.SshHostConfiguration;
import io.fabric8.common.util.Filter;
import io.fabric8.common.util.Filters;
import io.fabric8.common.util.Objects;
import io.fabric8.utils.CountingMap;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import static io.fabric8.common.util.Filters.trueFilter;

/**
 * A bunch of helper methods for implementing auto scalers implementations choosing & filtering configurations, hosts etc
 */
public class AutoScalers {
    public static Filter<String> createHostAliasFilter(HostScalingRequirements scalingRequirements) {
        if (scalingRequirements != null) {
            List<String> hostPatterns = scalingRequirements.getHostPatterns();
            if (hostPatterns != null && hostPatterns.size() > 0) {
                return Filters.createStringFilters(hostPatterns);
            }
        }
        return trueFilter();
    }

    public static Filter<HostConfiguration> createHostConfigFilter(HostScalingRequirements scalingRequirements) {
        if (scalingRequirements != null) {
            final List<String> matchTags = scalingRequirements.getHostTags();
            if (matchTags != null && matchTags.size() > 0) {
                return new Filter<HostConfiguration>() {
                    @Override
                    public String toString() {
                        return "Filter(HostConfiguration has tags: " + matchTags + ")";
                    }

                    @Override
                    public boolean matches(HostConfiguration hostConfiguration) {
                        List<String> tags = hostConfiguration.getTags();
                        if (tags != null) {
                            for (String matchTag : matchTags) {
                                if (!tags.contains(matchTag)) return false;
                            }
                            return true;
                        }
                        return false;
                    }
                };
            }
        }
        return trueFilter();
    }

    public static boolean isValidHost(HostConfiguration config, ProfileRequirements profileRequirements, HostProfileCounter hostProfileCounter, String hostAlias) {
        boolean valid = true;
        String profile = profileRequirements.getProfile();
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
        return valid;
    }

    public static Map<String, CountingMap> createHostToProfileScaleMap(HostProfileCounter hostContainerCounts, List<? extends HostConfiguration> hostConfigurations, Container[] containers) {
        Map<String, CountingMap> answer = new HashMap<>();
        if (containers != null) {
            if (containers != null) {
                for (Container container : containers) {
                    String hostAlias = findHostAlias(hostConfigurations, container);
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
    protected static String findHostAlias(Collection<? extends HostConfiguration> hostConfigurations, Container container) {
        for (HostConfiguration config : hostConfigurations) {
            String hostName = config.getHostName();
            if (Objects.equal(hostName, container.getLocalHostname()) ||
                    Objects.equal(hostName, container.getLocalIp()) ||
                    Objects.equal(hostName, container.getPublicHostname()) ||
                    Objects.equal(hostName, container.getIp()) ||
                    Objects.equal(hostName, container.getManualIp())) {
                return hostName;
            }
        }
        return null;
    }

    /**
     * Filters the available host configurations
     */
    public static SortedSet<LoadSortedHostConfiguration<SshHostConfiguration>> filterHosts(FabricRequirements requirements, ProfileRequirements profileRequirements, HostScalingRequirements scalingRequirements, HostProfileCounter hostProfileCounter) {
        SortedSet<LoadSortedHostConfiguration<SshHostConfiguration>> answer = new TreeSet<>();
        int index = 0;
        List<SshHostConfiguration> hosts = requirements.getSshHosts();
        Filter<String> hostFilter = createHostAliasFilter(scalingRequirements);
        Filter<HostConfiguration> configFilter = createHostConfigFilter(scalingRequirements);
        for (SshHostConfiguration config : hosts) {
            String hostName = config.getHostName();
            if (hostFilter.matches(hostName) && configFilter.matches(config)) {
                String profile = profileRequirements.getProfile();
                boolean valid = isValidHost(config, profileRequirements, hostProfileCounter, hostName);
                if (valid) {
                    answer.add(new LoadSortedHostConfiguration<>(hostName, config, profile, hostProfileCounter, index++));
                }
            }
        }
        return answer;
    }
}
