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
package io.fabric8.jolokia.facade.dto;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class ProfileDTO {
    public String id;
    public String version;
    public long lastModified;
    public boolean overlay;
    public boolean _abstract;
    public boolean locked;
    public boolean hidden;
    public Set<String> parents;
    public Map<String, String> attributes;
    public Set<String> configurations;
    public Set<String> fileConfigurations;
    public Set<String> associatedContainers;
    public List<String> bundles;
    public List<String> fabs;
    public List<String> features;
    public List<String> repositories;
    public List<String> overrides;

    @Override
    public String toString() {
        return String.format("Profile: { id: %s, version: %s, overlay: %s, abstract: %s, locked: %s, hidden: %s }",
                id,
                version,
                overlay,
                _abstract,
                locked,
                hidden);
    }
}
