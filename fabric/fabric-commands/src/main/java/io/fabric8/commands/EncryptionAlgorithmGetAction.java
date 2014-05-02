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
package io.fabric8.commands;

import org.apache.curator.framework.CuratorFramework;
import org.apache.felix.gogo.commands.Command;
import io.fabric8.boot.commands.support.FabricCommand;
import io.fabric8.zookeeper.ZkPath;
import org.apache.karaf.shell.console.AbstractAction;

import static io.fabric8.zookeeper.utils.ZooKeeperUtils.exists;
import static io.fabric8.zookeeper.utils.ZooKeeperUtils.getStringData;

@Command(name = "crypt-algorithm-get", scope = "fabric", description = "Displays the current encryption algorithm.")
public class EncryptionAlgorithmGetAction extends AbstractAction {

    private final CuratorFramework curator;

    EncryptionAlgorithmGetAction(CuratorFramework curator) {
        this.curator = curator;
    }

    public CuratorFramework getCurator() {
        return curator;
    }

    @Override
    protected Object doExecute() throws Exception {
        if (exists(getCurator(), ZkPath.AUTHENTICATION_CRYPT_ALGORITHM.getPath()) != null) {
            System.out.println(getStringData(getCurator(), ZkPath.AUTHENTICATION_CRYPT_ALGORITHM.getPath()));
        }
        return null;
    }
}
