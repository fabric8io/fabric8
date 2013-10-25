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

import org.fusesource.fabric.api.Container;
import org.fusesource.fabric.api.Profile;
import org.fusesource.fabric.itests.paxexam.support.ContainerBuilder;
import org.fusesource.fabric.itests.paxexam.support.FabricTestSupport;
import org.fusesource.fabric.itests.paxexam.support.Provision;
import org.fusesource.fabric.zookeeper.ZkPath;
import org.junit.After;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.MavenUtils;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.Configuration;
import org.ops4j.pax.exam.junit.ExamReactorStrategy;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.ops4j.pax.exam.options.DefaultCompositeOption;
import org.ops4j.pax.exam.spi.reactors.AllConfinedStagedReactorFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static org.apache.karaf.tooling.exam.options.KarafDistributionOption.editConfigurationFilePut;
import static org.fusesource.fabric.zookeeper.utils.ZooKeeperUtils.setData;

@RunWith(JUnit4TestRunner.class)
@ExamReactorStrategy(AllConfinedStagedReactorFactory.class)
@Ignore("[FABRIC-644] Fix fabric smoke FabricDosgiCamelTest")
public class FabricDosgiCamelTest extends FabricTestSupport {

    @After
    public void tearDown() throws InterruptedException {
        ContainerBuilder.destroy();
    }

    @Test
    public void testFeatureProvisioning() throws Exception {
        System.err.println(executeCommand("fabric:create -n root"));
        waitForFabricCommands();

        Set<Container> containers = ContainerBuilder.create(2).withName("dosgi").withProfiles("example-dosgi-camel").assertProvisioningResult().build();
        List<Container> containerList = new ArrayList<Container>(containers);
        List<Container> dosgiProviderContainers = containerList.subList(0, containerList.size() / 2);
        List<Container> dosgiCamelContainers = containerList.subList(containerList.size() / 2, containerList.size());



        for (Container c : dosgiProviderContainers) {
            setData(getCurator(), ZkPath.CONTAINER_PROVISION_RESULT.getPath(c.getId()), "changing profile");
            Profile p = c.getVersion().getProfile("example-dosgi-camel.provider");
            c.setProfiles(new Profile[]{p});
        }

        for (Container c : dosgiCamelContainers) {
            setData(getCurator(), ZkPath.CONTAINER_PROVISION_RESULT.getPath(c.getId()), "changing profile");
            Profile p = c.getVersion().getProfile("example-dosgi-camel.consumer");
            c.setProfiles(new Profile[]{p});
        }

        Provision.provisioningSuccess(dosgiProviderContainers, PROVISION_TIMEOUT);
        Provision.provisioningSuccess(dosgiCamelContainers, PROVISION_TIMEOUT);

        Thread.sleep(20000L);
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
                //debugConfiguration("5005",false),
                editConfigurationFilePut("etc/system.properties", "fabric.version", MavenUtils.asInProject().getVersion(GROUP_ID, ARTIFACT_ID))
        };
    }
}
