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

import com.codahale.metrics.Timer;
import io.fabric8.apmagent.ApmAgent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.Map;

public class ThreadContextMethodMetrics extends MethodMetrics implements ThreadContextMethodMetricsMBean {
  private static final Logger LOG = LoggerFactory.getLogger(ThreadContextMethodMetrics.class);
  private final ApmAgentContext context;
  private final Thread thread;
  private final ThreadContextMethodMetrics parent;
  private final ThreadContextMethodMetricsStack stack;
  private final Map<String, ThreadContextMethodMetrics> children = new LinkedHashMap<String, ThreadContextMethodMetrics>();
  private Timer.Context timerContext;

  public ThreadContextMethodMetrics(ApmAgentContext context, Thread thread, ThreadContextMethodMetrics parent, ThreadContextMethodMetricsStack stack, String name) {
    super(name);
    this.context = context;
    this.thread = thread;
    this.parent = parent;
    this.stack = stack != null ? stack : new ThreadContextMethodMetricsStack();
  }

  public String getThreadName() {
    return thread.getName();
  }

  public long getThreadId() {
    return thread.getId();
  }

  public String getParentName() {
    return parent != null ? parent.getName() : "";
  }

  public void onMethodEnter(String name) {
    if (name.equals(getName())) {
      onEnter();
    } else {
      ThreadContextMethodMetrics last = stack.getLast();
      if (last == null) {
        //we are root
        last = this;
      }

      ThreadContextMethodMetrics methodMetrics = last.children.get(name);
      if (methodMetrics == null) {
        //direct descendant
        methodMetrics = new ThreadContextMethodMetrics(context, thread, last, stack, name);
        last.children.put(name, methodMetrics);
        context.registerThreadContextMethodMetricsMBean(methodMetrics);
      }
      methodMetrics.onEnter();
    }

  }

  public long onMethodExit(String methodName) {
    long elapsed = -1;
    ThreadContextMethodMetrics last = stack.pop();
    while (last != null) {
      long time = last.onExit(false);
      if (last.getName().equals(methodName)) {
        elapsed = time;
        break;
      }
      last = stack.pop();
    }
    return elapsed;
  }

  void onEnter() {
    timerContext = timer.time();
    stack.push(this);
    if (ApmAgent.INSTANCE.getConfiguration().isTrace()) {
      LOG.trace("Thread " + Thread.currentThread().getName() + " Method: " + getName() + " on enter STACK: " + stack);
    }
  }

  long onExit(boolean removeFromStack) {
    long elaspsed = -1;
    if (timerContext != null) {
      elaspsed = timerContext.stop();
    }
    if (removeFromStack) {
      stack.pop();
    }
    if (ApmAgent.INSTANCE.getConfiguration().isTrace()) {
      LOG.trace("Thread " + Thread.currentThread().getName() + " Method: " + getName() + " on exit STACK: " + stack);
    }
    return elaspsed;
  }

  public String toString() {
    return "ThreadContextMethodMetrics:" + getName();
  }

  public void destroy() {
    context.unregisterThreadContextMethodMetricsMBean(this);
    for (ThreadContextMethodMetrics threadContextMethodMetrics : children.values()) {
      threadContextMethodMetrics.destroy();
    }
  }

}

