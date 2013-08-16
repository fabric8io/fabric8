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
package org.fusesource.fabric.service.git;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.curator.framework.CuratorFramework;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.Service;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.StoredConfig;
import org.fusesource.fabric.api.DataStore;
import org.fusesource.fabric.api.FabricException;
import org.fusesource.fabric.api.FabricRequirements;
import org.fusesource.fabric.api.PlaceholderResolver;
import org.fusesource.fabric.internal.DataStoreHelpers;
import org.fusesource.fabric.internal.RequirementsJson;
import org.fusesource.fabric.service.DataStoreSupport;
import org.fusesource.fabric.utils.Files;
import org.fusesource.fabric.utils.Strings;
import org.fusesource.fabric.zookeeper.ZkPath;
import org.gitective.core.RepositoryUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.fusesource.fabric.zookeeper.utils.ZooKeeperUtils.exists;
import static org.fusesource.fabric.zookeeper.utils.ZooKeeperUtils.getPropertiesAsMap;
import static org.fusesource.fabric.zookeeper.utils.ZooKeeperUtils.getStringData;
import static org.fusesource.fabric.zookeeper.utils.ZooKeeperUtils.setData;
import static org.fusesource.fabric.zookeeper.utils.ZooKeeperUtils.setPropertiesAsMap;

/**
 * A git based implementation of {@link DataStore} which stores the profile configuration
 * versions in a branch per version and directory per profile.
 */
public class GitDataStore extends DataStoreSupport {
    private static final transient Logger LOG = LoggerFactory.getLogger(GitDataStore.class);

    private static final String PROFILE_ATTRIBUTES_PID = "org.fusesource.fabric.datastore";

    public static final String CONFIGS = "/fabric";
    public static final String CONFIGS_PROFILES = CONFIGS + "/profiles";
    public static final String CONFIGS_METRICS = CONFIGS + "/metrics";
    public static final String AGENT_METADATA_FILE = "org.fusesource.fabric.agent.properties";

    private FabricGitService gitService;

    private final Object lock = new Object();
    private String remote = "origin";
    private String masterBranch = "master";

    public FabricGitService getGitService() {
        return gitService;
    }

    public void setGitService(FabricGitService gitService) {
        this.gitService = gitService;
    }

    @Override
    public void importFromFileSystem(final String from) {
        importFromFileSystem(from, "");
    }

