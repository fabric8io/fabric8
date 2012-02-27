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
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.linkedin.zookeeper.client.IZKClient;
import org.openengsb.labs.paxexam.karaf.options.LogLevelOption;
import org.ops4j.pax.exam.MavenUtils;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.Configuration;
import org.ops4j.pax.exam.junit.ExamReactorStrategy;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.ops4j.pax.exam.spi.reactors.AllConfinedStagedReactorFactory;


import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static org.openengsb.labs.paxexam.karaf.options.KarafDistributionOption.debugConfiguration;
import static org.openengsb.labs.paxexam.karaf.options.KarafDistributionOption.editConfigurationFilePut;
import static org.openengsb.labs.paxexam.karaf.options.KarafDistributionOption.keepRuntimeFolder;
import static org.openengsb.labs.paxexam.karaf.options.KarafDistributionOption.logLevel;

@RunWith(JUnit4TestRunner.class)
@ExamReactorStrategy(AllConfinedStagedReactorFactory.class)
public class FabricDosgiCamelTest extends FabricCommandsTestSupport {

    @After
    public void tearDown() throws InterruptedException {
       destroyChildContainer("dosgi-camel");
       destroyChildContainer("dosgi-provider");
    }

    @Test
    public void testFeatureProvisioning() throws Exception {
        FabricService fabricService = getOsgiService(FabricService.class);
        assertNotNull(fabricService);

        System.err.println(executeCommand("echo $APPLICATION"));
        System.err.println(executeCommand("fabric:ensemble-create root"));
        Thread.sleep(DEFAULT_WAIT);

        //Wait for zookeeper service to become available.
        IZKClient zooKeeper = getOsgiService(IZKClient.class);

        System.err.println(executeCommand("shell:source mvn:org.fusesource.fabric/fuse-fabric/"+System.getProperty("fabric.version")+"/karaf/dosgi"));
        waitForProvisionSuccess(fabricService.getContainer("dosgi-provider"), PROVISION_TIMEOUT);
        waitForProvisionSuccess(fabricService.getContainer("dosgi-camel"), PROVISION_TIMEOUT);
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
                fabricDistributionConfiguration(), keepRuntimeFolder(),
                //debugConfiguration("5005",true),
                editConfigurationFilePut("etc/system.properties", "fabric.version", MavenUtils.asInProject().getVersion(GROUP_ID,ARTIFACT_ID)),
                logLevel(LogLevelOption.LogLevel.ERROR)};
    }
}
