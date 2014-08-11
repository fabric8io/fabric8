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

import io.fabric8.api.Builder;
import io.fabric8.api.Container;
import io.fabric8.api.CreateContainerMetadata;
import io.fabric8.api.FabricService;
import io.fabric8.api.OptionsProvider;
import io.fabric8.api.Profile;
import io.fabric8.api.Version;

import java.util.List;
import java.util.Map;
import java.util.Properties;

public class ImmutableContainerBuilder implements Builder<ImmutableContainerBuilder> {

    private String id;
    private String type;
    private Container parent;
    private Profile overlayProfile;
    private FabricService fabricService;
    private boolean alive;
    private boolean aliveAndOK;
    private boolean ensembleServer;
    private boolean root;
    private boolean managed;
    private String httpUrl;
    private String sshUrl;
    private String jmxUrl;
    private String jolokiaUrl;
    private String debugPort;
    private String versionId;
    private Version version;
    private Long processId;
    private Profile[] profiles;
    private String location;
    private String geoLocation;
    private String resolver;
    private String ip;
    private String localIp;
    private String localHostname;
    private String publicIp;
    private String publicHostname;
    private String manualIp;
    private int minimumPort;
    private int maximumPort;
    private Container[] children;
    private List<String> jmxDomains;
    private boolean provisioningComplete;
    private boolean provisioningPending;
    private String provisionResult;
    private String provisionException;
    private List<String> provisionList;
    private Properties provisionChecksums;
    private String provisionStatus;
    private Map<String, String> provisionStatusMap;
    private CreateContainerMetadata metadata;

    public ImmutableContainerBuilder id(final String id) {
        this.id = id;
        return this;
    }

    public ImmutableContainerBuilder type(final String type) {
        this.type = type;
        return this;
    }

    public ImmutableContainerBuilder parent(final Container parent) {
        this.parent = parent;
        return this;
    }

    public ImmutableContainerBuilder overlayProfile(final Profile overlayProfile) {
        this.overlayProfile = overlayProfile;
        return this;
    }

    public ImmutableContainerBuilder fabricService(final FabricService fabricService) {
        this.fabricService = fabricService;
        return this;
    }

    public ImmutableContainerBuilder alive(final boolean alive) {
        this.alive = alive;
        return this;
    }

    public ImmutableContainerBuilder aliveAndOK(final boolean aliveAndOK) {
        this.aliveAndOK = aliveAndOK;
        return this;
    }

    public ImmutableContainerBuilder ensembleServer(final boolean ensembleServer) {
        this.ensembleServer = ensembleServer;
        return this;
    }

    public ImmutableContainerBuilder root(final boolean root) {
        this.root = root;
        return this;
    }

    public ImmutableContainerBuilder managed(final boolean managed) {
        this.managed = managed;
        return this;
    }

    public ImmutableContainerBuilder httpUrl(final String httpUrl) {
        this.httpUrl = httpUrl;
        return this;
    }

    public ImmutableContainerBuilder sshUrl(final String sshUrl) {
        this.sshUrl = sshUrl;
        return this;
    }

    public ImmutableContainerBuilder jmxUrl(final String jmxUrl) {
        this.jmxUrl = jmxUrl;
        return this;
    }

    public ImmutableContainerBuilder jolokiaUrl(final String jolokiaUrl) {
        this.jolokiaUrl = jolokiaUrl;
        return this;
    }

    public ImmutableContainerBuilder debugPort(final String debugPort) {
        this.debugPort = debugPort;
        return this;
    }

    public ImmutableContainerBuilder versionId(final String versionId) {
        this.versionId = versionId;
        return this;
    }

    public ImmutableContainerBuilder version(final Version version) {
        this.version = version;
        return this;
    }

    public ImmutableContainerBuilder processId(final Long processId) {
        this.processId = processId;
        return this;
    }

    public ImmutableContainerBuilder profiles(final Profile[] profiles) {
        this.profiles = profiles;
        return this;
    }

    public ImmutableContainerBuilder location(final String location) {
        this.location = location;
        return this;
    }

    public ImmutableContainerBuilder geoLocation(final String geoLocation) {
        this.geoLocation = geoLocation;
        return this;
    }

    public ImmutableContainerBuilder resolver(final String resolver) {
        this.resolver = resolver;
        return this;
    }

