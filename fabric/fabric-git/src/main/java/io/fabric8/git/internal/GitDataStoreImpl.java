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
package io.fabric8.git.internal;

import static io.fabric8.zookeeper.utils.ZooKeeperUtils.generateContainerToken;
import static io.fabric8.zookeeper.utils.ZooKeeperUtils.getContainerLogin;
import io.fabric8.api.Constants;
import io.fabric8.api.DataStore;
import io.fabric8.api.DataStoreTemplate;
import io.fabric8.api.FabricException;
import io.fabric8.api.GitContext;
import io.fabric8.api.LockHandle;
import io.fabric8.api.Profile;
import io.fabric8.api.ProfileBuilder;
import io.fabric8.api.ProfileBuilders;
import io.fabric8.api.ProfileRegistry;
import io.fabric8.api.Profiles;
import io.fabric8.api.RuntimeProperties;
import io.fabric8.api.Version;
import io.fabric8.api.VersionBuilder;
import io.fabric8.api.VersionSequence;
import io.fabric8.api.jcip.ThreadSafe;
import io.fabric8.api.scr.AbstractComponent;
import io.fabric8.api.scr.Configurer;
import io.fabric8.api.scr.ValidatingReference;
import io.fabric8.common.util.Files;
import io.fabric8.common.util.Strings;
import io.fabric8.common.util.Zips;
import io.fabric8.git.GitDataStore;
import io.fabric8.git.GitListener;
import io.fabric8.git.GitProxyService;
import io.fabric8.git.GitService;
import io.fabric8.utils.DataStoreUtils;
import io.fabric8.zookeeper.ZkPath;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.SocketAddress;
import java.net.URI;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;

import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.shared.SharedCount;
import org.apache.curator.framework.recipes.shared.SharedCountListener;
import org.apache.curator.framework.recipes.shared.SharedCountReader;
import org.apache.curator.framework.state.ConnectionState;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.felix.utils.properties.Properties;
import org.eclipse.jgit.api.CreateBranchCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ListBranchCommand;
import org.eclipse.jgit.api.MergeResult;
import org.eclipse.jgit.api.ResetCommand;
import org.eclipse.jgit.api.ResetCommand.ResetType;
import org.eclipse.jgit.api.errors.CannotDeleteCurrentBranchException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.merge.MergeStrategy;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.FetchResult;
import org.eclipse.jgit.transport.PushResult;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.eclipse.jgit.treewalk.AbstractTreeIterator;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.gitective.core.CommitUtils;
import org.gitective.core.RepositoryUtils;
import org.jboss.gravia.utils.IllegalArgumentAssertion;
import org.jboss.gravia.utils.IllegalStateAssertion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

/**
 * A git based implementation of {@link DataStore} which stores the profile
 * configuration versions in a branch per version and directory per profile.
 */
@ThreadSafe
@Component(name = Constants.DATASTORE_TYPE_PID, label = "Fabric8 Caching Git DataStore", policy = ConfigurationPolicy.OPTIONAL, immediate = true, metatype = true)
@Service({ GitDataStore.class, ProfileRegistry.class })
public final class GitDataStoreImpl extends AbstractComponent implements GitDataStore, ProfileRegistry {

    private static final transient Logger LOGGER = LoggerFactory.getLogger(GitDataStoreImpl.class);
    
    private static final String GIT_REMOTE_USER = "gitRemoteUser";
    private static final String GIT_REMOTE_PASSWORD = "gitRemotePassword";
    private static final String AGENT_METADATA_FILE = "io.fabric8.agent.properties";
    private static final int GIT_COMMIT_SHORT_LENGTH = 7;
    private static final String MASTER_BRANCH = "master";
    private static final int MAX_COMMITS_WITHOUT_GC = 40;

    @Reference(referenceInterface = CuratorFramework.class)
    private final ValidatingReference<CuratorFramework> curator = new ValidatingReference<>();
    @Reference(referenceInterface = GitService.class)
    private final ValidatingReference<GitService> gitService = new ValidatingReference<>();
    @Reference(referenceInterface = GitProxyService.class)
    private final ValidatingReference<GitProxyService> gitProxyService = new ValidatingReference<>();
    @Reference(referenceInterface = DataStore.class)
    private final ValidatingReference<DataStore> dataStore = new ValidatingReference<>();
    @Reference(referenceInterface = ProfileBuilders.class)
    private final ValidatingReference<ProfileBuilders> profileBuilders = new ValidatingReference<>();
    @Reference(referenceInterface = RuntimeProperties.class)
    private final ValidatingReference<RuntimeProperties> runtimeProperties = new ValidatingReference<>();

    @Reference
    private Configurer configurer;

    private final ScheduledExecutorService threadPool = Executors.newSingleThreadScheduledExecutor();

    private final Set<String> versions = new CopyOnWriteArraySet<String>();
    private final GitListener gitListener = new GitDataStoreListener();
    private final AtomicReference<String> remoteRef = new AtomicReference<String>("origin");
    private final ReentrantReadWriteLock readWriteLock = new ReentrantReadWriteLock();

    private int commitsWithoutGC = MAX_COMMITS_WITHOUT_GC;
    private Map<String, String> dataStoreProperties;
    private ProxySelector defaultProxySelector;
    private String lastFetchWarning;
    private volatile boolean initialPull;
    private SharedCount counter;
    private String remoteUrl;

    @Property(name = "configuredUrl", label = "External Git Repository URL", description = "The URL to a fixed external git repository")
    private String configuredUrl;
    @Property(name = "gitPushInterval", label = "Push Interval", description = "The interval between push (value in millis)")
    private long gitPushInterval = 60 * 1000L;
    // option to use old behavior without the shared counter
    @Property(name = "gitPullOnPush", label = "Pull before push", description = "Whether to do a push before pull")
    private boolean gitPullOnPush = false;
    @Property(name = "gitTimeout", label = "Timeout", description = "Timeout connecting to remote git server (value in seconds)")
    private int gitTimeout = 10;
    @Property(name = "importDir", label = "Import Directory", description = "Directory to import additional profiles", value = "fabric")
    private String importDir = "fabric";

    private final LoadingCache<String, Version> versionCache = CacheBuilder.newBuilder().build(new CacheLoader<String, Version>() {
        @Override
        public Version load(final String versionId) throws Exception {
            GitOperation<Version> gitop = new GitOperation<Version>() {
                public Version call(Git git, GitContext context) throws Exception {
                    return loadVersion(git, versionId);
                }
            };
            return executeRead(gitop, true);
        }
    });

    @Activate
    void activate(Map<String, ?> configuration) throws Exception {
        configurer.configure(configuration, this);

        // Remove non-String values from the configuration
        Map<String, String> properties = new HashMap<>();
        for (Map.Entry<String, ?> entry : configuration.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            if (value instanceof String) {
                properties.put(key, (String) value);
            }
        }
        this.dataStoreProperties = Collections.unmodifiableMap(properties);

        // DataStore activation accesses public API that is private by {@link AbstractComponent#assertValid()).
        // We activate the component first and rollback on error
        try {
            activateComponent();
            activateInternal();
        } catch (Exception ex) {
            deactivateComponent();
            throw ex;
        }
    }

    @Deactivate
    void deactivate() {
        deactivateComponent();
        deactivateInternal();
    }

