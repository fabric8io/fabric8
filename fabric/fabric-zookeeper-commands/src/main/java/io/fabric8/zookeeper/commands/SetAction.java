/**
 *  Copyright 2005-2014 Red Hat, Inc.
 *
 *  Red Hat licenses this file to you under the Apache License, version
 *  2.0 (the "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 *  implied.  See the License for the specific language governing
 *  permissions and limitations under the License.
 */
package io.fabric8.zookeeper.commands;

import java.net.URL;

import org.apache.curator.framework.CuratorFramework;
import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.apache.felix.gogo.commands.Option;

@Command(name = "set", scope = "zk", description = "Set a znode's data", detailedDescription = "classpath:set.txt")
public class SetAction extends ZooKeeperCommandSupport {

    @Option(name = "-i", aliases = {"--import"}, description = "Import data from a URL")
    boolean importUrl;

    @Argument(description = "Path of the znode to set", index = 0)
    String path;

    @Argument(description = "The new data or URL, if the '--import' option is specified", index = 1)
    String data;

    SetAction(CuratorFramework curator) {
        setCurator(curator);
    }

    @Override
    protected void doExecute(CuratorFramework curator) throws Exception {

        String nodeData = data;

        if (importUrl) {
            nodeData = loadUrl(new URL(data));
        }
        
        curator.setData().forPath(path, nodeData.getBytes());
    }
}
