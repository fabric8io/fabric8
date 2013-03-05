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

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.fusesource.fabric.api.Container;
import org.fusesource.fabric.api.FabricService;
import org.fusesource.fabric.api.Profile;
import org.fusesource.fabric.api.Version;
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

    private Map<String, String[]> featureArguments = new LinkedHashMap<String, String[]>();

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
    public void assertProvisionedFeature(String containerName, String featureNames, String profileName, String expectedSymbolicNames) throws Exception {
        System.out.println("Testing profile:"+profileName+" on container:"+containerName+" by adding feature:"+featureNames);
        FabricService fabricService = getFabricService();
        //We set container to default to clean the container up.
        containerSetProfile(containerName, "default");
        Container container = fabricService.getContainer(containerName);
        Version version = container.getVersion();
        Profile targetProfile = fabricService.getProfile(version.getName(), profileName);
        List<String> originalFeatures = targetProfile.getFeatures();
        List<String> testFeatures = new ArrayList(originalFeatures.size());
        Collections.copy(originalFeatures,testFeatures);
        for (String featureName : featureNames.split(" ")) {
            testFeatures.add(featureNames);
        }

        targetProfile.setFeatures(testFeatures);
        //Test the modified profile.
        containerSetProfile(containerName, profileName);
        for (String symbolicName : expectedSymbolicNames.split(" ")) {
            String bundles = executeCommand("container-connect -u admin -p admin " + containerName + " osgi:list -s -t 0 | grep " + symbolicName);
            Assert.assertNotNull(bundles);
            Assert.assertTrue(bundles.contains(symbolicName));
            System.out.println(bundles);
        }
        //We set the container to default to clean up the profile.
        containerSetProfile(containerName, "default");
        targetProfile.setFeatures(originalFeatures);
    }

    @Test
    public void testFeatures() throws Exception {
        String feature = System.getProperty("feature");
        if (feature != null && !feature.isEmpty() && featureArguments.containsKey(feature)) {
            String[] arguments = featureArguments.get(feature);
            Assert.assertEquals("Feature "+feature+" should have been prepared with 4 arguments", 4, arguments.length);
            assertProvisionedFeature(arguments[0],arguments[1],arguments[2],arguments[3]);
        } else {
            for (Map.Entry<String,String[]> entry : featureArguments.entrySet())  {
                feature = entry.getKey();
                String[] arguments = entry.getValue();
                Assert.assertEquals("Feature "+feature+" should have been prepared with 4 arguments", 4, arguments.length);
                assertProvisionedFeature(arguments[0],arguments[1],arguments[2],arguments[3]);
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
    public void prepareFeaturesForTesting(String containerName, String featureName, String profileName, String expectedSymbolicName)  {
        featureArguments.put(featureName, new String[] {containerName,featureName,profileName,expectedSymbolicName});
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
