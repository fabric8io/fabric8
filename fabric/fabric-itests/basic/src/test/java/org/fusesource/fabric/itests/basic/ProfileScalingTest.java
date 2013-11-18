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

package org.fusesource.fabric.itests.basic;

import static junit.framework.Assert.assertNotNull;

import java.io.IOException;

import org.fusesource.fabric.api.FabricRequirements;
import org.fusesource.fabric.api.FabricService;
import org.fusesource.fabric.api.ProfileRequirements;
import org.fusesource.fabric.itests.paxexam.support.FabricTestSupport;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.Configuration;
import org.ops4j.pax.exam.junit.ExamReactorStrategy;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.ops4j.pax.exam.options.DefaultCompositeOption;
import org.ops4j.pax.exam.spi.reactors.AllConfinedStagedReactorFactory;

@RunWith(JUnit4TestRunner.class)
@ExamReactorStrategy(AllConfinedStagedReactorFactory.class)
@Ignore("[FABRIC-727] Fix fabric basic ProfileScalingTest")
public class ProfileScalingTest extends FabricTestSupport {

        @Test
        public void testProfileScaling() throws Exception {
            System.err.println(executeCommand("fabric:create -n"));
            FabricService fabricService = getFabricService();
            assertNotNull(fabricService);
            waitForFabricCommands();
            String profile = "mq-amq";
            Integer expected = 1;
            boolean changed = fabricService.scaleProfile(profile, expected);
            assertProfileMinimumSize(profile, expected);

            // lets call the scale method again, should have no effect as already requirements are updated
            // and we've not started an auto-scaler yet
            changed = fabricService.scaleProfile(profile, expected);
            assertProfileMinimumSize(profile, expected);
            Assert.assertEquals("should not have changed!", false, changed);

            changed = fabricService.scaleProfile(profile, 2);
            assertProfileMinimumSize(profile, 2);

            // now lets scale down
            changed = fabricService.scaleProfile(profile, -1);

            // since we have no instances right now, scaling down just removes the minimumInstances requirements ;)
            assertProfileMinimumSize(profile, null);
    }

    protected void assertProfileMinimumSize(String profile, Integer expected) throws IOException {
        FabricRequirements requirements = getFabricService().getRequirements();
        ProfileRequirements profileRequirements = requirements.getOrCreateProfileRequirement(profile);
        Assert.assertNotNull("Should have profile requirements for profile " + profile, profileRequirements);
        Assert.assertEquals("profile " + profile + " minimum instances", expected, profileRequirements.getMinimumInstances());
        System.out.println("Profile " + profile + " now has requirements " + profileRequirements);
    }


    @Configuration
    public Option[] config() {
        return new Option[]{
                new DefaultCompositeOption(fabricDistributionConfiguration()),
        };
    }
}
