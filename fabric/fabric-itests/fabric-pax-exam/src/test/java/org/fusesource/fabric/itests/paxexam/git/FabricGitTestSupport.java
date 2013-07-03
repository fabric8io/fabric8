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

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.PullResult;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.fusesource.fabric.api.FabricException;
import org.fusesource.fabric.api.Version;
import org.fusesource.fabric.itests.paxexam.FabricTestSupport;
import org.fusesource.fabric.utils.Files;
import org.fusesource.fabric.zookeeper.ZkPath;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.MavenUtils;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.ExamReactorStrategy;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.ops4j.pax.exam.options.DefaultCompositeOption;
import org.ops4j.pax.exam.spi.reactors.AllConfinedStagedReactorFactory;

import java.io.File;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(JUnit4TestRunner.class)
@ExamReactorStrategy(AllConfinedStagedReactorFactory.class)
public class FabricGitTestSupport extends FabricTestSupport {

    private final CredentialsProvider credentialsProvider = new UsernamePasswordCredentialsProvider("admin", "admin");


    public CredentialsProvider getCredentialsProvider() {
        return credentialsProvider;
    }


    /**
     * Create a profile in git and check that its bridged to the registry.
     *
     * @param git     The git object of the test repository.
     * @param version The version of the profile.
     * @param profile The profile name.
     * @throws Exception
     */
    public void createAndTestProfileInGit(Git git, String version, String profile) throws Exception {
        //Create the test profile in git
        System.err.println("Create test profile:" + profile + " in git.");
        GitUtils.checkoutBranch(git, "origin", version);
        File testProfileDir = new File(git.getRepository().getWorkTree(), profile);
        testProfileDir.mkdirs();
        File testProfileConfig = new File(testProfileDir, "org.fusesource.fabric.agent.properties");
        testProfileConfig.createNewFile();
        Files.writeToFile(testProfileConfig, "", Charset.defaultCharset());
        git.add().addFilepattern(profile).call();
        git.commit().setMessage("Create " + profile).call();
        PullResult pullResult = git.pull().setCredentialsProvider(getCredentialsProvider()).setRebase(true).call();
        git.push().setCredentialsProvider(getCredentialsProvider()).setRemote("origin").call();
        GitUtils.waitForBranchUpdate(getCurator(), version);
        //Check that it has been bridged in zookeeper
        Thread.sleep(5000);
        assertNotNull(getCurator().checkExists().forPath(ZkPath.CONFIG_VERSIONS_PROFILE.getPath(version, profile)));
    }


    /**
     * Create a profile in the registry and check that its bridged to git.
     *
     * @param git     The git object of the test repository.
     * @param version The version of the profile.
     * @param profile The profile name.
     * @throws Exception
     */
    public void createAndTestProfileInZooKeeper(Git git, String version, String profile) throws Exception {
        System.err.println("Create test profile:" + profile + " in zookeeper.");
        List<String> versions = Lists.transform(Arrays.<Version>asList(getFabricService().getVersions()), new Function<Version, String>() {

            @Override
            public String apply(Version version) {
                return version.getId();
            }
        });

        if (!versions.contains(version)) {
            getFabricService().createVersion(version);
        }

        getFabricService().getVersion(version).createProfile(profile);
        GitUtils.waitForBranchUpdate(getCurator(), version);
        Thread.sleep(5000);
        GitUtils.checkoutBranch(git, "origin", version);
        PullResult pullResult = git.pull().setCredentialsProvider(getCredentialsProvider()).setRebase(true).call();
        assertTrue(pullResult.isSuccessful());
        File testProfileDir = new File(git.getRepository().getWorkTree(), profile);
        assertTrue(testProfileDir.exists());
        File testProfileConfig = new File(testProfileDir, "org.fusesource.fabric.agent.properties");
        assertTrue(testProfileConfig.exists());
    }

    public Option[] fabricWithGitConfiguration() {
        return new Option[]{
                new DefaultCompositeOption(fabricDistributionConfiguration()),
                mavenBundle("org.fusesource.fabric", "fabric-groups", MavenUtils.getArtifactVersion("org.fusesource.fabric", "fabric-groups")),
                mavenBundle("org.fusesource.fabric", "fabric-git", MavenUtils.getArtifactVersion("org.fusesource.fabric", "fabric-git"))
        };
    }


    public Option[] fabricWithGitAndBridgeConfiguration() {
        return new Option[]{
                new DefaultCompositeOption(fabricDistributionConfiguration()),
                mavenBundle("org.fusesource.fabric", "fabric-groups", MavenUtils.getArtifactVersion("org.fusesource.fabric", "fabric-groups")),
                mavenBundle("org.fusesource.fabric", "fabric-git", MavenUtils.getArtifactVersion("org.fusesource.fabric", "fabric-git")),
                mavenBundle("org.fusesource.fabric", "fabric-git-server", MavenUtils.getArtifactVersion("org.fusesource.fabric", "fabric-git-server")),
                mavenBundle("org.fusesource.fabric", "fabric-git-zkbridge", MavenUtils.getArtifactVersion("org.fusesource.fabric", "fabric-git-zkbridge"))
        };
    }
}

