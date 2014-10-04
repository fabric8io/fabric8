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
