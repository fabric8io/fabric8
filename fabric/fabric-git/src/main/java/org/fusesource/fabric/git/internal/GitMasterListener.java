package org.fusesource.fabric.git.internal;

import org.apache.curator.framework.CuratorFramework;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.fusesource.fabric.api.ContainerRegistration;
import org.fusesource.fabric.git.GitNode;
import org.fusesource.fabric.git.GitService;
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

@Component(name = "org.fusesource.fabric.git.master.listener", description = "Fabric Git Master Listener", immediate = true)
public class GitMasterListener implements GroupListener<GitNode> {

    private static final Logger LOGGER = LoggerFactory.getLogger(GitMasterListener.class);

    @Reference
    private GitService gitService;
    @Reference
    private CuratorFramework curator;
    //Use this reference to make sure that we listener for master changes only after the container registration is done.
    @Reference
    private ContainerRegistration containerRegistration;

    private Group<GitNode> group;

    @Activate
    public void init() throws IOException {
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

    @Override
    public void groupEvent(Group<GitNode> group, GroupListener.GroupEvent event) {
        switch (event) {
            case CONNECTED:
            case CHANGED:
                updateMasterUrl(group);
                break;
        }
    }

    /**
     * Updates the git master url, if needed.
     *
     * @param group
     */
    private void updateMasterUrl(Group<GitNode> group) {
        GitNode master = group.master();
        String masterUrl = master != null ? master.getUrl() : null;
        try {
            if (masterUrl != null) {
                gitService.notifyRemoteChanged(getSubstitutedData(curator, masterUrl));
            }
        } catch (Exception e) {
            LOGGER.error("Failed to point origin to the new master.", e);
        }
    }
}
