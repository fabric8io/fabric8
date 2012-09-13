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

import org.fusesource.bai.agent.CamelContextService;
import org.fusesource.bai.config.PolicySet;
import org.fusesource.bai.xml.PolicySetPropertiesSlurper;
import org.osgi.service.cm.ConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Dictionary;

/**
 * Audit Policy Injecter which uses the Policy Model constructed by {@link org.fusesource.bai.xml.PolicySetPropertiesSlurper}.
 *
 * @author Raul Kripalani
 */
@SuppressWarnings("rawtypes")
public class ConfigAdminAuditPolicy extends ConfigAdminAuditPolicySupport {
    private static final transient Logger LOG = LoggerFactory.getLogger(ConfigAdminAuditPolicy.class);

    private Dictionary cachedConfig;
    private FileWatcher fileWatcher = new FileWatcher() {

        @Override
        protected void onFileChange(File file) {
            if (cachedConfig != null) {
                try {
                    updated(cachedConfig);
                } catch (ConfigurationException e) {
                    LOG.error("Failed to update configuration due to file change of " + file + ". Reason: " + e, e);
                }
            }
        }
    };

    @Override
    public void updated(Dictionary dict) throws ConfigurationException {
        this.cachedConfig = dict;
        LOG.info("Updating BAI ConfigAdmin from " + dict);
        try {
            OsgiPropertiesPolicySetSlurper slurper = new OsgiPropertiesPolicySetSlurper(dict, getBundleContext(), fileWatcher);
            setPolicySet(slurper.slurpXml());
        } catch (Exception e) {
            throw new ConfigurationException(OsgiPropertiesPolicySetSlurper.BAI_XML_PROPERTY, e.getMessage(), e);
        }
    }

    @Override
    public void destroy() {
        fileWatcher.destroy();
        super.destroy();
    }
}
