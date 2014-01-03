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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 */
public class MetaTypeSummaryDTO {
    private Map<String, List<Long>> pidToBundleIds = new HashMap<String, List<Long>>();
    private Map<String, List<Long>> factoryPidToBundleIds = new HashMap<String, List<Long>>();
    private Set<String> locales = new HashSet<String>();


    public void addTypeInformation(Bundle bundle, MetaTypeInformation info) {
        long bundleId = bundle.getBundleId();

        addToMap(factoryPidToBundleIds, bundleId, info.getFactoryPids());
        addToMap(pidToBundleIds, bundleId, info.getPids());
        String[] localeArray = info.getLocales();
        if (localeArray != null) {
            for (String locale : localeArray) {
                locales.add(locale);
            }
        }
    }

    public Map<String, List<Long>> getPidToBundleIds() {
        return pidToBundleIds;
    }

    public Map<String, List<Long>> getFactoryPidToBundleIds() {
        return factoryPidToBundleIds;
    }

    public Set<String> getLocales() {
        return locales;
    }

    protected void addToMap(Map<String, List<Long>> map, long bundleId, String[] pids) {
        if (pids != null) {
            for (String pid : pids) {
                List<Long> list = map.get(pid);
                if (list == null) {
                    list = new ArrayList<Long>();
                    map.put(pid, list);
                }
                list.add(bundleId);
            }
        }
    }
}
