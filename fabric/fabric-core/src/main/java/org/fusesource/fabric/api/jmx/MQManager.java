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
package org.fusesource.fabric.api.jmx;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.MappingIterator;
import org.codehaus.jackson.map.ObjectMapper;
import org.fusesource.fabric.api.Container;
import org.fusesource.fabric.api.ContainerProvider;
import org.fusesource.fabric.api.CreateChildContainerOptions;
import org.fusesource.fabric.api.CreateContainerBasicOptions;
import org.fusesource.fabric.api.FabricRequirements;
import org.fusesource.fabric.api.FabricService;
import org.fusesource.fabric.api.MQService;
import org.fusesource.fabric.api.Profile;
import org.fusesource.fabric.internal.Objects;
import org.fusesource.fabric.service.MQServiceImpl;
import org.fusesource.fabric.utils.Strings;
import org.fusesource.fabric.zookeeper.ZkDefs;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

/**
 * An MBean for working with the global A-MQ topology configuration inside the Fabric profiles
 */
@Component(description = "Fabric MQ Manager JMX MBean")
public class MQManager implements MQManagerMXBean {
    private static final transient Logger LOG = LoggerFactory.getLogger(MQManager.class);

    private static ObjectName OBJECT_NAME;

    static {
        try {
            OBJECT_NAME = new ObjectName("org.fusesource.fabric:type=MQManager");
        } catch (MalformedObjectNameException e) {
            // ignore
        }
    }

    @Reference(referenceInterface = FabricService.class)
    private FabricService fabricService;
    @Reference(referenceInterface = MBeanServer.class)
    private MBeanServer mbeanServer;

    @Activate
    void activate(ComponentContext context) throws Exception {
        if (mbeanServer != null) {
            JMXUtils.registerMBean(this, mbeanServer, OBJECT_NAME);
        }
    }

    @Deactivate
    void deactivate() throws Exception {
        if (mbeanServer != null) {
            JMXUtils.unregisterMBean(mbeanServer, OBJECT_NAME);
        }
    }


    @Override
    public List<MQTopologyDTO> loadTopology() {
        List<MQTopologyDTO> answer = new ArrayList<MQTopologyDTO>();

        // TODO load the topology DTOs for the existing broker profiles and containers

        return answer;
    }

    @Override
    public void updateMQTopologyJson(String json) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationConfig.Feature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.configure(DeserializationConfig.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);
        List<MQTopologyDTO> dtos = new ArrayList<MQTopologyDTO>();
        MappingIterator<Object> iter = mapper.reader(FabricRequirements.class).readValues(json);
        while (iter.hasNext()) {
            Object next = iter.next();
            if (next instanceof MQTopologyDTO) {
                dtos.add((MQTopologyDTO) next);
            } else {
                LOG.warn("Expected MQTopologyDTO but parsed invalid DTO " + next);
            }
        }

        updateMQTopology(dtos);
    }

    public void updateMQTopology(List<MQTopologyDTO> dtos) {
        for (MQTopologyDTO dto : dtos) {
            // TODO check if the broker profile exists and if not create it
            createProfilesAndContainerBuilders(dto, fabricService, "child");
        }
    }

    /**
     * Creates (or updates) the profile for a given MQ group and broker configuration
     */
    public static List<CreateContainerBasicOptions.Builder> createProfilesAndContainerBuilders(MQTopologyDTO dto,
                                                                                               FabricService fabricService, String containerProviderScheme) {

        ContainerProvider containerProvider = fabricService.getProvider(containerProviderScheme);
        Objects.notNull(containerProvider, "No ContainerProvider available for scheme: " + containerProviderScheme);

        MQService service = new MQServiceImpl(fabricService);
        HashMap<String, String> configuration = new HashMap<String, String>();

        List<String> properties = dto.getProperties();
        String version = dto.getVersion();
        if (Strings.isNullOrBlank(version)) {
            version = ZkDefs.DEFAULT_VERSION;
        }

        if (properties != null) {
            for (String entry : properties) {
                String[] parts = entry.split("=", 2);
                if (parts.length == 2) {
                    configuration.put(parts[0], parts[1]);
                } else {
                    configuration.put(parts[0], "");
                }
            }
        }


        String data = dto.getData();
        String name = dto.getName();
        if (data == null) {
            data = System.getProperty("karaf.base") + System.getProperty("file.separator") + "data" + System.getProperty("file.separator") + name;
        }
        configuration.put("data", data);

        String config = dto.getConfig();
        if (config != null) {
            configuration.put("config", service.getConfig(version, config));
        }

        String group = dto.getGroup();
        if (group != null) {
            configuration.put("group", group);
        }

        String networks = dto.getNetworks();
        if (networks != null) {
            configuration.put("network", networks);
        }

        String networksUserName = dto.getNetworksUserName();
        if (networksUserName != null) {
            configuration.put("network.userName", networksUserName);
        }

        String networksPassword = dto.getNetworksPassword();
        if (networksPassword != null) {
            configuration.put("network.password", networksPassword);
        }

        String parentProfile = dto.getParentProfile();
        if (parentProfile != null) {
            configuration.put("parent", parentProfile);
        }

        Profile profile = service.createMQProfile(version, name, configuration);
        System.out.println("MQ profile " + profile.getId() + " ready");

        // assign profile to existing containers
        String assign = dto.getAssign();
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
        List<CreateContainerBasicOptions.Builder> containerBuilders = new ArrayList<CreateContainerBasicOptions.Builder>();
        String create = dto.getCreate();
        if (create != null) {
            String[] createContainers = create.split(",");
            for (String container : createContainers) {

                String type = null;
                String parent = fabricService.getCurrentContainerName();

                String jmxUser = dto.getUsername();
                String jmxPassword = dto.getPassword();
                String jvmOpts = dto.getJvmOpts();


                CreateContainerBasicOptions.Builder builder = containerProvider.newBuilder();

                builder = (CreateContainerBasicOptions.Builder) builder
                        .name(container)
                        .parent(parent)
                        .number(1)
                        .ensembleServer(false)
                        .proxyUri(fabricService.getMavenRepoURI())
                        .jvmOpts(jvmOpts)
                        .zookeeperUrl(fabricService.getZookeeperUrl())
                        .zookeeperPassword(fabricService.getZookeeperPassword())
                        .profiles(profile.getId())
                        .version(version);

                if (builder instanceof CreateChildContainerOptions.Builder) {
                    CreateChildContainerOptions.Builder childBuilder = (CreateChildContainerOptions.Builder) builder;
                    builder = childBuilder.jmxUser(jmxUser).jmxPassword(jmxPassword);
                }
                containerBuilders.add(builder);
            }
        }
        return containerBuilders;
    }
}
