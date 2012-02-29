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
import org.fusesource.fabric.api.CreateContainerMetadata;
import org.fusesource.fabric.api.CreateContainerOptions;
import org.fusesource.fabric.api.CreateContainerOptionsBuilder;
import org.fusesource.fabric.api.MQService;
import org.fusesource.fabric.api.Profile;
import org.fusesource.fabric.commands.support.FabricCommand;
import org.fusesource.fabric.service.MQServiceImpl;
import org.fusesource.fabric.zookeeper.ZkDefs;

import java.net.URI;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

@Command(name = "mq-create", scope = "fabric", description = "Create a new broker")
public class MQCreate extends FabricCommand {

    @Argument(index=0, required = true, description = "Broker name")
    protected String name = null;
    
    @Option(name = "--config", description = "Configuration to use")
    protected String config;

    @Option(name = "--group", description = "Broker group")
    protected String group;    

    @Option(name = "--version", description = "The version id in the registry")
    protected String version = ZkDefs.DEFAULT_VERSION;

    @Option(name = "--create-container", multiValued = false, required = false, description = "Comma separated list of containers to create with mq profile")
    protected String create;

    @Option(name = "--assign-container", multiValued = false, required = false, description = "Assign this mq profile to the following containers")
    protected String assign;

    @Override
    protected Object doExecute() throws Exception {

        // create profile

        MQService service = new MQServiceImpl(fabricService);

        HashMap<String, String> configuration = new HashMap<String, String>();
        configuration.put("data", bundleContext.getDataFile(name).getAbsolutePath());

        if (config != null) {
            configuration.put("config", service.getConfig(version, config));
        }
        
        if (group != null) {
            configuration.put("group", group);
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
            String[] createContainers = create.split(",");
            for (String url : createContainers) {

                String type = null;
                String parent = "root";
                String name = url;
                if (url.contains("://")) {
                    URI uri = new URI(url);
                    type = uri.getScheme();
                    parent = null;
                    name = uri.getHost();
                } else {
                    type = "child";
                    url = "child://root";
                }


                CreateContainerOptions args = CreateContainerOptionsBuilder.type(type)
                        .name(name)
                        .parent(parent)
                        .number(1)
                        .debugContainer(false)
                        .ensembleServer(false)
                        .providerUri(url)
                        .proxyUri(fabricService.getMavenRepoURI())
                        .zookeeperUrl(fabricService.getZookeeperUrl());


                CreateContainerMetadata[] metadatas = fabricService.createContainers(args);

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
}
