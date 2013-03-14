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

package org.fusesource.fabric.itests.paxexam;

import java.util.*;

import org.fusesource.fabric.api.Container;
import org.fusesource.fabric.api.FabricService;
import org.fusesource.fabric.api.Profile;
import org.fusesource.fabric.api.Version;
import org.fusesource.fabric.itests.paxexam.support.Provision;
import org.fusesource.fabric.zookeeper.IZKClient;
import org.fusesource.fabric.zookeeper.ZkPath;
import org.fusesource.fabric.zookeeper.utils.ZooKeeperUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.Configuration;
import org.ops4j.pax.exam.options.DefaultCompositeOption;

import static org.openengsb.labs.paxexam.karaf.options.KarafDistributionOption.*;

/**
 * Tests various Fabric Features.
 */
public abstract class FabricFeaturesTest extends FabricTestSupport {

    private final Map<String, String[]> featureArguments = new LinkedHashMap<String, String[]>();
    private final Set<Container> targetContainers = new HashSet<Container>();

    @After
    public void tearDown() throws InterruptedException {
    }

    /**
     * Adds a feature to the profile and tests it on the container.
     * <p>Note:</p> Before and after the test the container moves to default profile.
     *
     * @param featureNames
     * @param profileName
     * @param expectedSymbolicNames
     */
    public void assertProvisionedFeature(Set<Container> containers, String featureNames, String profileName, String expectedSymbolicNames) throws Exception {
        IZKClient zooKeeper = getZookeeper();
        StringBuilder sb = new StringBuilder();
        sb.append("[ ");
        for (Container container : containers) {
            sb.append(container.getId()).append(" ");
        }
        sb.append("]");

        System.out.println("Testing profile:" + profileName + " on container:" + sb.toString() + " by adding feature:" + featureNames);
        FabricService fabricService = getFabricService();
        Version version = fabricService.getDefaultVersion();

        Profile defaultProfile = fabricService.getProfile(version.getName(), "default");
        Profile targetProfile = fabricService.getProfile(version.getName(), profileName);

        for (Container container : containers) {
            //We set container to default to clean the container up.
            container.setProfiles(new Profile[]{defaultProfile});
        }
        Provision.waitForContainerStatus(containers, PROVISION_TIMEOUT);

        for (String featureName : featureNames.split(" ")) {
            System.out.println(executeCommand("fabric:profile-edit --features "+featureName+" "+targetProfile.getId()));
        }

        for (Container container : containers) {
            //Test the modified profile.
            if (!defaultProfile.configurationEquals(targetProfile)) {
                ZooKeeperUtils.set(getZookeeper(), ZkPath.CONTAINER_PROVISION_RESULT.getPath(container.getId()), "switching profile");
            }
            container.setProfiles(new Profile[]{targetProfile});
            //containerSetProfile(container.getId(), profileName, false);
        }

        Provision.waitForContainerStatus(containers, PROVISION_TIMEOUT);
        System.out.println(executeCommand("fabric:profile-display "+ profileName));
        System.out.println(executeCommand("fabric:container-list"));

        for (Container container : containers) {
            for (String symbolicName : expectedSymbolicNames.split(" ")) {
                System.out.println( executeCommand("container-connect -u admin -p admin " + container.getId() + " osgi:list -s -t 0"));
                String bundles = executeCommand("container-connect -u admin -p admin " + container.getId() + " osgi:list -s -t 0 | grep " + symbolicName);
                System.out.flush();
                Assert.assertNotNull(bundles);
                Assert.assertTrue("Expected to find symbolic name:" + symbolicName, bundles.contains(symbolicName));
                System.out.println(bundles);
            }
        }

        for (Container container : containers) {
            //We set the container to default to clean up the profile.
            if (!defaultProfile.configurationEquals(targetProfile)) {
                ZooKeeperUtils.set(getZookeeper(), ZkPath.CONTAINER_PROVISION_RESULT.getPath(container.getId()), "switching profile");
            }
            container.setProfiles(new Profile[]{defaultProfile});
        }

        Provision.waitForContainerStatus(containers, PROVISION_TIMEOUT);
        for (String featureName : featureNames.split(" ")) {
            System.out.println(executeCommand("fabric:profile-edit --delete --features "+featureName+" "+targetProfile.getId()));
        }
    }

    @Test
    public void testFeatures() throws Exception {
        String feature = System.getProperty("feature");
        if (feature != null && !feature.isEmpty() && featureArguments.containsKey(feature)) {
            String[] arguments = featureArguments.get(feature);
            Assert.assertEquals("Feature " + feature + " should have been prepared with 4 arguments", 3, arguments.length);
            assertProvisionedFeature(targetContainers, arguments[0], arguments[1], arguments[2]);
        } else {
            for (Map.Entry<String, String[]> entry : featureArguments.entrySet()) {
                feature = entry.getKey();
                String[] arguments = entry.getValue();
                Assert.assertEquals("Feature " + feature + " should have been prepared with 4 arguments", 3, arguments.length);
                assertProvisionedFeature(targetContainers, arguments[0], arguments[1], arguments[2]);
            }
        }
    }

    /**
     * Adds a feature to the profile and tests it on the container.
     * <p>Note:</p> Before and after the test the container moves to default profile.
     *
     * @param featureName
     * @param profileName
     * @param expectedSymbolicName
     */
    public void prepareFeaturesForTesting(Set<Container> containers, String featureName, String profileName, String expectedSymbolicName) {
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
