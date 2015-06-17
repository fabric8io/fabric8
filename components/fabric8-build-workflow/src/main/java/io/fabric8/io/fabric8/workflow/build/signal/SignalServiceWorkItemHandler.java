/**
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.fabric8.io.fabric8.workflow.build.signal;

import io.fabric8.io.fabric8.workflow.build.trigger.BuildWorkItemHandler;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.process.WorkItem;
import org.kie.api.runtime.process.WorkItemHandler;
import org.kie.api.runtime.process.WorkItemManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Starts up the {@link io.fabric8.io.fabric8.workflow.build.signal.BuildSignallerService} to ensure its running
 */
public class SignalServiceWorkItemHandler implements WorkItemHandler {
    private static final transient Logger LOG = LoggerFactory.getLogger(BuildWorkItemHandler.class);

    private KieSession ksession;

    public SignalServiceWorkItemHandler(KieSession ksession) {
        this.ksession = ksession;
    }

    public void executeWorkItem(WorkItem workItem, WorkItemManager manager) {
        BuildSignallerService signalService = new BuildSignallerService(ksession);

        LOG.info("Starting the BuildSignallerService");
        signalService.start();
        LOG.info("Started the BuildSignallerService");
    }

    public void abortWorkItem(WorkItem workItem, WorkItemManager manager) {
    }

    public KieSession getKsession() {
        return ksession;
    }

    public void setKsession(KieSession ksession) {
        this.ksession = ksession;
    }


}
