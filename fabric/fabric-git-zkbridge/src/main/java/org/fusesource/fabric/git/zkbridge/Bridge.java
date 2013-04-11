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

import org.apache.felix.utils.properties.Properties;
import org.eclipse.jgit.api.FetchCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ListBranchCommand;
import org.eclipse.jgit.api.MergeResult;
import org.eclipse.jgit.api.ResetCommand;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.merge.MergeStrategy;
import org.eclipse.jgit.transport.RefSpec;
import org.fusesource.fabric.git.FabricGitService;
import org.fusesource.fabric.utils.Closeables;
import org.fusesource.fabric.utils.Files;
import org.fusesource.fabric.zookeeper.IZKClient;
import org.fusesource.fabric.zookeeper.ZkPath;
import org.fusesource.fabric.zookeeper.utils.ZooKeeperUtils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Bridge {

    private FabricGitService gitService;
    private IZKClient zookeeper;
    private long period;
    private ScheduledExecutorService executors;

    public void setGitService(FabricGitService gitService) {
        this.gitService = gitService;
    }

    public void setZookeeper(IZKClient zookeeper) {
        this.zookeeper = zookeeper;
    }

    public void setPeriod(long period) {
        this.period = period;
    }

    public void init() {
        executors = Executors.newSingleThreadScheduledExecutor();
        executors.scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                try {
                    update(gitService.get(), zookeeper);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }, period, period, TimeUnit.MILLISECONDS);
    }

    public void destroy() {
        executors.shutdown();
    }

    public static void update(Git git, IZKClient zookeeper) throws Exception {
        String remoteName = "origin";

        boolean remoteAvailable = false;
        try {
            FetchCommand fetch = git.fetch();
            fetch.setRemote(remoteName);
            fetch.call();
            remoteAvailable = true;
        } catch (Exception e) {
            // Ignore fetch exceptions
        }

        // ZooKeeper -> Git changes
        List<String> zkVersions = zookeeper.getChildren(ZkPath.CONFIG_VERSIONS.getPath());
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
                tmp = git.branchCreate().setName(version + "-tmp").call();
            }
            git.clean().setCleanDirectories(true).call();
            if (remote != null) {
                git.rebase().setUpstream(remote.getObjectId()).call();
            }
            git.checkout().setName(version + "-tmp").setForce(true).call();

            // Version metadata
            Properties versionProps = new Properties();
            String versionAttrs = zookeeper.getStringData(zkNode);
            if (versionAttrs != null) {
                versionProps.load(new StringReader(versionAttrs));
            }
            String gitCommit = versionProps.remove("git");
            if (gitCommit != null) {
                git.reset().setMode(ResetCommand.ResetType.HARD).setRef(gitCommit).call();
            }
            versionProps.save(new File(git.getRepository().getWorkTree(), ".metadata"));
            git.add().addFilepattern(".metadata").call();

            // Profiles
            List<String> existingProfiles = list(git.getRepository().getWorkTree());
            existingProfiles.remove(".git");
            existingProfiles.remove(".metadata");
            existingProfiles.remove("containers.properties");
            for (String profile : zookeeper.getChildren(zkNode + "/profiles")) {
                Properties profileProps = new Properties();
                String profileAttrs = zookeeper.getStringData(zkNode + "/profiles/" + profile);
                if (profileAttrs != null) {
                    profileProps.load(new StringReader(profileAttrs));
                }
                File profileDir = new File(git.getRepository().getWorkTree(), profile);
                profileDir.mkdirs();
                profileProps.save(new File(git.getRepository().getWorkTree(), profile + "/" + ".metadata"));
                git.add().addFilepattern(profile + "/" + ".metadata").call();
                List<String> files = list(profileDir);
                files.remove(".metadata");
                for (String file : zookeeper.getChildren(zkNode + "/profiles/" + profile)) {
                    byte[] data = zookeeper.getData(zkNode + "/profiles/" + profile + "/" + file);
                    Files.writeToFile(new File(git.getRepository().getWorkTree(), profile + "/" + file), data);
                    files.remove(file);
                    git.add().addFilepattern(profile + "/" + file).call();
                }
                for (String file : files) {
                    new File(profileDir, file).delete();
                    git.rm().addFilepattern(profile + "/" + file).call();
                }
                existingProfiles.remove(profile);
            }
            for (String profile : existingProfiles) {
                delete(new File(git.getRepository().getWorkTree(), profile));
                git.rm().addFilepattern(profile).call();
            }

            Properties containerProps = new Properties();
            for (String container : zookeeper.getChildren(zkNode + "/containers")) {
                String str = zookeeper.getStringData(zkNode + "/containers/" + container);
                if (str != null) {
                    containerProps.setProperty(container, str);
                }
            }
            containerProps.save(new File(git.getRepository().getWorkTree(), "containers.properties"));
            git.add().addFilepattern("containers.properties").call();

            ObjectId rev = git.getRepository().getRef("HEAD").getObjectId();
            boolean nochange = git.status().call().isClean();
            if (!nochange) {
                rev = git.commit().setMessage("Merge zookeeper update").call().getId();
            }
            git.checkout().setName(version).setForce(true).call();
            if (!nochange) {
                MergeResult result = git.merge().setStrategy(MergeStrategy.OURS).include(rev).call();
                rev = result.getNewHead();
            }
            if (remoteAvailable) {
                git.push().setRefSpecs(new RefSpec(version)).call();
            }

            // Apply changes to zookeeper
            List<String> profiles = list(git.getRepository().getWorkTree());
            profiles.remove(".git");
            profiles.remove(".metadata");
            profiles.remove("containers.properties");
            byte[] versionMetadata = read(new File(git.getRepository().getWorkTree(), ".metadata"));
            ZooKeeperUtils.set(zookeeper, zkNode, versionMetadata);
            existingProfiles = zookeeper.getChildren(zkNode + "/profiles");
            for (String profile : profiles) {
                byte[] profileMetadata = read(new File(git.getRepository().getWorkTree(), profile + "/" + ".metadata"));
                ZooKeeperUtils.set(zookeeper, zkNode + "/profiles/" + profile, profileMetadata);
                List<String> nodes = zookeeper.getChildren(zkNode + "/profiles/" + profile);
                List<String> files = list(new File(git.getRepository().getWorkTree(), profile));
                files.remove(".metadata");
                for (String file : files) {
                    byte[] data = read(new File(git.getRepository().getWorkTree(), profile + "/" + file));
                    ZooKeeperUtils.set(zookeeper, zkNode + "/profiles/" + profile + "/" + file, data);
                    nodes.remove(file);
                }
                for (String file : nodes) {
                    zookeeper.delete(zkNode + "/profiles/" + profile + "/" + file);
                }
                existingProfiles.remove(profile);
            }
            for (String profile : existingProfiles) {
                ZooKeeperUtils.deleteSafe(zookeeper, zkNode + "/profiles/" + profile);
            }

            containerProps.clear();
            if (new File(git.getRepository().getWorkTree(), "containers.properties").isFile()) {
                containerProps.load(new File(git.getRepository().getWorkTree(), "containers.properties"));
            }
            for (String container : containerProps.keySet()) {
                ZooKeeperUtils.set(zookeeper, zkNode + "/containers/" + container, containerProps.getProperty(container));
            }
            for (String container : zookeeper.getChildren(zkNode + "/containers")) {
                if (!containerProps.containsKey(container)) {
                    ZooKeeperUtils.deleteSafe(zookeeper, zkNode + "/containers/" + container);
                }
            }

            versionProps.put("git", rev.name());
            StringWriter sw = new StringWriter();
            versionProps.save(sw);
            zookeeper.setData(zkNode, sw.toString());
        }
        // TODO new git versions and deleted zk versions
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
}
