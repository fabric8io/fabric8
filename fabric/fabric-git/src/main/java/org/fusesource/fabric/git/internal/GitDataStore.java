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
package org.fusesource.fabric.git.internal;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.curator.framework.CuratorFramework;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.References;
import org.apache.felix.scr.annotations.Service;
import org.eclipse.jgit.api.CreateBranchCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ListBranchCommand;
import org.eclipse.jgit.api.MergeResult;
import org.eclipse.jgit.api.errors.CannotDeleteCurrentBranchException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.merge.MergeStrategy;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.PushResult;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.fusesource.fabric.api.DataStore;
import org.fusesource.fabric.api.DataStorePlugin;
import org.fusesource.fabric.api.FabricException;
import org.fusesource.fabric.api.FabricRequirements;
import org.fusesource.fabric.api.PlaceholderResolver;
import org.fusesource.fabric.git.GitListener;
import org.fusesource.fabric.internal.DataStoreHelpers;
import org.fusesource.fabric.internal.RequirementsJson;
import org.fusesource.fabric.service.DataStoreSupport;
import org.fusesource.fabric.utils.Files;
import org.fusesource.fabric.utils.PropertiesHelper;
import org.fusesource.fabric.utils.Strings;
import org.fusesource.fabric.zookeeper.ZkPath;
import org.gitective.core.CommitUtils;
import org.gitective.core.RepositoryUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.fusesource.fabric.zookeeper.utils.ZooKeeperUtils.exists;
import static org.fusesource.fabric.zookeeper.utils.ZooKeeperUtils.generateContainerToken;
import static org.fusesource.fabric.zookeeper.utils.ZooKeeperUtils.getContainerLogin;
import static org.fusesource.fabric.zookeeper.utils.ZooKeeperUtils.getPropertiesAsMap;
import static org.fusesource.fabric.zookeeper.utils.ZooKeeperUtils.getStringData;
import static org.fusesource.fabric.zookeeper.utils.ZooKeeperUtils.setData;
import static org.fusesource.fabric.zookeeper.utils.ZooKeeperUtils.setPropertiesAsMap;

/**
 * A git based implementation of {@link DataStore} which stores the profile configuration
 * versions in a branch per version and directory per profile.
 */
@Component(name = "org.fusesource.datastore.git",
        description = "Fabric Git DataStore")
@References({
        @Reference(cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE,
                referenceInterface = PlaceholderResolver.class,
                bind = "bindPlaceholderResolver", unbind = "unbindPlaceholderResolver", policy = ReferencePolicy.DYNAMIC),
        @Reference(referenceInterface = CuratorFramework.class, bind = "bindCurator", unbind = "unbindCurator", cardinality = org.apache.felix.scr.annotations.ReferenceCardinality.MANDATORY_UNARY),
        @Reference(referenceInterface = GitService.class, bind = "bindGitService", unbind = "unbindGitService", cardinality = org.apache.felix.scr.annotations.ReferenceCardinality.MANDATORY_UNARY)
}
)
@Service(DataStorePlugin.class)
public class GitDataStore extends DataStoreSupport implements DataStorePlugin<GitDataStore> {
    private static final transient Logger LOG = LoggerFactory.getLogger(GitDataStore.class);

    private static final String PROFILE_ATTRIBUTES_PID = "org.fusesource.fabric.profile.attributes";
    private static final String CONTAINER_CONFIG_PID = "org.fusesource.fabric.agent";

    private static final String MASTER_BRANCH = "master";
    private static final String CONFIG_ROOT_DIR = "fabric";

    public static final String GIT_PULL_PERIOD = "gitPullPeriod";
    public static final String GIT_REMOTE_URL = "gitRemoteUrl";
    public static final String GIT_REMOTE_USER = "gitRemoteUser";
    public static final String GIT_REMOTE_PASSWORD = "gitRemotePassword";
    public static final String[] SUPPORTED_CONFIGURATION = {DATASTORE_TYPE_PROPERTY, GIT_REMOTE_URL, GIT_REMOTE_USER, GIT_REMOTE_PASSWORD, GIT_PULL_PERIOD};


    public static final String CONFIGS = "/" + CONFIG_ROOT_DIR;
    public static final String CONFIGS_PROFILES = CONFIGS + "/profiles";
    public static final String AGENT_METADATA_FILE = "org.fusesource.fabric.agent.properties";
    public static final String TYPE = "git";

    /**
     * Should we convert a directory of profiles called "foo-bar" into a directory "foo/bar.profile" structure to use
     * the file system better, to better organise profiles into folders and make it easier to work with profiles in the wiki
     */
    public static final boolean useDirectoriesForProfiles = true;
    public static final String PROFILE_FOLDER_SUFFIX = ".profile";

