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
package org.fusesource.bai.agent;

import org.apache.camel.spi.EventNotifier;
import org.apache.camel.spi.ManagementStrategy;
import org.apache.camel.support.ServiceSupport;
import org.apache.camel.util.ServiceHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A service that represents the registration of an {@link EventNotifier} to ensure that only started notifiers are added
 * and they are stopped after removal
 */
public class NotifierRegistration extends ServiceSupport {
    private static final transient Logger LOG = LoggerFactory.getLogger(NotifierRegistration.class);

    private final String id;
    private final EventNotifier notifier;
    private final ManagementStrategy managementStrategy;

    public NotifierRegistration(String id, EventNotifier notifier, ManagementStrategy managementStrategy) {
        this.id = id;
        this.notifier = notifier;
        this.managementStrategy = managementStrategy;
    }

    @Override
    public String toString() {
        return "NotifierRegistration(" + id + ", " + notifier + ", " + managementStrategy + ")";
    }

    @Override
    protected void doStart() throws Exception {
        LOG.info("Starting notifier " + notifier + " for CamelContext: " + id);
        ServiceHelper.startService(notifier);
        managementStrategy.addEventNotifier(notifier);
    }

    @Override
    protected void doStop() throws Exception {
        LOG.info("Stopping " + notifier + " for CamelContext " + id);
        managementStrategy.removeEventNotifier(notifier);
        ServiceHelper.stopAndShutdownService(notifier);
    }
}
