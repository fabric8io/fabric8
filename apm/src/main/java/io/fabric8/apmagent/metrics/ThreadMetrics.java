/*
 * Copyright 2005-2014 Red Hat, Inc.
 * Red Hat licenses this file to you under the Apache License, version
 * 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *    http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */

package io.fabric8.apmagent.metrics;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;

public class ThreadMetrics {

  private final ApmAgentContext apmAgentContext;
  private final ThreadInfo threadInfo;
  private final Thread thread;
  private final ThreadContextMethodMetrics root;
  private final ThreadMXBean threadMXBean;

  public ThreadMetrics(ApmAgentContext apmAgentContext, Thread thread) {
    this.apmAgentContext = apmAgentContext;
    this.threadMXBean = ManagementFactory.getThreadMXBean();
    this.threadInfo = threadMXBean.getThreadInfo(thread.getId());
    this.thread = thread;
    this.root = new ThreadContextMethodMetrics(apmAgentContext, thread, null, new ThreadContextMethodMetricsStack(), "java.lang.thread@run");
    this.apmAgentContext.registerThreadContextMethodMetricsMBean(root);
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

  public void enter(String methodName) {
    root.onMethodEnter(methodName);
  }

  public long exit(String methodName) {
    return root.onMethodExit(methodName);
  }

  public String toString() {
    return "ThreadMetrics:" + getName();
  }

  public ThreadContextMethodMetrics getRoot() {
    return root;
  }

  public void destroy() {
    ThreadContextMethodMetrics threadContextMethodMetrics = root;
    if (threadContextMethodMetrics != null) {
      threadContextMethodMetrics.destroy();
      apmAgentContext.unregisterThreadContextMethodMetricsMBean(threadContextMethodMetrics);
    }
  }
}
