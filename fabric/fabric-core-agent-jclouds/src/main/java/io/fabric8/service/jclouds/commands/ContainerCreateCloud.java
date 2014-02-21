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
package io.fabric8.service.jclouds.commands;

import java.net.URI;
import java.util.List;
import java.util.Set;

import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.apache.felix.gogo.commands.Option;
import io.fabric8.api.*;
import io.fabric8.boot.commands.support.ContainerCreateSupport;
import io.fabric8.internal.PrintStreamCreationStateListener;
import io.fabric8.service.jclouds.CreateJCloudsContainerMetadata;
import io.fabric8.service.jclouds.CreateJCloudsContainerOptions;
import io.fabric8.service.jclouds.JCloudsInstanceType;
import io.fabric8.service.jclouds.internal.CloudUtils;
import io.fabric8.utils.Ports;
import io.fabric8.utils.shell.ShellUtils;
import static io.fabric8.utils.FabricValidations.validateProfileName;

@Command(name = "container-create-cloud", scope = "fabric", description = "Creates one or more new containers on the cloud")
public class ContainerCreateCloud extends ContainerCreateSupport {

    static final String DISPLAY_FORMAT = "%22s %-30s %-30s %-30s ";
    static final String[] OUTPUT_HEADERS = {"[id]", "[container]", "[public addresses]", "[status]"};

    static final String ENSEMBLE_SERVER_DISPLAY_FORMAT = "%22s %-30s %-16s %-30s %-30s ";
    static final String[] ENSEMBLE_SERVER_OUTPUT_HEADERS = {"[id]", "[container]", "[registry password]", "[public addresses]", "[status]"};

    @Option(name = "--path", description = "Path on the remote filesystem where the container is to be installed.")
    private String path;
    @Option(name = "--env", required = false, multiValued = true, description = "Adds an environmental variable. Can be used multiple times")
    private List<String> environmentalVariables;
    @Option(name = "--name", required = true, description = "The context name. Used to distinct between multiple services of the same provider/api.")
    protected String contextName;
    @Option(name = "--provider", required = false, description = "The cloud provider name")
    private String providerName;
    @Option(name = "--api", required = false, description = "The cloud api name")
    private String apiName;
    @Option(name = "--os-family", multiValued = false, required = false, description = "OS Family")
    private String osFamily = "ubuntu";
    @Option(name = "--os-version", multiValued = false, required = false, description = "OS Version")
    private String osVersion;
    @Option(name = "--identity", required = false, description = "The identity used to access the cloud provider")
    private String identity;
    @Option(name = "--credential", required = false, description = "The credential used The identity used to access the cloud provider")
    private String credential;
    @Option(name = "--hardwareId", required = false, description = "The kind of hardware to use")
    private String hardwareId;
    @Option(name = "--instanceType", required = false, description = "The kind of instance required")
    private JCloudsInstanceType instanceType;
    @Option(name = "--imageId", required = false, description = "The image ID to use for the new node(s)")
    private String imageId;
    @Option(name = "--locationId", required = false, description = "The location to use to create the new node(s)")
    private String locationId;
    @Option(name = "--user", required = false, description = "The user account to use on the new node(s)")
    private String user;
    @Option(name = "--password", required = false, description = "The user password to use on the new node(s)")
    private String password;
    @Option(name = "--no-admin-access", required = false, description = "Disables admin access as it might no be feasible on all images.")
    private boolean disableAdminAccess;
    @Option(name = "--public-key-file", required = false, description = "Path to the public key file to use for authenticating to the container")
    private String publicKeyFile;
    @Option(name = "--owner", description = "Optional owner of images; only really used for EC2 and deprecated going forward")
    private String owner;
    @Option(name = "--add-option", required = false, multiValued = true, description = "Node specific properties. These options are provider specific. Example: --option withSubnetId=someAwsSubnetId.")
    private String[] options;
    @Option(name = "--group", description = "The group tag to use on the new node(s)")
    private String group = "fabric";
    @Option(name = "--proxy-uri", description = "The Maven proxy URL to use")
    private URI proxyUri;

    @Option(name = "--min-port", multiValued = false, description = "The minimum port of the allowed port range")
    private int minimumPort = Ports.MIN_PORT_NUMBER;

    @Option(name = "--max-port", multiValued = false, description = "The maximum port of the allowed port range")
    private int maximumPort = Ports.MAX_PORT_NUMBER;

    @Option(name = "--new-user", multiValued = false, description = "The username of a new user. The option refers to karaf user (ssh, http, jmx).")
    private String newUser="admin";
    @Option(name = "--new-user-password", multiValued = false, description = "The password of the new user. The option refers to karaf user (ssh, http, jmx).")
    private String newUserPassword;
    @Option(name = "--new-user-role", multiValued = false, description = "The role of the new user. The option refers to karaf user (ssh, http, jmx).")
    private String newUserRole = "admin";

