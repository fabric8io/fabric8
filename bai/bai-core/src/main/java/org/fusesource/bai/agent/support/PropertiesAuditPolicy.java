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

import org.apache.camel.util.ObjectHelper;
import org.fusesource.bai.AuditEventNotifier;
import org.fusesource.bai.agent.CamelContextService;
import org.fusesource.bai.config.PolicySet;
import org.fusesource.bai.xml.PolicySetPropertiesSlurper;

import javax.annotation.PostConstruct;
import java.util.Properties;

/**
 * Uses a properties file to create the {@link PolicySet}
 */
public class PropertiesAuditPolicy extends DefaultAuditPolicy {
    private PolicySet policySet;
    private Properties properties = new Properties();

    @PostConstruct
    public void start() {
        Properties properties = getProperties();
        PolicySetPropertiesSlurper slurper = new PolicySetPropertiesSlurper(properties);
        this.policySet = slurper.getPolicies();
        System.out.println("Loaded policySet from properties: " + policySet);
    }

    @Override
    public void configureNotifier(CamelContextService service, AuditEventNotifier notifier) {
        ObjectHelper.notNull(policySet, "policySet");
        notifier.setPolicySet(policySet);
    }

    @Override
    public boolean isAuditEnabled(CamelContextService service) {
        return true;
    }

    public PolicySet getPolicySet() {
        return policySet;
    }

    public Properties getProperties() {
        return properties;
    }

    public void setProperties(Properties properties) {
        this.properties = properties;
    }
}
