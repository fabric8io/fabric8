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

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import io.fabric8.api.FabricService;
import io.fabric8.api.Version;
import io.fabric8.itests.paxexam.support.FabricTestSupport;
import io.fabric8.utils.Files;

import java.io.File;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;

import org.apache.curator.framework.CuratorFramework;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.PullResult;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;

import com.google.common.base.Function;
import com.google.common.collect.Lists;

public class FabricGitTestSupport extends FabricTestSupport {

    private final CredentialsProvider credentialsProvider = new UsernamePasswordCredentialsProvider("admin", "admin");

    protected CredentialsProvider getCredentialsProvider() {
        return credentialsProvider;
    }


    /**
     * Create a profile in git and check that its bridged to the registry.
     */
    protected void createAndTestProfileInGit(FabricService fabricService, CuratorFramework curator, Git git, String version, String profile) throws Exception {
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
        GitUtils.waitForBranchUpdate(curator, version);
        for (int i = 0; i < 5; i++) {
            if (fabricService.getDataStore().hasProfile(version, profile)) {
                return;
            } else {
                Thread.sleep(1000);
            }
        }
        fail("Expected to find profile " + profile + " in version " + version);
    }


    /**
     * Create a profile in the registry and check that its bridged to git.
     */
    protected void createAndTestProfileInDataStore(FabricService fabricService, CuratorFramework curator, Git git, String version, String profile) throws Exception {
        System.err.println("Create test profile:" + profile + " in datastore.");
        List<String> versions = Lists.transform(Arrays.<Version>asList(fabricService.getVersions()), new Function<Version, String>() {

            @Override
            public String apply(Version version) {
                return version.getId();
            }
        });

        if (!versions.contains(version)) {
            fabricService.createVersion(version);
        }

        fabricService.getDataStore().createProfile(version, profile);
        GitUtils.waitForBranchUpdate(curator, version);
        GitUtils.checkoutBranch(git, "origin", version);
        PullResult pullResult = git.pull().setCredentialsProvider(getCredentialsProvider()).setRebase(true).call();
        assertTrue(pullResult.isSuccessful());
        String relativeProfileDir = "fabric/profiles/" + profile + ".profile";
        File testProfileDir = new File(git.getRepository().getWorkTree(), relativeProfileDir);
        assertTrue(testProfileDir.exists());
        File testProfileConfig = new File(testProfileDir, "io.fabric8.agent.properties");
        assertTrue(testProfileConfig.exists());
    }

}

