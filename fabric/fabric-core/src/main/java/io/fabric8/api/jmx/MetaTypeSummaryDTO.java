/**
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.fabric8.api.jmx;

import org.osgi.framework.Bundle;
import org.osgi.service.metatype.MetaTypeInformation;
import org.osgi.service.metatype.ObjectClassDefinition;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

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
                MetaTypeObjectSummaryDTO summary = this.pids.get(pid);
                if (summary == null) {
                    summary = new MetaTypeObjectSummaryDTO(pid);
                    this.pids.put(pid, summary);
                }
                if (factory) {
                    summary.getFactoryPidBundleIds().add(bundleId);
                } else {
                    summary.getPidBundleIds().add(bundleId);
                }
                ObjectClassDefinition objectClassDefinition = MetaTypeFacade.tryGetObjectClassDefinition(info, pid, locale);
                if (objectClassDefinition != null) {
                    summary.appendObjectDefinition(objectClassDefinition);
                }
            }
        }
    }
}
