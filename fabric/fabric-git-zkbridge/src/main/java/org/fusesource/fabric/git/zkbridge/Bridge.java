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
package org.fusesource.fabric.git.zkbridge;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.state.ConnectionState;
import org.apache.curator.framework.state.ConnectionStateListener;
import org.apache.felix.utils.properties.Properties;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ListBranchCommand;
import org.eclipse.jgit.api.MergeResult;
import org.eclipse.jgit.api.ResetCommand;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.merge.MergeStrategy;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.fusesource.fabric.git.FabricGitService;
import org.fusesource.fabric.groups.GroupListener;
import org.fusesource.fabric.groups.Group;
import org.fusesource.fabric.groups.internal.ZooKeeperGroup;
import org.fusesource.fabric.utils.Closeables;
import org.fusesource.fabric.utils.Files;
import org.fusesource.fabric.zookeeper.ZkPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.fusesource.fabric.zookeeper.utils.ZooKeeperUtils.create;
import static org.fusesource.fabric.zookeeper.utils.ZooKeeperUtils.createDefault;
import static org.fusesource.fabric.zookeeper.utils.ZooKeeperUtils.deleteSafe;
import static org.fusesource.fabric.zookeeper.utils.ZooKeeperUtils.exists;
import static org.fusesource.fabric.zookeeper.utils.ZooKeeperUtils.generateContainerToken;
import static org.fusesource.fabric.zookeeper.utils.ZooKeeperUtils.getChildren;
import static org.fusesource.fabric.zookeeper.utils.ZooKeeperUtils.getContainerLogin;
import static org.fusesource.fabric.zookeeper.utils.ZooKeeperUtils.getStringData;
import static org.fusesource.fabric.zookeeper.utils.ZooKeeperUtils.lastModified;
import static org.fusesource.fabric.zookeeper.utils.ZooKeeperUtils.setData;
import static org.fusesource.fabric.zookeeper.utils.ZooKeeperUtils.setPropertiesAsMap;

public class Bridge implements ConnectionStateListener, GroupListener<GitZkBridgeNode> {

    public static final String CONTAINERS_PROPERTIES = "containers.properties";
    public static final String METADATA = ".metadata";

    private static final Logger LOGGER = LoggerFactory.getLogger(Bridge.class);

    private Group<GitZkBridgeNode> group;
    private boolean connected = false;

    private FabricGitService gitService;
    private CuratorFramework curator;
    private long period;
    private ScheduledExecutorService executors;

    public synchronized void bindGitService(FabricGitService gitService) {
        this.gitService = gitService;
        if (connected) {
            group.update(createState());
        }
    }

    public synchronized void unbindGitService(FabricGitService gitService) {
        if (connected) {
            try {
                group.close();
            } catch (IOException e) {
                // Ignore
            }
        }
        this.gitService = null;
    }

    @Override
    public void stateChanged(CuratorFramework client, ConnectionState newState) {
        switch (newState) {
            case CONNECTED:
            case RECONNECTED:
                this.curator = client;
                onConnected();
                break;
            default:
                onDisconnected();
        }
    }

    public synchronized void onConnected() {
        connected = true;
        group = new ZooKeeperGroup<GitZkBridgeNode>(curator, "/fabric/registry/clusters/gitzkbridge", GitZkBridgeNode.class);
        group.add(this);
        if (gitService != null) {
            group.update(createState());
        }
        group.start();
    }

