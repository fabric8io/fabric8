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
package io.fabric8.commands.support;

import org.apache.curator.framework.CuratorFramework;
import org.apache.karaf.shell.console.Completer;
import org.apache.karaf.shell.console.completer.StringsCompleter;

import java.util.List;

import static io.fabric8.zookeeper.utils.ZooKeeperUtils.getChildren;

public class ClusterCompleter implements Completer {

    private static final String CLUSTER_PATH = "/fabric/registry/clusters";

    private CuratorFramework curator;

    public ClusterCompleter() {
    }

    public void setCurator(CuratorFramework curator) {
        this.curator = curator;
    }

    @SuppressWarnings("unchecked")
    public int complete(String buffer, int cursor, List candidates) {

        StringsCompleter delegate = new StringsCompleter();
        try {
            if (curator.getZookeeperClient().isConnected()) {
                delegate.getStrings().addAll(getChildren(curator, CLUSTER_PATH));
            }
        } catch (Exception ex) {
            //ignore
        }
        return delegate.complete(buffer, cursor, candidates);
    }
}
