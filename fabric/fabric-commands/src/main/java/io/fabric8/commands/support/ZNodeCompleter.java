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

import java.util.List;

import org.apache.curator.framework.CuratorFramework;
import org.apache.karaf.shell.console.Completer;
import static io.fabric8.zookeeper.utils.ZooKeeperUtils.getChildren;

public class ZNodeCompleter implements Completer {
    private CuratorFramework curator;

    public ZNodeCompleter() {
    }

    public void setCurator(CuratorFramework curator) {
        this.curator = curator;
    }

    @SuppressWarnings("unchecked")
    public int complete(String buffer, int cursor, List candidates) {
        try {
            if (curator.getZookeeperClient().isConnected()) {
                // Guarantee that the final token is the one we're expanding
                if (buffer == null) {
                    candidates.add("/");
                    return 1;
                } else if (!buffer.startsWith("/")) {
                    return 0;
                }
                buffer = buffer.substring(0, cursor);
                String path = buffer;
                int idx = path.lastIndexOf("/") + 1;
                String prefix = path.substring(idx);
                // Only the root path can end in a /, so strip it off every other prefix
                String dir = idx == 1 ? "/" : path.substring(0, idx - 1);
                List<String> children = getChildren(curator, dir);
                for (String child : children) {
                    if (child.startsWith(prefix)) {
                        candidates.add(child);
                    }
                }
                return candidates.size() == 0 ? buffer.length() : buffer.lastIndexOf("/") + 1;
            }
        } catch (Exception e) {
            // Ignore
        }
        return 0;
    }
}
