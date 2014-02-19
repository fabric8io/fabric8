package io.fabric8.itests.basic.git;


import static io.fabric8.zookeeper.utils.ZooKeeperUtils.getSubstitutedData;
import io.fabric8.api.ContainerRegistration;
import io.fabric8.api.ServiceLocator;
import io.fabric8.git.GitNode;
import io.fabric8.groups.Group;
import io.fabric8.groups.GroupListener;
import io.fabric8.groups.internal.ZooKeeperGroup;
import io.fabric8.zookeeper.ZkPath;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.curator.framework.CuratorFramework;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.eclipse.jgit.api.CreateBranchCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ListBranchCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.osgi.framework.BundleContext;

import com.google.common.base.Objects;

public class GitUtils {

    public static final CredentialsProvider DEFAULT_CREDENTIALS_PROVIDER = new UsernamePasswordCredentialsProvider("admin", "admin");


    /**
     * Waits until the master url becomes available & returns it.
     */
    public static String getMasterUrl(BundleContext bundleContext, CuratorFramework curator) throws InterruptedException, URISyntaxException {
        ServiceLocator.awaitService(bundleContext, ContainerRegistration.class);
        Group<GitNode> group = new ZooKeeperGroup<GitNode>(curator, ZkPath.GIT.getPath(), GitNode.class);
        final CountDownLatch latch = new CountDownLatch(1);

        group.add(new GroupListener<GitNode>() {
            @Override
            public void groupEvent(Group<GitNode> group, GroupEvent event) {
                if (group.master() != null && group.master().getUrl() != null) {
                    latch.countDown();
                }
            }
        });
        group.start();
        latch.await(10, TimeUnit.SECONDS);
        return getSubstitutedData(curator, group.master().getUrl());
    }


    /**
     * Wait until the version znode gets updated (indicating that entries has been bridge from/to git).
     * @param curator       The {@link CuratorFramework} instance to use for looking up the registry.
     * @param branch        The name of the branch/version.
     * @throws Exception
     */
    public  static void waitForBranchUpdate(CuratorFramework curator, String branch) throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);
        final Watcher watcher = new Watcher() {
            @Override
            public void process(WatchedEvent watchedEvent) {
                latch.countDown();
            }
        };

        for (int i = 0;
             curator.checkExists().usingWatcher(watcher).forPath(ZkPath.CONFIG_VERSION.getPath(branch)) == null && i < 3;
             i++) {
            Thread.sleep(1000);
        }

        latch.await(10, TimeUnit.SECONDS);
    }

    public static void configureBranch(Git git, String remote, String remoteUrl, String branch) {
        if (git != null && remoteUrl != null && !remoteUrl.isEmpty()) {
            Repository repository = git.getRepository();
            if (repository != null) {
                StoredConfig config = repository.getConfig();
                config.setString("remote", remote, "url", remoteUrl);
                config.setString("remote", remote, "fetch", "+refs/heads/*:refs/remotes/" + branch + "/*");
                config.setString("branch", branch, "merge", "refs/heads/" + branch);
                config.setString("branch", branch, "remote", "origin");
                try {
                    config.save();
                } catch (IOException e) {
                    //Ignore
                }
            }
        }
    }

    /**
     * Returns the name of the current branch.
     * @param git
     * @return
     */
    public static String currentBranch(Git git) {
        try {
            return git.getRepository().getBranch();
        } catch (IOException e) {
            return null;
        }
    }

    public static void checkoutBranch(Git git,   String remote, String branch) throws GitAPIException {
        checkoutBranch(git, DEFAULT_CREDENTIALS_PROVIDER, remote, branch);
    }

    public static void checkoutBranch(Git git, CredentialsProvider credentialsProvider,  String remote, String branch) throws GitAPIException {
        String current = currentBranch(git);
        if (Objects.equal(current, branch)) {
            return;
        }
        boolean localExists = localBranchExists(git, branch);
        boolean remoteExists = remoteBranchExists(git, remote, branch);
        if (localExists) {
            git.checkout().setName(branch).call();
        } else if (remoteExists) {
            git.checkout().setName(branch).setCreateBranch(true).setForce(true).
                    setUpstreamMode(CreateBranchCommand.SetupUpstreamMode.TRACK).
                    setStartPoint(remote + "/" + branch).call();
        } else {
            git.branchCreate().setName(branch).setForce(true).call();
            git.checkout().setName(branch).call();
            git.push().setCredentialsProvider(credentialsProvider).setRemote(remote).setRefSpecs(new RefSpec(branch)).setForce(true).call();
        }
        configureBranch(git, remote, getRemote(git, remote), branch);
    }

    public static String getRemote(Git git, String remote) throws GitAPIException {
        StoredConfig config = git.getRepository().getConfig();
        return config.getString("remote", remote, "url");
    }

    /**
     * Checks if a local branch exists.
     * @param git       The git object to use.
     * @param branch    The name of the local branch.
     * @return
     * @throws GitAPIException
     */
    public static boolean localBranchExists(Git git, String branch) throws GitAPIException {
        List<Ref> list = git.branchList().call();
        String fullName = "refs/heads/" + branch;
        boolean exists = false;
        for (Ref ref : list) {
            String name = ref.getName();
            if (Objects.equal(name, fullName)) {
                exists = true;
            }
        }
        return exists;
    }

    /**
     * Checks if a remote branch exists.
     * @param git       The git object to use.
     * @param remote    The name of the remote repo to check.
     * @param branch    The name of the local branch.
     * @return
     * @throws GitAPIException
     */
    public static boolean remoteBranchExists(Git git, String remote, String branch) throws GitAPIException {
        List<Ref> list = git.branchList().setListMode(ListBranchCommand.ListMode.REMOTE).call();
        String fullName = "refs/remotes/" + remote + "/" + branch;
        boolean exists = false;
        for (Ref ref : list) {
            String name = ref.getName();
            if (Objects.equal(name, fullName)) {
                exists = true;
            }
        }
        return exists;
    }
}