    public ImmutableContainerBuilder ip(final String ip) {
        this.ip = ip;
        return this;
    }

    public ImmutableContainerBuilder localIp(final String localIp) {
        this.localIp = localIp;
        return this;
    }

    public ImmutableContainerBuilder localHostname(final String localHostname) {
        this.localHostname = localHostname;
        return this;
    }

    public ImmutableContainerBuilder publicIp(final String publicIp) {
        this.publicIp = publicIp;
        return this;
    }

    public ImmutableContainerBuilder publicHostname(final String publicHostname) {
        this.publicHostname = publicHostname;
        return this;
    }

    public ImmutableContainerBuilder manualIp(final String manualIp) {
        this.manualIp = manualIp;
        return this;
    }

    public ImmutableContainerBuilder minimumPort(final int minimumPort) {
        this.minimumPort = minimumPort;
        return this;
    }

    public ImmutableContainerBuilder maximumPort(final int maximumPort) {
        this.maximumPort = maximumPort;
        return this;
    }

    public ImmutableContainerBuilder children(final Container[] children) {
        this.children = children;
        return this;
    }

    public ImmutableContainerBuilder jmxDomains(final List<String> jmxDomains) {
        this.jmxDomains = jmxDomains;
        return this;
    }

    public ImmutableContainerBuilder provisioningComplete(final boolean provisioningComplete) {
        this.provisioningComplete = provisioningComplete;
        return this;
    }

    public ImmutableContainerBuilder provisioningPending(final boolean provisioningPending) {
        this.provisioningPending = provisioningPending;
        return this;
    }

    public ImmutableContainerBuilder provisionResult(final String provisionResult) {
        this.provisionResult = provisionResult;
        return this;
    }

    public ImmutableContainerBuilder provisionException(final String provisionException) {
        this.provisionException = provisionException;
        return this;
    }

    public ImmutableContainerBuilder provisionList(final List<String> provisionList) {
        this.provisionList = provisionList;
        return this;
    }

    public ImmutableContainerBuilder provisionChecksums(final Properties provisionChecksums) {
        this.provisionChecksums = provisionChecksums;
        return this;
    }

    public ImmutableContainerBuilder provisionStatus(final String provisionStatus) {
        this.provisionStatus = provisionStatus;
        return this;
    }

    public ImmutableContainerBuilder provisionStatusMap(final Map<String, String> provisionStatusMap) {
        this.provisionStatusMap = provisionStatusMap;
        return this;
    }

    public ImmutableContainerBuilder metadata(final CreateContainerMetadata metadata) {
        this.metadata = metadata;
        return this;
    }


    @Override
    public ImmutableContainerBuilder addOptions(OptionsProvider<ImmutableContainerBuilder> optionsProvider) {
        return this;
    }

    /**
     *  private boolean root;
     private boolean managed;
     private String httpUrl;
     private String sshUrl;
     private String jmxUrl;
     private String jolokiaUrl;
     private String debugPort;
     private String versionId;
     private Version version;
     private Long processId;
     private Profile[] profiles;
     private String location;
     private String geoLocation;
     private String resolver;
     private String ip;
     private String localIp;
     private String localHostname;
     private String publicIp;
     private String publicHostname;
     private String manualIp;
     private int minimumPort;
     private int maximumPort;
     private Container[] children;
     private List<String> jmxDomains;
     private boolean provisioningComplete;
     private boolean provisioningPending;
     private String provisionResult;
     private String provisionException;
     private List<String> provisionList;
     private Properties provisionChecksums;
     private String provisionStatus;
     private Map<String, String> provisionStatusMap;
     private CreateContainerMetadata metadata;
     * @return
     */
    
    public ImmutableContainerImpl build() {
      return new ImmutableContainerImpl(id,type,parent, overlayProfile,fabricService, alive, aliveAndOK, ensembleServer,
                root, managed, httpUrl, sshUrl, jmxUrl, jolokiaUrl, debugPort, versionId, version, processId, profiles,
                location, geoLocation, resolver, ip, localIp, localHostname, publicIp, publicHostname, manualIp,
                minimumPort, maximumPort, children, jmxDomains, provisioningComplete, provisioningPending, provisionResult,
                provisionException, provisionList, provisionChecksums, provisionStatus, provisionStatusMap, metadata);
    }
}