    public synchronized void onDisconnected() {
        connected = false;
        try {
            if (group != null) {
                group.close();
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to remove git server from registry.", e);
        }
    }

    @Override
    public void groupEvent(Group<GitZkBridgeNode> group, GroupEvent event) {
        if (group.isMaster()) {
            LOGGER.info("Git/zk bridge is active");
        } else {
            LOGGER.info("Git/zk bridge is inactive");
        }
        try {
            group.update(createState());
        } catch (IllegalStateException e) {
            // Ignore
        }
    }

    GitZkBridgeNode createState() {
        GitZkBridgeNode state = new GitZkBridgeNode();
        state.setId("bridge");
        state.setContainer(System.getProperty("karaf.name"));
        return state;
    }

    public void init() {
        executors = Executors.newSingleThreadScheduledExecutor();
        executors.scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                try {
                    if (gitService != null && connected) {
                        String login = getContainerLogin();
                        String token = generateContainerToken(curator);
                        CredentialsProvider cp = new UsernamePasswordCredentialsProvider(login, token);
                        if (group.isMaster()) {
                            update(gitService.get(), curator, cp);
                        } else {
                            updateLocal(gitService.get(), curator, cp);
                        }
                    }
                } catch (Exception e) {
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("Unable to sync git/zookeeper", e);
                    } else {
                        LOGGER.info("Unable to sync git / zookeeper: " + e.getClass().getName() + ": " + e.getMessage());
                    }
                }
            }
        }, period, period, TimeUnit.MILLISECONDS);
    }

    public void destroy() {
        executors.shutdown();
        try {
            group.close();
        } catch (IOException e) {
            // Ignore
        }
    }

    public static void updateLocal(Git git, CuratorFramework zookeeper, CredentialsProvider credentialsProvider) throws Exception {
        String remoteName = "origin";

        try {
            git.fetch().setCredentialsProvider(credentialsProvider).setRemote(remoteName).call();
        } catch (Exception e) {
            // Ignore fetch exceptions
            return;
        }

        // Get local and remote branches
        Map<String, Ref> localBranches = new HashMap<String, Ref>();
        Map<String, Ref> remoteBranches = new HashMap<String, Ref>();
        Set<String> gitVersions = new HashSet<String>();
        for (Ref ref : git.branchList().setListMode(ListBranchCommand.ListMode.ALL).call()) {
            if (ref.getName().startsWith("refs/remotes/" + remoteName + "/")) {
                String name = ref.getName().substring(("refs/remotes/" + remoteName + "/").length());
                if (!"master".equals(name) && !name.endsWith("-tmp")) {
                    remoteBranches.put(name, ref);
                    gitVersions.add(name);
                }
            } else if (ref.getName().startsWith("refs/heads/")) {
                String name = ref.getName().substring(("refs/heads/").length());
                if (!name.equals("master") && !name.endsWith("-tmp")) {
                    localBranches.put(name, ref);
                    gitVersions.add(name);
                }
            }
        }

        // Check git commmits
        for (String version : gitVersions) {
            // Delete unneeded local branches
            if (!remoteBranches.containsKey(version)) {
                git.branchDelete().setBranchNames(localBranches.get(version).getName()).setForce(true).call();
            }
            // Create new local branches
            else if (!localBranches.containsKey(version)) {
                git.branchCreate().setName(version).call();
                git.reset().setMode(ResetCommand.ResetType.HARD).setRef(remoteBranches.get(version).getName()).call();
            } else {
                String localCommit = localBranches.get(version).getObjectId().getName();
                String remoteCommit = remoteBranches.get(version).getObjectId().getName();
                if (!localCommit.equals(remoteCommit)) {
                    git.clean().setCleanDirectories(true).call();
                    git.checkout().setName("HEAD").setForce(true).call();
                    git.checkout().setName(version).setForce(true).call();
                    MergeResult result = git.merge().setStrategy(MergeStrategy.THEIRS).include(remoteBranches.get(version).getObjectId()).call();
                    // TODO: handle conflicts
                }
            }
        }
    }

    public static void update(Git git, CuratorFramework zookeeper) throws Exception {
        update(git, zookeeper, null);
    }

    public static void update(Git git, CuratorFramework zookeeper, CredentialsProvider credentialsProvider) throws Exception {
        String remoteName = "origin";

        boolean remoteAvailable = false;
        try {
            git.fetch().setCredentialsProvider(credentialsProvider).setRemote(remoteName).call();
            remoteAvailable = true;
        } catch (Exception e) {
            // Ignore fetch exceptions
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Unable to fetch master", e);
            } else if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Unable to fetch master: " + e.getClass().getName() + ": " + e.getMessage());
            }
        }

        // Handle versions in git and not in zookeeper
        Map<String, Ref> localBranches = new HashMap<String, Ref>();
        Map<String, Ref> remoteBranches = new HashMap<String, Ref>();
        Set<String> gitVersions = new HashSet<String>();
        for (Ref ref : git.branchList().setListMode(ListBranchCommand.ListMode.ALL).call()) {
            if (ref.getName().startsWith("refs/remotes/" + remoteName + "/")) {
                String name = ref.getName().substring(("refs/remotes/" + remoteName + "/").length());
                if (!"master".equals(name) && !name.endsWith("-tmp")) {
                    remoteBranches.put(name, ref);
                    gitVersions.add(name);
                }
            } else if (ref.getName().startsWith("refs/heads/")) {
                String name = ref.getName().substring(("refs/heads/").length());
                if (!name.equals("master") && !name.endsWith("-tmp")) {
                    localBranches.put(name, ref);
                    gitVersions.add(name);
                }
            }
        }
        List<String> zkVersions = getChildren(zookeeper, ZkPath.CONFIG_VERSIONS.getPath());
        createDefault(zookeeper, "/fabric/configs/git", null);
        Properties versionsMetadata = loadProps(zookeeper, "/fabric/configs/git");

        boolean allDone = true;
        // Check no modifs in zookeeper
        String lastModified = Long.toString(lastModified(zookeeper, ZkPath.CONFIG_VERSIONS.getPath()));
        if (!lastModified.equals(versionsMetadata.get("zk-lastmodified"))) {
            allDone = false;
        }
        // Check the versions in zk and git are the same
        if (zkVersions.size() != gitVersions.size() || !zkVersions.containsAll(gitVersions)) {
            allDone = false;
        }
        // Check all local and remote branches exists
        if (gitVersions.size() != localBranches.size() || !localBranches.keySet().containsAll(gitVersions)) {
            allDone = false;
        }
        // If remote is available, check that all remote branches exist
        if (remoteAvailable && !remoteBranches.keySet().containsAll(gitVersions)) {
            allDone = false;
        }
        // Check git commmits
        if (allDone) {
            for (String version : zkVersions) {
                String zkCommit = versionsMetadata.get(version);
                String localCommit = localBranches.get(version).getObjectId().getName();
                String remoteCommit = remoteAvailable ? remoteBranches.get(version).getObjectId().getName() : null;
                if (!localCommit.equals(zkCommit) || remoteCommit != null && !localCommit.equals(remoteCommit)) {
                    allDone = false;
                    break;
                }
            }
        }
        if (allDone) {
            return;
        }

        // ZooKeeper -> Git changes
        for (String version : zkVersions) {
            String zkNode = ZkPath.CONFIG_VERSION.getPath(version);

            // Checkout updated version
            List<Ref> allBranches = git.branchList().setListMode(ListBranchCommand.ListMode.ALL).call();
            Ref local = null;
            Ref remote = null;
            Ref tmp = null;
            for (Ref ref : allBranches) {
                if (ref.getName().equals("refs/remotes/" + remoteName + "/" + version)) {
                    remote = ref;
                } else if (ref.getName().equals("refs/heads/" + version)) {
                    local = ref;
                } else if (ref.getName().equals("refs/heads/" + version + "-tmp")) {
                    tmp = ref;
                }
            }
            if (local == null) {
                git.branchCreate().setName(version).call();
            }
            if (tmp == null) {
                git.branchCreate().setName(version + "-tmp").call();
            }
            git.clean().setCleanDirectories(true).call();
            git.checkout().setName("HEAD").setForce(true).call();
            git.checkout().setName(version).setForce(true).call();
            if (remoteAvailable && remote != null) {
                MergeResult result = git.merge().setStrategy(MergeStrategy.THEIRS).include(remote.getObjectId()).call();
                // TODO: check merge conflicts
            }
            git.checkout().setName(version + "-tmp").setForce(true).call();
            String gitCommit = versionsMetadata.get(version);
            if (gitCommit != null) {
                try {
                    git.reset().setMode(ResetCommand.ResetType.HARD).setRef(gitCommit).call();
                } catch (Exception e) {
                    // Ignore, we did our best
                    if (LOGGER.isTraceEnabled()) {
                        LOGGER.trace("Unable to reset branch to commit", e);
                    } else if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("Unable to reset branch to commit " + gitCommit + ": " + e.getClass().getName() + ": " + e.getMessage());
                    }
                }
            }

            // Apply changes to git
            syncVersionFromZkToGit(git, zookeeper, zkNode);

            if (git.status().call().isClean()) {
                git.checkout().setName(version).setForce(true).call();
            } else {
                ObjectId rev = git.commit().setMessage("Merge zookeeper updates in version " + version).call().getId();
                git.checkout().setName(version).setForce(true).call();
                MergeResult result = git.merge().setStrategy(MergeStrategy.OURS).include(rev).call();
                // TODO: check merge conflicts
            }
            if (remoteAvailable) {
                git.push().setCredentialsProvider(credentialsProvider).setRefSpecs(new RefSpec(version)).call();
            }

            // Apply changes to zookeeper
            syncVersionFromGitToZk(git, zookeeper, zkNode);

            versionsMetadata.put(version, git.getRepository().getRef("HEAD").getObjectId().getName());
        }
        // Iterate through known git versions
        for (String version : gitVersions) {
            String state = versionsMetadata.get(version);
            if (zkVersions.contains(version)) {
                continue;
            }
            // The version is not known to zookeeper, so create it
            if (state == null) {
                if (localBranches.containsKey(version)) {
                    if (remoteAvailable) {
                        git.push().setRefSpecs(new RefSpec(version)).call();
                    }
                } else {
                    git.branchCreate().setName(version).call();
                    git.reset().setMode(ResetCommand.ResetType.HARD).setRef(remoteBranches.get(version).getName()).call();
                }
                git.checkout().setName(version).setForce(true).call();
                // Sync zookeeper
                String zkNode = ZkPath.CONFIG_VERSION.getPath(version);
                create(zookeeper, zkNode);
                create(zookeeper, ZkPath.CONFIG_VERSIONS_PROFILES.getPath(version));
                create(zookeeper, ZkPath.CONFIG_VERSIONS_CONTAINERS.getPath(version));
                syncVersionFromGitToZk(git, zookeeper, zkNode);
                // Flag version as active
                versionsMetadata.put(version, git.getRepository().getRef("HEAD").getObjectId().getName());
            }
            // The version has been deleted from zookeeper so delete it in git
            else {
                git.checkout().setName("master").setForce(true).call();
                git.branchDelete().setBranchNames(version, version + "-tmp").setForce(true).call();
                git.push().setRefSpecs(new RefSpec(version + ":")).call();
                versionsMetadata.remove(version);
            }
        }
        versionsMetadata.put("zk-lastmodified", Long.toString(lastModified(zookeeper, ZkPath.CONFIG_VERSIONS.getPath())));
        setPropertiesAsMap(zookeeper, "/fabric/configs/git", versionsMetadata);
    }

    private static void syncVersionFromZkToGit(Git git, CuratorFramework curator, String zkNode) throws Exception {
        // Version metadata
        Properties versionProps = loadProps(curator, zkNode);
        versionProps.save(new File(git.getRepository().getWorkTree(), METADATA));
        git.add().addFilepattern(METADATA).call();
        // Profiles
        List<String> gitProfiles = list(git.getRepository().getWorkTree());
        gitProfiles.remove(".git");
        gitProfiles.remove(METADATA);
        gitProfiles.remove(CONTAINERS_PROPERTIES);
        List<String> zkProfiles = getChildren(curator, zkNode + "/profiles");
        for (String profile : zkProfiles) {
            File profileDir = new File(git.getRepository().getWorkTree(), profile);
            profileDir.mkdirs();
            // Profile metadata
            Properties profileProps = loadProps(curator, zkNode + "/profiles/" + profile);
            profileProps.save(new File(git.getRepository().getWorkTree(), profile + "/" + METADATA));
            git.add().addFilepattern(profile + "/" + METADATA).call();
            // Configs
            List<String> gitConfigs = list(profileDir);
            gitConfigs.remove(METADATA);
            List<String> zkConfigs = getChildren(curator, zkNode + "/profiles/" + profile);
            for (String file : zkConfigs) {
                byte[] data = curator.getData().forPath(zkNode + "/profiles/" + profile + "/" + file);
                Files.writeToFile(new File(git.getRepository().getWorkTree(), profile + "/" + file), data);
                gitConfigs.remove(file);
                git.add().addFilepattern(profile + "/" + file).call();
            }
            for (String file : gitConfigs) {
                new File(profileDir, file).delete();
                git.rm().addFilepattern(profile + "/" + file).call();
            }
            gitProfiles.remove(profile);
        }
        for (String profile : gitProfiles) {
            delete(new File(git.getRepository().getWorkTree(), profile));
            git.rm().addFilepattern(profile).call();
        }
        // Containers
        Properties containerProps = new Properties();
        for (String container : getChildren(curator, zkNode + "/containers")) {
            String str = getStringData(curator, zkNode + "/containers/" + container);
            if (str != null) {
                containerProps.setProperty(container, str);
            }
        }
        containerProps.save(new File(git.getRepository().getWorkTree(), CONTAINERS_PROPERTIES));
        git.add().addFilepattern(CONTAINERS_PROPERTIES).call();
    }

    private static void syncVersionFromGitToZk(Git git, CuratorFramework curator, String zkNode) throws Exception {
        // Version metadata
        Properties versionProps = loadProps(git, METADATA);
        setData(curator, zkNode, toString(versionProps));
        // Profiles
        List<String> gitProfiles = list(git.getRepository().getWorkTree());
        gitProfiles.remove(".git");
        gitProfiles.remove(METADATA);
        gitProfiles.remove(CONTAINERS_PROPERTIES);
        List<String> zkProfiles = getChildren(curator, zkNode + "/profiles");
        for (String profile : gitProfiles) {
            // Profile metadata
            Properties profileProps = loadProps(git, profile + "/" + METADATA);
            setData(curator, zkNode + "/profiles/" + profile, toString(profileProps));
            // Configs
            List<String> zkConfigs = getChildren(curator, zkNode + "/profiles/" + profile);
            List<String> gitConfigs = list(new File(git.getRepository().getWorkTree(), profile));
            gitConfigs.remove(METADATA);
            for (String file : gitConfigs) {
                byte[] data = read(new File(git.getRepository().getWorkTree(), profile + "/" + file));
                setData(curator, zkNode + "/profiles/" + profile + "/" + file, data);
                zkConfigs.remove(file);
            }
            // Delete removed configs
            for (String config : zkConfigs) {
                deleteSafe(curator, zkNode + "/profiles/" + profile + "/" + config);
            }
            zkProfiles.remove(profile);
        }
        // Delete removed profiles
        for (String profile : zkProfiles) {
            deleteSafe(curator, zkNode + "/profiles/" + profile);
        }
        // Containers
        Properties containerProps = loadProps(git, CONTAINERS_PROPERTIES);
        for (String container : containerProps.keySet()) {
            setData(curator, zkNode + "/containers/" + container, containerProps.getProperty(container));
        }
        for (String container : getChildren(curator, zkNode + "/containers")) {
            if (!containerProps.containsKey(container)) {
                deleteSafe(curator, zkNode + "/containers/" + container);
            }
        }
    }

    private static Properties loadProps(CuratorFramework curator, String node) throws Exception {
        Properties props = new Properties();
        if (exists(curator, node) != null) {
            String data = getStringData(curator, node);
            if (data != null) {
                props.load(new StringReader(data));
            }
        }
        return props;
    }

    private static Properties loadProps(Git git, String path) throws IOException {
        Properties props = new Properties();
        File file = new File(git.getRepository().getWorkTree(), path);
        if (file.isFile()) {
            props.load(file);
        }
        return props;
    }

    private static String toString(Properties props) throws IOException {
        StringWriter sw = new StringWriter();
        props.save(sw);
        return sw.toString();
    }

    private static List<String> list(File dir) {
        List<String> files = new ArrayList<String>();
        String[] names = dir.list();
        if (names !=  null) {
            Collections.addAll(files, names);
        }
        return files;
    }

    private static void delete(File file) throws IOException {
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

    private static byte[] read(File file) throws IOException {
        if (!file.isFile()) {
            return null;
        }
        FileInputStream is = new FileInputStream(file);
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        try {
            Files.copy(is, os);
        } finally {
            Closeables.closeQuitely(is);
            Closeables.closeQuitely(os);
        }
        return os.toByteArray();
    }

    public long getPeriod() {
        return period;
    }

    public void setPeriod(long period) {
        this.period = period;
    }
}
