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

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.TreeData;
import org.apache.curator.utils.ZKPaths;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.zookeeper.KeeperException;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.StoredConfig;
import org.fusesource.common.util.Strings;
import org.fusesource.fabric.api.DataStore;
import org.fusesource.fabric.api.FabricException;
import org.fusesource.fabric.api.FabricRequirements;
import org.fusesource.fabric.git.FabricGitService;
import org.fusesource.fabric.internal.DataStoreHelpers;
import org.fusesource.fabric.internal.RequirementsJson;
import org.fusesource.fabric.service.DataStoreSupport;
import org.fusesource.fabric.utils.Files;
import org.fusesource.fabric.zookeeper.ZkPath;
import org.fusesource.fabric.zookeeper.ZkProfiles;
import org.gitective.core.RepositoryUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.fusesource.fabric.git.datastore.GitHelpers.getRootGitDirectory;
import static org.fusesource.fabric.git.datastore.GitHelpers.hasGitHead;
import static org.fusesource.fabric.zookeeper.utils.ZooKeeperUtils.create;
import static org.fusesource.fabric.zookeeper.utils.ZooKeeperUtils.createDefault;
import static org.fusesource.fabric.zookeeper.utils.ZooKeeperUtils.deleteSafe;
import static org.fusesource.fabric.zookeeper.utils.ZooKeeperUtils.exists;
import static org.fusesource.fabric.zookeeper.utils.ZooKeeperUtils.getAllChildren;
import static org.fusesource.fabric.zookeeper.utils.ZooKeeperUtils.getByteData;
import static org.fusesource.fabric.zookeeper.utils.ZooKeeperUtils.getChildren;
import static org.fusesource.fabric.zookeeper.utils.ZooKeeperUtils.getProperties;
import static org.fusesource.fabric.zookeeper.utils.ZooKeeperUtils.getPropertiesAsMap;
import static org.fusesource.fabric.zookeeper.utils.ZooKeeperUtils.getStringData;
import static org.fusesource.fabric.zookeeper.utils.ZooKeeperUtils.lastModified;
import static org.fusesource.fabric.zookeeper.utils.ZooKeeperUtils.setData;
import static org.fusesource.fabric.zookeeper.utils.ZooKeeperUtils.setProperties;
import static org.fusesource.fabric.zookeeper.utils.ZooKeeperUtils.setPropertiesAsMap;

/**
 * @author Stan Lewis
 */
@Component(name = "org.fusesource.fabric.git.datastore",
        description = "Fabric Git and ZooKeeper DataStore")
@Service(DataStore.class)
public class GitAndZooKeeperDataStore extends DataStoreSupport {
    private static final transient Logger LOG = LoggerFactory.getLogger(GitAndZooKeeperDataStore.class);
    private final String masterBranch = "master";

    public static final String CONFIGS = "/fabric";
    public static final String CONFIGS_PROFILES = CONFIGS + "/profiles";
    public static final String CONFIGS_METRICS = CONFIGS + "/metrics";

    @Reference(cardinality = org.apache.felix.scr.annotations.ReferenceCardinality.MANDATORY_UNARY)
    private FabricGitService gitService;

