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
package io.fabric8.groups.internal;

import org.apache.curator.framework.CuratorFramework;
import io.fabric8.groups.Group;
import io.fabric8.groups.GroupFactory;
import io.fabric8.groups.NodeState;

/**
 */
public class ZooKeeperGroupFactory implements GroupFactory {

    private CuratorFramework curator;

    public ZooKeeperGroupFactory(CuratorFramework curator) {
        this.curator = curator;
    }

    @Override
    public <T extends NodeState> Group<T> createGroup(String path, Class<T> clazz) {
        return new ZooKeeperGroup<T>(curator, path, clazz);
    }

    @Override
    public <T extends NodeState> Group<T> createMultiGroup(String path, Class<T> clazz) {
        return new ZooKeeperMultiGroup<T>(curator, path, clazz);
    }
}
