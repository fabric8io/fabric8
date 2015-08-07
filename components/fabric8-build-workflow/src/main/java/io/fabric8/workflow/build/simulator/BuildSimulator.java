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
package io.fabric8.workflow.build.simulator;

import io.fabric8.workflow.build.BuildCorrelationKey;
import io.fabric8.workflow.build.signal.BuildSignaller;
import io.fabric8.kubernetes.api.builds.BuildFinishedEvent;
import io.fabric8.utils.Strings;
import io.fabric8.utils.Systems;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicLong;

/**
 * This is used to simulate an OpenShift build environment so that you can test
 * out the jBPM workflow side of things without requiring an OpenShift environment
 * and lots of builds etc.
 */
public class BuildSimulator {
    public static final String FABRIC8_SIMULATOR_ENABLED = "FABRIC8_SIMULATOR_ENABLED";
    public static final String FABRIC8_SIMULATOR_START_BUILD_NAME = "FABRIC8_SIMULATOR_START_BUILD_NAME";

    private static BuildSimulator singleton;
    private AtomicLong buildCounter = new AtomicLong(0);
    private Timer timer;
    private long pollTime;
    private BuildSignaller buildListener;
    private String consoleLink;
    private String startBuildName = "A";
    private String startBuildNamespace = "default";
    private long initialBuildDelay = 1000;
    private long startBuildPeriod = 30 * 1000;

    /**
     * Returns true if the build simulator is enabled via the {@link FABRIC8_SIMULATOR_ENABLED}
     * environment variable or system property being "true"
     */
    public static boolean isEnabled() {
        String value = Systems.getEnvVarOrSystemProperty(FABRIC8_SIMULATOR_ENABLED, FABRIC8_SIMULATOR_ENABLED, "false");
        if (Strings.isNotBlank(value)) {
            return value.toLowerCase().equals("true");
        } else {
            return false;
        }
    }

    public static BuildSimulator getSingleton() {
        if (singleton == null) {
            singleton = new BuildSimulator();
        }
        return singleton;
    }

    public String triggerBuild(String namespace, String buildName) {
        String uuid = "" + buildCounter.incrementAndGet();
        final BuildCorrelationKey key = new BuildCorrelationKey(namespace, buildName, uuid);

        TimerTask notifyBuildTask = new TimerTask() {
            @Override
            public void run() {
                System.out.println("Triggering build for: " + key);
                notifyBuild(key);
            }
        };
        timer.schedule(notifyBuildTask, 2 * pollTime);
        return uuid;
    }

    public void schedule(Timer timer, long pollTime, BuildSignaller buildListener, String consoleLink) {
        this.timer = timer;
        this.pollTime = pollTime;
        this.buildListener = buildListener;
        this.consoleLink = consoleLink;

        startBuildName = Systems.getEnvVarOrSystemProperty(FABRIC8_SIMULATOR_START_BUILD_NAME, startBuildName);

        TimerTask startBuildTask = new TimerTask() {
            @Override
            public void run() {
                triggerBuild(startBuildNamespace, startBuildName);
            }
        };
        // lets trigger a start build every now and again
        timer.schedule(startBuildTask, initialBuildDelay, startBuildPeriod);
    }

    protected void notifyBuild(BuildCorrelationKey key) {
        BuildFinishedEvent event = new SimulatorBuildFinishedEvent(key, consoleLink);
        buildListener.onBuildFinished(event);
    }

    /**
     * Waits until this simulator is finished (which by default is forever)
     */
    public void join() {
        Object lock = new Object();
        while (true) {
            synchronized (lock) {
                try {
                    lock.wait();
                } catch (InterruptedException e) {
                    // ignore
                }
            }
        }
    }

    public String getStartBuildName() {
        return startBuildName;
    }

    public void setStartBuildName(String startBuildName) {
        this.startBuildName = startBuildName;
    }

    public String getStartBuildNamespace() {
        return startBuildNamespace;
    }

    public void setStartBuildNamespace(String startBuildNamespace) {
        this.startBuildNamespace = startBuildNamespace;
    }

    public long getStartBuildPeriod() {
        return startBuildPeriod;
    }

    public void setStartBuildPeriod(long startBuildPeriod) {
        this.startBuildPeriod = startBuildPeriod;
    }

    public long getInitialBuildDelay() {
        return initialBuildDelay;
    }

    public void setInitialBuildDelay(long initialBuildDelay) {
        this.initialBuildDelay = initialBuildDelay;
    }
}
