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
 */
public class XmlAuditPolicy implements AuditPolicy {

    @Override
    public boolean isAuditEnabled(CamelContextService service) {
        // for now lets just apply on all services
        return true;
    }

    @Override
    public AuditEventNotifier createAuditNotifier(CamelContextService service) {
        // TODO
        return null;
    }

    @Override
    public void configureNotifier(CamelContextService service, AuditEventNotifier notifier) {
        // TODO

    }

    @Override
    public void setAgent(BAIAgent agent) {
        // TODO

    }
}
