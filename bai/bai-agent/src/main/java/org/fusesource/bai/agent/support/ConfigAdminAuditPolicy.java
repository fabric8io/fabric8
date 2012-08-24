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

import java.util.Dictionary;
import java.util.Hashtable;

import org.fusesource.bai.AuditEventNotifier;
import org.fusesource.bai.agent.CamelContextService;
import org.fusesource.bai.model.policy.Constants.FilterElement;
import org.fusesource.bai.model.policy.PolicySet;
import org.fusesource.bai.model.policy.slurper.PropertyMapPolicySlurper;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 */
public class ConfigAdminAuditPolicy extends DefaultAuditPolicy {
    private static final transient Logger LOG = LoggerFactory.getLogger(ConfigAdminAuditPolicy.class);

    public final String KEY_CAMEL_CONTEXT_EXCLUDE = "camelContext.exclude";

    private String configPid = "org.fusesource.bai.agent";
    private ConfigurationAdmin configurationAdmin;
    private PolicySet policies = null;
    
    public void init() throws Exception {
        if (configurationAdmin != null) {
            Configuration config = configurationAdmin.getConfiguration(configPid);
            if (config != null) {
                Dictionary properties = config.getProperties();
                if (properties == null) {
                    // there are no configuration properties yet
                    properties = new Hashtable();
                }
                updated(properties);
            } else {
                LOG.warn("ConfigurationAdmin Configuration for " + configPid);
            }
        }
    }

    public void updated(Dictionary dict) throws ConfigurationException {
        System.out.println("Updating BAI Agent configuration " + dict);
        PropertyMapPolicySlurper pmps = new PropertyMapPolicySlurper(dict);
        this.policies = pmps.slurp();
        // obtain all policies whose scope is only a Context element, and whose resulting action is 'exclude'
        PolicySet excludedCamelContextsPolicies = policies.queryPolicyWithSingleScope(FilterElement.CONTEXT).queryAllExclusions();
        if (excludedCamelContextsPolicies.size() > 1) {
        	throw new ConfigurationException("*", "Inconsistency in audit policy configuration");
        }
        
        if (excludedCamelContextsPolicies.size() == 0) {
        	setExcludeCamelContextPattern(DEFAULT_EXCLUDE_CAMEL_CONTEXT_FILTER);
        } else {
        	setExcludeCamelContextPattern(excludedCamelContextsPolicies.iterator().next().scope.get(0).enumValues);
        }   	
    }

    @Override
    public void configureNotifier(CamelContextService camelContextService, AuditEventNotifier notifier) {
        // TODO
        // apply the current policy to the given notifier given the notifier for the camelContextService
    }

    public static String getOrElse(Dictionary dict, String key, String defaultValue) {
        Object value = dict.get(key);
        if (value == null) {
            return defaultValue;
        } else {
            return value.toString();
        }
    }

    public ConfigurationAdmin getConfigurationAdmin() {
        return configurationAdmin;
    }

    public void setConfigurationAdmin(ConfigurationAdmin configurationAdmin) {
        this.configurationAdmin = configurationAdmin;
    }

    public String getConfigPid() {
        return configPid;
    }

    public void setConfigPid(String configPid) {
        this.configPid = configPid;
    }
}