    private void activateInternal() {
        initialPull = false;

        try {
            LOGGER.info("Starting up DataStore " + this);

            // Call the bootstrap {@link DataStoreTemplate}
            DataStoreTemplate template = runtimeProperties.get().removeRuntimeAttribute(DataStoreTemplate.class);
            if (template != null) {
                LOGGER.info("Using template: " + template);
                template.doWith(this, dataStore.get());
            }

            if (gitProxyService.getOptional() != null) {
                // authenticator disabled, until properly tested it does not affect others, as Authenticator is static in the JVM
                // Authenticator.setDefault(new FabricGitLocalHostAuthenticator(gitProxyService.getOptional()));
                defaultProxySelector = ProxySelector.getDefault();
                ProxySelector fabricProxySelector = new FabricGitLocalHostProxySelector(defaultProxySelector, gitProxyService.getOptional());
                ProxySelector.setDefault(fabricProxySelector);
                LOGGER.info("Setting up FabricProxySelector: {}", fabricProxySelector);
            }

            // [FIXME] Why can we not rely on the injected GitService
            GitService optionalService = gitService.getOptional();

            if (configuredUrl != null) {
                gitListener.onRemoteUrlChanged(configuredUrl);
                remoteUrl = configuredUrl;
            } else if (optionalService != null) {
                optionalService.addGitListener(gitListener);
                remoteUrl = optionalService.getRemoteUrl();
                gitListener.onRemoteUrlChanged(remoteUrl);
            }

            forceGetVersions();

            // import additional profiles
            Path homePath = runtimeProperties.get().getHomePath();
            Path dir = homePath.resolve(importDir);
            importFromFilesystem(dir);

            LOGGER.info("Starting to push to remote git repository every {} millis", gitPushInterval);
            threadPool.scheduleWithFixedDelay(new Runnable() {
                @Override
                public void run() {
                    try {
                        // must do an initial pull to get data
                        if (!initialPull) {
                            LOGGER.trace("Performing initial pull");
                            pull();
                            initialPull = true;
                            LOGGER.debug("Performing initial pull done");
                        }

                        if (gitPullOnPush) {
                            LOGGER.trace("Performing timed pull");
                            pull();
                            LOGGER.debug("Performed timed pull done");
                        }
                        //a commit that failed to push for any reason, will not get pushed until the next commit.
                        //periodically pushing can address this issue.
                        LOGGER.trace("Performing timed push");
                        push();
                        LOGGER.debug("Performed timed push done");
                    } catch (Throwable e) {
                        LOGGER.debug("Error during performed timed pull/push due " + e.getMessage(), e);
                        // we dont want stacktrace in WARNs
                        LOGGER.warn("Error during performed timed pull/push due " + e.getMessage() + ". This exception is ignored.");
                    }
                }

                @Override
                public String toString() {
                    return "TimedPushTask";
                }
            }, 1000, gitPushInterval, TimeUnit.MILLISECONDS);
            // do the initial pull at first so just wait 1 sec

            if (!gitPullOnPush) {
                LOGGER.info("Using ZooKeeper SharedCount to react when master git repo is changed, so we can do a git pull to the local git repo.");
                counter = new SharedCount(curator.get(), ZkPath.GIT_TRIGGER.getPath(), 0);
                counter.addListener(new SharedCountListener() {
                    @Override
                    public void countHasChanged(SharedCountReader sharedCountReader, int value) throws Exception {
                        LOGGER.debug("Watch counter updated to " + value + ", doing a pull");
                        try {
                            // must sleep a bit as otherwise we are too fast
                            Thread.sleep(1000);
                            pull();
                        } catch (Throwable e) {
                            LOGGER.debug("Error during pull due " + e.getMessage(), e);
                            // we dont want stacktrace in WARNs
                            LOGGER.warn("Error during pull due " + e.getMessage() + ". This exception is ignored.");
                        }
                    }

                    @Override
                    public void stateChanged(CuratorFramework curatorFramework, ConnectionState connectionState) {
                        // ignore
                    }
                });
                counter.start();
            }

        } catch (Exception ex) {
            throw new FabricException("Failed to start GitDataStore:", ex);
        }
    }

    private void deactivateInternal() {
        GitService optsrv = gitService.getOptional();
        if (optsrv != null) {
            optsrv.removeGitListener(gitListener);
        }
        if (threadPool != null) {
            threadPool.shutdown();
            try {
                // Give some time to the running task to complete.
                if (!threadPool.awaitTermination(5, TimeUnit.SECONDS)) {
                    threadPool.shutdownNow();
                }
            } catch (InterruptedException ex) {
                threadPool.shutdownNow();
                // Preserve interrupt status.
                Thread.currentThread().interrupt();
            } catch (Exception ex) {
                throw FabricException.launderThrowable(ex);
            }
        }

        if (defaultProxySelector != null) {
            LOGGER.info("Restoring ProxySelector to original: {}", defaultProxySelector);
            ProxySelector.setDefault(defaultProxySelector);
            // authenticator disabled, until properly tested it does not affect others, as Authenticator is static in the JVM
            // reset authenticator by setting it to null
            // Authenticator.setDefault(null);
        }

        try {
            if (counter != null) {
                counter.close();
                counter = null;
            }
        } catch (IOException e) {
            LOGGER.warn("Error closing SharedCount due " + e.getMessage() + ". This exception is ignored.", e);
        }
    }

    @Override
    public LockHandle aquireWriteLock() {
        final WriteLock writeLock = readWriteLock.writeLock();
        boolean success;
        try {
            success = writeLock.tryLock() || writeLock.tryLock(10, TimeUnit.SECONDS);
        } catch (InterruptedException ex) {
            success = false;
        }
        IllegalStateAssertion.assertTrue(success, "Cannot obtain profile write lock in time");
        return new LockHandle() {
            @Override
            public void unlock() {
                writeLock.unlock();
            }
        };
    }

    @Override
    public LockHandle aquireReadLock() {
        final ReadLock readLock = readWriteLock.readLock();
        boolean success;
        try {
            success = readLock.tryLock() || readLock.tryLock(10, TimeUnit.SECONDS);
        } catch (InterruptedException ex) {
            success = false;
        }
        IllegalStateAssertion.assertTrue(success, "Cannot obtain profile read lock in time");
        return new LockHandle() {
            @Override
            public void unlock() {
                readLock.unlock();
            }
        };
    }

