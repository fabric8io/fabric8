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

import org.apache.curator.framework.CuratorFramework;
import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;

import static io.fabric8.zookeeper.utils.ZooKeeperUtils.getStringData;

@Command(name = "get", scope = "zk", description = "Get a znode's data", detailedDescription = "classpath:get.txt")
public class GetAction extends ZooKeeperCommandSupport {

    @Argument(description = "Path of the znode to get")
    String path;

    public GetAction(CuratorFramework curator) {
        setCurator(curator);
    }

    @Override
    protected void doExecute(CuratorFramework curator) throws Exception {
        System.out.println(getStringData(curator, path));
    }
}
