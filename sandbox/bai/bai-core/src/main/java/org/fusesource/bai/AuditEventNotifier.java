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
package org.fusesource.bai;

import org.apache.camel.ProducerTemplate;
import org.apache.camel.management.event.AbstractExchangeEvent;
import org.apache.camel.util.ObjectHelper;
import org.apache.camel.util.ServiceHelper;
import org.fusesource.bai.config.Policy;
import org.fusesource.bai.config.PolicySet;

import java.util.EventObject;
import java.util.List;

/**
 * An Auditor which uses an XML/JSON {@link org.fusesource.bai.config.PolicySet} to define its filters
 */
public class AuditEventNotifier extends AuditEventNotifierSupport {
    private PolicySet policySet;
    private ProducerTemplate producerTemplate;
    private String name = "";

    public AuditEventNotifier() {
    }

    public AuditEventNotifier(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "Auditor(" + name + ")";
    }

    public PolicySet getPolicySet() {
        return policySet;
    }

    public void setPolicySet(PolicySet policySet) {
        this.policySet = policySet;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }


    // Implementation methods
    //-------------------------------------------------------------------------

    @Override
    protected boolean isEnabledFor(EventObject coreEvent, AbstractExchangeEvent exchangeEvent) {
        return policySet.isEnabled(coreEvent, exchangeEvent);
    }

    protected void processAuditEvent(AuditEvent auditEvent) throws Exception {
        List<Policy> policies = policySet.getPolicies();
        for (Policy policy : policies) {
            policy.process(this, auditEvent);
        }
    }

    public ProducerTemplate getProducerTemplate() {
        if (producerTemplate == null) {
            producerTemplate = getCamelContext().createProducerTemplate();
        }
        return producerTemplate;
    }

    @Override
    protected void doStart() throws Exception {
        ObjectHelper.notNull(camelContext, "camelContext", this);
        ServiceHelper.startService(getProducerTemplate());

    }

    @Override
    protected void doStop() throws Exception {
        ServiceHelper.stopService(producerTemplate);
    }
}
