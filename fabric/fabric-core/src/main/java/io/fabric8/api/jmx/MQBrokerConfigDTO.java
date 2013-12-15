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
package io.fabric8.api.jmx;

import io.fabric8.utils.Strings;
import io.fabric8.zookeeper.ZkDefs;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents the broker configuration of a logical broker profile which can be mapped to multiple containers
 */
public class MQBrokerConfigDTO {

    private BrokerKind kind;
    private String brokerName;
    private String profile;
    private String parentProfile;
    private String clientProfile;
    private String clientParentProfile;
    private List<String> properties;
    private String configUrl;
    private String data;
    private Map<String,String> ports = new HashMap<String,String>();
    private String group;
    private String[] networks;
    private String networksUserName;
    private String networksPassword;
    private String version;
    private String jvmOpts;
    private Integer replicas;
    private Integer minimumInstances;

    @Override
    public String toString() {
        return "MQBrokerConfigDTO{" +
                "group='" + group + '\'' +
                ", profile='" + profile() + '\'' +
                ", brokerName='" + brokerName + '\'' +
                ", kind='" + kind + '\'' +
                '}';
    }

    /**
     * Based on the kind of replication (N+1 or replicated etc) or based on configuration
     * return now many instances of the broker profile are required
     */
    public int requiredInstances() {
        if (replicas != null) {
            return replicas.intValue();
        }
        if (minimumInstances != null) {
            return minimumInstances.intValue();
        }
        if (kind != null) {
            switch (kind) {
                case StandAlone:
                    return 1;
                case Replicated:
                    return 3;
            }
        }
        return 2;
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
     * Returns the group if there is one configured or 'default' for the default group
     */
    public String group() {
        String answer = getGroup();
        if (Strings.isNullOrBlank(answer)) {
            answer = "default";
        }
        return answer;
    }

    /**
     * Returns the kind of the broker or the default
     */
    public BrokerKind kind() {
        BrokerKind answer = getKind();
        if (answer == null) {
            answer = BrokerKind.DEFAULT;
        }
        return answer;
    }

    /**
     * Returns the configured profile name or defaults it to "mq-broker-$group.$brokerName"
     */
    public String profile() {
        if (Strings.isNullOrBlank(profile)) {
            profile = "mq-broker-" + group() + "." + getBrokerName();
        }
        return profile;
    }

    /**
     * Returns the client connection profile name or defaults it to "mq-client-$group"
     */
    public String clientProfile() {
        if (Strings.isNullOrBlank(clientProfile)) {
            clientProfile = "mq-client-" + group();
        }
        return clientProfile;
    }


    // Properties
    //-------------------------------------------------------------------------

    public BrokerKind getKind() {
        return kind;
    }

    public void setKind(BrokerKind kind) {
        this.kind = kind;
    }

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
     * Returns the profile that clients use to connect to this group
     */
    public String getClientProfile() {
        return clientProfile;
    }

    public void setClientProfile(String clientProfile) {
        this.clientProfile = clientProfile;
    }

    /**
     * Returns the parent profile for the client profile. Defaults to "default"
     */
    public String getClientParentProfile() {
        return clientParentProfile;
    }

    public void setClientParentProfile(String clientParentProfile) {
        this.clientParentProfile = clientParentProfile;
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
     * Returns the group names of the network of brokers to create
     */
    public String[] getNetworks() {
        return networks;
    }

    public void setNetworks(String[] networks) {
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
    public Integer getMinimumInstances() {
        return minimumInstances;
    }

    public void setMinimumInstances(Integer minimumInstances) {
        this.minimumInstances = minimumInstances;
    }

    public Map<String, String> getPorts() {
        return ports;
    }

    public void setPorts(Map<String, String> ports) {
        this.ports = ports;
    }
}
