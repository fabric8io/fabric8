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

import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Option;
import org.fusesource.fabric.utils.Strings;
import org.fusesource.fabric.zookeeper.ZkDefs;

import java.util.List;

/**
 * Represents the broker configuration of a logical broker profile which can be mapped to multiple containers
 */
public class MQBrokerConfigDTO {

    private String brokerName;
    private String profile;
    private String parentProfile;
    private List<String> properties;
    private String configUrl;
    private String data;
    private String group;
    private String networks;
    private String networksUserName;
    private String networksPassword;
    private String version;
    private String username;
    private String password;
    private String jvmOpts;
    private Integer replicas;
    private Integer slaves;

    /**
     * Based on the kind of replication (N+1 or replicated etc) or based on configuration
     * return now many instances of the broker profile are required
     */
    public int requiredInstances() {
        if (replicas != null) {
            return replicas.intValue();
        }
        if (slaves != null) {
            return slaves.intValue();
        }
        return 1;
    }

    /**
     * Returns the version if there is one configured or the default version
     */
    public String version() {
        String answer = getVersion();
        if (Strings.isNullOrBlank(answer)) {
            answer = ZkDefs.DEFAULT_VERSION;
        }
        return answer;
    }

    /**
     * Returns the configured profile name or defaults it to "mq-$group-$brokerName"
     */
    public String profileName() {
        if (Strings.isNullOrBlank(profile)) {
            profile = "mq-" + getGroup() + "-" + getBrokerName();
        }
        return profile;
    }


    // Properties
    //-------------------------------------------------------------------------

    /**
     * Return the Broker name
     */
    public String getBrokerName() {
        return brokerName;
    }

    public void setBrokerName(String brokerName) {
        this.brokerName = brokerName;
    }

    /**
     * Returns the profile name (which defaults to the broker name)
     */
    public String getProfile() {
        return profile;
    }

    public void setProfile(String profile) {
        this.profile = profile;
    }

    /**
     * Returns the parent profile to extend
     */
    public String getParentProfile() {
        return parentProfile;
    }

    public void setParentProfile(String parentProfile) {
        this.parentProfile = parentProfile;
    }

    /**
     * Returns additional properties to define in the profile
     */
    public List<String> getProperties() {
        return properties;
    }

    public void setProperties(List<String> properties) {
        this.properties = properties;
    }

    /**
     * Returns the configuration URL to use
     */
    public String getConfigUrl() {
        return configUrl;
    }

    public void setConfigUrl(String configUrl) {
        this.configUrl = configUrl;
    }

    /**
     * Returns the data directory for the broker
     */
    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    /**
     * Returns the broker group
     */
    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    /**
     * Returns the broker networks
     */
    public String getNetworks() {
        return networks;
    }

    public void setNetworks(String networks) {
        this.networks = networks;
    }

    /**
     * Returns the broker networks username
     */
    public String getNetworksUserName() {
        return networksUserName;
    }

    public void setNetworksUserName(String networksUserName) {
        this.networksUserName = networksUserName;
    }

    /**
     * Returns the broker networks password
     */
    public String getNetworksPassword() {
        return networksPassword;
    }

    public void setNetworksPassword(String networksPassword) {
        this.networksPassword = networksPassword;
    }

    /**
     * The version id in the registry
     */
    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    /**
     * Returns the jmx user name of the parent container
     */
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * Returns the jmx password of the parent container
     */
    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * Returns the JVM options to pass to the containers
     */
    public String getJvmOpts() {
        return jvmOpts;
    }

    public void setJvmOpts(String jvmOpts) {
        this.jvmOpts = jvmOpts;
    }

    /**
     * Returns the number of replicas for a replicated message broker
     */
    public Integer getReplicas() {
        return replicas;
    }

    public void setReplicas(Integer replicas) {
        this.replicas = replicas;
    }

    /**
     * Returns the number of slaves if using master/slave rather than replicated or N+1
     */
    public Integer getSlaves() {
        return slaves;
    }

    public void setSlaves(Integer slaves) {
        this.slaves = slaves;
    }

}
