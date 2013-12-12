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

import org.apache.curator.framework.CuratorFramework;
import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.apache.felix.gogo.commands.Option;

import static io.fabric8.zookeeper.utils.ZooKeeperUtils.getAllChildren;
import static io.fabric8.zookeeper.utils.ZooKeeperUtils.getChildren;

@Command(name = "list", scope = "zk", description = "List a znode's children", detailedDescription = "classpath:list.txt")
public class List extends ZooKeeperCommandSupport {

    @Argument(description = "Path of the znode to list")
    String path = "/";

    @Option(name = "-r", aliases = {"--recursive"}, description = "List children recursively")
    boolean recursive = false;

    @Option(name="-d", aliases={"--display"}, description="Display a znode's value if set")
    boolean display = false;

    //TODO - Be good to also have an option to show other ZK attributes for a node similar to ls -la

    @Override
    protected void doExecute(CuratorFramework zk) throws Exception {
        display(zk, path);
    }

    private java.util.List<String> getPaths(CuratorFramework zk) throws Exception {
        if (recursive) {
            return getAllChildren(zk, path);
        } else {
            return getChildren(zk, path);
        }
    }

    protected void display(CuratorFramework curator, String path) throws Exception {
        if (!path.endsWith("/")) {
            path = path + "/";
        }
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        java.util.List<String> paths = getPaths(curator);

        for(String p : paths) {
            if (display) {
                byte[] data = curator.getData().forPath(recursive ? p : path + p);
                if (data != null) {
                    System.out.printf("%s = %s\n", p, new String(data));
                } else {
                    System.out.println(p);
                }
            } else {
                System.out.println(p);
            }
        }
    }
}
