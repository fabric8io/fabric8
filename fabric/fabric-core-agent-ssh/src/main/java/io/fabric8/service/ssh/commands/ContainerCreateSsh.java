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
package io.fabric8.service.ssh.commands;

import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.apache.felix.gogo.commands.Option;
import io.fabric8.api.CreateContainerMetadata;
import io.fabric8.boot.commands.support.ContainerCreateSupport;
import io.fabric8.service.ssh.CreateSshContainerOptions;
import io.fabric8.utils.Ports;
import io.fabric8.utils.shell.ShellUtils;

import static io.fabric8.utils.FabricValidations.validateProfileName;

import java.net.InetAddress;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Command(name = "container-create-ssh", scope = "fabric", description = "Creates one or more new containers via SSH", detailedDescription = "classpath:containerCreateSsh.txt")
public class ContainerCreateSsh extends ContainerCreateSupport {

    @Option(name = "--host", required = true, description = "Host name to SSH into")
    private String host;
    @Option(name = "--path", description = "Path on the remote filesystem where the container is to be installed.")
    private String path;
    @Option(name = "--env", required = false, multiValued = true, description = "Adds an environmental variable. Can be used multiple times")
    private List<String> environmentalVariables;
    @Option(name = "--user", description = "User name for login.")
    private String user;
    @Option(name = "--password", description = "Password for login. If the password is omitted, private key authentication is used instead.")
    private String password;
    @Option(name = "--port", description = "The IP port number for the SSH connection.")
    private int port = Ports.DEFAULT_HOST_SSH_PORT;
    @Option(name = "--min-port", multiValued = false, description = "The minimum port of the allowed port range")
    private int minimumPort = Ports.MIN_PORT_NUMBER;
    @Option(name = "--max-port", multiValued = false, description = "The maximum port of the allowed port range")
    private int maximumPort = Ports.MAX_PORT_NUMBER;
    @Option(name = "--ssh-retries", description = "Number of retries to connect on SSH")
    private int sshRetries;
    @Option(name = "--proxy-uri", description = "Maven proxy URL to use")
    private URI proxyUri;
    @Option(name = "--private-key", description = "The path to the private key on the filesystem. Default is ~/.ssh/id_rsa on *NIX platforms or C:\\Documents and Settings\\<UserName>\\.ssh\\id_rsa on Windows.")
    private String privateKeyFile;
    @Option(name = "--pass-phrase", description = "The pass phrase of the key. This is for use with private keys that require a pass phrase.")
    private String passPhrase;

    @Option(name = "--new-user", multiValued = false, description = "The username of a new user. The option refers to karaf user (ssh, http, jmx).")
    private String newUser="admin";
    @Option(name = "--new-user-password", multiValued = false, description = "The password of the new user. The option refers to karaf user (ssh, http, jmx).")
    private String newUserPassword;
    @Option(name = "--new-user-role", multiValued = false, description = "The role of the new user. The option refers to karaf user (ssh, http, jmx).")
    private String newUserRole = "admin";
    @Option(name = "--with-admin-access", description = "Indicates that the target user has admin access (password-less sudo). When used installation of missing dependencies will be attempted.")
    private boolean adminAccess;

    @Argument(index = 0, required = true, description = "The name of the container to be created. When creating multiple containers it serves as a prefix")
    protected String name;
	@Argument(index = 1, required = false, description = "The number of containers that should be created")
	protected int number = 0;

    @Override
    protected Object doExecute() throws Exception {
        // validate input before creating containers
        preCreateContainer(name);
        validateProfileName(profiles);

        Map<String, String> datastoreProperties = new HashMap<String, String>();


        if (isEnsembleServer && newUserPassword == null) {
            newUserPassword = zookeeperPassword != null ? zookeeperPassword : fabricService.getZookeeperPassword();
        }

        CreateSshContainerOptions.Builder builder = CreateSshContainerOptions.builder()
        .name(name)
        .ensembleServer(isEnsembleServer)
        .resolver(resolver)
        .bindAddress(bindAddress)
        .manualIp(manualIp)
        .number(number)
        .host(host)
        .preferredAddress(InetAddress.getByName(host).getHostAddress())
        .username(user)
        .password(password)
        .privateKeyFile(privateKeyFile != null ? privateKeyFile : CreateSshContainerOptions.DEFAULT_PRIVATE_KEY_FILE)
        .passPhrase(passPhrase)
        .port(port)
        .adminAccess(adminAccess)
        .sshRetries(sshRetries)
        .minimumPort(minimumPort)
        .maximumPort(maximumPort)
        .password(password)
        .proxyUri(proxyUri != null ? proxyUri : fabricService.getMavenRepoURI())
        .zookeeperUrl(fabricService.getZookeeperUrl())
        .zookeeperPassword(isEnsembleServer && zookeeperPassword != null ? zookeeperPassword : fabricService.getZookeeperPassword())
        .jvmOpts(jvmOpts != null ? jvmOpts : fabricService.getDefaultJvmOptions())
        .environmentalVariable(environmentalVariables)
        .withUser(newUser, newUserPassword , newUserRole)
        .version(version)
        .profiles(getProfileNames())
        .dataStoreProperties(getDataStoreProperties())
        .dataStoreType(dataStoreType != null && isEnsembleServer ? dataStoreType : fabricService.getDataStore().getType());


        if (path != null && !path.isEmpty()) {
            builder.path(path);
        }

        CreateContainerMetadata[] metadatas = fabricService.createContainers(builder.build());

        if (isEnsembleServer && metadatas != null && metadatas.length > 0 && metadatas[0].isSuccess()) {
            ShellUtils.storeZookeeperPassword(session, metadatas[0].getCreateOptions().getZookeeperPassword());
        }
        // display containers
        displayContainers(metadatas);
        return null;
    }
}
