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
package io.fabric8.itests.smoke.embedded;

import io.fabric8.api.Constants;
import io.fabric8.api.CreateEnsembleOptions;
import io.fabric8.api.CreateEnsembleOptions.Builder;
import io.fabric8.api.GitContext;
import io.fabric8.api.Profile;
import io.fabric8.api.ProfileBuilder;
import io.fabric8.api.ProfileRegistry;
import io.fabric8.api.ProfileService;
import io.fabric8.api.RuntimeProperties;
import io.fabric8.api.ZooKeeperClusterBootstrap;
import io.fabric8.git.GitDataStore;
import io.fabric8.git.internal.GitOperation;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Dictionary;
import java.util.Map;
import java.util.Properties;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ResetCommand.ResetType;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.gravia.runtime.ServiceLocator;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

/**
 * Test the {@link ProfileService} with external git repo
 */
@RunWith(Arquillian.class)
public class ExternalGitRepositoryTest {

    private static File remoteRoot;
    private static Git git;
    
    private ProfileRegistry profileRegistry;
    private GitDataStore gitDataStore;
    
    @BeforeClass
    public static void beforeClass() throws Exception {
        Builder<?> builder = CreateEnsembleOptions.builder().agentEnabled(false).clean(true).waitForProvision(false);
        ServiceLocator.getRequiredService(ZooKeeperClusterBootstrap.class).create(builder.build());

        Path dataPath = ServiceLocator.getRequiredService(RuntimeProperties.class).getDataPath();
        Path remoteRepoPath = dataPath.resolve(Paths.get("git", "remote", "fabric"));
        remoteRoot = remoteRepoPath.toFile();
        recursiveDelete(remoteRoot.toPath());
        remoteRoot.mkdirs();
        
        URL remoteUrl = remoteRepoPath.toFile().toURI().toURL(); 
        git = Git.init().setDirectory(remoteRoot).call();
        
        ConfigurationAdmin configAdmin = ServiceLocator.getRequiredService(ConfigurationAdmin.class);
        Configuration config = configAdmin.getConfiguration(Constants.DATASTORE_PID);
        Dictionary<String, Object> properties = config.getProperties();
        properties.put("configuredUrl", remoteUrl.toExternalForm());
        config.update(properties);
        
        // Wait for the configuredUrl to show up the {@link ProfileRegistry}
        ProfileRegistry profileRegistry = ServiceLocator.awaitService(ProfileRegistry.class);
        Map<String, String> dsprops = profileRegistry.getDataStoreProperties();
        while (!dsprops.containsKey("configuredUrl")) {
            Thread.sleep(200);
            profileRegistry = ServiceLocator.awaitService(ProfileRegistry.class);
            dsprops = profileRegistry.getDataStoreProperties();
        }
    }

    @AfterClass
    public static void afterClass() throws Exception {
        ConfigurationAdmin configAdmin = ServiceLocator.getRequiredService(ConfigurationAdmin.class);
        Configuration config = configAdmin.getConfiguration(Constants.DATASTORE_PID);
        Dictionary<String, Object> properties = config.getProperties();
        properties.remove("configuredUrl");
        config.update(properties);
        
        // Wait for the configuredUrl to be removed from the {@link ProfileRegistry}
        ProfileRegistry profileRegistry = ServiceLocator.awaitService(ProfileRegistry.class);
        Map<String, String> dsprops = profileRegistry.getDataStoreProperties();
        while (dsprops.containsKey("configuredUrl")) {
            Thread.sleep(200);
            profileRegistry = ServiceLocator.awaitService(ProfileRegistry.class);
            dsprops = profileRegistry.getDataStoreProperties();
        }
    }

    @Before
    public void setUp() throws Exception {
        profileRegistry = ServiceLocator.getRequiredService(ProfileRegistry.class);
        gitDataStore = ServiceLocator.getRequiredService(GitDataStore.class);
    }
    
