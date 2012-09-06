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

import java.util.EventObject;

import org.apache.camel.management.event.AbstractExchangeEvent;
import org.fusesource.bai.config.AuditConfig;

/**
 * An Auditor which uses an XML/JSON {@link AuditConfig} to define its filters
 */
public class Auditor extends AuditEventNotifierSupport {
    private AuditConfig config;

    public Auditor() {
    }

    public Auditor(AuditConfig config) {
        setConfig(config);
    }

    @Override
    public String toString() {
        return "Auditor(" + config + ")";
    }

    public AuditConfig getConfig() {
        return config;
    }

    public void setConfig(AuditConfig config) {
        this.config = config;
        this.setEndpointUri(config.getEndpointUri());
    }


    // Implementation methods
    //-------------------------------------------------------------------------

    @Override
    protected Object createPayload(AuditEvent auditEvent) {
        return config.createPayload(auditEvent);

    }

    @Override
    protected boolean isEnabledFor(EventObject coreEvent, AbstractExchangeEvent exchangeEvent) {
        return config.isEnabled(coreEvent, exchangeEvent);
    }

}
