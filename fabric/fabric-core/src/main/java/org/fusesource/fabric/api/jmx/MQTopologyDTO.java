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
import org.fusesource.fabric.zookeeper.ZkDefs;

import java.util.List;

/**
 * Represents the topology of a logical broker
 */
public class MQTopologyDTO {

    private String name;
    private String parentProfile;
    private List<String> properties;
    private String config;
    private String data;
    private String group;
    private String networks;
    private String networksUserName;
    private String networksPassword;
    private String version;
    private String create;
    private String assign;
    private String username;
    private String password;
    private String jvmOpts;

    /**
     * Return the Broker name
     */
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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
     * Returns the configuration to use
     */
    public String getConfig() {
        return config;
    }

    public void setConfig(String config) {
        this.config = config;
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
     * Returns the comma separated list of child containers to create with mq profile
     */
    public String getCreate() {
        return create;
    }

    public void setCreate(String create) {
        this.create = create;
    }

    /**
     * Returns the containers to assign this mq profile to
     */
    public String getAssign() {
        return assign;
    }

    public void setAssign(String assign) {
        this.assign = assign;
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
}