    @Argument(index = 0, required = true, description = "The name of the container to be created. When creating multiple containers it serves as a prefix")
    protected String name;
    @Argument(index = 1, required = false, description = "The number of containers that should be created")
    protected int number = 0;

    @Override
    protected Object doExecute() throws Exception {
        // validate input before creating containers
        preCreateContainer(name);
        validateProfileName(profiles);

        if (isEnsembleServer && newUserPassword == null) {
            newUserPassword = zookeeperPassword != null ? zookeeperPassword : fabricService.getZookeeperPassword();
        }

        CreateJCloudsContainerOptions.Builder builder = CreateJCloudsContainerOptions.builder()
        .name(name)
        .bindAddress(bindAddress)
        .resolver(resolver)
        .manualIp(manualIp)
        .ensembleServer(isEnsembleServer)
        .credential(credential)
        .group(group)
        .hardwareId(hardwareId)
        .identity(identity)
        .osFamily(osFamily)
        .osVersion(osVersion)
        .imageId(imageId)
        .instanceType(instanceType)
        .locationId(locationId)
        .number(number)
        .nodeOptions(CloudUtils.parseProviderOptions(options))
        .owner(owner)
        .adminAccess(!disableAdminAccess)
        .publicKeyFile(publicKeyFile)
        .contextName(contextName)
        .providerName(providerName)
        .apiName(apiName)
        .user(user).password(password)
        .proxyUri(proxyUri != null ? proxyUri : fabricService.getMavenRepoURI())
        .zookeeperUrl(fabricService.getZookeeperUrl())
        .zookeeperPassword(isEnsembleServer && zookeeperPassword != null ? zookeeperPassword : fabricService.getZookeeperPassword())
        .jvmOpts(jvmOpts)
        .environmentalVariable(environmentalVariables)
        .version(version)
        .withUser(newUser, newUserPassword, newUserRole)
        .profiles(getProfileNames())
        .dataStoreProperties(getDataStoreProperties())
        .dataStoreType(dataStoreType != null && isEnsembleServer ? dataStoreType : fabricService.getDataStore().getType());

        if (path != null && !path.isEmpty()) {
            builder.path(path);
        }

        CreateContainerMetadata[] metadatas = fabricService.createContainers(builder.build(), new PrintStreamCreationStateListener(System.out));

        if (isEnsembleServer && metadatas != null && metadatas.length > 0 && metadatas[0].isSuccess()) {
            ShellUtils.storeZookeeperPassword(session, metadatas[0].getCreateOptions().getZookeeperPassword());
        }
        // display containers
        displayContainers(metadatas);
        return null;
    }

    protected void displayContainers(CreateContainerMetadata[] metadatas) {
        if (isEnsembleServer) {
            System.out.println(String.format(ENSEMBLE_SERVER_DISPLAY_FORMAT, ENSEMBLE_SERVER_OUTPUT_HEADERS));
        } else {
            System.out.println(String.format(DISPLAY_FORMAT, OUTPUT_HEADERS));
        }

        if (metadatas != null && metadatas.length > 0) {
            for (CreateContainerMetadata ccm : metadatas) {
                String status = "success";
                if (ccm.getFailure() != null) {
                    status = ccm.getFailure().getMessage();
                }

                String containerName = ccm.getContainerName() != null ? ccm.getContainerName() : "";
                String nodeId = "";
                Set<String> publicAddresses = null;

                if (ccm instanceof CreateJCloudsContainerMetadata) {
                    CreateJCloudsContainerMetadata metadata = (CreateJCloudsContainerMetadata) ccm;
                    nodeId = metadata.getNodeId() != null ? metadata.getNodeId() : "";
                    publicAddresses = metadata.getPublicAddresses();
                }

                if (isEnsembleServer) {
                    System.out.println(String.format(ENSEMBLE_SERVER_DISPLAY_FORMAT, nodeId, containerName, ccm.getCreateOptions().getZookeeperPassword(), publicAddresses, status));
                } else {
                    System.out.println(String.format(DISPLAY_FORMAT, nodeId, containerName, publicAddresses, status));
                }

            }
        }
    }

    @Override
    protected void preCreateContainer(String name) {
        super.preCreateContainer(name);
        // validate number is not out of bounds
        if (number < 0 || number > 999) {
            // for cloud we accept 3 digits
            throw new IllegalArgumentException("The number of containers must be between 1 and 999.");
        }

        if (isEnsembleServer && number > 1) {
            throw new IllegalArgumentException("Can not create a new ZooKeeper ensemble on multiple containers.  Create the containers and then use the fabric:create command instead.");
        }
    }
}
