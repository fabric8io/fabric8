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

package io.fabric8.apmagent;

import io.fabric8.apmagent.metrics.ApmAgentContext;
import io.fabric8.apmagent.metrics.ThreadMetrics;
import io.fabric8.apmagent.utils.PropertyUtils;
import org.jolokia.jvmagent.JvmAgent;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.util.CheckClassAdapter;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.net.MalformedURLException;
import java.security.ProtectionDomain;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicBoolean;

public class ApmAgent implements ClassFileTransformer, ApmAgentMBean, ApmConfigurationFilterChangeListener {
    public static final ApmAgent INSTANCE = new ApmAgent();
    private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(ApmAgent.class);

    final ApmConfiguration configuration = new ApmConfiguration();
    private Instrumentation instrumentation;
    private AtomicBoolean cleanUp = new AtomicBoolean();
    private AtomicBoolean initialized = new AtomicBoolean();
    private AtomicBoolean started = new AtomicBoolean();
    private BlockingQueue<Class<?>> blockingQueue = new LinkedBlockingDeque<>();
    private Thread transformThread;
    private final ApmAgentContext apmAgentContext;

    // The following is the entry point when loaded dynamically to inject
    // recorders from the target process.

    private ApmAgent() {
        this.apmAgentContext = new ApmAgentContext(this);
    }

    // The following is the entry point when loaded as a java agent along with
    // the target process on the java command line.

    public static void agentmain(final String args,
                                 final Instrumentation instrumentation) throws Exception {
        try {

            ApmAgent agent = ApmAgent.INSTANCE;
            if (agent.initialize(instrumentation, args)) {
                if (agent.getConfiguration().isStartJolokiaAgent()) {
                    JvmAgent.agentmain(args);
                }
                agent.startMetrics();
            }
        } catch (Exception e) {
            LOG.info("Failed in agentmain", e);
            throw e;

        }

    }

    public static void premain(String args, Instrumentation instrumentation) throws Exception {
        try {
            ApmAgent agent = ApmAgent.INSTANCE;
            if (agent.initialize(instrumentation, args)) {
                if (agent.getConfiguration().isStartJolokiaAgent()) {
                    JvmAgent.premain(args);
                }
                if (agent.getConfiguration().isAutoStartMetrics()) {
                    agent.startMetrics();
                }
            }
        } catch (Exception e) {
            LOG.error("Failed in premain", e);
            throw e;
        }
    }

