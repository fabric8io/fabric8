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
import org.fusesource.bai.agent.AuditPolicy;
import org.fusesource.bai.agent.BAIAgent;
import org.fusesource.bai.agent.CamelContextService;

/**
 * A default implementation of {@link AuditPolicy} which filters out any audit elements
 */
public abstract class DefaultAuditPolicy implements AuditPolicy {
    private BAIAgent agent;


    // Properties
    //-------------------------------------------------------------------------

    @Override
    public AuditEventNotifier createAuditNotifier(CamelContextService service) {
        AuditEventNotifier notifier = new AuditEventNotifier();
        return notifier;
    }

    /**
     * Strategy method to allow derived implementations to override how to configure the notifier
     */
    @Override
    public void configureNotifier(CamelContextService service, AuditEventNotifier notifier) {
    }


    public BAIAgent getAgent() {
        return agent;
    }

    @Override
    public void setAgent(BAIAgent agent) {
        this.agent = agent;
    }

    // Implementation methods
    //-------------------------------------------------------------------------

    /**
     * Whenever a policy changes we can update all the active policies
     */
    protected void updateNotifiersWithNewPolicy() {
        if (agent != null) {
            agent.reconfigureNotifiers();
        }
    }

}
