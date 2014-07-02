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
package io.fabric8.deployer;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.nio.file.Paths;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.fabric8.api.Containers;
import io.fabric8.api.RuntimeProperties;
import io.fabric8.api.FabricService;
import io.fabric8.api.PlaceholderResolver;
import io.fabric8.api.Profile;
import io.fabric8.api.Version;
import io.fabric8.api.scr.Configurer;
import io.fabric8.common.util.Strings;
import io.fabric8.deployer.dto.DependencyDTO;
import io.fabric8.deployer.dto.ProjectRequirements;
import io.fabric8.git.internal.CachingGitDataStore;
import io.fabric8.git.internal.FabricGitServiceImpl;
import io.fabric8.git.internal.GitDataStore;
import io.fabric8.service.ChecksumPlaceholderResolver;
import io.fabric8.service.FabricServiceImpl;
import io.fabric8.service.ProfilePropertyPointerResolver;
import io.fabric8.service.VersionPropertyPointerResolver;
import io.fabric8.utils.SystemProperties;
import io.fabric8.zookeeper.bootstrap.DataStoreTemplateRegistry;
import io.fabric8.zookeeper.spring.ZKServerFactoryBean;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.RetryOneTime;
import org.easymock.EasyMock;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.StoredConfig;
import org.hamcrest.CoreMatchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hamcrest.CoreMatchers.any;
import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
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
    private ProjectDeployerImpl projectDeployer;
    private RuntimeProperties runtimeProperties;


    @Before
    public void setUp() throws Exception {
        URL.setURLStreamHandlerFactory(new CustomBundleURLStreamHandlerFactory());

        basedir = System.getProperty("basedir", ".");
        String karafRoot = basedir + "/target/karaf";
        System.setProperty("karaf.root", karafRoot);
        System.setProperty("karaf.data", karafRoot + "/data");

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

        runtimeProperties = EasyMock.createMock(RuntimeProperties.class);
        EasyMock.expect(runtimeProperties.getRuntimeIdentity()).andReturn("root").anyTimes();
        EasyMock.expect(runtimeProperties.getHomePath()).andReturn(Paths.get("target")).anyTimes();
        EasyMock.expect(runtimeProperties.getDataPath()).andReturn(Paths.get("target/data")).anyTimes();
        EasyMock.expect(runtimeProperties.getProperty(EasyMock.eq(SystemProperties.FABRIC_ENVIRONMENT))).andReturn("").anyTimes();
        EasyMock.replay(runtimeProperties);

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
            public <T> Map<String, ?> configure(Map<String, ?> configuration, T target, String... ignorePrefix) throws Exception {
                return null;
            }

            @Override
            public <T> Map<String, ?> configure(Dictionary<String, ?> configuration, T target, String... ignorePrefix) throws Exception {
                return null;
            }
        });
        Map<String, Object> datastoreProperties = new HashMap<String, Object>();
        datastoreProperties.put(GitDataStore.GIT_REMOTE_URL, remoteUrl);
        dataStore.activate(datastoreProperties);


        fabricService = new FabricServiceImpl();
        fabricService.bindDataStore(dataStore);
        fabricService.bindRuntimeProperties(runtimeProperties);
        fabricService.bindPlaceholderResolver(new DummyPlaceholerResolver("port"));
        fabricService.bindPlaceholderResolver(new DummyPlaceholerResolver("zk"));
        fabricService.bindPlaceholderResolver(new ProfilePropertyPointerResolver());
        fabricService.bindPlaceholderResolver(new ChecksumPlaceholderResolver());
        fabricService.bindPlaceholderResolver(new VersionPropertyPointerResolver());
        fabricService.activateComponent();


        projectDeployer = new ProjectDeployerImpl();
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
        EasyMock.verify(runtimeProperties);
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

        String requirementsFileName = "dependencies/" + groupId + "/" + artifactId + "-requirements.json";
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

        assertProfileMetadata();
    }

    public void assertProfileMetadata() throws Exception {
        Version version = fabricService.getVersion("1.0");
        assertNotNull("version", version);
        Profile profile = version.getProfile("containers-wildfly");
        assertNotNull("profile", profile);
        List<String> tags = profile.getTags();
        assertContains(tags, "containers");
        String summaryMarkdown = profile.getSummaryMarkdown();
        assertThat(summaryMarkdown, containsString("WildFly"));
        String iconURL = profile.getIconURL();
        assertEquals("iconURL", "/version/1.0/profile/containers-wildfly/file/icon.svg", iconURL);

        // lets test inheritance of icons
        profile = version.getProfile("containers-services-cassandra.local");
        assertNotNull("profile", profile);
        iconURL = profile.getIconURL();
        assertEquals("iconURL", "/version/1.0/profile/containers-services-cassandra/file/icon.svg", iconURL);

    }

    public static <T> void assertContains(Collection<T> collection, T expected) {
        assertNotNull("collection", collection);
        assertTrue("Should contain " + expected, collection.contains(expected));
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

    private static class DummyPlaceholerResolver implements PlaceholderResolver {
        private final String scheme;

        private DummyPlaceholerResolver(String scheme) {
            this.scheme = scheme;
        }

        @Override
        public String getScheme() {
            return scheme;
        }

        @Override
        public String resolve(FabricService fabricService, Map<String, Map<String, String>> configs, String pid, String key, String value) {
            return null;
        }
    }

    public class CustomBundleURLStreamHandlerFactory implements
            URLStreamHandlerFactory {
        private static final String MVN_URI_PREFIX = "mvn";

        public URLStreamHandler createURLStreamHandler(String protocol) {
            if (protocol.equals(MVN_URI_PREFIX)) {
                return new MvnHandler();
            } else {
                return null;
            }
        }

    }

    public class MvnHandler extends URLStreamHandler {
        @Override
        protected URLConnection openConnection(URL u) throws IOException {
            return new URLConnection(u) {
                @Override
                public void connect() throws IOException {
                }

                @Override
                public InputStream getInputStream() throws IOException {
                    return new ByteArrayInputStream("<features/>".getBytes());
                }
            };
        }
    }

}