    @Override
    public synchronized byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
        byte[] buffer = null;
        ClassInfo classInfo = apmAgentContext.getClassInfo(className);
        classInfo.setOriginalClass(classBeingRedefined);
        if (classInfo.getTransformed() == null) {
            //we haven't been transformed before
            classInfo.setOriginal(classfileBuffer);
        }
        if (!cleanUp.get()) {
            byte[] classBufferToRedefine = classInfo.getOriginal();

            if (configuration.isAudit(className)) {
                if (classInfo.isTransformed()) {
                    //remove metrics from methods no longer defined
                    apmAgentContext.resetMethods(classInfo);
                }
                ClassReader cr = new ClassReader(classBufferToRedefine);

                ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);

                ApmClassVisitor visitor = new ApmClassVisitor(this, cw, classInfo);
                cr.accept(visitor, ClassReader.SKIP_FRAMES);
                buffer = cw.toByteArray();
                if (!verifyClass(className, buffer)) {
                    classInfo.setCanTransform(false);
                    buffer = null;
                }
                classInfo.setTransformed(buffer);
            }
        } else {
            if (classInfo.getOriginal() != null) {
                buffer = classInfo.getOriginal();
                apmAgentContext.resetAll(classInfo);
            }
        }
        return buffer;
    }

    public static void enterMethod(String methodName) {
        if (INSTANCE.started.get()) {
            INSTANCE.apmAgentContext.enterMethod(methodName);
        }
    }

    public static void exitMethod(String methodName) {
        if (INSTANCE.started.get()) {
            INSTANCE.apmAgentContext.exitMethod(methodName);
        }
    }

    public List<String> getTransformedMethods() {
        if (isInitialized()) {
            return apmAgentContext.getTransformedMethods();
        }
        return Collections.EMPTY_LIST;
    }

    public List<String> getAllMethods() {
        if (isInitialized()) {
            return apmAgentContext.getAllMethods();
        }
        return Collections.EMPTY_LIST;
    }

    public List<ThreadMetrics> getThreadMetrics() {
        if (isInitialized()) {
            return apmAgentContext.getThreadMetrics();
        }
        return Collections.EMPTY_LIST;
    }

    // The following is the implementation for instrumenting the target code.

    public void instrumentApplication() throws FileNotFoundException, UnmodifiableClassException {
        if (!instrumentation.isRetransformClassesSupported()) {
            throw new UnmodifiableClassException();
        }

        instrumentation.addTransformer(this, true);

        for (Class<?> c : instrumentation.getAllLoadedClasses()) {
            if (isInstrumentClass(c)) {
                if (configuration.isAsyncTransformation()) {
                    try {
                        blockingQueue.put(c);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                } else {
                    try {
                        instrumentation.retransformClasses(new Class[]{c});
                    } catch (Throwable e) {
                        LOG.error("Could not transform " + c.getName(), e);
                    }
                }
            }
        }
        if (configuration.isAsyncTransformation() && !blockingQueue.isEmpty()) {
            startTransformThread();
        }
    }

    public boolean isInitialized() {
        return initialized.get();
    }

    public ApmConfiguration getConfiguration() {
        return configuration;
    }

    /**
     * @param instrumentation
     * @param args
     * @return false if already initialized, else true if is actually initialized
     */
    public boolean initialize(final Instrumentation instrumentation, String args) throws ClassNotFoundException, FileNotFoundException, MalformedURLException, InstantiationException, IllegalAccessException {
        boolean result;
        if ((result = initialized.compareAndSet(false, true))) {
            this.instrumentation = instrumentation;
            cleanUp.set(false);

            this.instrumentation = instrumentation;
            PropertyUtils.setProperties(configuration, args);
            configuration.addChangeListener(this);
            apmAgentContext.initialize();

            //add shutdown hook
            Thread cleanup = new Thread() {

                @Override
                public void run() {

                    try {

                        ApmAgent apmAgent = ApmAgent.INSTANCE;
                        apmAgent.shutDown();

                    } catch (Exception e) {
                        LOG.error("Failed to run shutdown hook", e);
                    }

                }

            };
            Runtime.getRuntime().addShutdownHook(cleanup);
        }
        return result;
    }

    public void startMetrics() {
        if (isInitialized() && started.compareAndSet(false, true)) {
            apmAgentContext.start();
            try {
                instrumentApplication();
            } catch (Throwable e) {
                LOG.error("Failed to instrument application ", e);
            }
        }
    }

    public void stopMetrics() {
        if (isInitialized() && started.compareAndSet(true, false)) {
            apmAgentContext.stop();
        }
    }

    // The following is the implementation for resetting the instrumentation.

    public void shutDown() {
        if (initialized.compareAndSet(true, false)) {
            configuration.removeChangeListener(this);
            apmAgentContext.shutDown();
            instrumentation.removeTransformer(this);
            Thread t = transformThread;
            transformThread = null;
            if (t != null && !t.isInterrupted()) {
                t.interrupt();
            }
            cleanUp.set(true);
            try {
                //clean up
                instrumentApplication();
            } catch (Throwable e) {
                LOG.error("Failed to shutdown", e);
            }
        }
    }

    @Override
    public void configurationFilterChanged() {
        if (started.get()) {
            List<ClassInfo> deltas = apmAgentContext.buildDeltaList();
            if (deltas != null && !deltas.isEmpty()) {
                for (ClassInfo classInfo : deltas) {
                    if (configuration.isAsyncTransformation()) {
                        try {
                            blockingQueue.put(classInfo.getOriginalClass());
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            break;
                        }
                    } else {
                        try {
                            instrumentation.retransformClasses(new Class[]{classInfo.getOriginalClass()});
                        } catch (Throwable e) {
                            LOG.error("Could not transform " + classInfo.getClassName(), e);
                        }
                    }
                }
                if (configuration.isAsyncTransformation() && !blockingQueue.isEmpty()) {
                    startTransformThread();
                }
            }
        }
    }

    private boolean isInstrumentClass(Class c) {

        if (!instrumentation.isModifiableClass(c)) {
            LOG.trace("NO INSTRUMENT: Class " + c.getName() + " is not modifiable");
            return false;
        }
        if (!configuration.isAudit(c.getName())) {
            LOG.trace("NO INSTRUMENT: Class " + c.getName() + " is blacklisted");
            return false;
        }
        if (c.isArray() || c.isAnnotation() || c.isInterface() || c.isPrimitive() || c.isSynthetic() || c.isEnum()) {
            LOG.trace("NO INSTRUMENT: Class " + c.getName() + " is an array, primitive, annotation or enum etc");
            return false;
        }
        return true;
    }

    private synchronized void startTransformThread() {
        if (configuration.isAsyncTransformation() && transformThread == null) {
            transformThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    while (isInitialized() && !blockingQueue.isEmpty()) {
                        try {
                            Class<?> aClass = blockingQueue.take();
                            if (aClass != null) {
                                if (isInstrumentClass(aClass)) {
                                    try {
                                        instrumentation.retransformClasses(new Class[]{aClass});
                                    } catch (Throwable e) {
                                        LOG.error("Could not transform " + aClass.getName(), e);
                                    }
                                }
                            }
                        } catch (InterruptedException e) {
                            shutDown();
                        }
                    }
                }
            });
            transformThread.setDaemon(true);
            transformThread.start();
        }
    }

    private boolean verifyClass(String className, byte[] transformed) {
        boolean result = true;
        if (configuration.isVerifyClasses()) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            CheckClassAdapter.verify(new ClassReader(transformed), false, pw);
            if (sw.toString().length() != 0) {
                result = false;
                LOG.error(" Failed to transform class: " + className);
                LOG.error(sw.toString());
            }
        }
        return result;
    }
}
