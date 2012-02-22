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
import static org.openengsb.labs.paxexam.karaf.options.KarafDistributionOption.editConfigurationFileExtend;
import static org.openengsb.labs.paxexam.karaf.options.KarafDistributionOption.keepRuntimeFolder;
import static org.openengsb.labs.paxexam.karaf.options.KarafDistributionOption.logLevel;
import static org.ops4j.pax.exam.CoreOptions.systemTimeout;

@RunWith(JUnit4TestRunner.class)
@ExamReactorStrategy(AllConfinedStagedReactorFactory.class)
public class FabricFeaturesTest extends FabricCommandsTestSupport {

    @After
    public void tearDown() throws InterruptedException {
       destroyChildContainer("child1");
    }

    @Test
    public void testFeatureProvisioning() throws Exception {
        FabricService fabricService = getOsgiService(FabricService.class);
        assertNotNull(fabricService);

        System.err.println(executeCommand("fabric:create"));

        System.err.println(executeCommand("fabric:profile-list"));
        System.err.println(executeCommand("fabric:profile-display camel"));
        System.err.println(executeCommand("fabric:container-create --parent root --profile camel child1"));
        waitForProvisionSuccess(fabricService.getContainer("child1"), PROVISION_TIMEOUT);
        System.err.println(executeCommand("fabric:container-list -v"));
        System.err.println(executeCommand("fabric:container-connect child1 osgi:list -t 0"));
        String camelBundleCount = executeCommand("fabric:container-connect child1 osgi:list -t 0| grep -c -i camel");
        int count = Integer.parseInt(camelBundleCount.trim());
        assertTrue("At least one camel bundle is expected", count >= 1);
    }

    @Configuration
    public Option[] config() {
        return new Option[]{
                fabricDistributionConfiguration(), keepRuntimeFolder(),
                editConfigurationFileExtend("etc/system.properties", "fabric.version", MavenUtils.asInProject().getVersion("org.fusesource.fabric","fuse-fabric")),
                logLevel(LogLevelOption.LogLevel.ERROR)};
    }
}
