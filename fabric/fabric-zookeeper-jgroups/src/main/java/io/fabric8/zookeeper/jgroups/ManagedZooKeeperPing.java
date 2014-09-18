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
package io.fabric8.zookeeper.jgroups;

import io.fabric8.api.scr.ValidatingReference;
import org.apache.curator.framework.CuratorFramework;
import org.apache.felix.scr.annotations.Reference;
import org.apache.zookeeper.KeeperException;
import org.jgroups.annotations.MBean;
import org.jgroups.conf.ClassConfigurator;

/**
 * Ping's curator is managed by the container.
 */
@MBean(description = "ZooKeeper based discovery protocol. Acts as a ZooKeeper client and accesses ZooKeeper servers " +
    "to fetch discovery information")
public class ManagedZooKeeperPing extends AbstractZooKeeperPing {
    static {
        ClassConfigurator.addProtocol(Constants.MANAGED_ZK_PING_ID, ManagedZooKeeperPing.class);
    }

    @Reference(referenceInterface = CuratorFramework.class, bind = "bindCurator", unbind = "unbindCurator")
    private final ValidatingReference<CuratorFramework> curator = new ValidatingReference<>();

    protected CuratorFramework createCurator() throws KeeperException {
        return curator.get();
    }

    void bindCurator(CuratorFramework curator) {
        this.curator.bind(curator);
    }

    void unbindCurator(CuratorFramework curator) {
        this.curator.unbind(curator);
    }
}
