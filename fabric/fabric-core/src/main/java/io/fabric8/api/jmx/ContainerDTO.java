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
package io.fabric8.api.jmx;

import io.fabric8.api.Container;
import io.fabric8.api.Containers;
import io.fabric8.api.Profiles;

import java.util.List;
import java.util.Map;

/**
 * A DTO for the container metadata
 */
public class ContainerDTO {
    private String id;
    private String type;
    private String sshUrl;
    private String jmxUrl;
    private String httpUrl;
    private String jolokiaUrl;
    private String location;
    private String geoLocation;
    private String resolver;
    private String ip;
    private String localIp;
    private String publicIp;
    private String manualIp;
    private String localHostName;
    private String publicHostName;
    private String provisionResult;
    private String provisionException;
    private String provisionStatus;
    private String debugPort;
    private boolean alive;
    private boolean ensembleServer;
    private boolean root;
    private boolean managed;
    private boolean provisioningComplete;
    private boolean provisioningPending;
    private boolean aliveAndOK;
    private int minimumPort;
    private int maximumPort;
    private String parent;
    private String version;
    private Map<String, String> profiles;
    private List<String> children;
    private List<String> jmxDomains;
    private List<String> provisionList;
    private Long processId;

    public ContainerDTO() {
    }

    public ContainerDTO(Container container, String baseApiLink) {
        this.id = container.getId();
        this.type = container.getType();

        this.children = Containers.containerIds(container.getChildren());
        List<String> profileIds = Profiles.profileIds(container.getProfiles());
        String profileLinkPrefix = baseApiLink + "/version/" + Profiles.versionId(container.getVersion()) + "/profile/";
        this.profiles = Links.mapIdsToLinks(profileIds, profileLinkPrefix);
        this.version = Profiles.versionId(container.getVersion());
        this.parent = Containers.containerId(container.getParent());

        this.ip = container.getIp();
        this.localIp = container.getLocalIp();
        this.manualIp = container.getManualIp();
        this.publicIp = container.getPublicIp();
        this.localHostName = container.getLocalHostname();
        this.publicHostName = container.getPublicHostname();
        this.resolver = container.getResolver();
        this.maximumPort = container.getMaximumPort();
        this.minimumPort = container.getMinimumPort();

        this.geoLocation = container.getGeoLocation();
        this.location = container.getLocation();

        this.processId = container.getProcessId();
        this.debugPort = container.getDebugPort();
        this.httpUrl = container.getHttpUrl();
        this.jmxUrl = container.getJmxUrl();
        this.jolokiaUrl = container.getJolokiaUrl();
        this.sshUrl = container.getSshUrl();

        this.provisionException = container.getProvisionException();
        this.provisionResult = container.getProvisionResult();
        this.provisionStatus = container.getProvisionStatus();
        this.provisionList = container.getProvisionList();
        this.jmxDomains = container.getJmxDomains();

        this.alive = container.isAlive();
        this.aliveAndOK = container.isAliveAndOK();
        this.ensembleServer = container.isEnsembleServer();
        this.managed = container.isManaged();
        this.provisioningComplete = container.isProvisioningComplete();
        this.provisioningPending = container.isProvisioningPending();
        this.root = container.isRoot();
    }


    @Override
    public String toString() {
        return String.format("Container: { id: %s, type: %s, provisionStatus: %s, alive: %s, version: %s }",
                id,
                type,
                provisionStatus,
                alive,
                version);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getSshUrl() {
        return sshUrl;
    }

    public void setSshUrl(String sshUrl) {
        this.sshUrl = sshUrl;
    }

    public String getJmxUrl() {
        return jmxUrl;
    }

    public void setJmxUrl(String jmxUrl) {
        this.jmxUrl = jmxUrl;
    }

    public String getHttpUrl() {
        return httpUrl;
    }

    public void setHttpUrl(String httpUrl) {
        this.httpUrl = httpUrl;
    }

    public String getJolokiaUrl() {
        return jolokiaUrl;
    }

    public void setJolokiaUrl(String jolokiaUrl) {
        this.jolokiaUrl = jolokiaUrl;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getGeoLocation() {
        return geoLocation;
    }

    public void setGeoLocation(String geoLocation) {
        this.geoLocation = geoLocation;
    }

    public String getResolver() {
        return resolver;
    }

    public void setResolver(String resolver) {
        this.resolver = resolver;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getLocalIp() {
        return localIp;
    }

    public void setLocalIp(String localIp) {
        this.localIp = localIp;
    }

    public String getPublicIp() {
        return publicIp;
    }

    public void setPublicIp(String publicIp) {
        this.publicIp = publicIp;
    }

    public String getManualIp() {
        return manualIp;
    }

    public void setManualIp(String manualIp) {
        this.manualIp = manualIp;
    }

    public String getLocalHostName() {
        return localHostName;
    }

    public void setLocalHostName(String localHostName) {
        this.localHostName = localHostName;
    }

    public String getPublicHostName() {
        return publicHostName;
    }

    public void setPublicHostName(String publicHostName) {
        this.publicHostName = publicHostName;
    }

    public String getProvisionResult() {
        return provisionResult;
    }

    public void setProvisionResult(String provisionResult) {
        this.provisionResult = provisionResult;
    }

    public String getProvisionException() {
        return provisionException;
    }

    public void setProvisionException(String provisionException) {
        this.provisionException = provisionException;
    }

    public String getProvisionStatus() {
        return provisionStatus;
    }

    public void setProvisionStatus(String provisionStatus) {
        this.provisionStatus = provisionStatus;
    }

    public String getDebugPort() {
        return debugPort;
    }

    public void setDebugPort(String debugPort) {
        this.debugPort = debugPort;
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

    public boolean isRoot() {
        return root;
    }

    public void setRoot(boolean root) {
        this.root = root;
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

    public boolean isAliveAndOK() {
        return aliveAndOK;
    }

    public void setAliveAndOK(boolean aliveAndOK) {
        this.aliveAndOK = aliveAndOK;
    }

    public int getMinimumPort() {
        return minimumPort;
    }

    public void setMinimumPort(int minimumPort) {
        this.minimumPort = minimumPort;
    }

    public int getMaximumPort() {
        return maximumPort;
    }

    public void setMaximumPort(int maximumPort) {
        this.maximumPort = maximumPort;
    }

    public String getParent() {
        return parent;
    }

    public void setParent(String parent) {
        this.parent = parent;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public Map<String, String> getProfiles() {
        return profiles;
    }

    public void setProfiles(Map<String, String> profiles) {
        this.profiles = profiles;
    }

    public List<String> getChildren() {
        return children;
    }

    public void setChildren(List<String> children) {
        this.children = children;
    }

    public List<String> getJmxDomains() {
        return jmxDomains;
    }

    public void setJmxDomains(List<String> jmxDomains) {
        this.jmxDomains = jmxDomains;
    }

    public List<String> getProvisionList() {
        return provisionList;
    }

    public void setProvisionList(List<String> provisionList) {
        this.provisionList = provisionList;
    }

    public Long getProcessId() {
        return processId;
    }

    public void setProcessId(Long processId) {
        this.processId = processId;
    }
}