    private Object lock = new Object();
    private String remote = "remote";

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
                File toDir = getRootGitDirectory(git);
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
        return new File(GitHelpers.getRootGitDirectory(git), GitAndZooKeeperDataStore.CONFIGS_PROFILES);
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
        todo();
        try {
            String path = ZkProfiles.getPath(version, profile);
            return getPropertiesAsMap(treeCache, path);
        } catch (Exception e) {
            throw new FabricException(e);
        }
    }

    @Override
    public void setProfileAttribute(String version, String profile, String key, String value) {
        try {
            String path = ZkProfiles.getPath(version, profile);
            Properties props = getProperties(getCurator(), path);
            if (value != null) {
                props.setProperty(key, value);
            } else {
                props.remove(key);
            }
            setProperties(getCurator(), path, props);
        } catch (Exception e) {
            throw new FabricException(e);
        }
    }

    @Override
    public long getLastModified(String version, String profile) {
        try {
            return lastModified(getCurator(), ZkProfiles.getPath(version, profile));
        } catch (Exception e) {
            throw new FabricException(e);
        }
    }

    @Override
    public Map<String, byte[]> getFileConfigurations(String version, String profile) {
        try {
            Map<String, byte[]> configurations = new HashMap<String, byte[]>();
            String path = ZkProfiles.getPath(version, profile);
            List<String> children = getAllChildren(treeCache, path);
            for (String child : children) {
                TreeData data = treeCache.getCurrentData(child);
                if (data.getData() != null && data.getData().length != 0) {
                    String relativePath = child.substring(path.length() + 1);
                    configurations.put(relativePath, getFileConfiguration(version, profile, relativePath));
                }
            }
            return configurations;
        } catch (Exception e) {
            throw new FabricException(e);
        }
    }

    @Override
    public byte[] getFileConfiguration(String version, String profile, String pid) {
        try {
            String path = ZkProfiles.getPath(version, profile) + "/" + pid;
            if (treeCache.getCurrentData(path) == null) {
                return null;
            }
            if (treeCache.getCurrentData(path).getData() == null) {
                List<String> children = treeCache.getChildrenNames(path);
                StringBuilder buf = new StringBuilder();
                for (String child : children) {
                    String value = new String(treeCache.getCurrentData(path + "/" + child).getData(),
                            "UTF-8");
                    buf.append(String.format("%s = %s\n", child, value));
                }
                return buf.toString().getBytes();
            } else {
                return getByteData(treeCache, path);
            }
        } catch (Exception e) {
            throw new FabricException(e);
        }
    }

    @Override
    public void setFileConfigurations(String version, String profile, Map<String, byte[]> configurations) {
        try {
            Map<String, byte[]> oldCfgs = getFileConfigurations(version, profile);
            String path = ZkProfiles.getPath(version, profile);

            for (Map.Entry<String, byte[]> entry : configurations.entrySet()) {
                String pid = entry.getKey();
                oldCfgs.remove(pid);
                byte[] newCfg = entry.getValue();
                setFileConfiguration(version, profile, pid, newCfg);
            }

            for (String pid : oldCfgs.keySet()) {
                deleteSafe(getCurator(), path + "/" + pid);
            }
        } catch (Exception e) {
            throw new FabricException(e);
        }
    }

    @Override
    public void setFileConfiguration(String version, String profile, String pid, byte[] configuration) {
        try {
            String path = ZkProfiles.getPath(version, profile);
            String configPath = path + "/" + pid;
            if (exists(getCurator(), configPath) != null
                    && getChildren(getCurator(), configPath).size() > 0) {
                List<String> kids = getChildren(getCurator(), configPath);
                ArrayList<String> saved = new ArrayList<String>();
                // old format, we assume that the byte stream is in
                // a .properties format
                for (String line : new String(configuration).split("\n")) {
                    if (line.startsWith("#") || line.length() == 0) {
                        continue;
                    }
                    String nameValue[] = line.split("=", 2);
                    if (nameValue.length < 2) {
                        continue;
                    }
                    String newPath = configPath + "/" + nameValue[0].trim();
                    setData(getCurator(), newPath, nameValue[1].trim());
                    saved.add(nameValue[0].trim());
                }
                for (String kid : kids) {
                    if (!saved.contains(kid)) {
                        deleteSafe(getCurator(), configPath + "/" + kid);
                    }
                }
            } else {
                setData(getCurator(), configPath, configuration);
            }
        } catch (Exception e) {
            throw new FabricException(e);
        }
    }

    @Override
    public Map<String, Map<String, String>> getConfigurations(String version, String profile) {
        try {
            Map<String, Map<String, String>> configurations = new HashMap<String, Map<String, String>>();
            Map<String, byte[]> configs = getFileConfigurations(version, profile);
            for (Map.Entry<String, byte[]> entry : configs.entrySet()) {
                if (entry.getKey().endsWith(".properties")) {
                    String pid = DataStoreHelpers.stripSuffix(entry.getKey(), ".properties");
                    configurations.put(pid,
                            DataStoreHelpers.toMap(DataStoreHelpers.toProperties(entry.getValue())));
                }
            }
            return configurations;
        } catch (Exception e) {
            throw new FabricException(e);
        }
    }

    @Override
    public Map<String, String> getConfiguration(String version, String profile, String pid) {
        try {
            String path = ZkProfiles.getPath(version, profile) + "/" + pid + ".properties";
            if (treeCache.getCurrentData(path) == null) {
                return null;
            }
            byte[] data = getByteData(treeCache, path);
            return DataStoreHelpers.toMap(DataStoreHelpers.toProperties(data));
        } catch (Exception e) {
            throw new FabricException(e);
        }
    }

    @Override
    public void setConfigurations(String version, String profile,
                                  Map<String, Map<String, String>> configurations) {
        try {
            Map<String, Map<String, String>> oldCfgs = getConfigurations(version, profile);
            // Store new configs
            String path = ZkProfiles.getPath(version, profile);
            for (Map.Entry<String, Map<String, String>> entry : configurations.entrySet()) {
                String pid = entry.getKey();
                oldCfgs.remove(pid);
                setConfiguration(version, profile, pid, entry.getValue());
            }
            for (String key : oldCfgs.keySet()) {
                deleteSafe(getCurator(), path + "/" + key + ".properties");
            }
        } catch (Exception e) {
            throw new FabricException(e);
        }
    }

    @Override
    public void setConfiguration(String version, String profile, String pid,
                                 Map<String, String> configuration) {
        try {
            String path = ZkProfiles.getPath(version, profile);
            byte[] data = DataStoreHelpers.toBytes(DataStoreHelpers.toProperties(configuration));
            String p = path + "/" + pid + ".properties";
            setData(getCurator(), p, data);
        } catch (Exception e) {
            throw new FabricException(e);
        }
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

    private static String substituteZookeeperUrl(String key, CuratorFramework curator) {
        try {
            return new String(ZkPath.loadURL(curator, key), "UTF-8");
        } catch (KeeperException.NoNodeException e) {
            return key;
        } catch (Exception e) {
            throw new FabricException(e);
        }
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

                if (hasGitHead(git)) {
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
        git.push().call();
    }

    /**
     * Performs a pull so the git repo is pretty much up to date before we start performing operations on it
     */
    protected void doPull(Git git) {
        try {
            Repository repository = git.getRepository();
            StoredConfig config = repository.getConfig();
            String url = config.getString("remote", "origin", "url");
            if (Strings.isNullOrBlank(url)) {
                LOG.warn("No remote repository defined for the git repository at " + getRootGitDirectory(git)
                        + " so not doing a pull");
                return;
            }
            String branch = repository.getBranch();
            String mergeUrl = config.getString("branch", branch, "merge");
            if (Strings.isNullOrBlank(mergeUrl)) {
                LOG.warn("No merge spec for branch." + branch + ".merge in the git repository at "
                        + getRootGitDirectory(git) + " so not doing a pull");
                return;
            }
            if (LOG.isDebugEnabled()) {
                LOG.debug(
                        "Performing a pull in git repository " + getRootGitDirectory(git) + " on remote URL: "
                                + url);
            }
            //git.pull().setCredentialsProvider(cp).setRebase(true).call();
            git.pull().setRebase(true).call();
        } catch (Throwable e) {
            LOG.error(
                    "Failed to pull from the remote git repo " + getRootGitDirectory(git) + ". Reason: " + e,
                    e);
        }
    }

    /**
     * Creates the given profile directory in the currently checked out version branch
     */
    protected String doCreateProfile(Git git, String profile) throws IOException, GitAPIException {
        File profileDirectory = getProfileDirectory(git, profile);
        File metadataFile = new File(profileDirectory, "org.fusesource.fabric.agent.properties");
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
        File rootDir = getRootGitDirectory(git);
        for (File file : files) {
            String relativePath = getFilePattern(rootDir, file);
            git.add().addFilepattern(relativePath).call();
        }
    }

    protected void doRecursiveDeleteAndRemove(Git git, File file) throws IOException, GitAPIException {
        File rootDir = getRootGitDirectory(git);
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

    protected String getFilePattern(File rootDir, File file) throws IOException {
        String relativePath = Files.getRelativePath(rootDir, file);
        if (relativePath.startsWith("/")) {
            relativePath = relativePath.substring(1);
        }
        return relativePath;
    }
}