    @SuppressWarnings("unchecked")
    private void importFromFilesystem(Path path) {
        LOGGER.info("Importing additional profiles from file system directory: {}", path);

        List<String> profiles = new ArrayList<String>();

        // find any zip files

        String[] zips = path.toFile().list(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith(".zip");
            }
        });
        int count = zips != null ? zips.length : 0;
        LOGGER.info("Found {} .zip files to import", count);

        if (zips != null && zips.length > 0) {
            for (String name : zips) {
                profiles.add("file:" + path + "/" + name);
                LOGGER.debug("Adding {} .zip file to import", name);
            }
        }

        // look for .properties file which can have list of urls to import
        String[] props = path.toFile().list(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith(".properties");
            }
        });
        count = props != null ? props.length : 0;
        LOGGER.info("Found {} .properties files to import", count);
        try {
            if (props != null && props.length > 0) {
                for (String name : props) {
                    java.util.Properties p = new java.util.Properties();
                    p.load(new FileInputStream(path.resolve(name).toFile()));

                    Enumeration<String> e = (Enumeration<String>) p.propertyNames();
                    while (e.hasMoreElements()) {
                        String key = e.nextElement();
                        String value = p.getProperty(key);

                        if (value != null) {
                            profiles.add(value);
                            LOGGER.debug("Adding {} to import", value);
                        }
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.debug("Error importing profiles due " + e.getMessage(), e);
            // we dont want stacktrace in WARNs
            LOGGER.warn("Error importing profiles due " + e.getMessage() + ". This exception is ignored.", e);
        }

        if (!profiles.isEmpty()) {
            LOGGER.info("Importing additional profiles from {} url locations ...", profiles.size());
            importProfiles(dataStore.get().getDefaultVersion(), profiles);
            LOGGER.info("Importing additional profiles done");
        }
    }

    private Version loadVersion(Git git, String versionId) throws Exception {
        
        // Collect the profiles with parent hierarchy unresolved
        VersionBuilder vbuilder = VersionBuilder.Factory.create(versionId);
        populateVersionBuilder(git, vbuilder, versionId, versionId);
        populateVersionBuilder(git, vbuilder, "master", versionId);
        Version auxVersion = vbuilder.getVersion();
        
        // Use a new version builder for resolved profiles
        vbuilder = VersionBuilder.Factory.create(versionId);
        vbuilder.setAttributes(getVersionAttributesInternal(versionId));
        
        // Resolve the profile hierarchies
        for (Profile profile : auxVersion.getProfiles()) {
            resolveVersionProfiles(vbuilder, auxVersion, profile.getId(), new HashMap<String, Profile>());
        }
        
        return vbuilder.getVersion();
    }

    private void populateVersionBuilder(Git git, VersionBuilder builder, String branch, String versionId) throws GitAPIException, IOException {
        checkoutVersion(git, branch);
        File profilesDir = GitHelpers.getProfilesDirectory(git);
        if (profilesDir.exists()) {
            File[] files = profilesDir.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        populateProfile(git, builder, branch, versionId, file, "");
                    }
                }
            }
        }
    }

    private void populateProfile(Git git, VersionBuilder versionBuilder, String branch, String versionId, File profileFile, String prefix) throws IOException {
        String profileId = profileFile.getName();
        if (Profiles.useDirectoriesForProfiles) {
            if (profileId.endsWith(Profiles.PROFILE_FOLDER_SUFFIX)) {
                profileId = prefix + profileId.substring(0, profileId.length() - Profiles.PROFILE_FOLDER_SUFFIX.length());
            } else {
                // lets recurse all children
                File[] files = profileFile.listFiles();
                if (files != null) {
                    for (File childFile : files) {
                        if (childFile.isDirectory()) {
                            populateProfile(git, versionBuilder, branch, versionId, childFile, prefix + profileFile.getName() + "-");
                        }
                    }
                }
                return;
            }
        }

        Ref versionRef = git.getRepository().getRefDatabase().getRef(branch);
        IllegalStateAssertion.assertNotNull(versionRef, "Cannot get version ref for: " + branch);

        String revision = versionRef.getObjectId().getName();
        String path = GitHelpers.convertProfileIdToDirectory(profileId);
        RevCommit commit = CommitUtils.getLastCommit(git.getRepository(), revision, GitHelpers.CONFIGS_PROFILES + File.separator + path);
        String lastModified = commit != null ? commit.getId().abbreviate(GIT_COMMIT_SHORT_LENGTH).name() : "";

        Map<String, byte[]> fileConfigurations = doGetFileConfigurations(git, profileId);
        Map<String, Map<String, String>> configurations = new HashMap<String, Map<String, String>>();
        for (Map.Entry<String, byte[]> entry : fileConfigurations.entrySet()) {
            if (entry.getKey().endsWith(".properties")) {
                String pid = DataStoreUtils.stripSuffix(entry.getKey(), ".properties");
                configurations.put(pid, DataStoreUtils.toMap(DataStoreUtils.toProperties(entry.getValue())));
            }
        }
        
        ProfileBuilder profileBuilder = ProfileBuilder.Factory.create(versionId, profileId);
        profileBuilder.setFileConfigurations(fileConfigurations).setConfigurations(configurations).setLastModified(lastModified);
        versionBuilder.addProfile(profileBuilder.getProfile());
    }

    private void resolveVersionProfiles(VersionBuilder versionBuilder, Version auxVersion, String profileId, Map<String, Profile> profiles) {
        Profile resolved = profiles.get(profileId);
        if (resolved == null) {
            String versionId = auxVersion.getId();
            Profile auxProfile = auxVersion.getProfile(profileId);
            IllegalStateAssertion.assertNotNull(auxProfile, "Cannot obtain profile '" + profileId + "' from: " + auxVersion);
            String pspec = auxProfile.getAttributes().get(Profile.PARENTS);
            List<String> parents = pspec != null ? Arrays.asList(pspec.split(" ")) : Collections.<String>emptyList();
            for (String parentId : parents) {
                resolveVersionProfiles(versionBuilder, auxVersion, parentId, profiles);
            }
            ProfileBuilder profileBuilder = ProfileBuilder.Factory.create(versionId, profileId);
            profileBuilder.setFileConfigurations(auxProfile.getFileConfigurations());
            profileBuilder.setConfigurations(auxProfile.getConfigurations());
            profileBuilder.setLastModified(auxProfile.getProfileHash());
            for (String parentId : parents) {
                Profile parent = profiles.get(parentId);
                profileBuilder.addParent(parent);
            }
            Profile profile = profileBuilder.getProfile();
            versionBuilder.addProfile(profile);
            profiles.put(profileId, profile);
        }
    }
    
    @Override
    public Map<String, String> getDataStoreProperties() {
        assertValid();
        return Collections.unmodifiableMap(dataStoreProperties);
    }

    @Override
    public String getRemote() {
        return remoteRef.get();
    }

    @Override
    public void setRemote(String remote) {
        IllegalArgumentAssertion.assertNotNull(remote, "Remote name cannot be null");
        this.remoteRef.set(remote);
    }

    @Override
    public void createVersion(final String parentId, final String versionId, final Map<String, String> attributes) {
        LockHandle writeLock = aquireWriteLock();
        try {
            assertValid();
            GitOperation<Void> gitop = new GitOperation<Void>() {
                public Void call(Git git, GitContext context) throws Exception {
                    checkoutVersion(git, parentId);
                    createOrCheckoutVersion(git, versionId);
                    if (attributes != null) {
                        for (Entry<String, String> att : attributes.entrySet()) {
                            setVersionAttributeInternal(context, versionId, att.getKey(), att.getValue());
                        }
                    }
                    context.commitMessage("Create version: " + parentId + " => " + versionId);
                    return null;
                }
            };
            executeWrite(gitop, true);
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public String createVersion(Version version) {
        LockHandle writeLock = aquireWriteLock();
        try {
            assertValid();
            String versionId = version.getId();
            GitContext context = new GitContext();
            createVersionInternal(context, versionId);
            for (Entry<String, String> entry : version.getAttributes().entrySet()) {
                setVersionAttributeInternal(context, versionId, entry.getKey(), entry.getValue());
            }
            for (Profile profile : version.getProfiles()) {
                createOrUpdateProfile(context, profile, true, new HashSet<String>());
            }
            doCommit(getGit(), context.requireCommit().requirePush());
            return versionId;
        } finally {
            writeLock.unlock();
        }
    }

    private void createVersionInternal(GitContext context, final String versionId) {
        assertWriteLock();
        GitOperation<Void> gitop = new GitOperation<Void>() {
            public Void call(Git git, GitContext context) throws Exception {
                createOrCheckoutVersion(git, versionId);
                context.commitMessage("Create version: " + versionId);
                return null;
            }
        };
        executeInternal(context, null, gitop);
    }

    @Override
    public List<String> getVersions() {
        LockHandle readLock = aquireReadLock();
        try {
            assertValid();
            List<String> result = new ArrayList<>(versions);
            Collections.sort(result, VersionSequence.getComparator());
            return Collections.unmodifiableList(result);
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public boolean hasVersion(String versionId) {
        LockHandle readLock = aquireReadLock();
        try {
            assertValid();
            return getVersions().contains(versionId);
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public Version getVersion(String versionId) {
        LockHandle readLock = aquireReadLock();
        try {
            assertValid();
            return getVersionFromCache(versionId);
        } finally {
            readLock.unlock();
        }
    }
    
    @Override
    public Version getRequiredVersion(String versionId) {
        Version version = getVersion(versionId);
        IllegalStateAssertion.assertNotNull(version, "Version does not exist: " + versionId);
        return version;
    }

    @Override
    public void deleteVersion(final String versionId) {
        LockHandle writeLock = aquireWriteLock();
        try {
            assertValid();
            GitOperation<Void> gitop = new GitOperation<Void>() {
                public Void call(Git git, GitContext context) throws Exception {
                    removeVersionFromCaches(versionId);
                    GitHelpers.removeBranch(git, versionId);
                    doPush(git, context, getCredentialsProvider());
                    return null;
                }
            };
            executeWrite(gitop, true);
        } finally {
            writeLock.unlock();
        }
    }

    private Map<String, String> getVersionAttributesInternal(String version) {
        return dataStore.get().getVersionAttributes(version);
    }

    private void setVersionAttributeInternal(GitContext context, String version, String key, String value) {
        dataStore.get().setVersionAttribute(version, key, value);
    }

    private void removeVersionFromCaches(String versionId) {
        versions.remove(versionId);
        versionCache.invalidate(versionId);
    }

    @Override
    public String createProfile(Profile profile) {
        LockHandle writeLock = aquireWriteLock();
        try {
            assertValid();
            GitContext context = new GitContext();
            checkoutProfileBranch(profile.getVersion(), profile.getId());
            String profileId = createOrUpdateProfile(context, profile, true, new HashSet<String>());
            doCommit(getGit(), context.requireCommit().requirePush());
            return profileId;
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public String updateProfile(Profile profile) {
        LockHandle writeLock = aquireWriteLock();
        try {
            assertValid();
            GitContext context = new GitContext();
            checkoutProfileBranch(profile.getVersion(), profile.getId());
            String profileId = createOrUpdateProfile(context, profile, false, new HashSet<String>());
            doCommit(getGit(), context.requireCommit().requirePush());
            return profileId;
        } finally {
            writeLock.unlock();
        }
    }

    private String createOrUpdateProfile(GitContext context, Profile profile, boolean allowCreate, Set<String> profiles) {
        assertWriteLock();

        // Here we only want to do updates on an existing checkout without pull, commit or push 
        IllegalStateAssertion.assertFalse(context.isRequirePull(), "Invalid pull requirement");
        IllegalStateAssertion.assertFalse(context.isRequireCommit(), "Invalid commit requirement");
        IllegalStateAssertion.assertFalse(context.isRequirePush(), "Invalid push requirement");
        
        String versionId = profile.getVersion();
        String profileId = profile.getId();

        if (!profiles.contains(profileId)) {
            
            // Process parents first
            List<Profile> parents = profile.getParents();
            for (Profile parent : parents) {
                createOrUpdateProfile(context, parent, allowCreate, profiles);
            }
            
            // Create the profile branch & directory
            if (allowCreate) {
                createProfileDirectoryAfterCheckout(context, versionId, profileId);
            } else {
                Profile existingProfile = getProfileFromCache(versionId, profileId);
                Set<String> removeFiles = new HashSet<>(existingProfile.getFileConfigurations().keySet());
                removeFiles.removeAll(profile.getFileConfigurations().keySet());
                removeFiles.remove(Constants.AGENT_PID + Profile.PROPERTIES_SUFFIX);
                deleteProfileContent(context, versionId, profileId, removeFiles);
            }

            // FileConfigurations
            Map<String, byte[]> fileConfigurations = profile.getFileConfigurations();
            if (!fileConfigurations.isEmpty()) {
                setFileConfigurationsInternal(context, versionId, profileId, fileConfigurations);
            }
            
            // Configurations
            Map<String, Map<String, String>> configurations = profile.getConfigurations();
            if (!configurations.isEmpty()) {
                setConfigurationsInternal(context, versionId, profileId, configurations);
            }
            
            // A warning commit message if there has been none yet 
            if (context.getCommitMessage().length() == 0) {
                context.commitMessage("WARNING - Profile with no content: " + versionId + "/" + profileId);
            }
            
            // Mark this profile as processed
            profiles.add(profileId);
        }

        return profileId;
    }

    private String checkoutProfileBranch(final String versionId, final String profileId) {
        assertReadLock();
        GitOperation<String> gitop = new GitOperation<String>() {
            public String call(Git git, GitContext context) throws Exception {
                checkoutVersion(git, GitProfiles.getBranch(versionId, profileId));
                return profileId;
            }
        };
        return executeRead(gitop, true);
    }

    private String createProfileDirectoryAfterCheckout(GitContext context, final String versionId, final String profileId) {
        assertWriteLock();
        String resultId = profileId;
        IllegalStateAssertion.assertFalse(context.isRequirePull(), "Cannot require pull when checkout is assumed");
        File profileDirectory = GitHelpers.getProfileDirectory(getGit(), profileId);
        if (!profileDirectory.exists()) {
            GitOperation<String> gitop = new GitOperation<String>() {
                public String call(Git git, GitContext context) throws Exception {
                    return doCreateProfile(git, context, versionId, profileId);
                }
            };
            context = new GitContext(context).setRequirePull(false);
            resultId = executeInternal(context, null, gitop);
        }
        return resultId;
    }

    private void setFileConfigurationsInternal(GitContext context, final String version, final String profile, final Map<String, byte[]> configurations) {
        assertWriteLock();
        GitOperation<Void> gitop = new GitOperation<Void>() {
            public Void call(Git git, GitContext context) throws Exception {
                File profileDirectory = GitHelpers.getProfileDirectory(git, profile);
                doSetFileConfigurations(git, profileDirectory, profile, configurations);
                context.commitMessage("Updated configuration for profile " + profile);
                return null;
            }
        };
        executeInternal(context, null, gitop);
    }

    private void setConfigurationsInternal(GitContext context, final String version, final String profile, final Map<String, Map<String, String>> configurations) {
        assertWriteLock();
        GitOperation<Void> gitop = new GitOperation<Void>() {
            public Void call(Git git, GitContext context) throws Exception {
                File profileDirectory = GitHelpers.getProfileDirectory(git, profile);
                doSetConfigurations(git, profileDirectory, profile, configurations);
                context.commitMessage("Updated configuration for profile " + profile);
                return null;
            }
        };
        executeInternal(context, null, gitop);
    }

    @Override
    public boolean hasProfile(String versionId, String profileId) {
        LockHandle readLock = aquireReadLock();
        try {
            assertValid();
            Profile profile = getProfileFromCache(versionId, profileId);
            return profile != null;
        } finally {
            readLock.unlock();
        }
    }

    private Version getVersionFromCache(String versionId) {
        assertReadLock();
        try {
            return versionCache.get(versionId);
        } catch (ExecutionException e) {
            throw FabricException.launderThrowable(e);
        }
    }

    private Profile getProfileFromCache(String versionId, String profileId) {
        assertReadLock();
        Version version = getVersionFromCache(versionId);
        return version != null ? version.getProfile(profileId) : null;
    }

    @Override
    public Profile getProfile(String versionId, String profileId) {
        LockHandle readLock = aquireReadLock();
        try {
            assertValid();
            return getProfileFromCache(versionId, profileId);
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public Profile getRequiredProfile(String versionId, String profileId) {
        Profile profile = getProfile(versionId, profileId);
        IllegalStateAssertion.assertNotNull(profile, "Cannot obtain profile: " + versionId + "/" + profileId);
        return profile;
    }

    @Override
    public List<String> getProfiles(String versionId) {
        LockHandle readLock = aquireReadLock();
        try {
            assertValid();
            Version version = getVersionFromCache(versionId);
            List<String> profiles = version != null ? version.getProfileIds() : Collections.<String>emptyList();
            return Collections.unmodifiableList(profiles);
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public void deleteProfile(final String versionId, final String profileId) {
        LockHandle writeLock = aquireWriteLock();
        try {
            assertValid();
            deleteProfileInternal(versionId, profileId);
        } finally {
            writeLock.unlock();
        }
    }

    private void deleteProfileInternal(final String versionId, final String profileId) {
        assertWriteLock();
        GitOperation<Void> gitop = new GitOperation<Void>() {
            public Void call(Git git, GitContext context) throws Exception {
                checkoutVersion(git, GitProfiles.getBranch(versionId, profileId));
                File profileDirectory = GitHelpers.getProfileDirectory(git, profileId);
                doRecursiveDeleteAndRemove(git, profileDirectory);
                context.commitMessage("Removed profile " + profileId);
                return null;
            }
        };
        executeWrite(gitop, false);
    }

    private void deleteProfileContent(final GitContext context, final String versionId, final String profileId, final Set<String> removeFiles) {
        assertWriteLock();
        GitOperation<Void> gitop = new GitOperation<Void>() {
            public Void call(Git git, GitContext context) throws Exception {
                File profileDirectory = GitHelpers.getProfileDirectory(git, profileId);
                for (String fileKey : removeFiles) {
                    File file = profileDirectory.toPath().resolve(fileKey).toFile();
                    doRecursiveDeleteAndRemove(git, file);
                }
                return null;
            }
        };
        executeInternal(context, null, gitop);
    }

    @Override
    public void importFromFileSystem(final String importPath) {
        assertValid();

        File sourceDir = new File(importPath);
        if (!sourceDir.isDirectory())
            throw new IllegalArgumentException("Not a valid source dir: " + sourceDir.getAbsolutePath());

        // lets try and detect the old ZooKeeper style file layout and transform it into the git layout
        // so we may /fabric/configs/versions/1.0/profiles => /fabric/profiles in branch 1.0
        File fabricsDir = new File(sourceDir, "fabric");
        File configs = new File(fabricsDir, "configs");
        String defaultVersion = dataStore.get().getDefaultVersion();
        if (configs.exists()) {
            LOGGER.info("Importing the old ZooKeeper layout");
            File versions = new File(configs, "versions");
            if (versions.exists() && versions.isDirectory()) {
                File[] files = versions.listFiles();
                if (files != null) {
                    for (File versionFolder : files) {
                        String version = versionFolder.getName();
                        if (versionFolder.isDirectory()) {
                            File[] versionFiles = versionFolder.listFiles();
                            if (versionFiles != null) {
                                for (File versionFile : versionFiles) {
                                    LOGGER.info("Importing version configuration " + versionFile + " to branch " + version);
                                    importFromFileSystem(versionFile, GitHelpers.CONFIG_ROOT_DIR, version, true);
                                }
                            }
                        }
                    }
                }
            }
            File metrics = new File(fabricsDir, "metrics");
            if (metrics.exists()) {
                LOGGER.info("Importing metrics from " + metrics + " to branch " + defaultVersion);
                importFromFileSystem(metrics, GitHelpers.CONFIG_ROOT_DIR, defaultVersion, false);
            }
        } else {
            // default to version 1.0
            String version = "1.0";
            LOGGER.info("Importing " + fabricsDir + " as version " + version);
            importFromFileSystem(fabricsDir, "", version, false);
        }
    }

    private void importFromFileSystem(final File from, final String destinationPath, final String versionId, final boolean isProfileDir) {
        GitOperation<Void> gitop = new GitOperation<Void>() {
            public Void call(Git git, GitContext context) throws Exception {
                createOrCheckoutVersion(git, versionId);
                // now lets recursively add files
                File toDir = GitHelpers.getRootGitDirectory(git);
                if (Strings.isNotBlank(destinationPath)) {
                    toDir = new File(toDir, destinationPath);
                }
                if (isProfileDir && Profiles.useDirectoriesForProfiles) {
                    recursiveAddLegacyProfileDirectoryFiles(git, from, toDir, destinationPath);
                } else {
                    recursiveCopyAndAdd(git, from, toDir, destinationPath, false);
                }
                context.commitMessage("Imported from " + from);
                return null;
            }
        };
        executeWrite(gitop, true);
    }

    private List<String> forceGetVersions() {
        GitOperation<List<String>> gitop = new GitOperation<List<String>>() {
            public List<String> call(Git git, GitContext context) throws Exception {
                Collection<String> branches = RepositoryUtils.getBranches(git.getRepository());
                List<String> answer = new ArrayList<String>();
                for (String branch : branches) {
                    String name = branch;
                    String prefix = "refs/heads/";
                    if (name.startsWith(prefix)) {
                        name = name.substring(prefix.length());
                        if (!name.equals(MASTER_BRANCH)) {
                            answer.add(name);
                        }
                    }
                }
                versions.clear();
                versions.addAll(answer);
                return answer;
            }
        };
        return executeRead(gitop, false);
    }

    @Override
    public void importProfiles(final String versionId, final List<String> profileZipUrls) {
        assertValid();
        GitOperation<String> gitop = new GitOperation<String>() {
            public String call(Git git, GitContext context) throws Exception {
                checkoutVersion(git, versionId);
                return doImportProfiles(git, context, profileZipUrls);
            }
        };
        executeWrite(gitop, true);
    }

    @Override
    public void exportProfiles(final String versionId, final String outputFileName, String wildcard) {
        assertValid();
        
        final File outputFile = new File(outputFileName);
        outputFile.getParentFile().mkdirs();
        
        // Setup the file filter
        final FileFilter filter;
        if (Strings.isNotBlank(wildcard)) {
            final WildcardFileFilter matcher = new WildcardFileFilter(wildcard);
            filter = new FileFilter() {
                @Override
                public boolean accept(File file) {
                    // match either the file or parent folder
                    boolean answer = matcher.accept(file);
                    if (!answer) {
                        File parentFile = file.getParentFile();
                        if (parentFile != null) {
                            answer = accept(parentFile);
                        }
                    }
                    return answer;
                }
            };
        } else {
            filter = null;
        }
        
        GitOperation<String> gitop = new GitOperation<String>() {
            public String call(Git git, GitContext context) throws Exception {
                checkoutVersion(git, versionId);
                return doExportProfiles(git, context, outputFile, filter);
            }
        };
        executeRead(gitop, false);
    }

    private void doSetConfigurations(Git git, File profileDirectory, String profileId, Map<String, Map<String, String>> configurations) throws IOException, GitAPIException {
        for (Map.Entry<String, Map<String, String>> entry : configurations.entrySet()) {
            String pid = entry.getKey();
            Map<String, String> config = entry.getValue();
            doSetConfiguration(git, profileId, pid, config);
        }
    }

    private void doSetConfiguration(Git git, String profileId, String pid, Map<String, String> configuration) throws IOException, GitAPIException {
        File profileDirectory = GitHelpers.getProfileDirectory(git, profileId);
        File file = new File(profileDirectory, pid + Profile.PROPERTIES_SUFFIX);
        Properties props = new Properties();
        for (Map.Entry<String, String> entry : configuration.entrySet()) {
            props.setProperty(entry.getKey(), entry.getValue());
        }
        props.save(file);
        doAddFiles(git, file);
    }

    private Map<String, byte[]> doGetFileConfigurations(Git git, String profileId) throws IOException {
        Map<String, byte[]> configurations = new HashMap<String, byte[]>();
        File profileDirectory = GitHelpers.getProfileDirectory(git, profileId);
        populateFileConfigurations(configurations, profileDirectory, profileDirectory);
        return configurations;
    }

    private void populateFileConfigurations(Map<String, byte[]> configurations, File profileDirectory, File directory) throws IOException {
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isFile()) {
                    String relativePath = getFilePattern(profileDirectory, file);
                    configurations.put(relativePath, doLoadFileConfiguration(file));
                } else if (file.isDirectory()) {
                    populateFileConfigurations(configurations, profileDirectory, file);
                }
            }
        }
    }

    private void doSetFileConfigurations(Git git, File profileDirectory, String profile, Map<String, byte[]> configurations) throws IOException, GitAPIException {
        for (Map.Entry<String, byte[]> entry : configurations.entrySet()) {
            String file = entry.getKey();
            byte[] newCfg = entry.getValue();
            doSetFileConfiguration(git, profile, file, newCfg);
        }
    }

    private void doSetFileConfiguration(Git git, String profileId, String fileName, byte[] configuration) throws IOException, GitAPIException {
        File profileDirectory = GitHelpers.getProfileDirectory(git, profileId);
        File file = new File(profileDirectory, fileName);
        Files.writeToFile(file, configuration);
        doAddFiles(git, file);
    }

    private Git getGit() {
        return gitService.get().getGit();
    }

    @Override
    public <T> T gitOperation(PersonIdent personIdent, GitOperation<T> operation, boolean pullFirst, GitContext context) {
        assertValid();
        return executeInternal(context.setRequirePull(pullFirst), personIdent, operation);
    }

    private <T> T executeRead(GitOperation<T> operation, boolean pullFirst) {
        return executeInternal(new GitContext().setRequirePull(pullFirst), null, operation);
    }

    private <T> T executeWrite(GitOperation<T> operation, boolean pullFirst) {
        GitContext context = new GitContext().requireCommit().requirePush().setRequirePull(pullFirst);
        return executeInternal(context, null, operation);
    }

    private synchronized <T> T executeInternal(GitContext context, PersonIdent personIdent, GitOperation<T> operation) {
        // Must set the TCCL to the classloader that loaded GitDataStore as we need the classloader
        // that could load this class, as jgit will load resources from classpath using the TCCL
        // and that requires the TCCL to the classloader that could load GitDataStore as the resources
        // jgit requires are in the same bundle as GitDataSource (eg embedded inside fabric-git)
        // see FABRIC-887
        ClassLoader oldCl = Thread.currentThread().getContextClassLoader();
        ClassLoader cl = GitDataStoreImpl.class.getClassLoader();
        Thread.currentThread().setContextClassLoader(cl);
        LOGGER.trace("Setting ThreadContextClassLoader to {} instead of {}", cl, oldCl);
        try {
            Git git = getGit();
            Repository repository = git.getRepository();

            if (personIdent == null) {
                personIdent = new PersonIdent(repository);
            }

            CredentialsProvider credentialsProvider = getCredentialsProvider();
            if (context.isRequirePull()) {
                doPull(git, credentialsProvider, false);
            }

            T answer = operation.call(git, context);

            if (context.isRequireCommit()) {
                doCommit(git, context);
            }

            return answer;
        } catch (Exception e) {
            throw FabricException.launderThrowable(e);
        } finally {
            LOGGER.trace("Restoring ThreadContextClassLoader to {}", oldCl);
            Thread.currentThread().setContextClassLoader(oldCl);
        }
    }

    private void doCommit(Git git, GitContext context) {
        try {
            String message = context.getCommitMessage();
            IllegalStateAssertion.assertTrue(message.length() > 0, "Empty commit message");
            
            // git add --all
            git.add().addFilepattern(".").call();
            
            // git commit -m message
            git.commit().setMessage(message).call();

            if (--commitsWithoutGC < 0) {
                commitsWithoutGC = MAX_COMMITS_WITHOUT_GC;
                LOGGER.debug("Performing 'git gc' after {} commits", MAX_COMMITS_WITHOUT_GC);
                git.gc().call();
            }

            // Clear caches on successful commit
            versionCache.invalidateAll();

            // Notify on successful commit
            if (context.isRequirePush()) {
                doPush(git, context, getCredentialsProvider());
            }
        } catch (GitAPIException ex) {
            throw FabricException.launderThrowable(ex);
        } finally {
            dataStore.get().fireChangeNotifications();
        }
    }

    /**
     * Pushes any changes - assumed to be invoked within a gitOperation method!
     */
    @Override
    public Iterable<PushResult> doPush(Git git, GitContext context) throws Exception {
        assertValid();
        return doPush(git, context, getCredentialsProvider());
    }

    /**
     * Pushes any committed changes to the remote repo
     */
    private Iterable<PushResult> doPush(Git git, GitContext context, CredentialsProvider credentialsProvider) {
        Iterable<PushResult> results = Collections.emptyList();
        try {
            Repository repository = git.getRepository();
            StoredConfig config = repository.getConfig();
            String url = config.getString("remote", remoteRef.get(), "url");
            if (Strings.isNullOrBlank(url)) {
                LOGGER.info("No remote repository defined yet for the git repository at " + GitHelpers.getRootGitDirectory(git) + " so not doing a push");
            } else {
                results = git.push().setTimeout(gitTimeout).setCredentialsProvider(credentialsProvider).setPushAll().call();
            }
        } catch (Throwable ex) {
            LOGGER.debug("Failed to push from the remote git repo " + GitHelpers.getRootGitDirectory(git) + ". This exception is ignored.", ex);
            LOGGER.warn("Failed to push from the remote git repo " + GitHelpers.getRootGitDirectory(git) + " due " + ex.getMessage() + ". This exception is ignored.");
        }
        return results;
    }

    private CredentialsProvider getCredentialsProvider() {
        Map<String, String> properties = getDataStoreProperties();
        String username;
        String password;
        if (isExternalGitConfigured(properties)) {
            username = getExternalUser(properties);
            password = getExternalCredential(properties);
        } else {
            RuntimeProperties sysprops = runtimeProperties.get();
            username = getContainerLogin(sysprops);
            password = generateContainerToken(sysprops, curator.get());
        }
        return new UsernamePasswordCredentialsProvider(username, password);
    }

    /**
     * Check if the datastore has been configured with an external git repository.
     */
    private boolean isExternalGitConfigured(Map<String, String> properties) {
        return properties != null && properties.containsKey(GIT_REMOTE_USER) && properties.containsKey(GIT_REMOTE_PASSWORD);
    }

    private String getExternalUser(Map<String, String> properties) {
        return properties.get(GIT_REMOTE_USER);
    }

    private String getExternalCredential(Map<String, String> properties) {
        return properties.get(GIT_REMOTE_PASSWORD);
    }

    /**
     * Performs a pull so the git repo is pretty much up to date before we start performing operations on it.
     *
     * @param git                 The {@link Git} instance to use.
     * @param credentialsProvider The {@link CredentialsProvider} to use.
     * @param doDeleteBranches    Flag that determines if local branches that don't exist in remote should get deleted.
     */
    private void doPull(Git git, CredentialsProvider credentialsProvider, boolean doDeleteBranches) {
        try {
            Repository repository = git.getRepository();
            StoredConfig config = repository.getConfig();
            String url = config.getString("remote", remoteRef.get(), "url");
            if (Strings.isNullOrBlank(url)) {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("No remote repository defined for the git repository at " + GitHelpers.getRootGitDirectory(git) + " so not doing a pull");
                }
                return;
            }
            
            // Reset the workspace
            doResetHard(git);
            
            boolean hasChanged = false;
            try {
                LOGGER.debug("Performing a fetch in git repository {} on remote URL: {}", GitHelpers.getRootGitDirectory(git), url);
                FetchResult result = git.fetch().setTimeout(gitTimeout).setCredentialsProvider(credentialsProvider).setRemote(remoteRef.get()).call();
                LOGGER.debug("Git fetch result: {}", result.getMessages());
                lastFetchWarning = null;
            } catch (Exception ex) {
                String fetchWarning = ex.getMessage();
                if (!fetchWarning.equals(lastFetchWarning)) {
                    LOGGER.warn("Fetch failed because of: " + fetchWarning);
                    LOGGER.debug("Fetch failed - the error will be ignored", ex);
                    lastFetchWarning = fetchWarning;
                }
                return;
            }

            // Get local and remote branches
            Map<String, Ref> localBranches = new HashMap<String, Ref>();
            Map<String, Ref> remoteBranches = new HashMap<String, Ref>();
            Set<String> gitVersions = new HashSet<String>();
            for (Ref ref : git.branchList().setListMode(ListBranchCommand.ListMode.ALL).call()) {
                if (ref.getName().startsWith("refs/remotes/" + remoteRef.get() + "/")) {
                    String name = ref.getName().substring(("refs/remotes/" + remoteRef.get() + "/").length());
                    remoteBranches.put(name, ref);
                    gitVersions.add(name);
                } else if (ref.getName().startsWith("refs/heads/")) {
                    String name = ref.getName().substring(("refs/heads/").length());
                    localBranches.put(name, ref);
                    gitVersions.add(name);
                }
            }

            // Check git commits
            for (String version : gitVersions) {
                // Delete unneeded local branches.
                // Check if any remote branches was found as a guard for unwanted deletions.
                if (remoteBranches.isEmpty()) {
                    // Do nothing
                } else if (!remoteBranches.containsKey(version)) {
                    // We never want to delete the master branch.
                    if (doDeleteBranches && !version.equals(MASTER_BRANCH)) {
                        try {
                            git.branchDelete().setBranchNames(localBranches.get(version).getName()).setForce(true).call();
                        } catch (CannotDeleteCurrentBranchException ex) {
                            git.checkout().setName(MASTER_BRANCH).setForce(true).call();
                            git.branchDelete().setBranchNames(localBranches.get(version).getName()).setForce(true).call();
                        }
                        removeVersionFromCaches(version);
                        hasChanged = true;
                    }
                }
                // Create new local branches
                else if (!localBranches.containsKey(version)) {
                    addVersion(version);
                    git.checkout().setCreateBranch(true).setName(version).setStartPoint(remoteRef.get() + "/" + version)
                            .setUpstreamMode(CreateBranchCommand.SetupUpstreamMode.TRACK).setForce(true).call();
                    hasChanged = true;
                } else {
                    String localCommit = localBranches.get(version).getObjectId().getName();
                    String remoteCommit = remoteBranches.get(version).getObjectId().getName();
                    if (!localCommit.equals(remoteCommit)) {
                        git.clean().setCleanDirectories(true).call();
                        git.checkout().setName("HEAD").setForce(true).call();
                        git.checkout().setName(version).setForce(true).call();
                        MergeResult result = git.merge().setStrategy(MergeStrategy.THEIRS).include(remoteBranches.get(version).getObjectId()).call();
                        if (result.getMergeStatus() != MergeResult.MergeStatus.ALREADY_UP_TO_DATE && hasChanged(git, localCommit, remoteCommit)) {
                            hasChanged = true;
                        }
                        // TODO: handle conflicts
                    }
                }
            }
            if (hasChanged) {
                LOGGER.debug("Changed after pull!");
                if (credentialsProvider != null) {
                    // TODO lets test if the profiles directory is present after checking out version 1.0?
                    GitHelpers.getProfilesDirectory(git);
                }
                dataStore.get().fireChangeNotifications();
            }
        } catch (Throwable ex) {
            LOGGER.debug("Failed to pull from the remote git repo " + GitHelpers.getRootGitDirectory(git), ex);
            LOGGER.warn("Failed to pull from the remote git repo " + GitHelpers.getRootGitDirectory(git) + " due " + ex.getMessage() + ". This exception is ignored.");
        }
    }
    
    private void doResetHard(Git git) throws GitAPIException {
        ResetCommand resetCmd = git.reset().setMode(ResetType.HARD);
        resetCmd.call();
    }
    
    /**
     * Creates the given profile directory in the currently checked out version branch
     */
    private String doCreateProfile(Git git, GitContext context, String versionId, String profileId) throws IOException, GitAPIException {
        File profileDirectory = GitHelpers.getProfileDirectory(git, profileId);
        File metadataFile = new File(profileDirectory, AGENT_METADATA_FILE);
        IllegalStateAssertion.assertFalse(metadataFile.exists(), "Profile metadata file already exists: " + metadataFile);
        profileDirectory.mkdirs();
        Files.writeToFile(metadataFile, "#Profile:" + profileId + "\n", Charset.defaultCharset());
        doAddFiles(git, profileDirectory, metadataFile);
        context.commitMessage("Added profile " + profileId);
        return profileId;
    }

    /**
     * Imports one or more profile zips into the given version
     */
    private String doImportProfiles(Git git, GitContext context, List<String> profileZipUrls) throws GitAPIException, IOException {
        // we cannot use fabricService as it has not been initialized yet, so we can only support
        // dynamic version of one token ${version:fabric} in the urls
        String fabricVersion = dataStore.get().getFabricReleaseVersion();

        File profilesDirectory = GitHelpers.getProfilesDirectory(git);
        for (String profileZipUrl : profileZipUrls) {
            String token = "\\$\\{version:fabric\\}";
            String url = profileZipUrl.replaceFirst(token, fabricVersion);
            URL zipUrl;
            try {
                zipUrl = new URL(url);
            } catch (MalformedURLException e) {
                throw new IOException("Failed to create URL for " + url + ". " + e, e);
            }
            InputStream inputStream = zipUrl.openStream();
            if (inputStream == null) {
                throw new IOException("Could not open zip: " + url);
            }
            try {
                Zips.unzip(inputStream, profilesDirectory);
            } catch (IOException e) {
                throw new IOException("Failed to unzip " + url + ". " + e, e);
            }
        }
        doAddFiles(git, profilesDirectory);
        context.commitMessage("Added profile zip(s) " + profileZipUrls);
        return null;
    }

    /**
     * exports one or more profile folders from the given version into the zip
     */
    private String doExportProfiles(Git git, GitContext context, File outputFile, FileFilter filter) throws IOException {
        File profilesDirectory = GitHelpers.getProfilesDirectory(git);
        Zips.createZipFile(LOGGER, profilesDirectory, outputFile, filter);
        return null;
    }

    /**
     * Recursively copies the given files from the given directory to the specified directory
     * adding them to the git repo along the way
     */
    private void recursiveCopyAndAdd(Git git, File from, File toDir, String path, boolean useToDirAsDestination) throws GitAPIException, IOException {
        String name = from.getName();
        String pattern = path + (path.length() > 0 && !path.endsWith(File.separator) ? File.separator : "") + name;
        File toFile = new File(toDir, name);

        if (from.isDirectory()) {
            if (useToDirAsDestination) {
                toFile = toDir;
            }
            toFile.mkdirs();
            File[] files = from.listFiles();
            if (files != null) {
                for (File file : files) {
                    recursiveCopyAndAdd(git, file, toFile, pattern, false);
                }
            }
        } else {
            Files.copy(from, toFile);
        }
        git.add().addFilepattern(fixFilePattern(pattern)).call();
    }

    /**
     * Recursively copies the profiles in a single flat directory into the new
     * directory layout; changing "foo-bar" directory into "foo/bar.profile" along the way
     */
    private void recursiveAddLegacyProfileDirectoryFiles(Git git, File from, File toDir, String path) throws GitAPIException, IOException {
        if (!from.isDirectory()) {
            throw new IllegalStateException("Should only be invoked on the profiles directory but was given file " + from);
        }
        String name = from.getName();
        String pattern = path + (path.length() > 0 && !path.endsWith(File.separator) ? File.separator : "") + name;
        File[] profiles = from.listFiles();
        File toFile = new File(toDir, name);
        if (profiles != null) {
            for (File profileDir : profiles) {
                // TODO should we try and detect regular folders somehow using some naming convention?
                if (isProfileDirectory(profileDir)) {
                    String profileId = profileDir.getName();
                    String toProfileDirName = GitHelpers.convertProfileIdToDirectory(profileId);
                    File toProfileDir = new File(toFile, toProfileDirName);
                    toProfileDir.mkdirs();
                    recursiveCopyAndAdd(git, profileDir, toProfileDir, pattern, true);
                } else {
                    recursiveCopyAndAdd(git, profileDir, toFile, pattern, false);
                }
            }
        }
        git.add().addFilepattern(fixFilePattern(pattern)).call();
    }

    private boolean isProfileDirectory(File profileDir) {
        if (profileDir.isDirectory()) {
            String[] list = profileDir.list();
            if (list != null) {
                for (String file : list) {
                    if (file.endsWith(Profile.PROPERTIES_SUFFIX) || file.endsWith(".mvel")) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private void pull() {
        if (isValid()) {
            try {
                GitOperation<Object> gitop = new GitOperation<Object>() {
                    public Object call(Git git, GitContext context) throws Exception {
                        return null;
                    }
                };
                executeRead(gitop, true);
            } catch (Exception e) {
                LOGGER.warn("Failed to perform a pull " + e, e);
            }
        }
    }

    private void push() {
        if (isValid()) {
            try {
                GitOperation<Object> gitop = new GitOperation<Object>() {
                    public Object call(Git git, GitContext context) throws Exception {
                        context.requirePush();
                        return null;
                    }
                };
                executeRead(gitop, false);
            } catch (Exception e) {
                LOGGER.warn("Failed to perform a pull " + e, e);
            }
        }
    }

    private void createOrCheckoutVersion(Git git, String versionId) throws GitAPIException {
        assertWriteLock();
        GitHelpers.createOrCheckoutBranch(git, versionId, remoteRef.get());
        addVersion(versionId);
    }

    private void checkoutVersion(Git git, String version) throws GitAPIException {
        GitHelpers.checkoutBranch(git, version);
        addVersion(version);
    }

    private void doAddFiles(Git git, File... files) throws GitAPIException, IOException {
        File rootDir = GitHelpers.getRootGitDirectory(git);
        for (File file : files) {
            String relativePath = getFilePattern(rootDir, file);
            git.add().addFilepattern(relativePath).call();
        }
    }

    private void doRecursiveDeleteAndRemove(Git git, File file) throws IOException, GitAPIException {
        File rootDir = GitHelpers.getRootGitDirectory(git);
        String relativePath = getFilePattern(rootDir, file);
        if (file.exists() && !relativePath.equals(".git")) {
            if (file.isDirectory()) {
                File[] files = file.listFiles();
                if (files != null) {
                    for (File child : files) {
                        doRecursiveDeleteAndRemove(git, child);
                    }
                }
            }
            file.delete();
            git.rm().addFilepattern(relativePath).call();
        }
    }

    private byte[] doLoadFileConfiguration(File file) throws IOException {
        if (file.isDirectory()) {
            // Not sure why we do this, but for directory pids, lets recurse...
            StringBuilder buf = new StringBuilder();
            File[] files = file.listFiles();
            if (files != null) {
                for (File child : files) {
                    String value = Files.toString(child);
                    buf.append(String.format("%s = %s\n", child.getName(), value));
                }
            }
            return buf.toString().getBytes();
        } else if (file.exists() && file.isFile()) {
            return Files.readBytes(file);
        }
        return null;
    }

    private String fixFilePattern(String pattern) {
        return pattern.replace(File.separatorChar, '/');
    }

    private String getFilePattern(File rootDir, File file) throws IOException {
        String relativePath = Files.getRelativePath(rootDir, file);
        if (relativePath.startsWith(File.separator)) {
            relativePath = relativePath.substring(1);
        }
        return fixFilePattern(relativePath);
    }

    /**
     * Checks if there is an actual difference between two commits.
     * In some cases a container may push a commit, without actually modifying anything.
     * So comparing the commit hashes is not always enough. We need to actually diff the two commits.
     *
     * @param git    The {@link Git} instance to use.
     * @param before The hash of the first commit.
     * @param after  The hash of the second commit.
     */
    private boolean hasChanged(Git git, String before, String after) throws IOException, GitAPIException {
        if (isCommitEqual(before, after)) {
            return false;
        }
        Repository db = git.getRepository();
        List<DiffEntry> entries = git.diff().setOldTree(getTreeIterator(db, before)).setNewTree(getTreeIterator(db, after)).call();
        return entries.size() > 0;
    }

    private AbstractTreeIterator getTreeIterator(Repository db, String name) throws IOException {
        final ObjectId id = db.resolve(name);
        if (id == null)
            throw new IllegalArgumentException(name);
        final CanonicalTreeParser p = new CanonicalTreeParser();
        final ObjectReader or = db.newObjectReader();
        try {
            p.reset(or, new RevWalk(db).parseTree(id));
            return p;
        } finally {
            or.release();
        }
    }

    private static boolean isCommitEqual(Object a, Object b) {
        return (a == b) || (a != null && a.equals(b));
    }

    private void addVersion(String versionId) {
        if (!MASTER_BRANCH.equals(versionId)) {
            versions.add(versionId);
        }
    }

    private void assertReadLock() {
        //IllegalStateAssertion.assertTrue(readWriteLock.getReadLockCount() > 0 || readWriteLock.isWriteLocked(), "No read lock obtained");
        if (!(readWriteLock.getReadLockCount() > 0 || readWriteLock.isWriteLocked())) 
            LOGGER.warn("No read lock obtained");
    }

    private void assertWriteLock() {
        //IllegalStateAssertion.assertTrue(readWriteLock.isWriteLocked(), "No write lock obtained");
        if (!readWriteLock.isWriteLocked()) 
            LOGGER.warn("No write lock obtained");
    }

    class GitDataStoreListener implements GitListener {

        @Override
        public void onRemoteUrlChanged(final String updatedUrl) {
            final String actualUrl = configuredUrl != null ? configuredUrl : updatedUrl;
            if (isValid()) {
                threadPool.submit(new Runnable() {
                    @Override
                    public void run() {
                        if (isValid()) {
                            GitOperation<Void> gitop = new GitOperation<Void>() {
                                @Override
                                public Void call(Git git, GitContext context) throws Exception {
                                    Repository repository = git.getRepository();
                                    StoredConfig config = repository.getConfig();
                                    String currentUrl = config.getString("remote", "origin", "url");
                                    if (actualUrl != null && !actualUrl.equals(currentUrl)) {
                                        LOGGER.info("Performing on remote url changed from: {} to: {}", currentUrl, actualUrl);
                                        remoteUrl = actualUrl;
                                        config.setString("remote", "origin", "url", actualUrl);
                                        config.setString("remote", "origin", "fetch", "+refs/heads/*:refs/remotes/origin/*");
                                        config.save();
                                        //Make sure that we don't delete branches at this pull.
                                        doPull(git, getCredentialsProvider(), false);
                                        doPush(git, context);
                                    }
                                    return null;
                                }
                            };
                            executeRead(gitop, true);
                        }
                    }

                    @Override
                    public String toString() {
                        return "RemoteUrlChangedTask";
                    }
                });
            }
        }

        @Override
        public void onReceivePack() {
            assertValid();
            versionCache.invalidateAll();
        }
    }

    /**
     * A {@link java.net.ProxySelector} that uses the {@link io.fabric8.git.GitProxyService} to handle
     * proxy git communication if needed.
     */
    static class FabricGitLocalHostProxySelector extends ProxySelector {

        final static String GIT_FABRIC_PATH = "/git/fabric/";

        final ProxySelector delegate;
        final GitProxyService proxyService;
        final List<Proxy> noProxy;

        FabricGitLocalHostProxySelector(ProxySelector delegate, GitProxyService proxyService) {
            this.delegate = delegate;
            this.proxyService = proxyService;
            this.noProxy = new ArrayList<Proxy>(1);
            this.noProxy.add(Proxy.NO_PROXY);
        }

        @Override
        public List<Proxy> select(URI uri) {
            String host = uri.getHost();
            String path = uri.getPath();
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("ProxySelector uri: {}", uri);
                LOGGER.trace("ProxySelector nonProxyHosts {}", proxyService.getNonProxyHosts());
                LOGGER.trace("ProxySelector proxyHost {}", proxyService.getProxyHost());
            }

            // we should only intercept when its a git/fabric request
            List<Proxy> answer;
            if (path != null && path.startsWith(GIT_FABRIC_PATH)) {
                answer = doSelect(host, proxyService.getNonProxyHosts(), proxyService.getProxyHost(), proxyService.getProxyPort());
            } else {
                // use delegate
                answer = delegate.select(uri);
            }

            LOGGER.debug("ProxySelector uri: {} -> {}", uri, answer);
            return answer;
        }

        private List<Proxy> doSelect(String host, String nonProxy, String proxyHost, int proxyPort) {
            // match any non proxy
            if (nonProxy != null) {
                StringTokenizer st = new StringTokenizer(nonProxy, "|", false);
                while (st.hasMoreTokens()) {
                    String token = st.nextToken();
                    if (host.matches(token)) {
                        return noProxy;
                    }
                }
            }

            // okay then it should proxy if we have a proxy setting
            if (proxyHost != null) {
                InetSocketAddress adr = InetSocketAddress.createUnresolved(proxyHost, proxyPort);
                List<Proxy> answer = new ArrayList<Proxy>(1);
                answer.add(new Proxy(Proxy.Type.HTTP, adr));
                return answer;
            } else {
                // use no proxy
                return noProxy;
            }
        }

        @Override
        public void connectFailed(URI uri, SocketAddress sa, IOException ioe) {
            delegate.connectFailed(uri, sa, ioe);
        }
    }

    void bindConfigurer(Configurer service) {
        this.configurer = service;
    }
    void unbindConfigurer(Configurer service) {
        this.configurer = null;
    }

    void bindCurator(CuratorFramework service) {
        this.curator.bind(service);
    }
    void unbindCurator(CuratorFramework service) {
        this.curator.unbind(service);
    }

    void bindDataStore(DataStore service) {
        this.dataStore.bind(service);
    }
    void unbindDataStore(DataStore service) {
        this.dataStore.unbind(service);
    }

    void bindGitProxyService(GitProxyService service) {
        this.gitProxyService.bind(service);
    }
    void unbindGitProxyService(GitProxyService service) {
        this.gitProxyService.unbind(service);
    }

    void bindGitService(GitService service) {
        this.gitService.bind(service);
    }
    void unbindGitService(GitService service) {
        this.gitService.unbind(service);
    }

    void bindProfileBuilders(ProfileBuilders service) {
        this.profileBuilders.bind(service);
    }
    void unbindProfileBuilders(ProfileBuilders service) {
        this.profileBuilders.unbind(service);
    }
    
    void bindRuntimeProperties(RuntimeProperties service) {
        this.runtimeProperties.bind(service);
    }
    void unbindRuntimeProperties(RuntimeProperties service) {
        this.runtimeProperties.unbind(service);
    }
}