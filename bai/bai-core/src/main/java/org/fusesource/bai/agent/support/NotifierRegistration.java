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

import org.apache.camel.spi.EventNotifier;
import org.apache.camel.spi.ManagementStrategy;
import org.apache.camel.support.ServiceSupport;
import org.apache.camel.util.ServiceHelper;
import org.fusesource.bai.AuditEventNotifier;
import org.fusesource.bai.agent.CamelContextService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A service that represents the registration of an {@link EventNotifier} to ensure that only started notifiers are added
 * and they are stopped after removal
 */
public class NotifierRegistration extends ServiceSupport {
    private static final transient Logger LOG = LoggerFactory.getLogger(NotifierRegistration.class);

    private final String id;
    private final CamelContextService camelContextService;
    private final AuditEventNotifier notifier;
    private final ManagementStrategy managementStrategy;

    public NotifierRegistration(String id, CamelContextService camelContextService, AuditEventNotifier notifier, ManagementStrategy managementStrategy) {
        this.id = id;
        this.camelContextService = camelContextService;
        this.notifier = notifier;
        this.managementStrategy = managementStrategy;
    }

    @Override
    public String toString() {
        return "NotifierRegistration(" + id + ", " + notifier + ", " + managementStrategy + ")";
    }

    public String getId() {
        return id;
    }

    public CamelContextService getCamelContextService() {
        return camelContextService;
    }

    public ManagementStrategy getManagementStrategy() {
        return managementStrategy;
    }

    public AuditEventNotifier getNotifier() {
        return notifier;
    }

    @Override
    protected void doStart() throws Exception {
        LOG.info("Starting notifier " + notifier);
        ServiceHelper.startService(notifier);
        managementStrategy.addEventNotifier(notifier);
    }

    @Override
    protected void doStop() throws Exception {
        LOG.info("Stopping " + notifier);
        managementStrategy.removeEventNotifier(notifier);
        ServiceHelper.stopAndShutdownService(notifier);
    }
}
