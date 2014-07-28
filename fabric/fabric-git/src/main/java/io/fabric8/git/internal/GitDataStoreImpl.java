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
import io.fabric8.api.ProfileRegistry;
import io.fabric8.api.Profiles;
import io.fabric8.api.RuntimeProperties;
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
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.StringTokenizer;
import java.util.TreeSet;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

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
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.eclipse.jgit.treewalk.AbstractTreeIterator;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.gitective.core.CommitUtils;
import org.gitective.core.RepositoryUtils;
import org.jboss.gravia.utils.IllegalStateAssertion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

/**
 * A git based implementation of {@link DataStore} which stores the profile configuration
 * versions in a branch per version and directory per profile.
 */
@ThreadSafe
@Component(name = Constants.DATASTORE_TYPE_PID, label = "Fabric8 Caching Git DataStore", policy = ConfigurationPolicy.OPTIONAL, immediate = true, metatype = true)
@Service({ GitDataStore.class, ProfileRegistry.class })
public final class GitDataStoreImpl extends AbstractComponent implements GitDataStore, ProfileRegistry {
    
    private static final transient Logger LOG = LoggerFactory.getLogger(GitDataStoreImpl.class);

    private static final String CONFIG_ROOT_DIR = "fabric";
    private static final String GIT_REMOTE_USER = "gitRemoteUser";
    private static final String GIT_REMOTE_PASSWORD = "gitRemotePassword";
    private static final String CONFIGS = CONFIG_ROOT_DIR;
    private static final String CONFIGS_PROFILES = CONFIGS + File.separator + "profiles";
    private static final String AGENT_METADATA_FILE = "io.fabric8.agent.properties";
    private static final int GIT_COMMIT_SHORT_LENGTH = 7;
    private static final String MASTER_BRANCH = "master";
    private static final String PROPERTIES_SUFFIX = ".properties";
    private static final int MAX_COMMITS_WITHOUT_GC = 40;
    
    @Reference(referenceInterface = CuratorFramework.class)
    private final ValidatingReference<CuratorFramework> curator = new ValidatingReference<>();
    @Reference(referenceInterface = GitService.class)
    private final ValidatingReference<GitService> gitService = new ValidatingReference<>();
    @Reference(referenceInterface = GitProxyService.class)
    private final ValidatingReference<GitProxyService> gitProxyService = new ValidatingReference<>();
    @Reference(referenceInterface = DataStore.class)
    private final ValidatingReference<DataStore> dataStore = new ValidatingReference<>();
    @Reference(referenceInterface = RuntimeProperties.class)
    private final ValidatingReference<RuntimeProperties> runtimeProperties = new ValidatingReference<>();

    @Reference
    private Configurer configurer;
    

    private Map<String, String> dataStoreProperties;

    private final ScheduledExecutorService threadPool = Executors.newSingleThreadScheduledExecutor();

    private final Object gitOperationMonitor = new Object();
    private final Set<String> versions = new CopyOnWriteArraySet<String>();
    private final GitListener gitListener = new GitDataStoreListener();
    private final AtomicReference<String> remoteRef = new AtomicReference<String>("origin");
    private ProxySelector defaultProxySelector;

    private int commitsWithoutGC = MAX_COMMITS_WITHOUT_GC;
    private String remoteUrl;
    private String lastFetchWarning;
    private volatile boolean initialPull;
    private SharedCount counter;

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

