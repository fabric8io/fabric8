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
package io.fabric8.container.process;

import io.fabric8.api.Container;
import io.fabric8.api.CreateChildContainerOptions;
import io.fabric8.api.scr.support.Strings;
import io.fabric8.zookeeper.utils.ZooKeeperUtils;
import org.apache.curator.framework.CuratorFramework;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.PropertyOption;
import org.apache.zookeeper.CreateMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Represents the configuration to publish a value to ZooKeeper
 */
@Component(name = "io.fabric8.zk.publish", label = "Fabric8 ZooKeeper Publish Rule", immediate = false, metatype = true)
public class ZooKeeperPublishConfig {
    /**
     * The PID for the process container ZooKeeper publish values; for what entries to write into ZK
     */
    public static final String PROCESS_CONTAINER_ZK_PUBLISH_PID = "io.fabric8.zookeeper.publish";

    private static final transient Logger LOG = LoggerFactory.getLogger(ZooKeeperPublishConfig.class);

    @Property(label = "ZooKeeper Path",
            description = "The path in ZooKeeper to publish to")
    private String path;

    @Property(label = "Publish Value",
            description = "The value to publish to ZooKeeper.")
    private String publishValue;

    @Property(label = "Export environment variable",
            description = "The name of the environment variable to export after the publish")
    private String exportEnvironmentName;

    @Property(label = "Export value",
            description = "The value which is evaluated which if it is not blank will export an environment variable after the publish to ZooKeeper.")
    private String exportValue;

    @Property(label = "Create mode", options = {
                @PropertyOption(name = "Ephemeral", value = "EPHEMERAL"),
                @PropertyOption(name = "Ephemeral Sequential", value = "EPHEMERAL_SEQUENTIAL"),
                @PropertyOption(name = "Persistent", value = "PERSISTENT"),
                @PropertyOption(name = "Persistent Sequential", value = "PERSISTENT_SEQUENTIAL")},
            description = "What kind of entry is required; persistent. ephemeral or sequenced etc.")
    private CreateMode createMode;

    @Override
    public String toString() {
        return "ZooKeeperPublishConfig{" +
                "path='" + path + '\'' +
                ", publishValue='" + publishValue + '\'' +
                '}';
    }

    public void publish(CuratorFramework curator, CreateChildContainerOptions options, ProcessContainerConfig processConfig, Container container, Map<String, String> environmentVariables) {
        LOG.info("Publishing to ZK path: " + path + " value " + publishValue + " createMode: " + createMode);
        if (!Strings.isNullOrBlank(path)) {
            try {
                if (createMode != null) {
                    ZooKeeperUtils.create(curator, path, publishValue, createMode);
                } else {
                    ZooKeeperUtils.setData(curator, path, publishValue);
                }
            } catch (Exception e) {
                LOG.error("Failed to write ZK path " + path + " value " + publishValue + " createMode: " + createMode+ ". " + e, e);
            }

            // TODO now optionally export an environment variable using an value?
            // e.g. to find the master or something...
        }
    }


    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getPublishValue() {
        return publishValue;
    }

    public void setPublishValue(String publishValue) {
        this.publishValue = publishValue;
    }

    public String getExportEnvironmentName() {
        return exportEnvironmentName;
    }

    public void setExportEnvironmentName(String exportEnvironmentName) {
        this.exportEnvironmentName = exportEnvironmentName;
    }

    public String getExportValue() {
        return exportValue;
    }

    public void setExportValue(String exportValue) {
        this.exportValue = exportValue;
    }

}
