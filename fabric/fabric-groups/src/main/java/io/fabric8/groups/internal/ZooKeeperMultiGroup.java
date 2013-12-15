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
package io.fabric8.groups.internal;

import org.apache.curator.framework.CuratorFramework;
import io.fabric8.groups.MultiGroup;
import io.fabric8.groups.NodeState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadFactory;

public class ZooKeeperMultiGroup<T extends NodeState> extends ZooKeeperGroup<T> implements MultiGroup<T> {
    private final Logger log = LoggerFactory.getLogger(getClass());

    public ZooKeeperMultiGroup(CuratorFramework client, String path, Class<T> clazz) {
        super(client, path, clazz);
    }

    public ZooKeeperMultiGroup(CuratorFramework client, String path, Class<T> clazz, ExecutorService executorService) {
        super(client, path, clazz, executorService);
    }

    public ZooKeeperMultiGroup(CuratorFramework client, String path, Class<T> clazz, ThreadFactory threadFactory) {
        super(client, path, clazz, threadFactory);
    }

    @Override
    public boolean isMaster(String name) {
        List<ChildData<T>> children = new ArrayList<ChildData<T>>(currentData.values());
        Collections.sort(children, sequenceComparator);
        for (ChildData child : children) {
            NodeState node = (NodeState) child.getNode();
            if (node.id.equals(name)) {
                if (child.getPath().equals(getId())) {
                    return true;
                } else {
                    return false;
                }
            }
        }
        return false;
    }
}
