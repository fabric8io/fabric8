package io.fabric8.git.internal;

import io.fabric8.utils.Strings;
import org.apache.curator.framework.CuratorFramework;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import io.fabric8.api.ContainerRegistration;
import io.fabric8.api.jcip.ThreadSafe;
import io.fabric8.api.scr.AbstractComponent;
import io.fabric8.api.scr.ValidatingReference;
import io.fabric8.git.GitNode;
import io.fabric8.git.GitService;
import io.fabric8.groups.Group;
import io.fabric8.groups.GroupListener;
import io.fabric8.groups.internal.ZooKeeperGroup;
import io.fabric8.utils.Closeables;
import io.fabric8.zookeeper.ZkPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;

import static io.fabric8.zookeeper.utils.ZooKeeperUtils.getSubstitutedData;

@ThreadSafe
@Component(name = "io.fabric8.git.master.listener", label = "Fabric8 Git Master Listener", immediate = true, metatype = false)
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
        if (isValid()) {
            switch (event) {
            case CONNECTED:
            case CHANGED:
                updateMasterUrl(group);
                break;
            }
        }
    }

    /**
     * Updates the git master url, if needed.
     */
    private void updateMasterUrl(Group<GitNode> group) {
        GitNode master = group.master();
        String masterUrl = master != null ? master.getUrl() : null;
        try {
            if (masterUrl != null) {
                GitService gitservice = gitService.get();
                String substitutedUrl = getSubstitutedData(curator.get(), masterUrl);
                if (!Strings.isNotBlank(substitutedUrl)) {
                    LOGGER.warn("Could not render git master URL {}.", masterUrl);
                }
                //Catch any possible issue indicating that the URL is invalid.
                URL url = new URL(substitutedUrl);
                gitservice.notifyRemoteChanged(substitutedUrl);
            }
        } catch (Exception e) {
            LOGGER.error("Failed to point origin to the new master.", e);
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
