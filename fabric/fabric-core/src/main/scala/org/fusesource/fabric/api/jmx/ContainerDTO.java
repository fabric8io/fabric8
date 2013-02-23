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

import org.fusesource.fabric.api.Container;
import org.fusesource.fabric.api.HasId;
import org.fusesource.fabric.api.Ids;
import org.fusesource.fabric.api.Versions;

import java.util.ArrayList;
import java.util.List;

/**
 * A DTO for returning containers from JSON MBeans
 */
public class ContainerDTO implements HasId {
    private String id;
    private boolean alive;
    private boolean managed;
    private boolean ensembleServer;
    private boolean provisioningComplete;
    private boolean root;
    private boolean provisioningPending;
    private String ip;
    private List<String> jmxDomains;
    private List<String> childrenIds;
    private String jmxUrl;
    private String localHostName;
    private String localIp;
    private String location;
    private String manualIp;
    private int maximumPort;
    private int minimumPort;
    private String parentId;
    private List<String> profileIds;
    private String provisionException;
    private String provisionResult;
    private String provisionStatus;
    private String publicHostname;
    private String publicIp;
    private String resolver;
    private String sshUrl;
    private String type;
    private String versionId;
    private String geoLocation;


    /**
     * Factory method which handles nulls gracefully
     */
    public static ContainerDTO newInstance(Container container) {
        if (container != null) {
            return new ContainerDTO(container);
        } else {
            return null;
        }
    }

    public static List<ContainerDTO> newInstances(Container... containers) {
        List<ContainerDTO> answer = new ArrayList<ContainerDTO>();
        if (containers != null) {
            for (Container container : containers) {
                ContainerDTO dto = newInstance(container);
                if (dto != null) {
                    answer.add(dto);
                }
            }
        }
        return answer;
    }

    public ContainerDTO() {
    }

    public ContainerDTO(Container container) {
        this.id = container.getId();
        this.ip = container.getIp();
        this.alive = container.isAlive();
        this.managed = container.isManaged();
        this.ensembleServer = container.isEnsembleServer();
        this.root = container.isRoot();
        this.provisioningComplete = container.isProvisioningComplete();
        this.provisioningPending = container.isProvisioningPending();
        this.jmxDomains = container.getJmxDomains();
        this.childrenIds = Ids.getIds(container.getChildren());
        this.jmxUrl = container.getJmxUrl();
        this.localHostName = container.getLocalHostname();
        this.localIp = container.getLocalIp();
        this.location = container.getLocation();
        this.manualIp = container.getManulIp();
        this.maximumPort = container.getMaximumPort();
        this.minimumPort = container.getMinimumPort();
        this.parentId = Ids.getId(container.getParent());
        this.profileIds = Ids.getIds(container.getProfiles());
        this.provisionException = container.getProvisionException();
        this.provisionResult = container.getProvisionResult();
        this.provisionStatus = container.getProvisionStatus();
        this.publicHostname = container.getPublicHostname();
        this.publicIp = container.getPublicIp();
        this.resolver = container.getResolver();
        this.sshUrl = container.getSshUrl();
        this.type = container.getType();
        this.versionId = Versions.getId(container.getVersion());
        this.geoLocation = container.getGeoLocation();
    }

    public String toString() {
        return "ContainerDTO(" + id + ")";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ContainerDTO that = (ContainerDTO) o;
        if (!id.equals(that.id)) return false;
        return true;
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    public List<String> getChildrenIds() {
        return childrenIds;
    }

    public void setChildrenIds(List<String> childrenIds) {
        this.childrenIds = childrenIds;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public List<String> getJmxDomains() {
        return jmxDomains;
    }

    public void setJmxDomains(List<String> jmxDomains) {
        this.jmxDomains = jmxDomains;
    }

    public String getJmxUrl() {
        return jmxUrl;
    }

    public void setJmxUrl(String jmxUrl) {
        this.jmxUrl = jmxUrl;
    }

    public String getLocalHostName() {
        return localHostName;
    }

    public void setLocalHostName(String localHostName) {
        this.localHostName = localHostName;
    }

    public String getLocalIp() {
        return localIp;
    }

    public void setLocalIp(String localIp) {
        this.localIp = localIp;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getManualIp() {
        return manualIp;
    }

    public void setManualIp(String manualIp) {
        this.manualIp = manualIp;
    }

    public int getMaximumPort() {
        return maximumPort;
    }

    public void setMaximumPort(int maximumPort) {
        this.maximumPort = maximumPort;
    }

    public int getMinimumPort() {
        return minimumPort;
    }

    public void setMinimumPort(int minimumPort) {
        this.minimumPort = minimumPort;
    }

    public String getParentId() {
        return parentId;
    }

    public void setParentId(String parentId) {
        this.parentId = parentId;
    }

    public List<String> getProfileIds() {
        return profileIds;
    }

    public void setProfileIds(List<String> profileIds) {
        this.profileIds = profileIds;
    }

    public String getProvisionException() {
        return provisionException;
    }

    public void setProvisionException(String provisionException) {
        this.provisionException = provisionException;
    }

    public String getProvisionResult() {
        return provisionResult;
    }

    public void setProvisionResult(String provisionResult) {
        this.provisionResult = provisionResult;
    }

    public String getProvisionStatus() {
        return provisionStatus;
    }

    public void setProvisionStatus(String provisionStatus) {
        this.provisionStatus = provisionStatus;
    }

    public String getPublicHostname() {
        return publicHostname;
    }

    public void setPublicHostname(String publicHostname) {
        this.publicHostname = publicHostname;
    }

    public String getPublicIp() {
        return publicIp;
    }

    public void setPublicIp(String publicIp) {
        this.publicIp = publicIp;
    }

    public String getResolver() {
        return resolver;
    }

    public void setResolver(String resolver) {
        this.resolver = resolver;
    }

    public String getSshUrl() {
        return sshUrl;
    }

    public void setSshUrl(String sshUrl) {
        this.sshUrl = sshUrl;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getVersionId() {
        return versionId;
    }

    public void setVersionId(String versionId) {
        this.versionId = versionId;
    }

    public boolean isAlive() {
        return alive;
    }

    public void setAlive(boolean alive) {
        this.alive = alive;
    }

    public boolean isEnsembleServer() {
        return ensembleServer;
    }

    public void setEnsembleServer(boolean ensembleServer) {
        this.ensembleServer = ensembleServer;
    }

    public boolean isManaged() {
        return managed;
    }

    public void setManaged(boolean managed) {
        this.managed = managed;
    }

    public boolean isProvisioningComplete() {
        return provisioningComplete;
    }

    public void setProvisioningComplete(boolean provisioningComplete) {
        this.provisioningComplete = provisioningComplete;
    }

    public boolean isProvisioningPending() {
        return provisioningPending;
    }

    public void setProvisioningPending(boolean provisioningPending) {
        this.provisioningPending = provisioningPending;
    }

    public boolean isRoot() {
        return root;
    }

    public void setRoot(boolean root) {
        this.root = root;
    }

    public String getGeoLocation() {
        return geoLocation;
    }

    public void setGeoLocation(String geoLocation) {
        this.geoLocation = geoLocation;
    }
}
