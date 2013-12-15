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
package io.fabric8.commands.hadoop;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.felix.gogo.commands.Command;
import org.apache.felix.gogo.commands.Option;
import org.apache.karaf.shell.console.OsgiCommandSupport;
import io.fabric8.api.Container;
import io.fabric8.api.CreateChildContainerOptions;
import io.fabric8.api.CreateContainerMetadata;
import io.fabric8.api.CreateContainerOptions;
import io.fabric8.api.FabricService;
import io.fabric8.api.Profile;
import io.fabric8.api.Version;

@Command(scope = "hadoop", name = "create", description = "Create an hadoop cluster")
public class Create extends OsgiCommandSupport {

    @Option(name = "--name")
    private String name = "default";

    @Option(name = "--name-node")
    private String nameNode = "namenode";

    @Option(name = "--secondary-name-node")
    private String secondaryNameNode;

    @Option(name = "--data-nodes")
    private List<String> dataNodes = Collections.singletonList("datanode");

    @Option(name = "--job-tracker")
    private String jobTracker;

    @Option(name = "--task-trackers")
    private List<String> taskTrackers = Collections.emptyList();

    @Option(name = "--create-children")
    private boolean createChildren = false;

    @Option(name = "--force")
    private boolean force = false;

    private FabricService service;

    public FabricService getService() {
        return service;
    }

    public void setService(FabricService service) {
        this.service = service;
    }

