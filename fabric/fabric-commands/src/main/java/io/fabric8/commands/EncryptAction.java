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
import org.apache.felix.gogo.commands.Option;
import org.apache.karaf.shell.console.AbstractAction;
import org.jasypt.encryption.pbe.StandardPBEStringEncryptor;

import static io.fabric8.zookeeper.utils.ZooKeeperUtils.exists;
import static io.fabric8.zookeeper.utils.ZooKeeperUtils.getStringData;

@Command(name = "encrypt-message", scope = "fabric", description = "Encrypts a value using the configured algorithm and master password.")
public class EncryptAction extends AbstractAction {

    private static final String FORMAT = "Encrypting message %s\n Using algorithm %s and password %s\n Result: %s";

    @Option(name = "-a", aliases = "--alogrithm", description = "The algorithm to use. (Defaults to the configured one).")
    private String algorithm;
    @Option(name = "-p", aliases = "--password", description = "The password to use. (Defaults to the configured one).")
    private String password;

    @Argument(index = 0, required = true, name = "message", description = "The message to encrypt.")
    private String message;

    private final CuratorFramework curator;

    EncryptAction(CuratorFramework curator) {
        this.curator = curator;
    }

    public CuratorFramework getCurator() {
        return curator;
    }

    @Override
    protected Object doExecute() throws Exception {
        if (exists(getCurator(), ZkPath.AUTHENTICATION_CRYPT_ALGORITHM.getPath()) == null) {
            System.out.println("No encryption algorithm found in the registry.");
            return null;
        } else if (exists(getCurator(), ZkPath.AUTHENTICATION_CRYPT_PASSWORD.getPath()) == null) {
            System.out.println("No encryption master password found in the registry.");
            return null;
        } else {
            algorithm = !Strings.isNullOrBlank(algorithm) ? algorithm : getStringData(getCurator(), ZkPath.AUTHENTICATION_CRYPT_ALGORITHM.getPath());
            String rawZookeeperPassword = getStringData(getCurator(), ZkPath.AUTHENTICATION_CRYPT_PASSWORD.getPath());
            if (rawZookeeperPassword != null) {
                rawZookeeperPassword = PasswordEncoder.decode(rawZookeeperPassword);
            }
            password = password != null ? password : rawZookeeperPassword;
            StandardPBEStringEncryptor encryptor = new StandardPBEStringEncryptor();
            encryptor.setAlgorithm(algorithm);
            encryptor.setPassword(password);
            System.out.println(String.format(FORMAT, message, algorithm, password, encryptor.encrypt(message)));
        }
        return null;
    }
}