    public void importFromFileSystem(final String from, final String destinationPath) {
        gitOperation(new GitOperation<Void>() {
            public Void call(Git git) throws Exception {
                String version = getDefaultVersion();
                createVersion(version);

                // now lets recursively add files
                File toDir = GitHelpers.getRootGitDirectory(git);
                if (Strings.isNotBlank(destinationPath)) {
                    toDir = new File(toDir, destinationPath);
                }
                recursiveCopyAndAdd(git, new File(from), toDir, destinationPath);
                git.commit().setMessage("Imported from " + from).call();
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
            public Void call(Git git) throws Exception {
                // TODO lets checkout the previous versionu first!
                checkoutVersion(git, version);
                return null;
            }
        });
    }

    @Override
    public void createVersion(final String parentVersionId, final String toVersion) {
        // create a branch
        gitOperation(new GitOperation<Void>() {
            public Void call(Git git) throws Exception {
                // lets checkout the parent version first
                checkoutVersion(git, parentVersionId);
                checkoutVersion(git, toVersion);
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
        return gitOperation(new GitOperation<List<String>>() {
            public List<String> call(Git git) throws Exception {
                Collection<String> branches = RepositoryUtils.getBranches(git.getRepository());
                List<String> answer = new ArrayList<String>();
                for (String branch : branches) {
                    String name = branch;
                    String prefix = "refs/heads/";
                    if (name.startsWith(prefix)) {
                        name = name.substring(prefix.length());
                    }
                    if (!name.equals(masterBranch)) {
                        answer.add(name);
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
        return gitOperation(new GitOperation<List<String>>() {
            public List<String> call(Git git) throws Exception {
                checkoutVersion(git, version);
                File profilesDir = getProfilesDirectory(git);
                List<String> answer = new ArrayList<String>();
                if (profilesDir.exists()) {
                    File[] files = profilesDir.listFiles();
                    if (files != null) {
                        for (File file : files) {
                            if (file.isDirectory()) {
                                // TODO we could recursively scan for magic ".profile" files or something
                                // then we could put profiles into nicer tree structure?
                                answer.add(file.getName());
                            }
                        }
                    }
                }
                return answer;
            }
        });
    }

    public File getProfilesDirectory(Git git) {
        return new File(GitHelpers.getRootGitDirectory(git), GitDataStore.CONFIGS_PROFILES);
    }

    public File getProfileDirectory(Git git, String profile) {
        File profilesDirectory = getProfilesDirectory(git);
        return new File(profilesDirectory, profile);
    }

    @Override
    public String getProfile(final String version, final String profile, final boolean create) {
        return gitOperation(new GitOperation<String>() {
            public String call(Git git) throws Exception {
                checkoutVersion(git, version);
                File profileDirectory = getProfileDirectory(git, profile);
                if (!profileDirectory.exists()) {
                    if (create) {
                        return doCreateProfile(git, profile);
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
            public String call(Git git) throws Exception {
                checkoutVersion(git, version);
                return doCreateProfile(git, profile);
            }
        });
    }

    @Override
    public void deleteProfile(final String version, final String profile) {
        gitOperation(new GitOperation<Void>() {
            public Void call(Git git) throws Exception {
                checkoutVersion(git, version);
                File profileDirectory = getProfileDirectory(git, profile);
                doRecursiveDeleteAndRemove(git, profileDirectory);
                git.commit().setMessage("Removed profile " + profile).call();
                return null;
            }
        });
    }


    @Override
    public Map<String, String> getVersionAttributes(String version) {
        // TODO
        todo();
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
        todo();
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
        return getConfiguration(version, profile, PROFILE_ATTRIBUTES_PID);
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
        Long answer = gitOperation(new GitOperation<Long>() {
            public Long call(Git git) throws Exception {
                checkoutVersion(git, version);
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
        return gitOperation(new GitOperation<Map<String, byte[]>>() {
            public Map<String, byte[]> call(Git git) throws Exception {
                checkoutVersion(git, version);
                return doGetFileConfigurations(git, profile);
            }
        });
    }

    protected Map<String, byte[]> doGetFileConfigurations(Git git, String profile) throws IOException {
        Map<String, byte[]> configurations = new HashMap<String, byte[]>();
        File profileDirectory = getProfileDirectory(git, profile);
        File[] files = profileDirectory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isFile()) {
                    String relativePath = getFilePattern(profileDirectory, file);
                    configurations.put(relativePath, doLoadFileConfiguration(file));
                }
            }
        }
        return configurations;
    }

    @Override
    public byte[] getFileConfiguration(final String version, final String profile, final String fileName) {
        return gitOperation(new GitOperation<byte[]>() {
            public byte[] call(Git git) throws Exception {
                checkoutVersion(git, version);
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
            public Void call(Git git) throws Exception {
                checkoutVersion(git, version);
                File profileDirectory = getProfileDirectory(git, profile);
                doSetFileConfigurations(git, profileDirectory, profile, configurations);
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
            doRecursiveDeleteAndRemove(git, getPidFile(profileDirectory, pid));
        }
        git.commit().setMessage("Updated configuration for profile " + profile).call();
    }

    @Override
    public void setFileConfiguration(final String version, final String profile, final String fileName,
                                     final byte[] configuration) {
        gitOperation(new GitOperation<Void>() {
            public Void call(Git git) throws Exception {
                checkoutVersion(git, version);
                doSetFileConfiguration(git, profile, fileName, configuration);
                git.commit().setMessage("Updated " + fileName + " for profile " + profile).call();
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
        return gitOperation(new GitOperation<Map<String, String>>() {
            public Map<String, String> call(Git git) throws Exception {
                checkoutVersion(git, version);
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
    protected <T> T gitOperation(GitOperation<T> operation) {
        return gitOperation(null, operation);
    }

    protected <T> T gitOperation(PersonIdent personIdent, GitOperation<T> operation) {
        synchronized (lock) {
            try {
                Git git = getGit();
                // lets default the identity if none specified
                if (personIdent == null) {
                    personIdent = new PersonIdent(git.getRepository());
                }

                if (GitHelpers.hasGitHead(git)) {
                    // lets stash any local changes just in case..
                    git.stashCreate().setPerson(personIdent)
                            .setWorkingDirectoryMessage("Stash before a write").setRef("HEAD").call();
                }
                doPull(git);
                T answer = operation.call(git);
                doPush(git);
                return answer;
            } catch (Exception e) {
                throw new FabricException(e);
            }
        }
    }

    /**
     * Pushes any committed changes to the remote repo
     */
    protected void doPush(Git git) throws GitAPIException {
        Repository repository = git.getRepository();
        StoredConfig config = repository.getConfig();
        String url = config.getString("remote", remote, "url");
        if (Strings.isNullOrBlank(url)) {
            LOG.warn("No remote repository defined for the git repository at " + GitHelpers
                    .getRootGitDirectory(git)
                    + " so not doing a push");
            return;
        }
        git.push().call();
    }

    /**
     * Performs a pull so the git repo is pretty much up to date before we start performing operations on it
     */
    protected void doPull(Git git) {
        try {
            Repository repository = git.getRepository();
            StoredConfig config = repository.getConfig();
            String url = config.getString("remote", remote, "url");
            if (Strings.isNullOrBlank(url)) {
                LOG.warn("No remote repository defined for the git repository at " + GitHelpers
                        .getRootGitDirectory(git)
                        + " so not doing a pull");
                return;
            }
            String branch = repository.getBranch();
            String mergeUrl = config.getString("branch", branch, "merge");
            if (Strings.isNullOrBlank(mergeUrl)) {
                LOG.warn("No merge spec for branch." + branch + ".merge in the git repository at "
                        + GitHelpers.getRootGitDirectory(git) + " so not doing a pull");
                return;
            }
            if (LOG.isDebugEnabled()) {
                LOG.debug(
                        "Performing a pull in git repository " + GitHelpers.getRootGitDirectory(git)
                                + " on remote URL: "
                                + url);
            }
            //git.pull().setCredentialsProvider(cp).setRebase(true).call();
            git.pull().setRebase(true).call();
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
    protected String doCreateProfile(Git git, String profile) throws IOException, GitAPIException {
        File profileDirectory = getProfileDirectory(git, profile);
        File metadataFile = new File(profileDirectory, AGENT_METADATA_FILE);
        if (metadataFile.exists()) {
            return null;
        }
        profileDirectory.mkdirs();
        Files.writeToFile(metadataFile, "#Profile:" + profile + "\n", Charset.defaultCharset());
        doAddFiles(git, profileDirectory, metadataFile);
        git.commit().setMessage("Added profile " + profile).call();
        return profile;
    }


    /**
     * Recursively copies the given files from the given directory to the specified directory
     * adding them to the git repo along the way
     */
    protected void recursiveCopyAndAdd(Git git, File from, File toDir, String path)
            throws GitAPIException, IOException {
        String name = from.getName();
        String pattern = path + (path.length() > 0 ? "/" : "") + name;
        File toFile = new File(toDir, name);

        if (from.isDirectory()) {
            // lets assume the contents of the first directory go directly into toDir
            // rather than, say, creating an 'import' directory when importing ;)
            if (path.length() == 0) {
                toFile = toDir;
            }
            toFile.mkdirs();
            File[] files = from.listFiles();
            if (files != null) {
                for (File file : files) {
                    recursiveCopyAndAdd(git, file, toFile, pattern);
                }
            }
        } else {
            Files.copy(from, toFile);
        }
        git.add().addFilepattern(pattern).call();
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
}
