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
package io.fabric8.git.zkbridge;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.RetryOneTime;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.StoredConfig;
import io.fabric8.utils.Files;
import io.fabric8.zookeeper.ZkPath;
import io.fabric8.zookeeper.spring.ZKServerFactoryBean;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;

import static io.fabric8.zookeeper.utils.ZooKeeperUtils.deleteSafe;
import static io.fabric8.zookeeper.utils.ZooKeeperUtils.setData;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BridgeTest {

    private ZKServerFactoryBean sfb;
    private CuratorFramework curator;
    private Git git;
    private Git remote;

    @Before
    public void setUp() throws Exception {
        sfb = new ZKServerFactoryBean();
        delete(sfb.getDataDir());
        delete(sfb.getDataLogDir());
        sfb.afterPropertiesSet();

        CuratorFrameworkFactory.Builder builder = CuratorFrameworkFactory.builder()
                .connectString("localhost:" + sfb.getClientPortAddress().getPort())
                .retryPolicy(new RetryOneTime(1000))
                .connectionTimeoutMs(360000);

        curator = builder.build();
        curator.start();
        curator.getZookeeperClient().blockUntilConnectedOrTimedOut();

        File root =  new File(System.getProperty("basedir", ".") + "/target/git").getCanonicalFile();
        delete(root);

        new File(root, "remote").mkdirs();
        remote = Git.init().setDirectory(new File(root, "remote")).call();
        remote.commit().setMessage("First Commit").setCommitter("fabric", "user@fabric").call();

        new File(root, "local").mkdirs();
        git = Git.init().setDirectory(new File(root, "local")).call();
        git.commit().setMessage("First Commit").setCommitter("fabric", "user@fabric").call();
        StoredConfig config = git.getRepository().getConfig();
        config.setString("remote", "origin", "url", "file://" + new File(root, "remote").getCanonicalPath());
        config.setString("remote", "origin", "fetch", "+refs/heads/*:refs/remotes/origin/*");
        config.save();

    }

    @After
    public void tearDown() throws Exception {
        sfb.destroy();
    }

    @Test
    public void testNoLocalNorRemoteBranch() throws Exception {
        deleteSafe(curator, ZkPath.CONFIG_VERSIONS.getPath());
        setData(curator, ZkPath.CONFIG_VERSION.getPath("1.0"), "description = default version\n");
        setData(curator, ZkPath.CONFIG_VERSIONS_PROFILE.getPath("1.0", "p1") + "/thepid.properties", "foo = bar\n");
        setData(curator, ZkPath.CONFIG_VERSIONS_PROFILE.getPath("1.0", "p1") + "/thexml.xml", "<hello/>\n");
        setData(curator, ZkPath.CONFIG_VERSIONS_CONTAINER.getPath("1.0", "root"), "p1");


        ObjectId rev1 = git.getRepository().getRef("HEAD").getObjectId();
        Bridge.update(git, curator);
        ObjectId rev2 = git.getRepository().getRef("HEAD").getObjectId();
        Bridge.update(git, curator);
        ObjectId rev3 = git.getRepository().getRef("HEAD").getObjectId();

        assertFalse(rev1.equals(rev2));
        assertTrue(rev2.equals(rev3));

        remote.checkout().setName("1.0").call();
        File tree = remote.getRepository().getWorkTree();
        Files.writeToFile(new File(tree, "p2/.metadata"), "\n", Charset.forName("UTF-8"));
        Files.writeToFile(new File(tree, "p2/anotherpid.properties"), "key = value\n", Charset.forName("UTF-8"));
        remote.add().addFilepattern(".").call();
        remote.commit().setMessage("Add p2 profile").call();

        setData(curator, ZkPath.CONFIG_VERSIONS_PROFILE.getPath("1.0", "p3") + "/thepid.properties", "foo = bar\n");

        rev1 = git.getRepository().getRef("HEAD").getObjectId();
        Bridge.update(git, curator);
        rev2 = git.getRepository().getRef("HEAD").getObjectId();
        Bridge.update(git, curator);
        rev3 = git.getRepository().getRef("HEAD").getObjectId();

        assertFalse(rev1.equals(rev2));
        assertTrue(rev2.equals(rev3));
    }

    private void delete(File file) throws IOException {
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

}
