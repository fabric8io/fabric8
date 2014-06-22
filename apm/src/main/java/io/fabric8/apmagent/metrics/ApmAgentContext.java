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

import io.fabric8.apmagent.ApmAgent;
import io.fabric8.apmagent.ApmConfiguration;
import org.jolokia.jmx.JolokiaMBeanServerUtil;
import org.jolokia.jvmagent.JolokiaServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.MBeanServer;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class ApmAgentContext implements ApmAgentContextMBean {
  public static final ApmAgentContext INSTANCE = new ApmAgentContext();
  private static final Logger LOG = LoggerFactory.getLogger(ApmAgent.class);
  private final String DEFAULT_DOMAIN = "io.fabric8.apmagent";
  private final Set<String> transformedClasses = new HashSet<String>();
  private final Set<String> transformedMethods = new HashSet<String>();
  private AtomicBoolean initialized = new AtomicBoolean();
  private AtomicBoolean started = new AtomicBoolean();
  private Map<Thread, ThreadMetrics> threadMetricsMap = new ConcurrentHashMap<Thread, ThreadMetrics>();
  private Map<String, MethodMetrics> methodMetricsMap = new ConcurrentHashMap<String, MethodMetrics>();
  private Map<Object, ObjectName> objectNameMap = new ConcurrentHashMap<Object, ObjectName>();
  private MBeanServer mBeanServer;
  private JolokiaServer jolokiaServer;
  private Thread backGroundThread;

  private ApmAgentContext() {
  }

  public static void enterMethod(String methodName) {
    ApmAgentContext apmAgentContext = ApmAgentContext.INSTANCE;
    if (apmAgentContext.isInitialized()) {
      Thread currentThread = Thread.currentThread();
      ThreadMetrics threadMetrics = apmAgentContext.threadMetricsMap.get(currentThread);
      if (threadMetrics == null) {
        threadMetrics = new ThreadMetrics(apmAgentContext, currentThread);
        apmAgentContext.threadMetricsMap.put(currentThread, threadMetrics);
      }
      threadMetrics.enter(methodName);

      MethodMetrics methodMetrics = apmAgentContext.methodMetricsMap.get(methodName);
      if (methodMetrics == null) {
        methodMetrics = new MethodMetrics(methodName);
        apmAgentContext.methodMetricsMap.put(methodName, methodMetrics);
        apmAgentContext.registerMethodMetricsMBean(methodMetrics);
      }
    }

  }

  public static void exitMethod(String methodName) {
    ApmAgentContext apmAgentContext = ApmAgentContext.INSTANCE;
    if (apmAgentContext.isInitialized()) {

      Thread currentThread = Thread.currentThread();

      ThreadMetrics threadMetrics = apmAgentContext.threadMetricsMap.get(currentThread);

      long elapsed = -1;
      if (threadMetrics != null) {
        elapsed = threadMetrics.exit(methodName);
      }

      MethodMetrics methodMetrics = apmAgentContext.methodMetricsMap.get(methodName);
      if (methodMetrics != null) {
        methodMetrics.update(elapsed);
      }
    }
  }

  public void initialize() {
    if (initialized.compareAndSet(false, true)) {
      backGroundThread = new Thread(new Runnable() {
        @Override
        public void run() {
          while (started.get())
            try {
              checkForDeadThreads();
              Thread.sleep(2000);
            } catch (Throwable e) {
              break;
            }
        }
      }, "ApmAgentContext background thread");
      backGroundThread.setDaemon(true);
    }
  }

  public void start() {
    if (initialized.get()) {
      if (started.compareAndSet(false, true)) {
        backGroundThread.start();
        try {
          ObjectName objectName = new ObjectName(DEFAULT_DOMAIN, "type", "apmAgent");
          ObjectInstance objectInstance = getMBeanServer().registerMBean(this, objectName);
          objectNameMap.put(this, objectInstance.getObjectName());
          ApmConfiguration configuration = ApmAgent.INSTANCE.getConfiguration();
          objectName = new ObjectName(DEFAULT_DOMAIN, "type", "configuration");
          objectInstance = getMBeanServer().registerMBean(configuration, objectName);
          objectNameMap.put(configuration, objectInstance.getObjectName());
        } catch (Throwable e) {
          LOG.error("Failed to register apmAgent mbeans with mBeanServer ", e);
        }
      }
    }
  }

  public void shutDown() {
    if (initialized.compareAndSet(true, false)) {
      started.set(false);
      backGroundThread = null;
      if (mBeanServer != null) {
        for (ObjectName objectName : objectNameMap.values()) {
          try {
            mBeanServer.unregisterMBean(objectName);
          } catch (Throwable e) {
            LOG.error("Failed to unregister mbean: " + objectName, e);
          }
        }
        if (jolokiaServer != null) {
          jolokiaServer.stop();
          jolokiaServer = null;

        }
        mBeanServer = null;
      }
      //unwind instrumentation
      ApmAgent.INSTANCE.shutdowm("");
    }
  }

  public boolean isAlreadyTransformed(String className) {
    synchronized (transformedClasses) {
      return transformedClasses.contains(className.replace("/", "."));
    }
  }

  public void addTransformedClass(String className) {
    synchronized (transformedClasses) {
      transformedClasses.add(className.replace("/", "."));
    }
  }

  public void removedTransformedClass(String className) {
    synchronized (transformedClasses) {
      transformedClasses.remove(className);
    }
  }

  public List<String> getTransformed() {
    List<String> result = new ArrayList<String>();
    result.addAll(transformedMethods);
    return result;
  }

  public List<ThreadMetrics> getThreadMetrics() {
    List<ThreadMetrics> result = new ArrayList<ThreadMetrics>();
    for (ThreadMetrics threadMetrics : threadMetricsMap.values()) {
      result.add(threadMetrics);
    }

    Collections.sort(result, new Comparator<ThreadMetrics>() {
      @Override
      public int compare(ThreadMetrics threadMetrics1, ThreadMetrics threadMetrics2) {
        return (int) (threadMetrics1.getCpuTime() - threadMetrics2.getCpuTime());
      }
    });
    return result;
  }

  public ApmConfiguration getApmConfiguration() {
    return ApmAgent.INSTANCE.getConfiguration();
  }

  public boolean isInitialized() {
    return initialized.get();
  }

  void registerThreadContextMethodMetricsMBean(ThreadContextMethodMetrics threadMetrics) {
    Hashtable<String, String> properties = new Hashtable<String, String>();
    properties.put("type", "threadContextMetrics");
    properties.put("name", ObjectName.quote(threadMetrics.getName()));
    properties.put("threadName", ObjectName.quote(threadMetrics.getThreadName()));
    properties.put("threadId", String.valueOf(threadMetrics.getThreadId()));
    properties.put("parent", ObjectName.quote(threadMetrics.getParentName()));
    properties.put("methodId", String.valueOf(System.identityHashCode(threadMetrics)));

    try {
      ObjectName objectName = new ObjectName(DEFAULT_DOMAIN, properties);
      ObjectInstance objectInstance = getMBeanServer().registerMBean(threadMetrics, objectName);
      objectNameMap.put(threadMetrics, objectInstance.getObjectName());
    } catch (Throwable e) {
      LOG.error("Failed to register mbean " + threadMetrics.toString(), e);
    }
  }

  void unregisterThreadContextMethodMetricsMBean(ThreadContextMethodMetrics threadMetrics) {
    ObjectName objectName = objectNameMap.remove(threadMetrics);
    if (objectName != null) {
      try {
        getMBeanServer().unregisterMBean(objectName);
      } catch (Throwable e) {
        LOG.error("Failed to unregister " + threadMetrics, e);
      }
    }
  }

  void registerMethodMetricsMBean(MethodMetrics methodMetrics) {
    Hashtable<String, String> properties = new Hashtable<String, String>();
    properties.put("type", "MethodMetrics");
    properties.put("name", ObjectName.quote(methodMetrics.getName()));
    properties.put("methodId", String.valueOf(System.identityHashCode(methodMetrics)));

    try {
      ObjectName objectName = new ObjectName(DEFAULT_DOMAIN, properties);
      ObjectInstance objectInstance = getMBeanServer().registerMBean(methodMetrics, objectName);
      objectNameMap.put(methodMetrics, objectInstance.getObjectName());
    } catch (Throwable e) {
      LOG.error("Failed to register mbean " + methodMetrics.toString(), e);
    }
  }

  void unregisterMethodMetricsMBean(MethodMetrics methodMetrics) {
    ObjectName objectName = objectNameMap.remove(methodMetrics);
    if (objectName != null) {
      try {
        getMBeanServer().unregisterMBean(objectName);
      } catch (Throwable e) {
        LOG.error("Failed to unregister " + methodMetrics, e);
      }
    }
  }

  private synchronized MBeanServer getMBeanServer() {
    if (mBeanServer == null) {
      // return platform mbean server if the option is specified.
      if (getApmConfiguration().isUsePlatformMBeanServer()) {
        mBeanServer = ManagementFactory.getPlatformMBeanServer();
      } else {
        mBeanServer = JolokiaMBeanServerUtil.getJolokiaMBeanServer();
      }
    }
    return mBeanServer;
  }

  private void checkForDeadThreads() {
    for (ThreadMetrics tm : threadMetricsMap.values()) {
      if (tm.isDead()) {
        tm.destroy();
        threadMetricsMap.remove(tm.getThread());
      }
    }
  }
}
