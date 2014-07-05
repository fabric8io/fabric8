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
package io.fabric8.itests.basic;

import io.fabric8.api.FabricRequirements;
import io.fabric8.api.FabricService;
import io.fabric8.api.ProfileRequirements;
import io.fabric8.api.ServiceProxy;
import io.fabric8.itests.paxexam.support.FabricTestSupport;

import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.options.DefaultCompositeOption;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerMethod;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerMethod.class)
public class ProfileScalingTest extends FabricTestSupport {

    @Test
    public void testProfileScaling() throws Exception {
        System.out.println(executeCommand("fabric:create -n --wait-for-provisioning"));
        //System.out.println(executeCommand("shell:info"));
        //System.out.println(executeCommand("fabric:info"));
        //System.out.println(executeCommand("fabric:profile-list"));

        ServiceProxy<FabricService> fabricProxy = ServiceProxy.createServiceProxy(bundleContext, FabricService.class);
        try {
            FabricService fabricService = fabricProxy.getService();
            waitForFabricCommands();
            String profile = "mq-amq";
            Integer expected = 1;
            boolean changed = fabricService.scaleProfile(profile, expected);
            assertProfileMinimumSize(fabricService, profile, expected);

            // lets call the scale method again, should have no effect as already requirements are updated
            // and we've not started an auto-scaler yet
            changed = fabricService.scaleProfile(profile, expected);
            assertProfileMinimumSize(fabricService, profile, expected);
            Assert.assertEquals("should not have changed!", false, changed);

            changed = fabricService.scaleProfile(profile, 2);
            assertProfileMinimumSize(fabricService, profile, 2);

            // now lets scale down
            changed = fabricService.scaleProfile(profile, -1);

            // since we have no instances right now, scaling down just removes the minimumInstances requirements ;)
            assertProfileMinimumSize(fabricService, profile, null);
        } finally {
            fabricProxy.close();
        }
    }

    protected void assertProfileMinimumSize(FabricService fabricService, String profile, Integer expected) throws IOException {
        // we need a little slack to have fabric provision this so lets retry up till 3 times
        boolean done = false;
        int tries = 0;
        while (!done) {
            try {
                doAssertProfileMinimumSize(fabricService, profile, expected);
                done = true;
            } catch (Error e) {
                if (++tries > 3) {
                    throw e;
                }
            }
        }
    }

    protected void doAssertProfileMinimumSize(FabricService fabricService, String profile, Integer expected) throws IOException {
        // lests add a little but of time to make sure that the ZK / Git cache has updated correctly
        try {
            Thread.sleep(2000);
        } catch (Exception e) {
            // ignore
        }
        FabricRequirements requirements = fabricService.getRequirements();
        ProfileRequirements profileRequirements = requirements.getOrCreateProfileRequirement(profile);
        Assert.assertNotNull("Should have profile requirements for profile " + profile, profileRequirements);
        Assert.assertEquals("profile " + profile + " minimum instances", expected, profileRequirements.getMinimumInstances());
        System.out.println("Profile " + profile + " now has requirements " + profileRequirements);
    }

    @Configuration
    public Option[] config() {
        return new Option[] { new DefaultCompositeOption(fabricDistributionConfiguration()), };
    }
}
