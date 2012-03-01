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

import java.util.List;
import org.fusesource.fabric.api.Container;
import org.fusesource.fabric.api.FabricService;
import org.fusesource.fabric.api.Profile;
import org.fusesource.fabric.api.Version;
import org.junit.After;
import org.junit.Assert;

/**
 * Tests various Fabric Features.
 */
public abstract class FabricFeaturesTest extends FabricCommandsTestSupport {

    @After
    public void tearDown() throws InterruptedException {
    }

    /**
     * Adds a feature to the profile and tests it on the container.
     * <p>Note:</p> Before and after the test the container moves to default profile.
     *
     * @param featureName
     * @param profileName
     * @param expectedSymbolicName
     */
    public void assertProvisionedFeature(String containerName, String featureName, String profileName, String expectedSymbolicName) throws Exception {
        System.out.println("Testing profile:"+profileName+" on container:"+containerName+" by adding feature:"+featureName);
        FabricService fabricService = getOsgiService(FabricService.class);
        //We set container to default to clean the container up.
        containerSetProfile(containerName, "default");
        Container container = fabricService.getContainer(containerName);
        Version version = container.getVersion();
        Profile targetProfile = fabricService.getProfile(version.getName(), profileName);
        List<String> features = targetProfile.getFeatures();
        features.add(featureName);
        targetProfile.setFeatures(features);
        //Test the modified profile.
        containerSetProfile(containerName, profileName);
        String bundles = executeCommand("container-connect " + containerName + " osgi:list -s | grep " + expectedSymbolicName);
        Assert.assertNotNull(bundles);
        Assert.assertTrue(bundles.contains(expectedSymbolicName));
        System.out.println(bundles);
        //We set the container to default to clean up the profile.
        containerSetProfile(containerName, "default");
        features.remove(featureName);
        targetProfile.setFeatures(features);
    }

}