   private final LoadingCache<String, VersionData> cachedVersions = CacheBuilder.newBuilder()
            .build(new CacheLoader<String, VersionData>() {
                @Override
                public VersionData load(final String version) throws Exception {
                    return gitOperation(new GitOperation<VersionData>() {
                        public VersionData call(Git git, GitContext context) throws Exception {
                            VersionData data = new VersionData();
                            populateVersionData(git, version, data);
                            populateVersionData(git, "master", data);
                            return data;
                        }
                    }, true); //We always pull when the item is not present in the cache to prevent loading stale data.
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
            LOG.info("Starting up DataStore " + this);

            // Call the bootstrap {@link DataStoreTemplate}
            DataStoreTemplate template = runtimeProperties.get().removeRuntimeAttribute(DataStoreTemplate.class);
            if (template != null) {
                LOG.info("Using template: " + template);
                template.doWith(this, dataStore.get());
            }

            if (gitProxyService.getOptional() != null) {
                // authenticator disabled, until properly tested it does not affect others, as Authenticator is static in the JVM
                // Authenticator.setDefault(new FabricGitLocalHostAuthenticator(gitProxyService.getOptional()));
                defaultProxySelector = ProxySelector.getDefault();
                ProxySelector fabricProxySelector = new FabricGitLocalHostProxySelector(defaultProxySelector, gitProxyService.getOptional());
                ProxySelector.setDefault(fabricProxySelector);
                LOG.info("Setting up FabricProxySelector: {}", fabricProxySelector);
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
            Path homePath = getRuntimeProperties().getHomePath();
            Path dir = homePath.resolve(importDir);
            importFromFilesystem(dir);

            LOG.info("Starting to push to remote git repository every {} millis", gitPushInterval);
            threadPool.scheduleWithFixedDelay(new Runnable() {
                @Override
                public void run() {
                    try {
                        // must do an initial pull to get data
                        if (!initialPull) {
                            LOG.trace("Performing initial pull");
                            pull();
                            initialPull = true;
                            LOG.debug("Performing initial pull done");
                        }

                        if (gitPullOnPush) {
                            LOG.trace("Performing timed pull");
                            pull();
                            LOG.debug("Performed timed pull done");
                        }
                        //a commit that failed to push for any reason, will not get pushed until the next commit.
                        //periodically pushing can address this issue.
                        LOG.trace("Performing timed push");
                        push();
                        LOG.debug("Performed timed push done");
                    } catch (Throwable e) {
                        LOG.debug("Error during performed timed pull/push due " + e.getMessage(), e);
                        // we dont want stacktrace in WARNs
                        LOG.warn("Error during performed timed pull/push due " + e.getMessage() + ". This exception is ignored.");
                    }
                }

                @Override
                public String toString() {
                    return "TimedPushTask";
                }
            }, 1000, gitPushInterval, TimeUnit.MILLISECONDS);
            // do the initial pull at first so just wait 1 sec

            if (!gitPullOnPush) {
                LOG.info("Using ZooKeeper SharedCount to react when master git repo is changed, so we can do a git pull to the local git repo.");
                counter = new SharedCount(curator.get(), ZkPath.GIT_TRIGGER.getPath(), 0);
                counter.addListener(new SharedCountListener() {
                    @Override
                    public void countHasChanged(SharedCountReader sharedCountReader, int value) throws Exception {
                        LOG.debug("Watch counter updated to " + value + ", doing a pull");
                        try {
                            // must sleep a bit as otherwise we are too fast
                            Thread.sleep(1000);
                            pull();
                        } catch (Throwable e) {
                            LOG.debug("Error during pull due " + e.getMessage(), e);
                            // we dont want stacktrace in WARNs
                            LOG.warn("Error during pull due " + e.getMessage() + ". This exception is ignored.");
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
                //Give some time to the running task to complete.
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
            LOG.info("Restoring ProxySelector to original: {}", defaultProxySelector);
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
            LOG.warn("Error closing SharedCount due " + e.getMessage() + ". This exception is ignored.", e);
        }
    }

    @SuppressWarnings("unchecked")
    private void importFromFilesystem(Path path) {
        LOG.info("Importing additional profiles from file system directory: {}", path);

        List<String> profiles = new ArrayList<String>();

        // find any zip files

        String[] zips = path.toFile().list(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith(".zip");
            }
        });
        int count = zips != null ? zips.length : 0;
        LOG.info("Found {} .zip files to import", count);

        if (zips != null && zips.length > 0) {
            for (String name : zips) {
                profiles.add("file:" + path + "/" + name);
                LOG.debug("Adding {} .zip file to import", name);
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
        LOG.info("Found {} .properties files to import", count);
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
                            LOG.debug("Adding {} to import", value);
                        }
                    }
                }
            }
        } catch (Exception e) {
            LOG.debug("Error importing profiles due " + e.getMessage(), e);
            // we dont want stacktrace in WARNs
            LOG.warn("Error importing profiles due " + e.getMessage() + ". This exception is ignored.", e);
        }

        if (!profiles.isEmpty()) {
            LOG.info("Importing additional profiles from {} url locations ...", profiles.size());
            importProfiles(dataStore.get().getDefaultVersion(), profiles);
            LOG.info("Importing additional profiles done");
        }
    }

    /* (non-Javadoc)
     * @see io.fabric8.git.internal.GitDataStore#getRemote()
     */
    @Override
    public String getRemote() {
        return remoteRef.get();
    }

    /* (non-Javadoc)
     * @see io.fabric8.git.internal.GitDataStore#setRemote(java.lang.String)
     */
    @Override
    public void setRemote(String remote) {
        if (remote == null)
            throw new IllegalArgumentException("Remote name cannot be null");
        this.remoteRef.set(remote);
    }

    @Override
    public void importFromFileSystem(final String from) {
        assertValid();

        File sourceDir = new File(from);
        if (!sourceDir.isDirectory())
            throw new IllegalArgumentException("Not a valid source dir: " + sourceDir.getAbsolutePath());

        // lets try and detect the old ZooKeeper style file layout and transform it into the git layout
        // so we may /fabric/configs/versions/1.0/profiles => /fabric/profiles in branch 1.0
        File fabricsDir = new File(sourceDir, "fabric");
        File configs = new File(fabricsDir, "configs");
        String defaultVersion = dataStore.get().getDefaultVersion();
        if (configs.exists()) {
            LOG.info("Importing the old ZooKeeper layout");
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
                                    LOG.info("Importing version configuration " + versionFile + " to branch " + version);
                                    importFromFileSystem(versionFile, CONFIG_ROOT_DIR, version, true);
                                }
                            }
                        }
                    }
                }
            }
            File metrics = new File(fabricsDir, "metrics");
            if (metrics.exists()) {
                LOG.info("Importing metrics from " + metrics + " to branch " + defaultVersion);
                importFromFileSystem(metrics, CONFIG_ROOT_DIR, defaultVersion, false);
            }
        } else {
            // default to version 1.0
            String version = "1.0";
            LOG.info("Importing " + fabricsDir + " as version " + version);
            importFromFileSystem(fabricsDir, "", version, false);
        }
    }


    private void importFromFileSystem(final File from, final String destinationPath, final String version, final boolean isProfileDir) {
        assertValid();
        gitOperation(new GitOperation<Void>() {
            public Void call(Git git, GitContext context) throws Exception {
                createOrCheckoutVersion(git, version);
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
                context.commit("Imported from " + from);
                return null;
            }
        });
    }

    @Override
    public void createVersion(final String version) {
        assertValid();
        // create a branch
        gitOperation(new GitOperation<Void>() {
            public Void call(Git git, GitContext context) throws Exception {
                // TODO lets checkout the previous version first!
                createOrCheckoutVersion(git, version);
                context.requirePush();
                return null;
            }
        });
    }

    @Override
    public void createVersion(final String parentVersionId, final String toVersion) {
        assertValid();
        // create a branch
        gitOperation(new GitOperation<Void>() {
            public Void call(Git git, GitContext context) throws Exception {
                // lets checkout the parent version first
                checkoutVersion(git, parentVersionId);
                createOrCheckoutVersion(git, toVersion);
                context.requirePush();
                return null;
            }
        });
    }

    @Override
    public Map<String, String> getVersionAttributes(String version) {
        return dataStore.get().getVersionAttributes(version);
    }

    @Override
    public void setVersionAttribute(String version, String key, String value) {
        dataStore.get().setVersionAttribute(version, key, value);
    }

    @Override
    public void deleteVersion(final String version) {
        assertValid();
        // remove a branch
        gitOperation(new GitOperation<Void>() {
            public Void call(Git git, GitContext context) throws Exception {
                removeVersion(version);
                GitHelpers.removeBranch(git, version);
                git.push().setTimeout(gitTimeout)
                        .setCredentialsProvider(getCredentialsProvider())
                        .setRefSpecs(new RefSpec().setSource(null).setDestination("refs/heads/" + version))
                        .call();
                return null;
            }
        });
    }

    @Override
    public List<String> getVersions() {
        assertValid();
        List<VersionSequence> sequences = new ArrayList<>();
        for (String versionId : versions) {
            sequences.add(new VersionSequence(versionId));
        }
        Collections.sort(sequences);
        List<String> verlist = new ArrayList<>();
        for (VersionSequence seq : sequences) {
            verlist.add(seq.getName());
        }
        return Collections.unmodifiableList(verlist);
    }

    private List<String> forceGetVersions() {
        return gitOperation(new GitOperation<List<String>>() {
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
        }, false);
    }

    @Override
    public boolean hasVersion(String name) {
        assertValid();
        return getVersions().contains(name);
    }

    private File getProfilesDirectory(Git git) {
        assertValid();
        return new File(GitHelpers.getRootGitDirectory(git), CONFIGS_PROFILES);
    }

    private File getProfileDirectory(Git git, String profile) {
        assertValid();
        File profilesDirectory = getProfilesDirectory(git);
        String path = convertProfileIdToDirectory(profile);
        return new File(profilesDirectory, path);
    }

    @Override
    public String getProfile(final String version, final String profile, final boolean create) {
        assertValid();
        return gitOperation(new GitOperation<String>() {
            public String call(Git git, GitContext context) throws Exception {
                checkoutVersion(git, GitProfiles.getBranch(version, profile));
                File profileDirectory = getProfileDirectory(git, profile);
                if (!profileDirectory.exists()) {
                    if (create) {
                        return doCreateProfile(git, context, profile, version);
                    }
                    return null;
                }
                return profile;
            }
        });
    }

    @Override
    public void importProfiles(final String version, final List<String> profileZipUrls) {
        assertValid();
        gitOperation(new GitOperation<String>() {
            public String call(Git git, GitContext context) throws Exception {
                checkoutVersion(git, version);
                return doImportProfiles(git, context, profileZipUrls);
            }
        });
    }

    @Override
    public void exportProfiles(final String version, final String outputFileName, String wildcard) {
        final File outputFile = new File(outputFileName);
        outputFile.getParentFile().mkdirs();
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
        assertValid();
        gitOperation(new GitOperation<String>() {
            public String call(Git git, GitContext context) throws Exception {
                checkoutVersion(git, version);
                return doExportProfiles(git, context, outputFile, filter);
            }
        });
    }

    @Override
    public void createProfile(final String version, final String profile) {
        assertValid();
        gitOperation(new GitOperation<String>() {
            public String call(Git git, GitContext context) throws Exception {
                checkoutVersion(git, GitProfiles.getBranch(version, profile));
                return doCreateProfile(git, context, profile, version);
            }
        });
    }

    @Override
    public void deleteProfile(final String version, final String profile) {
        assertValid();
        gitOperation(new GitOperation<Void>() {
            public Void call(Git git, GitContext context) throws Exception {
                checkoutVersion(git, GitProfiles.getBranch(version, profile));
                File profileDirectory = getProfileDirectory(git, profile);
                doRecursiveDeleteAndRemove(git, profileDirectory);
                context.commit("Removed profile " + profile);
                return null;
            }
        });
    }

    @Override
    public Collection<String> listFiles(final String version, final Iterable<String> profiles, final String path) {
        assertValid();
        return gitOperation(new GitOperation<Collection<String>>() {
            public Collection<String> call(Git git, GitContext context) throws Exception {
                SortedSet<String> answer = new TreeSet<String>();
                for (String profile : profiles) {
                    checkoutVersion(git, GitProfiles.getBranch(version, profile));
                    File profileDirectory = getProfileDirectory(git, profile);
                    File file = Strings.isNotBlank(path) ? new File(profileDirectory, path) : profileDirectory;
                    if (file.exists()) {
                        String[] values = file.list();
                        if (values != null) {
                            for (String value : values) {
                                answer.add(value);
                            }
                        }
                    }
                }
                return answer;
            }
        }, !hasVersion(version));
    }

    private Map<String, byte[]> doGetFileConfigurations(Git git, String profile) throws IOException {
        assertValid();
        Map<String, byte[]> configurations = new HashMap<String, byte[]>();
        File profileDirectory = getProfileDirectory(git, profile);
        doPutFileConfigurations(configurations, profileDirectory, profileDirectory);
        return configurations;
    }

    private Map<String, Map<String, String>> doGetConfigurations(Git git, String profile) throws IOException {
        assertValid();
        Map<String, Map<String, String>> configurations = new HashMap<String, Map<String, String>>();
        File profileDirectory = getProfileDirectory(git, profile);
        doPutConfigurations(configurations, profileDirectory, profileDirectory);
        return configurations;
    }

    private void doPutFileConfigurations(Map<String, byte[]> configurations, File profileDirectory, File directory) throws IOException {
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isFile()) {
                    String relativePath = getFilePattern(profileDirectory, file);
                    configurations.put(relativePath, doLoadFileConfiguration(file));
                } else if (file.isDirectory()) {
                    doPutFileConfigurations(configurations, profileDirectory, file);
                }
            }
        }
    }

    private void doPutConfigurations(Map<String, Map<String, String>> configurations, File profileDirectory, File directory) throws IOException {
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isFile() && file.getPath().endsWith(PROPERTIES_SUFFIX)) {
                    String relativePath = getFilePattern(profileDirectory, file);
                    configurations.put(DataStoreUtils.stripSuffix(relativePath, PROPERTIES_SUFFIX), doLoadConfiguration(file));
                }
            }
        }
    }

    @Override
    public void setFileConfigurations(final String version, final String profile, final Map<String, byte[]> configurations) {
        assertValid();
        gitOperation(new GitOperation<Void>() {
            public Void call(Git git, GitContext context) throws Exception {
                checkoutVersion(git, GitProfiles.getBranch(version, profile));
                File profileDirectory = getProfileDirectory(git, profile);
                doSetFileConfigurations(git, profileDirectory, profile, configurations);
                context.setPushBranch(version);
                context.commit("Updated configuration for profile " + profile);
                return null;
            }
        });
    }

    private void doSetFileConfigurations(Git git, File profileDirectory, String profile, Map<String, byte[]> configurations) throws IOException, GitAPIException {
        assertValid();
        Map<String, byte[]> oldCfgs = doGetFileConfigurations(git, profile);

        for (Map.Entry<String, byte[]> entry : configurations.entrySet()) {
            String file = entry.getKey();
            oldCfgs.remove(file);
            byte[] newCfg = entry.getValue();
            doSetFileConfiguration(git, profile, file, newCfg);
        }

        for (String pid : oldCfgs.keySet()) {
            doRecursiveDeleteAndRemove(git, new File(profileDirectory, pid));
        }
    }

    private void doSetConfigurations(Git git, File profileDirectory, String profile, Map<String, Map<String, String>> configurations) throws IOException,
            GitAPIException {
        assertValid();
        Map<String, Map<String, String>> oldCfgs = doGetConfigurations(git, profile);

        for (Map.Entry<String, Map<String, String>> entry : configurations.entrySet()) {
            String pid = entry.getKey();
            oldCfgs.remove(pid);
            Map<String, String> newCfg = entry.getValue();
            doSetConfiguration(git, profile, pid, newCfg);
        }

        for (String pid : oldCfgs.keySet()) {
            doRecursiveDeleteAndRemove(git, getPidFile(profileDirectory, pid));
        }
    }

    @Override
    public void setFileConfiguration(final String version, final String profile, final String fileName, final byte[] configuration) {
        assertValid();
        gitOperation(new GitOperation<Void>() {
            public Void call(Git git, GitContext context) throws Exception {
                checkoutVersion(git, GitProfiles.getBranch(version, profile));
                doSetFileConfiguration(git, profile, fileName, configuration);
                context.commit("Updated " + fileName + " for profile " + profile);
                return null;
            }
        });
    }

    private void doSetFileConfiguration(Git git, String profile, String fileName, byte[] configuration) throws IOException, GitAPIException {
        assertValid();
        File profileDirectory = getProfileDirectory(git, profile);
        File file = new File(profileDirectory, fileName);
        if (configuration == null) {
            doRecursiveDeleteAndRemove(git, file);
        } else {
            Files.writeToFile(file, configuration);
            doAddFiles(git, file);
        }
    }

    private void doSetConfiguration(Git git, String profile, String pid, Map<String, String> configuration) throws IOException, GitAPIException {
        assertValid();
        File profileDirectory = getProfileDirectory(git, profile);
        File file = new File(profileDirectory, pid + PROPERTIES_SUFFIX);
        if (configuration == null) {
            doRecursiveDeleteAndRemove(git, file);
        } else {
            Properties props = new Properties(file);
            for (Map.Entry<String, String> entry : configuration.entrySet()) {
                props.setProperty(entry.getKey(), entry.getValue());
            }
            for (String key : new ArrayList<String>(props.keySet())) {
                if (!configuration.containsKey(key)) {
                    props.remove(key);
                }
            }
            props.save();
            doAddFiles(git, file);
        }
    }

    private File getPidFile(File profileDirectory, String pid) {
        assertValid();
        return new File(profileDirectory, pid + PROPERTIES_SUFFIX);
    }

    @Override
    public void setConfigurations(final String version, final String profile, final Map<String, Map<String, String>> configurations) {
        assertValid();
        gitOperation(new GitOperation<Void>() {
            public Void call(Git git, GitContext context) throws Exception {
                checkoutVersion(git, GitProfiles.getBranch(version, profile));
                File profileDirectory = getProfileDirectory(git, profile);
                doSetConfigurations(git, profileDirectory, profile, configurations);
                context.setPushBranch(version);
                context.commit("Updated configuration for profile " + profile);
                return null;
            }
        });
    }

    @Override
    public void setConfiguration(final String version, final String profile, final String pid, final Map<String, String> configuration) {
        assertValid();
        gitOperation(new GitOperation<Void>() {
            public Void call(Git git, GitContext context) throws Exception {
                checkoutVersion(git, GitProfiles.getBranch(version, profile));
                doSetConfiguration(git, profile, pid, configuration);
                context.setPushBranch(version);
                context.commit("Updated configuration for profile " + profile);
                return null;
            }
        });
    }

    private Git getGit() throws IOException {
        return gitService.get().get();
    }

    /**
     * Performs a set of operations on the git repository & avoids concurrency issues
     */
    private <T> T gitOperation(GitOperation<T> operation) {
        assertValid();
        return gitOperation(null, operation, true);
    }

    @Override
    public <T> T gitOperation(GitOperation<T> operation, boolean pullFirst) {
        assertValid();
        return gitOperation(null, operation, pullFirst);
    }

    public <T> T gitOperation(PersonIdent personIdent, GitOperation<T> operation, boolean pullFirst) {
        assertValid();
        return gitOperation(personIdent, operation, pullFirst, new GitContext());
    }

    @Override
    public <T> T gitOperation(PersonIdent personIdent, GitOperation<T> operation, boolean pullFirst, GitContext context) {
        synchronized (gitOperationMonitor) {
            assertValid();

            // must set the TCCL to the classloader that loaded GitDataStore as we need the classloader
            // that could load this class, as jgit will load resources from classpath using the TCCL
            // and that requires the TCCL to the classloader that could load GitDataStore as the resources
            // jgit requires are in the same bundle as GitDataSource (eg embedded inside fabric-git)
            // see FABRIC-887
            ClassLoader oldCl = Thread.currentThread().getContextClassLoader();
            ClassLoader cl = GitDataStoreImpl.class.getClassLoader();
            Thread.currentThread().setContextClassLoader(cl);
            LOG.trace("Setting ThreadContextClassLoader to {} instead of {}", cl, oldCl);
            try {
                Git git = getGit();
                Repository repository = git.getRepository();
                // lets default the identity if none specified
                if (personIdent == null) {
                    personIdent = new PersonIdent(repository);
                }

                if (GitHelpers.hasGitHead(git)) {
                    // lets stash any local changes just in case..
                    git.stashCreate().setPerson(personIdent).setWorkingDirectoryMessage("Stash before a write").call();
                }

                if (pullFirst) {
                    doPull(git, getCredentialsProvider(), false);
                }

                T answer = operation.call(git, context);
                boolean requirePush = context.isRequirePush();
                if (context.isRequireCommit()) {
                    requirePush = true;
                    String message = context.getCommitMessage().toString();
                    if (message.length() == 0) {
                        LOG.warn("No commit message from " + operation + ". Please add one! :)");
                    }
                    git.commit().setMessage(message).call();
                    if (--commitsWithoutGC < 0) {
                        commitsWithoutGC = MAX_COMMITS_WITHOUT_GC;
                        LOG.debug("Performing \"git gc\" after {} commits", MAX_COMMITS_WITHOUT_GC);
                        git.gc().call();
                    }
                }

                if (requirePush) {
                    doPush(git, context, getCredentialsProvider());
                }

                if (context.isRequireCommit()) {
                    clearCaches();
                    dataStore.get().fireChangeNotifications();
                }
                return answer;
            } catch (Exception e) {
                throw FabricException.launderThrowable(e);
            } finally {
                LOG.trace("Restoring ThreadContextClassLoader to {}", oldCl);
                Thread.currentThread().setContextClassLoader(oldCl);
            }
        }
    }

    /**
     * Pushes any changes - assumed to be invoked within a gitOperation method!
     */
    @Override
    public Iterable<PushResult> doPush(Git git, GitContext gitContext) throws Exception {
        assertValid();
        return doPush(git, gitContext, getCredentialsProvider());
    }

    /**
     * Pushes any committed changes to the remote repo
     */
    private Iterable<PushResult> doPush(Git git, GitContext gitContext, CredentialsProvider credentialsProvider) throws Exception {
        assertValid();
        try {
            Repository repository = git.getRepository();
            StoredConfig config = repository.getConfig();
            String url = config.getString("remote", remoteRef.get(), "url");
            if (Strings.isNullOrBlank(url)) {
                LOG.info("No remote repository defined yet for the git repository at " + GitHelpers.getRootGitDirectory(git) + " so not doing a push");
                return Collections.emptyList();
            }

            return git.push().setTimeout(gitTimeout).setCredentialsProvider(credentialsProvider).setPushAll().call();
        } catch (Throwable ex) {
            // log stacktrace at debug level
            LOG.warn("Failed to push from the remote git repo " + GitHelpers.getRootGitDirectory(git) + " due " + ex.getMessage() + ". This exception is ignored.");
            LOG.debug("Failed to push from the remote git repo " + GitHelpers.getRootGitDirectory(git) + ". This exception is ignored.", ex);
            return Collections.emptyList();
        }
    }

    private CredentialsProvider getCredentialsProvider() {
        assertValid();
        Map<String, String> properties = getDataStoreProperties();
        String username;
        String password;
        if (isExternalGitConfigured(properties)) {
            username = getExternalUser(properties);
            password = getExternalCredential(properties);
        } else {
            RuntimeProperties sysprops = getRuntimeProperties();
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
        assertValid();
        try {
            Repository repository = git.getRepository();
            StoredConfig config = repository.getConfig();
            String url = config.getString("remote", remoteRef.get(), "url");
            if (Strings.isNullOrBlank(url)) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("No remote repository defined for the git repository at " + GitHelpers.getRootGitDirectory(git) + " so not doing a pull");
                }
                return;
            }
            /*
            String branch = repository.getBranch();
            String mergeUrl = config.getString("branch", branch, "merge");
            if (Strings.isNullOrBlank(mergeUrl)) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("No merge spec for branch." + branch + ".merge in the git repository at "
                            + GitHelpers.getRootGitDirectory(git) + " so not doing a pull");
                }
                return;
            }
            */
            if (LOG.isDebugEnabled()) {
                LOG.debug("Performing a fetch in git repository " + GitHelpers.getRootGitDirectory(git) + " on remote URL: " + url);
            }

            boolean hasChanged = false;
            try {
                FetchResult result = git.fetch().setTimeout(gitTimeout).setCredentialsProvider(credentialsProvider).setRemote(remoteRef.get()).call();
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Git fetch result: {}", result.getMessages());
                }
                lastFetchWarning = null;
            } catch (Exception ex) {
                String fetchWarning = ex.getMessage();
                if (!fetchWarning.equals(lastFetchWarning)) {
                    LOG.warn("Fetch failed because of: " + fetchWarning);
                    LOG.debug("Fetch failed - the error will be ignored", ex);
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
                //Check if any remote branches was found as a guard for unwanted deletions.
                if (remoteBranches.isEmpty()) {
                    //Do nothing
                } else if (!remoteBranches.containsKey(version)) {
                    //We never want to delete the master branch.
                    if (doDeleteBranches && !version.equals(MASTER_BRANCH)) {
                        try {
                            git.branchDelete().setBranchNames(localBranches.get(version).getName()).setForce(true).call();
                        } catch (CannotDeleteCurrentBranchException ex) {
                            git.checkout().setName(MASTER_BRANCH).setForce(true).call();
                            git.branchDelete().setBranchNames(localBranches.get(version).getName()).setForce(true).call();
                        }
                        removeVersion(version);
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
                LOG.debug("Changed after pull!");
                if (credentialsProvider != null) {
                    // TODO lets test if the profiles directory is present after checking out version 1.0?
                    getProfilesDirectory(git);
                }
                dataStore.get().fireChangeNotifications();
            }
        } catch (Throwable ex) {
            LOG.debug("Failed to pull from the remote git repo " + GitHelpers.getRootGitDirectory(git), ex);
            LOG.warn("Failed to pull from the remote git repo " + GitHelpers.getRootGitDirectory(git) + " due " + ex.getMessage() + ". This exception is ignored.");
        }
    }

    /**
     * Creates the given profile directory in the currently checked out version branch
     */
    private String doCreateProfile(Git git, GitContext context, String profile, String version) throws IOException, GitAPIException {
        assertValid();
        File profileDirectory = getProfileDirectory(git, profile);
        File metadataFile = new File(profileDirectory, AGENT_METADATA_FILE);
        if (metadataFile.exists()) {
            return null;
        }
        profileDirectory.mkdirs();
        Files.writeToFile(metadataFile, "#Profile:" + profile + "\n", Charset.defaultCharset());
        doAddFiles(git, profileDirectory, metadataFile);
        context.commit("Added profile " + profile);
        return profile;
    }

     /**
     * Imports one or more profile zips into the given version
     */
    private String doImportProfiles(Git git, GitContext context, List<String> profileZipUrls) throws GitAPIException, IOException {
        assertValid();
        // we cannot use fabricService as it has not been initialized yet, so we can only support
        // dynamic version of one token ${version:fabric} in the urls
        String fabricVersion = dataStore.get().getFabricReleaseVersion();

        File profilesDirectory = getProfilesDirectory(git);
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
        context.commit("Added profile zip(s) " + profileZipUrls);
        return null;
    }

    /**
     * exports one or more profile folders from the given version into the zip
     */
    private String doExportProfiles(Git git, GitContext context, File outputFile, FileFilter filter) throws IOException {
        assertValid();
        File profilesDirectory = getProfilesDirectory(git);
        Zips.createZipFile(LOG, profilesDirectory, outputFile, filter);
        return null;
    }

    /**
     * Recursively copies the given files from the given directory to the specified directory
     * adding them to the git repo along the way
     */
    private void recursiveCopyAndAdd(Git git, File from, File toDir, String path, boolean useToDirAsDestination) throws GitAPIException, IOException {
        assertValid();
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
        assertValid();
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
                    String toProfileDirName = convertProfileIdToDirectory(profileId);
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
        assertValid();
        if (profileDir.isDirectory()) {
            String[] list = profileDir.list();
            if (list != null) {
                for (String file : list) {
                    if (file.endsWith(PROPERTIES_SUFFIX) || file.endsWith(".mvel")) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Takes a profile ID of the form "foo-bar" and if we are using directory trees for profiles then
     * converts it to "foo/bar.profile"
     */
    private String convertProfileIdToDirectory(String profileId) {
        assertValid();
        return Profiles.convertProfileIdToPath(profileId);
    }

    private void pull() {
        if (isValid()) {
            try {
                gitOperation(new GitOperation<Object>() {
                    public Object call(Git git, GitContext context) throws Exception {
                        return null;
                    }
                });
            } catch (Exception e) {
                LOG.warn("Failed to perform a pull " + e, e);
            }
        }
    }

    private void push() {
        if (isValid()) {
            try {
                gitOperation(new GitOperation<Object>() {
                    public Object call(Git git, GitContext context) throws Exception {
                        context.requirePush();
                        return null;
                    }
                }, false);
            } catch (Exception e) {
                LOG.warn("Failed to perform a pull " + e, e);
            }
        }
    }

    private void createOrCheckoutVersion(Git git, String version) throws GitAPIException {
        assertValid();
        addVersion(version);
        GitHelpers.createOrCheckoutBranch(git, version, remoteRef.get());
    }

    private void checkoutVersion(Git git, String version) throws GitAPIException {
        assertValid();
        addVersion(version);
        GitHelpers.checkoutBranch(git, version);
    }

    private void doAddFiles(Git git, File... files) throws GitAPIException, IOException {
        assertValid();
        File rootDir = GitHelpers.getRootGitDirectory(git);
        for (File file : files) {
            String relativePath = getFilePattern(rootDir, file);
            git.add().addFilepattern(relativePath).call();
        }
    }

    private void doRecursiveDeleteAndRemove(Git git, File file) throws IOException, GitAPIException {
        assertValid();
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
        assertValid();
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

    private Map<String, String> doLoadConfiguration(File file) throws IOException {
        assertValid();
        Properties props = new Properties();
        props.load(file);
        return props;
    }

    private String fixFilePattern(String pattern) {
        return pattern.replace(File.separatorChar, '/');
    }

    private String getFilePattern(File rootDir, File file) throws IOException {
        assertValid();
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

    private void addVersion(String version) {
        if (!MASTER_BRANCH.equals(version)) {
            versions.add(version);
        }
    }

    private RuntimeProperties getRuntimeProperties() {
        return runtimeProperties.get();
    }

    @Override
    public Map<String, String> getDataStoreProperties() {
        assertValid();
        return dataStoreProperties;
    }

    @Override
    public Map<String, String> getProfileAttributes(String version, String profile) {
        assertValid();
        Map<String, String> attributes = new HashMap<String, String>();
        Map<String, String> config = getConfiguration(version, profile, Constants.AGENT_PID);
        for (Map.Entry<String, String> entry : config.entrySet()) {
            String key = entry.getKey();
            if (key.startsWith(DataStore.ATTRIBUTE_PREFIX)) {
                String attribute = key.substring(DataStore.ATTRIBUTE_PREFIX.length());
                String value = entry.getValue();
                attributes.put(attribute, value);
            }
        }
        return attributes;
    }

    @Override
    public void setProfileAttribute(final String version, final String profile, final String key, final String value) {
        assertValid();
        Map<String, String> config = getConfiguration(version, profile, Constants.AGENT_PID);
        if (value != null) {
            config.put(DataStore.ATTRIBUTE_PREFIX + key, value);
        } else {
            config.remove(key);
        }
        setConfiguration(version, profile, Constants.AGENT_PID, config);
    }

    private VersionData getVersionData(String version) {
        assertValid();
        VersionData data = null;
        try {
            data = cachedVersions.get(version);
        } catch (ExecutionException e) {
            throw FabricException.launderThrowable(e);
        }
        return data;
    }

    private void populateVersionData(Git git, String branch, VersionData data) throws Exception {
        assertValid();
        checkoutVersion(git, branch);
        File profilesDir = getProfilesDirectory(git);
        if (profilesDir.exists()) {
            File[] files = profilesDir.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        addProfileData(git, branch,  data, file, "");
                    }
                }
            }
        }
    }

    private void addProfileData(Git git, String version, VersionData data, File file, String prefix) throws IOException {
        // TODO we could recursively scan for magic ".profile" files or something
        // then we could put profiles into nicer tree structure?
        String profile = file.getName();
        if (Profiles.useDirectoriesForProfiles) {
            if (profile.endsWith(Profiles.PROFILE_FOLDER_SUFFIX)) {
                profile = prefix + profile.substring(0, profile.length() - Profiles.PROFILE_FOLDER_SUFFIX.length());
            } else {
                // lets recurse all children
                File[] files = file.listFiles();
                if (files != null) {
                    for (File child : files) {
                        if (child.isDirectory()) {
                            addProfileData(git, version, data, child, prefix + file.getName() + "-");
                        }
                    }
                }
                return;
            }
        }

        Ref versionRef = git.getRepository().getRefDatabase().getRef(version);
        IllegalStateAssertion.assertNotNull(versionRef, "Cannot get version ref for: " + version);
        
        String revision = versionRef.getObjectId().getName();
        String path = convertProfileIdToDirectory(profile);
        RevCommit commit = CommitUtils.getLastCommit(git.getRepository(), revision, CONFIGS_PROFILES + File.separator + path);
        String lastModified = commit != null ? commit.getId().abbreviate(GIT_COMMIT_SHORT_LENGTH).name() : "";

        Map<String, byte[]> configurations = doGetFileConfigurations(git, profile);
        Map<String, Map<String, String>> substituted = new HashMap<String, Map<String, String>>();
        for (Map.Entry<String, byte[]> entry : configurations.entrySet()) {
            if (entry.getKey().endsWith(".properties")) {
                String pid = DataStoreUtils.stripSuffix(entry.getKey(), ".properties");
                substituted.put(pid, DataStoreUtils.toMap(DataStoreUtils.toProperties(entry.getValue())));
            }
        }
        ProfileData profileData = new ProfileData(lastModified, Collections.unmodifiableMap(configurations), Collections.unmodifiableMap(substituted));
        data.profiles.put(profile, profileData);
    }

    public List<String> getProfiles(String version) {
        assertValid();
        VersionData v = getVersionData(version);
        return v != null && v.profiles != null
                ? new ArrayList<String>(v.profiles.keySet())
                : new ArrayList<String>();
    }

    public boolean hasProfile(String version, String profile) {
        assertValid();
        VersionData v = getVersionData(version);
        ProfileData p = v != null && v.profiles != null ? v.profiles.get(profile) : null;
        return p != null;
    }

    @Override
    public String getLastModified(String version, String profile) {
        assertValid();
        VersionData v = getVersionData(version);
        ProfileData p = v != null && v.profiles != null ? v.profiles.get(profile) : null;
        return p != null ? p.lastModified : "";
    }

    public byte[] getFileConfiguration(final String version, final String profile, final String fileName) {
        assertValid();
        VersionData v = getVersionData(version);
        ProfileData p = v != null && v.profiles != null ? v.profiles.get(profile) : null;
        return p != null && p.files != null ? p.files.get(fileName) : null;
    }

    public Map<String, byte[]> getFileConfigurations(String version, String profile) {
        assertValid();
        VersionData v = getVersionData(version);
        ProfileData p = v != null && v.profiles != null ? v.profiles.get(profile) : null;
        return p != null ? new HashMap<String, byte[]>(p.files) : Collections.<String, byte[]>emptyMap();
    }

    public Map<String, Map<String, String>> getConfigurations(String version, String profile) {
        assertValid();
        VersionData v = getVersionData(version);
        ProfileData p = v != null && v.profiles != null ? v.profiles.get(profile) : null;
        return p != null ? new HashMap<String, Map<String, String>>(p.configs) : Collections.<String, Map<String, String>>emptyMap();
    }

    public Map<String, String> getConfiguration(String version, String profile, String pid) {
        assertValid();
        Map<String, Map<String, String>> configs = getConfigurations(version, profile);
        if (configs.containsKey(pid)) {
            return new HashMap<String, String>(configs.get(pid));
        } else {
            return new HashMap<String, String>();
        }
    }

    private void removeVersion(String version) {
        versions.remove(version);
        cachedVersions.invalidate(version);
    }

    private void clearCaches() {
        assertValid();
        cachedVersions.invalidateAll();
    }

    private static class VersionData {
        final Map<String, ProfileData> profiles = new HashMap<String, ProfileData>();
    }

    private static class ProfileData {
        final String lastModified;
        final Map<String, byte[]> files;
        final Map<String, Map<String, String>> configs;
        ProfileData(String lastModified, Map<String, byte[]> files, Map<String, Map<String, String>> configs) {
            this.lastModified = lastModified;
            this.files = files;
            this.configs = configs;
        }
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
                            gitOperation(new GitOperation<Void>() {
                                @Override
                                public Void call(Git git, GitContext context) throws Exception {
                                    Repository repository = git.getRepository();
                                    StoredConfig config = repository.getConfig();
                                    String currentUrl = config.getString("remote", "origin", "url");
                                    if (actualUrl != null && !actualUrl.equals(currentUrl)) {
                                        LOG.info("Performing on remote url changed from: {} to: {}", currentUrl, actualUrl);
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
                            });
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
            clearCaches();
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
            if (LOG.isTraceEnabled()) {
                LOG.trace("ProxySelector uri: {}", uri);
                LOG.trace("ProxySelector nonProxyHosts {}", proxyService.getNonProxyHosts());
                LOG.trace("ProxySelector proxyHost {}", proxyService.getProxyHost());
            }

            // we should only intercept when its a git/fabric request
            List<Proxy> answer;
            if (path != null && path.startsWith(GIT_FABRIC_PATH)) {
                answer = doSelect(host, proxyService.getNonProxyHosts(), proxyService.getProxyHost(), proxyService.getProxyPort());
            } else {
                // use delegate
                answer = delegate.select(uri);
            }

            LOG.debug("ProxySelector uri: {} -> {}", uri, answer);
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

    void bindRuntimeProperties(RuntimeProperties service) {
        this.runtimeProperties.bind(service);
    }
    void unbindRuntimeProperties(RuntimeProperties service) {
        this.runtimeProperties.unbind(service);
    }
}