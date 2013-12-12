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

import com.google.common.base.Function;
import com.google.common.collect.Lists;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.PullResult;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import io.fabric8.api.Version;
import io.fabric8.itests.paxexam.support.FabricTestSupport;
import io.fabric8.utils.Files;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.MavenUtils;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.ExamReactorStrategy;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.ops4j.pax.exam.options.DefaultCompositeOption;
import org.ops4j.pax.exam.spi.reactors.AllConfinedStagedReactorFactory;

import java.io.File;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

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
        String relativeProfileDir = "fabric/profiles/" + profile + ".profile";
        File testProfileDir = new File(git.getRepository().getWorkTree(), relativeProfileDir);
        testProfileDir.mkdirs();
        File testProfileConfig = new File(testProfileDir, "io.fabric8.agent.properties");
        testProfileConfig.createNewFile();
        Files.writeToFile(testProfileConfig, "", Charset.defaultCharset());
        git.add().addFilepattern(relativeProfileDir).call();
        git.commit().setAll(true).setMessage("Create " + profile).call();
        PullResult pullResult = git.pull().setCredentialsProvider(getCredentialsProvider()).setRebase(true).call();
        git.push().setCredentialsProvider(getCredentialsProvider()).setPushAll().setRemote("origin").call();
        GitUtils.waitForBranchUpdate(getCurator(), version);
        for (int i = 0; i < 5; i++) {
            if (getFabricService().getDataStore().hasProfile(version, profile)) {
                return;
            } else {
                Thread.sleep(1000);
            }
        }
        fail("Expected to find profile " + profile + " in version " + version);
    }


    /**
     * Create a profile in the registry and check that its bridged to git.
     *
     * @param git     The git object of the test repository.
     * @param version The version of the profile.
     * @param profile The profile name.
     * @throws Exception
     */
    public void createAndTestProfileInDataStore(Git git, String version, String profile) throws Exception {
        System.err.println("Create test profile:" + profile + " in datastore.");
        List<String> versions = Lists.transform(Arrays.<Version>asList(getFabricService().getVersions()), new Function<Version, String>() {

            @Override
            public String apply(Version version) {
                return version.getId();
            }
        });

        if (!versions.contains(version)) {
            getFabricService().createVersion(version);
        }

        getFabricService().getDataStore().createProfile(version, profile);
        GitUtils.waitForBranchUpdate(getCurator(), version);
        GitUtils.checkoutBranch(git, "origin", version);
        PullResult pullResult = git.pull().setCredentialsProvider(getCredentialsProvider()).setRebase(true).call();
        assertTrue(pullResult.isSuccessful());
        String relativeProfileDir = "fabric/profiles/" + profile + ".profile";
        File testProfileDir = new File(git.getRepository().getWorkTree(), relativeProfileDir);
        assertTrue(testProfileDir.exists());
        File testProfileConfig = new File(testProfileDir, "io.fabric8.agent.properties");
        assertTrue(testProfileConfig.exists());
    }

    public Option[] fabricWithGitConfiguration() {
        return new Option[]{
                new DefaultCompositeOption(fabricDistributionConfiguration()),
                mavenBundle("io.fabric8", "fabric-utils", MavenUtils.getArtifactVersion("io.fabric8", "fabric-utils"))
        };
    }

}

