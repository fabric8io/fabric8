/*
 * ******************************************************************************
 *  * Copyright (c) 2013 Red Hat, Inc.
 *  * Distributed under license by Red Hat, Inc. All rights reserved.
 *  * This program is made available under the terms of the
 *  * Eclipse Public License v1.0 which accompanies this distribution,
 *  * and is available at http://www.eclipse.org/legal/epl-v10.html
 *  *
 *  * Contributors:
 *  *     Red Hat, Inc. - initial API and implementation
 *  *****************************************************************************
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
