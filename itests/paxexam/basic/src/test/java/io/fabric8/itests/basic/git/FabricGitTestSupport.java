/**
 *  Copyright 2005-2014 Red Hat, Inc.
 *
 *  Red Hat licenses this file to you under the Apache License, version
 *  2.0 (the "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 *  implied.  See the License for the specific language governing
 *  permissions and limitations under the License.
 */
package io.fabric8.itests.basic.git;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import io.fabric8.api.FabricService;
import io.fabric8.api.Profile;
import io.fabric8.api.ProfileBuilder;
import io.fabric8.api.ProfileRegistry;
import io.fabric8.api.ProfileService;
import io.fabric8.api.Version;
import io.fabric8.api.VersionBuilder;
import io.fabric8.common.util.Files;
import io.fabric8.itests.paxexam.support.FabricTestSupport;

import java.io.File;
import java.nio.charset.Charset;
import java.util.List;

import org.apache.curator.framework.CuratorFramework;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.PullResult;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;

public class FabricGitTestSupport extends FabricTestSupport {

    private final CredentialsProvider credentialsProvider = new UsernamePasswordCredentialsProvider("admin", "admin");

    protected CredentialsProvider getCredentialsProvider() {
        return credentialsProvider;
    }


    /**
     * Create a profile in git and check that its bridged to the registry.
     */
    protected void createAndTestProfileInGit(FabricService fabricService, CuratorFramework curator, Git git, String versionId, String profileId) throws Exception {
        //Create the test profile in git
        System.out.println("Create test profile:" + profileId + " in git.");
        GitUtils.checkoutBranch(git, "origin", versionId);
        String relativeProfileDir = "fabric/profiles/" + profileId + ".profile";
        File testProfileDir = new File(git.getRepository().getWorkTree(), relativeProfileDir);
        testProfileDir.mkdirs();
        File testProfileConfig = new File(testProfileDir, "io.fabric8.agent.properties");
        testProfileConfig.createNewFile();
        Files.writeToFile(testProfileConfig, "", Charset.defaultCharset());
        git.add().addFilepattern(relativeProfileDir).call();
        git.commit().setAll(true).setMessage("Create " + profileId).call();
        git.pull().setCredentialsProvider(getCredentialsProvider()).setRebase(true).call();
        git.push().setCredentialsProvider(getCredentialsProvider()).setPushAll().setRemote("origin").call();
        GitUtils.waitForBranchUpdate(curator, versionId);
        for (int i = 0; i < 5; i++) {
            if (fabricService.adapt(ProfileRegistry.class).hasProfile(versionId, profileId)) {
                return;
            } else {
                Thread.sleep(1000);
            }
        }
        fail("Expected to find profile " + profileId + " in version " + versionId);
    }


    /**
     * Create a profile in the registry and check that its bridged to git.
     */
    protected void createAndTestProfileInDataStore(FabricService fabricService, CuratorFramework curator, Git git, String versionId, String profileId) throws Exception {
        System.out.println("Create test profile:" + profileId + " in datastore.");
        ProfileService profileService = fabricService.adapt(ProfileService.class);
        List<String> versions = profileService.getVersions();
        if (!versions.contains(versionId)) {
            Version version = VersionBuilder.Factory.create(versionId).getVersion();
            profileService.createVersion(version);
        }
        ProfileBuilder builder = ProfileBuilder.Factory.create(versionId, profileId);
        builder.addAttribute("someAgentKey", "someAgentValue");
        profileService.createProfile(builder.getProfile());
        GitUtils.waitForBranchUpdate(curator, versionId);
        GitUtils.checkoutBranch(git, "origin", versionId);
        PullResult pullResult = git.pull().setCredentialsProvider(getCredentialsProvider()).setRebase(true).call();
        assertTrue(pullResult.isSuccessful());
        String relativeProfileDir = "fabric/profiles/" + profileId + ".profile";
        File testProfileDir = new File(git.getRepository().getWorkTree(), relativeProfileDir);
        assertTrue(testProfileDir.exists());
        File testProfileConfig = new File(testProfileDir, "io.fabric8.agent.properties");
        assertTrue(testProfileConfig.exists());
    }

}

