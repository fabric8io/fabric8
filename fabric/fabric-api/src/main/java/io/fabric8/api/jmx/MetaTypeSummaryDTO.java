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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.osgi.framework.Bundle;
import org.osgi.service.metatype.MetaTypeInformation;
import org.osgi.service.metatype.ObjectClassDefinition;

/**
 */
public class MetaTypeSummaryDTO {
    private Map<String, MetaTypeObjectSummaryDTO> pids = new HashMap<String, MetaTypeObjectSummaryDTO>();
    private Set<String> locales = new HashSet<String>();

    public void addTypeInformation(Bundle bundle, MetaTypeInformation info) {
        addBundleInfo(bundle, info, info.getFactoryPids(), true);
        addBundleInfo(bundle, info, info.getPids(), false);
        String[] localeArray = info.getLocales();
        if (localeArray != null) {
            for (String locale : localeArray) {
                locales.add(locale);
            }
        }
    }

    public Map<String, MetaTypeObjectSummaryDTO> getPids() {
        return pids;
    }

    public void setPids(Map<String, MetaTypeObjectSummaryDTO> pids) {
        this.pids = pids;
    }

    public Set<String> getLocales() {
        return locales;
    }

    private void addBundleInfo(Bundle bundle, MetaTypeInformation info, String[] pids, boolean factory) {
        String locale = null;
        if (pids != null) {
            long bundleId = bundle.getBundleId();
            for (String pid : pids) {
                MetaTypeObjectSummaryDTO summary = getOrCreateMetaTypeSummaryDTO(pid);
                if (factory) {
                    summary.getFactoryPidBundleIds().add(bundleId);
                } else {
                    summary.getPidBundleIds().add(bundleId);
                }
                ObjectClassDefinition objectClassDefinition = MetaTypeSummaryDTO.tryGetObjectClassDefinition(info, pid, locale);
                if (objectClassDefinition != null) {
                    summary.appendObjectDefinition(objectClassDefinition);
                }
            }
        }
    }

    public MetaTypeObjectSummaryDTO getOrCreateMetaTypeSummaryDTO(String pid) {
        MetaTypeObjectSummaryDTO summary = this.pids.get(pid);
        if (summary == null) {
            summary = new MetaTypeObjectSummaryDTO(pid);
            this.pids.put(pid, summary);
        }
        return summary;
    }

    /**
     * Attempts to get the object definition ignoring any failures of missing declarations
     */
    public static ObjectClassDefinition tryGetObjectClassDefinition(MetaTypeInformation info, String pid, String locale) {
        ObjectClassDefinition object = null;
        try {
            object = info.getObjectClassDefinition(pid, locale);
        } catch (Exception e) {
            // ignore missing definition
        }
        return object;
    }
}
