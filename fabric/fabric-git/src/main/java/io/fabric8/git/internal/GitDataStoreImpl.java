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
import io.fabric8.api.visibility.VisibleForExternal;
import io.fabric8.common.util.Files;
import io.fabric8.common.util.Strings;
import io.fabric8.common.util.Zips;
import io.fabric8.git.GitDataStore;
import io.fabric8.git.GitListener;
import io.fabric8.git.GitProxyService;
import io.fabric8.git.GitService;
import io.fabric8.git.PullPushPolicy;
import io.fabric8.git.PullPushPolicy.PullPolicyResult;
import io.fabric8.git.PullPushPolicy.PushPolicyResult;
import io.fabric8.service.EnvPlaceholderResolver;
import io.fabric8.utils.DataStoreUtils;
import io.fabric8.zookeeper.ZkPath;
import io.fabric8.zookeeper.utils.ZooKeeperUtils;

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
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
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
import org.apache.zookeeper.KeeperException;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.PushResult;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.gitective.core.RepositoryUtils;
import io.fabric8.api.gravia.IllegalArgumentAssertion;
import io.fabric8.api.gravia.IllegalStateAssertion;
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
@Component(name = Constants.DATASTORE_PID,
        label = "Fabric8 Git DataStore",
        description = "Configuration of the git based configuration data store for Fabric8",
        policy = ConfigurationPolicy.OPTIONAL, immediate = true, metatype = true)
@Service({ GitDataStore.class, ProfileRegistry.class })
public final class GitDataStoreImpl extends AbstractComponent implements GitDataStore, ProfileRegistry {

    private static final transient Logger LOGGER = LoggerFactory.getLogger(GitDataStoreImpl.class);


    private static final String GIT_REMOTE_USER = "gitRemoteUser";
    private static final String GIT_REMOTE_PASSWORD = "gitRemotePassword";
    private static final int GIT_COMMIT_SHORT_LENGTH = 7;
    private static final int MAX_COMMITS_WITHOUT_GC = 40;
    private static final long AQUIRE_LOCK_TIMEOUT = 25 * 1000L;

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

    private final ImportExportHandler importExportHandler = new ImportExportHandler();
    private final GitDataStoreListener gitListener = new GitDataStoreListener();
    private final ReentrantReadWriteLock readWriteLock = new ReentrantReadWriteLock();
    private final boolean strictLockAssert = true;

    private int commitsWithoutGC = MAX_COMMITS_WITHOUT_GC;
    private Map<String, String> dataStoreProperties;
    private ProxySelector defaultProxySelector;
    private PullPushPolicy pullPushPolicy;
    private boolean notificationRequired;
    private SharedCount counter;
    private String remoteUrl;

    @Property(name = Constants.GIT_REMOTE_URL, label = "External Git Repository URL", description = "The URL to a fixed external git repository")
    private String gitRemoteUrl;
    @Property(name = "gitTimeout", label = "Timeout", description = "Timeout connecting to remote git server (value in seconds)")
    private int gitTimeout = 5;
    @Property(name = "importDir", label = "Import Directory", description = "Directory to import additional profiles", value = "fabric")
    private String importDir = "fabric";
    @Property(name = "gitRemotePollInterval", label = "Remote poll Interval", description = "The interval between remote repo polling operations")
    private long gitRemotePollInterval = 60 * 1000L;

    private final LoadingCache<String, Version> versionCache = CacheBuilder.newBuilder().build(new VersionCacheLoader());
    private final Set<String> versions = new HashSet<String>();

