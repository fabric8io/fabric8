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
package io.fabric8.zookeeper.commands;

import java.net.URL;
import java.util.List;

import org.apache.curator.framework.CuratorFramework;
import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.apache.felix.gogo.commands.Option;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.data.ACL;

@Command(name = "create", scope = "zk", description = "Create a znode", detailedDescription = "classpath:create.txt")
public class Create extends ZooKeeperCommandSupport {

    @Option(name = "-e", aliases = {"--ephemeral"}, description = "Make the new znode epehemeral, so that it is automatically deleted after the current ZooKeeper client session closes.")
    boolean ephemeral;

    @Option(name = "-s", aliases = {"--sequential"}, description = "Make the new znode sequential, so that a unique 10-digit suffix is appended to the znode name.")
    boolean sequential;

    @Option(name = "-r", aliases = {"--recursive"}, description = "Automatically create any missing parent znodes in the specified path.")
    boolean recursive;

    @Option(name = "-i", aliases = {"--import"}, description = "Interpret the data argument as a URL that locates a resource containing the initial data for the new znode.")
    boolean importUrl;

    @Option(name = "-a", aliases = {"--acl"}, description = "Specifies the znode's ACL as a comma-separated list, where each entry in the list has the format, <Scheme>:<ID>:<Permissions>. The <Permissions> string consists of the following characters, concatenated in any order: r (read), w (write), c (create), d (delete), and a (admin).")
    String acl;

    @Option(name = "-o", aliases = {"--overwrite"}, description = "Overwrite the existing znode at this location, if there is one.")
    boolean overwrite;

    @Argument(index = 0, required = true, description = "Path of the node to create")
    String path;

    @Argument(index = 1, required = false, description = "Initial data for the node or, if --import is specified, a URL pointing at a location that contains the initial data.")
    String data;

    @Override
    protected void doExecute(CuratorFramework curator) throws Exception {
        List<ACL> acls = acl == null ? ZooDefs.Ids.OPEN_ACL_UNSAFE : parseACLs(acl);
        CreateMode mode;
        if (ephemeral && sequential) {
            mode = CreateMode.EPHEMERAL_SEQUENTIAL;
        } else if (ephemeral) {
            mode = CreateMode.EPHEMERAL;
        } else if (sequential) {
            mode = CreateMode.PERSISTENT_SEQUENTIAL;
        } else {
            mode = CreateMode.PERSISTENT;
        }

        String nodeData = data;

        if (importUrl) {
            nodeData = loadUrl(new URL(data));
        }

        try {
            if (recursive) {
                curator.create().creatingParentsIfNeeded().withMode(mode).withACL(acls).forPath(path, nodeData.getBytes());
            } else {
                curator.create().withMode(mode).withACL(acls).forPath(path, nodeData.getBytes());
            }
        } catch (KeeperException.NodeExistsException e) {
            if (overwrite) {
                curator.setData().forPath(path, nodeData.getBytes());
            } else {
                throw e;
            }
        }
    }
}
