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

import org.fusesource.fabric.api.Container;
import org.fusesource.fabric.api.FabricService;
import org.fusesource.fabric.api.Profile;
import org.fusesource.fabric.itests.paxexam.support.ContainerBuilder;
import org.fusesource.fabric.itests.paxexam.support.Provision;
import org.fusesource.fabric.zookeeper.IZKClient;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.MavenUtils;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.Configuration;
import org.ops4j.pax.exam.junit.ExamReactorStrategy;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.ops4j.pax.exam.options.DefaultCompositeOption;
import org.ops4j.pax.exam.spi.reactors.AllConfinedStagedReactorFactory;

import javax.inject.Inject;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static org.openengsb.labs.paxexam.karaf.options.KarafDistributionOption.editConfigurationFilePut;

@RunWith(JUnit4TestRunner.class)
@ExamReactorStrategy(AllConfinedStagedReactorFactory.class)
public class FabricDosgiCamelTest extends FabricTestSupport {

    @After
    public void tearDown() throws InterruptedException {
        ContainerBuilder.destroy();
    }

    @Test
    public void testFeatureProvisioning() throws Exception {
        System.err.println(executeCommand("fabric:create -n root"));
        executeCommand("fabric:profile-create --parents dosgi dosgi-provider");
        executeCommand("fabric:profile-edit --repositories mvn:org.fusesource.examples.fabric-camel-dosgi/features/" + System.getProperty("fabric.version") + "/xml/features dosgi-provider");
        executeCommand("fabric:profile-edit --features fabric-example-dosgi dosgi-provider");


        executeCommand("fabric:profile-create --parents dosgi --parents camel dosgi-camel");
        executeCommand("fabric:profile-edit --repositories mvn:org.fusesource.examples.fabric-camel-dosgi/features/" + System.getProperty("fabric.version") + "/xml/features dosgi-camel");
        executeCommand("fabric:profile-edit --features fabric-example-camel-dosgi dosgi-camel");

        Set<Container> containers = ContainerBuilder.create(2).withName("dosgi").withProfiles("dosgi").assertProvisioningResult().build();
        List<Container> containerList = new ArrayList<Container>(containers);
        List<Container> dosgiProviderContainers = containerList.subList(0, containerList.size() / 2);
        List<Container> dosgiCamelContainers = containerList.subList(containerList.size() / 2, containerList.size());

        for (Container c : dosgiProviderContainers) {
            Profile p = getFabricService().getProfile(c.getVersion().getName(), "dogi-provider");
            c.setProfiles(new Profile[]{p});
        }

        for (Container c : dosgiCamelContainers) {
            Profile p = getFabricService().getProfile(c.getVersion().getName(), "dogi-camel");
            c.setProfiles(new Profile[]{p});
        }

        Provision.assertSuccess(dosgiProviderContainers, PROVISION_TIMEOUT);
        Provision.assertSuccess(dosgiCamelContainers, PROVISION_TIMEOUT);

        for (Container c : dosgiCamelContainers) {
            String response = executeCommand("fabric:container-connect -u admin -p admin " + c.getId() + " log:display | grep \"Message from distributed service to\"");
            System.err.println(executeCommand("fabric:container-connect -u admin -p admin " + c.getId() + " camel:route-info fabric-client"));
            assertNotNull(response);
            System.err.println(response);
            String[] lines = response.split("\n");
            assertTrue("At least one message is expected", lines.length >= 1);
        }
    }

    @Configuration
    public Option[] config() {
        return new Option[]{
                new DefaultCompositeOption(fabricDistributionConfiguration()),
                //debugConfiguration("5005",true),
                editConfigurationFilePut("etc/system.properties", "fabric.version", MavenUtils.asInProject().getVersion(GROUP_ID, ARTIFACT_ID))
        };
    }
}
