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
package io.fabric8.deployer;

import io.fabric8.api.DefaultRuntimeProperties;
import io.fabric8.api.Profile;
import io.fabric8.api.scr.Configurer;
import io.fabric8.deployer.dto.DependencyDTO;
import io.fabric8.deployer.dto.ProjectRequirements;
import io.fabric8.git.internal.CachingGitDataStore;
import io.fabric8.git.internal.FabricGitServiceImpl;
import io.fabric8.git.internal.GitDataStore;
import io.fabric8.service.FabricServiceImpl;
import io.fabric8.utils.Strings;
import io.fabric8.utils.SystemProperties;
import io.fabric8.zookeeper.bootstrap.DataStoreTemplateRegistry;
import io.fabric8.zookeeper.spring.ZKServerFactoryBean;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.RetryOneTime;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.StoredConfig;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class ProjectDeployerTest {
    private static final transient Logger LOG = LoggerFactory.getLogger(ProjectDeployerTest.class);

    private ZKServerFactoryBean sfb;
    private CuratorFramework curator;
    protected CachingGitDataStore dataStore;
    private String basedir;
    private Git remote;
    private Git git;
    private FabricServiceImpl fabricService;
    private ProjectDeployer projectDeployer;


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

        DefaultRuntimeProperties runtimeProperties = new DefaultRuntimeProperties();
        runtimeProperties.setProperty(SystemProperties.KARAF_DATA, "target/data");
        FabricGitServiceImpl gitService = new FabricGitServiceImpl();
        gitService.bindRuntimeProperties(runtimeProperties);
        gitService.activate();
        gitService.setGitForTesting(git);

        DataStoreTemplateRegistry registrationHandler = new DataStoreTemplateRegistry();
        registrationHandler.activateComponent();

        dataStore = new CachingGitDataStore();
        dataStore.bindCurator(curator);
        dataStore.bindGitService(gitService);
        dataStore.bindRegistrationHandler(registrationHandler);
        dataStore.bindRuntimeProperties(runtimeProperties);
        dataStore.bindConfigurer(new Configurer() {
            @Override
            public <T> void configure(Map<String, ?> configuration, T target) throws Exception {

            }
        });
        Map<String, String> datastoreProperties = new HashMap<String, String>();
        datastoreProperties.put(GitDataStore.GIT_REMOTE_URL, remoteUrl);
        dataStore.activate(datastoreProperties);


        fabricService = new FabricServiceImpl();
        fabricService.bindDataStore(dataStore);
        fabricService.bindRuntimeProperties(runtimeProperties);
        fabricService.activateComponent();


        projectDeployer = new ProjectDeployer();
        projectDeployer.bindFabricService(fabricService);
        projectDeployer.bindMBeanServer(ManagementFactory.getPlatformMBeanServer());

        String defaultVersion = dataStore.getDefaultVersion();
        assertEquals("defaultVersion", "1.0", defaultVersion);

        // now lets import some data - using the old non-git file layout...
        String importPath = basedir + "/../fabric8-karaf/src/main/resources/distro/fabric/import";
        assertFolderExists(importPath);
        dataStore.importFromFileSystem(importPath);
        assertHasVersion(defaultVersion);
    }

    @After
    public void tearDown() throws Exception {
        //dataStore.deactivate();
        sfb.destroy();
    }

    @Test
    public void testProfileDeploy() throws Exception {
        String groupId = "foo";
        String artifactId = "bar";

        ProjectRequirements requirements = new ProjectRequirements();
        DependencyDTO rootDependency = new DependencyDTO();
        requirements.setRootDependency(rootDependency);
        rootDependency.setGroupId(groupId);
        rootDependency.setArtifactId(artifactId);
        projectDeployer.deployProject(requirements);

        String expectedProfileId = groupId + "-" + artifactId;
        String versionId = "1.0";

        // now we should have a profile created
        Profile profile = fabricService.getProfile(versionId, expectedProfileId);
        assertNotNull("Should have a profile for " + versionId + " and " + expectedProfileId);

        String requirementsFileName = "modules/" + groupId + "/" + artifactId + "-requirements.json";
        byte[] jsonData = profile.getFileConfiguration(requirementsFileName);
        assertNotNull("should have found some JSON for: " + requirementsFileName, jsonData);
        String json = new String(jsonData);
        LOG.info("Got JSON: " + json);

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
