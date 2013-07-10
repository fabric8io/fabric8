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
package org.fusesource.fabric.commands;

import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.apache.felix.gogo.commands.Option;
import org.fusesource.fabric.api.Container;
import org.fusesource.fabric.api.CreateChildContainerOptions;
import org.fusesource.fabric.api.CreateContainerMetadata;
import org.fusesource.fabric.api.CreateContainerOptionsBuilder;
import org.fusesource.fabric.api.FabricAuthenticationException;
import org.fusesource.fabric.api.MQService;
import org.fusesource.fabric.api.Profile;
import org.fusesource.fabric.boot.commands.support.FabricCommand;
import org.fusesource.fabric.service.MQServiceImpl;
import org.fusesource.fabric.utils.shell.ShellUtils;
import org.fusesource.fabric.zookeeper.ZkDefs;

import java.io.IOException;
import java.util.*;

@Command(name = "mq-create", scope = "fabric", description = "Create a new broker")
public class MQCreate extends FabricCommand {

    @Argument(index=0, required = true, description = "Broker name")
    protected String name = null;
    
    @Option(name = "--parent-profile", description = "The parent profile to extend")
    protected String parentProfile;

    @Option(name = "--property", aliases = {"-D"}, description = "Additional properties to define in the profile")
    List<String> properties;

    @Option(name = "--config", description = "Configuration to use")
    protected String config;

    @Option(name = "--data", description = "Data directory for the broker")
    protected String data;

    @Option(name = "--group", description = "Broker group")
    protected String group;

    @Option(name = "--networks", description = "Broker networks")
    protected String networks;

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

    @Override
    protected Object doExecute() throws Exception {

        // create profile

        MQService service = new MQServiceImpl(fabricService);

        HashMap<String, String> configuration = new HashMap<String, String>();

        if( properties!=null ) {
            for (String entry : properties) {
                String []parts = entry.split("=", 2);
                if( parts.length==2 ) {
                    configuration.put(parts[0], parts[1]);
                } else {
                    configuration.put(parts[0], "");
                }
            }
        }

        if (data == null) {
            data = System.getProperty("karaf.base") + System.getProperty("file.separator")+  "data" + System.getProperty("file.separator") + name;
        }
        configuration.put("data", data);

        if (config != null) {
            configuration.put("config", service.getConfig(version, config));
        }
        
        if (group != null) {
            configuration.put("group", group);
        }

        if (networks != null) {
            configuration.put("network", networks);
        }

        if (networksUserName != null) {
            configuration.put("network.userName", networksUserName);
        }

        if (networksPassword != null) {
            configuration.put("network.password", networksPassword);
        }

        if( parentProfile !=null ) {
            configuration.put("parent", parentProfile);
        }

        Profile profile = service.createMQProfile(version, name, configuration);
        System.out.println("MQ profile " + profile.getId() + " ready");

        // assign profile to existing containers
        if (assign != null) {
            String[] assignContainers = assign.split(",");
            for (String containerName : assignContainers) {
                try {
                    Container container = fabricService.getContainer(containerName);
                    if (container == null) {
                        System.out.println("Failed to assign profile to " + containerName + ": profile doesn't exists");
                    } else {
                        HashSet<Profile> profiles = new HashSet<Profile>(Arrays.asList(container.getProfiles()));
                        profiles.add(profile);
                        container.setProfiles(profiles.toArray(new Profile[profiles.size()]));
                        System.out.println("Profile successfully assigned to " + containerName);
                    }
                } catch (Exception e) {
                    System.out.println("Failed to assign profile to " + containerName + ": " + e.getMessage());
                }
            }
        }

        // create new containers
        if (create != null) {
            CreateContainerMetadata[] metadatas;

            String[] createContainers = create.split(",");
            for (String container : createContainers) {

                String type = null;
                String parent = fabricService.getCurrentContainerName();

                String jmxUser = username != null ? username : ShellUtils.retrieveFabricUser(session);
                String jmxPassword = password != null ? password : ShellUtils.retrieveFabricUserPassword(session);

                CreateChildContainerOptions.Builder builder = CreateContainerOptionsBuilder.child()
                        .name(container)
                        .parent(parent)
                        .number(1)
                        .ensembleServer(false)
                        .proxyUri(fabricService.getMavenRepoURI())
                        .zookeeperUrl(fabricService.getZookeeperUrl())
                        .zookeeperPassword(fabricService.getZookeeperPassword())
                        .jvmOpts(jvmOpts)
                        .jmxUser(jmxUser)
                        .jmxPassword(jmxPassword);

                try {
                    metadatas = fabricService.createContainers(builder.build());
                    ShellUtils.storeFabricCredentials(session, jmxUser, jmxPassword);
                } catch (FabricAuthenticationException fae) {
                    //If authentication fails, prompts for credentials and try again.
                    promptForJmxCredentialsIfNeeded();
                    metadatas = fabricService.createContainers(builder.jmxUser(username).jmxPassword(jmxPassword).build());
                    ShellUtils.storeFabricCredentials(session, username, password);
                }

                for (CreateContainerMetadata metadata : metadatas) {
                    if (metadata.isSuccess()) {
                        Container child = metadata.getContainer();
                        child.setProfiles(new Profile[]{profile});
                        System.out.println("Successfully created container " + metadata.getContainerName());
                    } else {
                        System.out.println("Failed to create container " + metadata.getContainerName() + ": " + metadata.getFailure().getMessage());
                    }
                }

            }
        }

      return null;
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
}
