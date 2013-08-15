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
package org.fusesource.fabric.git.datastore;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.RetryOneTime;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.StoredConfig;
import org.fusesource.fabric.git.FabricGitService;
import org.fusesource.fabric.zookeeper.spring.ZKServerFactoryBean;
import org.gitective.core.RepositoryUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;

import static org.fusesource.fabric.zookeeper.utils.ZooKeeperUtils.setData;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class GitAndZooKeeperDataStoreTest {

    private ZKServerFactoryBean sfb;
    private CuratorFramework curator;
    private Git git;
    private Git remote;
    private GitAndZooKeeperDataStore dataStore = new GitAndZooKeeperDataStore();
    private String basedir;

    @Before
    public void setUp() throws Exception {
        sfb = new ZKServerFactoryBean();
        delete(sfb.getDataDir());
        delete(sfb.getDataLogDir());
        sfb.afterPropertiesSet();

        CuratorFrameworkFactory.Builder builder = CuratorFrameworkFactory.builder()
                .connectString("localhost:" + sfb.getClientPortAddress().getPort())
                .retryPolicy(new RetryOneTime(1000))
                .connectionTimeoutMs(360000);

        curator = builder.build();
        curator.start();
        curator.getZookeeperClient().blockUntilConnectedOrTimedOut();

        // setup a local and remote git repo
        basedir = System.getProperty("basedir", ".");
        File root =  new File(basedir + "/target/git").getCanonicalFile();
        delete(root);

        new File(root, "remote").mkdirs();
        remote = Git.init().setDirectory(new File(root, "remote")).call();
        remote.commit().setMessage("First Commit").setCommitter("fabric", "user@fabric").call();

        new File(root, "local").mkdirs();
        git = Git.init().setDirectory(new File(root, "local")).call();
        git.commit().setMessage("First Commit").setCommitter("fabric", "user@fabric").call();
        StoredConfig config = git.getRepository().getConfig();
        config.setString("remote", "origin", "url", "file://" + new File(root, "remote").getCanonicalPath());
        config.setString("remote", "origin", "fetch", "+refs/heads/*:refs/remotes/origin/*");
        config.save();

        FabricGitService gitService = new FabricGitService() {
            public Git get() throws IOException {
                return git;
            }
        };

        dataStore.setCurator(curator);
        dataStore.setGitService(gitService);
        dataStore.init();
    }

    @After
    public void tearDown() throws Exception {
        dataStore.destroy();
        sfb.destroy();
    }

    @Test
    public void testDataStore() throws Exception {
        String defaultVersion = dataStore.getDefaultVersion();
        assertEquals("defaultVersion", "1.0", defaultVersion);

        // now lets import some data - using the old non-git file layout...
        String prefix = basedir + "/../fuse-fabric/src/main/resources/distro/fabric/import/fabric";
        String profileImport = prefix + "/configs/versions/1.0/profiles";
        String metricsImport = prefix + "/metrics";
        assertFolderExists(profileImport);
        assertFolderExists(metricsImport);

        dataStore.importFromFileSystem(profileImport, "fabric");
        dataStore.importFromFileSystem(metricsImport, "fabric");
        assertHasVersion(defaultVersion);

        String profile = "example-dozer";
        assertProfileExists(defaultVersion, profile);

        String version = "1.1";
        assertCreateVersion(version);

        // check we don't accidentally create a profile
        String profileNotCreated = "shouldNotBeCreated";
        assertEquals("Should not create profile: " + profileNotCreated, null, dataStore.getProfile(version, profileNotCreated, false));
        assertProfileNotExists(defaultVersion, profileNotCreated);
        assertFolderNotExists(getLocalGitFile("fabric/profiles/" + profileNotCreated));

        // now lets create some profiles in this new version
        String newProfile = "myNewProfile";
        dataStore.createProfile(version, newProfile);
        assertProfileExists(version, newProfile);

        // lazy create a profile
        String anotherNewProfile = "anotherNewProfile";
        dataStore.getProfile(version, anotherNewProfile, true);
        assertProfileExists(version, anotherNewProfile);

        version = "1.2";
        assertCreateVersion(version);

        // check this version has the profile too
        assertProfileExists(version, newProfile);
        assertProfileExists(version, profile);

        // now lets delete a profile
        dataStore.deleteProfile(version, newProfile);
        assertProfileNotExists(version, newProfile);

        // lets check the remote repo
        remote.checkout().setName("1.1").call();
        assertFolderExists(getRemoteGitFile("fabric/profiles/" + profile));
        assertFolderExists(getRemoteGitFile("fabric/profiles/" + newProfile));

        remote.checkout().setName("1.2").call();
        assertFolderExists(getRemoteGitFile("fabric/profiles/" + profile));
        assertFolderNotExists(getRemoteGitFile("fabric/profiles/" + newProfile));

        remote.checkout().setName("1.0").call();
        assertFolderExists(getRemoteGitFile("fabric/profiles/" + profile));
        assertFolderNotExists(getRemoteGitFile("fabric/profiles/" + newProfile));
    }

    protected File getLocalGitFile(String path) {
        return new File(GitHelpers.getRootGitDirectory(git), path);
    }

    protected File getRemoteGitFile(String path) {
        return new File(GitHelpers.getRootGitDirectory(remote), path);
    }

    protected void assertProfileExists(String version, String profile) {
        List<String> profiles = dataStore.getProfiles(version);
        assertTrue("Profile " + profile + " should exist but has: " + profiles + " for version " + version, profiles.contains(profile));
        assertFolderExists(getLocalGitFile("fabric/profiles/" + profile));
    }

    protected void assertProfileNotExists(String version, String profile) {
        List<String> profiles = dataStore.getProfiles(version);
        assertFalse(
                "Profile " + profile + " should not exist but has: " + profiles + " for version " + version,
                profiles.contains(profile));
        assertFolderNotExists(getLocalGitFile("fabric/profiles/" + profile));
    }

    protected void assertFolderExists(String path) {
        assertFolderExists(new File(path));
    }

    protected void assertFolderExists(File path) {
        assertTrue("Cannot find folder: " + path, path.exists());
    }

    protected void assertFolderNotExists(File path) {
        assertFalse("Should not have found folder: " + path, path.exists());
    }

    protected void assertCreateVersion(String version) {
        dataStore.createVersion(version);

        assertHasVersion(version);

        // we should now have a remote branch of this name too
        Collection<String> remoteBranches = RepositoryUtils.getBranches(remote.getRepository());
        System.out.println("Remote branches: " + remoteBranches);
        String remoteBranch = "refs/heads/" + version;
        assertTrue("Should contain " + remoteBranch + " but has remote branches " + remoteBranches, remoteBranches.contains(remoteBranch));
    }

    protected void assertHasVersion(String version) {
        List<String> versions = dataStore.getVersions();
        System.out.println("Has versions: " + versions);

        assertNotNull("No version list returned!", versions);
        assertTrue("Should contain version", versions.contains(version));
        assertTrue("Should contain version", dataStore.hasVersion(version));
    }

    private void delete(File file) throws IOException {
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    delete(child);
                }
            }
        }
        if (file.exists() && !file.delete()) {
            throw new IOException("Unable to delete file " + file);
        }
    }

}
