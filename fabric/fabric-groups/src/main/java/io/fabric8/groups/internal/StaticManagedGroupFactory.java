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
import io.fabric8.groups.Group;
import io.fabric8.groups.NodeState;

/**
 *
 */
public class StaticManagedGroupFactory implements ManagedGroupFactory {

    private final CuratorFramework curator;
    private final boolean shouldClose;

    StaticManagedGroupFactory(CuratorFramework curator, boolean shouldClose) {
        this.curator = curator;
        this.shouldClose = shouldClose;
    }

    @Override
    public CuratorFramework getCurator() {
        return curator;
    }

    @Override
    public <T extends NodeState> Group<T> createGroup(String path, Class<T> clazz) {
        return new ZooKeeperGroup<T>(curator, path, clazz);
    }

    @Override
    public <T extends NodeState> Group<T> createMultiGroup(String path, Class<T> clazz) {
        return new ZooKeeperMultiGroup<T>(curator, path, clazz);
    }

    @Override
    public void close() {
        if (shouldClose) {
            curator.close();
        }
    }
}
