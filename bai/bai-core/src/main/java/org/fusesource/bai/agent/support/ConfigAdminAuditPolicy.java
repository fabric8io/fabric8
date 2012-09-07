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
import org.fusesource.bai.agent.CamelContextService;
import org.fusesource.bai.config.*;
import org.fusesource.bai.xml.PropertyMapPolicySlurper;
import org.osgi.service.cm.ConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Dictionary;

/**
 * Audit Policy Injecter which uses the Policy Model constructed by {@link PropertyMapPolicySlurper}.
 *
 * @author Raul Kripalani
 */
@SuppressWarnings("rawtypes")
public class ConfigAdminAuditPolicy extends ConfigAdminAuditPolicySupport {
    private static final transient Logger LOG = LoggerFactory.getLogger(ConfigAdminAuditPolicy.class);

    private PolicySet policies;

    @Override
    public void updated(Dictionary dict) throws ConfigurationException {
        System.out.println("Updating BAI Agent configuration " + dict);
        PropertyMapPolicySlurper pmps = new PropertyMapPolicySlurper(dict);
        this.policies = pmps.slurp();
        updateNotifiersWithNewPolicy();
    }

    @Override
    public boolean isAuditEnabled(CamelContextService service) {
        // for now lets enable for all contexts
        return true;
    }

    /**
     * Apply the current policy to a notifier
     *
     * @author Raul Kripalani
     */
    @Override
    public void configureNotifier(CamelContextService camelContextService, AuditEventNotifier notifier) {
        LOG.info("Updating AuditEventNotifier " + notifier + " for bundle: " + camelContextService.getBundleSymbolicName() + " camelContext: " + camelContextService);

        PolicySet contextPolicy = policies.createConfig(camelContextService);
        notifier.setConfig(contextPolicy);
    }

}
