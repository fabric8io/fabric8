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


import io.fabric8.common.util.Strings;
import io.fabric8.utils.PasswordEncoder;
import io.fabric8.zookeeper.ZkPath;
import org.apache.curator.framework.CuratorFramework;
import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.apache.karaf.shell.console.AbstractAction;

import static io.fabric8.zookeeper.utils.ZooKeeperUtils.setData;

@Command(name = "crypt-password-set", scope = "fabric", description = "Sets the master password for encryption.")
public class EncryptionMasterPasswordSetAction extends AbstractAction {

    @Argument(index = 0, name = "password", description = "The master password to set.")
    private String newPassword;

    private final CuratorFramework curator;

    EncryptionMasterPasswordSetAction(CuratorFramework curator) {
        this.curator = curator;
    }

    public CuratorFramework getCurator() {
        return curator;
    }

    @Override
    protected Object doExecute() throws Exception {
        if (Strings.isNotBlank(newPassword)) {
            setData(getCurator(), ZkPath.AUTHENTICATION_CRYPT_PASSWORD.getPath(), PasswordEncoder.encode(newPassword));
        }
        return null;
    }
}
