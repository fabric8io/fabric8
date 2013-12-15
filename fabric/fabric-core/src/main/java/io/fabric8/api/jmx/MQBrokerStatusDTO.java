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

import java.util.List;

/**
 * Represents the status of the logical brokers
 */
public class MQBrokerStatusDTO {
    private String version;
    private String profile;
    private String brokerName;
    private String group;
    private String container;
    private String provisionStatus;
    private String provisionResult;
    private Integer minimumInstances;
    private List<String> services;
    private Boolean master;
    private boolean alive;
    private String jolokiaUrl;
    private String[] networks;

    public MQBrokerStatusDTO() {
    }

    public MQBrokerStatusDTO(MQBrokerConfigDTO configDTO) {
        setVersion(configDTO.version());
        setProfile(configDTO.profile());
        setBrokerName(configDTO.getBrokerName());
        setGroup(configDTO.getGroup());
        setNetworks(configDTO.getNetworks());
    }

    @Override
    public String toString() {
        return "MQBrokerStatusDTO{" +
                "version='" + version + '\'' +
                ", profile='" + profile + '\'' +
                ", container='" + container + '\'' +
                ", brokerName='" + brokerName + '\'' +
                ", group='" + group + '\'' +
                ", master='" + master + '\'' +
                ", provisionStatus='" + provisionStatus + '\'' +
                '}';
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getProfile() {
        return profile;
    }

    public void setProfile(String profile) {
        this.profile = profile;
    }

    public String getBrokerName() {
        return brokerName;
    }

    public void setBrokerName(String brokerName) {
        this.brokerName = brokerName;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public String getProvisionStatus() {
        return provisionStatus;
    }

    public void setProvisionStatus(String provisionStatus) {
        this.provisionStatus = provisionStatus;
    }

    public Integer getMinimumInstances() {
        return minimumInstances;
    }

    public void setMinimumInstances(Integer minimumInstances) {
        this.minimumInstances = minimumInstances;
    }

    public String getContainer() {
        return container;
    }

    public void setContainer(String container) {
        this.container = container;
    }

    /**
     * The list of master services for this node
     */
    public List<String> getServices() {
        return services;
    }

    public void setServices(List<String> services) {
        this.services = services;
    }

    public Boolean getMaster() {
        return master;
    }

    public void setMaster(Boolean master) {
        this.master = master;
    }

    public String getProvisionResult() {
        return provisionResult;
    }

    public void setProvisionResult(String provisionResult) {
        this.provisionResult = provisionResult;
    }

    public void setAlive(boolean alive) {
        this.alive = alive;
    }

    public boolean isAlive() {
        return alive;
    }

    public void setJolokiaUrl(String jolokiaUrl) {
        this.jolokiaUrl = jolokiaUrl;
    }

    public String getJolokiaUrl() {
        return jolokiaUrl;
    }

    public void setNetworks(String[] networks) {
        this.networks = networks;
    }

    public String[] getNetworks() {
        return networks;
    }
}
