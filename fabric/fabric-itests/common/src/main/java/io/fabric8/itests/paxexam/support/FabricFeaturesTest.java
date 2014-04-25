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
package io.fabric8.itests.paxexam.support;

import static io.fabric8.zookeeper.utils.ZooKeeperUtils.setData;
import io.fabric8.api.Container;
import io.fabric8.api.FabricService;
import io.fabric8.api.Profile;
import io.fabric8.api.Version;
import io.fabric8.zookeeper.ZkPath;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.apache.curator.framework.CuratorFramework;
import org.junit.Assert;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.Configuration;
import org.ops4j.pax.exam.options.DefaultCompositeOption;

/**
 * Tests various Fabric Features.
 */
public abstract class FabricFeaturesTest extends FabricTestSupport {

    private final Map<String, String[]> featureArguments = new LinkedHashMap<String, String[]>();
    private final Set<Container> targetContainers = new HashSet<Container>();

    private int maxTry = 3;

    /**
     * Adds a feature to the profile and tests it on the container.
     * <p>Note:</p> Before and after the test the container moves to default profile.
     */
    protected void assertProvisionedFeature(FabricService fabricService, CuratorFramework curator, Set<? extends Container> containers, String featureNames, String profileName, final String expectedSymbolicNames) throws Exception {
        StringBuilder sb = new StringBuilder();
        sb.append("[ ");
        for (Container container : containers) {
            sb.append(container.getId()).append(" ");
        }
        sb.append("]");

        System.out.println("Testing profile:" + profileName + " on container:" + sb.toString() + " by adding feature:" + featureNames);
        Version version = fabricService.getDefaultVersion();

        Profile defaultProfile = version.getProfile("default");
        Profile targetProfile = version.getProfile(profileName);

        for (Container container : containers) {
            //We set container to default to clean the container up.
            container.setProfiles(new Profile[]{defaultProfile});
        }
        Provision.containerStatus(containers, PROVISION_TIMEOUT);

        for (String featureName : featureNames.split(" ")) {
            System.out.println(executeCommand("fabric:profile-edit --features "+featureName+" "+targetProfile.getId()));
        }

        System.out.println(executeCommand("fabric:profile-display "+ profileName));

        for (Container container : containers) {
            //Test the modified profile.
            setData(curator, ZkPath.CONTAINER_PROVISION_RESULT.getPath(container.getId()), "switching profile");
            container.setProfiles(new Profile[]{targetProfile});
        }

        Provision.containerStatus(containers, PROVISION_TIMEOUT);
        System.out.println(executeCommand("fabric:container-list"));

        Assert.assertTrue(Provision.waitForCondition(containers, new ContainerCondition() {
            @Override
            public Boolean checkConditionOnContainer(Container c) {
                for (String symbolicName : expectedSymbolicNames.split(" ")) {
                    String bundles = executeCommand("container-connect -u admin -p admin " + c.getId() + " osgi:list -s -t 0 | grep " + symbolicName);
                    if (bundles != null) {
                        return bundles.contains(symbolicName);
                    }
                } return false;
            }
        }, PROVISION_TIMEOUT));

        for (Container container : containers) {
            //We set the container to default to clean up the profile.
            if (!defaultProfile.configurationEquals(targetProfile)) {
                setData(curator, ZkPath.CONTAINER_PROVISION_RESULT.getPath(container.getId()), "switching profile");
            }
            container.setProfiles(new Profile[]{defaultProfile});
        }

        Provision.containerStatus(containers, PROVISION_TIMEOUT);
        for (String featureName : featureNames.split(" ")) {
            System.out.println(executeCommand("fabric:profile-edit --delete --features "+featureName+" "+targetProfile.getId()));
        }
    }

    protected void assertFeatures(FabricService fabricService, CuratorFramework curator) throws Exception {
        String feature = System.getProperty("feature");
        if (feature != null && !feature.isEmpty() && featureArguments.containsKey(feature)) {
            String[] arguments = featureArguments.get(feature);
            Assert.assertEquals("Feature " + feature + " should have been prepared with 4 arguments", 3, arguments.length);
            assertProvisionedFeature(fabricService, curator, targetContainers, arguments[0], arguments[1], arguments[2]);
        } else {
            for (Map.Entry<String, String[]> entry : featureArguments.entrySet()) {
                feature = entry.getKey();
                String[] arguments = entry.getValue();
                Assert.assertEquals("Feature " + feature + " should have been prepared with 4 arguments", 3, arguments.length);
                assertProvisionedFeature(fabricService, curator, targetContainers, arguments[0], arguments[1], arguments[2]);
            }
        }
    }

    /**
     * Adds a feature to the profile and tests it on the container.
     * <p>Note:</p> Before and after the test the container moves to default profile.
     */
    protected void prepareFeaturesForTesting(Set<? extends Container> containers, String featureName, String profileName, String expectedSymbolicName) {
        targetContainers.addAll(containers);
        featureArguments.put(featureName, new String[]{ featureName, profileName, expectedSymbolicName});
    }

    @Configuration
    public Option[] config() {
        return new Option[]{
                new DefaultCompositeOption(fabricDistributionConfiguration()),
                //debugConfiguration("5005",false),
                copySystemProperty("feature")
        };
    }
}
