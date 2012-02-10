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
package org.fusesource.fabric.zookeeper.commands;

import java.net.URL;
import java.util.List;

import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.apache.felix.gogo.commands.Option;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.data.ACL;
import org.linkedin.zookeeper.client.IZKClient;

@Command(name = "create", scope = "zk", description = "Create a node")
public class Create extends ZooKeeperCommandSupport {

    @Option(name = "-e", aliases = {"--ephemeral"}, description = "Create an ephemeral node")
    boolean ephemeral;

    @Option(name = "-s", aliases = {"--sequential"}, description = "Create a sequential node")
    boolean sequential;

    @Option(name = "-r", aliases = {"--recursive"}, description = "Automatically create parents")
    boolean recursive;

    @Option(name = "-i", aliases = {"--import"}, description = "Import data from an url")
    boolean importUrl;

    @Option(name = "-a", aliases = {"--acl"}, description = "Node ACLs")
    String acl;

    @Option(name = "-o", aliases = {"--overwrite"}, description = "Overwrite existing entry if it already exists")
    boolean overwrite;

    @Argument(index = 0, required = true, description = "Path of the node to create")
    String path;

    @Argument(index = 1, required = false, description = "Data for the node, or url if 'import' option is used")
    String data;

    @Override
    protected void doExecute(IZKClient zk) throws Exception {
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
                zk.createWithParents(path, nodeData, acls, mode);
            } else {
                zk.create(path, nodeData, acls, mode);
            }
        } catch (KeeperException.NodeExistsException e) {
            if (overwrite) {
                zk.setData(path, nodeData);
            } else {
                throw e;
            }
        }
    }
}
