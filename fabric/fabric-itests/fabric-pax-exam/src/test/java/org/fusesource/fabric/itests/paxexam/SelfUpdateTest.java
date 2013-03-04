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

import java.io.File;

import com.google.inject.Inject;
import org.fusesource.fabric.api.FabricService;
import org.junit.After;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.openengsb.labs.paxexam.karaf.options.LogLevelOption;
import org.ops4j.pax.exam.MavenUtils;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.Configuration;
import org.ops4j.pax.exam.junit.ExamReactorStrategy;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.ops4j.pax.exam.options.DefaultCompositeOption;
import org.ops4j.pax.exam.spi.reactors.AllConfinedStagedReactorFactory;


import static org.openengsb.labs.paxexam.karaf.options.KarafDistributionOption.editConfigurationFilePut;
import static org.openengsb.labs.paxexam.karaf.options.KarafDistributionOption.karafDistributionConfiguration;
import static org.openengsb.labs.paxexam.karaf.options.KarafDistributionOption.keepRuntimeFolder;
import static org.openengsb.labs.paxexam.karaf.options.KarafDistributionOption.logLevel;
import static org.openengsb.labs.paxexam.karaf.options.KarafDistributionOption.useOwnExamBundlesStartLevel;
import static org.ops4j.pax.exam.CoreOptions.maven;

@RunWith(JUnit4TestRunner.class)
@ExamReactorStrategy(AllConfinedStagedReactorFactory.class)
public class SelfUpdateTest extends FabricTestSupport {

    private static final String FABRIC_FEATURE_REPO_URL = "mvn:org.fusesource.fabric/fuse-fabric/%s/xml/features";
    private static final String OLD_VERSION = "7.1.0.fuse-047";

    @After
    public void tearDown() throws InterruptedException {
        destroyChildContainer("child1");
    }

    @Test
    public void testDefaultProfileUpgrade() throws Exception {
        String newVersion = System.getProperty("fabric.version");

        FabricService fabricService = getFabricService();
        System.err.println(executeCommand("fabric:create"));
        addStagingRepoToDefaultProfile();
        createAndAssertChildContainer("child1", "root", "default");
        String newRepoURL = String.format(FABRIC_FEATURE_REPO_URL,newVersion);
        String oldRepoURL = String.format(FABRIC_FEATURE_REPO_URL, OLD_VERSION);
        System.out.println(executeCommand("fabric:version-create --parent 1.0 1.1"));
        System.out.println(executeCommand("fabric:profile-edit --delete --repositories "+oldRepoURL+" default 1.1"));
        System.out.println(executeCommand("fabric:profile-edit --repositories "+newRepoURL+" default 1.1"));
        System.out.println(executeCommand("fabric:profile-display --version 1.1 default"));
        System.out.println(executeCommand("fabric:container-upgrade 1.1 child1"));
        Thread.sleep(5000);
        waitForProvisionSuccess(fabricService.getContainer("child1"), PROVISION_TIMEOUT);
        System.out.println(executeCommand("fabric:container-list"));
    }

    @Configuration
    public Option[] config() {
        return new Option[]{
                copySystemProperty("fabric.version"),
                new DefaultCompositeOption(oldFabricDistributionConfiguration()),
                //debugConfiguration("5005",true),
                editConfigurationFilePut("etc/system.properties", "fabric.version", MavenUtils.getArtifactVersion("org.fusesource.fabric", "fuse-fabric"))
        };
    }

    protected Option[] oldFabricDistributionConfiguration() {
        return new Option[] {
                karafDistributionConfiguration().frameworkUrl(
                        maven().groupId(GROUP_ID).artifactId(ARTIFACT_ID).version(OLD_VERSION).type("zip"))
                        .karafVersion(getKarafVersion()).name("Fabric Karaf Distro").unpackDirectory(new File("target/paxexam/unpack/")),
                useOwnExamBundlesStartLevel(50),
                editConfigurationFilePut("etc/config.properties", "karaf.startlevel.bundle", "50"),
                mavenBundle("org.fusesource.tooling.testing","pax-exam-karaf", MavenUtils.getArtifactVersion("org.fusesource.tooling.testing","pax-exam-karaf")),
                logLevel(LogLevelOption.LogLevel.ERROR),
                keepRuntimeFolder()
        };
    }
}
