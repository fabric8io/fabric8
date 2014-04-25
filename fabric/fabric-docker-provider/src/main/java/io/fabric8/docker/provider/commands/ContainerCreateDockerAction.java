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
package io.fabric8.docker.provider.commands;

import io.fabric8.api.CreateContainerMetadata;
import io.fabric8.api.FabricService;
import io.fabric8.api.ZooKeeperClusterService;
import io.fabric8.boot.commands.support.AbstractContainerCreateAction;
import io.fabric8.docker.provider.CreateDockerContainerOptions;
import io.fabric8.utils.FabricValidations;
import io.fabric8.utils.Ports;
import io.fabric8.utils.shell.ShellUtils;
import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.apache.felix.gogo.commands.Option;

import java.net.InetAddress;
import java.net.URI;
import java.util.List;

@Command(name = "container-create-docker", scope = "fabric", description = "Creates one or more new containers via docker", detailedDescription = "classpath:containerCreateDocker.txt")
public class ContainerCreateDockerAction extends AbstractContainerCreateAction {

    @Option(name = "--host", required = false, description = "Preferred host name")
    private String host;
    @Option(name = "--env", required = false, multiValued = true, description = "Adds an environmental variable. Can be used multiple times")
    private List<String> environmentalVariables;
    @Option(name = "--min-port", multiValued = false, description = "The minimum port of the allowed port range")
    private int minimumPort = Ports.MIN_PORT_NUMBER;
    @Option(name = "--max-port", multiValued = false, description = "The maximum port of the allowed port range")
    private int maximumPort = Ports.MAX_PORT_NUMBER;
    @Option(name = "--proxy-uri", description = "Maven proxy URL to use")
    private URI proxyUri;

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

    ContainerCreateDockerAction(FabricService fabricService, ZooKeeperClusterService clusterService) {
        super(fabricService, clusterService);
    }

    @Override
    protected Object doExecute() throws Exception {
        // validate input before creating containers
        preCreateContainer(name);
        FabricValidations.validateProfileNames(profiles);

        if (isEnsembleServer && newUserPassword == null) {
            newUserPassword = zookeeperPassword != null ? zookeeperPassword : fabricService.getZookeeperPassword();
        }

        CreateDockerContainerOptions.Builder builder = CreateDockerContainerOptions.builder()
        .name(name)
        .ensembleServer(isEnsembleServer)
        .resolver(resolver)
        .bindAddress(bindAddress)
        .manualIp(manualIp)
        .number(number)
        .preferredAddress(InetAddress.getByName(host).getHostAddress())
        .adminAccess(adminAccess)
        .minimumPort(minimumPort)
        .maximumPort(maximumPort)
        .proxyUri(proxyUri != null ? proxyUri : fabricService.getMavenRepoURI())
        .zookeeperUrl(fabricService.getZookeeperUrl())
        .zookeeperPassword(isEnsembleServer && zookeeperPassword != null ? zookeeperPassword : fabricService.getZookeeperPassword())
        .jvmOpts(jvmOpts != null ? jvmOpts : fabricService.getDefaultJvmOptions())
        .withUser(newUser, newUserPassword , newUserRole)
        .version(version)
        .profiles(getProfileNames())
        .dataStoreProperties(getDataStoreProperties())
        .dataStoreType(dataStoreType != null && isEnsembleServer ? dataStoreType : fabricService.getDataStore().getType());


        CreateContainerMetadata<?>[] metadatas = fabricService.createContainers(builder.build());

        if (isEnsembleServer && metadatas != null && metadatas.length > 0 && metadatas[0].isSuccess()) {
            ShellUtils.storeZookeeperPassword(session, metadatas[0].getCreateOptions().getZookeeperPassword());
        }
        // display containers
        displayContainers(metadatas);
        return null;
    }
}
