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

import io.fabric8.groups.Group;
import io.fabric8.groups.MultiGroup;
import io.fabric8.groups.NodeState;
import org.apache.curator.framework.CuratorFramework;

public class DelegateZooKeeperMultiGroup<T extends NodeState> extends DelegateZooKeeperGroup<T> implements MultiGroup<T> {

    public DelegateZooKeeperMultiGroup(String path, Class<T> clazz) {
        super(path, clazz);
    }

    protected Group<T> createGroup(CuratorFramework client, String path, Class<T> clazz) {
        return new ZooKeeperMultiGroup<T>(client, path, clazz);
    }

    @Override
    public boolean isMaster(String id) {
        Group<T> group = this.group;
        if (group != null) {
            return ((MultiGroup) group).isMaster(id);
        } else {
            return false;
        }
    }

}
