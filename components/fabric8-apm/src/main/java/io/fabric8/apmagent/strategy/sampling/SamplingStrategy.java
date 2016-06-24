/**
 *  Copyright 2005-2016 Red Hat, Inc.
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
package io.fabric8.apmagent.strategy.sampling;

import io.fabric8.apmagent.ApmConfiguration;
import io.fabric8.apmagent.Strategy;
import io.fabric8.apmagent.metrics.ApmAgentContext;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class SamplingStrategy implements Strategy, Runnable {
    private static final long CLEANUP_INTERVAL = 1000;
    private ApmAgentContext context;
    private ApmConfiguration configuration;
    private AtomicBoolean initialized = new AtomicBoolean();
    private AtomicBoolean started = new AtomicBoolean();
    private Thread samplingThread;
    private final Map<Long, String> currentMethods = new HashMap<>();

    public SamplingStrategy(ApmAgentContext context) {
        this.context = context;
        this.configuration = context.getConfiguration();
    }

    @Override
    public void initialize() throws Exception {
        if (initialized.compareAndSet(false, true)) {
            samplingThread = new Thread(this, "Fabric8-ApmAgent-SamplingStrategy");
            samplingThread.setDaemon(true);
            configuration.addChangeListener(this);
        }
    }

    @Override
    public void start() throws Exception {
        if (started.compareAndSet(false, true)) {
            initialize();
            samplingThread.start();
        }

    }

    @Override
    public void stop() throws Exception {
        if (started.compareAndSet(true, false)) {

        }
    }

    @Override
    public void shutDown() throws Exception {
        if (initialized.compareAndSet(true, false)) {
            configuration.removeChangeListener(this);
            samplingThread = null;
        }
    }

    @Override
    public void configurationChanged() {

    }

    @Override
    public void run() {
        long lastTime = 0;
        while (started.get()) {
            try {
                for (Map.Entry<Thread, StackTraceElement[]> threadEntry : Thread.getAllStackTraces().entrySet()) {
                    if (threadEntry.getKey() != Thread.currentThread()) {
                        addMeasurement(threadEntry.getKey(), threadEntry.getValue());
                    }
                }
                long currentTime = System.currentTimeMillis();

                if ((currentTime - lastTime) > CLEANUP_INTERVAL) {
                    cleanup();
                    lastTime = currentTime;
                }
                Thread.sleep(configuration.getSamplingInterval());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private void cleanup() {
        List<ThreadInfo> removeList = null;
        ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
        for (Long id : currentMethods.keySet()) {
            ThreadInfo threadInfo = threadMXBean.getThreadInfo(id);
            if (threadInfo != null) {
                if (threadInfo.getThreadState() == Thread.State.TERMINATED) {
                    if (removeList == null) {
                        removeList = new ArrayList<>();
                    }
                    removeList.add(threadInfo);
                }
            }
        }
        if (removeList != null) {
            for (ThreadInfo threadInfo : removeList) {
                currentMethods.remove(threadInfo.getThreadId());
            }
        }
    }

    private void addMeasurement(Thread thread, StackTraceElement[] stackTraceElements) {
        if (thread != null && thread.isAlive() &&
                stackTraceElements != null && stackTraceElements.length > 0) {
            StackTraceElement topOfStack = stackTraceElements[0];
            String currentMethod = getCurrentMethod(topOfStack);
            if (configuration.isAudit(topOfStack.getClassName(), topOfStack.getMethodName())) {
                String lastMethod = currentMethods.put(thread.getId(), currentMethod);
                if (lastMethod == null) {
                    context.enterMethod(thread, currentMethod, true);
                } else if (!lastMethod.equals(currentMethod)) {
                    context.exitMethod(thread, lastMethod, true);
                } else {
                    //we are still in the currentMethod
                }
            }
        }
    }

    private String getCurrentMethod(StackTraceElement topOfStack) {
        StringBuilder stringBuilder = new StringBuilder(topOfStack.getClassName().length() + topOfStack.getMethodName().length() + 1);
        stringBuilder.append(topOfStack.getClassName()).append(".").append(topOfStack.getMethodName());
        return stringBuilder.toString();
    }
}

