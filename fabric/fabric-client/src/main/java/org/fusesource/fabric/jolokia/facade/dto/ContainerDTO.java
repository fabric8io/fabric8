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
package org.fusesource.fabric.jolokia.facade.dto;

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
    public ContainerDTO parent;
    public VersionDTO version;
    public ProfileDTO overlayProfile;
    public Set<ProfileDTO> profiles;
    public Set<ContainerDTO> children;
    public List<String> jmxDomains;
    public List<String> provisionList;
    public Map<Object, Object> metaData;
}
