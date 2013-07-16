/*
 * Copyright (C) FuseSource, Inc.
 *   http://fusesource.com
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package org.fusesource.fabric.git.internal;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.state.ConnectionState;
import org.apache.curator.framework.state.ConnectionStateListener;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.errors.RepositoryNotFoundException;
import org.eclipse.jgit.lib.StoredConfig;
import org.fusesource.fabric.git.FabricGitService;
import org.fusesource.fabric.git.GitNode;
import org.fusesource.fabric.groups.GroupListener;
import org.fusesource.fabric.groups.Group;
import org.fusesource.fabric.groups.internal.ZooKeeperGroup;
import org.fusesource.fabric.utils.Closeables;
import org.fusesource.fabric.zookeeper.ZkPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

import static org.fusesource.fabric.zookeeper.utils.ZooKeeperUtils.getSubstitutedData;

public class FabricGitServiceImpl implements FabricGitService, ConnectionStateListener, GroupListener<GitNode> {

    public static final String DEFAULT_LOCAL_LOCATION = System.getProperty("karaf.data") + File.separator + "git" + File.separator + "fabric";

    private static final Logger LOGGER = LoggerFactory.getLogger(FabricGitServiceImpl.class);

	private Group<GitNode> group;
	private CuratorFramework curator;

    public void init() {
    }

    public void destroy() {
        onDisconnected();
    }

	@Override
	public Git get() throws IOException {
		File localRepo = new File(DEFAULT_LOCAL_LOCATION);
		if (!localRepo.exists() && !localRepo.mkdirs()) {
			throw new IOException("Failed to create local repository");
		}
		try {
			return Git.open(localRepo);
		} catch (RepositoryNotFoundException e) {
			try {
				Git git = Git.init().setDirectory(localRepo).call();
				git.commit().setMessage("First Commit").setCommitter("fabric", "user@fabric").call();
				return git;
			} catch (GitAPIException ex) {
				throw new IOException(ex);
			}
		}
	}


    @Override
    public void groupEvent(Group<GitNode> group, GroupEvent event) {
        String masterUrl = null;
		GitNode master = group.master();
		if (master != null
                && !master.getContainer().equals(System.getProperty("karaf.name"))) {
            masterUrl = master.getUrl();
        }
		try {
			StoredConfig config = get().getRepository().getConfig();
            if (masterUrl != null) {
                config.setString("remote", "origin", "url", getSubstitutedData(curator, masterUrl));
                config.setString("remote", "origin", "fetch", "+refs/heads/*:refs/remotes/origin/*");
            } else {
                config.unsetSection("remote", "origin");
            }
			config.save();
		} catch (Exception e) {
			LOGGER.error("Failed to point origin to the new master.", e);
		}
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

	public void onConnected() {
		group = new ZooKeeperGroup<GitNode>(curator, ZkPath.GIT.getPath(), GitNode.class);
		group.add(this);
        group.start();
	}

    public void onDisconnected() {
        Closeables.closeQuitely(group);
        group = null;
    }
}
