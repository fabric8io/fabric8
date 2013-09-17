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
import org.fusesource.fabric.service.support.AbstractComponent;
import org.fusesource.fabric.service.support.ValidatingReference;
import org.fusesource.fabric.utils.Closeables;
import org.fusesource.fabric.utils.SystemProperties;
import org.fusesource.fabric.zookeeper.ZkPath;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import static org.fusesource.fabric.zookeeper.utils.ZooKeeperUtils.getSubstitutedData;

@Component(name = "org.fusesource.fabric.git.service", description = "Fabric Git Service", immediate = true)
@Service(FabricGitService.class)
public class FabricGitServiceImpl extends AbstractComponent implements FabricGitService, GroupListener<GitNode> {

    private static final Logger LOGGER = LoggerFactory.getLogger(FabricGitServiceImpl.class);

    @Reference(referenceInterface = CuratorFramework.class)
    private final ValidatingReference<CuratorFramework> curator = new ValidatingReference<CuratorFramework>();
    @Reference(referenceInterface = GitService.class)
    private final ValidatingReference<GitService> gitService = new ValidatingReference<GitService>();
    @Reference(referenceInterface = ContainerRegistration.class)
    private final ValidatingReference<ContainerRegistration> registration = new ValidatingReference<ContainerRegistration>();

    private Group<GitNode> group;

    @Activate
    synchronized void activate(ComponentContext context) {
        activateComponent(context);
        try {
            group = new ZooKeeperGroup<GitNode>(curator.get(), ZkPath.GIT.getPath(), GitNode.class);
            group.add(this);
            group.start();
        } catch (RuntimeException rte) {
            deactivateComponent();
            throw rte;
        }
    }

    @Deactivate
    synchronized void deactivate() {
        try {
            group.remove(this);
            Closeables.closeQuitely(group);
            group = null;
        } finally {
            deactivateComponent();
        }
    }

    public Git get() throws IOException {
        return gitService.get().get();
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
                fireRemoteChangedEvent(getSubstitutedData(curator.get(), masterUrl));
            } else {
                fireRemoteChangedEvent(null);
            }
        } catch (Exception e) {
            LOGGER.error("Failed to point origin to the new master.", e);
        }
    }

    private void fireRemoteChangedEvent(String masterUrl) {
        gitService.get().onRemoteChanged(masterUrl);
    }

    void bindCurator(CuratorFramework curator) {
        this.curator.set(curator);
    }

    void unbindCurator(CuratorFramework curator) {
        this.curator.set(null);
    }

    void bindRegistration(ContainerRegistration service) {
        this.registration.set(service);
    }

    void unbindRegistration(ContainerRegistration service) {
        this.registration.set(null);
    }

    void bindGitService(GitService service) {
        this.gitService.set(service);
    }

    void unbindGitService(GitService service) {
        this.gitService.set(null);
    }
}
