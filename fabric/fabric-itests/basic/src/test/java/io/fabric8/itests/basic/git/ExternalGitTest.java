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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import io.fabric8.api.FabricService;
import io.fabric8.api.ServiceProxy;

import java.io.File;

import javax.inject.Inject;

import org.apache.curator.framework.CuratorFramework;
import org.eclipse.jgit.api.Git;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.CoreOptions;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.Configuration;
import org.ops4j.pax.exam.junit.ExamReactorStrategy;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.ops4j.pax.exam.options.DefaultCompositeOption;
import org.ops4j.pax.exam.spi.reactors.AllConfinedStagedReactorFactory;
import org.osgi.framework.BundleContext;

@RunWith(JUnit4TestRunner.class)
@ExamReactorStrategy(AllConfinedStagedReactorFactory.class)
public class ExternalGitTest extends FabricGitTestSupport {

    File testrepo = new File("testRepo");

    @Inject
    BundleContext bundleContext;

    @Before
    public void setUp() throws InterruptedException {
        testrepo.mkdirs();
    }

    @Test
    public void testCreateProfilesMixedWithVersion() throws Exception {
        String testZkProfilebase = "zkprofile";
        String testGitProfilebase = "gitprofile";
        System.err.println(executeCommand("fabric:create -n"));
        ServiceProxy<FabricService> fabricProxy = ServiceProxy.createServiceProxy(bundleContext, FabricService.class);
        try {
            FabricService fabricService = fabricProxy.getService();
            CuratorFramework curator = fabricService.adapt(CuratorFramework.class);

            String gitRepoUrl = GitUtils.getMasterUrl(bundleContext, curator);
            assertNotNull(gitRepoUrl);
            GitUtils.waitForBranchUpdate(curator, "1.0");

            Git.cloneRepository().setURI(gitRepoUrl).setCloneAllBranches(true).setDirectory(testrepo).setCredentialsProvider(getCredentialsProvider()).call();
            Git git = Git.open(testrepo);
            GitUtils.configureBranch(git, "origin", gitRepoUrl, "1.0");
            git.fetch().setCredentialsProvider(getCredentialsProvider());
            GitUtils.checkoutBranch(git, "origin", "1.0");

            //Check that the default profile exists
            assertTrue(new File(testrepo, "fabric/profiles/default.profile").exists());

            for (int v = 0; v < 2; v++) {
                //Create test profile
                for (int i = 1; i < 2; i++) {
                    String gitProfile = testGitProfilebase + v + "p" + i;
                    String zkProfile = testZkProfilebase + v + "p" + i;
                    createAndTestProfileInGit(fabricService, curator, git, "1." + v, gitProfile);
                    createAndTestProfileInDataStore(fabricService, curator, git, "1." + v, zkProfile);
                }
            }
        } finally {
            fabricProxy.close();
        }
    }

    @Configuration
    public Option[] config() {
        return new Option[]{
                new DefaultCompositeOption(fabricDistributionConfiguration()),
                CoreOptions.wrappedBundle(mavenBundle("io.fabric8", "fabric-utils"))
        };
    }
}
