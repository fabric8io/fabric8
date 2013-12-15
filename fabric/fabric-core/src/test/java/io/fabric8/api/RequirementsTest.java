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
    @Test
    public void saveAndLoad() throws Exception {
        List<ProfileRequirements> profiles = new ArrayList<ProfileRequirements>();
        ProfileRequirements dummy = new ProfileRequirements("dummy", 1, null, "mq");
        profiles.add(dummy);
        profiles.add(new ProfileRequirements("mq", 1, 5));
        profiles.add(new ProfileRequirements("example-camel", 1, null, "mq"));

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

        ProfileRequirements profileMq = profileRequirements.get(1);
        assertEquals("name", "mq", profileMq.getProfile());
        assertEquals("minimumInstances", new Integer(1), profileMq.getMinimumInstances());
        assertEquals("maximumInstances", new Integer(5), profileMq.getMaximumInstances());

        ProfileRequirements profileCamel = profileRequirements.get(0);
        assertEquals("name", "example-camel", profileCamel.getProfile());
        assertEquals("minimumInstances", new Integer(1), profileCamel.getMinimumInstances());
        assertEquals("maximumInstances", null, profileCamel.getMaximumInstances());
        assertEquals("profiles", new ArrayList<String>(Arrays.asList("mq")), profileCamel.getDependentProfiles());
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
