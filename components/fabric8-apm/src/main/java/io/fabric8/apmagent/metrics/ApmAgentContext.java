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

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectInstance;
import javax.management.ObjectName;

import io.fabric8.apmagent.ApmAgent;
import io.fabric8.apmagent.ApmConfiguration;
import io.fabric8.apmagent.ClassInfo;
import io.fabric8.apmagent.MethodDescription;
import org.jolokia.jmx.JolokiaMBeanServerUtil;
import org.jolokia.jvmagent.JolokiaServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ApmAgentContext {
    private static final Logger LOG = LoggerFactory.getLogger(ApmAgent.class);
    private final String DEFAULT_DOMAIN = "io.fabric8.apmagent";
    private final long HOUSE_KEEPING_TIME = TimeUnit.SECONDS.toMillis(2);
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
    private final ApmConfiguration configuration;
    private final MonitoredMethodMetrics monitoredMethodMetrics;
    private AtomicBoolean doHouseKeeping = new AtomicBoolean();
    private Thread backgroundThread;
    private boolean monitorByDefault = true;

    public ApmAgentContext(ApmAgent agent) {
        this.apmAgent = agent;
        this.configuration = agent.getConfiguration();
        this.monitoredMethodMetrics = new MonitoredMethodMetrics(this);
        this.monitoredMethodMetrics.setMonitorSize(configuration.getMethodMetricDepth());
    }

    public void enterMethod(Thread currentThread, String fullMethodName, boolean alwaysActive) {
        if (isInitialized()) {
            ThreadMetrics threadMetrics = threadMetricsMap.get(currentThread);
            if (threadMetrics == null) {
                threadMetrics = new ThreadMetrics(this, currentThread);
                threadMetricsMap.put(currentThread, threadMetrics);
            }
            threadMetrics.enter(fullMethodName, alwaysActive);

            MethodMetrics methodMetrics = methodMetricsMap.get(fullMethodName);
            if (methodMetrics == null) {
                methodMetrics = new MethodMetrics(fullMethodName);
                methodMetrics.setActive(isMonitorByDefault());
                methodMetricsMap.putIfAbsent(fullMethodName, methodMetrics);
            }
        }

    }

    public void exitMethod(Thread currentThread, String methodName, boolean alwaysActive) {
        if (isInitialized()) {
            ThreadMetrics threadMetrics = threadMetricsMap.get(currentThread);

            long elapsed = -1;
            if (threadMetrics != null) {
                elapsed = threadMetrics.exit(methodName, alwaysActive);
            }

            if (elapsed >= 0) {
                MethodMetrics methodMetrics = methodMetricsMap.get(methodName);
                if (methodMetrics != null) {
                    methodMetrics.update(elapsed);
                }
            }
            doHouseKeeping();
        }
    }

    public void initialize() {
        if (initialized.compareAndSet(false, true)) {
            try {
                agentObjectName = new ObjectName(DEFAULT_DOMAIN, "type", "apmAgent");
                registerMBean(agentObjectName, apmAgent);
                configurationObjectName = new ObjectName(DEFAULT_DOMAIN, "type", "configuration");
                registerMBean(configurationObjectName, configuration);
            } catch (Throwable e) {
                LOG.warn("Failed to register ApmAgent mbeans with mBeanServer due " + e.getMessage(), e);
            }
        }
    }

    public void start() {
        if (initialized.get()) {
            if (started.compareAndSet(false, true)) {
                backgroundThread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        while (started.get()) {
                            try {
                                Thread.sleep(HOUSE_KEEPING_TIME);
                                doHouseKeeping.set(true);
                            } catch (Throwable e) {
                            }
                        }
                    }
                }, "Fabric8-ApmAgent-BackgroundThread");
                backgroundThread.setDaemon(true);
                backgroundThread.start();
            }
        }
    }

    void doHouseKeeping() {
        //the time is going to be the elapsed time from the latest method call
        //its not going to be terribly accurate - but then it doesn't really need to be
        if (doHouseKeeping.compareAndSet(true, false)) {
            try {
                List<ThreadMetrics> threadMetricsList = getThreadMetrics();
                for (ThreadMetrics tm : threadMetricsList) {
                    if (tm.isDead()) {
                        tm.destroy();
                        threadMetricsMap.remove(tm.getThread());
                    }
                }
                monitoredMethodMetrics.calculateMethodMetrics(getMethodMetrics());
                for (ThreadMetrics threadMetrics : threadMetricsList) {
                    threadMetrics.calculateMethodMetrics();
                }
            } catch (Throwable e) {
                LOG.warn("Error during housekeeping due " + e.getMessage() + ". This exception is ignored.", e);
            }
        }
    }

    public void stop() {
        if (initialized.get() && started.compareAndSet(true, false)) {
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
        List<ThreadMetrics> result = new ArrayList<>(threadMetricsMap.values());

        Collections.sort(result, new Comparator<ThreadMetrics>() {
            @Override
            public int compare(ThreadMetrics threadMetrics1, ThreadMetrics threadMetrics2) {
                return (int) (threadMetrics2.getCpuTime() - threadMetrics1.getCpuTime());
            }
        });
        return result;
    }

    public List<? extends MethodMetrics> getMethodMetrics() {
        return MethodMetrics.sortedMetrics(methodMetricsMap.values());
    }

    public boolean isInitialized() {
        return initialized.get();
    }

    public ApmConfiguration getConfiguration() {
        return configuration;
    }

    public boolean isMonitorByDefault() {
        return monitorByDefault;
    }

    public void setMonitorByDefault(boolean monitorByDefault) {
        this.monitorByDefault = monitorByDefault;
    }

    public void setActive(String fullMethodName, boolean flag) {
        if (isInitialized()) {
            for (ThreadMetrics threadMetrics : threadMetricsMap.values()) {
                threadMetrics.setActive(fullMethodName, flag);
            }

            MethodMetrics methodMetrics = methodMetricsMap.get(fullMethodName);
            if (methodMetrics != null) {
                methodMetrics.setActive(flag);
            }
        }
    }

    public List<ClassInfo> buildDeltaList() {
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

    public void methodMetricsDepthChanged() {
        monitoredMethodMetrics.setMonitorSize(configuration.getMethodMetricDepth());
    }

    public void threadMetricsDepthChanged() {
        for (ThreadMetrics threadMetrics : threadMetricsMap.values()) {
            threadMetrics.setMonitorSize(configuration.getThreadMetricDepth());
        }
    }

    private void remove(MethodDescription methodDescription) {
        methodMetricsMap.remove(methodDescription.getFullMethodName());
        for (ThreadMetrics threadMetrics : threadMetricsMap.values()) {
            threadMetrics.remove(methodDescription.getFullMethodName());
        }
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
                LOG.warn("Failed to unregister " + objectName + " due " + e.getMessage() + ". This exception is ignored.", e);
            }
        }
    }

    void registerMethodMetricsMBean(int rank, MethodMetricsProxy methodMetrics) {
        try {
            ObjectName objectName = new ObjectName(DEFAULT_DOMAIN + ":" +
                                                       "type=MethodMetrics" +
                                                       ",rank=" + ObjectName.quote("rank" + rank));
            LOG.debug("registered {}", objectName);
            registerMBean(objectName, methodMetrics);
            objectNameMap.put(methodMetrics, objectName);
        } catch (Throwable e) {
            LOG.warn("Failed to register mbean " + methodMetrics.toString() + " due " + e.getMessage() + ". This exception is ignored.", e);
        }
    }

    void registerMethodMetricsMBean(String threadName, long threadId, int rank, MethodMetricsProxy threadMetrics) {
        try {
            String threadIdentity = threadName + "[" + threadId + "]";
            ObjectName objectName = new ObjectName(DEFAULT_DOMAIN + ":"
                                                       + "type=ThreadContextMetrics"
                                                       + ",threadName=" + ObjectName.quote(threadIdentity)
                                                       + ",rank=" + ObjectName.quote("rank" + rank));
            registerMBean(objectName, threadMetrics);
            objectNameMap.put(threadMetrics, objectName);
        } catch (Throwable e) {
            LOG.warn("Failed to register mbean " + threadMetrics.toString() + " due " + e.getMessage() + ". This exception is ignored.", e);
        }
    }

    void unregisterMethodMetricsMBean(MethodMetricsProxy methodMetrics) {
        ObjectName objectName = objectNameMap.remove(methodMetrics);
        unregisterMBean(objectName);
    }

    private synchronized MBeanServer getMBeanServer() {
        if (mBeanServer == null) {
            // return platform mbean server if the option is specified.
            if (configuration.isUsePlatformMBeanServer()) {
                mBeanServer = ManagementFactory.getPlatformMBeanServer();
            } else {
                mBeanServer = JolokiaMBeanServerUtil.getJolokiaMBeanServer();
            }
        }
        return mBeanServer;
    }
}
