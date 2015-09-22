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

import io.fabric8.workflow.build.correlate.BuildProcessCorrelator;
import io.fabric8.workflow.build.correlate.BuildProcessCorrelators;
import io.fabric8.workflow.build.simulator.BuildSimulator;
import io.fabric8.kubernetes.api.builds.BuildWatcher;
import io.fabric8.kubernetes.api.builds.Links;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.openshift.client.DefaultOpenshiftClient;
import io.fabric8.openshift.client.OpenShiftClient;
import org.kie.api.runtime.KieSession;

import java.util.Timer;

/**
 * A service which listens to {@link io.fabric8.kubernetes.api.builds.BuildFinishedEvent} events from
 * the OpenShift build watcher and then signals the correlated jBPM process instances or
 * signals new processes to start.
 * <p/>
 * This service is a helper class to create a configured instance of a
 * {@link BuildSignaller} using the
 * {@link io.fabric8.kubernetes.api.builds.BuildWatcher} helper class.
 */
public class BuildSignallerService {
    private final KieSession ksession;
    private final String namespace;
    private OpenShiftClient client = new DefaultOpenshiftClient();
    private Timer timer = new Timer();
    private BuildProcessCorrelator buildProcessCorrelator = BuildProcessCorrelators.getSingleton();
    private BuildWatcher watcher;
    private BuildSimulator simulator;
    private String consoleLink;


    public BuildSignallerService(KieSession ksession) {
        this(ksession, null);
    }

    public BuildSignallerService(KieSession ksession, String namespace) {
        this.ksession = ksession;
        this.namespace = namespace;
    }

    public void start() {
        String consoleLink = getConsoleLink();
        BuildSignaller buildListener = new BuildSignaller(ksession, buildProcessCorrelator);

        long pollTime = 3000;

        if (BuildSimulator.isEnabled()) {
            simulator = BuildSimulator.getSingleton();
            simulator.schedule(timer, pollTime, buildListener, consoleLink);
        } else {
            watcher = new BuildWatcher(client, buildListener, namespace, consoleLink);
            watcher.schedule(timer, pollTime);
        }
    }

    public void stop() {
        timer.cancel();
    }

    public void join() {
        if (watcher != null) {
            watcher.join();
        } else if (simulator != null) {
            simulator.join();
        }
    }

    public BuildProcessCorrelator getBuildProcessCorrelator() {
        return buildProcessCorrelator;
    }

    public void setBuildProcessCorrelator(BuildProcessCorrelator buildProcessCorrelator) {
        this.buildProcessCorrelator = buildProcessCorrelator;
    }

    public OpenShiftClient getClient() {
        return client;
    }

    public void setClient(OpenShiftClient client) {
        this.client = client;
    }

    public Timer getTimer() {
        return timer;
    }

    public void setTimer(Timer timer) {
        this.timer = timer;
    }

    public String getConsoleLink() {
        if (consoleLink == null) {
            consoleLink = Links.getFabric8ConsoleLink();
        }
        return consoleLink;
    }

    public void setConsoleLink(String consoleLink) {
        this.consoleLink = consoleLink;
    }
}
