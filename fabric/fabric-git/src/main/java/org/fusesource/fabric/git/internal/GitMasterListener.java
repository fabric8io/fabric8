package org.fusesource.fabric.git.internal;

import org.apache.curator.framework.CuratorFramework;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.fusesource.fabric.api.ContainerRegistration;
import org.fusesource.fabric.api.jcip.ThreadSafe;
import org.fusesource.fabric.api.scr.AbstractComponent;
import org.fusesource.fabric.api.scr.ValidatingReference;
import org.fusesource.fabric.git.GitNode;
import org.fusesource.fabric.git.GitService;
import org.fusesource.fabric.groups.Group;
import org.fusesource.fabric.groups.GroupListener;
import org.fusesource.fabric.groups.internal.ZooKeeperGroup;
import org.fusesource.fabric.utils.Closeables;
import org.fusesource.fabric.zookeeper.ZkPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import static org.fusesource.fabric.zookeeper.utils.ZooKeeperUtils.getSubstitutedData;

@ThreadSafe
@Component(name = "org.fusesource.fabric.git.master.listener", description = "Fabric Git Master Listener", immediate = true)
public final class GitMasterListener extends AbstractComponent implements GroupListener<GitNode> {

    private static final Logger LOGGER = LoggerFactory.getLogger(GitMasterListener.class);

    @Reference(referenceInterface = GitService.class)
    private final ValidatingReference<GitService> gitService = new ValidatingReference<GitService>();
    @Reference(referenceInterface = CuratorFramework.class)
    private final ValidatingReference<CuratorFramework> curator = new ValidatingReference<CuratorFramework>();
    //Use this reference to make sure that we listen for master changes only after the container registration is done.
    @Reference(referenceInterface = ContainerRegistration.class)
    private final ValidatingReference<ContainerRegistration> containerRegistration = new ValidatingReference<ContainerRegistration>();

    private Group<GitNode> group;

    @Activate
    void activate() throws IOException {
        activateComponent();
        group = new ZooKeeperGroup<GitNode>(curator.get(), ZkPath.GIT.getPath(), GitNode.class);
        group.add(this);
        group.start();
    }

    @Deactivate
    void deactivate() {
        deactivateComponent();
        group.remove(this);
        Closeables.closeQuitely(group);
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
     */
    private void updateMasterUrl(Group<GitNode> group) {
        if (isValid()) {
            GitNode master = group.master();
            String masterUrl = master != null ? master.getUrl() : null;
            try {
                if (masterUrl != null) {
                    GitService gitservice = gitService.get();
                    gitservice.notifyRemoteChanged(getSubstitutedData(curator.get(), masterUrl));
                }
            } catch (Exception e) {
                LOGGER.error("Failed to point origin to the new master.", e);
            }
        }
    }

    void bindCurator(CuratorFramework curator) {
        this.curator.bind(curator);
    }

    void unbindCurator(CuratorFramework curator) {
        this.curator.unbind(curator);
    }

    void bindGitService(GitService service) {
        this.gitService.bind(service);
    }

    void unbindGitService(GitService service) {
        this.gitService.unbind(service);
    }

    void bindContainerRegistration(ContainerRegistration service) {
        this.containerRegistration.bind(service);
    }

    void unbindContainerRegistration(ContainerRegistration service) {
        this.containerRegistration.unbind(service);
    }
}