    @Activate
    @VisibleForExternal
    public void activate(Map<String, ?> configuration) throws Exception {
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
        this.pullPushPolicy = new DefaultPullPushPolicy(getGit(), GitHelpers.REMOTE_ORIGIN, gitTimeout);

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

    private void activateInternal() throws Exception {
        
        LOGGER.info("Starting up GitDataStore " + this);

        // Call the bootstrap {@link DataStoreTemplate}
        DataStoreTemplate template = runtimeProperties.get().removeRuntimeAttribute(DataStoreTemplate.class);
        if (template != null) {
            
            // Do the initial commit and set the root tag
            Ref rootTag = getGit().getRepository().getRef(GitHelpers.ROOT_TAG);
            if (rootTag == null) {
                getGit().commit().setMessage("First Commit").setCommitter("fabric", "user@fabric").call();
                getGit().tag().setName(GitHelpers.ROOT_TAG).setMessage("Tag the root commit").call();
            }
            
            LOGGER.debug("Running datastore bootstrap template: " + template);
            template.doWith(this, dataStore.get());
        }

        // Setup proxy service
        GitProxyService proxyService = gitProxyService.get();
        defaultProxySelector = ProxySelector.getDefault();
        
        // authenticator disabled, until properly tested it does not affect others, as Authenticator is static in the JVM
        // Authenticator.setDefault(new FabricGitLocalHostAuthenticator(proxyService));
        ProxySelector fabricProxySelector = new FabricGitLocalHostProxySelector(defaultProxySelector, proxyService);
        ProxySelector.setDefault(fabricProxySelector);
        LOGGER.debug("Setting up FabricProxySelector: {}", fabricProxySelector);

        if (gitRemoteUrl != null) {
            gitListener.runRemoteUrlChanged(gitRemoteUrl);
            remoteUrl = gitRemoteUrl;
        } else {
            gitService.get().addGitListener(gitListener);
            remoteUrl = gitService.get().getRemoteUrl();
            if (remoteUrl != null) {
                gitListener.runRemoteUrlChanged(remoteUrl);
            }
        }

        // Get initial versions
        getInitialVersions();

        // poll logic in case of remote git repo
        if(gitRemoteUrl != null) {
            // i need this old logic in case of remote repos
            LOGGER.info("Starting to pull from remote git repository every {} millis", gitRemotePollInterval);
            threadPool.scheduleWithFixedDelay(new Runnable() {
                @Override
                public void run() {
                    LockHandle writeLock = aquireWriteLock();
                    try {
                        LOGGER.trace("Performing timed pull");
                        doPullInternal();
                        LOGGER.debug("Performed timed pull from external git repo");
                    } catch (Throwable e) {
                        LOGGER.debug("Error during performed timed pull/push due " + e.getMessage(), e);
                        LOGGER.warn("Error during performed timed pull/push due " + e.getMessage() + ". This exception is ignored.");
                    } finally {
                        writeLock.unlock();
                    }
                }

                @Override
                public String toString() {
                    return "TimedPushTask";
                }
            }, 1000, gitRemotePollInterval, TimeUnit.MILLISECONDS);
        }

        LOGGER.info("Using ZooKeeper SharedCount to react when master git repo is changed, so we can do a git pull to the local git repo.");
        counter = new SharedCount(curator.get(), ZkPath.GIT_TRIGGER.getPath(), 0);
        counter.addListener(new SharedCountListener() {
            @Override
            public void countHasChanged(final SharedCountReader sharedCountReader, final int value) throws Exception {
               threadPool.submit(new Runnable() {
                   @Override
                   public void run() {
                       LOGGER.debug("Watch counter updated to " + value + ", doing a pull");
                       doPullInternal();
                   }
               });
            }

            @Override
            public void stateChanged(CuratorFramework curatorFramework, ConnectionState connectionState) {
                switch (connectionState) {
                    case SUSPENDED:
                    case READ_ONLY:
                    case LOST:
                        // do nothing
                        break;
                    case CONNECTED:
                    case RECONNECTED:
                        LOGGER.info("Shared Counter (Re)connected, doing a pull");
                        doPullInternal();
                        break;
                }
            }
        });
        try {
            counter.start();
        } catch (KeeperException.NotReadOnlyException ex) {
            //In read only mode the counter is not going to start.
            //If the connection is reestablished the component will reactivate.
            //We need to catch this error so that the component gets activated.
        }


        //It is not safe to assume that we will get notified by the ShareCounter, if the component is not activated
        // when the SharedCounter gets updated.
        //Also we cannot rely on the remote url change event, as it will only trigger when there is an actual change.
        //So we should be awesome and always attempt a pull when we are activating if we don't want to loose stuff.
        doPullInternal();
    }

    private void deactivateInternal() {
        
        // Remove the GitListener
        gitService.get().removeGitListener(gitListener);
        
        // Shutdown the thread pool
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

        LOGGER.debug("Restoring ProxySelector to original: {}", defaultProxySelector);
        ProxySelector.setDefault(defaultProxySelector);
        // authenticator disabled, until properly tested it does not affect others, as Authenticator is static in the JVM
        // reset authenticator by setting it to null
        // Authenticator.setDefault(null);

        // Closing the shared counter
        try {
            counter.close();
        } catch (IOException ex) {
            LOGGER.warn("Error closing SharedCount due " + ex.getMessage() + ". This exception is ignored.");
        }
    }
    
    @Override
    public Git getGit() {
        return gitService.get().getGit();
    }

    @Override
    public LockHandle aquireWriteLock() {
        final WriteLock writeLock = readWriteLock.writeLock();
        boolean success;
        try {
            success = writeLock.tryLock() || writeLock.tryLock(AQUIRE_LOCK_TIMEOUT, TimeUnit.MILLISECONDS);
        } catch (InterruptedException ex) {
            success = false;
        }
        IllegalStateAssertion.assertTrue(success, "Cannot obtain profile write lock in time");
        return new LockHandle() {
            @Override
            public void unlock() {
                if (notificationRequired && readWriteLock.getWriteHoldCount() == 1) {
                    try {
                        dataStore.get().fireChangeNotifications();
                    } finally {
                        notificationRequired = false;
                    }
                }
                writeLock.unlock();
            }
        };
    }

    @Override
    public LockHandle aquireReadLock() {
        final ReadLock readLock = readWriteLock.readLock();
        boolean success;
        try {
            success = readLock.tryLock() || readLock.tryLock(AQUIRE_LOCK_TIMEOUT, TimeUnit.MILLISECONDS);
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

    private List<String> getInitialVersions() {
        LockHandle readLock = aquireReadLock();
        try {
            GitOperation<List<String>> gitop = new GitOperation<List<String>>() {
                public List<String> call(Git git, GitContext context) throws Exception {
                    Collection<String> branches = RepositoryUtils.getBranches(git.getRepository());
                    List<String> answer = new ArrayList<String>();
                    for (String branch : branches) {
                        String name = branch;
                        String prefix = "refs/heads/";
                        if (name.startsWith(prefix)) {
                            name = name.substring(prefix.length());
                            if (!name.equals(GitHelpers.MASTER_BRANCH)) {
                                answer.add(name);
                            }
                        }
                    }
                    versions.clear();
                    versions.addAll(answer);
                    return answer;
                }
            };
            return executeRead(gitop);
        } finally {
            readLock.unlock();
        }
    }
    
    @Override
    public Map<String, String> getDataStoreProperties() {
        return Collections.unmodifiableMap(dataStoreProperties);
    }

    private Version getVersionFromCache(String versionId, String profileId) {
        LockHandle writeLock = aquireWriteLock();
        try {
            assertValid();
            String branch = GitHelpers.getProfileBranch(versionId, profileId);
            if (GitHelpers.localBranchExists(getGit(), branch)) {
                return versionCache.get(versionId);
            } else {
                return null;
            }
        } catch (Exception e) {
            throw FabricException.launderThrowable(e);
        } finally {
            writeLock.unlock();
        }
    }

    private Profile getProfileFromCache(String versionId, String profileId) {
        Version version = getVersionFromCache(versionId, profileId);
        return version != null ? version.getProfile(profileId) : null;
    }

    @Override
    public String createVersion(final String sourceId, final String targetId, final Map<String, String> attributes) {
        return createVersion(newGitWriteContext(), sourceId, targetId, attributes);
    }

    @Override
    public String createVersion(GitContext context, final String sourceId, final String targetId, final Map<String, String> attributes) {
        IllegalStateAssertion.assertNotNull(sourceId, "sourceId");
        IllegalStateAssertion.assertNotNull(targetId, "targetId");
        LockHandle writeLock = aquireWriteLock();
        try {
            assertValid();
            LOGGER.debug("Create version: {} => {}", sourceId, targetId);
            GitOperation<String> gitop = new GitOperation<String>() {
                public String call(Git git, GitContext context) throws Exception {
                    IllegalStateAssertion.assertNull(checkoutProfileBranch(git, context, targetId, null), "Version already exists: " + targetId);
                    checkoutRequiredProfileBranch(git, context, sourceId, null);
                    createOrCheckoutVersion(git, targetId);
                    if (attributes != null) {
                        setVersionAttributes(git, context, targetId, attributes);
                    }
                    context.commitMessage("Create version: " + sourceId + " => " + targetId);
                    return targetId;
                }
            };
            return executeInternal(context, null, gitop);
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public String createVersion(final Version version) {
        return createVersion(newGitWriteContext(), version);
    }

    @Override
    public String createVersion(GitContext context, final Version version) {
        IllegalStateAssertion.assertNotNull(version, "version");
        LockHandle writeLock = aquireWriteLock();
        try {
            assertValid();
            LOGGER.debug("Create version: {}", version);
            GitOperation<String> gitop = new GitOperation<String>() {
                public String call(Git git, GitContext context) throws Exception {
                    String versionId = version.getId();
                    IllegalStateAssertion.assertNull(checkoutProfileBranch(git, context, versionId, null), "Version already exists: " + versionId);
                    GitHelpers.checkoutTag(git, GitHelpers.ROOT_TAG);
                    createOrCheckoutVersion(git, version.getId());
                    setVersionAttributes(git, context, versionId, version.getAttributes());
                    context.commitMessage("Create version: " + version);
                    for (Profile profile : version.getProfiles()) {
                        createOrUpdateProfile(context, null, profile, new HashSet<String>());
                    }
                    return versionId;
                }
            };
            return executeInternal(context, null, gitop);
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public List<String> getVersionIds() {
        return getVersionIds(newGitReadContext());
    }

    @Override
    public List<String> getVersionIds(GitContext context) {
        LockHandle readLock = aquireReadLock();
        try {
            assertValid();
            GitOperation<List<String>> gitop = new GitOperation<List<String>>() {
                public List<String> call(Git git, GitContext context) throws Exception {
                    List<String> result = new ArrayList<>(versions);
                    // we do not want to expose master branch as a version
                    if (result.contains(GitHelpers.MASTER_BRANCH)) {
                        result.remove(GitHelpers.MASTER_BRANCH);
                    }
                    Collections.sort(result, VersionSequence.getComparator());
                    return Collections.unmodifiableList(result);
                }
            };
            return executeInternal(context, null, gitop);
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public boolean hasVersion(String versionId) {
        return hasVersion(newGitReadContext(), versionId);
    }

    @Override
    public boolean hasVersion(GitContext context, final String versionId) {
        IllegalStateAssertion.assertNotNull(versionId, "versionId");
        LockHandle readLock = aquireReadLock();
        try {
            assertValid();
            GitOperation<Boolean> gitop = new GitOperation<Boolean>() {
                public Boolean call(Git git, GitContext context) throws Exception {
                    return versions.contains(versionId);
                }
            };
            return executeInternal(context, null, gitop);
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public Version getVersion(String versionId) {
        IllegalStateAssertion.assertNotNull(versionId, "versionId");
        return getVersionFromCache(versionId, null);
    }

    @Override
    public Version getRequiredVersion(final String versionId) {
        IllegalStateAssertion.assertNotNull(versionId, "versionId");
        Version version = getVersionFromCache(versionId, null);
        IllegalStateAssertion.assertNotNull(version, "Version does not exist: " + versionId);
        return version;
    }

    @Override
    public void deleteVersion(String versionId) {
        deleteVersion(new GitContext(), versionId);
    }

    @Override
    public void deleteVersion(GitContext context, final String versionId) {
        IllegalStateAssertion.assertNotNull(versionId, "versionId");
        LockHandle writeLock = aquireWriteLock();
        try {
            assertValid();
            LOGGER.debug("Delete version: " + versionId);
            GitOperation<Void> gitop = new GitOperation<Void>() {
                public Void call(Git git, GitContext context) throws Exception {
                    removeVersionFromCaches(versionId);
                    GitHelpers.removeBranch(git, versionId);
                    return null;
                }
            };
            executeInternal(context, null, gitop);
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public String createProfile(Profile profile) {
        return createProfile(newGitWriteContext(), profile);
    }

    @Override
    public String createProfile(GitContext context, final Profile profile) {
        IllegalStateAssertion.assertNotNull(profile, "profile");
        LockHandle writeLock = aquireWriteLock();
        try {
            assertValid();
            GitOperation<String> gitop = new GitOperation<String>() {
                public String call(Git git, GitContext context) throws Exception {
                    String versionId = profile.getVersion();
                    String profileId = profile.getId();
                    Version version = getRequiredVersion(versionId);
                    IllegalStateAssertion.assertFalse(version.hasProfile(profileId), "Profile already exists: " + profileId);
                    checkoutRequiredProfileBranch(git, context, versionId, profileId);
                    return createOrUpdateProfile(context, null, profile, new HashSet<String>());
                }
            };
            return executeInternal(context, null, gitop);
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public String updateProfile(Profile profile) {
        return updateProfile(newGitWriteContext(), profile);
    }

    @Override
    public String updateProfile(GitContext context, final Profile profile) {
        IllegalStateAssertion.assertNotNull(profile, "profile");
        LockHandle writeLock = aquireWriteLock();
        try {
            assertValid();
            
            // Get the existing profile
            final String versionId = profile.getVersion();
            final String profileId = profile.getId();
            final Profile lastProfile = getRequiredProfile(versionId, profileId);
            
            if (!lastProfile.equals(profile)) {
                GitOperation<String> gitop = new GitOperation<String>() {
                    public String call(Git git, GitContext context) throws Exception {
                        checkoutRequiredProfileBranch(git, context, versionId, profileId);
                        return createOrUpdateProfile(context, lastProfile, profile, new HashSet<String>());
                    }
                };
                return executeInternal(context, null, gitop);
            } else {
                LOGGER.debug("Skip unchanged profile update for: {}", profile);
                return lastProfile.getId();
            }
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public boolean hasProfile(final String versionId, final String profileId) {
        IllegalStateAssertion.assertNotNull(versionId, "versionId");
        IllegalStateAssertion.assertNotNull(profileId, "profileId");
        Profile profile = getProfileFromCache(versionId, profileId);
        return profile != null;
    }

    @Override
    public Profile getProfile(final String versionId, final String profileId) {
        IllegalStateAssertion.assertNotNull(versionId, "versionId");
        IllegalStateAssertion.assertNotNull(profileId, "profileId");
        return getProfileFromCache(versionId, profileId);
    }

    @Override
    public Profile getRequiredProfile(final String versionId, final String profileId) {
        IllegalStateAssertion.assertNotNull(versionId, "versionId");
        IllegalStateAssertion.assertNotNull(profileId, "profileId");
        Profile profile = getProfileFromCache(versionId, profileId);
        IllegalStateAssertion.assertNotNull(profile, "Profile does not exist: " + versionId + "/" + profileId);
        return profile;
    }

    @Override
    public List<String> getProfiles(final String versionId) {
        IllegalStateAssertion.assertNotNull(versionId, "versionId");
        assertValid();
        Version version = getVersionFromCache(versionId, null);
        List<String> profiles = version != null ? version.getProfileIds() : Collections.<String>emptyList();
        return Collections.unmodifiableList(profiles);
    }

    @Override
    public void deleteProfile(String versionId, String profileId) {
        deleteProfile(newGitWriteContext(), versionId, profileId);
    }

    @Override
    public void deleteProfile(GitContext context, final String versionId, final String profileId) {
        IllegalStateAssertion.assertNotNull(versionId, "versionId");
        IllegalStateAssertion.assertNotNull(profileId, "profileId");
        LockHandle writeLock = aquireWriteLock();
        try {
            assertValid();
            LOGGER.debug("Delete " + ProfileBuilder.Factory.create(versionId, profileId).getProfile());
            GitOperation<Void> gitop = new GitOperation<Void>() {
                public Void call(Git git, GitContext context) throws Exception {
                    checkoutRequiredProfileBranch(git, context, versionId, profileId);
                    File profileDirectory = GitHelpers.getProfileDirectory(git, profileId);
                    recursiveDeleteAndRemove(git, profileDirectory);
                    context.commitMessage("Removed profile " + profileId);
                    return null;
                }
            };
            executeInternal(context, null, gitop);
        } finally {
            writeLock.unlock();
        }
    }

    private String createOrUpdateProfile(GitContext context, Profile lastProfile, Profile profile, Set<String> profiles) throws IOException, GitAPIException {
        assertWriteLock();

        String versionId = profile.getVersion();
        String profileId = profile.getId();

        if (!profiles.contains(profileId)) {
            
            // Process parents first
            for (String parentId : profile.getParentIds()) {
                Profile parent = getProfileFromCache(profile.getVersion(), parentId);
                IllegalStateAssertion.assertNotNull(parent, "Parent profile does not exist: " + parentId);
            }
            
            if (lastProfile == null) {
                LOGGER.debug("Create {}", Profiles.getProfileInfo(profile));
            } else {
                LOGGER.debug("Update {}", profile);
                LOGGER.debug("Update {}", Profiles.getProfileDifference(lastProfile, profile));
            }
            
            // Create the profile branch & directory
            if (lastProfile == null) {
                createProfileDirectoryAfterCheckout(context, versionId, profileId);
            }

            // FileConfigurations
            Map<String, byte[]> fileConfigurations = profile.getFileConfigurations();
            setFileConfigurations(context, versionId, profileId, fileConfigurations);
            
            // A warning commit message if there has been none yet 
            if (context.getCommitMessage().length() == 0) {
                context.commitMessage("WARNING - Profile with no content: " + versionId + "/" + profileId);
            }
            
            // Mark this profile as processed
            profiles.add(profileId);
        }

        return profileId;
    }
    
    private String createProfileDirectoryAfterCheckout(GitContext context, final String versionId, final String profileId) throws IOException, GitAPIException {
        assertWriteLock();
        File profileDirectory = GitHelpers.getProfileDirectory(getGit(), profileId);
        if (!profileDirectory.exists()) {
            context.commitMessage("Create profile: " + profileId);
            return doCreateProfile(getGit(), context, versionId, profileId);
        }
        return null;
    }
    
    private void setFileConfigurations(GitContext context, final String versionId, final String profileId, final Map<String, byte[]> fileConfigurations) throws IOException, GitAPIException {
        assertWriteLock();

        // Delete and remove stale file configurations
        File profileDir = GitHelpers.getProfileDirectory(getGit(), profileId);
        if (profileDir.exists()) {
            File[] files = profileDir.listFiles(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    return !Constants.AGENT_PROPERTIES.equals(name);
                }
            });
            for (File file : files) {
                recursiveDeleteAndRemove(getGit(), file);
            }
        }

        if (!fileConfigurations.isEmpty()) {
            setFileConfigurations(getGit(), profileId, fileConfigurations);
            context.commitMessage("Update configurations for profile: " + profileId);
        }
    }
    
    private void recursiveDeleteAndRemove(Git git, File file) throws IOException, GitAPIException {
        File rootDir = GitHelpers.getRootGitDirectory(git);
        String relativePath = getFilePattern(rootDir, file);
        if (file.exists() && !relativePath.equals(".git")) {
            if (file.isDirectory()) {
                File[] files = file.listFiles();
                if (files != null) {
                    for (File child : files) {
                        recursiveDeleteAndRemove(git, child);
                    }
                }
            }
            file.delete();
            git.rm().addFilepattern(relativePath).call();
        }
    }
    
    private void setFileConfigurations(Git git, String profileId, Map<String, byte[]> fileConfigurations) throws IOException, GitAPIException {
        for (Map.Entry<String, byte[]> entry : fileConfigurations.entrySet()) {
            String file = entry.getKey();
            byte[] newCfg = entry.getValue();
            setFileConfiguration(git, profileId, file, newCfg);
        }
    }

    private void setFileConfiguration(Git git, String profileId, String fileName, byte[] configuration) throws IOException, GitAPIException {
        File profileDirectory = GitHelpers.getProfileDirectory(git, profileId);
        File file = new File(profileDirectory, fileName);
        Files.writeToFile(file, configuration);
        addFiles(git, file);
    }

    @Override
    public void importProfiles(final String versionId, final List<String> profileZipUrls) {
        IllegalStateAssertion.assertNotNull(versionId, "versionId");
        IllegalStateAssertion.assertNotNull(profileZipUrls, "profileZipUrls");
        LockHandle writeLock = aquireWriteLock();
        try {
            assertValid();
            GitOperation<String> gitop = new GitOperation<String>() {
                public String call(Git git, GitContext context) throws Exception {
                    // TODO(tdi): Is it correct to implicitly create the version?
                    createOrCheckoutVersion(git, versionId);
                    //checkoutRequiredProfileBranch(git, versionId, null);
                    return doImportProfiles(git, context, profileZipUrls);
                }
            };
            executeWrite(gitop);
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public void importFromFileSystem(String importPath) {
        IllegalArgumentAssertion.assertNotNull(importPath, "importPath");
        Path importBase = Paths.get(importPath);
        importExportHandler.importFromFileSystem(importBase);
        importExportHandler.importZipAndArtifacts(importBase.getParent());
    }

    @Override
    public void exportProfiles(String versionId, String outputName, String wildcard) {
        IllegalArgumentAssertion.assertNotNull(versionId, "versionId");
        IllegalArgumentAssertion.assertNotNull(outputName, "outputName");
        importExportHandler.exportProfiles(versionId, outputName, wildcard);
    }

    @Override
    public Iterable<PushResult> doPush(Git git, GitContext context) throws Exception {
        IllegalArgumentAssertion.assertNotNull(git, "git");
        IllegalArgumentAssertion.assertNotNull(context, "context");
        LockHandle writeLock = aquireWriteLock();
        try {
            assertValid();
            LOGGER.debug("External call to push");
            PushPolicyResult pushResult = doPushInternal(context, getCredentialsProvider());
            return pushResult.getPushResults();
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public <T> T gitOperation(GitContext context, GitOperation<T> gitop, PersonIdent personIdent) {
        IllegalArgumentAssertion.assertNotNull(gitop, "gitop");
        IllegalArgumentAssertion.assertNotNull(context, "context");
        LockHandle writeLock = aquireWriteLock();
        try {
            assertValid();
            LOGGER.debug("External call to execute a git operation: " + gitop);
            return executeInternal(context, personIdent, gitop);
        } finally {
            writeLock.unlock();
        }
    }

    private <T> T executeRead(GitOperation<T> operation) {
        return executeInternal(newGitReadContext(), null, operation);
    }

    private <T> T executeWrite(GitOperation<T> operation) {
        return executeInternal(newGitWriteContext(), null, operation);
    }

    private GitContext newGitReadContext() {
        return new GitContext();
    }

    private GitContext newGitWriteContext() {
        return new GitContext().requireCommit().requirePush();
    }

    private <T> T executeInternal(GitContext context, PersonIdent personIdent, GitOperation<T> operation) {
        
        if (context.isRequirePull() || context.isRequireCommit()) {
            assertWriteLock();
        } else {
            assertReadLock();
        }
        
        // [FABRIC-887] Must set the TCCL to the classloader that loaded GitDataStore as we need the classloader
        // that could load this class, as jgit will load resources from classpath using the TCCL
        // and that requires the TCCL to the classloader that could load GitDataStore as the resources
        // jgit requires are in the same bundle as GitDataSource (eg embedded inside fabric-git)
        ClassLoader tccl = Thread.currentThread().getContextClassLoader();
        try {
            ClassLoader gitcl = GitDataStoreImpl.class.getClassLoader();
            Thread.currentThread().setContextClassLoader(gitcl);
            LOGGER.trace("Setting ThreadContextClassLoader to {} instead of {}", gitcl, tccl);
            
            Git git = getGit();
            Repository repository = git.getRepository();

            if (personIdent == null) {
                personIdent = new PersonIdent(repository);
            }

            if (context.isRequirePull()) {
                doPullInternal(context, getCredentialsProvider(), false);
            }

            T result = operation.call(git, context);

            if (context.isRequireCommit()) {
                doCommit(git, context);
                versionCache.invalidateAll();
                notificationRequired = true;
            }

            if (context.isRequirePush()) {
                PushPolicyResult pushResult = doPushInternal(context, getCredentialsProvider());
                if (!pushResult.getRejectedUpdates().isEmpty()) {
                    Exception gitex = pushResult.getLastException();
                    throw new IllegalStateException("Push rejected: " + pushResult.getRejectedUpdates(), gitex);
                }
            }
            
            return result;
        } catch (Exception e) {
            throw FabricException.launderThrowable(e);
        } finally {
            LOGGER.trace("Restoring ThreadContextClassLoader to {}", tccl);
            Thread.currentThread().setContextClassLoader(tccl);
        }
    }

    /**
     * Creates the given profile directory in the currently checked out version branch
     */
    private String doCreateProfile(Git git, GitContext context, String versionId, String profileId) throws IOException, GitAPIException {
        File profileDirectory = GitHelpers.getProfileDirectory(git, profileId);
        File metadataFile = new File(profileDirectory, Constants.AGENT_PROPERTIES);
        IllegalStateAssertion.assertFalse(metadataFile.exists(), "Profile metadata file already exists: " + metadataFile);
        profileDirectory.mkdirs();
        Files.writeToFile(metadataFile, "#Profile:" + profileId + "\n", Charset.defaultCharset());
        addFiles(git, profileDirectory, metadataFile);
        context.commitMessage("Added profile " + profileId);
        return profileId;
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
        } catch (GitAPIException ex) {
            throw FabricException.launderThrowable(ex);
        }
    }

    private void doPullInternal() {
        LockHandle writeLock = aquireWriteLock();
        try {
           doPullInternal(new GitContext(), getCredentialsProvider(), true);
        } catch (Throwable e) {
            LOGGER.debug("Error during pull due " + e.getMessage(), e);
            LOGGER.warn("Error during pull due " + e.getMessage() + ". This exception is ignored.");
        } finally {
            writeLock.unlock();
        }
    }
    
    private PullPolicyResult doPullInternal(GitContext context, CredentialsProvider credentialsProvider, boolean allowVersionDelete) {
        PullPolicyResult pullResult = pullPushPolicy.doPull(context, getCredentialsProvider(), allowVersionDelete);
        if (pullResult.getLastException() == null) {
            if (pullResult.localUpdateRequired()) {
                versionCache.invalidateAll();
                notificationRequired = true;
            }
            Set<String> pullVersions = pullResult.getVersions();
            if (!pullVersions.isEmpty() && !pullVersions.equals(versions)) {
                versions.clear();
                versions.addAll(pullVersions);
                versionCache.invalidateAll();
                notificationRequired = true;
            }
            if (pullResult.remoteUpdateRequired()) {
                doPushInternal(context, credentialsProvider);
            }
        }
        return pullResult;
    }

    private PushPolicyResult doPushInternal(GitContext context, CredentialsProvider credentialsProvider) {
        return pullPushPolicy.doPush(context, credentialsProvider);
    }

    /**
     * Imports one or more profile zips into the given version
     */
    private String doImportProfiles(Git git, GitContext context, List<String> profileZipUrls) throws GitAPIException, IOException {
        File profilesDirectory = GitHelpers.getProfilesDirectory(git);
        for (String url : profileZipUrls) {
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
        addFiles(git, profilesDirectory);
        context.commitMessage("Added profile zip(s) " + profileZipUrls);
        return null;
    }
    
    private void addFiles(Git git, File... files) throws GitAPIException, IOException {
        File rootDir = GitHelpers.getRootGitDirectory(git);
        for (File file : files) {
            String relativePath = getFilePattern(rootDir, file);
            git.add().addFilepattern(relativePath).call();
        }
    }

    private String getFilePattern(File rootDir, File file) throws IOException {
        String relativePath = Files.getRelativePath(rootDir, file);
        if (relativePath.startsWith(File.separator)) {
            relativePath = relativePath.substring(1);
        }
        return relativePath.replace(File.separatorChar, '/');
    }
    
    private Map<String, String> getVersionAttributes(Git git, GitContext context, String versionId) throws IOException {
        File rootDirectory = GitHelpers.getRootGitDirectory(git);
        File file = new File(rootDirectory, GitHelpers.VERSION_ATTRIBUTES);
        if (!file.exists()) {
            return Collections.emptyMap();
        }
        return DataStoreUtils.toMap(Files.readBytes(file));
    }

    private void setVersionAttributes(Git git, GitContext context, String versionId, Map<String, String> attributes) throws IOException, GitAPIException {
        File rootDirectory = GitHelpers.getRootGitDirectory(git);
        File file = new File(rootDirectory, GitHelpers.VERSION_ATTRIBUTES);
        Files.writeToFile(file, DataStoreUtils.toBytes(attributes));
        addFiles(git, file);
    }

    private void assertReadLock() {
        boolean locked = readWriteLock.getReadHoldCount() > 0 || readWriteLock.isWriteLockedByCurrentThread();
        IllegalStateAssertion.assertTrue(!strictLockAssert || locked, "No read lock obtained");
        if (!locked) { 
            LOGGER.warn("No read lock obtained");
        }
    }

    private void assertWriteLock() {
        boolean locked = readWriteLock.isWriteLockedByCurrentThread();
        IllegalStateAssertion.assertTrue(!strictLockAssert || locked, "No write lock obtained");
        if (!locked) {
            LOGGER.warn("No write lock obtained");
        }
    }

    private void createOrCheckoutVersion(Git git, String versionId) throws GitAPIException {
        assertWriteLock();
        GitHelpers.createOrCheckoutBranch(git, versionId, GitHelpers.REMOTE_ORIGIN);
        cacheVersionId(versionId);
    }

    private String checkoutProfileBranch(Git git, GitContext context, String versionId, String profileId) throws GitAPIException {
        String branch = GitHelpers.getProfileBranch(versionId, profileId);
        return GitHelpers.checkoutBranch(git, branch) ? branch : null;
    }
    
    private void checkoutRequiredProfileBranch(Git git, GitContext context, String versionId, String profileId) throws GitAPIException {
        String branch = checkoutProfileBranch(git, context, versionId, profileId);
        IllegalStateAssertion.assertNotNull(branch, "Cannot checkout profile branch: " + versionId + "/" + profileId);
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
            username = ZooKeeperUtils.getContainerLogin(sysprops);
            password = ZooKeeperUtils.generateContainerToken(sysprops, curator.get());
        }
        return new UsernamePasswordCredentialsProvider(username, password);
    }

    private boolean isExternalGitConfigured(Map<String, String> properties) {
        return properties != null && properties.containsKey(GIT_REMOTE_USER) && properties.containsKey(GIT_REMOTE_PASSWORD);
    }

    private String getExternalUser(Map<String, String> properties) {
        return properties.get(GIT_REMOTE_USER);
    }

    private String getExternalCredential(Map<String, String> properties) {
        return properties.get(GIT_REMOTE_PASSWORD);
    }

    private void cacheVersionId(String versionId) {
        if (!GitHelpers.MASTER_BRANCH.equals(versionId)) {
            versions.add(versionId);
        }
    }

    private void removeVersionFromCaches(String versionId) {
        versionCache.invalidate(versionId);
        versions.remove(versionId);
    }
    
    @VisibleForExternal public void bindConfigurer(Configurer service) {
        this.configurer = service;
    }
    void unbindConfigurer(Configurer service) {
        this.configurer = null;
    }

    @VisibleForExternal public void bindCurator(CuratorFramework service) {
        this.curator.bind(service);
    }
    void unbindCurator(CuratorFramework service) {
        this.curator.unbind(service);
    }

    @VisibleForExternal public void bindDataStore(DataStore service) {
        this.dataStore.bind(service);
    }
    void unbindDataStore(DataStore service) {
        this.dataStore.unbind(service);
    }

    @VisibleForExternal public void bindGitProxyService(GitProxyService service) {
        this.gitProxyService.bind(service);
    }
    void unbindGitProxyService(GitProxyService service) {
        this.gitProxyService.unbind(service);
    }

    @VisibleForExternal public void bindGitService(GitService service) {
        this.gitService.bind(service);
    }
    void unbindGitService(GitService service) {
        this.gitService.unbind(service);
    }

    @VisibleForExternal public void bindProfileBuilders(ProfileBuilders service) {
        this.profileBuilders.bind(service);
    }
    void unbindProfileBuilders(ProfileBuilders service) {
        this.profileBuilders.unbind(service);
    }

    @VisibleForExternal public void bindRuntimeProperties(RuntimeProperties service) {
        this.runtimeProperties.bind(service);
    }
    void unbindRuntimeProperties(RuntimeProperties service) {
        this.runtimeProperties.unbind(service);
    }

    class GitDataStoreListener implements GitListener {

        @Override
        public void onRemoteUrlChanged(final String updatedUrl) {
            final String actualUrl = gitRemoteUrl != null ? gitRemoteUrl : updatedUrl;
            threadPool.submit(new Runnable() {
                @Override
                public void run() {
                    runRemoteUrlChanged(actualUrl);
                }

                @Override
                public String toString() {
                    return "RemoteUrlChangedTask";
                }
            });
        }

        @Override
        public void onReceivePack() {
            assertValid();
            versionCache.invalidateAll();
        }
        
        private void runRemoteUrlChanged(final String updateUrl) {
            IllegalArgumentAssertion.assertNotNull(updateUrl, "updateUrl");
            LockHandle writeLock = aquireWriteLock();
            try {
                // TODO(tdi): this is check=then-act, use permit
                if (!isValid()) {
                    LOGGER.warn("Remote url change on invalid component: " + updateUrl);
                    return;
                }
                GitOperation<Void> gitop = new GitOperation<Void>() {
                    @Override
                    public Void call(Git git, GitContext context) throws Exception {
                        Repository repository = git.getRepository();
                        StoredConfig config = repository.getConfig();
                        String currentUrl = config.getString("remote", GitHelpers.REMOTE_ORIGIN, "url");
                        if (!updateUrl.equals(currentUrl)) {
                            
                            LOGGER.info("Remote url change from: {} to: {}", currentUrl, updateUrl);
                            
                            remoteUrl = updateUrl;
                            config.setString("remote", GitHelpers.REMOTE_ORIGIN, "url", updateUrl);
                            config.setString("remote", GitHelpers.REMOTE_ORIGIN, "fetch", "+refs/heads/*:refs/remotes/origin/*");
                            config.save();

                            doPullInternal(context, getCredentialsProvider(), false);
                        }
                        return null;
                    }
                };
                executeInternal(new GitContext(), null, gitop);
            } finally {
                writeLock.unlock();
            }
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
    
    class ImportExportHandler {

        void importFromFileSystem(final Path importPath) {
            LockHandle writeLock = aquireWriteLock();
            try {
                assertValid();

                File sourceDir = importPath.toFile();
                IllegalArgumentAssertion.assertTrue(sourceDir.isDirectory(), "Not a valid source dir: " + sourceDir);

                // lets try and detect the old ZooKeeper style file layout and transform it into the git layout
                // so we may /fabric/configs/versions/1.0/profiles => /fabric/profiles in branch 1.0
                File fabricDir = new File(sourceDir, "fabric");
                File configs = new File(fabricDir, "configs");
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
                                            importFromFileSystem(versionFile, GitHelpers.CONFIGS, version, true);
                                        }
                                    }
                                }
                            }
                        }
                    }
                    File metrics = new File(fabricDir, "metrics");
                    if (metrics.exists()) {
                        LOGGER.info("Importing metrics from " + metrics + " to branch " + defaultVersion);
                        importFromFileSystem(metrics, GitHelpers.CONFIGS, defaultVersion, false);
                    }
                } else {
                    // default to version 1.0
                    String version = "1.0";
                    LOGGER.info("Importing " + fabricDir + " as version " + version);
                    importFromFileSystem(fabricDir, "", version, false);
                }
            } finally {
                writeLock.unlock();
            }
        }

        void exportProfiles(final String versionId, final String outputFileName, String wildcard) {
            LockHandle readLock = aquireReadLock();
            try {
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
                        checkoutRequiredProfileBranch(git, context, versionId, null);
                        return exportProfiles(git, context, outputFile, filter);
                    }
                };
                executeRead(gitop);
            } finally {
                readLock.unlock();
            }
        }

        /**
         * exports one or more profile folders from the given version into the zip
         */
        private String exportProfiles(Git git, GitContext context, File outputFile, FileFilter filter) throws IOException {
            File profilesDirectory = GitHelpers.getProfilesDirectory(git);
            Zips.createZipFile(LOGGER, profilesDirectory, outputFile, filter);
            return null;
        }
        
        private void importFromFileSystem(final File fabricDir, final String destinationPath, final String versionId, final boolean isProfileDir) {
            assertWriteLock();
            GitOperation<Void> gitop = new GitOperation<Void>() {
                public Void call(Git git, GitContext context) throws Exception {
                    GitHelpers.checkoutTag(git, GitHelpers.ROOT_TAG);
                    createOrCheckoutVersion(git, versionId);
                    // now lets recursively add files
                    File toDir = GitHelpers.getRootGitDirectory(git);
                    if (Strings.isNotBlank(destinationPath)) {
                        toDir = new File(toDir, destinationPath);
                    }
                    if (isProfileDir) {
                        recursiveAddLegacyProfileDirectoryFiles(git, fabricDir, toDir, destinationPath);
                    } else {
                        recursiveCopyAndAdd(git, fabricDir, toDir, destinationPath, false);
                    }
                    context.commitMessage("Imported from " + fabricDir);
                    return null;
                }
            };
            executeWrite(gitop);
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
        
        /**
         * Recursively copies the given files from the given directory to the specified directory
         * adding them to the git repo along the way
         */
        private void recursiveCopyAndAdd(Git git, File from, File toDir, String path, boolean useToDirAsDestination) throws GitAPIException, IOException {
            String name = from.getName();
            String pattern = path + (path.length() > 0 && !path.endsWith(File.separator) ? File.separator : "") + name;
            File toFile = new File(toDir, name);

            if (from.isDirectory()) {
                if (acceptDirectory(from)) {
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
                    LOGGER.debug("Skip importing from directory: {}", from);
                }
            } else {
                Files.copy(from, toFile);
            }
            git.add().addFilepattern(fixFilePattern(pattern)).call();
        }

        private boolean acceptDirectory(File dir) {
            // we should skip directories which has a .skipimport file
            String[] files = dir.list();
            for (String name : files) {
                if (".skipimport".equals(name)) {
                    return false;
                }
            }

            return true;
        }

        @SuppressWarnings("unchecked")
        void importZipAndArtifacts(Path fromPath) {
            LOGGER.info("Importing additional profiles from file system directory: {}", fromPath);

            List<String> profiles = new ArrayList<String>();

            // find any zip files
            String[] zips = fromPath.toFile().list(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    return name.endsWith(".zip");
                }
            });
            int count = zips != null ? zips.length : 0;
            LOGGER.debug("Found {} .zip files to import", count);

            if (zips != null && zips.length > 0) {
                for (String name : zips) {
                    profiles.add("file:" + fromPath + "/" + name);
                    LOGGER.debug("Adding {} .zip file to import", name);
                }
            }

            // look for .properties file which can have list of urls to import
            String[] props = fromPath.toFile().list(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    return name.endsWith(Profile.PROPERTIES_SUFFIX);
                }
            });
            count = props != null ? props.length : 0;
            LOGGER.debug("Found {} .properties files to import", count);
            try {
                if (props != null && props.length > 0) {
                    for (String name : props) {
                        java.util.Properties p = new java.util.Properties();
                        p.load(new FileInputStream(fromPath.resolve(name).toFile()));

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
                LOGGER.warn("Error importing profiles due " + e.getMessage() + ". This exception is ignored.");
            }

            // we cannot use fabricService as it has not been initialized yet, so we can only support
            // dynamic version of one token ${version:fabric} in the urls
            String fabricVersion = dataStore.get().getFabricReleaseVersion();

            // parse the profiles for tokens and environment variables
            List<String> replaced = new ArrayList<>();
            for (String profileZipUrl : profiles) {
                String token = "\\$\\{version:fabric\\}";
                String url = profileZipUrl.replaceFirst(token, fabricVersion);

                // remove placeholder tokens which the EnvPlaceholderResolver do not expect
                url = EnvPlaceholderResolver.removeTokens(url);
                // resolve the url as it may point to a system environment to be used
                url = EnvPlaceholderResolver.resolveExpression(url, null, false);

                // maybe there is more in the same url so we split by comma
                String[] urls = url.split(",");

                // and then add each url to the list of replaced profile urls
                for (String s : urls) {
                    s = s.trim();
                    // skip profiles which is marked as off/false etc
                    // for example people can turn off quickstarts by setting environment variable FABRIC8_IMPORT_PROFILE_URLS=false
                    if ("false".equals(s) || "off".equals(s)) {
                        continue;
                    }
                    replaced.add(s);
                }
            }

            if (!replaced.isEmpty()) {
                LOGGER.info("Importing additional profiles from {} url locations ...", replaced.size());
                importProfiles(dataStore.get().getDefaultVersion(), replaced);
                for (String url : replaced) {
                    LOGGER.info("Importing additional profile: {}", url);
                }
                LOGGER.info("Importing additional profiles done");
            }
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
        
        private String fixFilePattern(String pattern) {
            return pattern.replace(File.separatorChar, '/');
        }
    }
    
    class VersionCacheLoader extends CacheLoader<String, Version> {
        
        @Override
        public Version load(final String versionId) {
            assertWriteLock();
            GitOperation<Version> gitop = new GitOperation<Version>() {
                public Version call(Git git, GitContext context) throws Exception {
                    String revision = git.getRepository().getRefDatabase().getRef(versionId).getObjectId().getName();
                    return loadVersion(git, context, versionId, revision);
                }
            };
            GitContext context = new GitContext();
            return executeInternal(context, null, gitop);
        }
        
        private Version loadVersion(Git git, GitContext context, String versionId, String revision) throws Exception {
            VersionBuilder vbuilder = VersionBuilder.Factory.create(versionId).setRevision(revision);
            vbuilder.setAttributes(getVersionAttributes(git, context, versionId));
            populateVersionBuilder(git, context, vbuilder, "master", versionId);
            populateVersionBuilder(git, context, vbuilder, versionId, versionId);
            return vbuilder.getVersion();
        }

        private void populateVersionBuilder(Git git, GitContext context, VersionBuilder builder, String branch, String versionId) throws GitAPIException, IOException {
            checkoutRequiredProfileBranch(git, context, branch, null);
            File profilesDir = GitHelpers.getProfilesDirectory(git);
            if (profilesDir.exists()) {
                String[] files = profilesDir.list();
                if (files != null) {
                    for (String childName : files) {
                        Path childPath = profilesDir.toPath().resolve(childName);
                        if (childPath.toFile().isDirectory()) {
                            RevCommit lastCommit = GitHelpers.getProfileLastCommit(git, branch, childName);
                            if (lastCommit != null) {
                                populateProfile(git, builder, branch, versionId, childPath.toFile(), "");
                            }
                        }
                    }
                }
            }
        }

        private void populateProfile(Git git, VersionBuilder versionBuilder, String branch, String versionId, File profileFile, String prefix) throws IOException {
            String profileName = profileFile.getName();
            String profileId = profileName;
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

            RevCommit lastCommit = GitHelpers.getProfileLastCommit(git, branch, profileName);
            String lastModified = lastCommit != null ? lastCommit.getId().abbreviate(GIT_COMMIT_SHORT_LENGTH).name() : "";
            Map<String, byte[]> fileConfigurations = doGetFileConfigurations(git, profileId);
            
            ProfileBuilder profileBuilder = ProfileBuilder.Factory.create(versionId, profileId);
            profileBuilder.setFileConfigurations(fileConfigurations).setLastModified(lastModified);
            versionBuilder.addProfile(profileBuilder.getProfile());
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
                        configurations.put(relativePath, loadFileConfiguration(file));
                    } else if (file.isDirectory()) {
                        populateFileConfigurations(configurations, profileDirectory, file);
                    }
                }
            }
        }

        private byte[] loadFileConfiguration(File file) throws IOException {
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
    }
}
