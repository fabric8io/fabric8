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

import io.fabric8.api.Containers;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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
        sfb.setPort(9123);
        sfb.afterPropertiesSet();

        int zkPort = sfb.getClientPortAddress().getPort();
        LOG.info("Connecting to ZK on port: " + zkPort);
        CuratorFrameworkFactory.Builder builder = CuratorFrameworkFactory.builder()
                .connectString("localhost:" + zkPort)
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
        String expectedProfileId = groupId + "-" + artifactId;
        String versionId = "1.0";

        ProjectRequirements requirements = new ProjectRequirements();
        DependencyDTO rootDependency = new DependencyDTO();
        requirements.setRootDependency(rootDependency);
        rootDependency.setGroupId(groupId);
        rootDependency.setArtifactId(artifactId);
        rootDependency.setVersion("1.0.0");
        List<String> parentProfileIds = Arrays.asList("karaf");
        List<String> features = Arrays.asList("cxf", "war");
        requirements.setParentProfiles(parentProfileIds);
        requirements.setFeatures(features);
        projectDeployer.deployProject(requirements);


        // now we should have a profile created
        Profile profile = assertProfileInFabric(expectedProfileId, versionId);
        assertBundleCount(profile, 1);
        assertEquals("parent ids", parentProfileIds, Containers.getParentProfileIds(profile));
        assertFeatures(profile, features);

        String requirementsFileName = "modules/" + groupId + "/" + artifactId + "-requirements.json";
        byte[] jsonData = profile.getFileConfiguration(requirementsFileName);
        assertNotNull("should have found some JSON for: " + requirementsFileName, jsonData);
        String json = new String(jsonData);
        LOG.info("Got JSON: " + json);

        // lets replace the version, parent, features
        rootDependency.setVersion("1.0.1");
        projectDeployer.deployProject(requirements);
        profile = assertProfileInFabric(expectedProfileId, versionId);
        assertBundleCount(profile, 1);


        // now lets make a new version
        expectedProfileId = "cheese";
        versionId = "1.2";
        requirements.setVersion(versionId);
        requirements.setProfileId(expectedProfileId);

        parentProfileIds = Arrays.asList("default");
        features = Arrays.asList("camel", "war");
        requirements.setParentProfiles(parentProfileIds);
        requirements.setFeatures(features);
        projectDeployer.deployProject(requirements);

        profile = assertProfileInFabric(expectedProfileId, versionId);
        assertBundleCount(profile, 1);
        assertEquals("parent ids", parentProfileIds, Containers.getParentProfileIds(profile));
        assertFeatures(profile, features);
    }

    protected void assertFeatures(Profile profile, List<String> features) {
        List<String> expected = new ArrayList<String>(features);
        List<String> actual = profile.getFeatures();
        Collections.sort(expected);
        Collections.sort(actual);
        assertEquals("features", expected, actual);
    }

    protected static void assertBundleCount(Profile profile, int size) {
        List<String> bundles = profile.getBundles();
        LOG.info("Profile " + profile + " now has bundles: " + bundles);
        assertEquals("Profile " + profile + " bundles are " + bundles, size, bundles.size());
    }

    protected Profile assertProfileInFabric(String profileId, String versionId) {
        Profile profile = fabricService.getProfile(versionId, profileId);
        assertNotNull("Should have a profile for " + versionId + " and " + profileId);
        return profile;
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
        LOG.info("Has versions: " + versions);

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
