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

import io.fabric8.io.fabric8.workflow.build.BuildCorrelationKey;
import io.fabric8.io.fabric8.workflow.build.correlate.BuildProcessCorrelator;
import io.fabric8.kubernetes.api.builds.BuildFinishedEvent;
import io.fabric8.kubernetes.api.builds.BuildListener;
import org.kie.api.KieBase;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.manager.RuntimeEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

//import org.jbpm.ruleflow.core.RuleFlowProcess;

/**
 * Listens to {@link BuildFinishedEvent} events from the OpenShift build watcher and then
 * signals the correlated jBPM process instances or signals new processes to start
 */
public class BuildSignaller implements BuildListener {
    private static final transient Logger LOG = LoggerFactory.getLogger(BuildSignaller.class);

    private final KieBase kbase;
    private final RuntimeEngine engine;
    private final BuildProcessCorrelator buildProcessCorrelator;
    private final KieSession ksession;

    public BuildSignaller(KieBase kbase, RuntimeEngine engine, BuildProcessCorrelator buildProcessCorrelator) {
        this.kbase = kbase;
        this.engine = engine;
        this.buildProcessCorrelator = buildProcessCorrelator;
        ksession = engine.getKieSession();
    }

    @Override
    public void onBuildFinished(BuildFinishedEvent event) {
        String buildName = event.getConfigName();
        String buildUuid = event.getUid();
        String buildLink = event.getBuildLink();

        System.out.println("Build: " + buildUuid
                + " for config: " + buildName
                + " finished. Status: " + event.getStatus()
                + " link: " + buildLink);


        BuildCorrelationKey key = BuildCorrelationKey.create(event);

        Map<String, String> signalObject = new HashMap<>();
        signalObject.put("buildUuid", buildUuid);
        signalObject.put("buildLink", buildLink);

        Long processId = buildProcessCorrelator.findProcessInstanceIdForBuild(key);
        if (processId == null) {
            LOG.info("No existing processes associated with build " + key + " so lets signal a new process to start");
            ksession.signalEvent(buildName, signalObject);
        } else {
            LOG.info("Signalling event on process id: " + processId + " for " + key + " with data: " + signalObject);
            ksession.signalEvent(buildName, signalObject, processId);
        }
    }
}
