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
package io.fabric8.jolokia.facade.dto;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class ContainerDTO {
    public String type;
    public String id;
    public String sshUrl;
    public String jmxUrl;
    public String httpUrl;
    public String jolokiaUrl;
    public String location;
    public String geoLocation;
    public String resolver;
    public String ip;
    public String localIp;
    public String publicIp;
    public String manualIp;
    public String localHostName;
    public String publicHostName;
    public String provisioningResult;
    public String provisioningException;
    public String provisionStatus;
    public boolean alive;
    public boolean ensembleServer;
    public boolean root;
    public boolean managed;
    public boolean provisioningComplete;
    public boolean provisioningPending;
    public boolean aliveAndOK;
    public int minimumPort;
    public int maximumPort;
    public String parent;
    public String version;
    public ContainerDTO overlayProfile;
    public Set<String> profiles;
    public Set<String> children;
    public List<String> jmxDomains;
    public List<String> provisionList;
    public Map<Object, Object> metaData;

    @Override
    public String toString() {
        return String.format("Container: { id: %s, type: %s, provisionStatus: %s, alive: %s, version: %s }",
                id,
                type,
                provisionStatus,
                alive,
                version);
    }
}
