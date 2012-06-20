/*
 * Copyright (C) FuseSource, Inc.
 *   http://fusesource.com
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package org.fusesource.fabric.itests.paxexam;

import org.fusesource.fabric.api.FabricService;
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


import static org.openengsb.labs.paxexam.karaf.options.KarafDistributionOption.editConfigurationFileExtend;
import static org.openengsb.labs.paxexam.karaf.options.KarafDistributionOption.editConfigurationFilePut;
import static org.ops4j.pax.exam.CoreOptions.scanFeatures;

@RunWith(JUnit4TestRunner.class)
@ExamReactorStrategy(AllConfinedStagedReactorFactory.class)
public class SelfUpdateTest extends FabricTestSupport {

    private static final String FABRIC_FEATURE_REPO_URL = "mvn:org.fusesource.fabric/fuse-fabric/%s/xml/features";
    private static final String TARGET_VERSION = "7.0.0.fuse-beta-040";

    @After
    public void tearDown() throws InterruptedException {
        destroyChildContainer("child1");
    }

    @Ignore // JIRA ESB-1687
    @Test
    public void testLocalChildCreation() throws Exception {
        FabricService fabricService = getOsgiService(FabricService.class);

        System.err.println(executeCommand("fabric:create -n"));
        addStagingRepoToDefaultProfile();
        createAndAssertChildContainer("child1", "root", "default");
        String curretRepoURL = String.format(FABRIC_FEATURE_REPO_URL,System.getProperty("fabric.version"));
        String targetRepoURL = String.format(FABRIC_FEATURE_REPO_URL,TARGET_VERSION);
        System.out.println(executeCommand("fabric:version-create --parent 1.0 1.1"));
        System.out.println(executeCommand("fabric:profile-edit --delete --repositories "+curretRepoURL+" default 1.1"));
        System.out.println(executeCommand("fabric:profile-edit --repositories "+targetRepoURL+" default 1.1"));
        System.out.println(executeCommand("fabric:profile-display --version 1.1 default"));
        System.out.println(executeCommand("fabric:container-upgrade --all 1.1"));
        Thread.sleep(5000);
        waitForProvisionSuccess(fabricService.getContainer("child1"), PROVISION_TIMEOUT);
        System.out.println(executeCommand("fabric:container-list"));
    }

    @Configuration
    public Option[] config() {
        return new Option[]{
                new DefaultCompositeOption(fabricDistributionConfiguration()),
                editConfigurationFilePut("etc/system.properties", "fabric.version", MavenUtils.getArtifactVersion("org.fusesource.fabric", "fuse-fabric"))
        };
    }
}
