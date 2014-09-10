/*
 * Copyright 2005-2014 Red Hat, Inc.
 *
 * Red Hat licenses this file to you under the Apache License, version
 * 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */

package io.fabric8.internal;

import io.fabric8.api.Container;
import io.fabric8.api.CreateContainerMetadata;
import io.fabric8.api.FabricService;
import io.fabric8.api.Profile;
import io.fabric8.api.Version;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class ImmutableContainerImpl implements Container {

    private final String id;
    private final String type;
    private final Container parent;
    private final Profile overlayProfile;
    private final FabricService fabricService;
    private final boolean alive;
    private final boolean aliveAndOK;
    private final boolean ensembleServer;
    private final boolean root;
    private final boolean managed;
    private final String httpUrl;
    private final String sshUrl;
    private final String jmxUrl;
    private final String jolokiaUrl;
    private final String debugPort;
    private final String versionId;
    private final Version version;
    private final Long processId;
    private final Map<String, Profile> profiles = new LinkedHashMap<String, Profile>();
    private final String location;
    private final String geoLocation;
    private final String resolver;
    private final String ip;
    private final String localIp;
    private final String localHostname;
    private final String publicIp;
    private final String publicHostname;
    private final String manualIp;
    private final int minimumPort;
    private final int maximumPort;
    private final Container[] children;
    private final List<String> jmxDomains;
    private final boolean provisioningComplete;
    private final boolean provisioningPending;
    private final String provisionResult;
    private final String provisionException;
    private final List<String> provisionList;
    private final Properties provisionChecksums;
    private final String provisionStatus;
    private final Map<String, String> provisionStatusMap;
    private final CreateContainerMetadata metadata;


    public ImmutableContainerImpl(String id, String type, Container parent, Profile overlayProfile, FabricService fabricService, boolean alive, boolean aliveAndOK, boolean ensembleServer, boolean root, boolean managed, String httpUrl, String sshUrl, String jmxUrl, String jolokiaUrl, String debugPort, String versionId, Version version, Long processId, Profile[] profiles, String location, String geoLocation, String resolver, String ip, String localIp, String localHostname, String publicIp, String publicHostname, String manualIp, int minimumPort, int maximumPort, Container[] children, List<String> jmxDomains, boolean provisioningComplete, boolean provisioningPending, String provisionResult, String provisionException, List<String> provisionList, Properties provisionChecksums, String provisionStatus, Map<String, String> provisionStatusMap, CreateContainerMetadata metadata) {
        this.id = id;
        this.type = type;
        this.parent = parent;
        this.overlayProfile = overlayProfile;
        this.fabricService = fabricService;
        this.alive = alive;
        this.aliveAndOK = aliveAndOK;
        this.ensembleServer = ensembleServer;
        this.root = root;
        this.managed = managed;
        this.httpUrl = httpUrl;
        this.sshUrl = sshUrl;
        this.jmxUrl = jmxUrl;
        this.jolokiaUrl = jolokiaUrl;
        this.debugPort = debugPort;
        this.versionId = versionId;
        this.version = version;
        this.processId = processId;
        this.location = location;
        this.geoLocation = geoLocation;
        this.resolver = resolver;
        this.ip = ip;
        this.localIp = localIp;
        this.localHostname = localHostname;
        this.publicIp = publicIp;
        this.publicHostname = publicHostname;
        this.manualIp = manualIp;
        this.minimumPort = minimumPort;
        this.maximumPort = maximumPort;
        this.children = children;
        this.jmxDomains = jmxDomains != null ? Collections.unmodifiableList(jmxDomains) : Collections.<String>emptyList();
        this.provisioningComplete = provisioningComplete;
        this.provisioningPending = provisioningPending;
        this.provisionResult = provisionResult;
        this.provisionException = provisionException;
        this.provisionList = provisionList;
        this.provisionChecksums = provisionChecksums;
        this.provisionStatus = provisionStatus;
        this.provisionStatusMap = provisionStatusMap != null ? Collections.unmodifiableMap(provisionStatusMap) : Collections.<String, String>emptyMap();
        this.metadata = metadata;
        
        if (profiles != null) {
            for (Profile prf : profiles) {
                this.profiles.put(prf.getId(), prf);
            }
        }
    }

    public String getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public Container getParent() {
        return parent;
    }

    public Profile getOverlayProfile() {
        return overlayProfile;
    }

    public FabricService getFabricService() {
        return fabricService;
    }

    public boolean isAlive() {
        return alive;
    }

    public boolean isAliveAndOK() {
        return aliveAndOK;
    }

    public boolean isEnsembleServer() {
        return ensembleServer;
    }

    public boolean isRoot() {
        return root;
    }

    public boolean isManaged() {
        return managed;
    }

    public String getHttpUrl() {
        return httpUrl;
    }

    public String getSshUrl() {
        return sshUrl;
    }

    public String getJmxUrl() {
        return jmxUrl;
    }

    public String getJolokiaUrl() {
        return jolokiaUrl;
    }

    public String getDebugPort() {
        return debugPort;
    }

    public String getVersionId() {
        return versionId;
    }

    public Version getVersion() {
        return version;
    }

    public Long getProcessId() {
        return processId;
    }

    @Override
    public List<String> getProfileIds() {
        return Collections.unmodifiableList(new ArrayList<String>(profiles.keySet()));
    }

    public Profile[] getProfiles() {
        Collection<Profile> values = profiles.values();
        return values.toArray(new Profile[values.size()]);
    }

    public String getLocation() {
        return location;
    }

    public String getGeoLocation() {
        return geoLocation;
    }

    public String getResolver() {
        return resolver;
    }

    public String getIp() {
        return ip;
    }

    public String getLocalIp() {
        return localIp;
    }

    public String getLocalHostname() {
        return localHostname;
    }

    public String getPublicIp() {
        return publicIp;
    }

    public String getPublicHostname() {
        return publicHostname;
    }

    public String getManualIp() {
        return manualIp;
    }

    public int getMinimumPort() {
        return minimumPort;
    }

    public int getMaximumPort() {
        return maximumPort;
    }

    public Container[] getChildren() {
        return children;
    }

    public List<String> getJmxDomains() {
        return jmxDomains;
    }

    public boolean isProvisioningComplete() {
        return provisioningComplete;
    }

    public boolean isProvisioningPending() {
        return provisioningPending;
    }

    public String getProvisionResult() {
        return provisionResult;
    }

    public String getProvisionException() {
        return provisionException;
    }

    public List<String> getProvisionList() {
        return provisionList;
    }

    public Properties getProvisionChecksums() {
        return provisionChecksums;
    }

    public String getProvisionStatus() {
        return provisionStatus;
    }

    public Map<String, String> getProvisionStatusMap() {
        return provisionStatusMap;
    }

    public CreateContainerMetadata getMetadata() {
        return metadata;
    }

    @Override
    public void setType(String type) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setAlive(boolean flag) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setJolokiaUrl(String location) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setVersionId(String versionId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setVersion(Version version) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setProfiles(Profile[] profiles) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addProfiles(Profile... profiles) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeProfiles(String... profileIds) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setLocation(String location) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setGeoLocation(String geoLocation) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setHttpUrl(String location) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setResolver(String resolver) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setLocalIp(String localIp) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setLocalHostname(String localHostname) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setPublicIp(String publicIp) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setPublicHostname(String publicHostname) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setManualIp(String manualIp) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setMinimumPort(int port) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setMaximumPort(int port) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void start() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void start(boolean force) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void stop() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void stop(boolean force) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void destroy() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void destroy(boolean force) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setJmxDomains(List<String> jmxDomains) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setProvisionResult(String result) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setProvisionException(String exception) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setProvisionList(List<String> bundles) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setProvisionChecksums(Properties checksums) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String toString() {
        return "ImmutableContainerImpl[" +
                "id='" + id + '\'' +
                ']';
    }
}