    @Override
    protected Object doExecute() throws Exception {
        Container[] containers = service.getContainers();
        if (nameNode == null || dataNodes.isEmpty()) {
            throw new IllegalArgumentException("The name node and at least one data node must be specified");
        }
        if (!taskTrackers.isEmpty() && jobTracker == null) {
            throw new IllegalArgumentException("Can not specify task trackers if no job tracker is specified");
        }
        if (taskTrackers.isEmpty() && jobTracker != null) {
            throw new IllegalArgumentException("At least one task tracker node must be specified");
        }
        if (!createChildren) {
            if (findContainer(containers, nameNode) == null) {
                throw new IllegalStateException("Container " + nameNode + " does not exists");
            }
            if (secondaryNameNode != null && findContainer(containers, secondaryNameNode) == null) {
                throw new IllegalStateException("Container " + secondaryNameNode + " does not exists");
            }
            for (String n : dataNodes) {
                if (findContainer(containers, n) == null) {
                    throw new IllegalStateException("Container " + n + " does not exists");
                }
            }
            if (jobTracker != null && findContainer(containers, jobTracker) == null) {
                throw new IllegalStateException("Container " + jobTracker + " does not exists");
            }
            for (String n : taskTrackers) {
                if (findContainer(containers, n) == null) {
                    throw new IllegalStateException("Container " + n + " does not exists");
                }
            }
        }
        for (String p : Arrays.asList("hadoop-" + name,
                                      "hadoop-" + name + "-namenode",
                                      "hadoop-" + name + "-secondary-namenode",
                                      "hadoop-" + name + "-datanode",
                                      "hadoop-" + name + "-job-tracker",
                                      "hadoop-" + name + "-task-tracker",
                                      "insight-hdfs-" + name)) {
            Profile profile = null;
            try {
                profile = service.getDefaultVersion().getProfile(p);
            } catch (Throwable t) {
                // Ignore
            }
            if (profile != null) {
                if (force) {
                    profile.delete();
                } else {
                    throw new IllegalStateException("Profile " + profile.getId() + " already exists. Use --force to recreate the profiles.");
                }
            }
        }

        Version version = service.getDefaultVersion();
        Profile hadoop = version.getProfile("hadoop");
        Map<String, Map<String, String>> configs;

        Profile cluster = version.createProfile("hadoop-" + name);
        cluster.setParents(new Profile[] { hadoop });
        configs = new HashMap<String, Map<String, String>>();
        configs.put("io.fabric8.hadoop", new HashMap<String, String>());
        configs.get("io.fabric8.hadoop").put("fs.default.name", "hdfs://${zk:" + nameNode + "/ip}:9000");
        configs.get("io.fabric8.hadoop").put("dfs.http.address", "hdfs://${zk:" + nameNode + "/ip}:9002");
        cluster.setConfigurations(configs);

        Profile nameNodeProfile = version.createProfile("hadoop-" + name + "-namenode");
        nameNodeProfile.setParents(new Profile[]{cluster});
        configs = new HashMap<String, Map<String, String>>();
        configs.put("io.fabric8.hadoop", new HashMap<String, String>());
        configs.get("io.fabric8.hadoop").put("nameNode", "true");
        nameNodeProfile.setConfigurations(configs);

        Profile secondaryNameNodeProfile = version.createProfile("hadoop-" + name + "-secondary-namenode");
        secondaryNameNodeProfile.setParents(new Profile[]{cluster});
        configs = new HashMap<String, Map<String, String>>();
        configs.put("io.fabric8.hadoop", new HashMap<String, String>());
        configs.get("io.fabric8.hadoop").put("secondaryNameNode", "true");
        secondaryNameNodeProfile.setConfigurations(configs);

        Profile dataNodeProfile = version.createProfile("hadoop-" + name + "-datanode");
        dataNodeProfile.setParents(new Profile[]{cluster});
        configs = new HashMap<String, Map<String, String>>();
        configs.put("io.fabric8.hadoop", new HashMap<String, String>());
        configs.get("io.fabric8.hadoop").put("dataNode", "true");
        dataNodeProfile.setConfigurations(configs);

        Profile jobTrackerProfile = version.createProfile("hadoop-" + name + "-job-tracker");
        jobTrackerProfile.setParents(new Profile[]{cluster});
        configs = new HashMap<String, Map<String, String>>();
        configs.put("io.fabric8.hadoop", new HashMap<String, String>());
        configs.get("io.fabric8.hadoop").put("jobTracker", "true");
        jobTrackerProfile.setConfigurations(configs);

        Profile taskTrackerProfile = version.createProfile("hadoop-" + name + "-task-tracker");
        taskTrackerProfile.setParents(new Profile[]{cluster});
        configs = new HashMap<String, Map<String, String>>();
        configs.put("io.fabric8.hadoop", new HashMap<String, String>());
        configs.get("io.fabric8.hadoop").put("taskTracker", "true");
        taskTrackerProfile.setConfigurations(configs);

        Profile insightProfile = version.createProfile("insight-hdfs-" + name);
        insightProfile.setParents(new Profile[]{ version.getProfile("insight-hdfs")});
        configs = new HashMap<String, Map<String, String>>();
        configs.put("org.fusesource.insight.elasticsearch-default", new HashMap<String, String>());
        configs.get("org.fusesource.insight.elasticsearch-default").put("gateway.hdfs.uri", "hdfs://${zk:" + nameNode + "/ip}:9000");
        insightProfile.setConfigurations(configs);

        // Name node
        Container nameNodeContainer = findContainer(containers, nameNode);
        if (nameNodeContainer == null && createChildren) {
            nameNodeContainer = createChild(nameNode);
        }
        addProfile(nameNodeContainer, nameNodeProfile);
        // Secondary name node
        if (secondaryNameNode != null) {
            Container secondaryNameNodeContainer = findContainer(containers, secondaryNameNode);
            if (secondaryNameNodeContainer == null && createChildren) {
                secondaryNameNodeContainer = createChild(secondaryNameNode);
            }
            addProfile(secondaryNameNodeContainer, secondaryNameNodeProfile);
        }
        // Data nodes
        for (String n : dataNodes) {
            Container cont = findContainer(containers, n);
            if (cont == null) {
                cont = createChild(n);
            }
            addProfile(cont, dataNodeProfile);
        }
        // Job tracker
        if (jobTracker != null) {
            Container jobTrackerContainer = findContainer(containers, jobTracker);
            if (jobTrackerContainer == null && createChildren) {
                jobTrackerContainer = createChild(jobTracker);
            }
            addProfile(jobTrackerContainer, jobTrackerProfile);
        }
        // Task trackers
        for (String n : taskTrackers) {
            Container cont = findContainer(containers, n);
            if (cont == null) {
                cont = createChild(n);
            }
            addProfile(cont, taskTrackerProfile);
        }
        return null;
    }

    private Container createChild(String name) throws URISyntaxException {
        CreateContainerOptions options = CreateChildContainerOptions.builder()
                .name(name)
                .parent(service.getCurrentContainer().getId()).build();
        CreateContainerMetadata[] metadatas = service.createContainers(options);
        Container container = metadatas[0].getContainer();
        return container;
    }

    private void addProfile(Container container, Profile profile) {
        List<Profile> profiles = new ArrayList<Profile>();
        for (Profile p : container.getProfiles()) {
            if (!isAncestor(p, profile)) {
                profiles.add(p);
            }
        }
        profiles.add(profile);
        container.setProfiles(profiles.toArray(new Profile[profiles.size()]));

    }

    private boolean isAncestor(Profile parent, Profile child) {
        if (child.getId().equals(parent.getId())) {
            return true;
        }
        for (Profile p : child.getParents()) {
            if (isAncestor(parent, p)) {
                return true;
            }
        }
        return false;
    }

    private Container findContainer(Container[] containers, String name) {
        for (Container cont : containers) {
            if (name.equals(cont.getId())) {
                return cont;
            }
        }
        return null;
    }

}
