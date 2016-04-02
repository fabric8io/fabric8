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
package io.fabric8.apmagent.metrics;

import io.fabric8.apmagent.ApmConfiguration;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReference;

public class ThreadMetrics {
    private final AtomicReference<ThreadContextMethodMetricsStack> methodStackRef;
    private final ApmAgentContext apmAgentContext;
    private final ThreadInfo threadInfo;
    private final Thread thread;
    private final ThreadMXBean threadMXBean;
    private final ConcurrentMap<String, ThreadContextMethodMetrics> methods = new ConcurrentHashMap<>();
    private final MonitoredThreadMethodMetrics monitoredThreadMethodMetrics;

    public ThreadMetrics(ApmAgentContext apmAgentContext, Thread thread) {
        this.methodStackRef = new AtomicReference<>(new ThreadContextMethodMetricsStack());
        this.apmAgentContext = apmAgentContext;
        this.threadMXBean = ManagementFactory.getThreadMXBean();
        this.threadInfo = threadMXBean.getThreadInfo(thread.getId());
        this.thread = thread;
        ApmConfiguration configuration = apmAgentContext.getConfiguration();
        this.monitoredThreadMethodMetrics = new MonitoredThreadMethodMetrics(thread, apmAgentContext);
        this.monitoredThreadMethodMetrics.setMonitorSize(configuration.getThreadMetricDepth());
    }

    public String getName() {
        return thread.getName() + "[" + thread.getId() + "]";
    }

    Thread getThread() {
        return thread;
    }

    public boolean isDead() {
        return !thread.isAlive();
    }

    public long getCpuTime() {
        return threadMXBean.getThreadCpuTime(thread.getId());
    }

    public long getUserTime() {
        return threadMXBean.getThreadUserTime(thread.getId());
    }

    public ThreadInfo getThreadInfo() {
        return threadInfo;
    }

    public void setMonitorSize(int monitorSize) {
        monitoredThreadMethodMetrics.setMonitorSize(monitorSize);
    }

    public void enter(String methodName, boolean alwaysActive) {
        ThreadContextMethodMetrics threadContextMethodMetrics = methods.get(methodName);
        if (threadContextMethodMetrics == null) {
            threadContextMethodMetrics = new ThreadContextMethodMetrics(thread, this.methodStackRef, methodName);
            threadContextMethodMetrics.setActive(apmAgentContext.isMonitorByDefault());
            methods.putIfAbsent(methodName, threadContextMethodMetrics);
        }
        if (alwaysActive || threadContextMethodMetrics.isActive()) {
            threadContextMethodMetrics.onEnter();
        }
    }

    public long exit(String methodName, boolean alwaysActive) {
        long result = -1;
        ThreadContextMethodMetrics threadContextMethodMetrics = methods.get(methodName);
        if (threadContextMethodMetrics != null) {
            if (alwaysActive || threadContextMethodMetrics.isActive()) {
                result = threadContextMethodMetrics.onExit();
            }
        } else {
            //something weird happended reset the stack
            methodStackRef.set(new ThreadContextMethodMetricsStack());
        }
        return result;
    }

    public String toString() {
        return "ThreadMetrics:" + getName();
    }

    public void destroy() {
        monitoredThreadMethodMetrics.destroy();
    }

    public ThreadContextMethodMetrics remove(String fullMethodName) {
        ThreadContextMethodMetrics result = methods.remove(fullMethodName);
        return result;
    }

    public void calculateMethodMetrics() {
        List<ThreadContextMethodMetrics> list = (List<ThreadContextMethodMetrics>) MethodMetrics.sortedMetrics(this.methods.values());
        monitoredThreadMethodMetrics.calculateMethodMetrics(list);
    }

    public void setActive(String methodName, boolean flag) {
        ThreadContextMethodMetrics threadContextMethodMetrics = methods.get(methodName);
        if (threadContextMethodMetrics != null) {
            threadContextMethodMetrics.setActive(flag);
        }
    }

    public boolean isActive(String methodName) {
        ThreadContextMethodMetrics threadContextMethodMetrics = methods.get(methodName);
        return threadContextMethodMetrics != null ? threadContextMethodMetrics.isActive() : false;
    }
}
