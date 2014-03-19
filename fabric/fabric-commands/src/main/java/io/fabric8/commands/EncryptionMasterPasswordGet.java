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
package io.fabric8.commands;

import org.apache.felix.gogo.commands.Command;
import io.fabric8.boot.commands.support.FabricCommand;
import io.fabric8.utils.PasswordEncoder;
import io.fabric8.zookeeper.ZkPath;

import static io.fabric8.zookeeper.utils.ZooKeeperUtils.exists;
import static io.fabric8.zookeeper.utils.ZooKeeperUtils.getStringData;

@Command(name = "crypt-password-get", scope = "fabric", description = "Displays the master password for encryption.")
public class EncryptionMasterPasswordGet extends FabricCommand {

    @Override
    protected Object doExecute() throws Exception {
        if (exists(getCurator(), ZkPath.AUTHENTICATION_CRYPT_PASSWORD.getPath()) != null) {
            System.out.println(PasswordEncoder.decode(getStringData(getCurator(), ZkPath.AUTHENTICATION_CRYPT_PASSWORD.getPath())));
        }
        return null;
    }
}
