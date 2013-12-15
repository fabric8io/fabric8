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

import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.apache.felix.gogo.commands.CompleterValues;
import org.apache.felix.gogo.commands.Option;
import io.fabric8.api.CreateChildContainerOptions;
import io.fabric8.api.CreateContainerBasicOptions;
import io.fabric8.api.CreateContainerMetadata;
import io.fabric8.api.FabricAuthenticationException;
import io.fabric8.api.Profile;
import io.fabric8.api.jmx.BrokerKind;
import io.fabric8.api.jmx.MQBrokerConfigDTO;
import io.fabric8.api.jmx.MQManager;
import io.fabric8.boot.commands.support.FabricCommand;
import io.fabric8.utils.Strings;
import io.fabric8.utils.shell.ShellUtils;
import io.fabric8.zookeeper.ZkDefs;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Command(name = "mq-create", scope = "fabric", description = "Create a new broker")
public class MQCreate extends FabricCommand {

    @Argument(index = 0, required = true, description = "Broker name")
    protected String name = null;

    @Option(name = "--parent-profile", description = "The parent profile to extend")
    protected String parentProfile;

    @Option(name = "--profile", description = "The profile name to create/update if defining N+1 broker groups (otherwise this is defaulted to the broker name). Defaults to 'mq-broker-$GROUP.$NAME'")
    protected String profile;

    @Option(name = "--client-profile", description = "The profile name for clients to use to connect to the broker group. Defaults to 'mq-client-$GROUP'")
    protected String clientProfile;

    @Option(name = "--client-parent-profile", description = "The parent profile used for the client-profile for clients connecting to the broker group. Defaults to 'default'")
    protected String clientParentProfile;

    @Option(name = "--property", aliases = {"-D"}, description = "Additional properties to define in the profile")
    List<String> properties;

    @Option(name = "--config", description = "Configuration to use")
    protected String config;

    @Option(name = "--data", description = "Data directory for the broker")
    protected String data;

    @Option(name = "--ports", multiValued = true, description = "Port number for the transport connectors")
    protected String[] ports;

    @Option(name = "--group", description = "Broker group")
    protected String group;

    @Option(name = "--networks", multiValued = true, description = "Broker networks")
    protected String[] networks;

    @Option(name = "--networks-username", description = "Broker networks UserName")
    protected String networksUserName;

    @Option(name = "--networks-password", description = "Broker networks Password")
    protected String networksPassword;

    @Option(name = "--version", description = "The version id in the registry")
    protected String version = ZkDefs.DEFAULT_VERSION;

    @Option(name = "--create-container", multiValued = false, required = false, description = "Comma separated list of child containers to create with mq profile")
    protected String create;

    @Option(name = "--assign-container", multiValued = false, required = false, description = "Assign this mq profile to the following containers")
    protected String assign;

    @Option(name = "--jmx-user", multiValued = false, required = false, description = "The jmx user name of the parent container.")
    protected String username;

    @Option(name = "--jmx-password", multiValued = false, required = false, description = "The jmx password of the parent container.")
    protected String password;

    @Option(name = "--jvm-opts", multiValued = false, required = false, description = "Options to pass to the container's JVM.")
    protected String jvmOpts;

    @Option(name = "--minimumInstances", multiValued = false, required = false, description = "Minimum number of containers required of this broker's profile.")
    protected Integer minimumInstances;

    @Option(name = "--replicas", multiValued = false, required = false, description = "Number of replicas required for replicated brokers (which typically use a parent-profile of mq-replicated profile).")
    protected Integer replicas;

    @Option(name = "--kind", multiValued = false, required = false, description = "The kind of broker to create")
    @CompleterValues()
    protected BrokerKind kind;

    @Override
    protected Object doExecute() throws Exception {
        MQBrokerConfigDTO dto = createDTO();

        Profile profile = MQManager.createOrUpdateProfile(dto, fabricService);
        String profileId = profile.getId();

        System.out.println("MQ profile " + profileId + " ready");

        // assign profile to existing containers
        if (assign != null) {
            String[] assignContainers = assign.split(",");
            MQManager.assignProfileToContainers(fabricService, profile, assignContainers);
        }

        // create containers
        if (create != null) {
            String[] createContainers = create.split(",");
            List<CreateContainerBasicOptions.Builder> builderList = MQManager.createContainerBuilders(
                    dto, fabricService, "child", profileId, dto.version(), createContainers);
            for (CreateContainerBasicOptions.Builder builder : builderList) {
                CreateContainerMetadata[] metadatas = null;
                try {
                    if (builder instanceof CreateChildContainerOptions.Builder) {
                        CreateChildContainerOptions.Builder childBuilder = (CreateChildContainerOptions.Builder) builder;
                        builder = childBuilder.jmxUser(username).jmxPassword(password);
                    }
                    metadatas = fabricService.createContainers(builder.build());
                    ShellUtils.storeFabricCredentials(session, username, password);
                } catch (FabricAuthenticationException fae) {
                    //If authentication fails, prompts for credentials and try again.
                    if (builder instanceof CreateChildContainerOptions.Builder) {
                        CreateChildContainerOptions.Builder childBuilder = (CreateChildContainerOptions.Builder) builder;
                        promptForJmxCredentialsIfNeeded();
                        metadatas = fabricService.createContainers(childBuilder.jmxUser(username).jmxPassword(password).build());
                        ShellUtils.storeFabricCredentials(session, username, password);
                    }
                }
            }
        }
        return null;
    }

    private MQBrokerConfigDTO createDTO() {
        if (Strings.isNullOrBlank(username)) {
            username = ShellUtils.retrieveFabricUser(session);
        }
        if (Strings.isNullOrBlank(password)) {
            password = ShellUtils.retrieveFabricUserPassword(session);
        }

        MQBrokerConfigDTO dto = new MQBrokerConfigDTO();
        dto.setConfigUrl(config);
        dto.setData(data);
        if (ports != null && ports.length > 0) {
            for (String port : ports) {
                addConfig(port, dto.getPorts());
            }
        }
        dto.setGroup(group);
        dto.setJvmOpts(jvmOpts);
        dto.setBrokerName(name);
        dto.setProfile(profile);
        dto.setClientProfile(clientProfile);
        dto.setClientParentProfile(clientParentProfile);
        dto.setNetworks(networks);
        dto.setNetworksPassword(networksPassword);
        dto.setNetworksUserName(networksUserName);
        dto.setParentProfile(parentProfile);
        dto.setProperties(properties);
        dto.setVersion(version);
        dto.setMinimumInstances(minimumInstances);
        dto.setReplicas(replicas);
        if (kind != null) {
            dto.setKind(kind);
        }
        return dto;
    }

    private void promptForJmxCredentialsIfNeeded() throws IOException {
        // If the username was not configured via cli, then prompt the user for the values
        if (username == null) {
            log.debug("Prompting user for jmx login");
            username = ShellUtils.readLine(session, "Jmx Login for " + fabricService.getCurrentContainerName() + ": ", false);
        }

        if (password == null) {
            password = ShellUtils.readLine(session, "Jmx Password for " + fabricService.getCurrentContainerName() + ": ", true);
        }
    }

    private void addConfig(String config, Map<String, String> map) {
        String key = null;
        String value = null;
        if (config.contains("=")) {
            key = config.substring(0, config.indexOf("="));
            value = config.substring(config.indexOf("=") + 1);
        }
        if (key != null && value != null) {
           map.put(key, value);
        }
    }
}
