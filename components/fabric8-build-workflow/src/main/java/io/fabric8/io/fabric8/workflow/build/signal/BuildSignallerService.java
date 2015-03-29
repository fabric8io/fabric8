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

import io.fabric8.io.fabric8.workflow.build.correlate.BuildProcessCorrelator;
import io.fabric8.io.fabric8.workflow.build.correlate.BuildProcessCorrelators;
import io.fabric8.kubernetes.api.KubernetesClient;
import io.fabric8.kubernetes.api.builds.BuildWatcher;
import io.fabric8.kubernetes.api.builds.Links;
import org.kie.api.KieBase;
import org.kie.api.runtime.manager.RuntimeEngine;

import java.util.Timer;

/**
 * A service which listens to {@link io.fabric8.kubernetes.api.builds.BuildFinishedEvent} events from
 * the OpenShift build watcher and then signals the correlated jBPM process instances or
 * signals new processes to start.
 *
 * This service is a helper class to create a configured instance of a
 * {@link io.fabric8.io.fabric8.workflow.build.signal.BuildSignaller} using the
 * {@link io.fabric8.kubernetes.api.builds.BuildWatcher} helper class.
 */
public class BuildSignallerService {
    private final KieBase kbase;
    private final RuntimeEngine engine;
    private KubernetesClient client = new KubernetesClient();
    private Timer timer = new Timer();
    private BuildProcessCorrelator buildProcessCorrelator = BuildProcessCorrelators.getSingleton();
    private BuildWatcher watcher;

    public BuildSignallerService(KieBase kbase, RuntimeEngine engine) {
        this.kbase = kbase;
        this.engine = engine;
    }

    public void start() {
        String consoleLink = Links.getFabric8ConsoleLink();
        String namespace = null;

        watcher = new BuildWatcher(client, new BuildSignaller(kbase, engine, buildProcessCorrelator), namespace, consoleLink);

        long pollTime = 3000;
        watcher.schedule(timer, pollTime);
    }

    public void stop() {
        timer.cancel();
    }

    public void join() {
        watcher.join();
    }

    public BuildProcessCorrelator getBuildProcessCorrelator() {
        return buildProcessCorrelator;
    }

    public void setBuildProcessCorrelator(BuildProcessCorrelator buildProcessCorrelator) {
        this.buildProcessCorrelator = buildProcessCorrelator;
    }

    public KubernetesClient getClient() {
        return client;
    }

    public void setClient(KubernetesClient client) {
        this.client = client;
    }

    public Timer getTimer() {
        return timer;
    }

    public void setTimer(Timer timer) {
        this.timer = timer;
    }
}