    private GitService gitService;

    private final Object lock = new Object();
    private String remote = "origin";

    private GitListener remoteChangeListener = new GitListener() {
        @Override
        public void onRemoteUrlChanged(final String remoteUrl) {
            final String actualUrl = gitRemoteUrl != null ? gitRemoteUrl : remoteUrl;
            getThreadPool().submit(new Runnable() {
                @Override
                public void run() {
                    gitOperation(new GitOperation<Void>() {
                        @Override
                        public Void call(Git git, GitContext context) throws Exception {
                            Repository repository = git.getRepository();
                            StoredConfig config = repository.getConfig();
                            String currentUrl = config.getString("remote", "origin", "url");
                            if (actualUrl == null) {
                                config.unsetSection("remote", "origin");
                                config.save();
                            } else if (!actualUrl.equals(currentUrl)) {
                                config.setString("remote", "origin", "url", actualUrl);
                                config.setString("remote", "origin", "fetch", "+refs/heads/*:refs/remotes/origin/*");
                                config.save();
                            }
                            return null;
                        }
                    });
                    pull();
                }
            });
        }
    };

    private String gitRemoteUrl;
    private long pullPeriod = 1000;

    private ScheduledExecutorService threadPool;

    public String toString() {
        return "GitDataStore(" + getGitService() + ")";
    }

    @Activate
    public void init() {
    }

    @Deactivate
    public void destroy() {
        stop();
    }

    public synchronized void start() {
        try {
            super.start();
            this.threadPool = Executors.newSingleThreadScheduledExecutor();
            Map<String, String> properties = getDataStoreProperties();
            if (properties != null) {
                this.pullPeriod = PropertiesHelper.getLongValue(properties, GIT_PULL_PERIOD, this.pullPeriod);
                this.gitRemoteUrl = properties.get(GIT_REMOTE_URL);
            }

            if (gitRemoteUrl != null) {
                remoteChangeListener.onRemoteUrlChanged(gitRemoteUrl);
            } else if (gitService != null) {
                gitService.addRemoteChangeListener(remoteChangeListener);
                gitRemoteUrl = gitService.getRemoteUrl();
                remoteChangeListener.onRemoteUrlChanged(gitRemoteUrl);
                pull();
            }

            LOG.info("starting to pull from remote repository every " + pullPeriod + " millis");
            getThreadPool().scheduleWithFixedDelay(new Runnable() {
                @Override
                public void run() {
                    LOG.debug("Performing timed pull");
                    pull();
                }
            }, pullPeriod, pullPeriod, TimeUnit.MILLISECONDS);
        } catch (Exception ex) {
            throw new FabricException("Failed to start GitDataStore:", ex);
        }
    }

    public synchronized void stop() {
        try {
            if (gitService != null) {
                gitService.removeRemoteChangeListener(remoteChangeListener);
            }
            if (threadPool != null) {
                threadPool.shutdown();
                try {
                    //Give some time to the running task to complete.
                    threadPool.awaitTermination(5, TimeUnit.SECONDS);
                } catch (Exception ex) {
                    throw new FabricException(ex);
                }
            }
        } finally {
            super.stop();
        }
    }

    public String getRemote() {
        return remote;
    }

    /**
     * Sets the name of the remote repository
     */
    public void setRemote(String remote) {
        this.remote = remote;
    }

    public GitService getGitService() {
        return gitService;
    }

    public void setGitService(GitService gitService) {
        this.gitService = gitService;
    }

    public void bindGitService(GitService gitService) {
        this.gitService = gitService;
    }

    public void unbindGitService(GitService gitService) {
        this.gitService = null;
    }

    private synchronized ScheduledExecutorService getThreadPool() {
        return this.threadPool;
    }

    public void setThreadPool(ScheduledExecutorService threadPool) {
        this.threadPool = threadPool;
    }

