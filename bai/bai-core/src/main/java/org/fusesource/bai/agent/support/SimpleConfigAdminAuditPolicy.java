/*
 * Copyright (C) FuseSource, Inc.
 * http://fusesource.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.fusesource.bai.agent.support;

import org.fusesource.bai.AuditEventNotifier;
import org.fusesource.bai.support.EventTypeConfigurationSet;
import org.fusesource.bai.agent.CamelContextService;
import org.osgi.service.cm.ConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * A simple implementation of {@link ConfigAdminAuditPolicySupport} which does not have any kind of policy model
 */
public class SimpleConfigAdminAuditPolicy extends ConfigAdminAuditPolicySupport {
    private static final transient Logger LOG = LoggerFactory.getLogger(SimpleConfigAdminAuditPolicy.class);

    private SortedMap<String, String> map = new TreeMap<String, String>();

    @Override
    public void updated(Dictionary dict) throws ConfigurationException {
        map.clear();
        Enumeration e = dict.keys();
        while (e.hasMoreElements()) {
            Object o = e.nextElement();
            if (o != null) {
                Object value = dict.get(o);
                String key = o.toString();
                if (value != null) {
                    map.put(key, value.toString());
                }
            }
        }
        String pattern = getOrElse(map, KEY_CAMEL_CONTEXT_EXCLUDE, DEFAULT_EXCLUDE_CAMEL_CONTEXT_FILTER);
        System.out.println("Setting the camelContext exclude pattern to " + pattern);
        setExcludeCamelContextPattern(pattern);
        updateNotifiersWithNewPolicy();
    }

    @Override
    public void configureNotifier(CamelContextService camelContextService, AuditEventNotifier notifier) {
        //System.out.println("Updating AuditEventNotifier for " + camelContextService.getDescription() + " using: " + map);
        EventTypeConfigurationSet configs = new EventTypeConfigurationSet();


        Set<Map.Entry<String, String>> entries = map.entrySet();
        for (Map.Entry<String, String> entry : entries) {
            String key = entry.getKey();
            String value = entry.getValue();
            configs.configureValue(camelContextService, key, value);
        }
        System.out.println("Updating configuration for " + camelContextService.getDescription() + " to " + configs);
        notifier.configure(configs);
    }

    public static String getOrElse(Map<String,String> map, String key, String defaultValue) {
        Object value = map.get(key);
        if (value == null) {
            return defaultValue;
        } else {
            return value.toString();
        }
    }

}
