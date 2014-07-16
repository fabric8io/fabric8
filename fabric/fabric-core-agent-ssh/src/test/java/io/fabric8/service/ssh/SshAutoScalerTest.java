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
package io.fabric8.service.ssh;

import io.fabric8.api.AutoScaleRequest;
import io.fabric8.api.AutoScaleStatus;
import io.fabric8.api.FabricRequirements;
import io.fabric8.api.ProfileRequirements;
import io.fabric8.api.SshHostConfiguration;
import io.fabric8.insight.log.support.Strings;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static io.fabric8.service.ssh.SshAutoScaler.chooseHostContainerOptions;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class SshAutoScalerTest {
    private static final transient Logger LOG = LoggerFactory.getLogger(SshAutoScalerTest.class);

    String mqProfileId = "mq-default";
    String exampleProfileId = "quickstarts-karaf-camel-amq";

    @Test
    public void testMaximumProfileCountPerHost() throws Exception {
        //String hostPostfix = ".cheese.com";
        String hostPostfix = "";

        String hostSmall = "small";
        String hostMedium = "medium";
        String hostBig = "big";

        FabricRequirements requirements = new FabricRequirements();
        requirements.sshConfiguration().defaultPath("/opt/fuse").defaultUsername("root").defaultPassword("adminuser").defaultPassPhrase("cheese");
        requirements.sshHost(hostSmall).hostName(hostSmall + hostPostfix).maximumContainerCount(1);
        requirements.sshHost(hostMedium).hostName(hostMedium + hostPostfix).maximumContainerCount(2);
        requirements.sshHost(hostBig).hostName(hostBig + hostPostfix).maximumContainerCount(8);
        requirements.profile(mqProfileId).minimumInstances(2).maximumInstancesPerHost(1).sshScaling().hostPatterns("!small");
        requirements.profile(exampleProfileId).minimumInstances(5).dependentProfiles(mqProfileId);

        HostProfileCounter hostProfileCounter = assertSshAutoScale(requirements);
        assertHostHasProfileCount(hostProfileCounter, hostSmall, exampleProfileId, 1);
        assertHostHasProfileCount(hostProfileCounter, hostMedium, exampleProfileId, 1);
        assertHostHasProfileCount(hostProfileCounter, hostMedium, mqProfileId, 1);
        assertHostHasProfileCount(hostProfileCounter, hostBig, mqProfileId, 1);
        assertHostHasProfileCount(hostProfileCounter, hostBig, exampleProfileId, 3);
        dumpHostProfiles(hostProfileCounter);
    }

    public static void assertHostHasProfileCount(HostProfileCounter hostProfileCounter, String hostAlias, String profileId, int expectedCount) {
        int actual = hostProfileCounter.profileCount(hostAlias, profileId);
        assertEquals("Host " + hostAlias + " instance count for profile " + profileId, expectedCount, actual);
        if (LOG.isDebugEnabled()) {
            LOG.debug("Host " + hostAlias + " instance count for profile " + profileId + " is the expected " + actual);
        }
    }

    public static void dumpHostProfiles(HostProfileCounter hostProfileCounter) {
        for (Map.Entry<String, CountingMap> entry : hostProfileCounter.getHostToProfileCounts().entrySet()) {
            CountingMap countingMap = entry.getValue();
            String host = entry.getKey();
            LOG.info("Host " + host + " has " + countingMap);
        }
    }

    public static HostProfileCounter assertSshAutoScale(FabricRequirements requirements) {
        return assertSshAutoScale(requirements, new AutoScaleStatus());
    }

    public static HostProfileCounter assertSshAutoScale(FabricRequirements requirements, AutoScaleStatus status) {
        HostProfileCounter hostProfileCounter = new HostProfileCounter();
        String version = requirements.getVersion();
        if (Strings.isEmpty(version)) {
            version = "1.0";
        }
        List<ProfileRequirements> profileRequirements = requirements.getProfileRequirements();
        for (ProfileRequirements profileRequirement : profileRequirements) {
            Integer minimumInstances = profileRequirement.getMinimumInstances();
            if (minimumInstances != null) {
                for (int i = 0; i < minimumInstances; i++) {
                    String profileId = profileRequirement.getProfile();
                    AutoScaleRequest request = new AutoScaleRequest(null, version, profileId, 1, requirements, profileRequirement, status);
                    CreateSshContainerOptions.Builder builder = chooseHostContainerOptions(request, hostProfileCounter);
                    assertNotNull("Should have found a builder for " + profileId, builder);
                    String host = builder.getHost();
                    hostProfileCounter.incrementContainers(host);
                    hostProfileCounter.incrementProfileCount(host, profileId);
                }
            }
        }
        Map<String, CountingMap> hostToProfileCounts = hostProfileCounter.getHostToProfileCounts();
        assertProfilesUseSeparateHost(requirements, hostToProfileCounts);
        assertMaximumContainerCountNotExceeded(requirements, hostToProfileCounts);
        return hostProfileCounter;
    }

    /**
     * lets assert that no host has more than its maximum number of containers
     */
    public static void assertMaximumContainerCountNotExceeded(FabricRequirements requirements, Map<String, CountingMap> hostToProfileCounts) {
        for (Map.Entry<String, CountingMap> entry : hostToProfileCounts.entrySet()) {
            String hostName = entry.getKey();
            CountingMap counts = entry.getValue();
            int total = counts.total();
            Map<String, SshHostConfiguration> sshHostsMap = requirements.getSshHostsMap();
            assertNotNull("Should have a hosts map!", sshHostsMap);
            SshHostConfiguration hostConfiguration = sshHostsMap.get(hostName);
            assertNotNull("Should have a hosts configuration for host " + hostName, hostConfiguration);
            Integer maximumContainerCount = hostConfiguration.getMaximumContainerCount();
            if (maximumContainerCount != null) {
                assertTrue("Host " + hostName + " has a maximum container count of " + maximumContainerCount + " but was " + total, total <= maximumContainerCount);
            }
        }
    }


    /**
     * lets assert that no host has more than its maximum number of containers
     */
    public static void assertProfilesUseSeparateHost(FabricRequirements requirements, Map<String, CountingMap> hostToProfileCounts) {
        for (Map.Entry<String, CountingMap> entry : hostToProfileCounts.entrySet()) {
            String hostName = entry.getKey();
            CountingMap counts = entry.getValue();
            Set<String> keys = counts.keySet();
            for (String profileId : keys) {
                int count = counts.count(profileId);
                // lets see if we have a maximum number of profile count
                ProfileRequirements profileRequirement = requirements.getOrCreateProfileRequirement(profileId);
                Integer maximum = profileRequirement.getMaximumInstancesPerHost();
                if (maximum != null) {
                    assertTrue("Host " + hostName + " has " + count + " instances of " + profileId
                                    + " but this is configured to have a maximium of " + maximum + " per host",
                            count <= maximum
                    );
                }
            }
        }
    }

}
