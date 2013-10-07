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

package org.fusesource.fabric.itests.smoke;

import org.fusesource.fabric.api.FabricService;
import org.fusesource.fabric.api.Profile;
import org.fusesource.fabric.itests.paxexam.support.FabricTestSupport;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.options.DefaultCompositeOption;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerMethod;

import java.util.LinkedList;
import java.util.List;

import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerMethod.class)
public class FabricCreateTest extends FabricTestSupport {

    @Test
    public void testImportedProfiles() throws Exception {
        System.err.println(executeCommand("fabric:create -n"));
        FabricService fabricService = getFabricService();
        assertNotNull(fabricService);

        Profile karafProfile = fabricService.getDefaultVersion().getProfile("karaf");
        assertNotNull(karafProfile);

        Profile camelProfile = fabricService.getDefaultVersion().getProfile("feature-camel");
        assertNotNull(camelProfile);

        Profile activeMq = fabricService.getDefaultVersion().getProfile("mq-default");
        assertNotNull(activeMq);
    }


    @Test
    public void testCreateWithProfileSelection() throws Exception {
        System.err.println(executeCommand("fabric:create -n --profile feature-camel"));
        FabricService fabricService = getFabricService();
        assertNotNull(fabricService);


        Profile[] profiles = fabricService.getCurrentContainer().getProfiles();
        List<String> profileNames = new LinkedList<String>();
        for (Profile profile : profiles) {
            profileNames.add(profile.getId());
        }

        assertTrue(profileNames.contains("fabric"));
        assertTrue(profileNames.contains("feature-camel"));
    }

    @Configuration
    public Option[] config() {
        return new Option[]{
                new DefaultCompositeOption(fabricDistributionConfiguration()),
        };
    }
}
