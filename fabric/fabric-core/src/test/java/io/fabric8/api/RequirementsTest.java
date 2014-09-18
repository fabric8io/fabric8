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
package io.fabric8.api;

import io.fabric8.internal.RequirementsJson;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static junit.framework.Assert.assertEquals;

/**
 */
public class RequirementsTest {
    String mqProfileId = "mq-default";
    String exampleProfileId = "quickstarts-karaf-camel-amq";

    @Test
    public void saveAndLoad() throws Exception {
        List<ProfileRequirements> profiles = new ArrayList<ProfileRequirements>();
        ProfileRequirements dummy = new ProfileRequirements("dummy", 1, null, mqProfileId);
        profiles.add(dummy);
        profiles.add(new ProfileRequirements(mqProfileId, 1, 5));
        profiles.add(new ProfileRequirements(exampleProfileId, 1, null, mqProfileId));

        // lets check we can make it empty
        assertEquals(false, dummy.checkIsEmpty());

        dummy.setDependentProfiles(null);
        dummy.setMinimumInstances(0);
        assertEquals(true, dummy.checkIsEmpty());
        dummy.setMinimumInstances(null);
        assertEquals(true, dummy.checkIsEmpty());

        FabricRequirements requirements = new FabricRequirements(profiles);
        requirements.removeEmptyRequirements();

        String json = RequirementsJson.toJSON(requirements);

        System.out.println("JSON: " + json);

        FabricRequirements actual = RequirementsJson.fromJSON(json);
        List<ProfileRequirements> profileRequirements = actual.getProfileRequirements();
        assertEquals("size", 2, profileRequirements.size());

        ProfileRequirements profileMq = profileRequirements.get(0);
        assertEquals("name", mqProfileId, profileMq.getProfile());
        assertEquals("minimumInstances", new Integer(1), profileMq.getMinimumInstances());
        assertEquals("maximumInstances", new Integer(5), profileMq.getMaximumInstances());

        ProfileRequirements profileCamel = profileRequirements.get(1);
        assertEquals("name", exampleProfileId, profileCamel.getProfile());
        assertEquals("minimumInstances", new Integer(1), profileCamel.getMinimumInstances());
        assertEquals("maximumInstances", null, profileCamel.getMaximumInstances());
        assertEquals("profiles", new ArrayList<String>(Arrays.asList(mqProfileId)), profileCamel.getDependentProfiles());
    }

    @Test
    public void sshRequirements() throws Exception {
        FabricRequirements requirements = new FabricRequirements();
        requirements.sshConfiguration().defaultPath("/opt/fuse").defaultUsername("root").defaultPassword("adminuser").defaultPassPhrase("cheese");
        requirements.sshHost("foo").hostName("foo.cheese.com").path("/opt/thingy");
        requirements.sshHost("bar").hostName("bar.cheese.com").path("/opt/another");
        requirements.sshHost("another").hostName("another.cheese.com").username("foo").password("bar");
        requirements.profile(mqProfileId).minimumInstances(1).sshScaling().hostPatterns("foo");
        requirements.profile(exampleProfileId).minimumInstances(1).dependentProfiles(mqProfileId).sshScaling().hostPatterns("!foo*");

        System.out.println("SSH JSON:");
        System.out.println(RequirementsJson.toJSON(requirements));
    }


    @Test
    public void healthNumbers() throws Exception {
        ProfileRequirements requirements = new ProfileRequirements("mq", 2, 5);

        assertEquals(0.0, requirements.getHealth(0));
        assertEquals(0.5, requirements.getHealth(1));
        assertEquals(1.0, requirements.getHealth(2));
        assertEquals(1.5, requirements.getHealth(3));
    }

    @Test
    public void healthNumbersWithNoNumbers() throws Exception {
        ProfileRequirements requirements = new ProfileRequirements("mq");

        assertEquals(0.0, requirements.getHealth(0));
        assertEquals(1.0, requirements.getHealth(1));
    }
}
