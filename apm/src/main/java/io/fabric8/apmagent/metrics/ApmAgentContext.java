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
import io.fabric8.apmagent.ClassInfo;
import io.fabric8.apmagent.MethodDescription;
import org.jolokia.jmx.JolokiaMBeanServerUtil;
import org.jolokia.jvmagent.JolokiaServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class ApmAgentContext {
    private static final Logger LOG = LoggerFactory.getLogger(ApmAgent.class);
    private final String DEFAULT_DOMAIN = "io.fabric8.apmagent";
    private final ConcurrentMap<String, ClassInfo> allMethods = new ConcurrentHashMap<>();
    private AtomicBoolean initialized = new AtomicBoolean();
    private AtomicBoolean started = new AtomicBoolean();
    private ConcurrentMap<Thread, ThreadMetrics> threadMetricsMap = new ConcurrentHashMap<>();
    private ConcurrentMap<String, MethodMetrics> methodMetricsMap = new ConcurrentHashMap<>();
    private ConcurrentMap<Object, ObjectName> objectNameMap = new ConcurrentHashMap<>();
    private MBeanServer mBeanServer;
    private JolokiaServer jolokiaServer;
    private final ApmAgent apmAgent;
    private ObjectName agentObjectName;
    private ObjectName configurationObjectName;
    private final AtomicReference<Timer> backgroundTimerRef = new AtomicReference<>();


    public ApmAgentContext(ApmAgent agent) {
        this.apmAgent = agent;
    }

    public void enterMethod(String fullMethodName) {
        if (isInitialized()) {
            Thread currentThread = Thread.currentThread();
            ThreadMetrics threadMetrics = threadMetricsMap.get(currentThread);
            if (threadMetrics == null) {
                threadMetrics = new ThreadMetrics(this, currentThread);
                threadMetricsMap.put(currentThread, threadMetrics);
            }
            threadMetrics.enter(fullMethodName);

            MethodMetrics methodMetrics = methodMetricsMap.get(fullMethodName);
            if (methodMetrics == null) {
                methodMetrics = new MethodMetrics(fullMethodName);
                if (methodMetricsMap.putIfAbsent(fullMethodName, methodMetrics) == null) {
                    //another thread could be doing the same thing at the same time,
                    //so only register if we actually added the method
                    registerMethodMetricsMBean(methodMetrics);
                }
            }
        }

    }

    public void exitMethod(String methodName) {
        if (isInitialized()) {

            Thread currentThread = Thread.currentThread();

            ThreadMetrics threadMetrics = threadMetricsMap.get(currentThread);

            long elapsed = -1;
            if (threadMetrics != null) {
                elapsed = threadMetrics.exit(methodName);
            }

            if (elapsed >= 0) {
                MethodMetrics methodMetrics = methodMetricsMap.get(methodName);
                if (methodMetrics != null) {
                    methodMetrics.update(elapsed);
                }
            }
        }
    }

    public void initialize() {
        if (initialized.compareAndSet(false, true)) {
            try {
                agentObjectName = new ObjectName(DEFAULT_DOMAIN, "type", "apmAgent");
                registerMBean(agentObjectName, apmAgent);
                ApmConfiguration configuration = apmAgent.getConfiguration();

                configurationObjectName = new ObjectName(DEFAULT_DOMAIN, "type", "configuration");
                registerMBean(configurationObjectName, configuration);
            } catch (Throwable e) {
                LOG.error("Failed to register apmAgent mbeans with mBeanServer ", e);
            }
        }
    }


    public void start() {
        if (initialized.get()) {
            if (started.compareAndSet(false, true)) {
                Timer oldValue = backgroundTimerRef.getAndSet(new Timer(true));
                if (oldValue != null) {
                    oldValue.cancel();
                }
                TimerTask deadThread = new TimerTask() {
                    @Override
                    public void run() {
                        for (ThreadMetrics tm : threadMetricsMap.values()) {
                            if (tm.isDead()) {
                                tm.destroy();
                                threadMetricsMap.remove(tm.getThread());
                            }
                        }
                    }
                };
                backgroundTimerRef.get().scheduleAtFixedRate(deadThread, 0, 2 * 1000);
            }
        }
    }

    public void stop() {
        if (initialized.get() && started.compareAndSet(true, false)) {
            Timer oldValue = backgroundTimerRef.getAndSet(null);
            if (oldValue != null) {
                oldValue.cancel();
            }
            for (ObjectName objectName : objectNameMap.values()) {
               unregisterMBean(objectName);
            }
            objectNameMap.clear();
            methodMetricsMap.clear();
            threadMetricsMap.clear();
        }
    }

    public void shutDown() {
        if (initialized.compareAndSet(true, false)) {
            stop();
            unregisterMBean(configurationObjectName);
            unregisterMBean(agentObjectName);
            if (jolokiaServer != null) {
                jolokiaServer.stop();
                jolokiaServer = null;

            }
            mBeanServer = null;
        }
    }


    public ClassInfo getClassInfo(String className) {
        String key = className.replace('/', '.');

        ClassInfo result = allMethods.get(key);
        if (result == null) {
            ClassInfo classInfo = new ClassInfo();
            classInfo.setClassName(key);
            result = allMethods.putIfAbsent(key, classInfo);
            if (result == null) {
                result = classInfo;
            }
        }
        return result;
    }

    public List<String> getTransformedMethods() {
        List<String> result = new ArrayList<>();
        for (ClassInfo classInfo : allMethods.values()) {
            for (String methodName : classInfo.getAllTransformedMethodNames()) {
                result.add(classInfo.getClassName() + "@" + methodName);
            }
        }
        return result;
    }

    public List<String> getAllMethods() {
        List<String> result = new ArrayList<>();
        for (ClassInfo classInfo : allMethods.values()) {
            for (String methodName : classInfo.getAllMethodNames()) {
                result.add(classInfo.getClassName() + "@" + methodName);
            }
        }
        return result;
    }

    public List<ThreadMetrics> getThreadMetrics() {
        List<ThreadMetrics> result = new ArrayList<>();
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

    public boolean isInitialized() {
        return initialized.get();
    }

    public List<ClassInfo> buildDeltaList() {
        ApmConfiguration configuration = apmAgent.getConfiguration();
        List<ClassInfo> result = new ArrayList<>();
        for (ClassInfo classInfo : allMethods.values()) {
            if (classInfo.isTransformed()) {
                //check to see its still should be audited
                if (configuration.isAudit(classInfo.getClassName())) {
                    boolean retransform = false;
                    //check to see if there's a change to methods that should be transformed
                    Set<String> transformedMethodNames = classInfo.getAllTransformedMethodNames();
                    for (String methodName : transformedMethodNames) {
                        if (!configuration.isAudit(classInfo.getClassName(), methodName)) {
                            retransform = true;
                            break;
                        }
                    }
                    if (!retransform) {
                        //check to see if there are methods that should now be audited but weren't
                        Set<String> allMethodNames = classInfo.getAllMethodNames();
                        for (String methodName : allMethodNames) {
                            if (!transformedMethodNames.contains(methodName) && configuration.isAudit(classInfo.getClassName(), methodName)) {
                                retransform = true;
                                break;
                            }
                        }
                    }
                    if (retransform) {
                        result.add(classInfo);
                    }

                } else {
                    //we were once audited - but now need to be removed
                    result.add(classInfo);
                }
            } else if (configuration.isAudit(classInfo.getClassName())) {
                if (classInfo.isCanTransform()) {
                    result.add(classInfo);
                }
            }
        }

        return result;
    }

    public void resetMethods(ClassInfo classInfo) {
        ApmConfiguration configuration = apmAgent.getConfiguration();
        Collection<MethodDescription> list = classInfo.getTransformedMethodDescriptions();
        for (MethodDescription methodDescription : list) {
            if (!configuration.isAudit(classInfo.getClassName(), methodDescription.getMethodName())) {
                remove(methodDescription);
            }
        }
    }

    public void resetAll(ClassInfo classInfo) {
        Collection<MethodDescription> list = classInfo.getTransformedMethodDescriptions();
        for (MethodDescription methodDescription : list) {
            remove(methodDescription);
        }
        classInfo.resetTransformed();
    }

    private void remove(MethodDescription methodDescription) {
        MethodMetrics methodMetrics = this.methodMetricsMap.remove(methodDescription.getFullMethodName());
        if (methodMetrics != null) {
            unregisterMethodMetricsMBean(methodMetrics);
        }
        for (ThreadMetrics threadMetrics : threadMetricsMap.values()) {
            threadMetrics.remove(methodDescription.getFullMethodName());
        }
    }

    void registerThreadContextMethodMetricsMBean(ThreadContextMethodMetrics threadMetrics) {
        try {
            ObjectName objectName = new ObjectName(DEFAULT_DOMAIN + ":"
                    + "type=ThreadContextMetrics"
                    + ",name=" + ObjectName.quote(threadMetrics.getName())
                    + ",threadName=" + ObjectName.quote(threadMetrics.getThreadName())
                    // + ",threadId=" + threadMetrics.getThreadId()
            );
            registerMBean(objectName, threadMetrics);
            objectNameMap.put(threadMetrics, objectName);
        } catch (Throwable e) {
            LOG.error("Failed to register mbean " + threadMetrics.toString(), e);
        }
    }

    void unregisterThreadContextMethodMetricsMBean(ThreadContextMethodMetrics threadMetrics) {
        ObjectName objectName = objectNameMap.remove(threadMetrics);
        unregisterMBean(objectName);
    }

    protected ObjectInstance registerMBean(ObjectName objectName, Object object) throws InstanceAlreadyExistsException, MBeanRegistrationException, NotCompliantMBeanException {
        MBeanServer server = getMBeanServer();
        if (server != null && !server.isRegistered(objectName)) {
            return server.registerMBean(object, objectName);
        }
        return null;
    }

    protected void unregisterMBean(ObjectName objectName) {
        MBeanServer beanServer = getMBeanServer();
        if (objectName != null && beanServer != null && beanServer.isRegistered(objectName)) {
            try {
                beanServer.unregisterMBean(objectName);
            } catch (Throwable e) {
                LOG.error("Failed to unregister " + objectName, e);
            }
        }
    }

    void registerMethodMetricsMBean(MethodMetrics methodMetrics) {
        try {
            ObjectName objectName = new ObjectName(DEFAULT_DOMAIN + ":" +
                    "type=MethodMetrics" +
                    ",name=" + ObjectName.quote(methodMetrics.getName())
                    // "methodId" + System.identityHashCode(methodMetrics))
            );
            registerMBean(objectName, methodMetrics);
            objectNameMap.put(methodMetrics, objectName);
        } catch (Throwable e) {
            LOG.error("Failed to register mbean " + methodMetrics.toString(), e);
        }
    }

    void unregisterMethodMetricsMBean(MethodMetrics methodMetrics) {
        ObjectName objectName = objectNameMap.remove(methodMetrics);
        unregisterMBean(objectName);
    }

    private synchronized MBeanServer getMBeanServer() {
        if (mBeanServer == null) {
            // return platform mbean server if the option is specified.
            if (apmAgent.getConfiguration().isUsePlatformMBeanServer()) {
                mBeanServer = ManagementFactory.getPlatformMBeanServer();
            } else {
                mBeanServer = JolokiaMBeanServerUtil.getJolokiaMBeanServer();
            }
        }
        return mBeanServer;
    }
}