    @Override
    public void importFromFileSystem(final String from) {
        // lets try and detect the old ZooKeeper style file layout and transform it into the git layout
        // so we may /fabric/configs/versions/1.0/profiles => /fabric/profiles in branch 1.0
        File file = new File(from);
        File fabricsDir = new File(file, "fabric");
        File configs = new File(fabricsDir, "configs");
        String defaultVersion = getDefaultVersion();
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
                                    LOG.info("Importing version configuration " + versionFile + " to branch "
                                            + version);
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
            LOG.info("Importing " + file + " as version " + defaultVersion);
            importFromFileSystem(file, "", defaultVersion, false);
        }
    }

    public void importFromFileSystem(final File from, final String destinationPath, final String version, final boolean isProfileDir) {
        gitOperation(new GitOperation<Void>() {
            public Void call(Git git, GitContext context) throws Exception {
                createVersion(version);
                checkoutVersion(git, version);
                // now lets recursively add files
                File toDir = GitHelpers.getRootGitDirectory(git);
                if (Strings.isNotBlank(destinationPath)) {
                    toDir = new File(toDir, destinationPath);
                }
                if (isProfileDir && useDirectoriesForProfiles) {
                    recursiveAddLegacyProfileDirectoryFiles(git, from, toDir, destinationPath);
                } else {
                    recursiveCopyAndAdd(git, from, toDir, destinationPath, true);
                }
                context.setPushBranch(version);
                context.commit("Imported from " + from);
                return null;
            }
        });
    }

    protected void todo() {
        throw new UnsupportedOperationException("TODO");
    }


    @Override
    public void createVersion(final String version) {
        // create a branch
        gitOperation(new GitOperation<Void>() {
            public Void call(Git git, GitContext context) throws Exception {
                // TODO lets checkout the previous versionu first!
                checkoutVersion(git, version);
                context.setPushBranch(version);
                context.requirePush();
                return null;
            }
        });
    }

    @Override
    public void createVersion(final String parentVersionId, final String toVersion) {
        // create a branch
        gitOperation(new GitOperation<Void>() {
            public Void call(Git git, GitContext context) throws Exception {
                // lets checkout the parent version first
                checkoutVersion(git, parentVersionId);
                checkoutVersion(git, toVersion);
                context.setPushBranch(toVersion);
                context.requirePush();
                return null;
            }
        });
    }

    @Override
    public void deleteVersion(String version) {
        // TODO
        todo();
    }

    @Override
    public List<String> getVersions() {
        return gitReadOperation(new GitOperation<List<String>>() {
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
                return answer;
            }
        });
    }

    @Override
    public boolean hasVersion(String name) {
        return getVersions().contains(name);
    }

    @Override
    public List<String> getProfiles(final String version) {
        return gitReadOperation(new GitOperation<List<String>>() {
            public List<String> call(Git git, GitContext context) throws Exception {
                List<String> answer = new ArrayList<String>();
                File profilesDir = getProfilesDirectory(git);
                if (hasVersion(version)) {
                    //We are also checking the master branch for non versioned profiles (e.g. ensemble profiles).
                    checkoutVersion(git, "master");
                    doAddProfileNames(answer, profilesDir, "");

                    checkoutVersion(git, version);
                    doAddProfileNames(answer, profilesDir, "");

                }
                return answer;
            }
        });
    }

    protected void doAddProfileNames(List<String> answer, File profilesDir, String prefix) {
        if (profilesDir.exists()) {
            File[] files = profilesDir.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        // TODO we could recursively scan for magic ".profile" files or something
                        // then we could put profiles into nicer tree structure?
                        String name = file.getName();
                        if (useDirectoriesForProfiles) {
                            if (name.endsWith(PROFILE_FOLDER_SUFFIX)) {
                                name = name.substring(0, name.length() - PROFILE_FOLDER_SUFFIX.length());
                                answer.add(prefix + name);
                            } else {
                                doAddProfileNames(answer, file, prefix + name + "-");
                            }
                        } else {
                            answer.add(name);
                        }
                    }
                }
            }
        }
    }

    public File getProfilesDirectory(Git git) {
        return new File(GitHelpers.getRootGitDirectory(git), GitDataStore.CONFIGS_PROFILES);
    }

    public File getProfileDirectory(Git git, String profile) {
        File profilesDirectory = getProfilesDirectory(git);
        String path = convertProfileIdToDirectory(profile);
        return new File(profilesDirectory, path);
    }

    @Override
    public String getProfile(final String version, final String profile, final boolean create) {
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
    public void createProfile(final String version, final String profile) {
        gitOperation(new GitOperation<String>() {
            public String call(Git git, GitContext context) throws Exception {
                checkoutVersion(git, GitProfiles.getBranch(version, profile));
                return doCreateProfile(git, context, profile, version);
            }
        });
    }

    @Override
    public void deleteProfile(final String version, final String profile) {
        gitOperation(new GitOperation<Void>() {
            public Void call(Git git, GitContext context) throws Exception {
                checkoutVersion(git, GitProfiles.getBranch(version, profile));
                File profileDirectory = getProfileDirectory(git, profile);
                doRecursiveDeleteAndRemove(git, profileDirectory);
                context.setPushBranch(version);
                context.commit("Removed profile " + profile);
                return null;
            }
        });
    }


    @Override
    public Map<String, String> getVersionAttributes(String version) {
        // TODO
        try {
            String node = ZkPath.CONFIG_VERSION.getPath(version);
            return getPropertiesAsMap(treeCache, node);
        } catch (Exception e) {
            throw new FabricException(e);
        }
    }

    @Override
    public void setVersionAttribute(String version, String key, String value) {
        // TODO
        try {
            Map<String, String> props = getVersionAttributes(version);
            if (value != null) {
                props.put(key, value);
            } else {
                props.remove(key);
            }
            String node = ZkPath.CONFIG_VERSION.getPath(version);
            setPropertiesAsMap(getCurator(), node, props);
        } catch (Exception e) {
            throw new FabricException(e);
        }
    }


    @Override
    public Map<String, String> getProfileAttributes(String version, String profile) {
        // TODO we should probably remove this hack at some point and just let the
        // ProfileImpl delegate the getParent() mechanism to the DataStore so we don't have to look in 2 files
        Map<String, String> configuration = getConfiguration(version, profile, PROFILE_ATTRIBUTES_PID);
        Map<String, String> containerConfiguration = getConfiguration(version, profile, CONTAINER_CONFIG_PID);
        String parents = containerConfiguration.get("parents");
        if (parents != null && !parents.isEmpty()) {
            configuration.put("parents", parents);
            //configuration.put(p.substring(0, p.lastIndexOf('/')), "parents=" + parents);
        }
        return configuration;
    }

    @Override
    public void setProfileAttribute(final String version, final String profile, final String key,
                                    final String value) {
        Map<String, String> config = getConfiguration(version, profile, PROFILE_ATTRIBUTES_PID);
        if (value != null) {
            config.put(key, value);
        } else {
            config.remove(key);
        }
        setConfiguration(version, profile, PROFILE_ATTRIBUTES_PID, config);
    }

    @Override
    public long getLastModified(final String version, final String profile) {
        Long answer = gitReadOperation(new GitOperation<Long>() {
            public Long call(Git git, GitContext context) throws Exception {
                checkoutVersion(git, GitProfiles.getBranch(version, profile));
                File profileDirectory = getProfileDirectory(git, profile);
                File metadataFile = new File(profileDirectory, AGENT_METADATA_FILE);
                Long answer = null;
                if (profileDirectory.exists()) {
                    answer = profileDirectory.lastModified();
                    if (metadataFile.exists()) {
                        long modified = metadataFile.lastModified();
                        if (modified > answer) {
                            answer = modified;
                        }
                    }
                }
                return answer;
            }
        });
        return answer != null ? answer.longValue() : 0;
    }

    @Override
    public Map<String, byte[]> getFileConfigurations(final String version, final String profile) {
        return gitReadOperation(new GitOperation<Map<String, byte[]>>() {
            public Map<String, byte[]> call(Git git, GitContext context) throws Exception {
                checkoutVersion(git, GitProfiles.getBranch(version, profile));
                return doGetFileConfigurations(git, profile);
            }
        });
    }

    protected Map<String, byte[]> doGetFileConfigurations(Git git, String profile) throws IOException {
        Map<String, byte[]> configurations = new HashMap<String, byte[]>();
        File profileDirectory = getProfileDirectory(git, profile);
        doPutFileConfigurations(configurations, profileDirectory, profileDirectory);
        return configurations;
    }

    private void doPutFileConfigurations(Map<String, byte[]> configurations, File profileDirectory,
                                         File directory)
            throws IOException {
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

    @Override
    public byte[] getFileConfiguration(final String version, final String profile, final String fileName) {
        return gitReadOperation(new GitOperation<byte[]>() {
            public byte[] call(Git git, GitContext context) throws Exception {
                checkoutVersion(git, GitProfiles.getBranch(version, profile));
                File profileDirectory = getProfileDirectory(git, profile);
                File file = new File(profileDirectory, fileName);
                return doLoadFileConfiguration(file);
            }
        });
    }

    @Override
    public void setFileConfigurations(final String version, final String profile,
                                      final Map<String, byte[]> configurations) {
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

    protected void doSetFileConfigurations(Git git, File profileDirectory, String profile,
                                           Map<String, byte[]> configurations)
            throws IOException, GitAPIException {
        Map<String, byte[]> oldCfgs = doGetFileConfigurations(git, profile);

        for (Map.Entry<String, byte[]> entry : configurations.entrySet()) {
            String file = entry.getKey();
            oldCfgs.remove(file);
            byte[] newCfg = entry.getValue();
            doSetFileConfiguration(git, profile, file, newCfg);
        }

        for (String pid : oldCfgs.keySet()) {
            doRecursiveDeleteAndRemove(git, getPidFile(profileDirectory, getPidFromFileName(pid)));
        }
    }

    @Override
    public void setFileConfiguration(final String version, final String profile, final String fileName,
                                     final byte[] configuration) {
        gitOperation(new GitOperation<Void>() {
            public Void call(Git git, GitContext context) throws Exception {
                checkoutVersion(git, GitProfiles.getBranch(version, profile));
                doSetFileConfiguration(git, profile, fileName, configuration);
                context.setPushBranch(version);
                context.commit("Updated " + fileName + " for profile " + profile);
                return null;
            }
        });
    }

    protected void doSetFileConfiguration(Git git, String profile, String fileName, byte[] configuration)
            throws IOException, GitAPIException {
        File profileDirectory = getProfileDirectory(git, profile);
        File file = new File(profileDirectory, fileName);
        if (configuration == null) {
            doRecursiveDeleteAndRemove(git, file);
        } else {
            Files.writeToFile(file, configuration);
            doAddFiles(git, file);
        }
    }

    protected File getPidFile(File profileDirectory, String pid) {
        return new File(profileDirectory, pid + ".properties");
    }

    protected String getPidFromFileName(String relativePath) throws IOException {
        return DataStoreHelpers.stripSuffix(relativePath, ".properties");
    }

    @Override
    public Map<String, String> getConfiguration(final String version, final String profile,
                                                final String pid) {
        return gitReadOperation(new GitOperation<Map<String, String>>() {
            public Map<String, String> call(Git git, GitContext context) throws Exception {
                checkoutVersion(git, GitProfiles.getBranch(version, profile));
                File profileDirectory = getProfileDirectory(git, profile);
                File file = getPidFile(profileDirectory, pid);
                if (file.isFile() && file.exists()) {
                    byte[] data = Files.readBytes(file);
                    return DataStoreHelpers.toMap(DataStoreHelpers.toProperties(data));
                } else {
                    return new HashMap<String, String>();
                }
            }
        });
    }

    @Override
    public void setConfigurations(String version, String profile,
                                  Map<String, Map<String, String>> configurations) {
        Map<String, byte[]> fileConfigs = new HashMap<String, byte[]>();
        try {
            for (Map.Entry<String, Map<String, String>> entry : configurations.entrySet()) {
                String pid = entry.getKey();
                Map<String, String> map = entry.getValue();
                byte[] data = DataStoreHelpers.toBytes(DataStoreHelpers.toProperties(map));
                fileConfigs.put(pid + ".properties", data);
            }
        } catch (IOException e) {
            throw new FabricException(e);
        }
        setFileConfigurations(version, profile, fileConfigs);
    }

    @Override
    public void setConfiguration(String version, String profile, String pid,
                                 Map<String, String> configuration) {
        byte[] data;
        try {
            data = DataStoreHelpers.toBytes(DataStoreHelpers.toProperties(configuration));
        } catch (IOException e) {
            throw new FabricException(e);
        }
        setFileConfiguration(version, profile, pid + ".properties", data);
    }

    @Override
    public String getDefaultJvmOptions() {
        try {
            if (getCurator().getZookeeperClient().isConnected()
                    && exists(getCurator(), JVM_OPTIONS_PATH) != null) {
                return getStringData(treeCache, JVM_OPTIONS_PATH);
            } else {
                return "";
            }
        } catch (Exception e) {
            throw new FabricException(e);
        }
    }

    @Override
    public void setDefaultJvmOptions(String jvmOptions) {
        try {
            String opts = jvmOptions != null ? jvmOptions : "";
            setData(getCurator(), JVM_OPTIONS_PATH, opts);
        } catch (Exception e) {
            throw new FabricException(e);
        }
    }

    @Override
    public FabricRequirements getRequirements() {
        try {
            FabricRequirements answer = null;
            if (treeCache.getCurrentData(REQUIREMENTS_JSON_PATH) != null) {
                String json = getStringData(treeCache, REQUIREMENTS_JSON_PATH);
                answer = RequirementsJson.fromJSON(json);
            }
            if (answer == null) {
                answer = new FabricRequirements();
            }
            return answer;
        } catch (Exception e) {
            throw new FabricException(e);
        }
    }

    @Override
    public void setRequirements(FabricRequirements requirements) throws IOException {
        try {
            requirements.removeEmptyRequirements();
            String json = RequirementsJson.toJSON(requirements);
            setData(getCurator(), REQUIREMENTS_JSON_PATH, json);
        } catch (Exception e) {
            throw new FabricException(e);
        }
    }

    @Override
    public String getClusterId() {
        try {
            return getStringData(getCurator(), ZkPath.CONFIG_ENSEMBLES.getPath());
        } catch (Exception e) {
            throw new FabricException(e);
        }
    }

    @Override
    public List<String> getEnsembleContainers() {
        List<String> containers = new ArrayList<String>();
        try {
            String ensemble = getStringData(getCurator(), ZkPath.CONFIG_ENSEMBLE.getPath(getClusterId()));
            if (ensemble != null) {
                for (String name : ensemble.trim().split(",")) {
                    containers.add(name);
                }
            }
        } catch (Exception e) {
            throw new FabricException(e);
        }
        return containers;
    }

    public Git getGit() throws IOException {
        return gitService.get();
    }

    /**
     * Performs a set of operations on the git repository & avoids concurrency issues
     */
    public <T> T gitOperation(GitOperation<T> operation) {
        return gitOperation(null, operation, true);
    }

    /**
     * Performs a read only set of operations on the git repository
     * so that a pull is not done first
     */
    public <T> T gitReadOperation(GitOperation<T> operation) {
        return gitOperation(null, operation, false);
    }

    public <T> T gitOperation(PersonIdent personIdent, GitOperation<T> operation, boolean pullFirst) {
        synchronized (lock) {
            try {
                Git git = getGit();
                Repository repository = git.getRepository();
                CredentialsProvider credentialsProvider = getCredentialsProvider();
                // lets default the identity if none specified
                if (personIdent == null) {
                    personIdent = new PersonIdent(repository);
                }

                if (GitHelpers.hasGitHead(git)) {
                    // lets stash any local changes just in case..
                    git.stashCreate().setPerson(personIdent)
                            .setWorkingDirectoryMessage("Stash before a write").call();
                }
                if (pullFirst) {
                    doPull(git, credentialsProvider);
                }

                String originalBranch = repository.getBranch();
                RevCommit statusBefore = CommitUtils.getHead(repository);

                GitContext context = new GitContext();
                T answer = operation.call(git, context);
                boolean requirePush = context.isRequirePush();
                if (context.isRequireCommit()) {
                    requirePush = true;
                    String message = context.getCommitMessage().toString();
                    if (message.length() == 0) {
                        LOG.warn("No commit message from " + operation + ". Please add one! :)");
                    }
                    git.commit().setMessage(message).call();
                }

                git.checkout().setName(originalBranch).call();
                if (requirePush || hasChanged(statusBefore, CommitUtils.getHead(repository))) {
                    doPush(git, context, credentialsProvider);
                    fireChangeNotifications();
                }
                return answer;
            } catch (Exception e) {
                throw new FabricException(e);
            }
        }
    }

    protected void fireChangeNotifications() {
        LOG.debug("Firing change notifications!");
        runCallbacks();
    }

    /**
     * Returns true if a commit has been done, so we need to try push it
     *
     * @param statusBefore
     * @param statusAfter
     */
    private boolean hasChanged(RevCommit statusBefore, RevCommit statusAfter) {
        return !equals(statusBefore.getId(), statusAfter.getId());
    }

    /**
     * Pushes any changes - assumed to be invoked within a gitOperation method!
     */
    public Iterable<PushResult> doPush(Git git, GitContext gitContext) throws Exception {
        return doPush(git, gitContext, getCredentialsProvider());
    }


    /**
     * Pushes any committed changes to the remote repo
     */
    protected Iterable<PushResult> doPush(Git git, GitContext gitContext, CredentialsProvider credentialsProvider) throws Exception {
        Repository repository = git.getRepository();
        StoredConfig config = repository.getConfig();
        String url = config.getString("remote", remote, "url");
        if (Strings.isNullOrBlank(url)) {
            LOG.info("No remote repository defined yet for the git repository at " + GitHelpers
                    .getRootGitDirectory(git)
                    + " so not doing a push");
            return Collections.EMPTY_LIST;
        }

        String branch = gitContext != null && gitContext.getPushBranch() != null ? gitContext.getPushBranch() : GitHelpers.currentBranch(git);
        return git.push().setCredentialsProvider(credentialsProvider).setRefSpecs(new RefSpec(branch)).call();
    }

    protected CredentialsProvider getCredentialsProvider() throws Exception {
        Map<String, String> properties = getDataStoreProperties();
        String username = null;
        String password = null;
        if (isExternalGitConfigured(properties)) {
            username = getExternalUser(properties);
            password = getExternalCredential(properties);

        } else {
            username = getContainerLogin();
            password = generateContainerToken(getCurator());
        }
        return new UsernamePasswordCredentialsProvider(username, password);
    }

    /**
     * Check if the datastore has been configured with an external git repository.
     *
     * @param properties
     * @return
     */
    private boolean isExternalGitConfigured(Map<String, String> properties) {
        return properties != null
                && properties.containsKey(GIT_REMOTE_USER)
                && properties.containsKey(GIT_REMOTE_PASSWORD);
    }

    private String getExternalUrl(Map<String, String> properties) {
        return properties.get(GIT_REMOTE_URL);
    }

    private String getExternalUser(Map<String, String> properties) {
        return properties.get(GIT_REMOTE_USER);
    }

    private String getExternalCredential(Map<String, String> properties) throws IOException {
        return properties.get(GIT_REMOTE_PASSWORD);
    }


    /**
     * Performs a pull so the git repo is pretty much up to date before we start performing operations on it
     */
    protected void doPull(Git git, CredentialsProvider credentialsProvider) {
        try {
            Repository repository = git.getRepository();
            StoredConfig config = repository.getConfig();
            String url = config.getString("remote", remote, "url");
            if (Strings.isNullOrBlank(url)) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("No remote repository defined for the git repository at " + GitHelpers
                            .getRootGitDirectory(git)
                            + " so not doing a pull");
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
                LOG.debug(
                        "Performing a fetch in git repository " + GitHelpers.getRootGitDirectory(git)
                                + " on remote URL: "
                                + url);
            }

            boolean hasChanged = false;
            try {
                git.fetch().setCredentialsProvider(credentialsProvider).setRemote(remote).call();
            } catch (Exception e) {
                LOG.debug("Fetch failed: " + e, e);
            }

            // Get local and remote branches
            Map<String, Ref> localBranches = new HashMap<String, Ref>();
            Map<String, Ref> remoteBranches = new HashMap<String, Ref>();
            Set<String> gitVersions = new HashSet<String>();
            for (Ref ref : git.branchList().setListMode(ListBranchCommand.ListMode.ALL).call()) {
                if (ref.getName().startsWith("refs/remotes/" + remote + "/")) {
                    String name = ref.getName().substring(("refs/remotes/" + remote + "/").length());
                    if (!name.endsWith("-tmp")) {
                        remoteBranches.put(name, ref);
                        gitVersions.add(name);
                    }
                } else if (ref.getName().startsWith("refs/heads/")) {
                    String name = ref.getName().substring(("refs/heads/").length());
                    if (!name.endsWith("-tmp")) {
                        localBranches.put(name, ref);
                        gitVersions.add(name);
                    }
                }
            }

            // Check git commmits
            for (String version : gitVersions) {
                // Delete unneeded local branches
                if (!remoteBranches.containsKey(version)) {
                    //We never want to delete the master branch.
                    if (!version.equals(MASTER_BRANCH)) {
                        try {
                            git.branchDelete().setBranchNames(localBranches.get(version).getName()).setForce(true).call();
                        } catch (CannotDeleteCurrentBranchException ex) {
                            git.checkout().setName(MASTER_BRANCH).setForce(true).call();
                            git.branchDelete().setBranchNames(localBranches.get(version).getName()).setForce(true).call();
                        }
                        hasChanged = true;
                    }
                }
                // Create new local branches
                else if (!localBranches.containsKey(version)) {
                    git.checkout().setCreateBranch(true).setName(version).setStartPoint(remote +"/" + version).setUpstreamMode(CreateBranchCommand.SetupUpstreamMode.TRACK).setForce(true).call();
                    hasChanged = true;
                } else {
                    String localCommit = localBranches.get(version).getObjectId().getName();
                    String remoteCommit = remoteBranches.get(version).getObjectId().getName();
                    if (!localCommit.equals(remoteCommit)) {
                        git.clean().setCleanDirectories(true).call();
                        git.checkout().setName("HEAD").setForce(true).call();
                        git.checkout().setName(version).setForce(true).call();
                        MergeResult result = git.merge().setStrategy(MergeStrategy.THEIRS).include(remoteBranches.get(version).getObjectId()).call();
                        if (result.getMergeStatus() != MergeResult.MergeStatus.ALREADY_UP_TO_DATE) {
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
                    File profilesDirectory = getProfilesDirectory(git);
                }
                fireChangeNotifications();
            }
        } catch (Throwable e) {
            LOG.error(
                    "Failed to pull from the remote git repo " + GitHelpers.getRootGitDirectory(git)
                            + ". Reason: " + e,
                    e);
        }
    }

    /**
     * Creates the given profile directory in the currently checked out version branch
     */
    protected String doCreateProfile(Git git, GitContext context, String profile, String version)
            throws IOException, GitAPIException {
        File profileDirectory = getProfileDirectory(git, profile);
        File metadataFile = new File(profileDirectory, AGENT_METADATA_FILE);
        if (metadataFile.exists()) {
            return null;
        }
        profileDirectory.mkdirs();
        Files.writeToFile(metadataFile, "#Profile:" + profile + "\n", Charset.defaultCharset());
        doAddFiles(git, profileDirectory, metadataFile);
        context.setPushBranch(version);
        context.commit("Added profile " + profile);
        return profile;
    }


    /**
     * Recursively copies the given files from the given directory to the specified directory
     * adding them to the git repo along the way
     */
    protected void recursiveCopyAndAdd(Git git, File from, File toDir, String path,
                                       boolean useToDirAsDestination)
            throws GitAPIException, IOException {
        String name = from.getName();
        String pattern = path + (path.length() > 0 ? "/" : "") + name;
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
        git.add().addFilepattern(pattern).call();
    }

    /**
     * Recursively copies the profiles in a single flat directory into the new
     * directory layout; changing "foo-bar" directory into "foo/bar.profile" along the way
     */
    protected void recursiveAddLegacyProfileDirectoryFiles(Git git, File from, File toDir, String path)
            throws GitAPIException, IOException {

        if (!from.isDirectory()) {
            throw new IllegalStateException(
                    "Should only be invoked on the profiles directory but was given file " + from);
        }
        String name = from.getName();
        String pattern = path + (path.length() > 0 ? "/" : "") + name;
        File[] profiles = from.listFiles();
        File toFile = new File(toDir, name);
        if (profiles != null) {
            for (File profileDir : profiles) {
                // TODO should we try and detect regular folders somehow using some naming convention?
                if (profileDir.isDirectory()) {
                    String profileId = profileDir.getName();
                    String toProfileDirName = convertProfileIdToDirectory(profileId);
                    File toProfileDir = new File(toFile, toProfileDirName);
                    toProfileDir.mkdirs();
                    recursiveCopyAndAdd(git, profileDir, toProfileDir, pattern, true);
                } else {
                    recursiveCopyAndAdd(git, profileDir, toFile, pattern, true);
                }
            }
        }
        git.add().addFilepattern(pattern).call();
    }

    /**
     * Takes a profile ID of the form "foo-bar" and if we are using directory trees for profiles then
     * converts it to "foo/bar.profile"
     */
    public String convertProfileIdToDirectory(String profileId) {
        if (useDirectoriesForProfiles) {
            return profileId.replace('-', '/') + PROFILE_FOLDER_SUFFIX;
        } else {
            return profileId;
        }
    }

    protected void pull() {
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

    protected void checkoutVersion(Git git, String version) throws GitAPIException {
        GitHelpers.checkoutBranch(git, version, remote);
    }

    protected void doAddFiles(Git git, File... files) throws GitAPIException, IOException {
        File rootDir = GitHelpers.getRootGitDirectory(git);
        for (File file : files) {
            String relativePath = getFilePattern(rootDir, file);
            git.add().addFilepattern(relativePath).call();
        }
    }

    protected void doRecursiveDeleteAndRemove(Git git, File file) throws IOException, GitAPIException {
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

    protected byte[] doLoadFileConfiguration(File file) throws IOException {
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

    protected String getFilePattern(File rootDir, File file) throws IOException {
        String relativePath = Files.getRelativePath(rootDir, file);
        if (relativePath.startsWith("/")) {
            relativePath = relativePath.substring(1);
        }
        return relativePath;
    }

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public GitDataStore getDataStore() {
        return this;
    }

    @Override
    public Map<String, String> getDataStoreProperties() {
        return super.getDataStoreProperties();
    }

    @Override
    public void setDataStoreProperties(Map<String, String> dataStoreProperties) {
        Map<String, String> properties = new HashMap<String, String>();
        for (Map.Entry<String, String> entry : dataStoreProperties.entrySet()) {
            String key = entry.getKey();
            if (Arrays.asList(SUPPORTED_CONFIGURATION).contains(key)) {
                properties.put(key, entry.getValue());
            }
        }
        super.setDataStoreProperties(properties);
    }

    private static boolean equals(Object a, Object b) {
        return (a == b) || (a != null && a.equals(b));
    }
}
