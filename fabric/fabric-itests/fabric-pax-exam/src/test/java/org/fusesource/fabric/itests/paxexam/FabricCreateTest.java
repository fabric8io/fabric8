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

import java.util.LinkedList;
import java.util.List;
import org.apache.maven.profiles.ProfilesConversionUtils;
import org.fusesource.fabric.api.FabricService;
import org.fusesource.fabric.api.Profile;
import org.fusesource.fabric.zookeeper.IZKClient;
import org.fusesource.fabric.zookeeper.ZkDefs;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.Configuration;
import org.ops4j.pax.exam.junit.ExamReactorStrategy;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.ops4j.pax.exam.spi.reactors.AllConfinedStagedReactorFactory;


import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;

@RunWith(JUnit4TestRunner.class)
@ExamReactorStrategy(AllConfinedStagedReactorFactory.class)
public class FabricCreateTest extends FabricTestSupport {

    @Test
    public void testImportedProfiles() throws Exception {
        FabricService fabricService = getFabricService();
        assertNotNull(fabricService);

        System.err.println(executeCommand("fabric:create -n"));

        Profile karafProfile = fabricService.getProfile(ZkDefs.DEFAULT_VERSION,"karaf");
        assertNotNull(karafProfile);

        Profile camelProfile = fabricService.getProfile(ZkDefs.DEFAULT_VERSION,"camel");
        assertNotNull(camelProfile);

        Profile activeMq = fabricService.getProfile(ZkDefs.DEFAULT_VERSION,"mq");
        assertNotNull(activeMq);
    }


    @Test
    public void testCreateWithProfileSelextion() throws Exception {
        FabricService fabricService = getFabricService();
        assertNotNull(fabricService);

        System.err.println(executeCommand("fabric:create -n --profile camel"));

        Profile[] profiles = fabricService.getCurrentContainer().getProfiles();
        List<String> profileNames = new LinkedList<String>();
        for (Profile profile:profiles) {
            profileNames.add(profile.getId());
        }

        assertTrue(profileNames.contains("fabric"));
        assertTrue(profileNames.contains("camel"));
    }

    @Configuration
    public Option[] config() {
        return fabricDistributionConfiguration();
    }
}
