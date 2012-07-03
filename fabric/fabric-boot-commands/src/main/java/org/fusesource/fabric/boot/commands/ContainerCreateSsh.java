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
package org.fusesource.fabric.boot.commands;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.apache.felix.gogo.commands.Option;
import org.fusesource.fabric.api.CreateContainerMetadata;
import org.fusesource.fabric.api.CreateContainerOptionsBuilder;
import org.fusesource.fabric.api.CreateSshContainerOptions;
import org.fusesource.fabric.boot.commands.support.ContainerCreateSupport;
import org.fusesource.fabric.utils.PortUtils;

@Command(name = "container-create-ssh", scope = "fabric", description = "Creates one or more new containers via SSH", detailedDescription = "classpath:containerCreateSsh.txt")
public class ContainerCreateSsh extends ContainerCreateSupport {

    @Option(name = "--host", required = true, description = "Host name to SSH into")
    private String host;
    @Option(name = "--path", description = "Path on the remote filesystem where the container is to be installed.")
    private String path;
    @Option(name = "--user", description = "User name for login.")
    private String user;
    @Option(name = "--password", description = "Password for login. If the password is omitted, private key authentication is used instead.")
    private String password;
    @Option(name = "--port", description = "The IP port number for the SSH connection.")
    private Integer port;
    @Option(name = "--min-port", multiValued = false, description = "The minimum port of the allowed port range")
    private int minimumPort = PortUtils.MIN_PORT_NUMBER;
    @Option(name = "--max-port", multiValued = false, description = "The maximum port of the allowed port range")
    private int maximumPort = PortUtils.MAX_PORT_NUMBER;
    @Option(name = "--ssh-retries", description = "Number of retries to connect on SSH")
    private Integer sshRetries;
    @Option(name = "--proxy-uri", description = "Maven proxy URL to use")
    private URI proxyUri;
    @Option(name = "--private-key", description = "The path to the private key on the filesystem. Default is ~/.ssh/id_rsa on *NIX platforms or C:\\Documents and Settings\\<UserName>\\.ssh\\id_rsa on Windows.")
    private String privateKeyFile;
    @Option(name = "--pass-phrase", description = "The pass phrase of the key. This is for use with private keys that require a pass phrase.")
    private String passPhrase;

    @Argument(index = 0, required = true, description = "The name of the container to be created. When creating multiple containers it serves as a prefix")
    protected String name;
    @Argument(index = 1, required = false, description = "The number of containers that should be created")
    protected int number = 1;

    @Override
    protected Object doExecute() throws Exception {
        // validate input before creating containers
        preCreateContainer(name);

        CreateSshContainerOptions options = CreateContainerOptionsBuilder.ssh()
        .name(name)
        .resolver(resolver)
        .ensembleServer(isEnsembleServer)
        .number(number)
        .host(host)
        .username(user)
        .password(password)
        .privateKeyFile(privateKeyFile != null ? privateKeyFile : CreateSshContainerOptions.DEFAULT_PRIVATE_KEY_FILE)
        .passPhrase(passPhrase)
        .path(path)
        .port(port)
        .sshRetries(sshRetries)
        .minimumPort(minimumPort)
        .maximumPort(maximumPort)
        .password(password)
        .proxyUri(proxyUri != null ? proxyUri : fabricService.getMavenRepoURI())
        .zookeeperUrl(fabricService.getZookeeperUrl())
        .jvmOpts(jvmOpts);

        CreateContainerMetadata[] metadatas = fabricService.createContainers(options);
        // display containers
        displayContainers(metadatas);
        // and set its profiles and versions after creation
        postCreateContainers(metadatas);
        return null;
    }

    @Override
    protected void preCreateContainer(String name) {
        super.preCreateContainer(name);

        // validate number is not out of bounds
        if (number < 1 || number > 99) {
            throw new IllegalArgumentException("The number of containers must be between 1 and 99.");
        }

        // Check if the specified host is local
        try {
            InetAddress[] localIps = InetAddress.getAllByName(InetAddress.getLocalHost().getCanonicalHostName());
            for (InetAddress address : InetAddress.getAllByName(host)) {
                if (address.isAnyLocalAddress() || address.isLoopbackAddress()) {
                    throw new IllegalArgumentException("Container creation not permitted locally");
                }
                for (InetAddress local : localIps) {
                    if (local.equals(address)) {
                        throw new IllegalArgumentException("Container creation not permitted locally");
                    }
                }
            }
        } catch (IOException e) {
            throw new IllegalArgumentException("Unable to find host address: " + host, e);
        }
    }
}
