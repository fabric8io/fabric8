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

package org.fusesource.fabric.itests.paxexam.git;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.PullResult;
import org.eclipse.jgit.lib.IndexDiff;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.transport.PushResult;
import org.eclipse.jgit.treewalk.FileTreeIterator;
import org.fusesource.fabric.api.Container;
import org.fusesource.fabric.api.FabricService;
import org.fusesource.fabric.itests.paxexam.support.ContainerBuilder;
import org.fusesource.fabric.utils.Files;
import org.fusesource.fabric.zookeeper.ZkPath;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.Configuration;
import org.ops4j.pax.exam.junit.ExamReactorStrategy;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.ops4j.pax.exam.options.DefaultCompositeOption;
import org.ops4j.pax.exam.spi.reactors.AllConfinedStagedReactorFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Set;

import static org.apache.karaf.tooling.exam.options.KarafDistributionOption.debugConfiguration;
import static org.junit.Assert.*;

@RunWith(JUnit4TestRunner.class)
@ExamReactorStrategy(AllConfinedStagedReactorFactory.class)
public class GitBridgeTest extends FabricGitTestSupport {

    File testrepo = new File("testRepo");

    @After
    public void tearDown() throws InterruptedException {
        ContainerBuilder.stop();
    }

    @Before
    public void setUp() throws InterruptedException {
        testrepo.mkdirs();
    }

    @Ignore
    @Test
    public void testCreateProfileInGit() throws Exception {
        String testProfileNameBase = "mytestprofile-";
        System.err.println(executeCommand("fabric:create -n"));
        System.err.println(executeCommand("features:install fabric-git"));
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
        assertTrue(new File(testrepo, "default").exists());

        //Create test profile
        for (int i = 1; i <= 5; i++) {
            String testProfileName = testProfileNameBase + i;
            createAndTestProfileInGit(git, "1.0", testProfileName);
        }
    }

    @Ignore
    @Test
    public void testCreateProfileInZk() throws Exception {
        String testProfileNameBase = "mytestprofile-";
        System.err.println(executeCommand("fabric:create -n"));
        System.err.println(executeCommand("features:install fabric-git"));
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
        assertTrue(new File(testrepo, "default").exists());

        FabricService fabricService = getFabricService();
        //Create test profile
        for (int i = 1; i <= 5; i++) {
            String testProfileName = testProfileNameBase + i;
            createAndTestProfileInZooKeeper(git, "1.0", testProfileName);
        }
    }

    @Ignore
    @Test
    public void testCreateProfilesMixed() throws Exception {
        String testZkProfilebase = "zkprofile-";
        String testGitProfilebase = "gitprofile-";
        System.err.println(executeCommand("fabric:create -n"));
        System.err.println(executeCommand("features:install fabric-git"));
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
        assertTrue(new File(testrepo, "default").exists());

        FabricService fabricService = getFabricService();
        //Create test profile
        for (int i = 1; i <= 5; i++) {
            String gitProfile = testGitProfilebase + i;
            String zkProfile = testZkProfilebase + i;
            createAndTestProfileInGit(git, "1.0", gitProfile);
            createAndTestProfileInZooKeeper(git, "1.0", zkProfile);
        }
    }


    @Test
    public void testCreateProfilesMixedWithVersion() throws Exception {
        String testZkProfilebase = "zkprofile-";
        String testGitProfilebase = "gitprofile-";
        System.err.println(executeCommand("fabric:create -n"));
        System.err.println(executeCommand("features:install fabric-git"));
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
        assertTrue(new File(testrepo, "default").exists());

        FabricService fabricService = getFabricService();
        for (int v = 0; v < 3; v++) {
            //Create test profile
            for (int i = 1; i <= 3; i++) {
                String gitProfile = testGitProfilebase + v + "-" + i;
                String zkProfile = testZkProfilebase + v + "-" + i;
                createAndTestProfileInGit(git, "1." + v, gitProfile);
                createAndTestProfileInZooKeeper(git, "1." + v, zkProfile);
            }
        }
    }


    public void testWithProfiles() throws Exception {
        String testProfileNameBase = "mytestprofile-";
        System.err.println(executeCommand("fabric:create -n"));
        Set<Container> containers = ContainerBuilder.create(1, 1).withName("child").assertProvisioningResult().build();
        String gitRepoUrl = GitUtils.getMasterUrl(getCurator());

        GitUtils.waitForBranchUpdate(getCurator(), "1.0");

        Git.cloneRepository().setURI(gitRepoUrl).setCloneAllBranches(true).setDirectory(testrepo).setCredentialsProvider(getCredentialsProvider()).call();
        Git git = Git.open(testrepo);
        GitUtils.configureBranch(git, "origin", gitRepoUrl, "1.0");
        git.fetch().setCredentialsProvider(getCredentialsProvider());
        GitUtils.checkoutBranch(git, "origin", "1.0");

        //Check that the default profile exists
        assertTrue(new File(testrepo, "default").exists());

        //Create test profile
        for (int i = 1; i <= 3; i++) {
            String testProfileName = testProfileNameBase + i;
            System.err.println("Create test profile:" + testProfileName + " in zookeeper");
            getFabricService().getVersion("1.0").createProfile(testProfileName);
            GitUtils.waitForBranchUpdate(getCurator(), "1.0");
            git.pull().setRebase(true).setCredentialsProvider(getCredentialsProvider()).call();
            //Check that a newly created profile exists
            assertTrue(new File(testrepo, testProfileName).exists());
            //Delete test profile
            System.err.println("Delete test profile:" + testProfileName + " in git.");
            git.rm().addFilepattern(testProfileName).call();
            git.commit().setMessage("Delete " + testProfileName).call();
            git.push().setCredentialsProvider(getCredentialsProvider()).setRemote("origin").call();
            GitUtils.waitForBranchUpdate(getCurator(), "1.0");
            Thread.sleep(5000);
            assertFalse(new File(testrepo, testProfileName).exists());
            assertNull(getCurator().checkExists().forPath(ZkPath.CONFIG_VERSIONS_PROFILE.getPath("1.0", testProfileName)));

            //Create the test profile in git
            System.err.println("Create test profile:" + testProfileName + " in git.");
            File testProfileDir = new File(testrepo, testProfileName);
            testProfileDir.mkdirs();
            File testProfileConfig = new File(testProfileDir, "org.fusesource.fabric.agent.properties");
            testProfileConfig.createNewFile();
            Files.writeToFile(testProfileConfig, "", Charset.defaultCharset());
            git.add().addFilepattern(testProfileName).call();
            RevCommit commit = git.commit().setMessage("Create " + testProfileName).call();
            FileTreeIterator fileTreeItr = new FileTreeIterator(git.getRepository());
            IndexDiff indexDiff = new IndexDiff(git.getRepository(), commit.getId(), fileTreeItr);
            System.out.println(indexDiff.getChanged());
            System.out.println(indexDiff.getAdded());
            git.push().setCredentialsProvider(getCredentialsProvider()).setRemote("origin").call();
            GitUtils.waitForBranchUpdate(getCurator(), "1.0");
            //Check that it has been bridged in zookeeper
            Thread.sleep(15000);
            assertNotNull(getCurator().checkExists().forPath(ZkPath.CONFIG_VERSIONS_PROFILE.getPath("1.0", testProfileName)));
        }
    }

    @Configuration
    public Option[] config() {
        return new Option[]{
                new DefaultCompositeOption(fabricWithGitConfiguration()),
                debugConfiguration("5005", false)
        };
    }
}
