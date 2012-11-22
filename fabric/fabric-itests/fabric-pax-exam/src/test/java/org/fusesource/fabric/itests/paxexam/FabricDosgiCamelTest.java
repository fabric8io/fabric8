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

import org.fusesource.fabric.api.FabricService;
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

import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static org.openengsb.labs.paxexam.karaf.options.KarafDistributionOption.editConfigurationFilePut;

@RunWith(JUnit4TestRunner.class)
@ExamReactorStrategy(AllConfinedStagedReactorFactory.class)
public class FabricDosgiCamelTest extends FabricTestSupport {

    @After
    public void tearDown() throws InterruptedException {
       destroyChildContainer("dosgi-camel");
       destroyChildContainer("dosgi-provider");
    }

    @Test
    public void testFeatureProvisioning() throws Exception {
        FabricService fabricService = getOsgiService(FabricService.class);
        assertNotNull(fabricService);

        System.err.println(executeCommand("fabric:create -n root"));

        addStagingRepoToDefaultProfile();

        //Wait for zookeeper service to become available.
        IZKClient zooKeeper = getOsgiService(IZKClient.class);

        executeCommand("fabric:profile-create --parents dosgi dosgi-provider");
        executeCommand("fabric:profile-edit --repositories mvn:org.fusesource.examples.fabric-camel-dosgi/features/"+System.getProperty("fabric.version")+"/xml/features dosgi-provider");
        executeCommand("fabric:profile-edit --features fabric-example-dosgi dosgi-provider");


        executeCommand("fabric:profile-create --parents dosgi dosgi-camel");
        executeCommand("fabric:profile-edit --repositories mvn:org.fusesource.examples.fabric-camel-dosgi/features/"+System.getProperty("fabric.version")+"/xml/features dosgi-camel");
        executeCommand("fabric:profile-edit --features fabric-example-camel-dosgi dosgi-camel");

        createAndAssertChildContainer("dosgi-provider", "root", "dosgi-provider");
        createAndAssertChildContainer("dosgi-camel", "root", "dosgi-camel");

        String response = executeCommand("fabric:container-connect dosgi-camel log:display | grep \"Message from distributed service to\"");
        System.err.println(executeCommand("fabric:container-connect dosgi-camel camel:route-info fabric-client"));
        assertNotNull(response);
        System.err.println(response);
        String[] lines = response.split("\n");
        assertTrue("At least one message is expected", lines.length >= 1);
    }

    @Configuration
    public Option[] config() {
        return new Option[]{
                new DefaultCompositeOption(fabricDistributionConfiguration()),
                //debugConfiguration("5005",true),
                editConfigurationFilePut("etc/system.properties", "fabric.version", MavenUtils.asInProject().getVersion(GROUP_ID,ARTIFACT_ID))
        };
    }
}
