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

package io.fabric8.itests.basic.git;

import org.eclipse.jgit.api.Git;
import io.fabric8.api.FabricService;
import io.fabric8.itests.paxexam.support.ContainerBuilder;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.Configuration;
import org.ops4j.pax.exam.junit.ExamReactorStrategy;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.ops4j.pax.exam.options.DefaultCompositeOption;
import org.ops4j.pax.exam.spi.reactors.AllConfinedStagedReactorFactory;

import java.io.File;

import static org.apache.karaf.tooling.exam.options.KarafDistributionOption.debugConfiguration;
import static org.junit.Assert.*;

@RunWith(JUnit4TestRunner.class)
@ExamReactorStrategy(AllConfinedStagedReactorFactory.class)
public class ExternalGitTest extends FabricGitTestSupport {

    File testrepo = new File("testRepo");

    @After
    public void tearDown() throws InterruptedException {
        ContainerBuilder.stop();
    }

    @Before
    public void setUp() throws InterruptedException {
        testrepo.mkdirs();
    }

    @Test
    public void testCreateProfilesMixedWithVersion() throws Exception {
        String testZkProfilebase = "zkprofile";
        String testGitProfilebase = "gitprofile";
        System.err.println(executeCommand("fabric:create -n"));
        //Set<Container> containers = ContainerBuilder.create(1, 1).withName("child").assertProvisioningResult().build();
        String gitRepoUrl = GitUtils.getMasterUrl(getCurator());
        assertNotNull(gitRepoUrl);
        GitUtils.waitForBranchUpdate(getCurator(), "1.0");

        Git.cloneRepository().setURI(gitRepoUrl).setCloneAllBranches(true).setDirectory(testrepo).setCredentialsProvider(getCredentialsProvider()).call();
        Git git = Git.open(testrepo);
        GitUtils.configureBranch(git, "origin", gitRepoUrl, "1.0");
        git.fetch().setCredentialsProvider(getCredentialsProvider());
        GitUtils.checkoutBranch(git, "origin", "1.0");

        //Check that the default profile exists
        assertTrue(new File(testrepo, "fabric/profiles/default.profile").exists());

        FabricService fabricService = getFabricService();
        for (int v = 0; v < 2; v++) {
            //Create test profile
            for (int i = 1; i < 2; i++) {
                String gitProfile = testGitProfilebase + v + "p" + i;
                String zkProfile = testZkProfilebase + v + "p" + i;
                createAndTestProfileInGit(git, "1." + v, gitProfile);
                createAndTestProfileInDataStore(git, "1." + v, zkProfile);
            }
        }
    }

    @Configuration
    public Option[] config() {
        return new Option[]{
                new DefaultCompositeOption(fabricWithGitConfiguration())
        };
    }
}
