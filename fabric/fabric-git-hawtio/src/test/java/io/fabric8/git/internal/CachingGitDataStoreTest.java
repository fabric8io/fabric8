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
package io.fabric8.git.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.fabric8.api.scr.Configurer;
import io.fabric8.service.ComponentConfigurer;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.RetryOneTime;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.StoredConfig;
import io.fabric8.api.Constants;
import io.fabric8.api.DefaultRuntimeProperties;
import io.fabric8.git.hawtio.FabricGitFacade;
import io.fabric8.utils.Strings;
import io.fabric8.utils.SystemProperties;
import io.fabric8.zookeeper.bootstrap.DataStoreTemplateRegistry;
import io.fabric8.zookeeper.spring.ZKServerFactoryBean;
import org.gitective.core.RepositoryUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class CachingGitDataStoreTest {

    /**
     * Should we use the old way of importing data into Fabric
     */
    protected boolean useOldImportFormat = true;

    private ZKServerFactoryBean sfb;
    private CuratorFramework curator;
    private Git git;
    private Git remote;
    protected CachingGitDataStore dataStore;
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
        File root = new File(basedir + "/target/git").getCanonicalFile();
        delete(root);

        new File(root, "remote").mkdirs();
        remote = Git.init().setDirectory(new File(root, "remote")).call();
        remote.commit().setMessage("First Commit").setCommitter("fabric", "user@fabric").call();
        String remoteUrl = "file://" + new File(root, "remote").getCanonicalPath();

        new File(root, "local").mkdirs();
        git = Git.init().setDirectory(new File(root, "local")).call();
        git.commit().setMessage("First Commit").setCommitter("fabric", "user@fabric").call();
        StoredConfig config = git.getRepository().getConfig();
        config.setString("remote", "origin", "url", remoteUrl);
        config.setString("remote", "origin", "fetch", "+refs/heads/*:refs/remotes/origin/*");
        config.save();

        DefaultRuntimeProperties sysprops = new DefaultRuntimeProperties();
        sysprops.setProperty(SystemProperties.KARAF_DATA, "target/data");
        FabricGitServiceImpl gitService = new FabricGitServiceImpl();
        gitService.bindRuntimeProperties(sysprops);
        gitService.activate();
        gitService.setGitForTesting(git);

        DataStoreTemplateRegistry registrationHandler = new DataStoreTemplateRegistry();
        registrationHandler.activateComponent();

        dataStore = new CachingGitDataStore();
        dataStore.bindCurator(curator);
        dataStore.bindGitService(gitService);
        dataStore.bindRegistrationHandler(registrationHandler);
        dataStore.bindRuntimeProperties(sysprops);
        dataStore.bindConfigurer(new Configurer() {
            @Override
            public <T> void configure(Map<String, ?> configuration, T target) throws Exception {

            }
        });
        Map<String, String> datastoreProperties = new HashMap<String, String>();
        datastoreProperties.put(GitDataStore.GIT_REMOTE_URL, remoteUrl);
        dataStore.activate(datastoreProperties);
    }

    @After
    public void tearDown() throws Exception {
        //dataStore.deactivate();
        sfb.destroy();
    }

    @Test
    public void testDataStore() throws Exception {
        String defaultVersion = dataStore.getDefaultVersion();
        assertEquals("defaultVersion", "1.0", defaultVersion);

        // now lets import some data - using the old non-git file layout...
        String importPath = basedir + "/../fabric8-karaf/src/main/resources/distro/fabric/import";
        if (useOldImportFormat) {
            assertFolderExists(importPath);
            dataStore.importFromFileSystem(importPath);
            assertHasVersion(defaultVersion);
        } else {
            String prefix = importPath + "/fabric";
            String profileImport = prefix + "/configs/versions/1.0/profiles";
            assertFolderExists(profileImport);

            dataStore.importFromFileSystem(new File(profileImport), "fabric", "1.0", true);
            assertHasVersion(defaultVersion);
        }

        remote.checkout().setName("1.0").call();
        String importedProfile = "example-camel-twitter";
        String profile = importedProfile;
        assertProfileExists(defaultVersion, profile);

        //assertFolderExists("Should have imported an mq/ReadMe.md file!", getLocalGitFile("fabric/profiles/mq/ReadMe.md"));

        String version = "1.1";
        assertCreateVersion("1.0", version);

        assertProfileConfiguration(version, importedProfile, Constants.AGENT_PID, "attribute.parents",
                "feature-camel insight-camel");
        assertProfileTextFileConfigurationContains(version, "example-camel-mq", "camel.xml",
                "http://camel.apache.org/schema/blueprint");

        List<String> fileNames = dataStore.getConfigurationFileNames(version, "example-camel-mq");
        assertNotNull("Should have some file names", fileNames);
        assertTrue("Should have some file names", fileNames.size() > 0);
        assertTrue("Should contain 'came", fileNames.size() > 0);
        assertCollectionContains("configurationFileNames", fileNames, "camel.xml");

        // lets test the profile attributes
        Map<String, String> profileAttributes = dataStore.getProfileAttributes(version, importedProfile);
        String parent = profileAttributes.get("parents");
        assertEquals(importedProfile + ".profileAttributes[parent]", "feature-camel insight-camel", parent);

        System.out.println("Profile attributes: " + profileAttributes);
        String profileAttributeKey = "myKey";
        String expectedProfileAttributeValue = "myValue";
        dataStore.setProfileAttribute(version, importedProfile, profileAttributeKey,
                expectedProfileAttributeValue);
        profileAttributes = dataStore.getProfileAttributes(version, importedProfile);
        System.out.println("Profile attributes: " + profileAttributes);
        assertMapContains("Profile attribute[" + profileAttributeKey + "]", profileAttributes,
                profileAttributeKey, expectedProfileAttributeValue);

        String hawtioRepoKey = "repository.hawtio";
        Map<String, String> hawtioAttrbutes = dataStore.getConfiguration(version, "hawtio", Constants.AGENT_PID);
        String currentHawtRepo = hawtioAttrbutes.get(hawtioRepoKey);
        System.out.println("Current repository.hawtio: " + currentHawtRepo);

        // now lets write via the hawtio API
        FabricGitFacade hawtio = new FabricGitFacade();
        hawtio.bindGitDataStoreForTesting(dataStore);
        hawtio.activateForTesting();
        String hawtioPropertyFile = "/fabric/profiles/" + dataStore.convertProfileIdToDirectory("hawtio") + "/"
                + Constants.AGENT_PID + ".properties";
        hawtio.write(version, hawtioPropertyFile, "My commit message", "me", "me@apache.org", "# new file\n" + hawtioRepoKey + " = " + "mvn\\:io.hawt/hawtio-karaf/myNewVersion/xml/features"
                + "\n");
        hawtioAttrbutes = dataStore.getConfiguration(version, "hawtio", Constants.AGENT_PID);
        String actual = hawtioAttrbutes.get(hawtioRepoKey);
        assertEquals("should have found the updated hawtio repo key",
                "mvn:io.hawt/hawtio-karaf/myNewVersion/xml/features", actual);


        // lets check that the file configurations recurses into folders
        Map<String, byte[]> tomcatFileConfigurations = dataStore.getFileConfigurations("1.0", "controller-tomcat");
        assertHasFileConfiguration(tomcatFileConfigurations, "tomcat/conf/server.xml.mvel");

        Collection<String> files = dataStore.listFiles("1.0", Arrays.asList("example-camel-twitter"), ".");
        assertNotNull(files);
        assertCollectionContains("Should contain file: io.fabric8.examples.camel.twitter.properties", files, "io.fabric8.examples.camel.twitter.properties");

        // check we don't accidentally create a profile
        String profileNotCreated = "shouldNotBeCreated";
        assertEquals("Should not create profile: " + profileNotCreated, null,
                dataStore.getProfile(version, profileNotCreated, false));
        assertProfileNotExists(defaultVersion, profileNotCreated);
        assertFolderNotExists(getLocalGitFile("fabric/profiles/" + dataStore.convertProfileIdToDirectory(profileNotCreated)));

        // now lets create some profiles in this new version
        String newProfile = "myNewProfile";
        dataStore.createProfile(version, newProfile);
        assertProfileExists(version, newProfile);

        // lazy create a profile
        String anotherNewProfile = "anotherNewProfile";
        dataStore.getProfile(version, anotherNewProfile, true);
        assertProfileExists(version, anotherNewProfile);

        version = "1.2";
        assertCreateVersion("1.1", version);

        // check this version has the profile too
        assertProfileExists(version, newProfile);
        assertProfileExists(version, profile);

        // now lets delete a profile
        dataStore.deleteProfile(version, newProfile);
        assertProfileNotExists(version, newProfile);

        // lets check the remote repo
        remote.checkout().setName("1.1").call();

        assertProfileExists("1.1", profile);
        assertProfileExists("1.1", newProfile);
        assertFolderExists(getRemoteGitFile("fabric/profiles/" + dataStore.convertProfileIdToDirectory(profile)));
        assertFolderExists(getRemoteGitFile("fabric/profiles/" + dataStore.convertProfileIdToDirectory(newProfile)));

        remote.checkout().setName("1.2").call();


        assertProfileExists("1.2", profile);
        assertProfileNotExists("1.2", newProfile);
        assertFolderExists(getRemoteGitFile("fabric/profiles/" + dataStore.convertProfileIdToDirectory(profile)));
        assertFolderNotExists(getRemoteGitFile("fabric/profiles/" + dataStore.convertProfileIdToDirectory(newProfile)));

        remote.checkout().setName("1.0").call();
        assertFolderExists(getRemoteGitFile("fabric/profiles/" + dataStore.convertProfileIdToDirectory(profile)));
        assertFolderNotExists(getRemoteGitFile("fabric/profiles/" + dataStore.convertProfileIdToDirectory(newProfile)));

        // delete version 1.2
        assertHasVersion("1.1");
        assertHasVersion("1.2");
        dataStore.removeVersion("1.2");
        assertHasVersion("1.1");
        assertHasNotVersion("1.2");

        Collection<String> remoteBranches = RepositoryUtils.getBranches(remote.getRepository());
        System.out.println("Remote branches after delete: " + remoteBranches);
    }

    public static void assertContainerEquals(String message, List<String> expected, List<String> actual) {
        assertEquals(message + "Size wrong for actual " + actual + " expected " + expected, expected.size(), actual.size());
        for (int i = 0, size = expected.size(); i < size; i++) {
            Object expectedItem = expected.get(i);
            Object actualItem = actual.get(i);
            assertEquals(message + " item " + i, expectedItem, actualItem);
        }
    }

    public static void assertHasFileConfiguration(Map<String, byte[]> fileConfigurations, String pid) {
        byte[] data = fileConfigurations.get(pid);
        assertNotNull("has no file config for " + pid, data);
        assertTrue("empty file config for " + pid, data.length > 0);
        System.out.println("" + pid + " has " + data.length + " bytes");
    }

    protected void assertProfileTextFileConfigurationContains(String version, String profile, String fileName,
                                                              String expectedContents) {
        byte[] bytes = dataStore.getFileConfiguration(version, profile, fileName);
        String message = "file " + fileName + " in version " + version + " profile " + profile;
        assertNotNull("should have got data for " + message, bytes);
        assertTrue("empty file for file for " + message, bytes.length > 0);
        String text = new String(bytes);
        assertTrue("text file does not contain " + expectedContents + " was: " + text,
                text.contains(expectedContents));
    }

    protected void assertProfileConfiguration(String version, String profile, String pid, String key,
                                              String expectedValue) {
        String file = pid + ".properties";
        byte[] fileConfiguration = dataStore.getFileConfiguration(version, profile, file);
        assertNotNull("fileConfiguration", fileConfiguration);
        Map<String, byte[]> fileConfigurations = dataStore.getFileConfigurations(version, profile);
        assertNotNull("fileConfigurations", fileConfigurations);

        Map<String, String> configuration = dataStore.getConfiguration(version, profile, pid);
        assertNotNull("configuration", configuration);
        Map<String, Map<String, String>> configurations = dataStore.getConfigurations(version, profile);
        assertNotNull("configurations", configurations);

        System.out.println("Configurations: " + configurations);
        System.out.println(pid + " configuration: " + configuration);

        assertMapContains("configuration", configuration, key, expectedValue);
        assertFalse("configurations is empty!", configurations.isEmpty());
        assertFalse("fileConfigurations is empty!", fileConfigurations.isEmpty());

        Map<String, String> pidConfig = configurations.get(pid);
        assertNotNull("configurations should have an entry for pid " + pid, pidConfig);
        assertMapContains("configurations[" + pid + "]", pidConfig, key, expectedValue);

        byte[] pidBytes = fileConfigurations.get(file);
        assertNotNull("fileConfigurations should have an entry for file " + file, pidConfig);
        assertTrue("should have found some bytes for fileConfigurations entry for pid " + pid,
                pidBytes.length > 0);

        assertEquals("sizes of fileConfiguration.length and fileConfigurations[" + file + "].length",
                fileConfiguration.length, pidBytes.length);
    }

    protected <T> void assertCollectionContains(String message, Collection<T> collection, T value) {
        assertTrue(message + ".contains(" + value + ")", collection.contains(value));
    }

    protected void assertMapContains(String message, Map<String, String> map, String key,
                                     String expectedValue) {
        String value = map.get(key);
        assertEquals(message + "[" + key + "]", expectedValue, value);
    }

    protected File getLocalGitFile(String path) {
        return new File(GitHelpers.getRootGitDirectory(git), path);
    }

    protected File getRemoteGitFile(String path) {
        return new File(GitHelpers.getRootGitDirectory(remote), path);
    }

    protected void assertProfileExists(String version, String profile) throws Exception {
        List<String> profiles = dataStore.getProfiles(version);
        assertTrue("Profile " + profile + " should exist but has: " + profiles + " for version " + version,
                profiles.contains(profile));
        //We can't directly access git as it gets locked.
        //git.checkout().setName(version).call();
        //assertFolderExists(getLocalGitFile("fabric/profiles/" + dataStore.convertProfileIdToDirectory(profile)));
    }

    protected void assertProfileNotExists(String version, String profile) {
        List<String> profiles = dataStore.getProfiles(version);
        assertFalse(
                "Profile " + profile + " should not exist but has: " + profiles + " for version " + version,
                profiles.contains(profile));
        //We can't directly access git as it gets locked.
        //assertFolderNotExists(getLocalGitFile("fabric/profiles/" + dataStore.convertProfileIdToDirectory(profile)));
    }

    protected void assertFolderExists(String path) {
        assertFolderExists(new File(path));
    }

    protected void assertFolderExists(File path) {
        assertFolderExists("", path);
    }

    protected void assertFolderExists(String message, File path) {
        assertTrue(messagePrefix(message) + "Cannot find folder: " + path, path.exists());
    }

    private String messagePrefix(String message) {
        return message + (Strings.isNotBlank(message) ? ". " : "");
    }

    protected void assertFolderNotExists(File path) {
        assertFolderNotExists("", path);
    }

    protected void assertFolderNotExists(String message, File path) {
        assertFalse(messagePrefix(message) + "Should not have found folder: " + path, path.exists());
    }

    protected void assertCreateVersion(String parrentVersion, String version) {
        dataStore.createVersion(parrentVersion, version);

        assertHasVersion(version);

        // we should now have a remote branch of this name too
        Collection<String> remoteBranches = RepositoryUtils.getBranches(remote.getRepository());
        System.out.println("Remote branches: " + remoteBranches);
        String remoteBranch = "refs/heads/" + version;
        assertTrue("Should contain " + remoteBranch + " but has remote branches " + remoteBranches,
                remoteBranches.contains(remoteBranch));
    }

    protected void assertHasVersion(String version) {
        List<String> versions = dataStore.getVersions();
        System.out.println("Has versions: " + versions);

        assertNotNull("No version list returned!", versions);
        assertTrue("Should contain version", versions.contains(version));
        assertTrue("Should contain version", dataStore.hasVersion(version));
    }

    protected void assertHasNotVersion(String version) {
        List<String> versions = dataStore.getVersions();
        System.out.println("Has versions: " + versions);

        assertNotNull("No version list returned!", versions);
        assertFalse("Should not contain version", versions.contains(version));
        assertFalse("Should not contain version", dataStore.hasVersion(version));
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
