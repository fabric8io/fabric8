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
package io.fabric8.workflow.build.signal;

import io.fabric8.workflow.build.BuildCorrelationKey;
import io.fabric8.workflow.build.correlate.BuildProcessCorrelator;
import io.fabric8.workflow.build.dto.BuildFinishedDTO;
import io.fabric8.kubernetes.api.builds.BuildFinishedEvent;
import io.fabric8.kubernetes.api.builds.BuildListener;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.process.ProcessInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Listens to {@link BuildFinishedEvent} events from the OpenShift build watcher and then
 * signals the correlated jBPM process instances or signals new processes to start
 */
public class BuildSignaller implements BuildListener {
    private static final transient Logger LOG = LoggerFactory.getLogger(BuildSignaller.class);

    private final KieSession ksession;
    private final BuildProcessCorrelator buildProcessCorrelator;
    private String startBuildProcessId = "io.fabric8.cdelivery.StartBuildProcess";

    public BuildSignaller(KieSession ksession, BuildProcessCorrelator buildProcessCorrelator) {
        this.ksession = ksession;
        this.buildProcessCorrelator = buildProcessCorrelator;
    }

    @Override
    public void onBuildFinished(BuildFinishedEvent event) {
        String namespace = event.getNamespace();
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

        BuildFinishedDTO buildFinishedDTO = new BuildFinishedDTO(event);

        Long workItemId = buildProcessCorrelator.findWorkItemIdForBuild(key);
        if (workItemId == null) {
            String startNodeName = getStartSignalName(namespace, buildName);
            LOG.info("No existing processes associated with build " + key + " so lets signal a new process to start");
            Map<String, Object> inputParameters = new HashMap<>();
            inputParameters.put("startSignalName", startNodeName);
            LOG.info("Starting process: " + startBuildProcessId + " with inputParameters: " + inputParameters);
            ProcessInstance processInstance = ksession.startProcess(startBuildProcessId, inputParameters);
        } else {
            //ksession.signalEvent(buildName, signalObject, workItemId);
            Map<String, Object> results = new HashMap<>();
            //results.put("response", buildFinishedDTO);
            populateParameters(results, buildFinishedDTO);

            LOG.info("Completing work item id: " + workItemId + " for " + key + " with data: " + results);
            try {
                ksession.getWorkItemManager().completeWorkItem(workItemId, results);
            } catch (Exception e) {
                LOG.error("Could not complete work item " + workItemId + " for " + key + " with data: " + results + ". Reason: " + e, e);
            }
        }
    }

    protected static void populateParameters(Map<String, Object> parameters, BuildFinishedDTO buildFinishedDTO) {
        parameters.put("namespace", buildFinishedDTO.getNamespace());
        parameters.put("buildName", buildFinishedDTO.getBuildName());
        parameters.put("buildUuid", buildFinishedDTO.getBuildUuid());
        parameters.put("buildLink", buildFinishedDTO.getBuildLink());
        parameters.put("status", buildFinishedDTO.getStatus());
    }

    /**
     * Returns the start signal name for the namespace and build name
     */
    public static String getStartSignalName(String namespace, String buildName) {
        return namespace + "/" + buildName;
    }
}
