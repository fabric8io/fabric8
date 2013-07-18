/*******************************************************************************
 * Copyright (c) 2013 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.fusesource.fabric.jolokia.facade.dto;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class ProfileDTO {
    public String id;
    public String version;
    public long lastModified;
    public boolean isOverlay;
    public boolean isAbstract;
    public boolean isLocked;
    public boolean isHidden;
    public Set<ProfileDTO> parents;
    public Map<String, String> attributes;
    public Map<String, Map<String, String>> configurations;
    public Map<String, byte[]> fileConfigurations;
    public Set<ContainerDTO> associatedContainers;
    public List<String> bundles;
    public List<String> fabs;
    public List<String> features;
    public List<String> repositories;
    public List<String> overrides;
}