    @Test
    public void createAndDeleteProfileLocal() throws Exception {
        
        String versionId = "1.0";
        Assert.assertFalse(profileRegistry.hasProfile(versionId, "prfA"));
        
        ProfileBuilder pbuilder = ProfileBuilder.Factory.create(versionId, "prfA");
        String profileId = profileRegistry.createProfile(pbuilder.getProfile());
        Assert.assertTrue(profileExists("1.0", "prfA"));
        
        profileRegistry.deleteProfile(versionId, profileId);
        Assert.assertFalse(profileExists("1.0", "prfA"));
    }

    @Test
    public void createAndDeleteProfileRemote() throws Exception {
        
        final String versionId = "1.0";
        Assert.assertFalse(profileRegistry.hasProfile(versionId, "prfB"));
        
        createProfileRemote(versionId, "prfB");
        
        GitOperation<Profile> gitop = new GitOperation<Profile>() {
            public Profile call(Git git, GitContext context) throws Exception {
                return profileRegistry.getProfile(versionId, "prfB");
            }
        };
        Profile profile = gitDataStore.gitOperation(new GitContext().requirePull(), gitop, null);
        Assert.assertEquals("1.0", profile.getVersion());
        Assert.assertEquals("prfB", profile.getId());
        
        deleteProfileRemote(versionId, "prfB");
        profile = gitDataStore.gitOperation(new GitContext().requirePull(), gitop, null);
        Assert.assertNull(profile);
    }

    @Test
    public void createProfileFailOnPush() throws Exception {
        
        final String versionId = "1.0";
        Assert.assertFalse(profileRegistry.hasProfile(versionId, "prfC"));
        Assert.assertFalse(profileRegistry.hasProfile(versionId, "prfD"));
        
        createProfileRemote(versionId, "prfC");
        
        ProfileBuilder pbuilder = ProfileBuilder.Factory.create(versionId, "prfD");
        try {
            profileRegistry.createProfile(pbuilder.getProfile());
            Assert.fail("IllegalStateException expected");
        } catch (IllegalStateException ex) {
            Assert.assertTrue(ex.getMessage(), ex.getMessage().startsWith("Cannot fast forward"));
        }
        Assert.assertFalse(profileRegistry.hasProfile(versionId, "prfD"));
        
        deleteProfileRemote(versionId, "prfC");
    }

    private boolean profileExists(String versionId, String profileId) throws Exception {
        checkoutBranch(versionId);
        checkoutBranch(versionId);
        return new File(remoteRoot, "fabric/profiles/" + profileId + ".profile").exists();
    }

    private void checkoutBranch(String versionId) throws Exception {
        // The remote workspace is dirty after the push ?!?
        git.reset().setMode(ResetType.HARD).call();
        git.checkout().setName(versionId).setForce(true).call();
    }

    private void createProfileRemote(String versionId, String profileId) throws Exception {
        checkoutBranch(versionId);
        Properties properties = new Properties();
        File pidFile = new File(remoteRoot, "fabric/profiles/" + profileId + ".profile/" + Constants.AGENT_PID);
        pidFile.getParentFile().mkdirs();
        properties.store(new FileOutputStream(pidFile), "Profile: " + profileId);
        git.add().addFilepattern(".").call();
        git.commit().setMessage("Create profile: " + profileId).call();
    }

    private void deleteProfileRemote(String versionId, String profileId) throws Exception {
        checkoutBranch(versionId);
        Path profilePath = new File(remoteRoot, "fabric/profiles/" + profileId + ".profile").toPath();
        recursiveRemove(profilePath);
        git.commit().setMessage("Delete profile: " + profileId).call();
    }

    private static void recursiveRemove(final Path rootPath) throws IOException {
        if (rootPath.toFile().exists()) {
            Files.walkFileTree(rootPath, new SimpleFileVisitor<Path>() {
                public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) throws IOException {
                    Path pattern = remoteRoot.toPath().relativize(path);
                    try {
                        git.rm().addFilepattern(pattern.toString()).call();
                    } catch (Exception ex) {
                        throw new IllegalStateException(ex);
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        }
    }

    private static void recursiveDelete(Path rootPath) throws IOException {
        if (rootPath.toFile().exists()) {
            Files.walkFileTree(rootPath, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    file.toFile().delete();
                    return FileVisitResult.CONTINUE;
                }
            });
        }
    }
}
