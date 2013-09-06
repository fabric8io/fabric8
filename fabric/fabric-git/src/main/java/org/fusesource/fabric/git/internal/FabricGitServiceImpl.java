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
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.eclipse.jgit.api.Git;
import org.fusesource.fabric.api.ContainerRegistration;
import org.fusesource.fabric.git.FabricGitService;
import org.fusesource.fabric.git.GitNode;
import org.fusesource.fabric.groups.Group;
import org.fusesource.fabric.groups.GroupListener;
import org.fusesource.fabric.groups.internal.ZooKeeperGroup;
import org.fusesource.fabric.utils.Closeables;
import org.fusesource.fabric.utils.SystemProperties;
import org.fusesource.fabric.zookeeper.ZkPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import static org.fusesource.fabric.zookeeper.utils.ZooKeeperUtils.getSubstitutedData;

@Component(name = "org.fusesource.fabric.git.service", description = "Fabric Git Service", immediate = true)
@Service(FabricGitService.class)
public class FabricGitServiceImpl implements FabricGitService, GroupListener<GitNode> {

    private static final Logger LOGGER = LoggerFactory.getLogger(FabricGitServiceImpl.class);

    @Reference
    private CuratorFramework curator;

    @Reference
    private GitService gitService;

    //We need to wait for the container registration, so that substitution of master url works.
    @Reference
    private ContainerRegistration containerRegistration;


    private Group<GitNode> group;

    @Activate
    public void init() {
        group = new ZooKeeperGroup<GitNode>(curator, ZkPath.GIT.getPath(), GitNode.class);
        group.add(this);
        group.start();
    }

    @Deactivate
    public void destroy() {
        group.remove(this);
        Closeables.closeQuitely(group);
        group = null;
    }

    public Git get() throws IOException {
        return gitService.get();
    }

    @Override
    public void groupEvent(Group<GitNode> group, GroupEvent event) {
        switch (event) {
            case CONNECTED:
            case CHANGED:
                updateMasterUrl(group);
                break;
            case DISCONNECTED:
                fireRemoteChangedEvent(null);
        }
    }

    /**
     * Updates the git master url, if needed.
     *
     * @param group
     */
    private void updateMasterUrl(Group<GitNode> group) {
        String masterUrl = null;
        GitNode master = group.master();
        if (master != null
                && !master.getContainer().equals(System.getProperty(SystemProperties.KARAF_NAME))) {
            masterUrl = master.getUrl();
        }
        try {
            if (masterUrl != null) {
                fireRemoteChangedEvent(getSubstitutedData(curator, masterUrl));
            } else {
                fireRemoteChangedEvent(null);
            }
        } catch (Exception e) {
            LOGGER.error("Failed to point origin to the new master.", e);
        }
    }

    private void fireRemoteChangedEvent(String masterUrl) {
        if (gitService != null) {
            gitService.onRemoteChanged(masterUrl);
        }
    }
}
