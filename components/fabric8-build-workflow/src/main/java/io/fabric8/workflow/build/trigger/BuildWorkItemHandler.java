/**
 *  Copyright 2005-2015 Red Hat, Inc.
 *
 *  Red Hat licenses this file to you under the Apache License, version
 *  2.0 (the "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 *  implied.  See the License for the specific language governing
 *  permissions and limitations under the License.
 */
package io.fabric8.workflow.build.trigger;

import io.fabric8.workflow.build.BuildCorrelationKey;
import io.fabric8.workflow.build.correlate.BuildProcessCorrelator;
import io.fabric8.workflow.build.correlate.BuildProcessCorrelators;
import io.fabric8.utils.Strings;
import org.kie.api.runtime.process.WorkItem;
import org.kie.api.runtime.process.WorkItemHandler;
import org.kie.api.runtime.process.WorkItemManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Invoked from inside a jBPM process to trigger a new build in OpenShift and register
 * the {@link BuildCorrelationKey}
 * of the new build into the {@link BuildProcessCorrelator}
 */
public class BuildWorkItemHandler implements WorkItemHandler {
    private static final transient Logger LOG = LoggerFactory.getLogger(BuildWorkItemHandler.class);

    private BuildProcessCorrelator buildProcessCorrelator = BuildProcessCorrelators.getSingleton();
    private BuildTrigger buildTrigger = BuildTriggers.getSingleton();

    public void executeWorkItem(WorkItem workItem, WorkItemManager manager) {
        long processInstanceId = workItem.getProcessInstanceId();

        long workItemId = workItem.getId();

        String namespace = WorkItemHandlers.getMandatoryParameter(workItem, manager, "namespace");
        String buildName = WorkItemHandlers.getMandatoryParameter(workItem, manager, "buildName");

        LOG.info("Executing build: " + namespace + "/" + buildName
                + " processInstanceId: " + processInstanceId + " workItemId: " + workItemId);

        String buildUuid = null;
        try {
            buildUuid = triggerBuild(processInstanceId, workItemId, namespace, buildName);
        } catch (Exception e) {
            WorkItemHandlers.fail(workItem, manager, "Could not trigger build for namespace: " + namespace + " build: " + buildName, e);
            return;
        }

        if (Strings.isNullOrBlank(buildUuid)) {
            WorkItemHandlers.fail(workItem, manager, "Could not trigger build for namespace: " + namespace + " build: " + buildName);
        }

        // TODO else you could trigger completion using:
        // POST http://localhost:8080/jbpm-console/rest/runtime/demo:Build:1.0/workitem/INSERT_WORKITEMID_HERE/complete?map_Outcome=Success&map_ResultUrl=www.jbpm.org
    }

    /**
     * API to be invoked from the remote REST API
     */
    public String triggerBuild(long processInstanceId, long workItemId, String namespace, String buildName) {
        String buildUuid;
        buildUuid = buildTrigger.trigger(namespace, buildName);
        LOG.info("Created " + buildUuid + " from build: " + namespace + "/" + buildName
                        + " processInstanceId: " + processInstanceId + " workItemId: " + workItemId);

        if (Strings.isNotBlank(buildUuid)) {
            BuildCorrelationKey key = new BuildCorrelationKey(namespace, buildName, buildUuid);
            buildProcessCorrelator.putBuildWorkItemId(key, workItemId);
        }
        return buildUuid;
    }

    public void abortWorkItem(WorkItem workItem, WorkItemManager manager) {
        System.out.println("Aborting " + workItem.getParameter("BuildId"));
    }

    public BuildTrigger getBuildTrigger() {
        return buildTrigger;
    }

    public void setBuildTrigger(BuildTrigger buildTrigger) {
        this.buildTrigger = buildTrigger;
    }

    public BuildProcessCorrelator getBuildProcessCorrelator() {
        return buildProcessCorrelator;
    }

    public void setBuildProcessCorrelator(BuildProcessCorrelator buildProcessCorrelator) {
        this.buildProcessCorrelator = buildProcessCorrelator;
    }
}
